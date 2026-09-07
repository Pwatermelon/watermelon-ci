package io.watermelon.ci.common.ids;

import java.util.UUID;

public final class Ids {
    private Ids() {}

    public static UUID newId() {
        return UUID.randomUUID();
    }
}
