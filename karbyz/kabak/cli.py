from __future__ import annotations

import argparse
import sys
from pathlib import Path

from .codegen_wm import emit_pipeline_ir
from .errors import KarbyzError
from .interpreter import Interpreter
from .parser import parse_source


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(
        description="Транслятор Карбыз — манифест Watermelon CI (татарский синтаксис)"
    )
    p.add_argument("source", help="манифест .kbz")
    p.add_argument(
        "--as-yaml",
        "--emit-wm",
        dest="as_yaml",
        action="store_true",
        help="показать тот же манифест в YAML-синтаксисе (эквивалент, не «генерация»)",
    )
    p.add_argument("--out", help="файл вывода YAML-эквивалента")
    p.add_argument("--execute-shell", action="store_true", help="реально выполнять башкар")
    args = p.parse_args(argv)

    text = Path(args.source).read_text(encoding="utf-8")
    try:
        program = parse_source(text)
        if args.as_yaml:
            yaml = emit_pipeline_ir(program)
            if args.out:
                Path(args.out).write_text(yaml, encoding="utf-8")
                print(f"written {args.out}")
            else:
                print(yaml, end="")
            return 0
        Interpreter(dry_run=not args.execute_shell).run(program)
        return 0
    except KarbyzError as e:
        print(str(e), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
