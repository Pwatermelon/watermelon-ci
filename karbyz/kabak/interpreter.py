from __future__ import annotations

from typing import Any, Callable, Optional, TextIO

from .ast_nodes import (
    ArrayLit, Assign, Binary, Block, Call, ExprStmt, ForStmt, FuncDecl, HostStmt,
    IfStmt, Index, LenCall, Literal, Name, PrintStmt, Program, ReturnStmt, RunStmt,
    StageStmt, Unary, VarDecl, WhileStmt, InputStmt,
)
from .errors import KarbyzError


class ReturnSignal(Exception):
    def __init__(self, value: Any):
        self.value = value


class Env:
    def __init__(self, parent: Optional["Env"] = None):
        self.parent = parent
        self.vars: dict[str, Any] = {}
        self.types: dict[str, str] = {}

    def declare(self, name: str, typ: str, value: Any):
        if name in self.vars:
            raise KarbyzError("семантика", 0, 0, f"повторное объявление {name}")
        self.types[name] = typ
        self.vars[name] = value

    def set(self, name: str, value: Any):
        env: Optional[Env] = self
        while env:
            if name in env.vars:
                typ = env.types.get(name)
                if typ:
                    ok = (
                        (typ == "сан" and isinstance(value, int) and not isinstance(value, bool))
                        or (typ == "суз" and isinstance(value, str))
                        or (typ == "тезма" and isinstance(value, list))
                    )
                    if not ok:
                        raise KarbyzError(
                            "семантика",
                            0,
                            0,
                            f"тип {typ} не совместим со значением {value!r}",
                        )
                env.vars[name] = value
                return
            env = env.parent
        raise KarbyzError("семантика", 0, 0, f"неизвестная переменная {name}")

    def get(self, name: str) -> Any:
        env: Optional[Env] = self
        while env:
            if name in env.vars:
                return env.vars[name]
            env = env.parent
        raise KarbyzError("семантика", 0, 0, f"неизвестная переменная {name}")


