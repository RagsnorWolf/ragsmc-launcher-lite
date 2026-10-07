package com.ragsmc.launcher;

import com.ragsmc.launcher.auth.Account;
import com.ragsmc.launcher.auth.AuthManager;
import com.ragsmc.launcher.auth.OfflineUuid;
import com.ragsmc.launcher.config.NewsManager;
import com.ragsmc.launcher.config.Settings;
import com.ragsmc.launcher.config.SettingsPersistence;
import com.ragsmc.launcher.minecraft.InstanceStore;
import com.ragsmc.launcher.minecraft.JavaVersionSelector;
import com.ragsmc.launcher.minecraft.MinecraftLauncher;
import com.ragsmc.launcher.minecraft.VersionManager;
import com.ragsmc.launcher.mods.ModrinthAPI;
import com.ragsmc.launcher.ui.ConsoleView;
import com.ragsmc.launcher.ui.MainWindow;
import com.ragsmc.launcher.ui.theme.Colors;
import com.ragsmc.launcher.util.AppDirs;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import static java.awt.Component.LEFT_ALIGNMENT;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class Launcher {

    private final MainWindow window;
    private final SettingsPersistence settingsPersistence;
    private final AuthManager auth;
    private final ModrinthAPI modrinth;
    private final InstanceStore instances;
    private final VersionManager versions;

    public Launcher(MainWindow window) {
        this.window = window;
        this.settingsPersistence = new SettingsPersistence();
        this.auth = new AuthManager(settingsPersistence);
        this.modrinth = new ModrinthAPI();
        this.instances = new InstanceStore(AppDirs.instancesFile());
        this.versions = new VersionManager(AppDirs.cacheDir());
    }

    public void wire() {
        scanJavaAtStartup();
        scanVersionsAtStartup();
        wireProfiles();
        wirePlay();
        wireHome();
        wireMods();
        wireSettings();
        wireContentDirs();
        wireInstallations();
        refreshInstallations();
    }

    private static File jdkCacheFile() {
        return new File(AppDirs.dataDir(), "jdks.json");
    }

    private void applyJdkCache() {
        try {
            File f = jdkCacheFile();
            if (!f.exists()) {
                return;
            }
            if (System.currentTimeMillis() - f.lastModified() > 3600000L) {
                return;
            }
            String json = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            Map<Integer, String> found = new java.util.TreeMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> e : root.entrySet()) {
                try {
                    int major = Integer.parseInt(e.getKey());
                    String path = e.getValue().getAsString();
                    if (new File(path).exists()) {
                        found.put(major, path);
                    }
                } catch (Exception ignored) {
                }
            }
            if (!found.isEmpty()) {
                window.getSettingsView().setDetectedJavas(found);
                window.getConsoleView().appendLine("Javas desde cache (" + found.size() + ").");
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveJdkCache(Map<Integer, String> found) {
        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            for (Map.Entry<Integer, String> e : found.entrySet()) {
                root.addProperty(String.valueOf(e.getKey()), e.getValue());
            }
            File f = jdkCacheFile();
            f.getParentFile().mkdirs();
            java.nio.file.Files.write(f.toPath(), new com.google.gson.Gson().toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private void scanJavaAtStartup() {
        window.getSettingsView().setOnRefreshJava(this::scanJavaAtStartup);
        applyJdkCache();
        new SwingWorker<Map<Integer, String>, Void>() {
            @Override
            protected Map<Integer, String> doInBackground() throws Exception {
                return JavaVersionSelector.scanAll();
            }

            @Override
            protected void done() {
                try {
                    Map<Integer, String> found = get();
                    if (found.isEmpty()) {
                        showError("Falta Java",
                                "No se detecto ningun Java instalado.\nInstala Temurin 8/17/21 desde https://adoptium.net/ o configura la ruta en Ajustes.");
                        return;
                    }
                    Settings s = settingsPersistence.load();
                    if (s.javaPath == null || s.javaPath.isEmpty() || !new File(s.javaPath).exists()) {
                        s.javaPath = "";
                        try {
                            settingsPersistence.save(s);
                        } catch (Exception ignored) {
                        }
                    }
                    StringBuilder sb = new StringBuilder("Javas detectados: ");
                    for (Map.Entry<Integer, String> e : found.entrySet()) {
                        sb.append("Java ").append(e.getKey()).append("  ");
                    }
                    window.getConsoleView().appendLine(sb.toString());
                    window.getSettingsView().setDetectedJavas(found);
                    saveJdkCache(found);
                } catch (Exception ex) {
                    window.getConsoleView().appendLine("No se pudo escanear Java: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void scanVersionsAtStartup() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                List<String> all = new ArrayList<>();
                all.addAll(versions.installedVersions(AppDirs.versionsDir()));
                for (String v : versions.installedVersions(new File(AppDirs.legacyMinecraftDir(), "versions"))) {
                    if (!all.contains(v)) {
                        all.add(v);
                    }
                }
                // También obtener versiones disponibles del manifest de Mojang
                try {
                    List<com.ragsmc.launcher.minecraft.VersionManager.McVersion> manifest = versions.fetchManifest(true);
                    for (com.ragsmc.launcher.minecraft.VersionManager.McVersion mv : manifest) {
                        if (com.ragsmc.launcher.minecraft.AllowedVersions.isAllowed(mv.id)) {
                            if (!all.contains(mv.id)) {
                                all.add(mv.id);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                return all;
            }

            @Override
            protected void done() {
                try {
                    List<String> found = get();
                    // Filtrar a solo versiones permitidas
                    List<String> allowed = new ArrayList<>();
                    for (String v : found) {
                        if (com.ragsmc.launcher.minecraft.AllowedVersions.isAllowed(v)) {
                            allowed.add(v);
                        }
                    }
                    window.getInstallationsView().setKnownVersions(allowed);
                    window.getConsoleView().appendLine("Versiones instaladas: " + found);
                    if (found.isEmpty()) {
                        window.getPlayView().setStatus("Sin versiones instaladas. Crea una instancia y descargala al jugar.");
                    }
                } catch (Exception ex) {
                    window.getConsoleView().appendLine("No se pudo escanear versiones: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void wirePlay() {
        window.getPlayView().getPlayButton().addActionListener(e -> launchSelected());
        window.getPlayView().setOnRamChanged(() -> {
            InstanceStore.Instance in = window.getPlayView().getSelectedInstance();
            if (in == null) {
                return;
            }
            int ram = parseRam(window.getPlayView().getSelectedRam());
            if (ram != in.ramMb) {
                in.ramMb = ram;
                try {
                    instances.update(in);
                    refreshInstallations();
                } catch (Exception ex) {
                    showError("No se pudo guardar la RAM", ex.getMessage());
                }
            }
        });
        window.getPlayView().setOnCreateFirst(() -> window.showView("installations"));
    }

    private String currentUsername() {
        String username = window.getPlayView().getSelectedProfile();
        if (username == null || username.trim().isEmpty()) {
            Account sel = auth.selected();
            username = sel != null ? sel.getUsername() : "";
        }
        return username.trim();
    }

    private String resolveJava(String version) {
        Settings s = settingsPersistence.load();
        int requiredJava = JavaVersionSelector.requiredMajor(version);
        if (s.javaPath != null && !s.javaPath.isEmpty() && new File(s.javaPath).exists()) {
            int customMajor = JavaVersionSelector.majorOf(s.javaPath);
            if (customMajor == requiredJava) {
                return s.javaPath;
            }
            com.ragsmc.launcher.util.Logger.info("Java guardado (" + customMajor + ") no sirve para " + version + ", activando Java " + requiredJava + " automaticamente");
        }
        java.util.Optional<String> found = JavaVersionSelector.findJava(requiredJava);
        if (!found.isPresent()) {
            showError("Falta Java " + requiredJava,
                    "Minecraft " + version + " necesita Java " + requiredJava + " y no se encontro instalado.\n"
                    + "Descargalo gratis en https://adoptium.net/ o configura la ruta en Ajustes.");
            return null;
        }
        String auto = found.get();
        s.javaPath = auto;
        try {
            settingsPersistence.save(s);
        } catch (Exception ignored) {
        }
        com.ragsmc.launcher.util.Logger.info("Java auto para " + version + " (requiere " + requiredJava + "): " + auto);
        return auto;
    }
    private void launchSelected() {
        InstanceStore.Instance in = window.getPlayView().getSelectedInstance();
        if (in == null) {
            showError("Sin instancias", "Crea tu primera instancia en Instalaciones.");
            window.showView("installations");
            return;
        }
        String username = currentUsername();
        if (username.isEmpty()) {
            showError("Falta el perfil", "Crea una cuenta en PERFIL o escribe un nombre de usuario.");
            window.showView("profiles");
            return;
        }
        int ram = parseRam(window.getPlayView().getSelectedRam());
        long maxRam = com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb();
        if (ram > maxRam) {
            showError("RAM excesiva", "El maximo usable es " + maxRam + " MB.");
            return;
        }
        if (ram != in.ramMb) {
            in.ramMb = ram;
            try {
                instances.update(in);
            } catch (Exception ignored) {
            }
        }
        Settings s = settingsPersistence.load();
        s.username = username;
        s.lastVersion = in.version;
        s.ramMb = ram;
        if (s.uuid == null || s.uuid.isEmpty()) {
            s.uuid = OfflineUuid.generate(username);
        }
        try {
            settingsPersistence.save(s);
        } catch (Exception ignored) {
        }
        String javaPath = resolveJava(in.version);
        if (javaPath == null) {
            return;
        }
        launchExistingInstance(in, username, javaPath, null, 25565);
    }

    private void launchExistingInstance(InstanceStore.Instance in, String username, String javaPath, String serverIp, int serverPort) {
        final long t0 = System.nanoTime();
        com.ragsmc.launcher.util.Logger.info("JUGAR instancia=" + in.name + " version=" + in.version + " ram=" + in.ramMb + " java=" + javaPath);
        window.getPlayView().setLaunching(true);
        window.getPlayView().setStatus("Preparando " + in.name + "...");
        if (versionReady(in.version)) {
            com.ragsmc.launcher.util.Logger.info("T fast-path listo en " + ((System.nanoTime() - t0) / 1000000) + "ms");
            buildAndLaunch(in, username, javaPath, t0, serverIp, serverPort);
            return;
        }
        Thread checker = new Thread(() -> {
            List<String> faltante = versions.missingParts(in.version, AppDirs.gameDir());
            if (!faltante.isEmpty() && importLegacyFast(in.version)) {
                faltante = versions.missingParts(in.version, AppDirs.gameDir());
            }
            if (faltante.isEmpty()) {
                try {
                    versions.ensureNatives(in.version, AppDirs.gameDir(), null);
                } catch (Exception ex) {
                    window.getConsoleView().appendLine("Aviso natives: " + ex.getMessage());
                }
                faltante = versions.missingParts(in.version, AppDirs.gameDir());
            }
            final List<String> missing = faltante;
            final long t1 = System.nanoTime();
            javax.swing.SwingUtilities.invokeLater(() -> {
                com.ragsmc.launcher.util.Logger.info("T verificacion bg en " + ((t1 - t0) / 1000000) + "ms, faltante=" + missing);
                if (!missing.isEmpty()) {
                    window.getPlayView().setStatus("Descargando " + in.version + " (" + String.join(", ", missing) + ")...");
                    window.getConsoleView().appendLine("Falta descargar para " + in.version + ": " + String.join(", ", missing));
                    downloadVersionDialog(in.version, () -> {
                        window.getPlayView().setLaunching(false);
                        List<String> resto = versions.missingParts(in.version, AppDirs.gameDir());
                        if (!resto.isEmpty()) {
                            window.getPlayView().setStatus("Descarga incompleta. Revisa la consola.");
                            showError("Descarga incompleta", "Sigue faltando: " + String.join(", ", resto));
                            return;
                        }
                        launchExistingInstance(instances.byId(in.id) != null ? instances.byId(in.id) : in, username, javaPath, serverIp, serverPort);
                    });
                    return;
                }
                buildAndLaunch(in, username, javaPath, t0, serverIp, serverPort);
            });
        }, "mc-verify");
        checker.setDaemon(true);
        checker.start();
    }

    private void buildAndLaunch(InstanceStore.Instance in, String username, String javaPath, long t0, String serverIp, int serverPort) {
        final LaunchConfig cfg = new LaunchConfig();
        cfg.setUsername(username);
        cfg.setUuid(OfflineUuid.generate(username));
        cfg.setJavaPath(javaPath);
        cfg.setMemory((int) Math.min(in.ramMb, com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb()));
        cfg.setGameDir(in.gameDir);
        cfg.setSharedDir(AppDirs.gameDir().getAbsolutePath());
        cfg.setVersion(in.version);
        cfg.setLoader(in.loader != null ? in.loader : "vanilla");
        cfg.setAccessToken("null");
        cfg.setUserType("mojang");
        if (serverIp != null && !serverIp.isEmpty()) {
            cfg.setServer(serverIp);
            cfg.setServerPort(serverPort);
        }
        cfg.setClientId(persistentClientId());
        cfg.setAssetIndex(resolveAssetIndex(in.version));
        Settings adv = settingsPersistence.load();
        if (adv.customJvmArgs != null && !adv.customJvmArgs.trim().isEmpty()) {
            cfg.setJvmArgs(adv.customJvmArgs.trim());
        }
        if (adv.customGameArgs != null && !adv.customGameArgs.trim().isEmpty()) {
            cfg.setGameArgs(adv.customGameArgs.trim());
        }
        final boolean hideOnLaunch = adv.hideOnLaunch;
        final ConsoleView console = window.getConsoleView();
        final String user = username;
        final String ver = in.version;
        final String iid = in.id;
        window.getPlayView().setLaunching(true);
        window.getPlayView().setStatus("Lanzando " + in.name + "...");
        console.appendLine("Lanzando " + in.name + " (" + ver + ") como " + user);
        console.appendLine("Instancia: " + in.gameDir);
        com.ragsmc.launcher.util.Logger.info("T cfg lista en " + ((System.nanoTime() - t0) / 1000000) + "ms desde click");
        LauncherLifecycle.launchAndManage(window,
                () -> {
                    try {
                        Process started = MinecraftLauncher.launch(cfg, console::appendLine);
                        markVersionReady(ver);
                        return started;
                    } catch (Exception e) {
                        clearVersionReady(ver);
                        throw new RuntimeException(e.getMessage(), e);
                    }
                },
                console::appendLine,
                () -> {
                    window.getPlayView().setLaunching(false);
                    window.getPlayView().setStatus("Listo para jugar");
                    instances.markPlayed(iid);
                    refreshInstallations();
                },
                hideOnLaunch);
    }

    private static File versionMarker(String version) {
        return new File(AppDirs.versionsDir(), version + File.separator + ".complete");
    }

    private static boolean versionReady(String version) {
        try {
            File marker = versionMarker(version);
            File jar = new File(AppDirs.versionsDir(), version + File.separator + version + ".jar");
            return marker.exists() && jar.exists() && jar.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void markVersionReady(String version) {
        try {
            File marker = versionMarker(version);
            marker.getParentFile().mkdirs();
            java.nio.file.Files.write(marker.toPath(), "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static void clearVersionReady(String version) {
        try {
            versionMarker(version).delete();
        } catch (Exception ignored) {
        }
    }

    private boolean importLegacyFast(String version) {
        try {
            File legacyVer = new File(AppDirs.legacyMinecraftDir(), "versions" + File.separator + version);
            File json = new File(legacyVer, version + ".json");
            File jar = new File(legacyVer, version + ".jar");
            boolean copiedAny = false;
            if (json.exists() && jar.exists() && jar.length() > 0) {
                File dest = new File(AppDirs.versionsDir(), version);
                dest.mkdirs();
                copyIfMissing(json, new File(dest, version + ".json"));
                copyIfMissing(jar, new File(dest, version + ".jar"));
                copiedAny = true;
                window.getConsoleView().appendLine("Cliente " + version + " importado (rapido).");
            }
            File legacyLibs = new File(AppDirs.legacyMinecraftDir(), "libraries");
            if (legacyLibs.isDirectory()) {
                int n = mirrorMissing(legacyLibs, new File(AppDirs.gameDir(), "libraries"));
                if (n > 0) {
                    window.getConsoleView().appendLine("Librerias importadas: " + n + ".");
                    copiedAny = true;
                }
            }
            File legacyNatives = new File(AppDirs.legacyMinecraftDir(), "versions" + File.separator + version + File.separator + "natives");
            if (legacyNatives.isDirectory()) {
                int n = mirrorMissing(legacyNatives, new File(AppDirs.versionsDir(), version + File.separator + "natives"));
                if (n > 0) {
                    window.getConsoleView().appendLine("Natives importados: " + n + ".");
                    copiedAny = true;
                }
            }
            return copiedAny;
        } catch (Exception ex) {
            window.getConsoleView().appendLine("Import rapido fallo: " + ex.getMessage());
            return false;
        }
    }

    private boolean importLegacyShared(String version) {
        try {
            boolean any = importLegacyFast(version);
            File legacyIdx = new File(AppDirs.legacyMinecraftDir(), "assets" + File.separator + "indexes");
            if (legacyIdx.isDirectory()) {
                mirrorMissing(legacyIdx, new File(AppDirs.gameDir(), "assets" + File.separator + "indexes"));
            }
            File legacyObj = new File(AppDirs.legacyMinecraftDir(), "assets" + File.separator + "objects");
            if (legacyObj.isDirectory()) {
                int n = mirrorMissing(legacyObj, new File(AppDirs.gameDir(), "assets" + File.separator + "objects"));
                if (n > 0) {
                    window.getConsoleView().appendLine("Assets importados: " + n + " archivos.");
                    any = true;
                }
            }
            return any;
        } catch (Exception ex) {
            window.getConsoleView().appendLine("No se pudo importar: " + ex.getMessage());
            return false;
        }
    }

    private static void copyIfMissing(File src, File dest) throws Exception {
        if (dest.exists() && dest.length() > 0) {
            return;
        }
        dest.getParentFile().mkdirs();
        java.nio.file.Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static int mirrorMissing(File srcDir, File destDir) throws Exception {
        final int[] count = {0};
        try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(srcDir.toPath())) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) walk::iterator) {
                File src = p.toFile();
                if (!src.isFile()) {
                    continue;
                }
                File dest = new File(destDir, srcDir.toPath().relativize(p).toString());
                if (!dest.exists() || dest.length() == 0) {
                    dest.getParentFile().mkdirs();
                    java.nio.file.Files.copy(p, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    count[0]++;
                }
            }
        }
        return count[0];
    }

    private void downloadVersionDialog(String version, Runnable afterOk) {
        com.ragsmc.launcher.download.DownloadStats stats = new com.ragsmc.launcher.download.DownloadStats();
        versions.setStats(stats);
        JDialog dlg = new JDialog(window, "Instalando Minecraft " + version, true);
        dlg.setLayout(new BorderLayout(12, 12));
        dlg.setSize(480, 300);
        dlg.setLocationRelativeTo(window);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Colors.BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));
        JLabel title = new JLabel("Instalando Minecraft " + version);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Colors.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        body.add(title);
        body.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel pctLabel = new JLabel("0%");
        pctLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        pctLabel.setForeground(Colors.NEON_GREEN);
        pctLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(pctLabel);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(false);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(440, 18));
        body.add(bar);
        body.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel fileLabel = new JLabel("Conectando con Mojang...");
        fileLabel.setForeground(Colors.TEXT_PRIMARY);
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(fileLabel);
        JLabel mbLabel = new JLabel("0.0 MB");
        mbLabel.setForeground(Colors.TEXT_SECONDARY);
        mbLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mbLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(mbLabel);
        JLabel speedLabel = new JLabel("");
        speedLabel.setForeground(Colors.TEXT_SECONDARY);
        speedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        speedLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(speedLabel);
        JLabel filesLabel = new JLabel("");
        filesLabel.setForeground(Colors.TEXT_SECONDARY);
        filesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filesLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(filesLabel);
        dlg.add(body, BorderLayout.CENTER);
        javax.swing.JButton cancel = new javax.swing.JButton("Cancelar");
        cancel.setBackground(Colors.FIELD_BACKGROUND);
        cancel.setForeground(Colors.TEXT_PRIMARY);
        cancel.setFocusPainted(false);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setBackground(Colors.BACKGROUND);
        btnRow.add(cancel);
        dlg.add(btnRow, BorderLayout.SOUTH);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancel.addActionListener(e -> {
            cancelled.set(true);
            cancel.setEnabled(false);
            cancel.setText("Cancelando...");
        });
        javax.swing.Timer uiTimer = new javax.swing.Timer(300, e -> {
            int pct = stats.percent();
            if (pct >= 0) {
                bar.setIndeterminate(false);
                bar.setValue(pct);
                pctLabel.setText(pct + "%");
            } else {
                bar.setIndeterminate(true);
            }
            String fn = stats.fileName;
            if (fn != null && !fn.isEmpty()) {
                String shortName = fn.contains("/") ? fn.substring(fn.lastIndexOf("/") + 1) : fn;
                if (shortName.length() > 52) {
                    shortName = shortName.substring(0, 52) + "...";
                }
                fileLabel.setText("Descargando: " + shortName);
            }
            mbLabel.setText(stats.mbText());
            double speed = stats.speedMBs();
            speedLabel.setText(speed > 0.05 ? String.format("%.1f MB/s  -  quedan %s", speed, stats.etaText()) : "");
            if (stats.filesTotal > 0) {
                filesLabel.setText(stats.filesDone + " / " + stats.filesTotal + " archivos");
            } else if (stats.filesDone > 0) {
                filesLabel.setText(stats.filesDone + " archivos");
            }
        });
        uiTimer.start();
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    try {
                        importLegacyShared(version);
                    } catch (Exception ignored) {
                    }
                    versions.downloadVersion(version, AppDirs.gameDir(), (msg, frac) -> {
                        publish(msg + "|" + (int) (frac * 100));
                    }, cancelled);
                    markVersionReady(version);
                    publish("OK|100");
                } catch (Exception ex) {
                    publish("ERROR: " + ex.getMessage() + "|-1");
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String c : chunks) {
                    int sep = c.lastIndexOf("|");
                    if (sep < 0) {
                        continue;
                    }
                    String msg = c.substring(0, sep);
                    int pct;
                    try {
                        pct = Integer.parseInt(c.substring(sep + 1));
                    } catch (Exception ex) {
                        pct = 0;
                    }
                    if (pct < 0) {
                        uiTimer.stop();
                        versions.setStats(null);
                        dlg.dispose();
                        showError("Fallo la descarga", msg);
                        window.getPlayView().setLaunching(false);
                        window.getPlayView().setStatus("Fallo la descarga. Revisa la consola.");
                        return;
                    }
                    if (msg.equals("OK")) {
                        uiTimer.stop();
                        versions.setStats(null);
                        dlg.dispose();
                        scanVersionsAtStartup();
                        window.getConsoleView().appendLine("Instalado: " + stats.mbText() + " descargados.");
                        if (afterOk != null) {
                            afterOk.run();
                        }
                        return;
                    }
                    fileLabel.setText(msg);
                }
            }

            @Override
            protected void done() {
                uiTimer.stop();
                versions.setStats(null);
            }
        };
        worker.execute();
        dlg.setVisible(true);
    }


    private String persistentClientId() {
        try {
            File f = new File(AppDirs.dataDir(), "ragsmc_clientid.txt");
            if (f.exists()) {
                String id = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
                if (!id.isEmpty()) {
                    return id;
                }
            }
            String id = UUID.randomUUID().toString().replace("-", "");
            java.nio.file.Files.write(f.toPath(), id.getBytes(StandardCharsets.UTF_8));
            return id;
        } catch (Exception e) {
            return "0";
        }
    }

    private String resolveAssetIndex(String version) {
        try {
            File json = new File(AppDirs.versionsDir(), version + File.separator + version + ".json");
            if (json.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8);
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                if (root.has("assetIndex")) {
                    com.google.gson.JsonObject ai = root.getAsJsonObject("assetIndex");
                    if (ai.has("id")) {
                        return ai.get("id").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return version;
    }

    private void refreshInstallations() {
        try {
            List<InstanceStore.Instance> all = instances.all();
            window.getInstallationsView().setInstances(all);
            List<String> names = new ArrayList<>();
            for (InstanceStore.Instance in : all) {
                names.add(in.name);
            }
            window.getHomeView().setRecent(names);
            String selectId = all.isEmpty() ? null : all.get(0).id;
            window.getPlayView().setInstances(all, selectId);
            updateHomeCards(all);
            window.getRecursosView().setInstances(all, selectId);
        } catch (Exception ignored) {
        }
    }

    private void updateHomeCards(List<InstanceStore.Instance> all) {
        try {
            int mods = 0;
            int mundos = 0;
            int packs = 0;
            String lastMundo = null;
            long lastTime = 0;
            for (InstanceStore.Instance in : all) {
                File base = new File(in.gameDir);
                mods += countFiles(new File(base, "mods"), ".jar");
                packs += countFiles(new File(base, "resourcepacks"), ".zip");
                packs += countFiles(new File(base, "shaderpacks"), ".zip");
                File saves = new File(base, "saves");
                File[] dirs = saves.listFiles(File::isDirectory);
                if (dirs != null) {
                    mundos += dirs.length;
                    for (File d : dirs) {
                        if (d.lastModified() > lastTime) {
                            lastTime = d.lastModified();
                            lastMundo = d.getName();
                        }
                    }
                }
            }
            window.getHomeView().setSummary(mods, mundos, packs, lastMundo);
            if (!all.isEmpty()) {
                InstanceStore.Instance sel = all.get(0);
                int mc = countFiles(new File(new File(sel.gameDir), "mods"), ".jar");
                window.getHomeView().setSelectedCard(sel.name,
                        "Version: " + sel.version + " | Loader: " + sel.loader + " | RAM: " + (sel.ramMb / 1024) + " GB | Mods: " + mc);
            } else {
                window.getHomeView().setSelectedCard("Sin seleccion", "Crea una instancia para empezar.");
            }
        } catch (Exception ignored) {
        }
    }

    private static int countFiles(File dir, String ext) {
        try {
            File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(ext));
            return files != null ? files.length : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void wireInstallations() {
        window.getInstallationsView().setOnCreate(ni -> {
            // Validar version permitida
            if (!com.ragsmc.launcher.minecraft.AllowedVersions.isAllowed(ni.version)) {
                showError("Version no soportada", com.ragsmc.launcher.minecraft.AllowedVersions.getAllowedMessage());
                return;
            }
            try {
                File dir = new File(AppDirs.instancesDir(), ni.name.toLowerCase().replaceAll("[^a-z0-9_-]", "-"));
                dir.mkdirs();
                new File(dir, "mods").mkdirs();
                new File(dir, "resourcepacks").mkdirs();
                new File(dir, "shaderpacks").mkdirs();
                new File(dir, "saves").mkdirs();
                InstanceStore.Instance created = instances.create(ni.name, ni.version, ni.loader, ni.ramMb > 0 ? ni.ramMb : 4096, dir);
                created.description = ni.description != null ? ni.description : "";
                created.iconPath = ni.iconPath != null ? ni.iconPath : "";
                created.bgPath = ni.bgPath != null ? ni.bgPath : "";
                try {
                    instances.update(created);
                } catch (Exception ignored) {
                }
                refreshInstallations();
                window.getPlayView().setInstances(instances.all(), created.id);
            } catch (Exception ex) {
                showError("No se pudo crear", ex.getMessage());
            }
        });
        window.getInstallationsView().setOnPlay(in -> {
            String username = settingsPersistence.load().username;
            if (username == null || username.isEmpty()) {
                Account sel = auth.selected();
                username = sel != null ? sel.getUsername() : "Jugador";
            }
            String javaPath = resolveJava(in.version);
            if (javaPath == null) {
                return;
            }
            InstanceStore.Instance fresh = instances.byId(in.id) != null ? instances.byId(in.id) : in;
            launchExistingInstance(fresh, username, javaPath, null, 25565);
        });
        window.getInstallationsView().setOnDuplicate(in -> {
            try {
                instances.duplicate(in.id);
                refreshInstallations();
            } catch (Exception ex) {
                showError("No se pudo duplicar", ex.getMessage());
            }
        });
        window.getInstallationsView().setOnDelete(in -> {
            try {
                instances.delete(in.id);
                refreshInstallations();
            } catch (Exception ex) {
                showError("No se pudo eliminar", ex.getMessage());
            }
        });
        window.getInstallationsView().setOnEdit(in -> {
            try {
                instances.update(in);
                refreshInstallations();
            } catch (Exception ex) {
                showError("No se pudo guardar", ex.getMessage());
            }
        });
        window.getInstallationsView().setOnContent(in -> openContent(in));
    }

    private void openContent(InstanceStore.Instance in) {
        InstanceStore.Instance fresh = instances.byId(in.id) != null ? instances.byId(in.id) : in;
        com.ragsmc.launcher.ui.InstanceDetailView dlg =
                new com.ragsmc.launcher.ui.InstanceDetailView(window, fresh, modrinth);
        dlg.setOnPlay(() -> {
            String username = settingsPersistence.load().username;
            if (username == null || username.isEmpty()) {
                Account sel = auth.selected();
                username = sel != null ? sel.getUsername() : "Jugador";
            }
            String javaPath = resolveJava(fresh.version);
            if (javaPath == null) {
                return;
            }
            launchExistingInstance(fresh, username, javaPath, null, 25565);
        });
        dlg.setVisible(true);
        refreshInstallations();
    }

    private void wireProfiles() {
        refreshProfiles();
        window.getProfilesView().setOnSaveName(name -> {
            try {
                Account sel = auth.selected();
                Account target = sel != null ? sel : (auth.all().isEmpty() ? null : auth.all().get(0));
                if (target == null) {
                    auth.addOffline(name);
                } else {
                    auth.rename(target.getId(), name);
                }
                refreshProfiles();
            } catch (Exception ex) {
                showError("No se pudo guardar", ex.getMessage());
            }
        });
        window.getProfilesView().setOnCreate(name -> {
            try {
                auth.addOffline(name);
                refreshProfiles();
            } catch (Exception ex) {
                showError("No se pudo crear la cuenta", ex.getMessage());
            }
        });
        window.getProfilesView().setOnSelect(id -> {
            auth.select(id);
            refreshProfiles();
        });
        window.getProfilesView().setOnDelete(id -> {
            try {
                auth.delete(id);
                refreshProfiles();
            } catch (Exception ex) {
                showError("No se pudo eliminar", ex.getMessage());
            }
        });
    }

    private void refreshProfiles() {
        List<Account> all = auth.all();
        List<String> names = new ArrayList<>();
        String selected = null;
        for (Account a : all) {
            String display = a.getUsername() + (a.isSelected() ? " (activa)" : "");
            names.add(display);
            if (a.isSelected()) {
                selected = display;
            }
        }
        if (names.isEmpty()) {
            Settings s = settingsPersistence.load();
            if (s.username != null && !s.username.isEmpty()) {
                try {
                    Account a = auth.addOffline(s.username);
                    names.add(a.getUsername() + " (activa)");
                    selected = a.getUsername() + " (activa)";
                } catch (Exception ignored) {
                }
            }
        }
        window.getProfilesView().setAccounts(names, selected);
        window.getProfilesView().setAccountsList(auth.all());
        List<String> plain = new ArrayList<>();
        String plainSel = null;
        for (Account a : auth.all()) {
            plain.add(a.getUsername());
            if (a.isSelected()) {
                plainSel = a.getUsername();
            }
        }
        if (!plain.isEmpty()) {
            window.getPlayView().setProfiles(plain.toArray(new String[0]), plainSel);
            window.getHomeView().setProfile(plainSel != null ? plainSel : plain.get(0));
            window.getSidebar().setUsername(plainSel != null ? plainSel : plain.get(0));
        }
    }

    private void wireHome() {
        window.getHomeView().setOnPlay(() -> {
            window.showView("home");
            launchSelected();
        });
        window.getHomeView().setOnShowInstallations(() -> window.showView("installations"));
        try {
            File newsFile = new File(AppDirs.dataDir(), "news.json");
            NewsManager nm = new NewsManager(newsFile);
            List<NewsManager.NewsEntry> entries = nm.load();
            List<String> items = new ArrayList<>();
            for (NewsManager.NewsEntry e : entries) {
                items.add(e.title + (e.description != null && !e.description.isEmpty() ? " - " + e.description : ""));
            }
            window.getHomeView().setNews(items);
        } catch (Exception ignored) {
        }
    }

    private void wireMods() {
        // FASE 3: la busqueda de mods vive en Recursos > Explorar.
    }

    private void wireSettings() {
        Settings s = settingsPersistence.load();
        window.getSettingsView().loadValues(
                s.javaPath != null ? s.javaPath : "",
                s.ramMb > 0 ? s.ramMb : 4096, 854, 480, false);
        window.getSettingsView().loadAdvanced(s.hideOnLaunch, s.gpuDedicated, s.mesaCpu,
                s.dnsOverride != null ? s.dnsOverride : "",
                s.customJvmArgs != null ? s.customJvmArgs : "",
                s.customGameArgs != null ? s.customGameArgs : "");
        window.getSettingsView().setOnSave(d -> {
            Settings cur = settingsPersistence.load();
            cur.javaPath = d.javaPath;
            cur.ramMb = d.ramMb;
            cur.hideOnLaunch = d.hideOnLaunch;
            cur.gpuDedicated = d.gpuDedicated;
            cur.mesaCpu = d.mesaCpu;
            cur.dnsOverride = d.dnsOverride;
            cur.customJvmArgs = d.jvmArgs;
            cur.customGameArgs = d.gameArgs;
            try {
                settingsPersistence.save(cur);
                window.getConsoleView().appendLine("Ajustes guardados: RAM " + d.ramMb + " MB, Java " + (d.javaPath.isEmpty() ? "auto" : d.javaPath));
            } catch (Exception ex) {
                showError("No se pudo guardar", ex.getMessage());
            }
        });
        window.getSettingsView().setOnDownloadJava(ver -> downloadJavaDialog(ver));
        window.getSettingsView().setOnApplyRamAll(() -> {
            int ram = 4096;
            try {
                ram = settingsPersistence.load().ramMb;
            } catch (Exception ignored) {
            }
            final int target = (int) Math.min(Math.max(ram, 512), com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb());
            try {
                for (InstanceStore.Instance in : instances.all()) {
                    in.ramMb = target;
                    instances.update(in);
                }
                refreshInstallations();
                window.getConsoleView().appendLine("RAM " + target + " MB aplicada a todas las instancias.");
            } catch (Exception ex) {
                showError("No se pudo aplicar", ex.getMessage());
            }
        });
    }

    private void downloadJavaDialog(int major) {
        JDialog dlg = new JDialog(window, "Descargando Java " + major, true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(420, 140);
        dlg.setLocationRelativeTo(window);
        JLabel label = new JLabel("Conectando con Adoptium...");
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setIndeterminate(true);
        javax.swing.JButton cancel = new javax.swing.JButton("Cancelar");
        dlg.add(label, BorderLayout.NORTH);
        dlg.add(bar, BorderLayout.CENTER);
        dlg.add(cancel, BorderLayout.SOUTH);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancel.addActionListener(e -> {
            cancelled.set(true);
            cancel.setEnabled(false);
            cancel.setText("Cancelando...");
        });
        SwingWorker<String, String> worker = new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() throws Exception {
                File dest = com.ragsmc.launcher.minecraft.JavaDownloader.downloadJava(major, (done, total) -> {
                    if (cancelled.get()) {
                        throw new RuntimeException("cancelado");
                    }
                    if (total > 0) {
                        publish(done + "|" + total);
                    } else {
                        publish(done + "|-1");
                    }
                });
                return dest.getAbsolutePath();
            }

            @Override
            protected void process(List<String> chunks) {
                for (String c : chunks) {
                    int sep = c.lastIndexOf("|");
                    if (sep < 0) {
                        continue;
                    }
                    try {
                        long done = Long.parseLong(c.substring(0, sep));
                        long total = Long.parseLong(c.substring(sep + 1));
                        if (total > 0) {
                            int pct = (int) (done * 100 / total);
                            bar.setIndeterminate(false);
                            bar.setValue(Math.max(0, Math.min(100, pct)));
                            label.setText(String.format("%.1f / %.1f MB", done / 1048576.0, total / 1048576.0));
                        } else {
                            label.setText(String.format("%.1f MB descargados", done / 1048576.0));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            protected void done() {
                dlg.dispose();
                try {
                    String path = get();
                    Settings cur = settingsPersistence.load();
                    cur.javaPath = path;
                    settingsPersistence.save(cur);
                    window.getSettingsView().loadValues(path, cur.ramMb, 854, 480, false);
                    scanJavaAtStartup();
                    window.getConsoleView().appendLine("Java " + major + " instalado: " + path);
                    JOptionPane.showMessageDialog(window, "Java " + major + " instalado y seleccionado.", "OK", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    if (msg.contains("cancelado")) {
                        window.getConsoleView().appendLine("Descarga de Java cancelada.");
                    } else {
                        showError("Fallo la descarga de Java", msg);
                    }
                }
            }
        };
        worker.execute();
        dlg.setVisible(true);
    }

    private void wireContentDirs() {
        // RecursosView es por instancia: se alimenta en refreshInstallations.
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(window, message != null ? message : "Error desconocido", title, JOptionPane.ERROR_MESSAGE);
    }

    private static int parseRam(String ram) {
        if (ram == null) {
            return 2048;
        }
        String digits = ram.replaceAll("[^0-9]", "");
        try {
            int v = Integer.parseInt(digits);
            if (v < 512) {
                return 512;
            }
            return (int) Math.min(v, com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb());
        } catch (Exception e) {
            return 2048;
        }
    }
}
