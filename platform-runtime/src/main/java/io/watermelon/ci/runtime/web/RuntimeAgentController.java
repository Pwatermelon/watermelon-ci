package io.watermelon.ci.runtime.web;

import io.watermelon.ci.domain.pipeline.PipelineRun;
import io.watermelon.ci.domain.pipeline.PipelineRunRepository;
import io.watermelon.ci.runtime.model.AgentHeartbeatRequest;
import io.watermelon.ci.runtime.model.DeploymentView;
import io.watermelon.ci.runtime.service.DeploymentTrackingService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/runtime/agent")
public class RuntimeAgentController {

    private final DeploymentTrackingService trackingService;
    private final PipelineRunRepository pipelineRunRepository;

    public RuntimeAgentController(
            DeploymentTrackingService trackingService, PipelineRunRepository pipelineRunRepository) {
        this.trackingService = trackingService;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @PostMapping("/heartbeat")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeploymentView heartbeat(@RequestBody AgentHeartbeatRequest request) {
        if (request.pipelineRunId() == null || request.externalId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pipelineRunId and externalId required");
        }
        UUID runId = UUID.fromString(request.pipelineRunId());
        PipelineRun run = pipelineRunRepository
                .findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pipeline run not found"));
        return trackingService.registerFromHeartbeat(run.getProjectId(), request);
    }
}
