package io.watermelon.ci.api.web;

import static io.watermelon.ci.api.dto.ApiDtos.CreateGroupRequest;
import static io.watermelon.ci.api.dto.ApiDtos.GrantMembershipRequest;
import static io.watermelon.ci.api.dto.ApiDtos.RegisterArtifactRequest;

import io.watermelon.ci.domain.artifact.ArtifactKind;
import io.watermelon.ci.domain.identity.AccessGroup;
import io.watermelon.ci.domain.identity.ProjectMembership;
import io.watermelon.ci.domain.project.Project;
import io.watermelon.ci.domain.project.ProjectRepository;
import io.watermelon.ci.identity.service.AccessControlService;
import io.watermelon.ci.manifest.template.ManifestTemplates;
import io.watermelon.ci.registry.docker.DockerRegistryClient;
import io.watermelon.ci.registry.model.ArtifactView;
import io.watermelon.ci.registry.service.ArtifactCatalogService;
import io.watermelon.ci.registry.spi.ArtifactRegistryClient.RemoteArtifact;
import io.watermelon.ci.runtime.model.DeploymentView;
import io.watermelon.ci.runtime.service.DeploymentTrackingService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final DockerRegistryClient dockerRegistryClient;
    private final ProjectRepository projectRepository;

    public CatalogController(
            ArtifactCatalogService artifactCatalogService,
            DeploymentTrackingService deploymentTrackingService,
            AccessControlService accessControlService,
            ManifestTemplates manifestTemplates,
            DockerRegistryClient dockerRegistryClient,
            ProjectRepository projectRepository) {
        this.artifactCatalogService = artifactCatalogService;
        this.deploymentTrackingService = deploymentTrackingService;
        this.accessControlService = accessControlService;
        this.manifestTemplates = manifestTemplates;
        this.dockerRegistryClient = dockerRegistryClient;
        this.projectRepository = projectRepository;
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

    @GetMapping("/registry/catalog")
    public List<Map<String, String>> registryCatalog(@RequestParam(required = false) String filter) {
        return dockerRegistryClient.listCatalog(filter == null ? "" : filter).stream()
                .map((RemoteArtifact a) -> Map.of(
                        "name", a.name() != null ? a.name() : "",
                        "version", a.version() != null ? a.version() : "",
                        "locator", a.locator() != null ? a.locator() : "",
                        "digest", a.digest() != null ? a.digest() : ""))
                .toList();
    }

    /** Org-wide artifacts + live registry tree for the Registry console. */
    @GetMapping("/organizations/{organizationId}/registry")
    public Map<String, Object> organizationRegistry(@PathVariable UUID organizationId) {
        List<Project> projects = projectRepository.findByOrganizationId(organizationId);
        Map<String, String> projectByImage = new LinkedHashMap<>();
        List<Map<String, Object>> projectArtifacts = new ArrayList<>();
        List<Map<String, Object>> runtimeOnly = new ArrayList<>();

        for (Project project : projects) {
            List<ArtifactView> arts = artifactCatalogService.listProjectArtifacts(project.getId());
            for (ArtifactView a : arts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", a.id());
                row.put("projectId", project.getId());
                row.put("projectSlug", project.getSlug());
                row.put("name", a.name());
                row.put("version", a.version());
                row.put("locator", a.locator());
                row.put("kind", a.kind() != null ? a.kind().name() : "DOCKER_IMAGE");
                row.put("createdAt", a.createdAt() != null ? a.createdAt().toString() : null);
                boolean seeded = a.locator() != null && a.locator().startsWith("nginx:");
                row.put("source", seeded ? "runtime-seed" : "recorded");
                projectArtifacts.add(row);
                if (a.name() != null) {
                    projectByImage.putIfAbsent(a.name(), project.getSlug());
                    projectByImage.putIfAbsent("watermelon/" + a.name(), project.getSlug());
                }
            }
            List<DeploymentView> deps = deploymentTrackingService.listByProject(project.getId());
            if (!deps.isEmpty()) {
                // defer live-catalog check after we load catalog once below
                DeploymentView d = deps.getFirst();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("projectId", project.getId());
                row.put("projectSlug", project.getSlug());
                row.put("releaseName", d.releaseName());
                row.put("imageRef", d.imageRef());
                row.put("environment", d.environment());
                row.put("status", d.status() != null ? d.status().name() : null);
                row.put("note", "Контейнер запущен без publish в registry (демо-seed nginx)");
                runtimeOnly.add(row);
            }
        }

        List<RemoteArtifact> live = dockerRegistryClient.listCatalog("");
        List<Map<String, Object>> catalog = live.stream()
                .map((RemoteArtifact a) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    String name = a.name() != null ? a.name() : "";
                    row.put("name", name);
                    row.put("version", a.version() != null ? a.version() : "");
                    row.put("locator", a.locator() != null ? a.locator() : "");
                    row.put("digest", a.digest() != null ? a.digest() : "");
                    String shortName = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
                    row.put("projectSlug", projectByImage.getOrDefault(name, projectByImage.get(shortName)));
                    String[] parts = name.split("/");
                    row.put("folders", List.of(parts));
                    return row;
                })
                .toList();

        runtimeOnly.removeIf(row -> {
            String slug = String.valueOf(row.get("projectSlug"));
            return live.stream().anyMatch(r -> {
                String n = r.name() != null ? r.name() : "";
                return n.equals(slug) || n.endsWith("/" + slug);
            });
        });

        return Map.of(
                "pushHost", artifactCatalogService.dockerPushHost(),
                "catalog", catalog,
                "artifacts", projectArtifacts,
                "runtimeOnly", runtimeOnly,
                "projects", projects.stream()
                        .map(p -> Map.of("id", p.getId(), "slug", p.getSlug(), "name", p.getName()))
                        .toList());
    }
}
