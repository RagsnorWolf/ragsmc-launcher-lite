package com.ragsmc.launcher.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ragsmc.launcher.download.DownloadManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

public class AutoUpdater {

    private final HttpClient client;
    private final DownloadManager downloads;

    public AutoUpdater() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        downloads = new DownloadManager();
    }

    public static class UpdateInfo {
        public String currentVersion;
        public String latestVersion;
        public boolean updateAvailable;
        public String downloadUrl;
        public String sha256;
        public String releaseNotes;
    }

    public String latestVersion(String currentVersion) throws IOException, InterruptedException {
        String manifestUrl = System.getenv("RAGSMC_UPDATE_URL");
        if (manifestUrl == null || manifestUrl.isEmpty()) {
            return currentVersion;
        }
        UpdateInfo info = check(currentVersion, manifestUrl);
        return info.latestVersion != null ? info.latestVersion : currentVersion;
    }

    public UpdateInfo check(String currentVersion, String manifestUrl) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(manifestUrl))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "RagsMC-Launcher/1.0")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Update HTTP " + resp.statusCode());
        }
        JsonObject o = JsonParser.parseString(resp.body()).getAsJsonObject();
        UpdateInfo info = new UpdateInfo();
        info.currentVersion = currentVersion;
        info.latestVersion = o.has("version") ? o.get("version").getAsString() : currentVersion;
        info.updateAvailable = !info.latestVersion.equals(currentVersion);
        info.downloadUrl = o.has("url") ? o.get("url").getAsString() : null;
        info.sha256 = (o.has("sha256") && !o.get("sha256").isJsonNull()) ? o.get("sha256").getAsString() : null;
        info.releaseNotes = (o.has("notes") && !o.get("notes").isJsonNull()) ? o.get("notes").getAsString() : null;
        return info;
    }

    public Path downloadAndVerify(UpdateInfo info, Path dest) throws IOException, InterruptedException {
        if (info.downloadUrl == null) {
            throw new IOException("Sin URL de descarga");
        }
        Path file = downloads.download(info.downloadUrl, dest, null);
        if (info.sha256 != null && !info.sha256.isEmpty()) {
            File f = file.toFile();
            if (!downloads.verifySha256(f, info.sha256)) {
                throw new IOException("SHA-256 no coincide, descarga corrupta");
            }
        }
        return file;
    }
}