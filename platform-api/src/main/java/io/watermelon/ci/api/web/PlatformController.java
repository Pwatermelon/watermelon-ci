package io.watermelon.ci.api.web;

import static io.watermelon.ci.api.dto.ApiDtos.CreateOrganizationRequest;
import static io.watermelon.ci.api.dto.ApiDtos.CreateProjectRequest;
import static io.watermelon.ci.api.dto.ApiDtos.OrganizationResponse;
import static io.watermelon.ci.api.dto.ApiDtos.PreviewManifestRequest;
import static io.watermelon.ci.api.dto.ApiDtos.ProjectResponse;
import static io.watermelon.ci.api.dto.ApiDtos.StartPipelineRequest;

import io.watermelon.ci.domain.organization.Organization;
import io.watermelon.ci.domain.pipeline.PipelineRun;
import io.watermelon.ci.domain.project.Project;
import io.watermelon.ci.orchestration.service.PlatformOrchestrator;
import io.watermelon.ci.runtime.model.DeploymentView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1")
public class PlatformController {

    private final PlatformOrchestrator orchestrator;

    public PlatformController(PlatformOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createOrg(@Valid @RequestBody CreateOrganizationRequest request) {
        Organization org = orchestrator.createOrganization(request.name());
        return new OrganizationResponse(org.getId(), org.getSlug(), org.getName());
    }

    @PostMapping("/organizations/{organizationId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(
            @PathVariable UUID organizationId, @Valid @RequestBody CreateProjectRequest request) {
        Project project = orchestrator.createProject(organizationId, request.name(), request.description());
        return new ProjectResponse(
                project.getId(),
                project.getOrganizationId(),
                project.getSlug(),
                project.getName(),
                project.getDescription());
    }

    @PostMapping("/projects/{projectId}/pipelines")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PipelineRun startPipeline(
            @PathVariable UUID projectId, @Valid @RequestBody StartPipelineRequest request) {
        return orchestrator.startPipeline(projectId, request.ref(), request.commitSha(), request.manifestYaml());
    }

    @GetMapping("/projects/{projectId}/pipelines")
    public List<PipelineRun> listPipelines(@PathVariable UUID projectId) {
        return orchestrator.listPipelines(projectId);
    }

    @GetMapping("/projects/{projectId}/pipelines/{number}")
    public PipelineRun getPipeline(@PathVariable UUID projectId, @PathVariable long number) {
        return orchestrator.getPipeline(projectId, number);
    }

    @PostMapping("/projects/{projectId}/manifests/preview")
    public Map<String, String> preview(
            @PathVariable UUID projectId, @Valid @RequestBody PreviewManifestRequest request) {
        return Map.of("jenkinsfile", orchestrator.previewJenkinsfile(projectId, request.manifestYaml()));
    }

    @GetMapping("/projects/{projectId}/deployments")
    public List<DeploymentView> deployments(@PathVariable UUID projectId) {
        return orchestrator.listDeployments(projectId);
    }
}
