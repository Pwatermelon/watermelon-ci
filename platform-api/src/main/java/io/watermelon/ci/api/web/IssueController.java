package io.watermelon.ci.api.web;

import static io.watermelon.ci.api.dto.ApiDtos.CreateIssueRequest;
import static io.watermelon.ci.api.dto.ApiDtos.UpdateIssueRequest;

import io.watermelon.ci.domain.issue.Issue;
import io.watermelon.ci.issues.service.IssueService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Issue create(@PathVariable UUID projectId, @Valid @RequestBody CreateIssueRequest request) {
        return issueService.create(projectId, request.title(), request.body());
    }

    @GetMapping
    public List<Issue> list(@PathVariable UUID projectId) {
        return issueService.list(projectId);
    }

    @GetMapping("/{number}")
    public Issue get(@PathVariable UUID projectId, @PathVariable long number) {
        return issueService.get(projectId, number);
    }

    @PutMapping("/{number}")
    public Issue update(
            @PathVariable UUID projectId, @PathVariable long number, @Valid @RequestBody UpdateIssueRequest request) {
        return issueService.update(
                projectId, number, request.title(), request.body(), request.status(), request.assigneeSubject());
    }
}
