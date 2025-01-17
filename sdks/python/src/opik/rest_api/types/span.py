# This file was auto-generated by Fern from our API Definition.

import datetime as dt
import typing

from ..core.datetime_utils import serialize_datetime
from ..core.pydantic_utilities import deep_union_pydantic_dicts, pydantic_v1
from .feedback_score import FeedbackScore
from .json_node import JsonNode
from .span_type import SpanType


class Span(pydantic_v1.BaseModel):
    id: typing.Optional[str] = None
    project_name: typing.Optional[str] = pydantic_v1.Field(default=None)
    """
    If null, the default project is used
    """

    project_id: typing.Optional[str] = None
    trace_id: str
    parent_span_id: typing.Optional[str] = None
    name: str
    type: SpanType
    start_time: dt.datetime
    end_time: typing.Optional[dt.datetime] = None
    input: typing.Optional[JsonNode] = None
    output: typing.Optional[JsonNode] = None
    metadata: typing.Optional[JsonNode] = None
    model: typing.Optional[str] = None
    provider: typing.Optional[str] = None
    tags: typing.Optional[typing.List[str]] = None
    usage: typing.Optional[typing.Dict[str, int]] = None
    created_at: typing.Optional[dt.datetime] = None
    last_updated_at: typing.Optional[dt.datetime] = None
    created_by: typing.Optional[str] = None
    last_updated_by: typing.Optional[str] = None
    feedback_scores: typing.Optional[typing.List[FeedbackScore]] = None
    total_estimated_cost: typing.Optional[float] = None

    def json(self, **kwargs: typing.Any) -> str:
        kwargs_with_defaults: typing.Any = {
            "by_alias": True,
            "exclude_unset": True,
            **kwargs,
        }
        return super().json(**kwargs_with_defaults)

    def dict(self, **kwargs: typing.Any) -> typing.Dict[str, typing.Any]:
        kwargs_with_defaults_exclude_unset: typing.Any = {
            "by_alias": True,
            "exclude_unset": True,
            **kwargs,
        }
        kwargs_with_defaults_exclude_none: typing.Any = {
            "by_alias": True,
            "exclude_none": True,
            **kwargs,
        }

        return deep_union_pydantic_dicts(
            super().dict(**kwargs_with_defaults_exclude_unset),
            super().dict(**kwargs_with_defaults_exclude_none),
        )

    class Config:
        frozen = True
        smart_union = True
        extra = pydantic_v1.Extra.allow
        json_encoders = {dt.datetime: serialize_datetime}
