from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Optional


@dataclass
class Program:
    name: str
    body: list[Any]


@dataclass
class VarDecl:
    type_name: str
    name: str
    init: Optional[Any] = None


@dataclass
class FuncDecl:
    name: str
    params: list[tuple[str, str]]
    body: list[Any]


@dataclass
class Block:
    stmts: list[Any]


@dataclass
class Assign:
    name: str
    index: Optional[Any]
    value: Any


@dataclass
class IfStmt:
    cond: Any
    then_body: Block
    else_body: Optional[Block]


@dataclass
class WhileStmt:
    cond: Any
    body: Block


@dataclass
class ForStmt:
    init: Optional[Any]
    cond: Any
    step: Optional[Any]
    body: Block


@dataclass
class ReturnStmt:
    value: Optional[Any]


@dataclass
class ExprStmt:
    expr: Any


@dataclass
class PrintStmt:
    args: list[Any]


@dataclass
class InputStmt:
    type_name: str
    name: str


@dataclass
class StageStmt:
    name: str
    body: Block


@dataclass
class RunStmt:
    cmd: Any


@dataclass
class HostStmt:
    name: str
    body: Block


@dataclass
class Binary:
    op: str
    left: Any
    right: Any


@dataclass
class Unary:
    op: str
    expr: Any


@dataclass
class Literal:
    value: Any


@dataclass
class Name:
    name: str


@dataclass
class Index:
    target: Any
    index: Any


@dataclass
class Call:
    name: str
    args: list[Any]


@dataclass
class ArrayLit:
    elements: list[Any]


@dataclass
class LenCall:
    expr: Any
