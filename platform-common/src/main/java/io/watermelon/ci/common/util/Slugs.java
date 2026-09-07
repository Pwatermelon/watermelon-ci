package io.watermelon.ci.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugs {
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9-]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private Slugs() {}

    public static String of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("slug source must not be blank");
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '-').replace('_', '-');
        s = NON_SLUG.matcher(s).replaceAll("-");
        s = MULTI_DASH.matcher(s).replaceAll("-");
        s = s.replaceAll("^-+|-+$", "");
        if (s.isBlank()) {
            throw new IllegalArgumentException("cannot derive slug from: " + raw);
        }
        return s;
    }
}
