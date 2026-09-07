package io.watermelon.ci.gitops.argocd;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.gitops.config.GitOpsProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ArgoCdClient {

    private static final Logger log = LoggerFactory.getLogger(ArgoCdClient.class);

    private final GitOpsProperties properties;
    private final RestTemplate restTemplate;

    public ArgoCdClient(GitOpsProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder.build();
    }

    public void upsertApplication(String appName, String manifestYaml) {
        if (properties.isDryRun()) {
            log.info("[dry-run] would upsert ArgoCD app {} ({} bytes yaml)", appName, manifestYaml.length());
            return;
        }
        String url = properties.getArgoCd().getBaseUrl() + "/api/v1/applications";
        try {
            try {
                restTemplate.exchange(
                        properties.getArgoCd().getBaseUrl() + "/api/v1/applications/" + appName,
                        HttpMethod.GET,
                        entity(null),
                        Void.class);
                restTemplate.exchange(
                        properties.getArgoCd().getBaseUrl() + "/api/v1/applications/" + appName,
                        HttpMethod.PUT,
                        entity(Map.of("metadata", Map.of("name", appName), "specYaml", manifestYaml)),
                        Void.class);
            } catch (HttpClientErrorException.NotFound ex) {
                restTemplate.exchange(url, HttpMethod.POST, entity(Map.of("yaml", manifestYaml)), Void.class);
            }
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.GITOPS_ERROR, "argocd upsert failed: " + ex.getMessage(), ex);
        }
    }

    public void sync(String appName) {
        if (properties.isDryRun()) {
            log.info("[dry-run] would sync ArgoCD app {}", appName);
            return;
        }
        String url = properties.getArgoCd().getBaseUrl() + "/api/v1/applications/" + appName + "/sync";
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity(Map.of("prune", true)), Void.class);
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.GITOPS_ERROR, "argocd sync failed: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> status(String appName) {
        if (properties.isDryRun()) {
            return Map.of("sync", "Synced", "health", "Healthy", "dryRun", "true");
        }
        try {
            Map<String, Object> body = restTemplate
                    .exchange(
                            properties.getArgoCd().getBaseUrl() + "/api/v1/applications/" + appName,
                            HttpMethod.GET,
                            entity(null),
                            new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody();
            if (body == null) {
                return Map.of("sync", "Unknown", "health", "Unknown");
            }
            Map<String, Object> status = body.get("status") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            Map<String, Object> sync = status.get("sync") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            Map<String, Object> health = status.get("health") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            return Map.of(
                    "sync", String.valueOf(sync.getOrDefault("status", "Unknown")),
                    "health", String.valueOf(health.getOrDefault("status", "Unknown")));
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.GITOPS_ERROR, "argocd status failed: " + ex.getMessage(), ex);
        }
    }

    private HttpEntity<?> entity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getArgoCd().getToken());
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }
}
