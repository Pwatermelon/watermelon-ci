package io.watermelon.ci.manifest.parse;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.manifest.model.JobSpec;
import io.watermelon.ci.manifest.model.PipelineManifest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Карбыз — тот же манифест Watermelon CI (татарский синтаксис).
 * Разбирает платформенное подмножество в {@link PipelineManifest}.
 */
@Component
public class KarbyzManifestParser {

    private static final Set<String> KEYWORDS = Set.of(
            "жыелма", "ахыр", "этап", "сервер", "башкар", "чыгар",
            "сан", "суз", "тезма", "эшлама", "агар", "булса", "юкса",
            "шулай", "вакыт", "очен", "да", "кадак", "кайтар", "керт",
            "дорес", "ялган", "озынлык", "яки", "хам", "тугел");

    public boolean isKarbyz(String source) {
        if (source == null) {
            return false;
        }
        String trimmed = stripLineComments(source).trim();
        return trimmed.startsWith("жыелма");
    }

    public PipelineManifest parse(String source) {
        try {
            Lexer lx = new Lexer(stripLineComments(source));
            return parseProgram(lx);
        } catch (PlatformException ex) {
            throw ex;
        } catch (ParseException ex) {
            throw new PlatformException(ErrorCode.MANIFEST_INVALID, "karbyz: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new PlatformException(ErrorCode.MANIFEST_INVALID, "failed to parse Karbyz: " + ex.getMessage(), ex);
        }
    }

    private PipelineManifest parseProgram(Lexer lx) {
        lx.expectKeyword("жыелма");
        String name = lx.expectIdent();
        lx.expect("::");

        PipelineManifest manifest = new PipelineManifest();
        manifest.setName(name);
        List<String> stages = new ArrayList<>();
        Map<String, JobSpec> jobs = new LinkedHashMap<>();

        while (!(lx.peekKeyword("ахыр") && lx.peekAheadKeyword(1, "жыелма"))) {
            if (lx.peekKeyword("этап")) {
                parseStage(lx, stages, jobs);
            } else if (lx.peekKeyword("сервер")) {
                parseHost(lx, stages, jobs);
            } else if (lx.peekKeyword("башкар") || lx.peekKeyword("чыгар")) {
                ensureShell(stages, jobs);
                jobs.get("shell").getScript().add(parseCommandLine(lx, null));
            } else if (lx.peekKeyword("эшлама")) {
                skipFunction(lx);
            } else if (lx.peekKeyword("сан") || lx.peekKeyword("суз") || lx.peekKeyword("тезма")) {
                skipDeclOrAssign(lx);
            } else if (lx.peekKeyword("агар") || lx.peekKeyword("шулай") || lx.peekKeyword("очен")) {
                skipControl(lx);
            } else if (lx.peekIdent()) {
                // вызов или присваивание — для манифеста игнорируем до конца «оператора»
                skipLooseStatement(lx);
            } else {
                throw new ParseException("unexpected token near pipeline body: " + lx.peekRaw());
            }
        }

        lx.expectKeyword("ахыр");
        lx.expectKeyword("жыелма");
        lx.expect(".");
        lx.expectEof();

        if (stages.isEmpty()) {
            stages.add("run");
            JobSpec job = new JobSpec();
            job.setStage("run");
            job.setScript(List.of("echo \"Karbyz pipeline " + name + "\""));
            jobs.put("run", job);
        }

        manifest.setStages(stages);
        manifest.setJobs(jobs);
        return manifest;
    }

    private void parseStage(Lexer lx, List<String> stages, Map<String, JobSpec> jobs) {
        lx.expectKeyword("этап");
        String stageName = lx.expectString();
        lx.expect("::");
        List<String> script = parseBlockCommands(lx, null);
        stages.add(stageName);
        JobSpec job = new JobSpec();
        job.setStage(stageName);
        if (script.isEmpty()) {
            script = List.of("echo \"stage " + stageName + "\"");
        }
        job.setScript(script);
        jobs.put(stageName, job);
    }

    private void parseHost(Lexer lx, List<String> stages, Map<String, JobSpec> jobs) {
        lx.expectKeyword("сервер");
        String host = lx.expectString();
        lx.expect("::");
        String stageName = "host-" + host;
        List<String> script = parseBlockCommands(lx, host);
        stages.add(stageName);
        JobSpec job = new JobSpec();
        job.setStage(stageName);
        if (script.isEmpty()) {
            script = List.of("echo \"host " + host + "\"");
        }
        job.setScript(script);
        jobs.put(stageName, job);
    }

    private List<String> parseBlockCommands(Lexer lx, String host) {
        List<String> script = new ArrayList<>();
        while (!lx.peekKeyword("ахыр")) {
            if (lx.peekKeyword("башкар") || lx.peekKeyword("чыгар")) {
                script.add(parseCommandLine(lx, host));
            } else if (lx.peekKeyword("этап") || lx.peekKeyword("сервер")) {
                throw new ParseException("nested этап/сервер is not supported in manifesto subset");
            } else if (lx.peekKeyword("эшлама")) {
                skipFunction(lx);
            } else if (lx.peekKeyword("сан") || lx.peekKeyword("суз") || lx.peekKeyword("тезма")) {
                skipDeclOrAssign(lx);
            } else if (lx.peekKeyword("агар") || lx.peekKeyword("шулай") || lx.peekKeyword("очен")) {
                skipControl(lx);
            } else if (lx.peekIdent()) {
                skipLooseStatement(lx);
            } else {
                throw new ParseException("unexpected token in block: " + lx.peekRaw());
            }
        }
        lx.expectKeyword("ахыр");
        return script;
    }

    private String parseCommandLine(Lexer lx, String host) {
        if (lx.peekKeyword("башкар")) {
            lx.expectKeyword("башкар");
            String cmd = expressionAsCommand(lx);
            return host == null ? cmd : "# host=" + host + " " + cmd;
        }
        lx.expectKeyword("чыгар");
        lx.expect("(");
        List<String> parts = new ArrayList<>();
        if (!lx.peek(")")) {
            parts.add(expressionAsEchoPart(lx));
            while (lx.match(",")) {
                parts.add(expressionAsEchoPart(lx));
            }
        }
        lx.expect(")");
        String echo = "echo " + String.join(" ", parts);
        return host == null ? echo : "# host=" + host + " " + echo;
    }

    private String expressionAsCommand(Lexer lx) {
        if (lx.peekString()) {
            return lx.expectString();
        }
        if (lx.peekIdent()) {
            return "${" + lx.expectIdent() + "}";
        }
        throw new ParseException("башкар expects string or identifier");
    }

    private String expressionAsEchoPart(Lexer lx) {
        if (lx.peekString()) {
            return lx.expectString();
        }
        if (lx.peekIdent()) {
            return "${" + lx.expectIdent() + "}";
        }
        if (lx.peekNumber()) {
            return lx.expectNumber();
        }
        // грубо съесть первичное выражение
        String raw = lx.peekRaw();
        lx.advance();
        return raw;
    }

    private void ensureShell(List<String> stages, Map<String, JobSpec> jobs) {
        if (!stages.contains("shell")) {
            stages.add("shell");
            JobSpec job = new JobSpec();
            job.setStage("shell");
            job.setScript(new ArrayList<>());
            jobs.put("shell", job);
        }
    }

    private void skipFunction(Lexer lx) {
        lx.expectKeyword("эшлама");
        lx.expectIdent();
        lx.expect("(");
        while (!lx.peek(")")) {
            lx.advance();
        }
        lx.expect(")");
        lx.expect("::");
        skipUntilMatchingAkhyr(lx);
    }

    private void skipControl(Lexer lx) {
        if (lx.peekKeyword("агар")) {
            lx.expectKeyword("агар");
            skipBalancedParens(lx);
            lx.expectKeyword("булса");
            lx.expect("::");
            while (!lx.peekKeyword("ахыр") && !lx.peekKeyword("юкса")) {
                skipLooseStatement(lx);
            }
            if (lx.peekKeyword("юкса")) {
                lx.expectKeyword("юкса");
                lx.expect("::");
                while (!lx.peekKeyword("ахыр")) {
                    skipLooseStatement(lx);
                }
            }
            lx.expectKeyword("ахыр");
            return;
        }
        if (lx.peekKeyword("шулай")) {
            lx.expectKeyword("шулай");
            skipBalancedParens(lx);
            lx.expectKeyword("вакыт");
            lx.expect("::");
            skipUntilMatchingAkhyr(lx);
            return;
        }
        lx.expectKeyword("очен");
        skipBalancedParens(lx);
        lx.expect("::");
        skipUntilMatchingAkhyr(lx);
    }

    private void skipDeclOrAssign(Lexer lx) {
        lx.advance(); // type
        lx.expectIdent();
        if (lx.match("<-")) {
            skipExpression(lx);
        }
    }

    private void skipLooseStatement(Lexer lx) {
        if (lx.peekKeyword("башкар") || lx.peekKeyword("чыгар")) {
            parseCommandLine(lx, null);
            return;
        }
        if (lx.peekKeyword("кайтар")) {
            lx.expectKeyword("кайтар");
            if (!lx.peekKeyword("ахыр") && !lx.peek("::") && !lx.peekEof()) {
                if (!lx.peekKeyword("жыелма") && !lx.peekKeyword("этап") && !lx.peekKeyword("сервер")) {
                    skipExpression(lx);
                }
            }
            return;
        }
        if (lx.peekKeyword("агар") || lx.peekKeyword("шулай") || lx.peekKeyword("очен")) {
            skipControl(lx);
            return;
        }
        if (lx.peekKeyword("эшлама")) {
            skipFunction(lx);
            return;
        }
        if (lx.peekKeyword("сан") || lx.peekKeyword("суз") || lx.peekKeyword("тезма")) {
            skipDeclOrAssign(lx);
            return;
        }
        // Ident <- …  or Ident(…)
        lx.expectIdent();
        if (lx.match("<-")) {
            skipExpression(lx);
            return;
        }
        if (lx.match("[")) {
            skipExpression(lx);
            lx.expect("]");
            if (lx.match("<-")) {
                skipExpression(lx);
            }
            return;
        }
        if (lx.match("(")) {
            if (!lx.peek(")")) {
                skipExpression(lx);
                while (lx.match(",")) {
                    skipExpression(lx);
                }
            }
            lx.expect(")");
            return;
        }
        throw new ParseException("cannot skip statement near: " + lx.peekRaw());
    }

    private void skipUntilMatchingAkhyr(Lexer lx) {
        int depth = 1;
        while (depth > 0) {
            if (lx.peekEof()) {
                throw new ParseException("unclosed block");
            }
            if (lx.peek("::")) {
                // possible nested block start already consumed with keyword; treat ахыр
                lx.advance();
                continue;
            }
            if (lx.peekKeyword("ахыр")) {
                lx.advance();
                depth--;
                continue;
            }
            // nested :: blocks after keywords that open blocks
            if (lx.peekKeyword("агар") || lx.peekKeyword("шулай") || lx.peekKeyword("очен")
                    || lx.peekKeyword("эшлама") || lx.peekKeyword("этап") || lx.peekKeyword("сервер")) {
                // let specialized skip handle if we call them; otherwise advance
            }
            if (lx.peekKeyword("булса") || lx.peekKeyword("юкса") || lx.peekKeyword("вакыт")) {
                lx.advance();
                if (lx.peek("::")) {
                    lx.advance();
                    depth++;
                }
                continue;
            }
            lx.advance();
        }
    }

    private void skipBalancedParens(Lexer lx) {
        lx.expect("(");
        int depth = 1;
        while (depth > 0) {
            if (lx.peekEof()) {
                throw new ParseException("unclosed '('");
            }
            if (lx.peek("(")) {
                depth++;
                lx.advance();
            } else if (lx.peek(")")) {
                depth--;
                lx.advance();
            } else {
                lx.advance();
            }
        }
    }

    private void skipExpression(Lexer lx) {
        // очень упрощённо: первичное + операторы
        skipPrimary(lx);
        while (lx.match("+") || lx.match("-") || lx.match("*") || lx.match("/") || lx.match("%")
                || lx.match("==") || lx.match("!=") || lx.match("<=") || lx.match(">=")
                || lx.match("<") || lx.match(">") || lx.matchKeyword("яки") || lx.matchKeyword("хам")
                || lx.match("||") || lx.match("&&")) {
            skipPrimary(lx);
        }
    }

    private void skipPrimary(Lexer lx) {
        if (lx.match("+") || lx.match("-") || lx.match("!") || lx.matchKeyword("тугел")) {
            skipPrimary(lx);
            return;
        }
        if (lx.peekString() || lx.peekNumber()) {
            lx.advance();
            return;
        }
        if (lx.peekKeyword("дорес") || lx.peekKeyword("ялган")) {
            lx.advance();
            return;
        }
        if (lx.match("[")) {
            if (!lx.peek("]")) {
                skipExpression(lx);
                while (lx.match(",")) {
                    skipExpression(lx);
                }
            }
            lx.expect("]");
            return;
        }
        if (lx.match("(")) {
            skipExpression(lx);
            lx.expect(")");
            return;
        }
        if (lx.peekKeyword("озынлык")) {
            lx.advance();
            lx.expect("(");
            skipExpression(lx);
            lx.expect(")");
            return;
        }
        if (lx.peekIdent()) {
            lx.advance();
            if (lx.match("(")) {
                if (!lx.peek(")")) {
                    skipExpression(lx);
                    while (lx.match(",")) {
                        skipExpression(lx);
                    }
                }
                lx.expect(")");
                return;
            }
            if (lx.match("[")) {
                skipExpression(lx);
                lx.expect("]");
            }
            return;
        }
        throw new ParseException("bad expression near: " + lx.peekRaw());
    }

    private static String stripLineComments(String source) {
        StringBuilder sb = new StringBuilder();
        for (String line : source.split("\n", -1)) {
            int hash = -1;
            boolean inStr = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    inStr = !inStr;
                }
                if (!inStr && c == '#') {
                    hash = i;
                    break;
                }
            }
            sb.append(hash >= 0 ? line.substring(0, hash) : line).append('\n');
        }
        return sb.toString();
    }

    static final class ParseException extends RuntimeException {
        ParseException(String message) {
            super(message);
        }
    }

    static final class Lexer {
        private final List<Tok> tokens = new ArrayList<>();
        private int i;

        Lexer(String source) {
            tokenize(source);
        }

        private void tokenize(String s) {
            int n = s.length();
            int p = 0;
            while (p < n) {
                char c = s.charAt(p);
                if (Character.isWhitespace(c)) {
                    p++;
                    continue;
                }
                if (c == '"' ) {
                    int start = ++p;
                    StringBuilder sb = new StringBuilder();
                    while (p < n) {
                        char ch = s.charAt(p);
                        if (ch == '\\' && p + 1 < n) {
                            sb.append(s.charAt(p + 1));
                            p += 2;
                            continue;
                        }
                        if (ch == '"') {
                            break;
                        }
                        sb.append(ch);
                        p++;
                    }
                    if (p >= n || s.charAt(p) != '"') {
                        throw new ParseException("unterminated string");
                    }
                    p++;
                    tokens.add(Tok.str(sb.toString()));
                    continue;
                }
                if (Character.isDigit(c)) {
                    int start = p;
                    while (p < n && Character.isDigit(s.charAt(p))) {
                        p++;
                    }
                    tokens.add(Tok.num(s.substring(start, p)));
                    continue;
                }
                if (isIdentStart(c)) {
                    int start = p;
                    p++;
                    while (p < n && isIdentPart(s.charAt(p))) {
                        p++;
                    }
                    String word = s.substring(start, p);
                    if (KEYWORDS.contains(word)) {
                        tokens.add(Tok.kw(word));
                    } else {
                        tokens.add(Tok.id(word));
                    }
                    continue;
                }
                // multi-char punct
                if (p + 1 < n) {
                    String two = s.substring(p, p + 2);
                    if (Set.of("::", "<-", "==", "!=", "<=", ">=", "||", "&&").contains(two)) {
                        tokens.add(Tok.sym(two));
                        p += 2;
                        continue;
                    }
                }
                tokens.add(Tok.sym(String.valueOf(c)));
                p++;
            }
            tokens.add(Tok.eof());
        }

        private static boolean isIdentStart(char c) {
            return Character.isLetter(c) || c == '_' || Character.UnicodeScript.of(c) == Character.UnicodeScript.CYRILLIC;
        }

        private static boolean isIdentPart(char c) {
            return isIdentStart(c) || Character.isDigit(c);
        }

        boolean peekEof() {
            return tokens.get(i).type == Type.EOF;
        }

        String peekRaw() {
            return tokens.get(i).text;
        }

        boolean peek(String sym) {
            Tok t = tokens.get(i);
            return t.type == Type.SYM && t.text.equals(sym);
        }

        boolean peekString() {
            return tokens.get(i).type == Type.STR;
        }

        boolean peekNumber() {
            return tokens.get(i).type == Type.NUM;
        }

        boolean peekIdent() {
            return tokens.get(i).type == Type.ID;
        }

        boolean peekKeyword(String kw) {
            Tok t = tokens.get(i);
            return t.type == Type.KW && t.text.equals(kw);
        }

        boolean peekAheadKeyword(int offset, String kw) {
            int j = i + offset;
            if (j >= tokens.size()) {
                return false;
            }
            Tok t = tokens.get(j);
            return t.type == Type.KW && t.text.equals(kw);
        }

        void advance() {
            if (!peekEof()) {
                i++;
            }
        }

        boolean match(String sym) {
            if (peek(sym)) {
                i++;
                return true;
            }
            return false;
        }

        boolean matchKeyword(String kw) {
            if (peekKeyword(kw)) {
                i++;
                return true;
            }
            return false;
        }

        void expect(String sym) {
            if (!match(sym)) {
                throw new ParseException("expected '" + sym + "', got '" + peekRaw() + "'");
            }
        }

        void expectKeyword(String kw) {
            if (!matchKeyword(kw)) {
                throw new ParseException("expected keyword '" + kw + "', got '" + peekRaw() + "'");
            }
        }

        String expectIdent() {
            if (!peekIdent()) {
                throw new ParseException("expected identifier, got '" + peekRaw() + "'");
            }
            String v = tokens.get(i).text;
            i++;
            return v;
        }

        String expectString() {
            if (!peekString()) {
                throw new ParseException("expected string, got '" + peekRaw() + "'");
            }
            String v = tokens.get(i).text;
            i++;
            return v;
        }

        String expectNumber() {
            if (!peekNumber()) {
                throw new ParseException("expected number, got '" + peekRaw() + "'");
            }
            String v = tokens.get(i).text;
            i++;
            return v;
        }

        void expectEof() {
            if (!peekEof()) {
                throw new ParseException("expected end of file, got '" + peekRaw() + "'");
            }
        }
    }

    enum Type { KW, ID, STR, NUM, SYM, EOF }

    record Tok(Type type, String text) {
        static Tok kw(String t) { return new Tok(Type.KW, t); }
        static Tok id(String t) { return new Tok(Type.ID, t); }
        static Tok str(String t) { return new Tok(Type.STR, t); }
        static Tok num(String t) { return new Tok(Type.NUM, t); }
        static Tok sym(String t) { return new Tok(Type.SYM, t); }
        static Tok eof() { return new Tok(Type.EOF, ""); }
    }
}
