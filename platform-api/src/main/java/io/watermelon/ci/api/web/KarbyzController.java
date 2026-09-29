package io.watermelon.ci.api.web;

import io.watermelon.ci.manifest.ManifestService;
import io.watermelon.ci.manifest.compile.CompileContext;
import io.watermelon.ci.orchestration.service.KarbyzRuntimeService;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/karbyz")
public class KarbyzController {

    private final KarbyzRuntimeService runtime;
    private final ManifestService manifestService;

    public KarbyzController(KarbyzRuntimeService runtime, ManifestService manifestService) {
        this.runtime = runtime;
        this.manifestService = manifestService;
    }

    public record RunRequest(@NotBlank String source, String mode, String stdin) {}

    public record CompileRequest(@NotBlank String source) {}

    @PostMapping("/run")
    public Map<String, Object> run(@RequestBody RunRequest request) {
        String mode = request.mode() == null ? "run" : request.mode();
        return runtime.run(request.source(), mode, request.stdin());
    }

    @PostMapping("/compile")
    public Map<String, Object> compile(@RequestBody CompileRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            var compiled = manifestService.compile(
                    request.source(),
                    new CompileContext(
                            "karbyz-ide",
                            "demo",
                            "registry.local/watermelon",
                            "http://nexus.local/repository/maven-releases/",
                            "karbyz-ide-run",
                            "wm-creds"));
            out.put("ok", true);
            out.put("jobName", compiled.jobName());
            out.put("jenkinsfile", compiled.jenkinsfile());
        } catch (Exception ex) {
            out.put("ok", false);
            out.put("error", ex.getMessage());
            out.put("jenkinsfile", "");
        }
        return out;
    }
}
