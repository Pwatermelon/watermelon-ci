package io.watermelon.ci.registry.service;

import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.domain.artifact.ArtifactKind;
import io.watermelon.ci.domain.artifact.ArtifactRecord;
import io.watermelon.ci.domain.artifact.ArtifactRecordRepository;
import io.watermelon.ci.registry.docker.DockerRegistryClient;
import io.watermelon.ci.registry.maven.MavenRegistryClient;
import io.watermelon.ci.registry.model.ArtifactView;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtifactCatalogService {

    private final ArtifactRecordRepository repository;
    private final DockerRegistryClient dockerRegistryClient;
    private final MavenRegistryClient mavenRegistryClient;
    private final Clock clock;

    public ArtifactCatalogService(
            ArtifactRecordRepository repository,
            DockerRegistryClient dockerRegistryClient,
            MavenRegistryClient mavenRegistryClient) {
        this.repository = repository;
        this.dockerRegistryClient = dockerRegistryClient;
        this.mavenRegistryClient = mavenRegistryClient;
        this.clock = Clock.system();
    }

    @Transactional
    public ArtifactView register(
            UUID projectId,
            UUID pipelineRunId,
            ArtifactKind kind,
            String name,
            String version,
            String locator,
            String digest) {
        ArtifactRecord record = new ArtifactRecord(
                Ids.newId(), projectId, pipelineRunId, kind, name, version, locator, digest, clock.now());
        repository.save(record);
        return toView(record);
    }

    @Transactional(readOnly = true)
    public List<ArtifactView> listProjectArtifacts(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<ArtifactView> listByKind(UUID projectId, ArtifactKind kind) {
        return repository.findByProjectIdAndKindOrderByCreatedAtDesc(projectId, kind).stream().map(this::toView).toList();
    }

    public String dockerPushHost() {
        return dockerRegistryClient.imageLocator("placeholder", "tag").replace("/placeholder:tag", "");
    }

    public String mavenRepositoryUrl() {
        return mavenRegistryClient.repositoryUrl();
    }

    private ArtifactView toView(ArtifactRecord record) {
        return new ArtifactView(
                record.getId(),
                record.getProjectId(),
                record.getPipelineRunId(),
                record.getKind(),
                record.getName(),
                record.getVersion(),
                record.getLocator(),
                record.getDigest(),
                record.getCreatedAt());
    }
}
