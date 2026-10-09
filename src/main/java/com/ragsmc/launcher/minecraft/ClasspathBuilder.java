package com.ragsmc.launcher.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ragsmc.launcher.LaunchConfig;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ClasspathBuilder {

    public static String build(LaunchConfig config) {
        File base = baseDirOf(config);
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        List<String> classpath = new ArrayList<>();
        File versionJar = new File(base, "versions" + File.separator + config.getVersion()
                + File.separator + config.getVersion() + ".jar");
        classpath.add(versionJar.getAbsolutePath());
        for (String lib : config.getLibraries()) {
            if (lib.contains("/") || lib.contains("\\") || lib.contains(":")) {
                classpath.add(new File(new File(base, "libraries"), lib).getAbsolutePath());
            }
        }
        try {
            File versionJson = new File(base, "versions" + File.separator + config.getVersion()
                    + File.separator + config.getVersion() + ".json");
            if (versionJson.exists()) {
                String json = new String(Files.readAllBytes(versionJson.toPath()), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonArray libraries = root.getAsJsonArray("libraries");
                if (libraries != null) {
                    String os = NativesExtractor.currentOs();
                    for (JsonElement el : libraries) {
                        JsonObject lib = el.getAsJsonObject();
                        if (!ruleAllows(lib, os) || !lib.has("downloads")) {
                            continue;
                        }
                        JsonObject dl = lib.getAsJsonObject("downloads");
                        if (dl.has("artifact")) {
                            String path = dl.getAsJsonObject("artifact").get("path").getAsString();
                            File f = new File(new File(base, "libraries"), path);
                            if (f.exists()) {
                                classpath.add(f.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < classpath.size(); i++) {
            String entry = classpath.get(i);
            if (!seen.add(entry)) {
                continue;
            }
            if (!first) {
                result.append(File.pathSeparator);
            }
            first = false;
            result.append(entry);
        }
        return result.toString();
    }

    private static File baseDirOf(LaunchConfig config) {
        if (config.getSharedDir() != null && !config.getSharedDir().isEmpty()) {
            return new File(config.getSharedDir());
        }
        return new File(config.getGameDir());
    }

    private static boolean ruleAllows(JsonObject lib, String os) {
        if (!lib.has("rules")) {
            return true;
        }
        String action = "disallow";
        JsonArray rules = lib.getAsJsonArray("rules");
        for (JsonElement el : rules) {
            JsonObject rule = el.getAsJsonObject();
            String ruleAction = rule.has("action") ? rule.get("action").getAsString() : "allow";
            if (!rule.has("os")) {
                action = ruleAction;
                continue;
            }
            String name = rule.getAsJsonObject("os").has("name")
                    ? rule.getAsJsonObject("os").get("name").getAsString() : "";
            boolean match = os.equals(name) || (os.equals("osx") && name.equals("macos"));
            if (match) {
                action = ruleAction;
            }
        }
        return action.equals("allow");
    }

    public static String mainClass(String loader) {
        if (loader == null) {
            return "net.minecraft.client.main.Main";
        }
        String l = loader.trim().toLowerCase();
        if (l.equals("fabric")) {
            return "net.fabricmc.loader.impl.launch.knot.KnotClient";
        }
        if (l.equals("quilt")) {
            return "org.quiltmc.loader.impl.launch.knot.KnotClient";
        }
        if (l.equals("forge") || l.equals("neoforge")) {
            return "cpw.mods.modlauncher.Launcher";
        }
        return "net.minecraft.client.main.Main";
    }
}