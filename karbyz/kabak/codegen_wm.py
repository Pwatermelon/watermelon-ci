from __future__ import annotations

from typing import Any

from .ast_nodes import (
    Block, HostStmt, Literal, PrintStmt, Program, RunStmt, StageStmt, Binary, Name,
)


def _collect_runs(node: Any, acc: list[str], host: str | None = None):
    if isinstance(node, list):
        for x in node:
            _collect_runs(x, acc, host)
    elif isinstance(node, Block):
        _collect_runs(node.stmts, acc, host)
    elif isinstance(node, StageStmt):
        _collect_runs(node.body, acc, host)
    elif isinstance(node, HostStmt):
        _collect_runs(node.body, acc, node.name)
    elif isinstance(node, RunStmt):
        cmd = _literal_cmd(node.cmd)
        prefix = f"# host={host} " if host else ""
        acc.append(prefix + cmd)
    elif isinstance(node, PrintStmt):
        parts = []
        for a in node.args:
            if isinstance(a, Literal):
                parts.append(str(a.value))
            elif isinstance(a, Name):
                parts.append(f"${{{a.name}}}")
            else:
                parts.append("…")
        acc.append("echo " + " ".join(parts))


def _literal_cmd(expr: Any) -> str:
    if isinstance(expr, Literal) and isinstance(expr.value, str):
        return expr.value
    if isinstance(expr, Binary) and expr.op == "+" and isinstance(expr.left, Literal):
        return str(expr.left.value) + _literal_cmd(expr.right)
    if isinstance(expr, Name):
        return f"${{{expr.name}}}"
    return "echo unresolved-command"


def emit_pipeline_ir(program: Program) -> str:
    """Каноническая YAML-сериализация той же модели манифеста (PipelineManifest).

    Карбыз уже является манифестом Watermelon CI; это не «генерация манифеста из языка»,
    а эквивалентная запись модели для компилятора Jenkinsfile.
    """
    stages: list[str] = []
    jobs: dict[str, list[str]] = {}

    for item in program.body:
        if isinstance(item, StageStmt):
            stages.append(item.name)
            scripts: list[str] = []
            _collect_runs(item.body, scripts)
            if not scripts:
                scripts = [f'echo "stage {item.name}"']
            jobs[item.name] = scripts
        elif isinstance(item, HostStmt):
            stage = f"host-{item.name}"
            stages.append(stage)
            scripts = []
            _collect_runs(item, scripts)
            jobs[stage] = scripts or [f'echo "host {item.name}"']
        elif isinstance(item, RunStmt):
            if "shell" not in stages:
                stages.append("shell")
                jobs["shell"] = []
            jobs.setdefault("shell", []).append(_literal_cmd(item.cmd))

    if not stages:
        stages = ["run"]
        jobs["run"] = ['echo "Karbyz pipeline"']

    lines = [
        f"name: {program.name}",
        f"stages: [{', '.join(stages)}]",
        "jobs:",
    ]
    for stage, scripts in jobs.items():
        lines.append(f"  {stage}:")
        lines.append(f"    stage: {stage}")
        lines.append("    script:")
        for s in scripts:
            safe = s.replace('"', '\\"')
            lines.append(f'      - "{safe}"')
    return "\n".join(lines) + "\n"


# обратная совместимость имени
emit_watermelon_yaml = emit_pipeline_ir
