package io.watermelon.ci.orchestration.service;

import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.common.util.Slugs;
import io.watermelon.ci.domain.artifact.ArtifactKind;
import io.watermelon.ci.domain.fleet.ClusterKind;
import io.watermelon.ci.domain.fleet.FleetCluster;
import io.watermelon.ci.domain.fleet.FleetClusterRepository;
import io.watermelon.ci.domain.fleet.NodeRole;
import io.watermelon.ci.domain.fleet.NodeStatus;
import io.watermelon.ci.domain.fleet.WorkerNode;
import io.watermelon.ci.domain.fleet.WorkerNodeRepository;
import io.watermelon.ci.domain.issue.Issue;
import io.watermelon.ci.domain.issue.IssueRepository;
import io.watermelon.ci.domain.issue.IssueStatus;
import io.watermelon.ci.domain.organization.Organization;
import io.watermelon.ci.domain.organization.OrganizationRepository;
import io.watermelon.ci.domain.pipeline.PipelineRun;
import io.watermelon.ci.domain.pipeline.PipelineRunRepository;
import io.watermelon.ci.domain.pipeline.PipelineStatus;
import io.watermelon.ci.domain.project.Project;
import io.watermelon.ci.domain.project.ProjectRepository;
import io.watermelon.ci.manifest.template.ManifestTemplates;
import io.watermelon.ci.orchestration.config.DemoProperties;
import io.watermelon.ci.registry.service.ArtifactCatalogService;
import io.watermelon.ci.secrets.service.ProjectSecretService;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(DemoBootstrapService.class);

    private final DemoProperties demoProperties;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final FleetClusterRepository fleetClusterRepository;
    private final WorkerNodeRepository workerNodeRepository;
    private final ArtifactCatalogService artifactCatalogService;
    private final ProjectSecretService projectSecretService;
    private final Clock clock = Clock.system();

    private volatile UUID demoOrgId;
    private volatile UUID demoProjectId;

    public DemoBootstrapService(
            DemoProperties demoProperties,
            OrganizationRepository organizationRepository,
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            PipelineRunRepository pipelineRunRepository,
            FleetClusterRepository fleetClusterRepository,
            WorkerNodeRepository workerNodeRepository,
            ArtifactCatalogService artifactCatalogService,
            ProjectSecretService projectSecretService) {
        this.demoProperties = demoProperties;
        this.organizationRepository = organizationRepository;
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.fleetClusterRepository = fleetClusterRepository;
        this.workerNodeRepository = workerNodeRepository;
        this.artifactCatalogService = artifactCatalogService;
        this.projectSecretService = projectSecretService;
    }

    @PostConstruct
    public void onStart() {
        if (demoProperties.isEnabled() && demoProperties.isSeedOnStartup()) {
            try {
                seed();
            } catch (Exception ex) {
                log.warn("Demo seed skipped: {}", ex.getMessage());
            }
        }
    }

    @Transactional
    public Map<String, Object> seed() {
        Organization org = organizationRepository.findBySlug("watermelon-demo").orElseGet(() ->
                organizationRepository.save(new Organization(
                        Ids.newId(), "watermelon-demo", "Watermelon Demo", clock.now())));
        demoOrgId = org.getId();

        Project project = projectRepository
                .findByOrganizationIdAndSlug(org.getId(), "storefront")
                .orElseGet(() -> projectRepository.save(new Project(
                        Ids.newId(),
                        org.getId(),
                        "storefront",
                        "Storefront API",
                        "Готовый демо-проект: Git → Maven build → registry → Vault secrets → ArgoCD deploy. Манифесты: watermelon-ci.yml и .kbz",
                        clock.now())));
        demoProjectId = project.getId();

        if (issueRepository.findByProjectIdOrderByNumberDesc(project.getId()).isEmpty()) {
            issueRepository.save(new Issue(
                    Ids.newId(), project.getId(), 1, "Wire CI manifest for staging",
                    "Use argocd runtime and Vault secrets", IssueStatus.IN_PROGRESS, clock.now()));
            issueRepository.save(new Issue(
                    Ids.newId(), project.getId(), 2, "Add canary deploy",
                    "Fleet console should show rollout", IssueStatus.OPEN, clock.now()));
        }

        if (pipelineRunRepository.findByProjectIdOrderByNumberDesc(project.getId()).isEmpty()) {
            PipelineRun run = new PipelineRun(
                    Ids.newId(),
                    project.getId(),
                    1,
                    "main",
                    "abc1234",
                    PipelineStatus.SUCCESS,
                    ManifestTemplates.STOREFRONT,
                    clock.now());
            run.markCompiled("// demo compiled jenkinsfile\npipeline { agent any; stages { stage('demo') { steps { echo 'ok' } } } }",
                    "watermelon-demo-storefront-argocd-app");
            run.markRunning(42);
            run.markFinished(PipelineStatus.SUCCESS, clock.now());
            pipelineRunRepository.save(run);
        }

        if (artifactCatalogService.listProjectArtifacts(project.getId()).isEmpty()) {
            artifactCatalogService.register(
                    project.getId(),
                    null,
                    ArtifactKind.DOCKER_IMAGE,
                    "storefront",
                    "1.0.0",
                    "registry.local/watermelon/storefront:1.0.0",
                    "sha256:demodigest");
        }

        try {
            projectSecretService.upsertBulk(project.getId(), "production", Map.of(
                    "DATABASE_URL", "postgres://store:secret@db:5432/store",
                    "API_TOKEN", "wm_demo_token_change_me",
                    "REGISTRY_USER", "demo",
                    "REGISTRY_PASSWORD", "demo-registry-pass"));
        } catch (Exception ex) {
            log.info("Vault unavailable during seed, secrets metadata may be partial: {}", ex.getMessage());
        }

        FleetCluster cluster = fleetClusterRepository
                .findByOrganizationIdAndSlug(org.getId(), "demo-fleet")
                .orElseGet(() -> fleetClusterRepository.save(new FleetCluster(
                        Ids.newId(),
                        org.getId(),
                        "demo-fleet",
                        "Demo Fleet",
                        ClusterKind.BARE_DOCKER,
                        "Local demo cluster for MVP walkthrough",
                        clock.now())));
        if (workerNodeRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()).isEmpty()) {
            WorkerNode node = new WorkerNode(
                    Ids.newId(),
                    cluster.getId(),
                    "worker-1",
                    NodeRole.WORKER,
                    NodeStatus.ONLINE,
                    "http://127.0.0.1:2375",
                    sha256("demo-join"),
                    clock.now());
            node.markOnline("demo-host", "arm64", 8, 16L * 1024 * 1024 * 1024, "0.1.0-demo", clock.now());
            workerNodeRepository.save(node);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("organizationId", org.getId());
        out.put("organizationSlug", org.getSlug());
        out.put("projectId", project.getId());
        out.put("projectSlug", project.getSlug());
        out.put("clusterId", cluster.getId());
        out.put("message", "Demo workspace ready");
        log.info("Demo workspace seeded: org={} project={}", org.getSlug(), project.getSlug());
        return out;
    }

    public UUID getDemoOrgId() { return demoOrgId; }
    public UUID getDemoProjectId() { return demoProjectId; }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Slugs.of(value);
        }
    }
}
