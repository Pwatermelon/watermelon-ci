from __future__ import annotations

from dataclasses import dataclass

from .errors import KarbyzError


@dataclass
class Token:
    kind: str
    value: str
    line: int
    col: int


# Только обычная кириллица (без әөүҗңһ). Синтаксис не C/Python:
# блоки «:: … ахыр», присваивание «<-», хвосты «булса/вакыт/да/кадак».
KEYWORDS = {
    "жыелма": "PROGRAM",
    "ахыр": "END",
    "сан": "TYPE_NUM",
    "суз": "TYPE_STR",
    "тезма": "TYPE_ARR",
    "дорес": "TRUE",
    "ялган": "FALSE",
    "агар": "IF",
    "юкса": "ELSE",
    "булса": "THEN",
    "шулай": "WHILE",
    "вакыт": "DO",
    "очен": "FOR",
    "да": "FOR_MID",
    "кадак": "FOR_STEP",
    "эшлама": "FUNC",
    "кайтар": "RETURN",
    "керт": "INPUT",
    "чыгар": "PRINT",
    "этап": "STAGE",
    "башкар": "RUN",
    "сервер": "HOST",
    "хам": "AND",
    "яки": "OR",
    "тугел": "NOT",
    "озынлык": "LEN",
}


class Lexer:
    def __init__(self, text: str):
        self.text = text
        self.i = 0
        self.line = 1
        self.col = 1

    def _peek(self, n: int = 0) -> str:
        j = self.i + n
        return self.text[j] if j < len(self.text) else "\0"

    def _adv(self) -> str:
        ch = self._peek()
        self.i += 1
        if ch == "\n":
            self.line += 1
            self.col = 1
        else:
            self.col += 1
        return ch

    def tokens(self) -> list[Token]:
        out: list[Token] = []
        while True:
            t = self.next_token()
            out.append(t)
            if t.kind == "EOF":
                break
        return out

    def next_token(self) -> Token:
        while True:
            ch = self._peek()
            if ch in " \t\r":
                self._adv()
                continue
            if ch == "#":
                while self._peek() not in "\n\0":
                    self._adv()
                continue
            if ch == "\n":
                self._adv()
                continue
            break

        line, col = self.line, self.col
        ch = self._peek()
        if ch == "\0":
            return Token("EOF", "", line, col)

        # присваивание <-
        if ch == "<" and self._peek(1) == "-":
            self._adv()
            self._adv()
            return Token("ASSIGN", "<-", line, col)

        # открытие блока ::
        if ch == ":" and self._peek(1) == ":":
            self._adv()
            self._adv()
            return Token("OPEN", "::", line, col)

        if ch.isalpha() or ch == "_":
            start = self.i
            while self._peek().isalnum() or self._peek() == "_":
                self._adv()
            word = self.text[start : self.i]
            kind = KEYWORDS.get(word.lower(), "IDENT")
            # ключевые слова всегда в нижнем регистре; IDENT сохраняет написание
            if kind != "IDENT":
                return Token(kind, word.lower(), line, col)
            return Token("IDENT", word, line, col)

        if ch.isdigit():
            start = self.i
            while self._peek().isdigit():
                self._adv()
            return Token("NUMBER", self.text[start : self.i], line, col)

        if ch == '"':
            self._adv()
            buf: list[str] = []
            while self._peek() not in '"\0':
                c = self._adv()
                if c == "\\" and self._peek() != "\0":
                    n = self._adv()
                    buf.append({"n": "\n", "t": "\t", '"': '"', "\\": "\\"}.get(n, n))
                else:
                    buf.append(c)
            if self._peek() != '"':
                raise KarbyzError("лексер", line, col, "незакрытая строка")
            self._adv()
            return Token("STRING", "".join(buf), line, col)

        two = ch + self._peek(1)
        for op in ("==", "!=", "<=", ">=", "&&", "||"):
            if two == op:
                self._adv()
                self._adv()
                return Token("OP", op, line, col)

        singles = {
            "(": "LPAREN", ")": "RPAREN",
            "[": "LBRACK", "]": "RBRACK",
            ",": "COMMA", ".": "DOT",
        }
        if ch in singles:
            self._adv()
            return Token(singles[ch], ch, line, col)
        if ch in "+-*/%<>!":
            self._adv()
            return Token("OP", ch, line, col)

        self._adv()
        raise KarbyzError("лексер", line, col, f"неожиданный символ {ch!r}")
