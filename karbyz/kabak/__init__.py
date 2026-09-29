"""Разбор и исполнение манифеста Карбыз (Watermelon CI)."""

from .parser import parse_source
from .interpreter import Interpreter
from .codegen_wm import emit_pipeline_ir, emit_watermelon_yaml
from .errors import KarbyzError

__all__ = [
    "parse_source",
    "Interpreter",
    "emit_pipeline_ir",
    "emit_watermelon_yaml",
    "KarbyzError",
]
