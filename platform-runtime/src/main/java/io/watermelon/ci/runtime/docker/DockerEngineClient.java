package io.watermelon.ci.runtime.docker;

import com.fasterxml.jackson.databind.JsonNode;
import io.watermelon.ci.domain.deployment.DeploymentStatus;
import io.watermelon.ci.runtime.config.RuntimeProperties;
import io.watermelon.ci.runtime.spi.ContainerRuntimeClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Observes containers deployed by CI via Docker Engine API.
 * Containers are discovered by labels watermelon.ci/*.
 */
@Component
public class DockerEngineClient implements ContainerRuntimeClient {

    private static final Logger log = LoggerFactory.getLogger(DockerEngineClient.class);

    private final RuntimeProperties properties;
    private final RestTemplate restTemplate;

    public DockerEngineClient(RuntimeProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder.build();
    }

    @Override
    public List<ObservedContainer> listWatermelonContainers() {
        if (!properties.getDocker().isEnabled()) {
            return List.of();
        }
        String url = properties.getDocker().getEngineUrl() + "/containers/json?all=1&filters={\"label\":[\"watermelon.ci/track=true\"]}";
        try {
            JsonNode arr = restTemplate.getForObject(url, JsonNode.class);
            if (arr == null || !arr.isArray()) {
                return List.of();
            }
            List<ObservedContainer> result = new ArrayList<>();
            for (JsonNode node : arr) {
                result.add(map(node));
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Docker engine unavailable: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<ObservedContainer> inspect(String externalId) {
        if (!properties.getDocker().isEnabled()) {
            return Optional.empty();
        }
        String url = properties.getDocker().getEngineUrl() + "/containers/" + externalId + "/json";
        try {
            JsonNode node = restTemplate.getForObject(url, JsonNode.class);
            if (node == null) {
                return Optional.empty();
            }
            return Optional.of(mapInspect(node));
        } catch (RestClientException ex) {
            log.debug("inspect failed for {}: {}", externalId, ex.getMessage());
            return Optional.empty();
        }
    }

    private ObservedContainer map(JsonNode node) {
        String id = text(node, "Id");
        String name = node.path("Names").isArray() && !node.path("Names").isEmpty()
                ? node.path("Names").get(0).asText().replaceFirst("^/", "")
                : id;
        String image = text(node, "Image");
        String state = text(node, "State");
        JsonNode labels = node.path("Labels");
        return new ObservedContainer(
                id,
                name,
                image,
                state,
                mapStatus(state),
                labels.path("watermelon.ci/pipeline-run").asText(null),
                labels.path("watermelon.ci/project").asText(null),
                labels.path("watermelon.ci/environment").asText(null));
    }

    private ObservedContainer mapInspect(JsonNode node) {
        String id = text(node, "Id");
        String name = node.path("Name").asText("").replaceFirst("^/", "");
        if (name.isBlank()) {
            name = id;
        }
        String image = node.path("Config").path("Image").asText("");
        String state = node.path("State").path("Status").asText("unknown");
        JsonNode labels = node.path("Config").path("Labels");
        return new ObservedContainer(
                id,
                name,
                image,
                state,
                mapStatus(state),
                labels.path("watermelon.ci/pipeline-run").asText(null),
                labels.path("watermelon.ci/project").asText(null),
                labels.path("watermelon.ci/environment").asText(null));
    }

    private static DeploymentStatus mapStatus(String state) {
        if (state == null) {
            return DeploymentStatus.UNKNOWN;
        }
        return switch (state.toLowerCase()) {
            case "running" -> DeploymentStatus.HEALTHY;
            case "created", "restarting" -> DeploymentStatus.DEPLOYING;
            case "paused", "dead" -> DeploymentStatus.DEGRADED;
            case "exited", "removing" -> DeploymentStatus.STOPPED;
            default -> DeploymentStatus.UNKNOWN;
        };
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }
}
