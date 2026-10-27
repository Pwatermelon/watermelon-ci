package io.watermelon.ci.registry.docker;

import io.watermelon.ci.domain.artifact.ArtifactKind;
import io.watermelon.ci.registry.config.RegistryProperties;
import io.watermelon.ci.registry.spi.ArtifactRegistryClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for Docker Distribution / Harbor catalog API.
 * Production installs should point this at Harbor with RBAC and vulnerability scanning.
 */
@Component
public class DockerRegistryClient implements ArtifactRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(DockerRegistryClient.class);

    private final RegistryProperties properties;
    private final RestTemplate restTemplate;

    public DockerRegistryClient(RegistryProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        String user = properties.getDocker().getUsername();
        String pass = properties.getDocker().getPassword();
        if (user != null && !user.isBlank()) {
            this.restTemplate = builder.basicAuthentication(user, pass != null ? pass : "").build();
        } else {
            this.restTemplate = builder.build();
        }
    }

    @Override
    public ArtifactKind kind() {
        return ArtifactKind.DOCKER_IMAGE;
    }

    @Override
    public List<RemoteArtifact> list(String projectKey) {
        return listCatalog(projectKey);
    }

    /** Live registry catalog (optionally filtered). Includes tags when available. */
    public List<RemoteArtifact> listCatalog(String filter) {
        String url = properties.getDocker().getBaseUrl() + "/v2/_catalog";
        try {
            Map<String, Object> body = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
            if (body == null || !(body.get("repositories") instanceof List<?> repos)) {
                return List.of();
            }
            List<RemoteArtifact> result = new ArrayList<>();
            String host = properties.getDocker().getBaseUrl()
                    .replace("https://", "")
                    .replace("http://", "")
                    .replaceAll("/$", "");
            for (Object repo : repos) {
                String name = String.valueOf(repo);
                if (filter != null && !filter.isBlank()
                        && !name.contains(filter)
                        && !name.startsWith(properties.getDocker().getProject() + "/")) {
                    continue;
                }
                List<String> tags = listTags(name);
                if (tags.isEmpty()) {
                    result.add(new RemoteArtifact(name, "latest", host + "/" + name + ":latest", null));
                } else {
                    for (String tag : tags) {
                        result.add(new RemoteArtifact(name, tag, host + "/" + name + ":" + tag, null));
                    }
                }
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Docker registry catalog unavailable at {}: {}", url, ex.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> listTags(String repository) {
        try {
            Map<String, Object> body = restTemplate.exchange(
                    properties.getDocker().getBaseUrl() + "/v2/" + repository + "/tags/list",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
            if (body != null && body.get("tags") instanceof List<?> tags) {
                List<String> out = new ArrayList<>();
                for (Object t : tags) {
                    out.add(String.valueOf(t));
                }
                return out;
            }
        } catch (RestClientException ignored) {
            // no tags yet
        }
        return List.of();
    }

    public String imageLocator(String imageName, String tag) {
        return properties.getDocker().pushHost() + "/" + imageName + ":" + tag;
    }
}
