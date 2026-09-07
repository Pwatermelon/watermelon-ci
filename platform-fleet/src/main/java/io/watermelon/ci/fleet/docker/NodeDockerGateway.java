package io.watermelon.ci.fleet.docker;

import com.fasterxml.jackson.databind.JsonNode;
import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Full Docker Engine control for a specific worker node endpoint.
 * Used by the in-platform fleet admin console (not just observation).
 */
@Component
public class NodeDockerGateway {

    private final RestTemplate restTemplate;

    public NodeDockerGateway(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public List<ManagedContainer> listContainers(String engineUrl, boolean all) {
        String url = engineUrl + "/containers/json?all=" + (all ? "1" : "0");
        try {
            JsonNode arr = restTemplate.getForObject(url, JsonNode.class);
            if (arr == null || !arr.isArray()) {
                return List.of();
            }
            List<ManagedContainer> result = new ArrayList<>();
            for (JsonNode node : arr) {
                result.add(mapList(node));
            }
            return result;
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.RUNTIME_ERROR, "list containers failed: " + ex.getMessage(), ex);
        }
    }

    public ManagedContainer inspect(String engineUrl, String containerId) {
        try {
            JsonNode node = restTemplate.getForObject(engineUrl + "/containers/" + containerId + "/json", JsonNode.class);
            if (node == null) {
                throw new PlatformException(ErrorCode.NOT_FOUND, "container not found: " + containerId);
            }
            return mapInspect(node);
        } catch (PlatformException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.RUNTIME_ERROR, "inspect failed: " + ex.getMessage(), ex);
        }
    }

    public void start(String engineUrl, String containerId) {
        postAction(engineUrl, containerId, "start");
    }

    public void stop(String engineUrl, String containerId) {
        postAction(engineUrl, containerId, "stop");
    }

    public void restart(String engineUrl, String containerId) {
        postAction(engineUrl, containerId, "restart");
    }

    public void remove(String engineUrl, String containerId, boolean force) {
        try {
            restTemplate.exchange(
                    engineUrl + "/containers/" + containerId + "?force=" + force,
                    HttpMethod.DELETE,
                    HttpEntity.EMPTY,
                    Void.class);
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.RUNTIME_ERROR, "remove failed: " + ex.getMessage(), ex);
        }
    }

    public String logs(String engineUrl, String containerId, int tail) {
        String url = engineUrl + "/containers/" + containerId + "/logs?stdout=1&stderr=1&timestamps=1&tail=" + tail;
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
            byte[] body = response.getBody();
            if (body == null) {
                return "";
            }
            return stripDockerLogHeaders(body);
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.RUNTIME_ERROR, "logs failed: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> stats(String engineUrl, String containerId) {
        String url = engineUrl + "/containers/" + containerId + "/stats?stream=0";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            return body != null ? body : Map.of();
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.RUNTIME_ERROR, "stats failed: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> info(String engineUrl) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(engineUrl + "/info", Map.class);
            return body != null ? body : Map.of();
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.RUNTIME_ERROR, "node info failed: " + ex.getMessage(), ex);
        }
    }

    public boolean ping(String engineUrl) {
        try {
            restTemplate.getForObject(URI.create(engineUrl + "/_ping"), String.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void postAction(String engineUrl, String containerId, String action) {
        try {
            restTemplate.exchange(
                    engineUrl + "/containers/" + containerId + "/" + action,
                    HttpMethod.POST,
                    HttpEntity.EMPTY,
                    Void.class);
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.RUNTIME_ERROR, action + " failed: " + ex.getMessage(), ex);
        }
    }

    private static ManagedContainer mapList(JsonNode node) {
        String id = node.path("Id").asText();
        String name = node.path("Names").isArray() && !node.path("Names").isEmpty()
                ? node.path("Names").get(0).asText().replaceFirst("^/", "")
                : id.substring(0, Math.min(12, id.length()));
        JsonNode labels = node.path("Labels");
        return new ManagedContainer(
                id,
                name,
                node.path("Image").asText(),
                node.path("State").asText(),
                node.path("Status").asText(),
                labels.path("watermelon.ci/project").asText(null),
                labels.path("watermelon.ci/environment").asText(null),
                labels.path("watermelon.ci/pipeline-run").asText(null),
                createdPorts(node.path("Ports")));
    }

    private static ManagedContainer mapInspect(JsonNode node) {
        String id = node.path("Id").asText();
        String name = node.path("Name").asText("").replaceFirst("^/", "");
        JsonNode labels = node.path("Config").path("Labels");
        return new ManagedContainer(
                id,
                name,
                node.path("Config").path("Image").asText(),
                node.path("State").path("Status").asText(),
                node.path("State").path("Status").asText(),
                labels.path("watermelon.ci/project").asText(null),
                labels.path("watermelon.ci/environment").asText(null),
                labels.path("watermelon.ci/pipeline-run").asText(null),
                List.of());
    }

    private static List<String> createdPorts(JsonNode ports) {
        if (ports == null || !ports.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode p : ports) {
            int privatePort = p.path("PrivatePort").asInt();
            int publicPort = p.path("PublicPort").asInt();
            String ip = p.path("IP").asText("");
            if (publicPort > 0) {
                result.add(ip + ":" + publicPort + "->" + privatePort + "/" + p.path("Type").asText("tcp"));
            } else {
                result.add(privatePort + "/" + p.path("Type").asText("tcp"));
            }
        }
        return result;
    }

    /** Docker multiplexed log stream has 8-byte headers per frame. */
    private static String stripDockerLogHeaders(byte[] raw) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i + 8 <= raw.length) {
            int size = ((raw[i + 4] & 0xff) << 24)
                    | ((raw[i + 5] & 0xff) << 16)
                    | ((raw[i + 6] & 0xff) << 8)
                    | (raw[i + 7] & 0xff);
            i += 8;
            if (i + size > raw.length) {
                break;
            }
            sb.append(new String(raw, i, size, StandardCharsets.UTF_8));
            i += size;
        }
        if (sb.isEmpty()) {
            return new String(raw, StandardCharsets.UTF_8);
        }
        return sb.toString();
    }

    public record ManagedContainer(
            String id,
            String name,
            String image,
            String state,
            String statusText,
            String projectSlug,
            String environment,
            String pipelineRunId,
            List<String> ports
    ) {
        public String shortId() {
            return id != null && id.length() > 12 ? id.substring(0, 12) : id;
        }
    }

    @SuppressWarnings("unused")
    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
