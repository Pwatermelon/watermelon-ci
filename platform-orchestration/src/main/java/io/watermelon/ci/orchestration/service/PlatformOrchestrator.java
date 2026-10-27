package io.watermelon.ci.orchestration.service;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.common.util.Slugs;
import io.watermelon.ci.domain.organization.Organization;
import io.watermelon.ci.domain.organization.OrganizationRepository;
import io.watermelon.ci.domain.pipeline.PipelineRun;
import io.watermelon.ci.domain.pipeline.PipelineRunRepository;
import io.watermelon.ci.domain.pipeline.PipelineStatus;
import io.watermelon.ci.domain.project.Project;
import io.watermelon.ci.domain.project.ProjectRepository;
import io.watermelon.ci.jenkins.client.JenkinsClient;
import io.watermelon.ci.jenkins.client.JenkinsClient.BuildInfo;
import io.watermelon.ci.jenkins.client.JenkinsClient.StageInfo;
import io.watermelon.ci.manifest.ManifestService;
import io.watermelon.ci.manifest.compile.CompileContext;
import io.watermelon.ci.manifest.compile.CompiledPipeline;
import io.watermelon.ci.registry.service.ArtifactCatalogService;
import io.watermelon.ci.runtime.model.DeploymentView;
import io.watermelon.ci.runtime.service.DeploymentTrackingService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PlatformOrchestrator.class);
    private static final Pattern STAGE_YAML = Pattern.compile("(?m)^\\s*-\\s*([A-Za-z0-9._-]+)\\s*$");
    private static final Pattern STAGE_KARBYZ = Pattern.compile("этап\\s+\"([^\"]+)\"");
    private static final Pattern STAGE_JF = Pattern.compile("stage\\('([^']+)'\\)");

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final ManifestService manifestService;
    private final JenkinsClient jenkinsClient;
    private final ArtifactCatalogService artifactCatalogService;
    private final DeploymentTrackingService deploymentTrackingService;
    private final Clock clock = Clock.system();

    public PlatformOrchestrator(
            OrganizationRepository organizationRepository,
            ProjectRepository projectRepository,
            PipelineRunRepository pipelineRunRepository,
            ManifestService manifestService,
            JenkinsClient jenkinsClient,
            ArtifactCatalogService artifactCatalogService,
            DeploymentTrackingService deploymentTrackingService) {
        this.organizationRepository = organizationRepository;
        this.projectRepository = projectRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.manifestService = manifestService;
        this.jenkinsClient = jenkinsClient;
        this.artifactCatalogService = artifactCatalogService;
        this.deploymentTrackingService = deploymentTrackingService;
    }

    @Transactional
    public Organization createOrganization(String name) {
        String slug = Slugs.of(name);
        if (organizationRepository.existsBySlug(slug)) {
            throw new PlatformException(ErrorCode.CONFLICT, "organization exists: " + slug);
        }
        return organizationRepository.save(new Organization(Ids.newId(), slug, name, clock.now()));
    }

    @Transactional
    public Project createProject(UUID organizationId, String name, String description) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new NotFoundException("organization not found"));
        String slug = Slugs.of(name);
        Project project = new Project(Ids.newId(), organizationId, slug, name, description, clock.now());
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("project not found"));
    }

    @Transactional(readOnly = true)
    public List<Organization> listOrganizations() {
        return organizationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("organization not found"));
    }

    @Transactional(readOnly = true)
    public List<Project> listProjects(UUID organizationId) {
        return projectRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public PipelineRun startPipeline(UUID projectId, String ref, String commitSha, String manifestYaml) {
        Project project = requireProject(projectId);
        Organization org = organizationRepository
                .findById(project.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("organization not found"));

        long number = pipelineRunRepository.findMaxNumber(projectId) + 1;
        PipelineRun run = new PipelineRun(
                Ids.newId(), projectId, number, ref, commitSha, PipelineStatus.QUEUED, manifestYaml, clock.now());

        try {
            CompiledPipeline compiled = manifestService.compile(
                    manifestYaml,
                    new CompileContext(
                            project.getSlug(),
                            org.getSlug(),
                            artifactCatalogService.dockerPushHost(),
                            artifactCatalogService.mavenRepositoryUrl(),
                            run.getId().toString(),
                            "watermelon-ci",
                            project.getId().toString(),
                            "http://watermelon-ci:8088"));

            run.markCompiled(compiled.jenkinsfile(), compiled.jobName());
            pipelineRunRepository.save(run);

            jenkinsClient.upsertPipelineJob(compiled.jobName(), compiled.jenkinsfile());
            int buildNumber = jenkinsClient.triggerBuild(compiled.jobName());
            run.markRunning(buildNumber);
            return pipelineRunRepository.save(run);
        } catch (RuntimeException ex) {
            run.markFinished(PipelineStatus.FAILED, clock.now());
            pipelineRunRepository.save(run);
            throw ex;
        }
    }

    /** Re-run the same manifest as a new pipeline number. */
    @Transactional
    public PipelineRun restartPipeline(UUID projectId, long number) {
        PipelineRun previous = getPipeline(projectId, number);
        return startPipeline(projectId, previous.getRef(), previous.getCommitSha(), previous.getManifestYaml());
    }

    @Transactional
    public PipelineRun syncPipeline(UUID projectId, long number) {
        PipelineRun run = getPipeline(projectId, number);
        if (run.getJenkinsJobName() == null || run.getJenkinsBuildNumber() == null) {
            return run;
        }
        BuildInfo info = jenkinsClient.getBuild(run.getJenkinsJobName(), run.getJenkinsBuildNumber());
        if (info == null) {
            // legacy off-by-one: we used to store nextBuildNumber after trigger
            if (run.getJenkinsBuildNumber() > 1) {
                info = jenkinsClient.getBuild(run.getJenkinsJobName(), run.getJenkinsBuildNumber() - 1);
            }
            if (info == null) {
                // ghost RUNNING: job never created / build never appeared
                if (run.getCreatedAt().isBefore(clock.now().minusSeconds(90))) {
                    run.markFinished(PipelineStatus.FAILED, clock.now());
                    return pipelineRunRepository.save(run);
                }
                return run;
            }
            run.markRunning(info.number());
        }
        String mapped = info.mappedStatus();
        if ("RUNNING".equals(mapped) || "QUEUED".equals(mapped)) {
            run.markRunning(info.number());
        } else if ("SUCCESS".equals(mapped)) {
            run.markFinished(PipelineStatus.SUCCESS, clock.now());
        } else if ("CANCELLED".equals(mapped) || "CANCELED".equals(mapped)) {
            run.markFinished(PipelineStatus.CANCELED, clock.now());
        } else {
            run.markFinished(PipelineStatus.FAILED, clock.now());
        }
        return pipelineRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public PipelineDetail getPipelineDetail(UUID projectId, long number) {
        PipelineRun run = getPipeline(projectId, number);
        List<StageView> stages = List.of();
        String console = null;
        String jenkinsUrl = null;
        if (run.getJenkinsJobName() != null) {
            jenkinsUrl = jenkinsClient.jobUrl(run.getJenkinsJobName());
            if (run.getJenkinsBuildNumber() != null) {
                int bn = run.getJenkinsBuildNumber();
                List<StageInfo> live = jenkinsClient.getStages(run.getJenkinsJobName(), bn);
                if (live.isEmpty() && bn > 1) {
                    live = jenkinsClient.getStages(run.getJenkinsJobName(), bn - 1);
                    bn = live.isEmpty() ? bn : bn - 1;
                }
                if (!live.isEmpty()) {
                    stages = live.stream()
                            .map(s -> new StageView(s.name(), s.status(), s.durationMs()))
                            .toList();
                }
                console = jenkinsClient.getConsoleText(run.getJenkinsJobName(), bn);
                jenkinsUrl = jenkinsClient.buildUrl(run.getJenkinsJobName(), bn);
            }
        }
        if (stages.isEmpty()) {
            stages = fallbackStages(run);
        }
        return new PipelineDetail(run, stages, console, jenkinsUrl);
    }

    @Scheduled(fixedDelayString = "8000")
    @Transactional
    public void syncRunningPipelines() {
        List<PipelineRun> running = pipelineRunRepository.findAll().stream()
                .filter(r -> r.getStatus() == PipelineStatus.RUNNING || r.getStatus() == PipelineStatus.QUEUED)
                .toList();
        for (PipelineRun run : running) {
            try {
                if (run.getJenkinsJobName() == null || run.getJenkinsBuildNumber() == null) {
                    // stuck without jenkins binding — fail old seed-ish ghosts after soft age
                    if (run.getCreatedAt().isBefore(clock.now().minusSeconds(120))) {
                        run.markFinished(PipelineStatus.FAILED, clock.now());
                        pipelineRunRepository.save(run);
                    }
                    continue;
                }
                syncPipeline(run.getProjectId(), run.getNumber());
            } catch (Exception ex) {
                log.debug("sync pipeline #{}: {}", run.getNumber(), ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PipelineRun> listPipelines(UUID projectId) {
        return pipelineRunRepository.findByProjectIdOrderByNumberDesc(projectId);
    }

    @Transactional(readOnly = true)
    public PipelineRun getPipeline(UUID projectId, long number) {
        return pipelineRunRepository
                .findByProjectIdAndNumber(projectId, number)
                .orElseThrow(() -> new NotFoundException("pipeline #" + number + " not found"));
    }

    @Transactional(readOnly = true)
    public List<DeploymentView> listDeployments(UUID projectId) {
        return deploymentTrackingService.listByProject(projectId);
    }

    @Transactional(readOnly = true)
    public String previewJenkinsfile(UUID projectId, String manifestYaml) {
        Project project = requireProject(projectId);
        Organization org = organizationRepository
                .findById(project.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("organization not found"));
        return manifestService
                .compile(
                        manifestYaml,
                        new CompileContext(
                                project.getSlug(),
                                org.getSlug(),
                                artifactCatalogService.dockerPushHost(),
                                artifactCatalogService.mavenRepositoryUrl(),
                                "preview",
                                "watermelon-ci",
                                project.getId().toString(),
                                "http://localhost:8088"))
                .jenkinsfile();
    }

    private List<StageView> fallbackStages(PipelineRun run) {
        List<String> names = extractStageNames(run);
        if (names.isEmpty()) {
            return List.of();
        }
        String overall = run.getStatus().name();
        List<StageView> out = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String st;
            if ("SUCCESS".equals(overall)) {
                st = "SUCCESS";
            } else if ("FAILED".equals(overall) || "CANCELED".equals(overall)) {
                st = i == names.size() - 1 ? overall : "SUCCESS";
            } else if ("RUNNING".equals(overall) || "QUEUED".equals(overall)) {
                st = i == 0 ? "RUNNING" : "PENDING";
            } else {
                st = "PENDING";
            }
            out.add(new StageView(names.get(i), st, 0));
        }
        return out;
    }

    private List<String> extractStageNames(PipelineRun run) {
        Set<String> names = new LinkedHashSet<>();
        String src = run.getCompiledJenkinsfile() != null ? run.getCompiledJenkinsfile() : "";
        Matcher jf = STAGE_JF.matcher(src);
        while (jf.find()) {
            String n = jf.group(1);
            if (!"publish".equals(n) || names.isEmpty()) {
                names.add(n);
            } else {
                names.add(n);
            }
        }
        if (names.isEmpty() && run.getManifestYaml() != null) {
            String m = run.getManifestYaml();
            if (m.contains("этап")) {
                Matcher k = STAGE_KARBYZ.matcher(m);
                while (k.find()) {
                    names.add(k.group(1));
                }
            } else if (m.contains("stages:")) {
                int idx = m.indexOf("stages:");
                int end = m.indexOf("jobs:", idx);
                String block = end > idx ? m.substring(idx, end) : m.substring(idx);
                Matcher y = STAGE_YAML.matcher(block);
                while (y.find()) {
                    names.add(y.group(1));
                }
            }
        }
        return new ArrayList<>(names);
    }

    public record StageView(String name, String status, long durationMs) {}

    public record PipelineDetail(
            PipelineRun run, List<StageView> stages, String consoleText, String jenkinsUrl) {}
}
