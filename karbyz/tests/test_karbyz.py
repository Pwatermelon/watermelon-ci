import io
import unittest
from pathlib import Path

from kabak.codegen_wm import emit_pipeline_ir
from kabak.errors import KarbyzError
from kabak.interpreter import Interpreter
from kabak.parser import parse_source

ROOT = Path(__file__).resolve().parents[1]
EX = ROOT / "examples"


class KarbyzTests(unittest.TestCase):
    def test_hello(self):
        prog = parse_source((EX / "hello.kbz").read_text(encoding="utf-8"))
        out = io.StringIO()
        Interpreter(stdout=out).run(prog)
        self.assertIn("Карбыз работает!", out.getvalue())

    def test_all_constructs(self):
        prog = parse_source((EX / "all_constructs.kbz").read_text(encoding="utf-8"))
        out = io.StringIO()
        Interpreter(stdout=out).run(prog)
        text = out.getvalue()
        self.assertIn("Привет,", text)
        self.assertIn("сервис:", text)
        self.assertIn("сумма= 25", text)

    def test_pipeline_ir_equivalent(self):
        """YAML-сериализация — эквивалент той же модели манифеста, не «генерация продукта»."""
        prog = parse_source((EX / "devops_demo.kbz").read_text(encoding="utf-8"))
        ir = emit_pipeline_ir(prog)
        self.assertIn("name: storefront", ir)
        self.assertIn("stages:", ir)
        self.assertIn("checkout", ir)
        self.assertIn("publish", ir)
        self.assertIn("script:", ir)

    def test_syntax_error(self):
        src = (EX / "with_error.kbz").read_text(encoding="utf-8")
        with self.assertRaises(KarbyzError) as cm:
            parse_source(src)
        self.assertEqual(cm.exception.phase, "синтаксис")

    def test_input_output(self):
        src = (EX / "io_demo.kbz").read_text(encoding="utf-8")
        prog = parse_source(src)
        out = io.StringIO()
        inp = io.StringIO("7\nhello\n")
        Interpreter(stdin=inp, stdout=out).run(prog)
        text = out.getvalue()
        self.assertIn("удвоенное: 14", text)
        self.assertIn("вы ввели: hello", text)

    def test_arrays_and_functions(self):
        src = """
жыелма t ::
  тезма a <- [1, 2, 3]
  эшлама квадрат (сан n) ::
    кайтар n * n
  ахыр
  чыгар (квадрат (a[2]))
ахыр жыелма.
"""
        prog = parse_source(src)
        out = io.StringIO()
        Interpreter(stdout=out).run(prog)
        self.assertEqual(out.getvalue().strip(), "9")


if __name__ == "__main__":
    unittest.main()
