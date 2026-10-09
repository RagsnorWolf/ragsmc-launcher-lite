package com.ragsmc.launcher.mods;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ModrinthAPI {

    private static final String BASE = "https://api.modrinth.com/v2";
    private final HttpClient client;
    private final Gson gson;

    public ModrinthAPI() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        gson = new Gson();
    }

    public static class ModResult {
        public String id;
        public String slug;
        public String title;
        public String description;
        public String iconUrl;
        public int downloads;
    }

    public List<ModResult> search(String query, String mcVersion, String loader, int limit) throws IOException, InterruptedException {
        return search(query, mcVersion, loader, limit, 0);
    }

    public List<ModResult> search(String query, String mcVersion, String loader, int limit, int offset) throws IOException, InterruptedException {
        String facets = "[[\"project_type:mod\"]]";
        String url = BASE + "/search?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8)
                + "&limit=" + limit + "&offset=" + Math.max(0, offset);
        String body = get(url);
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray hits = root.getAsJsonArray("hits");
        List<ModResult> out = new ArrayList<>(hits.size());
        for (JsonElement h : hits) {
            JsonObject o = h.getAsJsonObject();
            ModResult r = new ModResult();
            r.id = o.has("project_id") ? o.get("project_id").getAsString() : "";
            r.slug = o.has("slug") ? o.get("slug").getAsString() : "";
            r.title = o.has("title") ? o.get("title").getAsString() : "";
            r.description = o.has("description") ? o.get("description").getAsString() : "";
            r.iconUrl = (o.has("icon_url") && !o.get("icon_url").isJsonNull()) ? o.get("icon_url").getAsString() : "";
            r.downloads = o.has("downloads") ? o.get("downloads").getAsInt() : 0;
            out.add(r);
        }
        return out;
    }

    public String latestVersionFileUrl(String projectId, String mcVersion, String loader) throws IOException, InterruptedException {
        String url = BASE + "/project/" + projectId + "/version?game_versions=" + URLEncoder.encode(mcVersion, StandardCharsets.UTF_8)
                + "&loaders=" + URLEncoder.encode(loader, StandardCharsets.UTF_8);
        String body = get(url);
        JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
        if (versions.size() == 0) {
            return null;
        }
        JsonObject v = versions.get(0).getAsJsonObject();
        JsonArray files = v.getAsJsonArray("files");
        if (files == null || files.size() == 0) {
            return null;
        }
        return files.get(0).getAsJsonObject().get("url").getAsString();
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "RagsMC-Launcher/1.0")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Modrinth HTTP " + resp.statusCode());
        }
        return resp.body();
    }
}