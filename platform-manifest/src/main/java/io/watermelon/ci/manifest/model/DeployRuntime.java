package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DeployRuntime {
    DOCKER,
    KUBERNETES,
    COMPOSE,
    ARGOCD;

    @JsonCreator
    public static DeployRuntime from(String raw) {
        return DeployRuntime.valueOf(raw.trim().toUpperCase());
    }
}
