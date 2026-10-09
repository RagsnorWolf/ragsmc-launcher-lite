package com.ragsmc.launcher.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LoaderInstaller {

    private final HttpClient client;

    public LoaderInstaller() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String latestFabricLoader(String mcVersion) throws IOException, InterruptedException {
        String url = "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "RagsMC-Launcher/1.0")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Fabric meta HTTP " + resp.statusCode());
        }
        JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
        if (arr.size() == 0) {
            throw new IOException("Sin loaders Fabric para " + mcVersion);
        }
        JsonObject first = arr.get(0).getAsJsonObject();
        JsonObject loader = first.getAsJsonObject("loader");
        return loader.get("version").getAsString();
    }

    public String fabricProfileJsonUrl(String mcVersion, String loaderVersion) {
        return "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json";
    }

    public boolean isSupported(String loader) {
        return "fabric".equalsIgnoreCase(loader) || "vanilla".equalsIgnoreCase(loader);
    }

    public File loaderDir(File baseDir, String loader) {
        return new File(baseDir, "loaders" + File.separator + loader.toLowerCase());
    }
}