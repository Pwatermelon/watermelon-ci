package io.watermelon.ci.orchestration.service;

import io.watermelon.ci.common.error.NotFoundException;
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
import io.watermelon.ci.manifest.ManifestService;
import io.watermelon.ci.manifest.compile.CompileContext;
import io.watermelon.ci.manifest.compile.CompiledPipeline;
import io.watermelon.ci.registry.service.ArtifactCatalogService;
import io.watermelon.ci.runtime.model.DeploymentView;
import io.watermelon.ci.runtime.service.DeploymentTrackingService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformOrchestrator {

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
            throw new io.watermelon.ci.common.error.PlatformException(
                    io.watermelon.ci.common.error.ErrorCode.CONFLICT, "organization exists: " + slug);
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
}
