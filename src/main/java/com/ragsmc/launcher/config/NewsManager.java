package com.ragsmc.launcher.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class NewsManager {

    public static class NewsEntry {
        public String id = "";
        public String title = "";
        public String description = "";
        public String date = "";
    }

    private final Gson gson = new Gson();
    private final File file;

    public NewsManager(File file) {
        this.file = file;
    }

    public List<NewsEntry> load() {
        try {
            if (!file.exists()) {
                return defaults();
            }
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Type type = new TypeToken<List<NewsEntry>>() {
            }.getType();
            List<NewsEntry> list = gson.fromJson(json, type);
            return list != null ? list : defaults();
        } catch (Exception e) {
            return defaults();
        }
    }

    public void save(List<NewsEntry> news) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Files.write(file.toPath(), gson.toJson(news).getBytes(StandardCharsets.UTF_8));
    }

    private List<NewsEntry> defaults() {
        List<NewsEntry> list = new ArrayList<>();
        NewsEntry w = new NewsEntry();
        w.id = "welcome";
        w.title = "Bienvenido a RagsMC";
        w.description = "Launcher liviano para PCs de gama baja.";
        w.date = "";
        list.add(w);
        return list;
    }
}