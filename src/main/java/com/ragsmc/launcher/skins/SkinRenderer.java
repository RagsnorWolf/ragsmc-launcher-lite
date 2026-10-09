package com.ragsmc.launcher.skins;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SkinRenderer {

    private SkinRenderer() {
    }

    public static BufferedImage loadSkin(File png) throws IOException {
        BufferedImage img = ImageIO.read(png);
        if (img == null) {
            throw new IOException("PNG de skin invalido: " + png);
        }
        return img;
    }

    public static BufferedImage renderFrontPreview(BufferedImage skin, int scale) {
        int w = 64;
        int h = 64;
        BufferedImage canvas = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(java.awt.Color.BLACK);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            drawPart(g, skin, 8, 8, 8, 8, 0, 0, w, h, scale);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    public static BufferedImage renderHead(BufferedImage skin, int size) {
        BufferedImage head = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                head.setRGB(x, y, skin.getRGB(8 + x, 8 + y));
            }
        }
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(head, 0, 0, size, size, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static void drawPart(Graphics2D g, BufferedImage skin,
            int sx, int sy, int sw, int sh,
            int dx, int dy, int dw, int dh, int scale) {
        BufferedImage part = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < sw && sx + x < skin.getWidth(); x++) {
            for (int y = 0; y < sh && sy + y < skin.getHeight(); y++) {
                part.setRGB(x, y, skin.getRGB(sx + x, sy + y));
            }
        }
        g.drawImage(part, dx * scale, dy * scale, dw * scale, dh * scale, null);
    }
}