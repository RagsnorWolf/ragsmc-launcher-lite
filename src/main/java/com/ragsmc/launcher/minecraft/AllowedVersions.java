package com.ragsmc.launcher.minecraft;

import java.util.Set;

public final class AllowedVersions {

    private AllowedVersions() {
    }

    public static final Set<String> ALLOWED = Set.of(
        // Java 21
        "1.21.10",
        "1.21.4",
        // Java 17
        "1.20.4",
        "1.20.1",
        "1.19.2",
        "1.18.2",
        "1.17",
        "23w31a",
        // Java 8
        "1.16.5",
        "1.14.4",
        "1.12.2",
        "1.8.9",
        "1.7.10"
    );

    /** Versiones ordenadas de mas nueva a mas vieja (snapshot al final). */
    public static final java.util.List<String> ORDERED = java.util.List.of(
        "1.21.10",
        "1.21.4",
        "1.20.4",
        "1.20.1",
        "1.19.2",
        "1.18.2",
        "1.17",
        "1.16.5",
        "1.14.4",
        "1.12.2",
        "1.8.9",
        "1.7.10",
        "23w31a"
    );

    public static boolean isAllowed(String version) {
        return version != null && ALLOWED.contains(version.trim());
    }

    public static String getAllowedMessage() {
        return getMessage();
    }

    public static String getMessage() {
        return "Por ahora solo se soportan: 1.21.10, 1.21.4, 1.20.4, "
             + "1.20.1, 1.19.2, 1.18.2, 1.17, 1.16.5, 1.14.4, 1.12.2, "
             + "1.8.9, 1.7.10 y 23w31a";
    }
}
