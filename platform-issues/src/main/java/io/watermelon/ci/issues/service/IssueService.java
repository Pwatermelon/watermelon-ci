package io.watermelon.ci.issues.service;

import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.domain.issue.Issue;
import io.watermelon.ci.domain.issue.IssueRepository;
import io.watermelon.ci.domain.issue.IssueStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final Clock clock = Clock.system();

    public IssueService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    @Transactional
    public Issue create(UUID projectId, String title, String body) {
        long number = issueRepository.findMaxNumber(projectId) + 1;
        Issue issue = new Issue(Ids.newId(), projectId, number, title, body, IssueStatus.OPEN, clock.now());
        return issueRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public List<Issue> list(UUID projectId) {
        return issueRepository.findByProjectIdOrderByNumberDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Issue get(UUID projectId, long number) {
        return issueRepository
                .findByProjectIdAndNumber(projectId, number)
                .orElseThrow(() -> new NotFoundException("issue #" + number + " not found"));
    }

    @Transactional
    public Issue update(UUID projectId, long number, String title, String body, IssueStatus status, String assignee) {
        Issue issue = get(projectId, number);
        issue.update(title, body, status, assignee, clock.now());
        return issueRepository.save(issue);
    }
}
