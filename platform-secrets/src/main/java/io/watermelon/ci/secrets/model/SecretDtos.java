package io.watermelon.ci.secrets.model;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class SecretDtos {

    private SecretDtos() {}

    public record UpsertSecretRequest(
            @NotBlank String environment,
            @NotBlank String name,
            @NotBlank String value,
            String description) {}

    public record BulkUpsertRequest(@NotBlank String environment, Map<String, String> values) {}

    public record SecretMetaView(
            UUID id,
            UUID projectId,
            String environment,
            String name,
            String vaultPath,
            String description,
            Instant createdAt,
            Instant updatedAt) {}

    /** Never includes secret values. */
    public record SecretListView(java.util.List<SecretMetaView> secrets, String vaultPath) {}
}
