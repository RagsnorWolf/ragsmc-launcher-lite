package com.ragsmc.launcher.mods;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContentManager {

    public static class ContentItem {
        public String fileName = "";
        public String name = "";
        public long size = 0;
        public boolean enabled = true;
        public File file;
    }

    private ContentManager() {
    }

    public static List<ContentItem> list(File dir, String... extensions) {
        List<ContentItem> out = new ArrayList<>();
        if (dir == null) {
            return out;
        }
        dir.mkdirs();
        File[] files = dir.listFiles();
        if (files == null) {
            return out;
        }
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            String n = f.getName().toLowerCase();
            boolean enabled = true;
            String base = f.getName();
            if (base.toLowerCase().endsWith(".disabled")) {
                enabled = false;
                base = base.substring(0, base.length() - 9);
            }
            boolean match = extensions.length == 0;
            for (String ext : extensions) {
                if (base.toLowerCase().endsWith(ext)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            ContentItem item = new ContentItem();
            item.fileName = f.getName();
            item.name = base;
            item.size = f.length();
            item.enabled = enabled;
            item.file = f;
            out.add(item);
        }
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    public static void setEnabled(ContentItem item, boolean enabled) throws Exception {
        if (item.enabled == enabled) {
            return;
        }
        File target;
        if (enabled) {
            String n = item.fileName;
            if (n.toLowerCase().endsWith(".disabled")) {
                n = n.substring(0, n.length() - 9);
            }
            target = new File(item.file.getParentFile(), n);
        } else {
            target = new File(item.file.getParentFile(), item.fileName + ".disabled");
        }
        if (!item.file.renameTo(target)) {
            throw new Exception("No se pudo cambiar " + item.fileName);
        }
        item.file = target;
        item.fileName = target.getName();
        item.enabled = enabled;
    }

    public static void delete(ContentItem item) throws Exception {
        if (!item.file.delete()) {
            throw new Exception("No se pudo eliminar " + item.fileName);
        }
    }

    public static String sizeText(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    public static String stats(List<ContentItem> items) {
        int on = 0;
        long bytes = 0;
        for (ContentItem i : items) {
            if (i.enabled) {
                on++;
            }
            bytes += i.size;
        }
        return on + "/" + items.size() + " activos - " + sizeText(bytes);
    }
}