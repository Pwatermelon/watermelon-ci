from __future__ import annotations

from typing import Any

from .ast_nodes import (
    ArrayLit, Assign, Binary, Block, Call, ExprStmt, ForStmt, FuncDecl, HostStmt,
    IfStmt, Index, LenCall, Literal, Name, PrintStmt, Program, ReturnStmt, RunStmt,
    StageStmt, Unary, VarDecl, WhileStmt, InputStmt,
)
from .errors import KarbyzError
from .lexer import Lexer, Token


class Parser:
    """Рекурсивный спуск под синтаксис Карбыз v2 (блоки ::/ахыр, <-)."""

    def __init__(self, tokens: list[Token]):
        self.tokens = tokens
        self.i = 0

    def _cur(self) -> Token:
        return self.tokens[self.i]

    def _eat(self, kind: str | None = None, value: str | None = None) -> Token:
        t = self._cur()
        if kind and t.kind != kind:
            raise KarbyzError(
                "синтаксис", t.line, t.col,
                f"ожидалось {kind}, получено {t.kind} ({t.value!r})",
            )
        if value is not None and t.value != value:
            raise KarbyzError(
                "синтаксис", t.line, t.col,
                f"ожидалось {value!r}, получено {t.value!r}",
            )
        self.i += 1
        return t

    def _match(self, kind: str, value: str | None = None) -> bool:
        t = self._cur()
        if t.kind != kind:
            return False
        if value is not None and t.value != value:
            return False
        self.i += 1
        return True

    def parse(self) -> Program:
        self._eat("PROGRAM")
        name = self._eat("IDENT").value
        self._eat("OPEN")  # ::
        body = []
        while not (self._cur().kind == "END" and self._peek_program_end()):
            if self._cur().kind == "EOF":
                t = self._cur()
                raise KarbyzError("синтаксис", t.line, t.col, "ожидалось: ахыр жыелма.")
            body.append(self.decl_or_stmt())
        self._eat("END")
        self._eat("PROGRAM")
        self._eat("DOT")
        self._eat("EOF")
        return Program(name, body)

    def _peek_program_end(self) -> bool:
        # ахыр жыелма .
        if self.i + 1 >= len(self.tokens):
            return False
        return self.tokens[self.i + 1].kind == "PROGRAM"

    def decl_or_stmt(self):
        k = self._cur().kind
        if k in ("TYPE_NUM", "TYPE_STR", "TYPE_ARR"):
            return self.var_decl()
        if k == "FUNC":
            return self.func_decl()
        return self.statement()

    def type_name(self) -> str:
        t = self._cur()
        mapping = {"TYPE_NUM": "сан", "TYPE_STR": "суз", "TYPE_ARR": "тезма"}
        if t.kind not in mapping:
            raise KarbyzError("синтаксис", t.line, t.col, "ожидался тип сан|суз|тезма")
        self._eat()
        return mapping[t.kind]

    def var_decl(self) -> VarDecl:
        typ = self.type_name()
        name = self._eat("IDENT").value
        init = None
        if self._match("ASSIGN"):
            init = self.expr()
        return VarDecl(typ, name, init)

    def func_decl(self) -> FuncDecl:
        self._eat("FUNC")
        name = self._eat("IDENT").value
        self._eat("LPAREN")
        params: list[tuple[str, str]] = []
        if self._cur().kind != "RPAREN":
            typ = self.type_name()
            pname = self._eat("IDENT").value
            params.append((typ, pname))
            while self._match("COMMA"):
                typ = self.type_name()
                pname = self._eat("IDENT").value
                params.append((typ, pname))
        self._eat("RPAREN")
        body = self.block()
        return FuncDecl(name, params, body.stmts)

    def block(self) -> Block:
        self._eat("OPEN")
        stmts = []
        while self._cur().kind != "END":
            if self._cur().kind == "EOF":
                t = self._cur()
                raise KarbyzError("синтаксис", t.line, t.col, "незакрытый блок (нужен ахыр)")
            # внутри if: юкса закрывает then-блок
            if self._cur().kind == "ELSE":
                break
            stmts.append(self.statement())
        if self._cur().kind == "END":
            self._eat("END")
        return Block(stmts)

    def statement(self):
        k = self._cur().kind
        if k in ("TYPE_NUM", "TYPE_STR", "TYPE_ARR"):
            return self.var_decl()
        if k == "IF":
            return self.if_stmt()
        if k == "WHILE":
            return self.while_stmt()
        if k == "FOR":
            return self.for_stmt()
        if k == "RETURN":
            self._eat("RETURN")
            val = None
            t = self._cur()
            if t.kind in ("NUMBER", "STRING", "TRUE", "FALSE", "LPAREN", "LBRACK", "IDENT", "LEN", "NOT") or (
                t.kind == "OP" and t.value in ("+", "-", "!")
            ):
                val = self.expr()
            return ReturnStmt(val)
        if k == "PRINT":
            self._eat("PRINT")
            self._eat("LPAREN")
            args = []
            if self._cur().kind != "RPAREN":
                args.append(self.expr())
                while self._match("COMMA"):
                    args.append(self.expr())
            self._eat("RPAREN")
            return PrintStmt(args)
        if k == "INPUT":
            self._eat("INPUT")
            self._eat("LPAREN")
            typ = self.type_name()
            name = self._eat("IDENT").value
            self._eat("RPAREN")
            return InputStmt(typ, name)
        if k == "STAGE":
            self._eat("STAGE")
            name = self._eat("STRING").value
            return StageStmt(name, self.block())
        if k == "RUN":
            self._eat("RUN")
            cmd = self.expr()
            return RunStmt(cmd)
        if k == "HOST":
            self._eat("HOST")
            name = self._eat("STRING").value
            return HostStmt(name, self.block())
        if k == "IDENT":
            name = self._eat("IDENT").value
            if self._cur().kind == "LBRACK":
                self._eat("LBRACK")
                idx = self.expr()
                self._eat("RBRACK")
                self._eat("ASSIGN")
                return Assign(name, idx, self.expr())
            if self._match("ASSIGN"):
                return Assign(name, None, self.expr())
            if self._match("LPAREN"):
                args = []
                if self._cur().kind != "RPAREN":
                    args.append(self.expr())
                    while self._match("COMMA"):
                        args.append(self.expr())
                self._eat("RPAREN")
                return ExprStmt(Call(name, args))
            t = self._cur()
            raise KarbyzError("синтаксис", t.line, t.col, "ожидалось <- или вызов")
        t = self._cur()
        raise KarbyzError("синтаксис", t.line, t.col, f"неожиданный токен {t.kind}")

    def if_stmt(self) -> IfStmt:
        self._eat("IF")
        self._eat("LPAREN")
        cond = self.expr()
        self._eat("RPAREN")
        self._eat("THEN")  # булса
        then_b = self.block_until_else_or_end()
        else_b = None
        if self._match("ELSE"):
            else_b = self.block()
        elif self._cur().kind == "END":
            self._eat("END")
        return IfStmt(cond, then_b, else_b)

    def block_until_else_or_end(self) -> Block:
        self._eat("OPEN")
        stmts = []
        while self._cur().kind not in ("END", "ELSE", "EOF"):
            stmts.append(self.statement())
        if self._cur().kind == "END":
            self._eat("END")
        return Block(stmts)

    def while_stmt(self) -> WhileStmt:
        self._eat("WHILE")
        self._eat("LPAREN")
        cond = self.expr()
        self._eat("RPAREN")
        self._eat("DO")  # вакыт
        return WhileStmt(cond, self.block())

    def for_stmt(self) -> ForStmt:
        # очен (сан j <- 0 да j < 3 кадак j <- j + 1) :: ... ахыр
        self._eat("FOR")
        self._eat("LPAREN")
        init = None
        if self._cur().kind != "FOR_MID":
            if self._cur().kind in ("TYPE_NUM", "TYPE_STR", "TYPE_ARR"):
                typ = self.type_name()
                name = self._eat("IDENT").value
                self._eat("ASSIGN")
                init = VarDecl(typ, name, self.expr())
            else:
                name = self._eat("IDENT").value
                self._eat("ASSIGN")
                init = Assign(name, None, self.expr())
        self._eat("FOR_MID")  # да
        cond = self.expr()
        self._eat("FOR_STEP")  # кадак
        step = None
        if self._cur().kind != "RPAREN":
            name = self._eat("IDENT").value
            self._eat("ASSIGN")
            step = Assign(name, None, self.expr())
        self._eat("RPAREN")
        return ForStmt(init, cond, step, self.block())

    def expr(self):
        return self.or_expr()

    def or_expr(self):
        node = self.and_expr()
        while True:
            if self._match("OR") or self._match("OP", "||"):
                node = Binary("яки", node, self.and_expr())
            else:
                break
        return node

    def and_expr(self):
        node = self.not_expr()
        while True:
            if self._match("AND") or self._match("OP", "&&"):
                node = Binary("хам", node, self.not_expr())
            else:
                break
        return node

    def not_expr(self):
        if self._match("NOT") or self._match("OP", "!"):
            return Unary("тугел", self.not_expr())
        return self.rel_expr()

    def rel_expr(self):
        node = self.add_expr()
        while self._cur().kind == "OP" and self._cur().value in ("==", "!=", "<", "<=", ">", ">="):
            op = self._eat("OP").value
            node = Binary(op, node, self.add_expr())
        return node

    def add_expr(self):
        node = self.mul_expr()
        while self._cur().kind == "OP" and self._cur().value in ("+", "-"):
            op = self._eat("OP").value
            node = Binary(op, node, self.mul_expr())
        return node

    def mul_expr(self):
        node = self.unary()
        while self._cur().kind == "OP" and self._cur().value in ("*", "/", "%"):
            op = self._eat("OP").value
            node = Binary(op, node, self.unary())
        return node

    def unary(self):
        if self._cur().kind == "OP" and self._cur().value in ("+", "-"):
            op = self._eat("OP").value
            return Unary(op, self.unary())
        return self.primary()

    def primary(self):
        t = self._cur()
        if t.kind == "NUMBER":
            self._eat()
            return Literal(int(t.value))
        if t.kind == "STRING":
            self._eat()
            return Literal(t.value)
        if t.kind == "TRUE":
            self._eat()
            return Literal(True)
        if t.kind == "FALSE":
            self._eat()
            return Literal(False)
        if t.kind == "LEN":
            self._eat()
            self._eat("LPAREN")
            e = self.expr()
            self._eat("RPAREN")
            return LenCall(e)
        if t.kind == "LBRACK":
            self._eat("LBRACK")
            els = []
            if self._cur().kind != "RBRACK":
                els.append(self.expr())
                while self._match("COMMA"):
                    els.append(self.expr())
            self._eat("RBRACK")
            return ArrayLit(els)
        if t.kind == "LPAREN":
            self._eat("LPAREN")
            e = self.expr()
            self._eat("RPAREN")
            return e
        if t.kind == "IDENT":
            name = self._eat("IDENT").value
            node: Any = Name(name)
            if self._match("LPAREN"):
                args = []
                if self._cur().kind != "RPAREN":
                    args.append(self.expr())
                    while self._match("COMMA"):
                        args.append(self.expr())
                self._eat("RPAREN")
                node = Call(name, args)
            while self._match("LBRACK"):
                idx = self.expr()
                self._eat("RBRACK")
                node = Index(node, idx)
            return node
        raise KarbyzError("синтаксис", t.line, t.col, f"неожиданное выражение ({t.kind})")


def parse_source(text: str) -> Program:
    tokens = Lexer(text).tokens()
    return Parser(tokens).parse()
