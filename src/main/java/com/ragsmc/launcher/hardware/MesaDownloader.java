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

    // TODO(fase-7): Configurar URL y SHA-256 del release de Mesa3D
    // Subir mesa3d-opengl32-x64.zip (con opengl32.dll, libgallium_wgl.dll, libglapi.dll) a un release de GitHub
    // y poner aqui la URL y el hash SHA-256
    private static final String MESA_URL = "";
    private static final String MESA_SHA256 = "";

    private static final File MESA_DIR = new File(
        System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home"),
        "RagsMC" + File.separator + "mesa3d"
    );

    private MesaDownloader() {
    }

    public static File download(ProgressCallback callback) throws IOException, InterruptedException {
        if (MESA_URL.isEmpty() || MESA_SHA256.isEmpty()) {
            throw new IllegalStateException("Mesa no configurado: MESA_URL y MESA_SHA256 deben estar definidos en MesaDownloader.java");
        }

        File zipFile = new File(MESA_DIR, "mesa3d-opengl32-x64.zip");
        MESA_DIR.mkdirs();

        // Descargar con progreso
        downloadWithProgress(MESA_URL, zipFile, callback);

        // Verificar SHA-256
        String actualSha = calculateSha256(zipFile);
        if (!actualSha.equalsIgnoreCase(MESA_SHA256)) {
            Files.deleteIfExists(zipFile.toPath());
            throw new IOException("SHA-256 de Mesa no coincide. Esperado: " + MESA_SHA256 + ", actual: " + actualSha);
        }

        // Extraer 3 DLLs
        List<String> requiredDlls = List.of("opengl32.dll", "libgallium_wgl.dll", "libglapi.dll");
        extractDlls(zipFile, requiredDlls);

        if (callback != null) {
            callback.onProgress("Mesa descargado y verificado", 1.0);
        }
        return MESA_DIR;
    }

    private static void downloadWithProgress(String url, File dest, ProgressCallback callback) throws IOException, InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofMinutes(5))
                .header("User-Agent", "RagsMC-Launcher/1.0")
                .GET()
                .build();

        java.net.http.HttpResponse<InputStream> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " descargando Mesa");
        }

        long total = response.headers().firstValue("Content-Length").map(Long::parseLong).orElse(-1L);
        long downloaded = 0;
        byte[] buffer = new byte[8192];
        try (InputStream in = response.body(); FileOutputStream out = new FileOutputStream(dest)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;
                if (callback != null && total > 0) {
                    callback.onProgress("Descargando Mesa... " + (downloaded * 100 / total) + "%", (double) downloaded / total);
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
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                String baseName = new File(name).getName().toLowerCase();
                if (requiredDlls.contains(baseName)) {
                    File outFile = new File(MESA_DIR, baseName);
                    Files.copy(zis, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
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
        String[] dlls = {"opengl32.dll", "libgallium_wgl.dll", "libglapi.dll"};
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
        String[] dlls = {"opengl32.dll", "libgallium_wgl.dll", "libglapi.dll"};
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
        return !MESA_URL.isEmpty() && !MESA_SHA256.isEmpty();
    }

    public static File getMesaDir() {
        return MESA_DIR;
    }

    public static boolean isMesaAvailable() {
        String[] dlls = {"opengl32.dll", "libgallium_wgl.dll", "libglapi.dll"};
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
