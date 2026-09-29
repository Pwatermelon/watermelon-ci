#!/usr/bin/env python3
"""CLI для языка Карбыз — из корня: python3 karbyz/run.py …"""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))


def main() -> int:
    argv = sys.argv[1:]
    if not argv or argv[0] in ("-h", "--help"):
        print(
            "Карбыз — манифест Watermelon CI (татарский синтаксис)\n\n"
            "  python3 karbyz/run.py test\n"
            "  python3 karbyz/run.py examples/hello.kbz\n"
            "  python3 karbyz/run.py examples/devops_demo.kbz --as-yaml\n"
        )
        return 0

    if argv[0] == "test":
        suite = unittest.defaultTestLoader.discover(
            start_dir=str(ROOT / "tests"),
            pattern="test_*.py",
            top_level_dir=str(ROOT),
        )
        ok = unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful()
        return 0 if ok else 1

    from kabak.cli import main as cli_main

    args = []
    for a in argv:
        p = ROOT / a
        args.append(str(p) if (not a.startswith("-") and p.exists()) else a)
    return cli_main(args)


if __name__ == "__main__":
    raise SystemExit(main())
