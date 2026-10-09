package com.ragsmc.launcher.ui.theme;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public final class Images {

    private static final Map<String, ImageIcon> CACHE = new HashMap<>();

    private Images() {
    }

    public static ImageIcon icon(String name, int w, int h) {
        String key = name + "@" + w + "x" + h;
        ImageIcon cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            URL url = Images.class.getResource("/images/" + name);
            if (url == null) {
                return null;
            }
            BufferedImage img = ImageIO.read(url);
            if (img == null) {
                return null;
            }
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);
            if (CACHE.size() < 64) {
                CACHE.put(key, icon);
            }
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    public static BufferedImage image(String name) {
        try {
            URL url = Images.class.getResource("/images/" + name);
            if (url == null) {
                return null;
            }
            return ImageIO.read(url);
        } catch (Exception e) {
            return null;
        }
    }
}