package com.ragsmc.launcher.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstanceStore {

    public static class Instance {
        public String id = "";
        public String description = "";
        public String iconPath = "";
        public String bgPath = "";
        public String name = "";
        public String version = "";
        public String loader = "vanilla";
        public int ramMb = 4096;
        public String gameDir = "";
        public String lastPlayed = "Nunca";
        public String createdAt = "";
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final List<Instance> instances = new ArrayList<>();

    public InstanceStore(File file) {
        this.file = file;
        load();
    }

    public synchronized void load() {
        instances.clear();
        try {
            if (!file.exists()) {
                return;
            }
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Type type = new TypeToken<List<Instance>>() {
            }.getType();
            List<Instance> list = gson.fromJson(json, type);
            if (list != null) {
                instances.addAll(list);
            }
            System.out.println("[InstanceStore] Cargadas " + instances.size() + " instancias desde " + file.getName());
        } catch (JsonSyntaxException e) {
            System.err.println("[InstanceStore] JSON corrupto en " + file.getName() + ": " + e.getMessage());
            // Intentar cargar desde backup
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            if (backup.exists()) {
                try {
                    String json = new String(Files.readAllBytes(backup.toPath()), StandardCharsets.UTF_8);
                    Type type = new TypeToken<List<Instance>>() {
                    }.getType();
                    List<Instance> list = gson.fromJson(json, type);
                    if (list != null) {
                        instances.addAll(list);
                        System.out.println("[InstanceStore] Restauradas " + instances.size() + " instancias desde backup");
                    }
                } catch (Exception ex) {
                    System.err.println("[InstanceStore] Backup también corrupto: " + ex.getMessage());
                }
            } else {
                System.err.println("[InstanceStore] No hay backup disponible, empezando con lista vacía");
            }
        } catch (Exception e) {
            System.err.println("[InstanceStore] Error cargando instancias: " + e.getMessage());
        }
    }

    public synchronized void save() throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        String json = gson.toJson(instances);
        
        // Backup before saving
        if (file.exists()) {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[InstanceStore] Backup creado: " + backup.getName());
        }
        
        // Atomic write: write to temp file then atomic move
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        Files.write(temp.toPath(), json.getBytes(StandardCharsets.UTF_8));
        Files.move(temp.toPath(), file.toPath(), 
            StandardCopyOption.REPLACE_EXISTING, 
            StandardCopyOption.ATOMIC_MOVE);
        
        System.out.println("[InstanceStore] Guardadas " + instances.size() + " instancias (escritura atómica OK)");
    }

    public synchronized List<Instance> all() {
        return new ArrayList<>(instances);
    }

    public synchronized Instance create(String name, String version, String loader, int ramMb, File gameDir) throws IOException {
        Instance in = new Instance();
        in.id = UUID.randomUUID().toString();
        in.name = name;
        in.version = version;
        in.loader = loader != null ? loader : "vanilla";
        in.ramMb = ramMb;
        in.gameDir = gameDir.getAbsolutePath();
        in.createdAt = java.time.LocalDate.now().toString();
        in.lastPlayed = "Nunca";
        instances.add(in);
        save();
        return in;
    }

    public synchronized Instance duplicate(String id) throws IOException {
        Instance src = byId(id);
        if (src == null) {
            throw new IOException("Instancia no encontrada");
        }
        Instance copy = new Instance();
        copy.id = UUID.randomUUID().toString();
        copy.name = src.name + " (copia)";
        copy.version = src.version;
        copy.loader = src.loader;
        copy.ramMb = src.ramMb;
        copy.gameDir = src.gameDir + "-copia";
        copy.createdAt = java.time.LocalDate.now().toString();
        copy.lastPlayed = "Nunca";
        new File(copy.gameDir).mkdirs();
        instances.add(copy);
        save();
        return copy;
    }

    public synchronized void update(Instance updated) throws IOException {
        for (int i = 0; i < instances.size(); i++) {
            if (instances.get(i).id.equals(updated.id)) {
                instances.set(i, updated);
                save();
                return;
            }
        }
        throw new IOException("Instancia no encontrada");
    }

    public synchronized void delete(String id) throws IOException {
        Instance in = byId(id);
        if (in == null) {
            throw new IOException("Instancia no encontrada");
        }
        instances.remove(in);
        save();
        try {
            deleteRecursive(new File(in.gameDir));
        } catch (Exception ignored) {
        }
    }

    public synchronized Instance byId(String id) {
        for (Instance in : instances) {
            if (in.id.equals(id)) {
                return in;
            }
        }
        return null;
    }

    public synchronized void markPlayed(String id) {
        try {
            Instance in = byId(id);
            if (in != null) {
                in.lastPlayed = java.time.LocalDateTime.now().toString().substring(0, 16).replace("T", " ");
                save();
            }
        } catch (Exception ignored) {
        }
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