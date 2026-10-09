package com.ragsmc.launcher.minecraft;

import com.ragsmc.launcher.LaunchConfig;
import com.ragsmc.launcher.hardware.GpuDedicatedHelper;
import com.ragsmc.launcher.auth.AuthlibPatcher;
import com.ragsmc.launcher.auth.OfflineUuid;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MinecraftLauncher {

    private MinecraftLauncher() {
    }

    public static Process launch(LaunchConfig config) throws IOException {
        return launch(config, null);
    }

    public static Process launch(LaunchConfig config, Consumer<String> log) throws IOException {
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            throw new IOException("Falta el nombre de usuario");
        }
        if (config.getVersion() == null || config.getVersion().isEmpty()) {
            throw new IOException("Falta la version de Minecraft");
        }
        if (config.getJavaPath() == null || config.getJavaPath().isEmpty()) {
            throw new IOException("Falta la ruta de Java");
        }
        File shared = sharedDirOf(config);
        File gameDir = new File(config.getGameDir());
        gameDir.mkdirs();
        File versionDir = new File(shared, "versions" + File.separator + config.getVersion());
        File versionJson = new File(versionDir, config.getVersion() + ".json");
        if (!versionJson.exists()) {
            throw new IOException("La version " + config.getVersion() + " no esta instalada en versions/ (" + versionDir.getAbsolutePath() + ")");
        }
        try {
            AuthlibPatcher.patchIfNeeded(config.getVersion(), versionJson);
        } catch (Exception e) {
            info(log, "Aviso: no se pudo parchear authlib: " + e.getMessage());
        }
        String username = config.getUsername().trim();
        String uuid = config.getUuid() != null && !config.getUuid().isEmpty()
                ? config.getUuid()
                : OfflineUuid.generate(username);
        String accessToken = config.getAccessToken() != null ? config.getAccessToken() : "null";
        String userType = config.getUserType() != null ? config.getUserType() : "mojang";
        String assetIndex = config.getAssetIndex() != null ? config.getAssetIndex() : config.getVersion();
        String clientId = config.getClientId() != null ? config.getClientId() : "0";
        String authSession = "token:" + accessToken + ":" + uuid;
        File nativesDir = new File(shared, "versions" + File.separator + config.getVersion() + File.separator + "natives");
        nativesDir.mkdirs();
        String natives = nativesDir.getAbsolutePath();
        int ram = config.getMemory() > 0 ? config.getMemory() : 2048;
        List<String> args = new ArrayList<>();
        args.add(config.getJavaPath());
        args.add("-Xmx" + ram + "M");
        args.add("-Xms512M");
        args.add("-XX:+UseG1GC");
        args.add("-XX:-UseAdaptiveSizePolicy");
        args.add("-XX:G1HeapRegionSize=4M");
        args.add("-Djava.library.path=" + natives);
        args.add("-Djna.tmpdir=" + natives);
        args.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + natives);
        args.add("-Dio.netty.native.workdir=" + natives);
        args.add("-Dminecraft.launcher.brand=minecraft-launcher");
        args.add("-Dminecraft.launcher.version=2.3.173");
        if (config.getJvmArgs() != null && !config.getJvmArgs().trim().isEmpty()) {
            for (String a : config.getJvmArgs().trim().split(" ")) {
                if (!a.isEmpty()) {
                    args.add(a);
                }
            }
        }
        args.add("-cp");
        args.add(ClasspathBuilder.build(config));
        args.add(ClasspathBuilder.mainClass(config.getLoader()));
        args.add("--username");
        args.add(username);
        args.add("--version");
        args.add(config.getVersion());
        args.add("--gameDir");
        args.add(gameDir.getAbsolutePath());
        args.add("--assetsDir");
        args.add(new File(shared, "assets").getAbsolutePath());
        args.add("--assetIndex");
        args.add(assetIndex);
        args.add("--uuid");
        args.add(uuid);
        args.add("--accessToken");
        args.add(accessToken);
        args.add("--userType");
        args.add(userType);
        args.add("--versionType");
        args.add("release");
        args.add("--xuid");
        args.add("0");
        args.add("--clientId");
        args.add(clientId);
        args.add("--auth_session");
        args.add(authSession);
        args.add("--userProperties");
        args.add("{}");
        if (config.getGameArgs() != null && !config.getGameArgs().trim().isEmpty()) {
            for (String a : config.getGameArgs().trim().split(" ")) {
                if (!a.isEmpty()) {
                    args.add(a);
                }
            }
        }
        if (config.getServer() != null && !config.getServer().isEmpty()) {
            args.add("--server");
            args.add(config.getServer());
            args.add("--port");
            args.add(String.valueOf(config.getServerPort() != null ? config.getServerPort() : 25565));
        }
        info(log, "Lanzando: " + String.join(" ", args));
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(gameDir);
        pb.redirectErrorStream(true);

        // Apply GPU/Mesa settings
        if (config.isMesaCpu()) {
            info(log, "[Launch] Modo Mesa activado");
            // Verificar que las 3 DLLs esten junto al Java usado; si falta alguna, re-copiar
            try {
                File javaExe = new File(config.getJavaPath());
                File javaDir = javaExe.getParentFile();
                File mesaDir = com.ragsmc.launcher.hardware.MesaDownloader.getMesaDir();
                if (javaDir != null) {
                    boolean faltaAlguna = false;
                    for (String dll : com.ragsmc.launcher.hardware.MesaDownloader.REQUIRED_DLLS) {
                        if (!new File(javaDir, dll).exists()) {
                            faltaAlguna = true;
                            break;
                        }
                    }
                    if (faltaAlguna) {
                        info(log, "[Launch] Re-copiando DLLs de Mesa junto a " + javaDir.getAbsolutePath());
                        int n = com.ragsmc.launcher.hardware.MesaDownloader.copyDllsTo(javaDir, mesaDir);
                        info(log, "[Launch] DLLs de Mesa copiadas: " + n + "/3");
                    }
                }
            } catch (Exception e) {
                info(log, "[Launch] Aviso Mesa: " + e.getMessage());
            }
            // Apply Mesa environment variables
            pb.environment().put("GALLIUM_DRIVER", "llvmpipe");
            pb.environment().put("LIBGL_ALWAYS_SOFTWARE", "1");
            info(log, "[Launch] Aplicando modo Mesa (software rendering)");
        }

        // On Windows, GPU registry is already applied via GpuDedicatedHelper.enableForJava
        // No additional env vars needed for GPU preference on Windows

        return pb.start();
    }

    public static File sharedDirOf(LaunchConfig config) {
        if (config.getSharedDir() != null && !config.getSharedDir().isEmpty()) {
            return new File(config.getSharedDir());
        }
        return new File(config.getGameDir());
    }

    public static void pipeToConsole(Process process, Consumer<String> line) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String s;
                while ((s = r.readLine()) != null) {
                    try {
                        line.accept(s);
                    } catch (Exception ignored) {
                    }
                }
            } catch (IOException ignored) {
            }
        }, "mc-console-pipe");
        t.setDaemon(true);
        t.start();
    }

    private static void info(Consumer<String> log, String msg) {
        if (log != null) {
            try {
                log.accept(msg);
            } catch (Exception ignored) {
            }
        }
    }
}