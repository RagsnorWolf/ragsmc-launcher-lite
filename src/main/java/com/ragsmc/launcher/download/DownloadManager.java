package com.ragsmc.launcher.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.BiConsumer;

public class DownloadManager {

    private final HttpClient client;

    public DownloadManager() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private volatile DownloadStats stats;

    public void setStats(DownloadStats stats) {
        this.stats = stats;
    }

    public Path download(String url, Path dest, BiConsumer<Long, Long> progress) throws IOException, InterruptedException {
        Files.createDirectories(dest.getParent());
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " para " + url);
        }
        long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
        Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".part");
        try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            long done = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                done += n;
                if (progress != null) {
                    progress.accept(done, total);
                }
                DownloadStats st = stats;
                if (st != null) {
                    st.addBytes(n);
                }
            }
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    public boolean verifySha1(File file, String expectedSha1) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] data = Files.readAllBytes(file.toPath());
            byte[] digest = md.digest(data);
            String actual = HexFormat.of().formatHex(digest);
            return actual.equalsIgnoreCase(expectedSha1);
        } catch (Exception e) {
            throw new IOException("No se pudo verificar SHA-1: " + e.getMessage(), e);
        }
    }

    public boolean verifySha256(File file, String expectedSha256) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(file.toPath());
            byte[] digest = md.digest(data);
            String actual = HexFormat.of().formatHex(digest);
            return actual.equalsIgnoreCase(expectedSha256);
        } catch (Exception e) {
            throw new IOException("No se pudo verificar SHA-256: " + e.getMessage(), e);
        }
    }
}