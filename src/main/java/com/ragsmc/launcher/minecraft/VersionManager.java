package com.ragsmc.launcher.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ragsmc.launcher.download.DownloadManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class VersionManager {

    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

    private final HttpClient client;
    private final File cacheDir;
    private final DownloadManager downloads;
    private volatile com.ragsmc.launcher.download.DownloadStats stats;

    public VersionManager(File cacheDir) {
        this.cacheDir = cacheDir;
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        downloads = new DownloadManager();
    }

    public static class McVersion {
        public String id;
        public String type;
        public String url;
    }

    public void setStats(com.ragsmc.launcher.download.DownloadStats stats) {
        this.stats = stats;
        downloads.setStats(stats);
    }

    private java.nio.file.Path dl(String url, java.nio.file.Path dest, String display) throws IOException, InterruptedException {
        com.ragsmc.launcher.download.DownloadStats st = stats;
        if (st != null) {
            String name = display;
            if (name == null || name.isEmpty()) {
                try {
                    name = dest.getFileName().toString();
                } catch (Exception ignored) {
                    name = "";
                }
            }
            st.fileName = name;
        }
        java.nio.file.Path p = dl(url, dest, null);
        if (st != null) {
            st.filesDone++;
        }
        return p;
    }

    private void initTotals(JsonObject root) {
        com.ragsmc.launcher.download.DownloadStats st = stats;
        if (st == null) {
            return;
        }
        st.bytesDone = 0;
        st.filesDone = 0;
        st.startNanos = System.nanoTime();
        long total = 0;
        int files = 2;
        try {
            if (root.has("downloads")) {
                JsonObject dl = root.getAsJsonObject("downloads");
                if (dl.has("client")) {
                    JsonObject client = dl.getAsJsonObject("client");
                    if (client.has("size")) {
                        total += client.get("size").getAsLong();
                    }
                }
            }
            JsonArray libraries = root.getAsJsonArray("libraries");
            String os = NativesExtractor.currentOs();
            if (libraries != null) {
                for (JsonElement el : libraries) {
                    JsonObject lib = el.getAsJsonObject();
                    if (!ruleAllows(lib, os) || !lib.has("downloads")) {
                        continue;
                    }
                    JsonObject d = lib.getAsJsonObject("downloads");
                    if (d.has("artifact") && d.getAsJsonObject("artifact").has("size")) {
                        total += d.getAsJsonObject("artifact").get("size").getAsLong();
                        files++;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        st.bytesTotal = total;
        st.filesTotal = files;
    }

    private void addAssetsTotal(JsonObject objects) {
        com.ragsmc.launcher.download.DownloadStats st = stats;
        if (st == null || objects == null) {
            return;
        }
        try {
            long total = 0;
            int n = 0;
            for (java.util.Map.Entry<String, JsonElement> e : objects.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("size")) {
                    total += o.get("size").getAsLong();
                }
                n++;
            }
            st.bytesTotal += total;
            if (st.filesTotal < 0) {
                st.filesTotal = n;
            } else {
                st.filesTotal += n;
            }
        } catch (Exception ignored) {
        }
    }

    public List<McVersion> fetchManifest(boolean useCache) throws IOException, InterruptedException {
        File cached = new File(cacheDir, "version_manifest_v2.json");
        String body = null;
        if (useCache && cached.exists()) {
            try {
                body = new String(Files.readAllBytes(cached.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                body = null;
            }
        }
        if (body == null) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(MANIFEST_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "RagsMC-Launcher/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IOException("Manifest HTTP " + resp.statusCode());
            }
            body = resp.body();
            try {
                cacheDir.mkdirs();
                Files.write(cached.toPath(), body.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray versions = root.getAsJsonArray("versions");
        List<McVersion> out = new ArrayList<>(versions.size());
        for (JsonElement v : versions) {
            JsonObject o = v.getAsJsonObject();
            McVersion mv = new McVersion();
            mv.id = o.get("id").getAsString();
            mv.type = o.has("type") ? o.get("type").getAsString() : "release";
            mv.url = o.has("url") ? o.get("url").getAsString() : "";
            out.add(mv);
        }
        return out;
    }

    public List<String> installedVersions(File versionsDir) {
        List<String> out = new ArrayList<>();
        File[] dirs = versionsDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return out;
        }
        for (File d : dirs) {
            if (new File(d, d.getName() + ".json").exists()) {
                out.add(d.getName());
            }
        }
        return out;
    }

    public void downloadVersion(String versionId, File gameDir, BiConsumer<String, Double> progress) throws IOException, InterruptedException {
        downloadVersion(versionId, gameDir, progress, null);
    }

    public void downloadVersion(String versionId, File gameDir, BiConsumer<String, Double> progress, AtomicBoolean cancel) throws IOException, InterruptedException {
        String versionUrl = findVersionUrl(versionId);
        if (versionUrl == null) {
            throw new IOException("Version desconocida en Mojang: " + versionId);
        }
        File versionDir = new File(gameDir, "versions" + File.separator + versionId);
        versionDir.mkdirs();
        File versionJson = new File(versionDir, versionId + ".json");
        step(progress, "Descargando JSON de " + versionId, 0.02);
        dl(versionUrl, versionJson.toPath(), null);
        String json = new String(Files.readAllBytes(versionJson.toPath()), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        initTotals(root);
        if (root.has("assetIndex")) {
            JsonObject ai = root.getAsJsonObject("assetIndex");
            String aiId = ai.has("id") ? ai.get("id").getAsString() : versionId;
            String aiUrl = ai.has("url") ? ai.get("url").getAsString() : null;
            String aiSha1 = ai.has("sha1") ? ai.get("sha1").getAsString() : null;
            if (aiUrl != null) {
                step(progress, "Descargando assetIndex " + aiId, 0.05);
                File indexesDir = new File(gameDir, "assets" + File.separator + "indexes");
                indexesDir.mkdirs();
                File aiFile = new File(indexesDir, aiId + ".json");
                if (!aiFile.exists() || (aiSha1 != null && !aiSha1.isEmpty() && !aiSha1.equals(sha1(aiFile)))) {
                    dl(aiUrl, aiFile.toPath(), null);
                }
                downloadAssets(gameDir, aiFile, progress, cancel);
            }
        }
        checkCancel(cancel);
        if (root.has("downloads")) {
            JsonObject dl = root.getAsJsonObject("downloads");
            if (dl.has("client")) {
                JsonObject client = dl.getAsJsonObject("client");
                String url = client.get("url").getAsString();
                File jar = new File(versionDir, versionId + ".jar");
                step(progress, "Descargando cliente " + versionId, 0.15);
                if (!jar.exists() || jar.length() == 0) {
                    dl(url, jar.toPath(), null);
                }
            }
        }
        downloadLibraries(gameDir, versionId, root, progress, cancel);
        step(progress, "Version " + versionId + " lista", 1.0);
    }

    private void downloadAssets(File gameDir, File indexFile, BiConsumer<String, Double> progress, AtomicBoolean cancel) throws IOException, InterruptedException {
        String json = new String(Files.readAllBytes(indexFile.toPath()), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject objects = root.getAsJsonObject("objects");
        if (objects == null) {
            return;
        }
        addAssetsTotal(objects);
        List<String> hashes = new ArrayList<>();
        for (java.util.Map.Entry<String, JsonElement> e : objects.entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            if (o.has("hash")) {
                hashes.add(o.get("hash").getAsString());
            }
        }
        File objectsDir = new File(gameDir, "assets" + File.separator + "objects");
        int done = 0;
        int total = hashes.size();
        if (stats != null) {
            stats.assetsTotal = total;
            stats.assetsDone = 0;
        }
        for (String hash : hashes) {
            if (cancelled(cancel)) {
                throw new IOException("Descarga cancelada por el usuario");
            }
            done++;
            if (stats != null) {
                stats.assetsDone = done;
            }
            File dest = new File(objectsDir, hash.substring(0, 2) + File.separator + hash);
            if (!dest.exists()) {
                if (done % 25 == 0 || done == total) {
                    step(progress, "Assets " + done + "/" + total, 0.05 + 0.35 * done / (double) total);
                }
                try {
                    dest.getParentFile().mkdirs();
                    dl(RESOURCES_URL + hash.substring(0, 2) + "/" + hash, dest.toPath(), null);
                } catch (Exception ex) {
                    // asset individual fallido: seguir con el resto
                }
            }
        }
    }

    private void downloadLibraries(File gameDir, String versionId, JsonObject root, BiConsumer<String, Double> progress, AtomicBoolean cancel) throws IOException, InterruptedException {
        JsonArray libraries = root.getAsJsonArray("libraries");
        if (libraries == null) {
            return;
        }
        String os = NativesExtractor.currentOs();
        List<JsonObject> wanted = new ArrayList<>();
        for (JsonElement el : libraries) {
            JsonObject lib = el.getAsJsonObject();
            if (!ruleAllows(lib, os)) {
                continue;
            }
            wanted.add(lib);
        }
        File nativesBase = new File(gameDir, "versions" + File.separator + versionId + File.separator + "natives");
        nativesBase.mkdirs();
        cleanDir(nativesBase);
        int done = 0;
        int total = wanted.size();
        if (stats != null) {
            stats.libsTotal = total;
            stats.libsDone = 0;
        }
        int natTotal = 0;
        for (JsonObject l0 : wanted) {
            if (l0.has("downloads") && l0.getAsJsonObject("downloads").has("classifiers")) {
                natTotal++;
            }
        }
        if (stats != null) {
            stats.nativesTotal = natTotal;
            stats.nativesDone = 0;
        }
        int natDone = 0;
        for (JsonObject lib : wanted) {
            if (cancelled(cancel)) {
                throw new IOException("Descarga cancelada por el usuario");
            }
            done++;
            if (stats != null) {
                stats.libsDone = done;
            }
            if (!lib.has("downloads")) {
                continue;
            }
            JsonObject dl = lib.getAsJsonObject("downloads");
            if (dl.has("artifact")) {
                JsonObject art = dl.getAsJsonObject("artifact");
                String path = art.get("path").getAsString();
                String url = art.get("url").getAsString();
                File dest = new File(new File(gameDir, "libraries"), path);
                if (!dest.exists() || dest.length() == 0) {
                    if (done % 10 == 0 || done == total) {
                        step(progress, "Librerias " + done + "/" + total, 0.4 + 0.5 * done / (double) total);
                    }
                    try {
                        dest.getParentFile().mkdirs();
                        dl(url, dest.toPath(), null);
                    } catch (Exception ex) {
                        throw new IOException("Fallo libreria " + path + ": " + ex.getMessage());
                    }
                }
            }
            if (dl.has("classifiers")) {
                JsonObject classifiers = dl.getAsJsonObject("classifiers");
                String key = nativesKey(classifiers, os);
                if (key != null) {
                    JsonObject nat = classifiers.getAsJsonObject(key);
                    String url = nat.get("url").getAsString();
                    String name = lib.get("name").getAsString().replace(":", "-").replace(".", "_") + "-" + key + ".jar";
                    File dest = new File(nativesBase, name);
                    if (!dest.exists() || dest.length() == 0) {
                        try {
                            dl(url, dest.toPath(), null);
                        } catch (Exception ex) {
                            throw new IOException("Fallo native " + name + ": " + ex.getMessage());
                        }
                    }
                    try {
                        NativesExtractor.extract(dest, nativesBase);
                    } catch (Exception ignored) {
                    }
                    natDone++;
                    if (stats != null) {
                        stats.nativesDone = natDone;
                    }
                }
            }
        }
    }

    private static void cleanDir(File dir) {
        try {
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                if (f.isDirectory()) {
                    cleanDir(f);
                }
                f.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean cancelled(AtomicBoolean cancel) {
        return cancel != null && cancel.get();
    }

    private static String nativesKey(JsonObject classifiers, String os) {
        if (os.equals("windows")) {
            if (classifiers.has("natives-windows-64")) {
                return "natives-windows-64";
            }
            if (classifiers.has("natives-windows-32")) {
                return "natives-windows-32";
            }
            if (classifiers.has("natives-windows")) {
                return "natives-windows";
            }
            return null;
        }
        if (os.equals("osx")) {
            if (classifiers.has("natives-macos")) {
                return "natives-macos";
            }
            if (classifiers.has("natives-osx")) {
                return "natives-osx";
            }
            return null;
        }
        if (classifiers.has("natives-linux")) {
            return "natives-linux";
        }
        return null;
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
            JsonObject osRule = rule.getAsJsonObject("os");
            String name = osRule.has("name") ? osRule.get("name").getAsString() : "";
            boolean match = os.equals(name) || (os.equals("windows") && name.equals("windows"))
                    || (os.equals("osx") && (name.equals("osx") || name.equals("macos")))
                    || (os.equals("linux") && name.equals("linux"));
            if (match) {
                action = ruleAction;
            }
        }
        return action.equals("allow");
    }

    private String findVersionUrl(String versionId) throws IOException, InterruptedException {
        List<McVersion> all = fetchManifest(true);
        for (McVersion v : all) {
            if (v.id.equals(versionId)) {
                return v.url;
            }
        }
        List<McVersion> fresh = fetchManifest(false);
        for (McVersion v : fresh) {
            if (v.id.equals(versionId)) {
                return v.url;
            }
        }
        return null;
    }

    private static void checkCancel(AtomicBoolean cancel) throws IOException {
        if (cancelled(cancel)) {
            throw new IOException("Descarga cancelada por el usuario");
        }
    }

    private static String sha1(File f) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] data = Files.readAllBytes(f.toPath());
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void step(BiConsumer<String, Double> progress, String msg, double frac) {
        if (progress != null) {
            try {
                progress.accept(msg, frac);
            } catch (Exception ignored) {
            }
        }
    }

    public List<String> missingParts(String versionId, File gameDir) {
        List<String> missing = new ArrayList<>();
        File versionDir = new File(gameDir, "versions" + File.separator + versionId);
        File json = new File(versionDir, versionId + ".json");
        if (!json.exists() || json.length() == 0) {
            missing.add("json de version");
            return missing;
        }
        File jar = new File(versionDir, versionId + ".jar");
        if (!jar.exists() || jar.length() == 0) {
            missing.add("cliente .jar");
        }
        try {
            String content = new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            JsonArray libraries = root.getAsJsonArray("libraries");
            String os = NativesExtractor.currentOs();
            if (libraries != null) {
                int missingLibs = 0;
                for (JsonElement el : libraries) {
                    JsonObject lib = el.getAsJsonObject();
                    if (!ruleAllows(lib, os) || !lib.has("downloads")) {
                        continue;
                    }
                    JsonObject dl = lib.getAsJsonObject("downloads");
                    if (dl.has("artifact")) {
                        String path = dl.getAsJsonObject("artifact").get("path").getAsString();
                        File f = new File(new File(gameDir, "libraries"), path);
                        if (!f.exists() || f.length() == 0) {
                            missingLibs++;
                        }
                    }
                }
                if (missingLibs > 0) {
                    missing.add(missingLibs + " librerias");
                }
            }
            File nativesDir = new File(gameDir, "versions" + File.separator + versionId + File.separator + "natives");
            if (!hasDll(nativesDir)) {
                missing.add("natives");
            }
        } catch (Exception e) {
            missing.add("json ilegible");
        }
        return missing;
    }

    public void ensureNatives(String versionId, File gameDir, BiConsumer<String, Double> progress) throws IOException, InterruptedException {
        File nativesDir = new File(gameDir, "versions" + File.separator + versionId + File.separator + "natives");
        nativesDir.mkdirs();
        if (hasDll(nativesDir)) {
            return;
        }
        step(progress, "Preparando natives de " + versionId, 0.0);
        File versionJson = new File(gameDir, "versions" + File.separator + versionId + File.separator + versionId + ".json");
        if (!versionJson.exists()) {
            throw new IOException("Falta el JSON de " + versionId);
        }
        String json = new String(Files.readAllBytes(versionJson.toPath()), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray libraries = root.getAsJsonArray("libraries");
        if (libraries == null) {
            throw new IOException("Sin librerias en el JSON de " + versionId);
        }
        String os = NativesExtractor.currentOs();
        File jarDir = new File(nativesDir.getParentFile(), "natives-jars");
        jarDir.mkdirs();
        File sharedNatives = new File(gameDir, "natives");
        List<File> jars = new ArrayList<>();
        for (JsonElement el : libraries) {
            JsonObject lib = el.getAsJsonObject();
            if (!ruleAllows(lib, os) || !lib.has("downloads")) {
                continue;
            }
            JsonObject dl = lib.getAsJsonObject("downloads");
            if (!dl.has("classifiers")) {
                continue;
            }
            String key = nativesKey(dl.getAsJsonObject("classifiers"), os);
            if (key == null) {
                continue;
            }
            JsonObject nat = dl.getAsJsonObject("classifiers").getAsJsonObject(key);
            String url = nat.get("url").getAsString();
            String name = lib.get("name").getAsString().replace(":", "-").replace(".", "_") + "-" + key + ".jar";
            File dest = new File(jarDir, name);
            File shared = new File(sharedNatives, name);
            if ((!dest.exists() || dest.length() == 0) && shared.exists() && shared.length() > 0) {
                try {
                    Files.copy(shared.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                }
            }
            if (!dest.exists() || dest.length() == 0) {
                step(progress, "Descargando native " + name, 0.5);
                dl(url, dest.toPath(), null);
            }
            jars.add(dest);
        }
        if (jars.isEmpty()) {
            throw new IOException("No se encontraron natives para " + os + " en " + versionId);
        }
        cleanDir(nativesDir);
        for (File j : jars) {
            NativesExtractor.extract(j, nativesDir);
        }
        if (!hasDll(nativesDir)) {
            throw new IOException("No se pudo extraer ninguna DLL para " + versionId);
        }
        step(progress, "Natives listos", 1.0);
    }

    private static boolean hasDll(File dir) {
        try {
            File[] files = dir.listFiles();
            if (files == null) {
                return false;
            }
            for (File f : files) {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".dll") || n.endsWith(".so") || n.endsWith(".dylib") || n.endsWith(".jnilib")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public Path downloadVersionJson(String versionId, File gameDir) throws IOException, InterruptedException {
        String url = findVersionUrl(versionId);
        if (url == null) {
            throw new IOException("Version desconocida: " + versionId);
        }
        File versionDir = new File(gameDir, "versions" + File.separator + versionId);
        versionDir.mkdirs();
        Path dest = new File(versionDir, versionId + ".json").toPath();
        dl(url, dest, null);
        return dest;
    }
}