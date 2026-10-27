package io.watermelon.ci.jenkins.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.jenkins.config.JenkinsProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class JenkinsClient {

    private static final Logger log = LoggerFactory.getLogger(JenkinsClient.class);
    private static final MediaType XML_UTF8 = new MediaType("application", "xml", StandardCharsets.UTF_8);

    private final JenkinsProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JenkinsClient(JenkinsProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder.build();
    }

    public void upsertPipelineJob(String jobName, String jenkinsfile) {
        String configXml = toPipelineConfigXml(jenkinsfile);
        String encoded = encodeJob(jobName);
        try {
            boolean exists = jobExists(encoded);
            if (exists) {
                String updateUrl = properties.getBaseUrl() + "/job/" + encoded + "/config.xml";
                restTemplate.exchange(updateUrl, HttpMethod.POST, entityWithCrumb(configXml, XML_UTF8), Void.class);
            } else {
                String createUrl = properties.getBaseUrl() + "/createItem?name=" + encoded;
                restTemplate.exchange(createUrl, HttpMethod.POST, entityWithCrumb(configXml, XML_UTF8), Void.class);
            }
        } catch (RestClientException ex) {
            if (properties.isSoftFail()) {
                log.warn("Jenkins upsert soft-failed for {}: {}", jobName, ex.getMessage());
                return;
            }
            throw new PlatformException(ErrorCode.JENKINS_ERROR, "failed to upsert Jenkins job: " + ex.getMessage(), ex);
        }
    }

    public int triggerBuild(String jobName) {
        String encoded = encodeJob(jobName);
        String url = properties.getBaseUrl() + "/job/" + encoded + "/build";
        try {
            // Capture BEFORE POST: after trigger Jenkins bumps nextBuildNumber to N+1.
            int buildNumber = peekNextBuildNumber(encoded);
            ResponseEntity<Void> response =
                    restTemplate.exchange(url, HttpMethod.POST, entityWithCrumb(null, null), Void.class);
            String queue = response.getHeaders().getFirst("Location");
            log.info("Triggered Jenkins job {} build#{} queue={}", jobName, buildNumber, queue);
            return Math.max(1, buildNumber);
        } catch (RestClientException ex) {
            if (properties.isSoftFail()) {
                log.warn("Jenkins trigger soft-failed for {}: {}", jobName, ex.getMessage());
                return 1;
            }
            throw new PlatformException(ErrorCode.JENKINS_ERROR, "failed to trigger build: " + ex.getMessage(), ex);
        }
    }

    public BuildInfo getBuild(String jobName, int buildNumber) {
        String url = properties.getBaseUrl() + "/job/" + encodeJob(jobName) + "/" + buildNumber
                + "/api/json?tree=number,building,result,duration,url,timestamp";
        try {
            JsonNode body = restTemplate.exchange(url, HttpMethod.GET, authEntity(null, null), JsonNode.class).getBody();
            if (body == null) {
                return null;
            }
            return new BuildInfo(
                    body.path("number").asInt(buildNumber),
                    body.path("building").asBoolean(false),
                    body.path("result").asText(null),
                    body.path("duration").asLong(0),
                    body.path("url").asText(null),
                    body.path("timestamp").asLong(0));
        } catch (RestClientException ex) {
            log.debug("getBuild {} #{}: {}", jobName, buildNumber, ex.getMessage());
            return null;
        }
    }

    public List<StageInfo> getStages(String jobName, int buildNumber) {
        // Pipeline Stage View / wfapi (if available)
        String wf = properties.getBaseUrl() + "/job/" + encodeJob(jobName) + "/" + buildNumber + "/wfapi/describe";
        try {
            JsonNode body = restTemplate.exchange(wf, HttpMethod.GET, authEntity(null, null), JsonNode.class).getBody();
            if (body != null && body.path("stages").isArray()) {
                List<StageInfo> stages = new ArrayList<>();
                for (JsonNode s : body.path("stages")) {
                    stages.add(new StageInfo(
                            s.path("name").asText("stage"),
                            mapWfStatus(s.path("status").asText("UNKNOWN")),
                            s.path("durationMillis").asLong(0)));
                }
                return stages;
            }
        } catch (RestClientException ex) {
            log.debug("wfapi unavailable: {}", ex.getMessage());
        }
        return List.of();
    }

    public String getConsoleText(String jobName, int buildNumber) {
        String url = properties.getBaseUrl() + "/job/" + encodeJob(jobName) + "/" + buildNumber + "/consoleText";
        try {
            ResponseEntity<String> res =
                    restTemplate.exchange(url, HttpMethod.GET, authEntity(null, null), String.class);
            return res.getBody() != null ? res.getBody() : "";
        } catch (RestClientException ex) {
            return "(console unavailable: " + ex.getMessage() + ")";
        }
    }

    public String jobUrl(String jobName) {
        return properties.getBaseUrl() + "/job/" + encodeJob(jobName) + "/";
    }

    public String buildUrl(String jobName, int buildNumber) {
        return jobUrl(jobName) + buildNumber + "/";
    }

    private boolean jobExists(String encoded) {
        try {
            restTemplate.exchange(
                    properties.getBaseUrl() + "/job/" + encoded + "/api/json",
                    HttpMethod.GET,
                    authEntity(null, null),
                    Void.class);
            return true;
        } catch (RestClientException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private int peekNextBuildNumber(String encoded) {
        try {
            Map<String, Object> body = restTemplate
                    .exchange(
                            properties.getBaseUrl() + "/job/" + encoded + "/api/json?tree=nextBuildNumber",
                            HttpMethod.GET,
                            authEntity(null, null),
                            Map.class)
                    .getBody();
            if (body != null && body.get("nextBuildNumber") instanceof Number n) {
                return Math.max(1, n.intValue());
            }
        } catch (RestClientException ex) {
            log.debug("Could not resolve build number: {}", ex.getMessage());
        }
        return 1;
    }

    private HttpEntity<String> entityWithCrumb(String body, MediaType contentType) {
        HttpHeaders headers = authHeaders();
        applyCrumb(headers);
        if (body != null && contentType != null) {
            headers.setContentType(contentType);
        }
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> authEntity(String body, MediaType contentType) {
        HttpHeaders headers = authHeaders();
        if (body != null && contentType != null) {
            headers.setContentType(contentType);
        }
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String token = properties.getUsername() + ":" + properties.getApiToken();
        headers.set(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
        return headers;
    }

    private void applyCrumb(HttpHeaders headers) {
        try {
            String url = properties.getBaseUrl() + properties.getCrumbIssuerPath();
            ResponseEntity<String> res =
                    restTemplate.exchange(url, HttpMethod.GET, authEntity(null, null), String.class);
            if (res.getBody() == null) {
                return;
            }
            JsonNode node = objectMapper.readTree(res.getBody());
            String crumb = node.path("crumb").asText(null);
            String field = node.path("crumbRequestField").asText("Jenkins-Crumb");
            if (crumb != null && !crumb.isBlank()) {
                headers.set(field, crumb);
            }
            // Jenkins sometimes also wants the cookie session from crumb call
            List<String> cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null && !cookies.isEmpty()) {
                headers.put(HttpHeaders.COOKIE, cookies.stream().map(c -> c.split(";", 2)[0]).toList());
            }
        } catch (Exception ex) {
            log.warn("Jenkins crumb fetch failed (POST may 403): {}", ex.getMessage());
        }
    }

    private static String encodeJob(String jobName) {
        return jobName.replace("/", "%2F");
    }

    private static String mapWfStatus(String status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> "SUCCESS";
            case "FAILED", "FAILURE" -> "FAILED";
            case "IN_PROGRESS", "RUNNING" -> "RUNNING";
            case "NOT_EXECUTED", "ABORTED", "PAUSED_PENDING_INPUT" -> status.toUpperCase();
            default -> status.toUpperCase();
        };
    }

    private static String toPipelineConfigXml(String jenkinsfile) {
        // CDATA keeps UTF-8 / special chars valid; escape nested CDATA terminators.
        String cdata = jenkinsfile.replace("]]>", "]]]]><![CDATA[>");
        return """
                <?xml version='1.1' encoding='UTF-8'?>
                <flow-definition plugin="workflow-job@2.40">
                  <actions/>
                  <description>Managed by Watermelon CI</description>
                  <keepDependencies>false</keepDependencies>
                  <properties/>
                  <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps@2.90">
                    <script><![CDATA[%s]]></script>
                    <sandbox>true</sandbox>
                  </definition>
                  <triggers/>
                  <disabled>false</disabled>
                </flow-definition>
                """.formatted(cdata);
    }

    public record BuildInfo(int number, boolean building, String result, long durationMs, String url, long timestamp) {
        public String mappedStatus() {
            if (building) {
                return "RUNNING";
            }
            if (result == null) {
                return "QUEUED";
            }
            return switch (result) {
                case "SUCCESS" -> "SUCCESS";
                case "FAILURE", "FAILED" -> "FAILED";
                case "ABORTED" -> "CANCELED";
                case "UNSTABLE" -> "FAILED";
                default -> result;
            };
        }
    }

    public record StageInfo(String name, String status, long durationMs) {}
}
