package com.ragsmc.launcher.minecraft;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class JavaDownloader {

    private JavaDownloader() {
    }

    public static File runtimesDir() {
        String appdata = System.getenv("APPDATA");
        File base = appdata != null && !appdata.isEmpty()
                ? new File(appdata, "RagsMC" + File.separator + "runtimes")
                : new File(System.getProperty("user.home"), ".ragsmc" + File.separator + "runtimes");
        base.mkdirs();
        return base;
    }

    public static File installedJava(int major) {
        File dir = new File(runtimesDir(), "java-" + major);
        if (!dir.isDirectory()) {
            return null;
        }
        File found = findJavaExe(dir);
        if (found == null) {
            return null;
        }
        return JavaVersionSelector.majorOf(found.getAbsolutePath()) == major ? found : null;
    }

    public static File downloadJava(int major, BiConsumer<Long, Long> progress) throws IOException, InterruptedException {
        File existing = installedJava(major);
        if (existing != null) {
            return existing;
        }
        String os = System.getProperty("os.name", "").toLowerCase().contains("win") ? "windows" : "linux";
        String arch = System.getProperty("os.arch", "").toLowerCase().contains("64") ? "x64" : "x86";
        String url = "https://api.adoptium.net/v3/binary/latest/" + major + "/ga/" + os + "/" + arch + "/jre/hotspot/normal/eclipse";
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMinutes(5)).header("User-Agent", "RagsMC-Launcher/2.0").GET().build();
        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        int redirectHops = 0;
        while ((resp.statusCode() == 301 || resp.statusCode() == 302 || resp.statusCode() == 303 || resp.statusCode() == 307 || resp.statusCode() == 308) && redirectHops < 5) {
            String location = resp.headers().firstValue("Location").orElse(null);
            if (location == null || location.isEmpty()) {
                break;
            }
            try {
                resp.body().close();
            } catch (Exception ignored) {
            }
            URI nextUri = URI.create(url).resolve(location);
            HttpRequest nextReq = HttpRequest.newBuilder().uri(nextUri).timeout(Duration.ofMinutes(5)).header("User-Agent", "RagsMC-Launcher/2.0").GET().build();
            resp = client.send(nextReq, HttpResponse.BodyHandlers.ofInputStream());
            redirectHops++;
        }
        if (resp.statusCode() != 200) {
            try {
                resp.body().close();
            } catch (Exception ignored) {
            }
            throw new IOException("Adoptium HTTP " + resp.statusCode() + " para Java " + major);
        }
        long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
        File tmp = File.createTempFile("ragsmc-java-" + major + "-", ".zip");
        try (InputStream in = resp.body()) {
            try (OutputStream out = Files.newOutputStream(tmp.toPath())) {
                byte[] buf = new byte[32768];
                long done = 0;
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    done += n;
                    if (progress != null) {
                        try {
                            progress.accept(done, total);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        File dest = new File(runtimesDir(), "java-" + major);
        deleteRecursive(dest);
        dest.mkdirs();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(tmp.toPath()))) {
            ZipEntry entry;
            byte[] buf = new byte[32768];
            while ((entry = zip.getNextEntry()) != null) {
                File out = new File(dest, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    out.getParentFile().mkdirs();
                    try (OutputStream o = Files.newOutputStream(out.toPath())) {
                        int n;
                        while ((n = zip.read(buf)) != -1) {
                            o.write(buf, 0, n);
                        }
                    }
                }
                zip.closeEntry();
            }
        } finally {
            tmp.delete();
        }
        File javaExe = findJavaExe(dest);
        if (javaExe == null) {
            throw new IOException("No se encontro java.exe en el JRE descargado");
        }
        return javaExe;
    }

    private static File findJavaExe(File dir) {
        File direct = new File(dir, "bin" + File.separator + "java" + (isWindows() ? ".exe" : ""));
        if (direct.isFile()) {
            return direct;
        }
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) {
            return null;
        }
        for (File sub : subs) {
            File found = findJavaExe(sub);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    deleteRecursive(k);
                }
            }
        }
        f.delete();
    }
}