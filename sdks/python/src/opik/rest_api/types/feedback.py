# This file was auto-generated by Fern from our API Definition.

from __future__ import annotations

import datetime as dt
import typing

from ..core.datetime_utils import serialize_datetime
from ..core.pydantic_utilities import deep_union_pydantic_dicts, pydantic_v1
from .categorical_feedback_detail import CategoricalFeedbackDetail
from .numerical_feedback_detail import NumericalFeedbackDetail


class Base(pydantic_v1.BaseModel):
    id: typing.Optional[str] = None
    name: str
    details: typing.Dict[str, typing.Any]
    created_at: typing.Optional[dt.datetime] = pydantic_v1.Field(
        alias="createdAt", default=None
    )
    created_by: typing.Optional[str] = pydantic_v1.Field(
        alias="createdBy", default=None
    )
    last_updated_at: typing.Optional[dt.datetime] = pydantic_v1.Field(
        alias="lastUpdatedAt", default=None
    )
    last_updated_by: typing.Optional[str] = pydantic_v1.Field(
        alias="lastUpdatedBy", default=None
    )

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
        allow_population_by_field_name = True
        populate_by_name = True
        extra = pydantic_v1.Extra.allow
        json_encoders = {dt.datetime: serialize_datetime}


class Feedback_Numerical(Base):
    details: typing.Optional[NumericalFeedbackDetail] = None
    created_at: typing.Optional[dt.datetime] = None
    created_by: typing.Optional[str] = None
    last_updated_at: typing.Optional[dt.datetime] = None
    last_updated_by: typing.Optional[str] = None
    type: typing.Literal["numerical"] = "numerical"

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
        allow_population_by_field_name = True
        populate_by_name = True
        extra = pydantic_v1.Extra.allow
        json_encoders = {dt.datetime: serialize_datetime}


class Feedback_Categorical(Base):
    details: typing.Optional[CategoricalFeedbackDetail] = None
    created_at: typing.Optional[dt.datetime] = None
    created_by: typing.Optional[str] = None
    last_updated_at: typing.Optional[dt.datetime] = None
    last_updated_by: typing.Optional[str] = None
    type: typing.Literal["categorical"] = "categorical"

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
        allow_population_by_field_name = True
        populate_by_name = True
        extra = pydantic_v1.Extra.allow
        json_encoders = {dt.datetime: serialize_datetime}


Feedback = typing.Union[Feedback_Numerical, Feedback_Categorical]