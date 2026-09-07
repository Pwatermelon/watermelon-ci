package io.watermelon.ci.api.web;

import static io.watermelon.ci.api.dto.ApiDtos.CreateGroupRequest;
import static io.watermelon.ci.api.dto.ApiDtos.GrantMembershipRequest;
import static io.watermelon.ci.api.dto.ApiDtos.RegisterArtifactRequest;

import io.watermelon.ci.domain.artifact.ArtifactKind;
import io.watermelon.ci.domain.identity.AccessGroup;
import io.watermelon.ci.domain.identity.ProjectMembership;
import io.watermelon.ci.identity.service.AccessControlService;
import io.watermelon.ci.manifest.template.ManifestTemplates;
import io.watermelon.ci.registry.model.ArtifactView;
import io.watermelon.ci.registry.service.ArtifactCatalogService;
import io.watermelon.ci.runtime.model.DeploymentView;
import io.watermelon.ci.runtime.service.DeploymentTrackingService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final ArtifactCatalogService artifactCatalogService;
    private final DeploymentTrackingService deploymentTrackingService;
    private final AccessControlService accessControlService;
    private final ManifestTemplates manifestTemplates;

    public CatalogController(
            ArtifactCatalogService artifactCatalogService,
            DeploymentTrackingService deploymentTrackingService,
            AccessControlService accessControlService,
            ManifestTemplates manifestTemplates) {
        this.artifactCatalogService = artifactCatalogService;
        this.deploymentTrackingService = deploymentTrackingService;
        this.accessControlService = accessControlService;
        this.manifestTemplates = manifestTemplates;
    }

    @GetMapping("/projects/{projectId}/artifacts")
    public java.util.List<ArtifactView> artifacts(
            @PathVariable UUID projectId, @RequestParam(required = false) ArtifactKind kind) {
        if (kind == null) {
            return artifactCatalogService.listProjectArtifacts(projectId);
        }
        return artifactCatalogService.listByKind(projectId, kind);
    }

    @PostMapping("/projects/{projectId}/artifacts")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtifactView registerArtifact(
            @PathVariable UUID projectId, @Valid @RequestBody RegisterArtifactRequest request) {
        return artifactCatalogService.register(
                projectId,
                request.pipelineRunId(),
                request.kind(),
                request.name(),
                request.version(),
                request.locator(),
                request.digest());
    }

    @GetMapping("/projects/{projectId}/deployments/{deploymentId}")
    public DeploymentView deployment(@PathVariable UUID projectId, @PathVariable UUID deploymentId) {
        DeploymentView view = deploymentTrackingService.get(deploymentId);
        if (!view.projectId().equals(projectId)) {
            throw new io.watermelon.ci.common.error.NotFoundException("deployment not found");
        }
        return view;
    }

    @PostMapping("/organizations/{organizationId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessGroup createGroup(
            @PathVariable UUID organizationId, @Valid @RequestBody CreateGroupRequest request) {
        return accessControlService.createGroup(organizationId, request.name(), request.defaultRole());
    }

    @GetMapping("/organizations/{organizationId}/groups")
    public java.util.List<AccessGroup> groups(@PathVariable UUID organizationId) {
        return accessControlService.listGroups(organizationId);
    }

    @PostMapping("/projects/{projectId}/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMembership grant(
            @PathVariable UUID projectId, @Valid @RequestBody GrantMembershipRequest request) {
        return accessControlService.grant(projectId, request.subject(), request.role());
    }

    @GetMapping("/manifest-templates")
    public Map<String, String> templates() {
        return manifestTemplates.all();
    }

    @GetMapping("/registry/endpoints")
    public Map<String, String> registryEndpoints() {
        return Map.of(
                "dockerPushHost", artifactCatalogService.dockerPushHost(),
                "mavenRepositoryUrl", artifactCatalogService.mavenRepositoryUrl());
    }
}
