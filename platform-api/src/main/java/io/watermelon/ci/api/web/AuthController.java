package io.watermelon.ci.api.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RestClient restClient;
    private final String tokenUrl;
    private final String clientId;

    public AuthController(
            @Value("${KEYCLOAK_TOKEN_URL:http://localhost:8083/realms/watermelon/protocol/openid-connect/token}")
                    String tokenUrl,
            @Value("${KEYCLOAK_CLIENT_ID:watermelon-console}") String clientId) {
        this.restClient = RestClient.create();
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
    }

    public record ExchangeRequest(String code, String codeVerifier, String redirectUri) {}

    @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exchange(@RequestBody ExchangeRequest body) {
        if (body == null || body.code() == null || body.code().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code required"));
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("code", body.code());
        form.add("redirect_uri", body.redirectUri() == null || body.redirectUri().isBlank()
                ? "http://localhost:8088/auth/callback"
                : body.redirectUri());
        if (body.codeVerifier() != null) {
            form.add("code_verifier", body.codeVerifier());
        }
        try {
            Map<?, ?> tokens = restClient
                    .post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return ResponseEntity.ok(tokens);
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "token_exchange_failed", "message", ex.getMessage()));
        }
    }
}
