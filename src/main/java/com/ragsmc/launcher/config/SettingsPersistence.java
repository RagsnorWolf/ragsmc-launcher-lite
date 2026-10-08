package com.ragsmc.launcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SettingsPersistence {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;

    public SettingsPersistence() {
        this(defaultFile());
    }

    public SettingsPersistence(File file) {
        this.file = file;
    }

    public static File defaultFile() {
        String appdata = System.getenv("APPDATA");
        File dir;
        if (appdata != null && !appdata.isEmpty()) {
            dir = new File(appdata, "RagsMC");
        } else {
            dir = new File(System.getProperty("user.home"), ".ragsmc");
        }
        return new File(dir, "settings.json");
    }

    public Settings load() {
        try {
            if (!file.exists()) {
                return new Settings();
            }
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Settings s = gson.fromJson(json, Settings.class);
            return s != null ? s : new Settings();
        } catch (Exception e) {
            return new Settings();
        }
    }

    public void save(Settings settings) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        String json = gson.toJson(settings);
        Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    public void saveGpuWarnSuppressed(boolean suppressed) throws IOException {
        Settings s = load();
        s.gpuWarnSuppressed = suppressed;
        save(s);
    }

    public File getFile() {
        return file;
    }
}