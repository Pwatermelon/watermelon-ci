package io.watermelon.ci.secrets.service;

import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.domain.organization.Organization;
import io.watermelon.ci.domain.organization.OrganizationRepository;
import io.watermelon.ci.domain.project.Project;
import io.watermelon.ci.domain.project.ProjectRepository;
import io.watermelon.ci.domain.secret.ProjectSecretMeta;
import io.watermelon.ci.domain.secret.ProjectSecretMetaRepository;
import io.watermelon.ci.secrets.model.SecretDtos.SecretListView;
import io.watermelon.ci.secrets.model.SecretDtos.SecretMetaView;
import io.watermelon.ci.secrets.vault.VaultKvClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectSecretService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectSecretMetaRepository metaRepository;
    private final VaultKvClient vaultKvClient;
    private final Clock clock = Clock.system();

    public ProjectSecretService(
            ProjectRepository projectRepository,
            OrganizationRepository organizationRepository,
            ProjectSecretMetaRepository metaRepository,
            VaultKvClient vaultKvClient) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.metaRepository = metaRepository;
        this.vaultKvClient = vaultKvClient;
    }

    @Transactional
    public SecretMetaView upsert(UUID projectId, String environment, String name, String value, String description) {
        ProjectContext ctx = context(projectId);
        String path = vaultKvClient.logicalPath(ctx.org().getSlug(), ctx.project().getSlug(), environment);
        vaultKvClient.merge(path, Map.of(name, value));

        ProjectSecretMeta meta = metaRepository
                .findByProjectIdAndEnvironmentAndName(projectId, environment, name)
                .orElseGet(() -> new ProjectSecretMeta(
                        Ids.newId(), projectId, environment, name, path, description, clock.now()));
        meta.touch(clock.now());
        metaRepository.save(meta);
        return toView(meta);
    }

    @Transactional
    public SecretListView upsertBulk(UUID projectId, String environment, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return list(projectId, environment);
        }
        ProjectContext ctx = context(projectId);
        String path = vaultKvClient.logicalPath(ctx.org().getSlug(), ctx.project().getSlug(), environment);
        vaultKvClient.merge(path, values);
        for (String name : values.keySet()) {
            ProjectSecretMeta meta = metaRepository
                    .findByProjectIdAndEnvironmentAndName(projectId, environment, name)
                    .orElseGet(() -> new ProjectSecretMeta(
                            Ids.newId(), projectId, environment, name, path, null, clock.now()));
            meta.touch(clock.now());
            metaRepository.save(meta);
        }
        return list(projectId, environment);
    }

    @Transactional(readOnly = true)
    public SecretListView list(UUID projectId, String environmentOrNull) {
        ProjectContext ctx = context(projectId);
        List<ProjectSecretMeta> metas = environmentOrNull == null || environmentOrNull.isBlank()
                ? metaRepository.findByProjectIdOrderByEnvironmentAscNameAsc(projectId)
                : metaRepository.findByProjectIdAndEnvironmentOrderByNameAsc(projectId, environmentOrNull);
        String pathHint = environmentOrNull == null || environmentOrNull.isBlank()
                ? vaultKvClient.logicalPath(ctx.org().getSlug(), ctx.project().getSlug(), "*")
                : vaultKvClient.logicalPath(ctx.org().getSlug(), ctx.project().getSlug(), environmentOrNull);
        return new SecretListView(metas.stream().map(this::toView).toList(), pathHint);
    }

    @Transactional
    public void delete(UUID projectId, String environment, String name) {
        ProjectContext ctx = context(projectId);
        String path = vaultKvClient.logicalPath(ctx.org().getSlug(), ctx.project().getSlug(), environment);
        vaultKvClient.deleteKey(path, name);
        metaRepository
                .findByProjectIdAndEnvironmentAndName(projectId, environment, name)
                .ifPresent(metaRepository::delete);
    }

    /** Used by GitOps renderer — reads values only in control plane, never returned to UI list APIs. */
    @Transactional(readOnly = true)
    public Map<String, String> readEnvironmentValues(UUID projectId, String environment) {
        ProjectContext ctx = context(projectId);
        String path = vaultKvClient.logicalPath(ctx.org().getSlug(), ctx.project().getSlug(), environment);
        return vaultKvClient.read(path);
    }

    public String vaultPath(UUID projectId, String environment) {
        ProjectContext ctx = context(projectId);
        return vaultKvClient.logicalPath(ctx.org().getSlug(), ctx.project().getSlug(), environment);
    }

    private ProjectContext context(UUID projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("project not found"));
        Organization org = organizationRepository
                .findById(project.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("organization not found"));
        return new ProjectContext(project, org);
    }

    private SecretMetaView toView(ProjectSecretMeta meta) {
        return new SecretMetaView(
                meta.getId(),
                meta.getProjectId(),
                meta.getEnvironment(),
                meta.getName(),
                meta.getVaultPath(),
                meta.getDescription(),
                meta.getCreatedAt(),
                meta.getUpdatedAt());
    }

    private record ProjectContext(Project project, Organization org) {}
}
