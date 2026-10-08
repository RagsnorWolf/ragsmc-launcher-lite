package com.ragsmc.launcher.hardware;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class GpuDetector {

    private static final File CACHE_FILE = new File(
        System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home"),
        "RagsMC" + File.separator + "gpu_cache.json"
    );

    private GpuDetector() {
    }

    public static GpuInfo detect() {
        GpuInfo cached = loadCache();
        if (cached != null && System.currentTimeMillis() - cached.detectedAt < 86400000L * 30) {
            return cached;
        }

        String gpuName = detectGpuName();
        if (gpuName == null) {
            gpuName = "Unknown GPU";
        }

        String openglMax = estimateOpenGLMax(gpuName);
        GpuInfo info = new GpuInfo(gpuName, openglMax, System.currentTimeMillis());
        saveCache(info);
        return info;
    }

    public static GpuInfo forceRedetect() {
        String gpuName = detectGpuName();
        if (gpuName == null) {
            gpuName = "Unknown GPU";
        }
        String openglMax = estimateOpenGLMax(gpuName);
        GpuInfo info = new GpuInfo(gpuName, openglMax, System.currentTimeMillis());
        saveCache(info);
        return info;
    }

    private static String detectGpuName() {
        if (isWindows()) {
            return detectWindowsGpu();
        } else {
            return detectLinuxGpu();
        }
    }

    private static String detectWindowsGpu() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "Get-CimInstance Win32_VideoController | Select-Object Name | ConvertTo-Json"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return null;
            }
            String output = new String(p.getInputStream().readAllBytes());
            output = output.trim();
            if (output.isEmpty()) {
                return null;
            }
            JsonElement elem = JsonParser.parseString(output);
            List<String> names = new ArrayList<>();
            if (elem.isJsonArray()) {
                JsonArray arr = elem.getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();
                    if (obj.has("Name")) {
                        String name = obj.get("Name").getAsString().trim();
                        if (!name.isEmpty()) {
                            names.add(name);
                        }
                    }
                }
            } else if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has("Name")) {
                    String name = obj.get("Name").getAsString().trim();
                    if (!name.isEmpty()) {
                        names.add(name);
                    }
                }
            }
            String best = null;
            for (String n : names) {
                if (isDiscrete(n)) {
                    return n;
                }
                if (best == null) {
                    best = n;
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isDiscrete(String name) {
        String lower = name.toLowerCase();
        return lower.contains("nvidia") || lower.contains("geforce") || lower.contains("quadro") || lower.contains("tesla") ||
               (lower.contains("radeon") && !lower.contains("integrated")) || (lower.contains("amd") && !lower.contains("integrated")) ||
               lower.contains("arc") || lower.contains("dedicated");
    }

    private static String detectLinuxGpu() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "lspci | grep -i vga");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            String output = new String(p.getInputStream().readAllBytes()).trim();
            if (output.isEmpty()) {
                return null;
            }
            int idx = output.lastIndexOf(":");
            if (idx >= 0 && idx < output.length() - 1) {
                return output.substring(idx + 1).trim();
            }
            return output;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String estimateOpenGLMax(String gpuName) {
        String lower = gpuName.toLowerCase();

        if (lower.contains("intel")) {
            if (matchesAny(lower, "hd graphics 2000", "hd graphics 3000", "hd graphics")) {
                return "3.1";
            }
            if (matchesAny(lower, "hd graphics 2500", "hd graphics 4000")) {
                return "4.0";
            }
            if (matchesAny(lower, "hd graphics 4400", "hd graphics 4600", "hd graphics 5000", "hd graphics 510", "hd graphics 515",
                          "hd graphics 520", "hd graphics 530", "hd graphics 540", "hd graphics 550", "hd graphics 560",
                          "hd graphics 600", "hd graphics 605", "hd graphics 610", "hd graphics 615", "hd graphics 620",
                          "hd graphics 630", "iris plus", "iris pro")) {
                return "4.3";
            }
            if (matchesAny(lower, "uhd graphics 620", "uhd graphics 630", "uhd graphics 710", "uhd graphics 730", "uhd graphics 750", "uhd graphics 770")) {
                return "4.5";
            }
            if (lower.contains("iris xe") || lower.contains("arc")) {
                return "4.6";
            }
            if (lower.contains("core") && (lower.contains("11th") || lower.contains("12th") || lower.contains("13th") || lower.contains("14th"))) {
                return "4.6";
            }
            if (lower.contains("core") && (lower.contains("8th") || lower.contains("9th") || lower.contains("10th"))) {
                return "4.5";
            }
            return "3.1";
        }

        if (lower.contains("nvidia") || lower.contains("geforce") || lower.contains("quadro") || lower.contains("tesla")) {
            if (matchesAny(lower, "gtx 4", "gtx 5", "gtx 6", "gtx 7")) {
                return "4.4";
            }
            if (matchesAny(lower, "gtx 8", "gtx 9", "gtx 10")) {
                return "4.5";
            }
            if (matchesAny(lower, "gtx 16", "rtx 20", "rtx 30", "rtx 40", "rtx 50")) {
                return "4.6";
            }
            if (lower.contains("rtx")) {
                return "4.6";
            }
            return "4.1";
        }

        if (lower.contains("amd") || lower.contains("radeon") || lower.contains("rx ")) {
            if (matchesAny(lower, "hd 4", "hd 5", "hd 6", "hd 7", "hd 8")) {
                return "4.3";
            }
            if (matchesAny(lower, "rx 2", "rx 3", "rx 4", "rx 5", "rx 500")) {
                return "4.5";
            }
            if (matchesAny(lower, "rx 6", "rx 7", "rx 7000")) {
                return "4.6";
            }
            if (lower.contains("vega")) {
                return "4.5";
            }
            return "4.3";
        }

        return "3.3";
    }

    private static boolean matchesAny(String text, String... patterns) {
        for (String pat : patterns) {
            if (text.contains(pat)) {
                return true;
            }
        }
        return false;
    }

    private static GpuInfo loadCache() {
        try {
            if (!CACHE_FILE.exists()) {
                return null;
            }
            String json = new String(Files.readAllBytes(CACHE_FILE.toPath()));
            return new Gson().fromJson(json, GpuInfo.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static void saveCache(GpuInfo info) {
        try {
            CACHE_FILE.getParentFile().mkdirs();
            String json = new Gson().toJson(info);
            try (FileWriter w = new FileWriter(CACHE_FILE)) {
                w.write(json);
            }
        } catch (Exception ignored) {
        }
    }

    public static class GpuInfo {
        public String name;
        public String openglMax;
        public long detectedAt;

        public GpuInfo() {}

        public GpuInfo(String name, String openglMax, long detectedAt) {
            this.name = name;
            this.openglMax = openglMax;
            this.detectedAt = detectedAt;
        }
    }
}
