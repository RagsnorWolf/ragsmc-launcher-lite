package com.ragsmc.launcher.util;

public final class SystemInfo {

    private SystemInfo() {
    }

    public static long totalRamMb() {
        try {
            Object mx = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (mx instanceof com.sun.management.OperatingSystemMXBean) {
                long bytes = ((com.sun.management.OperatingSystemMXBean) mx).getTotalMemorySize();
                if (bytes > 0) {
                    return bytes / (1024L * 1024L);
                }
            }
        } catch (Throwable ignored) {
        }
        return fallbackTotalRamMb();
    }

    public static long freeRamMb() {
        try {
            Object mx = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (mx instanceof com.sun.management.OperatingSystemMXBean) {
                long bytes = ((com.sun.management.OperatingSystemMXBean) mx).getFreeMemorySize();
                if (bytes > 0) {
                    return bytes / (1024L * 1024L);
                }
            }
        } catch (Throwable ignored) {
        }
        return 1024L;
    }

    private static long fallbackTotalRamMb() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                Process p = new ProcessBuilder("wmic", "computersystem", "get", "TotalPhysicalMemory", "/value").start();
                byte[] out = p.getInputStream().readAllBytes();
                p.waitFor();
                String s = new String(out).replaceAll("[^0-9]", "");
                if (!s.isEmpty()) {
                    return Long.parseLong(s) / (1024L * 1024L);
                }
            }
        } catch (Throwable ignored) {
        }
        return 4096L;
    }

    public static long maxUsableRamMb() {
        long total = totalRamMb();
        long max = total - 512L;
        if (max < 1024L) {
            max = 1024L;
        }
        return max;
    }

    public static int suggestedRamMb() {
        long total = totalRamMb();
        if (total <= 4096L) {
            return 2048;
        }
        if (total <= 8192L) {
            return 4096;
        }
        return 6144;
    }

    public static long heapMaxMb() {
        return Runtime.getRuntime().maxMemory() / (1024L * 1024L);
    }

    public static String osName() {
        return System.getProperty("os.name", "desconocido");
    }
}