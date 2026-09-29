package io.watermelon.ci.orchestration.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KarbyzRuntimeService {

    private final Path karbyzHome;
    private final String pythonBin;

    public KarbyzRuntimeService(
            @Value("${watermelon.karbyz.home:/opt/karbyz}") String karbyzHome,
            @Value("${watermelon.karbyz.python:python3}") String pythonBin) {
        this.karbyzHome = Path.of(karbyzHome);
        this.pythonBin = pythonBin;
    }

    public Map<String, Object> run(String source, String mode, String stdin) {
        if (source == null || source.isBlank()) {
            return fail("пустой исходник");
        }
        if (source.length() > 80_000) {
            return fail("исходник слишком большой (лимит 80KB)");
        }
        Path work = null;
        try {
            work = Files.createTempDirectory("karbyz-ide-");
            Path file = work.resolve("program.kbz");
            Files.writeString(file, source, StandardCharsets.UTF_8);

            List<String> cmd = new ArrayList<>();
            cmd.add(pythonBin);
            Path runner = karbyzHome.resolve("run.py");
            if (!Files.isRegularFile(runner)) {
                // локальный dev: karbyz рядом с CWD / из репозитория
                Path local = Path.of("karbyz", "run.py");
                if (Files.isRegularFile(local)) {
                    runner = local.toAbsolutePath();
                }
            }
            cmd.add(runner.toString());
            cmd.add(file.toAbsolutePath().toString());
            if ("as-yaml".equalsIgnoreCase(mode) || "yaml".equalsIgnoreCase(mode)) {
                cmd.add("--as-yaml");
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Path runHome = runner.getParent();
            if (runHome != null) {
                pb.directory(runHome.toFile());
            }
            Process p = pb.start();
            if (stdin != null && !stdin.isBlank()) {
                p.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
                if (!stdin.endsWith("\n")) {
                    p.getOutputStream().write('\n');
                }
                p.getOutputStream().flush();
            }
            p.getOutputStream().close();

            boolean finished = p.waitFor(8, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return fail("таймаут исполнения (8s)");
            }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", p.exitValue() == 0);
            res.put("exitCode", p.exitValue());
            res.put("stdout", out);
            res.put("mode", mode == null ? "run" : mode);
            res.put("durationHint", Duration.ofSeconds(8).toString());
            return res;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            return fail("не удалось запустить Карбыз: " + ex.getMessage()
                    + " (проверьте watermelon.karbyz.home=" + karbyzHome + ")");
        } finally {
            if (work != null) {
                try {
                    Files.walk(work)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static Map<String, Object> fail(String message) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", false);
        res.put("exitCode", 1);
        res.put("stdout", message);
        return res;
    }
}
