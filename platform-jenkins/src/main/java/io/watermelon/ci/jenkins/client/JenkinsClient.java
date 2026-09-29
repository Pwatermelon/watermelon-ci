package io.watermelon.ci.jenkins.client;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.jenkins.config.JenkinsProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    private final JenkinsProperties properties;
    private final RestTemplate restTemplate;

    public JenkinsClient(JenkinsProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder.build();
    }

    public void upsertPipelineJob(String jobName, String jenkinsfile) {
        String configXml = toPipelineConfigXml(jenkinsfile);
        String encoded = jobName.replace("/", "%2F");
        try {
            String checkUrl = properties.getBaseUrl() + "/job/" + encoded + "/api/json";
            boolean exists = true;
            try {
                restTemplate.exchange(checkUrl, HttpMethod.GET, authEntity(null), Void.class);
            } catch (RestClientException ex) {
                exists = false;
            }
            if (exists) {
                String updateUrl = properties.getBaseUrl() + "/job/" + encoded + "/config.xml";
                restTemplate.exchange(updateUrl, HttpMethod.POST, authEntity(configXml, MediaType.APPLICATION_XML), Void.class);
            } else {
                String createUrl = properties.getBaseUrl() + "/createItem?name=" + encoded;
                restTemplate.exchange(createUrl, HttpMethod.POST, authEntity(configXml, MediaType.APPLICATION_XML), Void.class);
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
        String encoded = jobName.replace("/", "%2F");
        String url = properties.getBaseUrl() + "/job/" + encoded + "/build";
        try {
            ResponseEntity<Void> response =
                    restTemplate.exchange(url, HttpMethod.POST, authEntity(null), Void.class);
            String queue = response.getHeaders().getFirst("Location");
            log.info("Triggered Jenkins job {} queue={}", jobName, queue);
            return estimateBuildNumber(jobName);
        } catch (RestClientException ex) {
            if (properties.isSoftFail()) {
                log.warn("Jenkins trigger soft-failed for {}: {}", jobName, ex.getMessage());
                return 1;
            }
            throw new PlatformException(ErrorCode.JENKINS_ERROR, "failed to trigger build: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private int estimateBuildNumber(String jobName) {
        try {
            String url = properties.getBaseUrl() + "/job/" + jobName.replace("/", "%2F") + "/api/json?tree=nextBuildNumber";
            Map<String, Object> body = restTemplate.exchange(url, HttpMethod.GET, authEntity(null), Map.class).getBody();
            if (body != null && body.get("nextBuildNumber") instanceof Number n) {
                return n.intValue() - 1;
            }
        } catch (RestClientException ex) {
            log.debug("Could not resolve build number: {}", ex.getMessage());
        }
        return -1;
    }

    private HttpEntity<String> authEntity(String body) {
        return authEntity(body, MediaType.APPLICATION_JSON);
    }

    private HttpEntity<String> authEntity(String body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        String token = properties.getUsername() + ":" + properties.getApiToken();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
        if (body != null) {
            headers.setContentType(contentType);
        }
        return new HttpEntity<>(body, headers);
    }

    private static String toPipelineConfigXml(String jenkinsfile) {
        String escaped = jenkinsfile
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return """
                <?xml version='1.1' encoding='UTF-8'?>
                <flow-definition plugin="workflow-job">
                  <description>Managed by Watermelon CI</description>
                  <keepDependencies>false</keepDependencies>
                  <properties/>
                  <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps">
                    <script>%s</script>
                    <sandbox>true</sandbox>
                  </definition>
                  <triggers/>
                  <disabled>false</disabled>
                </flow-definition>
                """.formatted(escaped);
    }
}
