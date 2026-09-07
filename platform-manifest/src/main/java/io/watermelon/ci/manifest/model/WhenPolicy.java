package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WhenPolicy {
    ON_SUCCESS,
    MANUAL,
    ALWAYS;

    @JsonCreator
    public static WhenPolicy from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ON_SUCCESS;
        }
        return WhenPolicy.valueOf(raw.trim().toUpperCase());
    }
}
