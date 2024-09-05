package com.comet.opik.domain;

import com.clickhouse.client.ClickHouseException;
import com.comet.opik.api.Project;
import com.comet.opik.api.Trace;
import com.comet.opik.api.TraceSearchCriteria;
import com.comet.opik.api.TraceUpdate;
import com.comet.opik.api.error.EntityAlreadyExistsException;
import com.comet.opik.api.error.ErrorMessage;
import com.comet.opik.api.error.IdentifierMismatchException;
import com.comet.opik.infrastructure.auth.RequestContext;
import com.comet.opik.infrastructure.db.TransactionTemplate;
import com.comet.opik.infrastructure.redis.LockService;
import com.comet.opik.utils.AsyncUtils;
import com.comet.opik.utils.WorkspaceUtils;
import com.google.inject.ImplementedBy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.comet.opik.domain.FeedbackScoreDAO.EntityType;

@ImplementedBy(TraceServiceImpl.class)
public interface TraceService {

    Mono<UUID> create(Trace trace);

    Mono<Void> update(TraceUpdate trace, UUID id);

    Mono<Trace> get(UUID id);

    Mono<Void> delete(UUID id);

    Mono<Trace.TracePage> find(int page, int size, TraceSearchCriteria criteria);

    Mono<Boolean> validateTraceWorkspace(String workspaceId, Set<UUID> traceIds);

}

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class TraceServiceImpl implements TraceService {

    public static final String PROJECT_NAME_AND_WORKSPACE_NAME_MISMATCH = "Project name and workspace name do not match the existing trace";
    public static final String TRACE_KEY = "Trace";

    private final @NonNull TraceDAO dao;
    private final @NonNull SpanDAO spanDAO;
    private final @NonNull FeedbackScoreDAO feedbackScoreDAO;
    private final @NonNull TransactionTemplate template;
    private final @NonNull ProjectService projectService;
    private final @NonNull IdGenerator idGenerator;
    private final @NonNull LockService lockService;

    @Override
    public Mono<UUID> create(@NonNull Trace trace) {

        String projectName = WorkspaceUtils.getProjectName(trace.projectName());
        UUID id = trace.id() == null ? idGenerator.generateId() : trace.id();

        return IdGenerator
                .validateVersionAsync(id, TRACE_KEY)
                .then(Mono.defer(() -> getOrCreateProject(projectName)))
                .flatMap(project -> lockService.executeWithLock(
                        new LockService.Lock(id, TRACE_KEY),
                        Mono.defer(() -> insertTrace(trace, project, id))));
    }

    private Mono<UUID> insertTrace(Trace newTrace, Project project, UUID id) {
        //TODO: refactor to implement proper conflict resolution
        return template.nonTransaction(connection -> dao.findById(id, connection))
                .flatMap(existingTrace -> insertTrace(newTrace, project, id, existingTrace))
                .switchIfEmpty(Mono.defer(() -> create(newTrace, project, id)))
                .onErrorResume(this::handleDBError);
    }

    private <T> Mono<T> handleDBError(Throwable ex) {
        if (ex instanceof ClickHouseException
                && ex.getMessage().contains("TOO_LARGE_STRING_SIZE")
                && (ex.getMessage().contains("_CAST(project_id, FixedString(36))")
                        && ex.getMessage().contains(", CAST(leftPad(workspace_id, 40, '*'), 'FixedString(19)') ::"))) {

            return failWithConflict(PROJECT_NAME_AND_WORKSPACE_NAME_MISMATCH);
        }

        return Mono.error(ex);
    }

    private Mono<Project> getProjectById(TraceUpdate traceUpdate) {
        return AsyncUtils.makeMonoContextAware((userName, workspaceName, workspaceId) -> {

            if (traceUpdate.projectId() != null) {
                return Mono.fromCallable(() -> projectService.get(traceUpdate.projectId(), workspaceId));
            }

            return Mono.empty();
        });
    }

    private Mono<Project> getOrCreateProject(String projectName) {
        return AsyncUtils.makeMonoContextAware((userName, workspaceName, workspaceId) -> {
            return Mono.fromCallable(() -> projectService.getOrCreate(workspaceId, projectName, userName))
                    .onErrorResume(e -> handleProjectCreationError(e, projectName, workspaceId))
                    .subscribeOn(Schedulers.boundedElastic());
        });
    }

    private Mono<UUID> insertTrace(Trace newTrace, Project project, UUID id, Trace existingTrace) {
        return Mono.defer(() -> {
            //TODO: refactor to implement proper conflict resolution
            // check if a partial trace exists caused by a patch request
            if (existingTrace.name().isBlank()
                    && existingTrace.startTime().equals(Instant.EPOCH)
                    && existingTrace.projectId().equals(project.id())) {

                return create(newTrace, project, id);
            }

            if (!project.id().equals(existingTrace.projectId())) {
                return failWithConflict(PROJECT_NAME_AND_WORKSPACE_NAME_MISMATCH);
            }

            // otherwise, reject the trace creation
            return Mono
                    .error(new EntityAlreadyExistsException(new ErrorMessage(List.of("Trace already exists"))));
        });
    }

    private Mono<UUID> create(Trace trace, Project project, UUID id) {
        return template.nonTransaction(connection -> {
            var newTrace = trace.toBuilder().id(id).projectId(project.id()).build();
            return dao.insert(newTrace, connection);
        });
    }

    private Mono<Project> handleProjectCreationError(Throwable exception, String projectName, String workspaceId) {
        return switch (exception) {
            case EntityAlreadyExistsException __ ->
                Mono.fromCallable(
                        () -> projectService.findByNames(workspaceId, List.of(projectName)).stream().findFirst()
                                .orElseThrow())
                        .subscribeOn(Schedulers.boundedElastic());
            default -> Mono.error(exception);
        };
    }

    @Override
    public Mono<Void> update(@NonNull TraceUpdate traceUpdate, @NonNull UUID id) {

        var projectName = WorkspaceUtils.getProjectName(traceUpdate.projectName());

        return getProjectById(traceUpdate)
                .switchIfEmpty(Mono.defer(() -> getOrCreateProject(projectName)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(project -> lockService.executeWithLock(
                        new LockService.Lock(id, TRACE_KEY),
                        Mono.defer(() -> template.nonTransaction(connection -> dao.findById(id, connection))
                                .flatMap(trace -> updateOrFail(traceUpdate, id, trace, project).thenReturn(id))
                                .switchIfEmpty(Mono.defer(() -> insertUpdate(project, traceUpdate, id))
                                        .thenReturn(id))
                                .onErrorResume(this::handleDBError))))
                .then();
    }

    private Mono<Void> insertUpdate(Project project, TraceUpdate traceUpdate, UUID id) {
        return IdGenerator
                .validateVersionAsync(id, TRACE_KEY)
                .then(Mono.defer(() -> template.nonTransaction(
                        connection -> dao.partialInsert(project.id(), traceUpdate, id, connection))));
    }

    private Mono<Void> updateOrFail(TraceUpdate traceUpdate, UUID id, Trace trace, Project project) {
        if (project.id().equals(trace.projectId())) {
            return template.nonTransaction(connection -> dao.update(traceUpdate, id, connection));
        }

        return failWithConflict(PROJECT_NAME_AND_WORKSPACE_NAME_MISMATCH);
    }

    private Mono<Project> getProjectByName(String projectName) {
        return Mono.deferContextual(ctx -> {
            String workspaceId = ctx.get(RequestContext.WORKSPACE_ID);

            return Mono.fromCallable(() -> projectService.findByNames(workspaceId, List.of(projectName)))
                    .flatMap(projects -> projects.stream().findFirst().map(Mono::just).orElseGet(Mono::empty))
                    .subscribeOn(Schedulers.boundedElastic());
        });
    }

    private <T> Mono<T> failWithConflict(String error) {
        return Mono.error(new IdentifierMismatchException(new ErrorMessage(List.of(error))));
    }

    private NotFoundException failWithNotFound(String error) {
        return new NotFoundException(Response.status(404).entity(new ErrorMessage(List.of(error))).build());
    }

    @Override
    public Mono<Trace> get(@NonNull UUID id) {
        return template.nonTransaction(connection -> dao.findById(id, connection))
                .switchIfEmpty(Mono.defer(() -> Mono.error(failWithNotFound("Trace not found"))));
    }

    @Override
    public Mono<Void> delete(@NonNull UUID id) {
        return lockService.executeWithLock(
                new LockService.Lock(id, TRACE_KEY),
                Mono.defer(() -> template
                        .nonTransaction(
                                connection -> feedbackScoreDAO.deleteByEntityId(EntityType.TRACE, id, connection))
                        .then(Mono.defer(
                                () -> template.nonTransaction(connection -> spanDAO.deleteByTraceId(id, connection))))
                        .then(Mono.defer(() -> template.nonTransaction(connection -> dao.delete(id, connection))))));
    }

    @Override
    public Mono<Trace.TracePage> find(int page, int size, @NonNull TraceSearchCriteria criteria) {

        if (criteria.projectId() != null) {
            return template.nonTransaction(connection -> dao.find(size, page, criteria, connection));
        }

        return getProjectByName(criteria.projectName())
                .flatMap(project -> template.nonTransaction(connection -> dao.find(
                        size, page, criteria.toBuilder().projectId(project.id()).build(), connection)))
                .switchIfEmpty(Mono.just(Trace.TracePage.empty(page)));
    }

    @Override
    public Mono<Boolean> validateTraceWorkspace(@NonNull String workspaceId, @NonNull Set<UUID> traceIds) {
        if (traceIds.isEmpty()) {
            return Mono.just(true);
        }

        return template.nonTransaction(connection -> dao.getTraceWorkspace(traceIds, connection)
                .all(traceWorkspace -> workspaceId.equals(traceWorkspace.workspaceId())));
    }

}