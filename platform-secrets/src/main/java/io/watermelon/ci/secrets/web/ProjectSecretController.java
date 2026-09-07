package io.watermelon.ci.secrets.web;

import io.watermelon.ci.secrets.model.SecretDtos.BulkUpsertRequest;
import io.watermelon.ci.secrets.model.SecretDtos.SecretListView;
import io.watermelon.ci.secrets.model.SecretDtos.SecretMetaView;
import io.watermelon.ci.secrets.model.SecretDtos.UpsertSecretRequest;
import io.watermelon.ci.secrets.service.ProjectSecretService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/secrets")
public class ProjectSecretController {

    private final ProjectSecretService projectSecretService;

    public ProjectSecretController(ProjectSecretService projectSecretService) {
        this.projectSecretService = projectSecretService;
    }

    @GetMapping
    public SecretListView list(@PathVariable UUID projectId, @RequestParam(required = false) String environment) {
        return projectSecretService.list(projectId, environment);
    }

    @PutMapping
    public SecretMetaView upsert(@PathVariable UUID projectId, @Valid @RequestBody UpsertSecretRequest request) {
        return projectSecretService.upsert(
                projectId, request.environment(), request.name(), request.value(), request.description());
    }

    @PutMapping("/bulk")
    public SecretListView bulk(@PathVariable UUID projectId, @Valid @RequestBody BulkUpsertRequest request) {
        return projectSecretService.upsertBulk(projectId, request.environment(), request.values());
    }

    @DeleteMapping("/{environment}/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID projectId, @PathVariable String environment, @PathVariable String name) {
        projectSecretService.delete(projectId, environment, name);
    }
}
