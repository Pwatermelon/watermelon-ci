class KarbyzError(Exception):
    def __init__(self, phase: str, line: int, col: int, message: str):
        self.phase = phase
        self.line = line
        self.col = col
        self.message = message
        super().__init__(f"[{phase}] {line}:{col}: {message}")
