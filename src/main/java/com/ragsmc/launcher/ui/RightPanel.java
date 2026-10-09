package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.ui.theme.Colors;
import java.awt.Desktop;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

public class RightPanel extends JPanel {

    private final Consumer<String> onNavigate;
    private final File gameDir;

    public RightPanel(Consumer<String> onNavigate, File gameDir) {
        this.onNavigate = onNavigate;
        this.gameDir = gameDir;
        setPreferredSize(new Dimension(260, 0));
        setBackground(Colors.BACKGROUND_DARK);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 15));
        add(sectionCard("Personaliza tu juego", new String[][]{
            {"Recursos", "Mods, shaders y texturas", "resources"},
            {"Mods", "Amplia tu juego", "resources"}
        }));
        add(Box.createRigidArea(new Dimension(0, 12)));
        add(sectionCard("Acceso rapido", new String[][]{
            {"Abrir carpeta", "Explorar directorio", "action:folder"},
            {"Consola", "Ver registros", "console"},
            {"Respaldos", "Gestionar copias", "action:backups"},
            {"Opciones", "Configuracion avanzada", "settings"}
        }));
    }

    private JPanel sectionCard(String title, String[][] items) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Colors.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.setMaximumSize(new Dimension(240, 400));
        card.setAlignmentX(CENTER_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(Colors.TEXT_PRIMARY);
        t.setAlignmentX(LEFT_ALIGNMENT);
        card.add(t);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        for (String[] item : items) {
            card.add(actionRow(item[0], item[1], item[2]));
            card.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        return card;
    }

    private JButton actionRow(String title, String subtitle, String target) {
        JButton b = new JButton("<html><b>" + title + "</b><br><small>" + subtitle + "</small></html>");
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(216, 52));
        b.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        b.setBackground(Colors.BACKGROUND);
        b.setForeground(Colors.TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> onAction(target));
        return b;
    }

    private void onAction(String target) {
        try {
            if (target.startsWith("action:")) {
                String act = target.substring(7);
                if (act.equals("folder")) {
                    openFolder(gameDir);
                } else if (act.equals("backups")) {
                    File dir = new File(gameDir, "backups");
                    dir.mkdirs();
                    openFolder(dir);
                }
                return;
            }
            if (onNavigate != null) {
                onNavigate.accept(target);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Accion", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openFolder(File dir) throws Exception {
        if (dir != null) {
            dir.mkdirs();
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(dir);
        } else {
            throw new Exception("No se puede abrir el explorador en este sistema.");
        }
    }
}