class Interpreter:
    def __init__(self, stdin: Optional[TextIO] = None, stdout: Optional[TextIO] = None, dry_run: bool = True):
        import sys
        self.stdin = stdin or sys.stdin
        self.stdout = stdout or sys.stdout
        self.dry_run = dry_run
        self.functions: dict[str, FuncDecl] = {}
        self.global_env = Env()
        self.commands: list[str] = []

    def run(self, program: Program) -> list[str]:
        # register funcs first
        top: list[Any] = []
        for item in program.body:
            if isinstance(item, FuncDecl):
                self.functions[item.name] = item
            else:
                top.append(item)
        for item in top:
            self.exec_stmt(item, self.global_env)
        return self.commands

    def exec_block(self, block: Block, env: Env):
        for s in block.stmts:
            self.exec_stmt(s, env)

    def exec_stmt(self, stmt: Any, env: Env):
        if isinstance(stmt, Block):
            self.exec_block(stmt, Env(env))
        elif isinstance(stmt, VarDecl):
            val = self.eval(stmt.init, env) if stmt.init is not None else self.default(stmt.type_name)
            self.check_type(stmt.type_name, val)
            env.declare(stmt.name, stmt.type_name, val)
        elif isinstance(stmt, Assign):
            val = self.eval(stmt.value, env)
            if stmt.index is None:
                env.set(stmt.name, val)
            else:
                arr = env.get(stmt.name)
                if not isinstance(arr, list):
                    raise KarbyzError("исполнение", 0, 0, f"{stmt.name} не тезма")
                idx = self.eval(stmt.index, env)
                arr[int(idx)] = val
        elif isinstance(stmt, IfStmt):
            if self.truthy(self.eval(stmt.cond, env)):
                self.exec_block(stmt.then_body, Env(env))
            elif stmt.else_body:
                self.exec_block(stmt.else_body, Env(env))
        elif isinstance(stmt, WhileStmt):
            while self.truthy(self.eval(stmt.cond, env)):
                self.exec_block(stmt.body, Env(env))
        elif isinstance(stmt, ForStmt):
            loop_env = Env(env)
            if stmt.init:
                self.exec_stmt(stmt.init, loop_env)
            while self.truthy(self.eval(stmt.cond, loop_env)):
                self.exec_block(stmt.body, Env(loop_env))
                if stmt.step:
                    self.exec_stmt(stmt.step, loop_env)
        elif isinstance(stmt, ReturnStmt):
            raise ReturnSignal(self.eval(stmt.value, env) if stmt.value is not None else None)
        elif isinstance(stmt, PrintStmt):
            parts = [self.stringify(self.eval(a, env)) for a in stmt.args]
            print(" ".join(parts), file=self.stdout)
        elif isinstance(stmt, InputStmt):
            line = self.stdin.readline()
            if line == "":
                raise KarbyzError("исполнение", 0, 0, "конец ввода")
            line = line.rstrip("\n")
            if stmt.type_name == "тезма":
                raise KarbyzError(
                    "семантика", 0, 0, "керт(тезма) не поддерживается в версии 1.0"
                )
            if stmt.type_name == "сан":
                try:
                    val: Any = int(line, 10)
                except ValueError:
                    raise KarbyzError(
                        "исполнение",
                        0,
                        0,
                        f"неверный формат для типа сан: ожидалось десятичное целое, получено {line!r}",
                    ) from None
            elif stmt.type_name == "суз":
                val = line
            else:
                raise KarbyzError("семантика", 0, 0, f"неизвестный тип ввода {stmt.type_name}")
            env.declare(stmt.name, stmt.type_name, val)
        elif isinstance(stmt, StageStmt):
            print(f"# этап {stmt.name}", file=self.stdout)
            self.exec_block(stmt.body, Env(env))
        elif isinstance(stmt, RunStmt):
            cmd = self.stringify(self.eval(stmt.cmd, env))
            self.commands.append(cmd)
            if self.dry_run:
                print(f"$ {cmd}", file=self.stdout)
            else:
                import subprocess
                subprocess.run(cmd, shell=True, check=False)
        elif isinstance(stmt, HostStmt):
            print(f"# сервер {stmt.name}", file=self.stdout)
            self.exec_block(stmt.body, Env(env))
        elif isinstance(stmt, ExprStmt):
            self.eval(stmt.expr, env)
        elif isinstance(stmt, FuncDecl):
            self.functions[stmt.name] = stmt
        else:
            raise KarbyzError("исполнение", 0, 0, f"неизвестный оператор {type(stmt)}")

    def default(self, typ: str) -> Any:
        return {"сан": 0, "суз": "", "тезма": []}.get(typ, None)

    def check_type(self, typ: str, val: Any):
        ok = (
            (typ == "сан" and isinstance(val, int) and not isinstance(val, bool))
            or (typ == "суз" and isinstance(val, str))
            or (typ == "тезма" and isinstance(val, list))
        )
        if not ok:
            raise KarbyzError("семантика", 0, 0, f"тип {typ} не совместим со значением {val!r}")

    def truthy(self, v: Any) -> bool:
        if isinstance(v, bool):
            return v
        if isinstance(v, int):
            return v != 0
        if isinstance(v, str):
            return v != ""
        if isinstance(v, list):
            return len(v) > 0
        return bool(v)

    def stringify(self, v: Any) -> str:
        if isinstance(v, bool):
            return "истина" if v else "ложь"
        if isinstance(v, list):
            return "[" + ", ".join(self.stringify(x) for x in v) + "]"
        return str(v)

    def eval(self, node: Any, env: Env) -> Any:
        if node is None:
            return None
        if isinstance(node, Literal):
            return node.value
        if isinstance(node, Name):
            return env.get(node.name)
        if isinstance(node, ArrayLit):
            return [self.eval(e, env) for e in node.elements]
        if isinstance(node, LenCall):
            v = self.eval(node.expr, env)
            if not isinstance(v, (list, str)):
                raise KarbyzError("исполнение", 0, 0, "озынлык только для суз|тезма")
            return len(v)
        if isinstance(node, Index):
            target = self.eval(node.target, env)
            idx = int(self.eval(node.index, env))
            return target[idx]
        if isinstance(node, Call):
            return self.call(node.name, [self.eval(a, env) for a in node.args], env)
        if isinstance(node, Unary):
            v = self.eval(node.expr, env)
            if node.op in ("тугел", "!"):
                return not self.truthy(v)
            if node.op == "-":
                return -int(v)
            if node.op == "+":
                return int(v)
        if isinstance(node, Binary):
            if node.op in ("хам", "&&"):
                return self.truthy(self.eval(node.left, env)) and self.truthy(self.eval(node.right, env))
            if node.op in ("яки", "||"):
                return self.truthy(self.eval(node.left, env)) or self.truthy(self.eval(node.right, env))
            l = self.eval(node.left, env)
            r = self.eval(node.right, env)
            if node.op == "+":
                if isinstance(l, str) or isinstance(r, str):
                    return self.stringify(l) + self.stringify(r)
                return int(l) + int(r)
            if node.op == "-":
                return int(l) - int(r)
            if node.op == "*":
                return int(l) * int(r)
            if node.op == "/":
                return int(l) // int(r)
            if node.op == "%":
                return int(l) % int(r)
            if node.op == "==":
                return l == r
            if node.op == "!=":
                return l != r
            if node.op == "<":
                return l < r
            if node.op == "<=":
                return l <= r
            if node.op == ">":
                return l > r
            if node.op == ">=":
                return l >= r
        raise KarbyzError("исполнение", 0, 0, f"не удалось вычислить {node!r}")

    def call(self, name: str, args: list[Any], env: Env) -> Any:
        fn = self.functions.get(name)
        if not fn:
            raise KarbyzError("семантика", 0, 0, f"неизвестная функция {name}")
        if len(args) != len(fn.params):
            raise KarbyzError("семантика", 0, 0, f"аргументов у {name}: ожид. {len(fn.params)}")
        local = Env(self.global_env)
        for (typ, pname), val in zip(fn.params, args):
            self.check_type(typ, val)
            local.declare(pname, typ, val)
        try:
            for s in fn.body:
                self.exec_stmt(s, local)
        except ReturnSignal as rs:
            return rs.value
        return None
