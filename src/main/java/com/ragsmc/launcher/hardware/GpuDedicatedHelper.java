package com.ragsmc.launcher.hardware;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class GpuDedicatedHelper {

    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences";

    private GpuDedicatedHelper() {
    }

    public static void enableForJava(String javaExePath) {
        if (!isWindows()) {
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "add", REG_KEY,
                "/v", javaExePath,
                "/t", "REG_SZ",
                "/d", "GpuPreference=2;",
                "/f"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) ? p.exitValue() : -1;
            System.out.println("[ForceGPU] Configurado: " + javaExePath + " (exit=" + exitCode + ")");
            // Verify
            verifyRegistry(javaExePath);
        } catch (Exception e) {
            System.err.println("[ForceGPU] Error configurando " + javaExePath + ": " + e.getMessage());
        }
    }

    private static void verifyRegistry(String javaExePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", REG_KEY, "/v", javaExePath
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            String output = new String(p.getInputStream().readAllBytes());
            if (output.contains("GpuPreference=2")) {
                System.out.println("[ForceGPU] Verificado OK: " + javaExePath);
            } else {
                System.err.println("[ForceGPU] VERIFICACION FALLIDA: " + javaExePath);
            }
        } catch (Exception e) {
            System.err.println("[ForceGPU] Error verificando " + javaExePath + ": " + e.getMessage());
        }
    }

    public static void disableForJava(String javaExePath) {
        if (!isWindows()) {
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "delete", REG_KEY,
                "/v", javaExePath,
                "/f"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    public static void enableForAllKnownJava() {
        if (!isWindows()) {
            return;
        }
        // Get all known Java paths from JavaVersionSelector
        java.util.Map<Integer, String> found = com.ragsmc.launcher.minecraft.JavaVersionSelector.scanAll();
        System.out.println("[ForceGPU] Activado. Configurando " + found.size() + " JVMs...");
        int count = 0;
        for (String path : found.values()) {
            enableForJava(path);
            count++;
        }
        System.out.println("[ForceGPU] Total: " + count + " JVMs configuradas");
    }

    public static void disableForAllKnownJava() {
        if (!isWindows()) {
            return;
        }
        java.util.Map<Integer, String> found = com.ragsmc.launcher.minecraft.JavaVersionSelector.scanAll();
        System.out.println("[ForceGPU] Desactivado. Removiendo de " + found.size() + " JVMs...");
        int count = 0;
        for (String path : found.values()) {
            disableForJava(path);
            count++;
        }
        System.out.println("[ForceGPU] Total: " + count + " JVMs desactivadas");
    }

    public static List<String> detectGpus() {
        List<String> gpus = new ArrayList<>();
        if (isWindows()) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-Command",
                    "Get-CimInstance Win32_VideoController | Select-Object Name | ConvertTo-Json"
                );
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                String output = new String(p.getInputStream().readAllBytes()).trim();
                JsonElement elem = JsonParser.parseString(output);
                if (elem.isJsonArray()) {
                    JsonArray arr = elem.getAsJsonArray();
                    for (JsonElement e : arr) {
                        JsonObject obj = e.getAsJsonObject();
                        if (obj.has("Name")) {
                            String name = obj.get("Name").getAsString().trim();
                            if (!name.isEmpty()) {
                                gpus.add(name);
                            }
                        }
                    }
                } else if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    if (obj.has("Name")) {
                        String name = obj.get("Name").getAsString().trim();
                        if (!name.isEmpty()) {
                            gpus.add(name);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } else {
            // Linux: use lspci
            try {
                ProcessBuilder pb = new ProcessBuilder("sh", "-c", "lspci | grep -i vga");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                String output = new String(p.getInputStream().readAllBytes()).trim();
                for (String line : output.split("\n")) {
                    int idx = line.lastIndexOf(":");
                    if (idx >= 0 && idx < line.length() - 1) {
                        gpus.add(line.substring(idx + 1).trim());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return gpus;
    }

    public static boolean hasDedicatedGpu() {
        List<String> gpus = detectGpus();
        if (gpus.size() <= 1) {
            return false;
        }
        // Check if any GPU is discrete
        for (String gpu : gpus) {
            if (isDiscrete(gpu)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDiscrete(String name) {
        String lower = name.toLowerCase();
        return lower.contains("nvidia") || lower.contains("geforce") || lower.contains("quadro") || lower.contains("tesla") ||
               (lower.contains("radeon") && !lower.contains("integrated")) || (lower.contains("amd") && !lower.contains("integrated")) ||
               lower.contains("arc") || lower.contains("dedicated");
    }

    public static void applyLinuxEnv(ProcessBuilder pb, boolean enable) {
        if (isWindows()) {
            // On Windows, also set environment variables for Mesa if needed
            if (enable) {
                pb.environment().put("GALLIUM_DRIVER", "llvmpipe");
                pb.environment().put("LIBGL_ALWAYS_SOFTWARE", "1");
            }
            return;
        }
        if (enable) {
            // Detect GPU vendor
            List<String> gpus = detectGpus();
            boolean hasAmd = false, hasNvidia = false;
            for (String gpu : gpus) {
                String lower = gpu.toLowerCase();
                if (lower.contains("amd") || lower.contains("radeon")) {
                    hasAmd = true;
                }
                if (lower.contains("nvidia") || lower.contains("geforce") || lower.contains("quadro")) {
                    hasNvidia = true;
                }
            }
            if (hasAmd) {
                pb.environment().put("DRI_PRIME", "1");
            }
            if (hasNvidia) {
                pb.environment().put("__NV_PRIME_RENDER_OFFLOAD", "1");
                pb.environment().put("__GLX_VENDOR_LIBRARY_NAME", "nvidia");
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
