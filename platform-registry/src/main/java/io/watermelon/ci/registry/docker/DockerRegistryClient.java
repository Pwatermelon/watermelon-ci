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
        this.restTemplate = builder
                .basicAuthentication(properties.getDocker().getUsername(), properties.getDocker().getPassword())
                .build();
    }

    @Override
    public ArtifactKind kind() {
        return ArtifactKind.DOCKER_IMAGE;
    }

    @Override
    public List<RemoteArtifact> list(String projectKey) {
        String url = properties.getDocker().getBaseUrl() + "/v2/_catalog";
        try {
            Map<String, Object> body = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
            if (body == null || !(body.get("repositories") instanceof List<?> repos)) {
                return List.of();
            }
            List<RemoteArtifact> result = new ArrayList<>();
            for (Object repo : repos) {
                String name = String.valueOf(repo);
                if (!name.contains(projectKey) && !name.startsWith(properties.getDocker().getProject() + "/")) {
                    continue;
                }
                result.add(new RemoteArtifact(name, "latest", properties.getDocker().pushHost() + "/" + name + ":latest", null));
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Docker registry catalog unavailable at {}: {}", url, ex.getMessage());
            return List.of();
        }
    }

    public String imageLocator(String imageName, String tag) {
        return properties.getDocker().pushHost() + "/" + imageName + ":" + tag;
    }
}
