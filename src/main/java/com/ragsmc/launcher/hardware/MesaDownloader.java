package com.ragsmc.launcher.hardware;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MesaDownloader {

    // Release oficial pal1000/mesa-dist-win (version fija, NO "latest").
    // Verificado el 2026-10-09: tamano 70257286 bytes, SHA-256 coincide con el digest oficial de GitHub
    // y el .7z contiene x64/opengl32.dll + x64/libgallium_wgl.dll (extraccion probada con 7-Zip).
    // NOTA: los releases actuales (~25.x en adelante) YA NO traen libglapi.dll; el driver WGL por
    // software funciona solo con opengl32.dll + libgallium_wgl.dll.
    private static final String MESA_7Z_URL =
        "https://github.com/pal1000/mesa-dist-win/releases/download/26.2.4/mesa3d-26.2.4-release-msvc.7z";
    private static final String MESA_7Z_SHA256 =
        "351fc8c8b695878ffb3eaa044b3ead08672a48b1a045e3c3e3975811df0f6695";
    // URL del ZIP propio (flujo anterior). Si algun dia se sube mesa3d-opengl32-x64.zip a un
    // release de RagsMC, poner aqui URL y SHA y usar downloadZip().
    private static final String MESA_URL = "";
    private static final String MESA_SHA256 = "";
    public static final String[] REQUIRED_DLLS = {
        "opengl32.dll",
        "libgallium_wgl.dll"
    };
    private static final String SEVENZIP_FALLBACK_URL = "https://www.7-zip.org/a/7zr.exe";

    private static final File MESA_DIR = new File(
        System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home"),
        "RagsMC" + File.separator + "mesa3d"
    );

    private MesaDownloader() {
    }

    public static File download(ProgressCallback callback) throws IOException, InterruptedException {
        System.out.println("[Mesa] Iniciando descarga");
        // 1. Early-return si ya esta instalado
        if (isMesaAvailable()) {
            System.out.println("[Mesa] Ya instalado en " + MESA_DIR.getAbsolutePath());
            if (callback != null) {
                callback.onProgress("Mesa ya instalado", 1.0);
            }
            return MESA_DIR;
        }
        // 2. Buscar 7-Zip; si no esta, descargar 7zr.exe portable
        File sevenZip = find7Zip();
        if (sevenZip == null) {
            if (callback != null) {
                callback.onProgress("Descargando 7-Zip...", 0.02);
            }
            System.out.println("[Mesa] 7-Zip no encontrado, descargando 7zr.exe portable...");
            sevenZip = download7zr(callback);
        } else {
            System.out.println("[Mesa] 7-Zip encontrado en " + sevenZip.getAbsolutePath());
        }
        MESA_DIR.mkdirs();
        // 3. Descargar .7z de Mesa
        System.out.println("[Mesa] Descargando mesa3d-26.2.4-release-msvc.7z");
        File archive = new File(MESA_DIR, "mesa3d.7z");
        final ProgressCallback outerCb = callback;
        try {
            downloadWithProgress(MESA_7Z_URL, archive, outerCb == null ? null : (msg, frac) -> {
                double mapped = frac < 0 ? 0.3 : 0.1 + frac * 0.5;
                outerCb.onProgress(msg, mapped);
            });
        } catch (RuntimeException e) {
            Files.deleteIfExists(archive.toPath());
            throw e;
        }
        // 4. Verificar SHA-256
        if (outerCb != null) {
            outerCb.onProgress("Verificando SHA-256...", 0.62);
        }
        System.out.println("[Mesa] Verificando SHA-256...");
        String actualSha = calculateSha256(archive);
        if (!actualSha.equalsIgnoreCase(MESA_7Z_SHA256)) {
            Files.deleteIfExists(archive.toPath());
            throw new IOException("SHA-256 no coincide. Archivo corrupto o release modificado.");
        }
        System.out.println("[Mesa] SHA-256 OK");
        // 5. Extraer las DLLs
        if (outerCb != null) {
            outerCb.onProgress("Extrayendo DLLs...", 0.8);
        }
        System.out.println("[Mesa] Extrayendo DLLs...");
        extractDllsFrom7z(sevenZip, archive, MESA_DIR);
        // 6. Borrar .7z
        Files.deleteIfExists(archive.toPath());
        if (outerCb != null) {
            outerCb.onProgress("Mesa instalado correctamente", 1.0);
        }
        System.out.println("[Mesa] Mesa instalado correctamente en " + MESA_DIR.getAbsolutePath());
        return MESA_DIR;
    }

    /** Flujo anterior con ZIP propio (requiere MESA_URL/MESA_SHA256). Se mantiene por compatibilidad. */
    public static File downloadZip(ProgressCallback callback) throws IOException, InterruptedException {
        if (!isMesaConfigured()) {
            throw new IllegalStateException("Mesa no configurado: MESA_URL y MESA_SHA256 deben estar definidos en MesaDownloader.java");
        }

        // 1. Si ya esta descargado, no descargar de nuevo
        if (isMesaAvailable()) {
            System.out.println("[Mesa] Ya instalado en " + MESA_DIR.getAbsolutePath());
            if (callback != null) {
                callback.onProgress("Mesa ya instalado", 1.0);
            }
            return MESA_DIR;
        }

        System.out.println("[Mesa] Iniciando descarga desde " + MESA_URL);
        File zipFile = new File(MESA_DIR, "mesa3d-opengl32-x64.zip");
        MESA_DIR.mkdirs();

        // 2. Descargar ZIP con progreso
        downloadWithProgress(MESA_URL, zipFile, callback);

        // 3. Verificar SHA-256
        System.out.println("[Mesa] Descarga completa. Verificando SHA-256...");
        String actualSha = calculateSha256(zipFile);
        if (!actualSha.equalsIgnoreCase(MESA_SHA256)) {
            Files.deleteIfExists(zipFile.toPath());
            throw new IOException("SHA-256 no coincide. Archivo corrupto o URL incorrecta.");
        }
        System.out.println("[Mesa] SHA-256 OK");

        // 4. Extraer las 3 DLLs
        System.out.println("[Mesa] Extrayendo 3 DLLs...");
        extractDlls(zipFile, java.util.Arrays.asList(REQUIRED_DLLS));

        // 5. Borrar ZIP
        Files.deleteIfExists(zipFile.toPath());

        if (callback != null) {
            callback.onProgress("Mesa descargado y verificado", 1.0);
        }
        System.out.println("[Mesa] Mesa activado correctamente en " + MESA_DIR.getAbsolutePath());
        return MESA_DIR;
    }

    public static File find7Zip() {
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String[] paths = {
            "C:\\Program Files\\7-Zip\\7z.exe",
            "C:\\Program Files (x86)\\7-Zip\\7z.exe",
            programFiles != null ? programFiles + "\\7-Zip\\7z.exe" : null,
            programFilesX86 != null ? programFilesX86 + "\\7-Zip\\7z.exe" : null
        };
        for (String path : paths) {
            if (path == null) {
                continue;
            }
            File f = new File(path);
            if (f.isFile()) {
                return f;
            }
        }
        // Copia portable descargada antes por nosotros
        File portable = new File(MESA_DIR.getParentFile(), "tools" + File.separator + "7zr.exe");
        if (portable.isFile()) {
            return portable;
        }
        return null;
    }

    private static File download7zr(ProgressCallback callback) throws IOException, InterruptedException {
        File toolsDir = new File(MESA_DIR.getParentFile(), "tools");
        toolsDir.mkdirs();
        File dest = new File(toolsDir, "7zr.exe");
        if (dest.isFile() && dest.length() > 100000) {
            System.out.println("[Mesa] Reusando 7zr.exe descargado antes");
            return dest;
        }
        System.out.println("[Mesa] Descargando 7zr.exe portable desde " + SEVENZIP_FALLBACK_URL);
        final ProgressCallback outerCb = callback;
        downloadWithProgress(SEVENZIP_FALLBACK_URL, dest, outerCb == null ? null : (msg, frac) -> {
            outerCb.onProgress("Descargando 7-Zip portable... " + msg, frac < 0 ? 0.02 : frac * 0.05);
        });
        if (!dest.isFile() || dest.length() < 100000) {
            Files.deleteIfExists(dest.toPath());
            throw new IOException("No se pudo descargar 7-Zip. Descargalo manual desde https://www.7-zip.org/download.html");
        }
        System.out.println("[Mesa] 7zr.exe listo en " + dest.getAbsolutePath());
        return dest;
    }

    private static void extractDllsFrom7z(File sevenZip, File archive7z, File destDir) throws IOException {
        destDir.mkdirs();
        File tempDir = new File(destDir, "tmp_extract");
        deleteRecursiveQuiet(tempDir);
        tempDir.mkdirs();
        try {
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(sevenZip.getAbsolutePath());
            cmd.add("x");
            cmd.add(archive7z.getAbsolutePath());
            cmd.add("-o" + tempDir.getAbsolutePath());
            cmd.add("-y");
            for (String dll : REQUIRED_DLLS) {
                cmd.add("x64/" + dll);
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            byte[] out = p.getInputStream().readAllBytes();
            boolean done;
            try {
                done = p.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Extraccion de Mesa interrumpida");
            }
            if (!done) {
                p.destroyForcibly();
                throw new IOException("7-Zip tardo demasiado (timeout 10 min)");
            }
            if (p.exitValue() != 0) {
                throw new IOException("7-Zip fallo con codigo " + p.exitValue() + ": " + new String(out));
            }
            File x64Dir = new File(tempDir, "x64");
            int ok = 0;
            for (String dll : REQUIRED_DLLS) {
                File src = new File(x64Dir, dll);
                if (!src.isFile()) {
                    throw new IOException("Falta " + dll + " en el .7z de Mesa");
                }
                Files.copy(src.toPath(), new File(destDir, dll).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[Mesa] Extraido: " + dll);
                ok++;
            }
            if (ok != REQUIRED_DLLS.length) {
                throw new IOException("Solo se extrajeron " + ok + "/" + REQUIRED_DLLS.length + " DLLs");
            }
        } finally {
            deleteRecursiveQuiet(tempDir);
        }
    }

    private static void deleteRecursiveQuiet(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    deleteRecursiveQuiet(k);
                }
            }
        }
        try {
            Files.deleteIfExists(f.toPath());
        } catch (IOException ignored) {
        }
    }

    private static void downloadWithProgress(String url, File dest, ProgressCallback callback) throws IOException, InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofMinutes(10))
                .header("User-Agent", "RagsMC-Launcher/2.0")
                .GET()
                .build();

        java.net.http.HttpResponse<InputStream> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
        int hops = 0;
        while ((response.statusCode() == 301 || response.statusCode() == 302 || response.statusCode() == 303
                || response.statusCode() == 307 || response.statusCode() == 308) && hops < 5) {
            String location = response.headers().firstValue("Location").orElse(null);
            if (location == null || location.isEmpty()) {
                break;
            }
            try {
                response.body().close();
            } catch (Exception ignored) {
            }
            java.net.URI nextUri = java.net.URI.create(url).resolve(location);
            java.net.http.HttpRequest nextReq = java.net.http.HttpRequest.newBuilder()
                    .uri(nextUri)
                    .timeout(java.time.Duration.ofMinutes(10))
                    .header("User-Agent", "RagsMC-Launcher/2.0")
                    .GET()
                    .build();
            response = client.send(nextReq, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
            hops++;
        }
        if (response.statusCode() != 200) {
            try {
                response.body().close();
            } catch (Exception ignored) {
            }
            throw new IOException("HTTP " + response.statusCode() + " descargando Mesa. Revisa tu conexion a internet.");
        }

        long total = -1L;
        try {
            total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        } catch (Exception ignored) {
        }
        long downloaded = 0;
        long startNanos = System.nanoTime();
        byte[] buffer = new byte[32768];
        try (InputStream in = response.body(); FileOutputStream out = new FileOutputStream(dest)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;
                if (callback != null) {
                    double frac = total > 0 ? (double) downloaded / total : -1.0;
                    double elapsedSec = (System.nanoTime() - startNanos) / 1_000_000_000.0;
                    double speedMBs = elapsedSec > 0 ? (downloaded / 1048576.0) / elapsedSec : 0.0;
                    String eta = "";
                    if (total > 0 && speedMBs > 0.01) {
                        long etaSec = (long) ((total - downloaded) / 1048576.0 / speedMBs);
                        eta = ", quedan " + etaSec + "s";
                    }
                    double mbDone = downloaded / 1048576.0;
                    if (total > 0) {
                        long pct = downloaded * 100 / total;
                        callback.onProgress("Descargando Mesa3D... " + pct + "% (" + String.format("%.1f", speedMBs) + " MB/s" + eta + ")", frac);
                        if (pct % 25 == 0) {
                            System.out.println("[Mesa] Progreso: " + pct + "% (" + String.format("%.1f", speedMBs) + " MB/s" + eta + ")");
                        }
                    } else {
                        callback.onProgress("Descargando Mesa3D... " + String.format("%.1f", mbDone) + " MB (" + String.format("%.1f", speedMBs) + " MB/s)", -1.0);
                    }
                }
            }
        }
    }

    private static String calculateSha256(File file) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IOException("SHA-256 no disponible", e);
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    private static void extractDlls(File zipFile, List<String> requiredDlls) throws IOException {
        java.util.Set<String> extracted = new java.util.HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    String baseName = new File(name).getName().toLowerCase();
                    if (requiredDlls.contains(baseName)) {
                        File outFile = new File(MESA_DIR, baseName);
                        Files.copy(zis, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        extracted.add(baseName);
                        System.out.println("[Mesa] Extraido: " + baseName);
                    }
                } finally {
                    zis.closeEntry();
                }
            }
        }
        for (String dll : requiredDlls) {
            if (!extracted.contains(dll) && !new File(MESA_DIR, dll).exists()) {
                throw new IOException("Falta " + dll + " en el ZIP de Mesa");
            }
        }
    }

    /** Copia las 3 DLLs a un directorio de Java. Devuelve cuantas copio. */
    public static int copyDllsTo(File javaDir, File mesaDir) {
        int copied = 0;
        if (javaDir == null || mesaDir == null) {
            return 0;
        }
        for (String dll : REQUIRED_DLLS) {
            File src = new File(mesaDir, dll);
            if (!src.exists()) {
                System.err.println("[Mesa] Falta en " + mesaDir.getAbsolutePath() + ": " + dll);
                continue;
            }
            try {
                Files.copy(src.toPath(), new File(javaDir, dll).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                System.err.println("[Mesa] Error copiando " + dll + ": " + e.getMessage());
            }
        }
        return copied;
    }

    /** Desinstala Mesa: borra DLLs de cada Java y la carpeta mesa3d. */
    public static void uninstall() throws IOException {
        System.out.println("[Mesa] Desinstalando...");
        removeFromAllJava();
        try {
            if (MESA_DIR.exists()) {
                java.util.Comparator<java.nio.file.Path> rev = java.util.Comparator.reverseOrder();
                try (java.util.stream.Stream<java.nio.file.Path> walk = Files.walk(MESA_DIR.toPath())) {
                    for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) walk.sorted(rev)::iterator) {
                        Files.deleteIfExists(p);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("No se pudo borrar " + MESA_DIR.getAbsolutePath() + ": " + e.getMessage());
        }
        System.out.println("[Mesa] Desinstalado correctamente");
    }

    public static boolean applyToJava(String javaExePath) {
        if (!isWindows()) {
            return false;
        }
        File javaExe = new File(javaExePath);
        File javaDir = javaExe.getParentFile();
        if (javaDir == null) {
            return false;
        }
        // Copiar las 3 DLLs junto a javaw.exe
        String[] dlls = REQUIRED_DLLS;
        boolean anyCopied = false;
        for (String dll : dlls) {
            File src = new File(MESA_DIR, dll);
            File dst = new File(javaDir, dll);
            if (src.exists()) {
                try {
                    Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    anyCopied = true;
                    System.out.println("[Mesa] Copiado " + dll + " a " + javaDir.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("[Mesa] Error copiando " + dll + " a " + javaDir + ": " + e.getMessage());
                }
            }
        }
        if (anyCopied) {
            System.out.println("[Mesa] Aplicado a: " + javaExePath);
        } else {
            System.err.println("[Mesa] No se encontraron DLLs en " + MESA_DIR.getAbsolutePath());
        }
        return anyCopied;
    }

    public static void removeFromAllJava() {
        if (!isWindows()) {
            return;
        }
        java.util.Map<Integer, String> found = com.ragsmc.launcher.minecraft.JavaVersionSelector.scanAll();
        System.out.println("[Mesa] Removiendo de " + found.size() + " JVMs...");
        int count = 0;
        for (String path : found.values()) {
            removeFromJava(path);
            count++;
        }
        System.out.println("[Mesa] Total: " + count + " JVMs limpiadas");
    }

    public static void removeFromJava(String javaExePath) {
        if (!isWindows()) {
            return;
        }
        File javaExe = new File(javaExePath);
        File javaDir = javaExe.getParentFile();
        if (javaDir == null) {
            return;
        }
        String[] dlls = REQUIRED_DLLS;
        for (String dll : dlls) {
            File dst = new File(javaDir, dll);
            if (dst.exists()) {
                try {
                    Files.deleteIfExists(dst.toPath());
                    System.out.println("[Mesa] Removido " + dll + " de " + javaDir.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("[Mesa] Error removiendo " + dll + ": " + e.getMessage());
                }
            }
        }
    }

    public static boolean isConfigured() {
        return isMesaConfigured();
    }

    public static boolean isMesaConfigured() {
        return MESA_URL != null && !MESA_URL.isEmpty()
                && MESA_SHA256 != null && !MESA_SHA256.isEmpty()
                && !MESA_URL.contains("<") && !MESA_SHA256.contains("<");
    }

    public static File getMesaDir() {
        return MESA_DIR;
    }

    public static boolean isMesaAvailable() {
        String[] dlls = REQUIRED_DLLS;
        for (String dll : dlls) {
            if (!new File(MESA_DIR, dll).exists()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public interface ProgressCallback {
        void onProgress(String message, double fraction);
    }
}
