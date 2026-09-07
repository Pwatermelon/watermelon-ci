package io.watermelon.ci.identity.rbac;

import io.watermelon.ci.domain.identity.PlatformRole;

public final class RoleHierarchy {

    private RoleHierarchy() {}

    public static boolean atLeast(PlatformRole actual, PlatformRole required) {
        return ordinal(actual) >= ordinal(required);
    }

    private static int ordinal(PlatformRole role) {
        return switch (role) {
            case VIEWER -> 1;
            case DEVELOPER -> 2;
            case MAINTAINER -> 3;
            case ADMIN -> 4;
            case OWNER -> 5;
        };
    }
}
