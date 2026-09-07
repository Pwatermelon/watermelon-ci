package io.watermelon.ci.api.dto;

import io.watermelon.ci.domain.identity.PlatformRole;
import io.watermelon.ci.domain.issue.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class ApiDtos {

    private ApiDtos() {}

    public record CreateOrganizationRequest(@NotBlank String name) {}

    public record OrganizationResponse(UUID id, String slug, String name) {}

    public record CreateProjectRequest(@NotBlank String name, String description) {}

    public record ProjectResponse(UUID id, UUID organizationId, String slug, String name, String description) {}

    public record StartPipelineRequest(@NotBlank String ref, String commitSha, @NotBlank String manifestYaml) {}

    public record PreviewManifestRequest(@NotBlank String manifestYaml) {}

    public record CreateIssueRequest(@NotBlank String title, String body) {}

    public record UpdateIssueRequest(
            @NotBlank String title, String body, @NotNull IssueStatus status, String assigneeSubject) {}

    public record CreateGroupRequest(@NotBlank String name, @NotNull PlatformRole defaultRole) {}

    public record GrantMembershipRequest(@NotBlank String subject, @NotNull PlatformRole role) {}

    public record RegisterArtifactRequest(
            @NotNull io.watermelon.ci.domain.artifact.ArtifactKind kind,
            @NotBlank String name,
            @NotBlank String version,
            @NotBlank String locator,
            String digest,
            UUID pipelineRunId) {}
}
