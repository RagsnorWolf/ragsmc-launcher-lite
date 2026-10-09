package com.ragsmc.launcher.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AuthlibPatcher {

    public static void patchIfNeeded(String versionId, File versionJson) throws IOException {
        if (!needsPatch(versionId)) {
            return;
        }
        String content = new String(Files.readAllBytes(versionJson.toPath()), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        JsonArray libraries = root.getAsJsonArray("libraries");
        if (libraries == null) {
            return;
        }
        for (JsonElement lib : libraries) {
            JsonObject libObj = lib.getAsJsonObject();
            if (!libObj.has("name")) {
                continue;
            }
            String name = libObj.get("name").getAsString();
            if (name != null && name.startsWith("com.mojang:authlib:") && name.contains("2.1.28")) {
                libObj.addProperty("name", name.replace("2.1.28", "2.3.31"));
                JsonObject downloads = libObj.getAsJsonObject("downloads");
                if (downloads != null) {
                    JsonObject artifact = downloads.getAsJsonObject("artifact");
                    if (artifact != null) {
                        artifact.remove("size");
                        artifact.remove("sha1");
                        artifact.remove("url");
                    }
                }
            }
        }
        String newContent = new Gson().toJson(root);
        Files.write(versionJson.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean needsPatch(String versionId) {
        if (versionId == null) {
            return false;
        }
        try {
            String v = versionId.trim();
            if (v.startsWith("1.")) {
                String rest = v.substring(2);
                int dot = rest.indexOf(".");
                String minorStr = dot < 0 ? rest : rest.substring(0, dot);
                int minor = Integer.parseInt(minorStr.replaceAll("[^0-9]", ""));
                return minor <= 16;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}