package io.watermelon.ci.secrets.vault;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.secrets.config.SecretsProperties;
import java.util.HashMap;
import java.util.Map;
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

/**
 * KV v2 client for OpenBao / HashiCorp Vault.
 * Path layout: {mount}/data/{prefix}/{org}/{project}/{env}
 */
@Component
public class VaultKvClient {

    private final SecretsProperties properties;
    private final RestTemplate restTemplate;

    public VaultKvClient(SecretsProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder.build();
    }

    public String logicalPath(String orgSlug, String projectSlug, String environment) {
        return properties.getPathPrefix() + "/" + orgSlug + "/" + projectSlug + "/" + environment;
    }

    public void put(String logicalPath, Map<String, String> data) {
        String url = properties.getVaultAddr() + "/v1/" + properties.getKvMount() + "/data/" + logicalPath;
        Map<String, Object> body = Map.of("data", data);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity(body), Void.class);
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.SECRETS_ERROR, "vault write failed: " + ex.getMessage(), ex);
        }
    }

    public void merge(String logicalPath, Map<String, String> patch) {
        Map<String, String> current = read(logicalPath);
        Map<String, String> merged = new HashMap<>(current);
        merged.putAll(patch);
        put(logicalPath, merged);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> read(String logicalPath) {
        String url = properties.getVaultAddr() + "/v1/" + properties.getKvMount() + "/data/" + logicalPath;
        try {
            Map<String, Object> response = restTemplate
                    .exchange(url, HttpMethod.GET, entity(null), new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody();
            if (response == null) {
                return Map.of();
            }
            Object data = response.get("data");
            if (data instanceof Map<?, ?> outer) {
                Object inner = outer.get("data");
                if (inner instanceof Map<?, ?> values) {
                    Map<String, String> result = new HashMap<>();
                    values.forEach((k, v) -> result.put(String.valueOf(k), v == null ? "" : String.valueOf(v)));
                    return result;
                }
            }
            return Map.of();
        } catch (HttpClientErrorException.NotFound ex) {
            return Map.of();
        } catch (RestClientException ex) {
            throw new PlatformException(ErrorCode.SECRETS_ERROR, "vault read failed: " + ex.getMessage(), ex);
        }
    }

    public void deleteKey(String logicalPath, String key) {
        Map<String, String> current = new HashMap<>(read(logicalPath));
        current.remove(key);
        put(logicalPath, current);
    }

    private HttpEntity<?> entity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", properties.getToken());
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }
}
