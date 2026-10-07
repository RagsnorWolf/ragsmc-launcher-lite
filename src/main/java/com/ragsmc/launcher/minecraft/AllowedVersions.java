package com.ragsmc.launcher.minecraft;

import java.util.Set;

public final class AllowedVersions {

    private AllowedVersions() {
    }

    public static final Set<String> ALLOWED = Set.of(
        "1.16.5", "1.17", "1.20.1", "1.21.10", "23w31a"
    );

    public static boolean isAllowed(String version) {
        return ALLOWED.contains(version);
    }

    public static String getAllowedMessage() {
        return "Por ahora solo se soportan: 1.16.5, 1.17, 1.20.1, 1.21.10, 23w31a";
    }
}
