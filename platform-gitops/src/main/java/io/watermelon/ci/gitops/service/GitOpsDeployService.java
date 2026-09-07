package io.watermelon.ci.gitops.service;

import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.domain.gitops.GitOpsApplication;
import io.watermelon.ci.domain.gitops.GitOpsApplicationRepository;
import io.watermelon.ci.domain.project.Project;
import io.watermelon.ci.domain.project.ProjectRepository;
import io.watermelon.ci.gitops.argocd.ArgoCdClient;
import io.watermelon.ci.gitops.config.GitOpsProperties;
import io.watermelon.ci.gitops.render.SimpleWorkloadRenderer;
import io.watermelon.ci.gitops.render.SimpleWorkloadRenderer.WorkloadSpec;
import io.watermelon.ci.manifest.model.DeploySpec;
import io.watermelon.ci.secrets.service.ProjectSecretService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GitOps deploy facade: Watermelon deploy block → rendered K8s YAML → ArgoCD Application.
 * DevOps do not maintain Helm charts for standard apps.
 */
@Service
public class GitOpsDeployService {

    private static final Logger log = LoggerFactory.getLogger(GitOpsDeployService.class);

    private final ProjectRepository projectRepository;
    private final GitOpsApplicationRepository applicationRepository;
    private final SimpleWorkloadRenderer renderer;
    private final ArgoCdClient argoCdClient;
    private final ProjectSecretService projectSecretService;
    private final GitOpsProperties properties;
    private final Clock clock = Clock.system();

    public GitOpsDeployService(
            ProjectRepository projectRepository,
            GitOpsApplicationRepository applicationRepository,
            SimpleWorkloadRenderer renderer,
            ArgoCdClient argoCdClient,
            ProjectSecretService projectSecretService,
            GitOpsProperties properties) {
        this.projectRepository = projectRepository;
        this.applicationRepository = applicationRepository;
        this.renderer = renderer;
        this.argoCdClient = argoCdClient;
        this.projectSecretService = projectSecretService;
        this.properties = properties;
    }

    @Transactional
    public GitOpsApplication deploy(
            UUID projectId,
            String environment,
            DeploySpec deploy,
            String imageOverride,
            int replicas) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("project not found"));

        String release = deploy.getRelease() != null && !deploy.getRelease().isBlank()
                ? SimpleWorkloadRenderer.sanitizeName(deploy.getRelease())
                : SimpleWorkloadRenderer.sanitizeName(project.getSlug() + "-" + environment);
        String namespace = deploy.getNamespace() != null && !deploy.getNamespace().isBlank()
                ? deploy.getNamespace()
                : release;
        String image = imageOverride != null && !imageOverride.isBlank()
                ? imageOverride
                : (deploy.getImage() != null ? deploy.getImage() : "registry.local/watermelon/" + project.getSlug() + ":latest");
        List<String> secretKeys = deploy.getSecrets() != null ? deploy.getSecrets() : List.of();
        String vaultPath = projectSecretService.vaultPath(projectId, environment);

        // Ensure secret keys exist in Vault metadata (values must be set via secrets API beforehand)
        Map<String, String> available = projectSecretService.readEnvironmentValues(projectId, environment);
        for (String key : secretKeys) {
            if (!available.containsKey(key)) {
                log.warn("Secret key {} missing in Vault for project {} env {} — placeholder will be rendered",
                        key, projectId, environment);
            }
        }

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("app", release);
        labels.put("watermelon.ci/project", project.getSlug());
        labels.put("watermelon.ci/environment", environment);

        String workloadYaml = renderer.render(new WorkloadSpec(
                release,
                namespace,
                image,
                replicas > 0 ? replicas : Math.max(1, deploy.getReplicas()),
                SimpleWorkloadRenderer.containerPortsOnly(deploy.getPorts()),
                secretKeys,
                vaultPath,
                labels));

        String repoPath = "apps/" + project.getSlug() + "/" + environment + "/" + release;
        writeGitOpsFiles(repoPath, workloadYaml);

        String argoAppName = SimpleWorkloadRenderer.sanitizeName(project.getSlug() + "-" + environment + "-" + release);
        String argoAppYaml = renderer.renderArgoApplication(
                argoAppName,
                properties.getArgoCd().getProject(),
                properties.getGitopsRepoUrl(),
                repoPath,
                namespace,
                resolveClusterServer());

        argoCdClient.upsertApplication(argoAppName, argoAppYaml);
        argoCdClient.sync(argoAppName);
        Map<String, String> status = argoCdClient.status(argoAppName);

        GitOpsApplication app = applicationRepository
                .findByProjectIdAndEnvironmentAndReleaseName(projectId, environment, release)
                .orElseGet(() -> new GitOpsApplication(
                        Ids.newId(),
                        projectId,
                        environment,
                        release,
                        argoAppName,
                        namespace,
                        repoPath,
                        image,
                        clock.now()));
        app.markRendered(image, workloadYaml + "\n---\n" + argoAppYaml, clock.now());
        app.markSync(status.getOrDefault("sync", "Unknown"), status.getOrDefault("health", "Unknown"), clock.now());
        return applicationRepository.save(app);
    }

    @Transactional(readOnly = true)
    public List<GitOpsApplication> list(UUID projectId) {
        return applicationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
    }

    @Transactional
    public GitOpsApplication refreshStatus(UUID projectId, UUID appId) {
        GitOpsApplication app = applicationRepository
                .findById(appId)
                .orElseThrow(() -> new NotFoundException("gitops application not found"));
        if (!app.getProjectId().equals(projectId)) {
            throw new NotFoundException("gitops application not found");
        }
        Map<String, String> status = argoCdClient.status(app.getArgoAppName());
        app.markSync(status.getOrDefault("sync", "Unknown"), status.getOrDefault("health", "Unknown"), clock.now());
        return applicationRepository.save(app);
    }

    @Transactional
    public void sync(UUID projectId, UUID appId) {
        GitOpsApplication app = refreshStatus(projectId, appId);
        argoCdClient.sync(app.getArgoAppName());
        Map<String, String> status = argoCdClient.status(app.getArgoAppName());
        app.markSync(status.getOrDefault("sync", "Unknown"), status.getOrDefault("health", "Unknown"), clock.now());
        applicationRepository.save(app);
    }

    private void writeGitOpsFiles(String repoPath, String workloadYaml) {
        try {
            Path root = Path.of(properties.getGitopsRepoPath()).resolve(repoPath);
            Files.createDirectories(root);
            Files.writeString(root.resolve("workload.yaml"), workloadYaml);
            Files.writeString(root.resolve("kustomization.yaml"), """
                    apiVersion: kustomize.config.k8s.io/v1beta1
                    kind: Kustomization
                    resources:
                      - workload.yaml
                    """);
            log.info("Wrote GitOps bundle to {}", root.toAbsolutePath());
        } catch (Exception ex) {
            throw new io.watermelon.ci.common.error.PlatformException(
                    io.watermelon.ci.common.error.ErrorCode.GITOPS_ERROR,
                    "failed to write gitops files: " + ex.getMessage(),
                    ex);
        }
    }

    private String resolveClusterServer() {
        if ("in-cluster".equals(properties.getDefaultCluster())) {
            return "https://kubernetes.default.svc";
        }
        return properties.getDefaultCluster();
    }
}
