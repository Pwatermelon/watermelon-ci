
package io.watermelon.ci.domain.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_runs")
public class PipelineRun {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private long number;

    @Column(nullable = false, length = 200)
    private String ref;

    @Column(length = 64)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PipelineStatus status;

    @Column(length = 500)
    private String jenkinsJobName;

    @Column
    private Integer jenkinsBuildNumber;

    @Lob
    @Column(nullable = false)
    private String manifestYaml;

    @Lob
    private String compiledJenkinsfile;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant finishedAt;

    protected PipelineRun() {}

    public PipelineRun(
            UUID id,
            UUID projectId,
            long number,
            String ref,
            String commitSha,
            PipelineStatus status,
            String manifestYaml,
            Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.number = number;
        this.ref = ref;
        this.commitSha = commitSha;
        this.status = status;
        this.manifestYaml = manifestYaml;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public long getNumber() { return number; }
    public String getRef() { return ref; }
    public String getCommitSha() { return commitSha; }
    public PipelineStatus getStatus() { return status; }
    public String getJenkinsJobName() { return jenkinsJobName; }
    public Integer getJenkinsBuildNumber() { return jenkinsBuildNumber; }
    public String getManifestYaml() { return manifestYaml; }
    public String getCompiledJenkinsfile() { return compiledJenkinsfile; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }

    public void markCompiled(String jenkinsfile, String jobName) {
        this.compiledJenkinsfile = jenkinsfile;
        this.jenkinsJobName = jobName;
    }

    public void markRunning(int buildNumber) {
        this.status = PipelineStatus.RUNNING;
        this.jenkinsBuildNumber = buildNumber;
    }

    public void markFinished(PipelineStatus status, Instant finishedAt) {
        this.status = status;
        this.finishedAt = finishedAt;
    }
}
