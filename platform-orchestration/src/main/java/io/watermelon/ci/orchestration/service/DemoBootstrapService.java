package io.watermelon.ci.orchestration.service;

import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.common.util.Slugs;
import io.watermelon.ci.domain.artifact.ArtifactKind;
import io.watermelon.ci.domain.fleet.ClusterKind;
import io.watermelon.ci.domain.fleet.FleetCluster;
import io.watermelon.ci.domain.fleet.FleetClusterRepository;
import io.watermelon.ci.domain.fleet.WorkerNode;
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
import io.watermelon.ci.fleet.service.FleetAdminService;
import io.watermelon.ci.git.spi.GitHostingClient;
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
    private final ArtifactCatalogService artifactCatalogService;
    private final ProjectSecretService projectSecretService;
    private final GitHostingClient gitHostingClient;
    private final FleetAdminService fleetAdminService;
    private final Clock clock = Clock.system();

    private volatile UUID demoOrgId;
    private volatile UUID demoProjectId;
    private volatile UUID paymentsProjectId;
    private volatile UUID demoClusterId;
    private volatile UUID vmANodeId;
    private volatile UUID vmBNodeId;

    public DemoBootstrapService(
            DemoProperties demoProperties,
            OrganizationRepository organizationRepository,
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            PipelineRunRepository pipelineRunRepository,
            FleetClusterRepository fleetClusterRepository,
            ArtifactCatalogService artifactCatalogService,
            ProjectSecretService projectSecretService,
            GitHostingClient gitHostingClient,
            FleetAdminService fleetAdminService) {
        this.demoProperties = demoProperties;
        this.organizationRepository = organizationRepository;
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.fleetClusterRepository = fleetClusterRepository;
        this.artifactCatalogService = artifactCatalogService;
        this.projectSecretService = projectSecretService;
        this.gitHostingClient = gitHostingClient;
        this.fleetAdminService = fleetAdminService;
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

        Project storefront = projectRepository
                .findByOrganizationIdAndSlug(org.getId(), "storefront")
                .orElseGet(() -> projectRepository.save(new Project(
                        Ids.newId(),
                        org.getId(),
                        "storefront",
                        "Storefront",
                        "Демо-сервис витрины: build → registry → deploy на demo-vm-a",
                        clock.now())));
        demoProjectId = storefront.getId();

        Project payments = projectRepository
                .findByOrganizationIdAndSlug(org.getId(), "payments")
                .orElseGet(() -> projectRepository.save(new Project(
                        Ids.newId(),
                        org.getId(),
                        "payments",
                        "Payments",
                        "Демо-сервис платежей: build → registry → deploy на demo-vm-b",
                        clock.now())));
        paymentsProjectId = payments.getId();

        seedIssues(storefront);
        seedIssues(payments);
        seedPipeline(storefront, ManifestTemplates.STOREFRONT);
        seedPipeline(payments, ManifestTemplates.PAYMENTS);

        seedArtifact(storefront, "storefront", "registry:5000/watermelon/storefront:latest");
        seedArtifact(payments, "payments", "registry:5000/watermelon/payments:latest");

        try {
            projectSecretService.upsertBulk(storefront.getId(), "staging", Map.of(
                    "DATABASE_URL", "postgres://store:secret@db:5432/store",
                    "API_TOKEN", "wm_demo_token"));
            projectSecretService.upsertBulk(payments.getId(), "staging", Map.of(
                    "STRIPE_KEY", "sk_demo_xxx",
                    "API_TOKEN", "wm_payments_token"));
        } catch (Exception ex) {
            log.info("Vault unavailable during seed: {}", ex.getMessage());
        }

        FleetCluster cluster = fleetClusterRepository
                .findByOrganizationIdAndSlug(org.getId(), "demo-fleet")
                .orElseGet(() -> fleetClusterRepository.save(new FleetCluster(
                        Ids.newId(),
                        org.getId(),
                        "demo-fleet",
                        "Demo Fleet",
                        ClusterKind.BARE_DOCKER,
                        "Две Docker-ВМ (DinD) для демо CI/CD",
                        clock.now())));
        demoClusterId = cluster.getId();

        WorkerNode vmA = fleetAdminService.upsertOnlineNode(
                cluster.getId(), "demo-vm-a", demoProperties.getVmAUrl(), "demo-vm-a", 4, 4L * 1024 * 1024 * 1024);
        WorkerNode vmB = fleetAdminService.upsertOnlineNode(
                cluster.getId(), "demo-vm-b", demoProperties.getVmBUrl(), "demo-vm-b", 4, 4L * 1024 * 1024 * 1024);
        vmANodeId = vmA.getId();
        vmBNodeId = vmB.getId();

        seedGitRepo("storefront", ManifestTemplates.STOREFRONT, ManifestTemplates.KARBYZ_STOREFRONT,
                "Storefront", "#36d47a");
        seedGitRepo("payments", ManifestTemplates.PAYMENTS, ManifestTemplates.KARBYZ_PAYMENTS,
                "Payments API", "#ff5d72");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("organizationId", org.getId());
        out.put("organizationSlug", org.getSlug());
        out.put("projectId", storefront.getId());
        out.put("projectSlug", storefront.getSlug());
        out.put("paymentsProjectId", payments.getId());
        out.put("clusterId", cluster.getId());
        out.put("vmANodeId", vmA.getId());
        out.put("vmBNodeId", vmB.getId());
        out.put("gitOwner", "watermelon-demo");
        out.put("gitRepos", java.util.List.of("storefront", "payments"));
        out.put("message", "Demo ready: 2 VMs, 2 repos, Karbyz+YAML pipelines");
        log.info("Demo seeded: org={} storefront={} payments={} vms=a,b", org.getSlug(), storefront.getSlug(), payments.getSlug());
        return out;
    }

    private void seedIssues(Project project) {
        if (!issueRepository.findByProjectIdOrderByNumberDesc(project.getId()).isEmpty()) {
            return;
        }
        issueRepository.save(new Issue(
                Ids.newId(), project.getId(), 1, "Собрать образ и запушить в registry",
                "Пайплайн build → publish", IssueStatus.IN_PROGRESS, clock.now()));
        issueRepository.save(new Issue(
                Ids.newId(), project.getId(), 2, "Раскатить на demo-vm",
                "Deploy runtime docker + host", IssueStatus.OPEN, clock.now()));
    }

    private void seedPipeline(Project project, String manifest) {
        if (!pipelineRunRepository.findByProjectIdOrderByNumberDesc(project.getId()).isEmpty()) {
            return;
        }
        PipelineRun run = new PipelineRun(
                Ids.newId(), project.getId(), 1, "main", "seed000",
                PipelineStatus.SUCCESS, manifest, clock.now());
        run.markCompiled("// seed", "seed-job");
        run.markRunning(1);
        run.markFinished(PipelineStatus.SUCCESS, clock.now());
        pipelineRunRepository.save(run);
    }

    private void seedArtifact(Project project, String name, String locator) {
        if (!artifactCatalogService.listProjectArtifacts(project.getId()).isEmpty()) {
            return;
        }
        artifactCatalogService.register(
                project.getId(), null, ArtifactKind.DOCKER_IMAGE, name, "latest", locator, null);
    }

    private void seedGitRepo(String repo, String yaml, String kbz, String title, String accent) {
        try {
            gitHostingClient.createRepository("watermelon-demo", repo, title + " demo service", false);
            gitHostingClient.writeFile("watermelon-demo", repo, "watermelon-ci.yml", yaml,
                    "seed yaml", "main", null);
            gitHostingClient.writeFile("watermelon-demo", repo, "watermelon-ci.kbz", kbz,
                    "seed kbz", "main", null);
            gitHostingClient.writeFile("watermelon-demo", repo, "Dockerfile",
                    """
                    FROM nginx:alpine
                    COPY public/ /usr/share/nginx/html/
                    EXPOSE 80
                    """,
                    "seed Dockerfile", "main", null);
            gitHostingClient.writeFile("watermelon-demo", repo, "public/index.html",
                    """
                    <!doctype html>
                    <html lang="ru"><meta charset="utf-8"/>
                    <title>%s</title>
                    <style>
                      body{margin:0;min-height:100vh;display:grid;place-items:center;
                        font-family:system-ui,sans-serif;background:#0b1913;color:#f3faf5}
                      .card{padding:2rem 2.5rem;border:1px solid #274536;border-radius:16px;
                        background:#112419;box-shadow:0 20px 50px rgba(0,0,0,.35)}
                      h1{margin:0 0 .5rem;font-size:1.6rem}
                      p{margin:0;opacity:.75}
                      .pill{display:inline-block;margin-top:1rem;padding:.35rem .7rem;border-radius:999px;
                        background:%s;color:#06110c;font-weight:700;font-size:.85rem}
                    </style>
                    <div class="card">
                      <h1>%s</h1>
                      <p>Сервис раскатан через Watermelon CI → Docker VM</p>
                      <span class="pill">healthy</span>
                    </div>
                    """.formatted(title, accent, title),
                    "seed index", "main", null);
            gitHostingClient.writeFile("watermelon-demo", repo, "README.md",
                    "# " + title + "\n\nПайплайн: `watermelon-ci.yml` / `watermelon-ci.kbz`\n",
                    "seed readme", "main", null);
        } catch (Exception ex) {
            log.info("Git seed {} skipped: {}", repo, ex.getMessage());
        }
    }

    public UUID getDemoOrgId() { return demoOrgId; }
    public UUID getDemoProjectId() { return demoProjectId; }
    public UUID getPaymentsProjectId() { return paymentsProjectId; }
    public UUID getDemoClusterId() { return demoClusterId; }
    public UUID getVmANodeId() { return vmANodeId; }
    public UUID getVmBNodeId() { return vmBNodeId; }

    @SuppressWarnings("unused")
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Slugs.of(value);
        }
    }
}
