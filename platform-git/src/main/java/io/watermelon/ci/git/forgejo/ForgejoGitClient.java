package io.watermelon.ci.git.forgejo;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.git.config.GitProperties;
import io.watermelon.ci.git.spi.GitHostingClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class ForgejoGitClient implements GitHostingClient {

    private final GitProperties properties;
    private final RestTemplate restTemplate;

    public ForgejoGitClient(GitProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder.build();
    }

    @Override
    public RemoteRepository createRepository(String owner, String name, String description, boolean isPrivate) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("private", isPrivate);
        body.put("auto_init", true);
        try {
            Map<String, Object> response = restTemplate
                    .exchange(
                            properties.getBaseUrl() + "/api/v1/user/repos",
                            HttpMethod.POST,
                            entity(body),
                            new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody();
            if (response == null) {
                throw new PlatformException(ErrorCode.GIT_ERROR, "empty response from Forgejo");
            }
            return mapRepo(response, owner, name);
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "create repository failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<RemoteRepository> findRepository(String owner, String name) {
        try {
            Map<String, Object> response = restTemplate
                    .exchange(
                            properties.getBaseUrl() + "/api/v1/repos/" + owner + "/" + name,
                            HttpMethod.GET,
                            entity(null),
                            new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody();
            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(mapRepo(response, owner, name));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "find repository failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<String> readFile(String owner, String name, String path, String ref) {
        try {
            Map<String, Object> response = restTemplate
                    .exchange(
                            properties.getBaseUrl() + "/api/v1/repos/" + owner + "/" + name + "/contents/" + path + "?ref=" + ref,
                            HttpMethod.GET,
                            entity(null),
                            new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody();
            if (response == null || response.get("content") == null) {
                return Optional.empty();
            }
            String encoded = String.valueOf(response.get("content")).replace("\n", "");
            return Optional.of(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "read file failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RemoteFile> listFiles(String owner, String name, String path, String ref) {
        try {
            List<Map<String, Object>> response = restTemplate
                    .exchange(
                            properties.getBaseUrl() + "/api/v1/repos/" + owner + "/" + name + "/contents/" + path + "?ref=" + ref,
                            HttpMethod.GET,
                            entity(null),
                            new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .getBody();
            if (response == null) {
                return List.of();
            }
            return response.stream()
                    .map(item -> new RemoteFile(
                            String.valueOf(item.get("path")),
                            String.valueOf(item.get("type")),
                            item.get("size") instanceof Number n ? n.longValue() : 0L))
                    .toList();
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "list files failed: " + ex.getMessage(), ex);
        }
    }

    private HttpEntity<?> entity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }

    private static RemoteRepository mapRepo(Map<String, Object> response, String owner, String name) {
        return new RemoteRepository(
                owner,
                name,
                String.valueOf(response.getOrDefault("clone_url", "")),
                String.valueOf(response.getOrDefault("html_url", "")),
                String.valueOf(response.getOrDefault("default_branch", "main")));
    }
}
