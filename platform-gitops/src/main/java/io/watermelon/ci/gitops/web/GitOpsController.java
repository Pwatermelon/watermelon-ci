package io.watermelon.ci.gitops.web;

import io.watermelon.ci.domain.gitops.GitOpsApplication;
import io.watermelon.ci.gitops.service.GitOpsDeployService;
import io.watermelon.ci.manifest.model.DeployRuntime;
import io.watermelon.ci.manifest.model.DeploySpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/gitops")
public class GitOpsController {

    private final GitOpsDeployService gitOpsDeployService;

    public GitOpsController(GitOpsDeployService gitOpsDeployService) {
        this.gitOpsDeployService = gitOpsDeployService;
    }

    public record DeployRequest(
            @NotBlank String environment,
            String release,
            String image,
            String namespace,
            Integer replicas,
            List<String> ports,
            List<String> secrets) {}

    @PostMapping("/deploy")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GitOpsApplication deploy(@PathVariable UUID projectId, @Valid @RequestBody DeployRequest request) {
        DeploySpec spec = new DeploySpec();
        spec.setRuntime(DeployRuntime.ARGOCD);
        spec.setRelease(request.release());
        spec.setImage(request.image());
        spec.setNamespace(request.namespace());
        spec.setReplicas(request.replicas() != null ? request.replicas() : 1);
        spec.setPorts(request.ports());
        spec.setSecrets(request.secrets());
        return gitOpsDeployService.deploy(
                projectId,
                request.environment(),
                spec,
                request.image(),
                request.replicas() != null ? request.replicas() : 1);
    }

    @GetMapping("/applications")
    public List<GitOpsApplication> list(@PathVariable UUID projectId) {
        return gitOpsDeployService.list(projectId);
    }

    @PostMapping("/applications/{appId}/sync")
    public void sync(@PathVariable UUID projectId, @PathVariable UUID appId) {
        gitOpsDeployService.sync(projectId, appId);
    }

    @PostMapping("/applications/{appId}/refresh")
    public GitOpsApplication refresh(@PathVariable UUID projectId, @PathVariable UUID appId) {
        return gitOpsDeployService.refreshStatus(projectId, appId);
    }
}
