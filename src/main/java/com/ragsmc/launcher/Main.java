package com.ragsmc.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.ragsmc.launcher.ui.MainWindow;
import java.awt.Color;
import java.awt.Font;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        try {
            com.ragsmc.launcher.util.Logger.init(new java.io.File(
                    com.ragsmc.launcher.util.AppDirs.dataDir(), "ragsmc_debug.log"));
        } catch (Exception ignored) {
        }
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                com.ragsmc.launcher.util.Logger.error("Excepcion no capturada en " + t.getName(), e);
            } catch (Exception ignored) {
            }
        });
        FlatDarkLaf.setup();
        applyRagsMcTheme();
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            new Launcher(window).wire();
            window.setVisible(true);
        });
    }

    private static void applyRagsMcTheme() {
        Color bg = new Color(0x0a0e0d);
        Color bgDark = new Color(0x060807);
        Color field = new Color(0x161c1a);
        Color neon = new Color(0x00ff88);
        Color text = new Color(0xf4f4f5);
        UIManager.put("Panel.background", bg);
        UIManager.put("Frame.background", bg);
        UIManager.put("OptionPane.background", bg);
        UIManager.put("TextField.background", field);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.caretForeground", neon);
        UIManager.put("ComboBox.background", field);
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("List.background", new Color(0x101413));
        UIManager.put("List.foreground", text);
        UIManager.put("List.selectionBackground", new Color(0x0d3324));
        UIManager.put("List.selectionForeground", neon);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.track", bgDark);
        UIManager.put("TabbedPane.selectedBackground", bg);
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("defaultFont", base);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
    }
}