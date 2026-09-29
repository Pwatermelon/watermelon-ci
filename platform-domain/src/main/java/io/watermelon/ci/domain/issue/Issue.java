
package io.watermelon.ci.domain.issue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private long number;

    @Column(nullable = false, length = 300)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IssueStatus status;

    @Column(length = 200)
    private String assigneeSubject;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Issue() {}

    public Issue(UUID id, UUID projectId, long number, String title, String body, IssueStatus status, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.number = number;
        this.title = title;
        this.body = body;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public long getNumber() { return number; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public IssueStatus getStatus() { return status; }
    public String getAssigneeSubject() { return assigneeSubject; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String title, String body, IssueStatus status, String assigneeSubject, Instant now) {
        this.title = title;
        this.body = body;
        this.status = status;
        this.assigneeSubject = assigneeSubject;
        this.updatedAt = now;
    }
}
