package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.minecraft.InstanceStore;
import com.ragsmc.launcher.mods.ContentManager;
import com.ragsmc.launcher.mods.ModrinthAPI;
import com.ragsmc.launcher.ui.theme.Colors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class InstanceDetailView extends JDialog {

    private final InstanceStore.Instance instance;
    private final ModrinthAPI modrinth;
    private final File modsDir;
    private final File packsDir;
    private final File shadersDir;

    public InstanceDetailView(JFrame owner, InstanceStore.Instance instance, ModrinthAPI modrinth) {
        super(owner, "Contenido - " + instance.name, true);
        this.instance = instance;
        this.modrinth = modrinth;
        File base = new File(instance.gameDir);
        this.modsDir = new File(base, "mods");
        this.packsDir = new File(base, "resourcepacks");
        this.shadersDir = new File(base, "shaderpacks");
        setLayout(new BorderLayout(8, 8));
        setSize(860, 600);
        setLocationRelativeTo(owner);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Colors.BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        JLabel title = new JLabel(instance.name + "  -  " + instance.version + " " + instance.loader);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Colors.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        JButton play = new JButton("Jugar");
        play.setBackground(Colors.NEON_GREEN);
        play.setForeground(Color.BLACK);
        play.setFocusPainted(false);
        play.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        play.setCursor(new Cursor(Cursor.HAND_CURSOR));
        play.addActionListener(e -> {
            dispose();
            if (onPlay != null) {
                onPlay.run();
            }
        });
        header.add(play, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Colors.BACKGROUND);
        tabs.setForeground(Colors.TEXT_PRIMARY);
        tabs.addTab("Contenido", buildContentTab());
        tabs.addTab("Archivos", buildFilesTab());
        tabs.addTab("Mundos", buildWorldsTab());
        tabs.addTab("Logs", buildLogsTab());
        tabs.addTab("Capturas", buildShotsTab());
        add(tabs, BorderLayout.CENTER);
    }

    private Runnable onPlay;

    public void setOnPlay(Runnable onPlay) {
        this.onPlay = onPlay;
    }
    private JPanel buildContentTab() {
        JTabbedPane sub = new JTabbedPane();
        sub.setBackground(Colors.BACKGROUND);
        sub.setForeground(Colors.TEXT_PRIMARY);
        sub.addTab("Mods", new ContentTabPanel(modsDir, new String[]{".jar"}));
        sub.addTab("Resource Packs", new ContentTabPanel(packsDir, new String[]{".zip"}));
        sub.addTab("Shaders", new ContentTabPanel(shadersDir, new String[]{".zip"}));
        sub.addTab("Explorar Mods", buildExplorePanel());
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Colors.BACKGROUND);
        wrap.add(sub, BorderLayout.CENTER);
        return wrap;
    }

    private class ContentTabPanel extends JPanel {
        private final File dir;
        private final String[] exts;
        private final DefaultListModel<String> model = new DefaultListModel<>();
        private final JList<String> list = new JList<>(model);
        private final JLabel stats = new JLabel("");
        private final JTextField filter = new JTextField();
        private List<ContentManager.ContentItem> items = new ArrayList<>();

        ContentTabPanel(File dir, String[] exts) {
            this.dir = dir;
            this.exts = exts;
            setLayout(new BorderLayout(6, 6));
            setBackground(Colors.BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setBackground(Colors.BACKGROUND);
            stats.setForeground(Colors.TEXT_SECONDARY);
            stats.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            top.add(stats, BorderLayout.WEST);
            filter.setBackground(Colors.FIELD_BACKGROUND);
            filter.setForeground(Colors.TEXT_PRIMARY);
            filter.setCaretColor(Colors.NEON_GREEN);
            filter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            });
            top.add(filter, BorderLayout.CENTER);
            JPanel topBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            topBtns.setBackground(Colors.BACKGROUND);
            JButton refresh = ghostButton("Actualizar");
            refresh.addActionListener(e -> refresh());
            JButton open = ghostButton("Abrir carpeta");
            open.addActionListener(e -> openFolder(dir));
            topBtns.add(refresh);
            topBtns.add(open);
            top.add(topBtns, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
            list.setBackground(Colors.CARD_BG);
            list.setForeground(Colors.TEXT_PRIMARY);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
            add(scroll, BorderLayout.CENTER);
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            bottom.setBackground(Colors.BACKGROUND);
            JButton toggle = ghostButton("Activar/Desactivar");
            toggle.addActionListener(e -> toggleSelected());
            JButton del = ghostButton("Eliminar");
            del.addActionListener(e -> deleteSelected());
            bottom.add(toggle);
            bottom.add(del);
            add(bottom, BorderLayout.SOUTH);
            refresh();
        }

        private JButton ghostButton(String text) {
            JButton b = new JButton(text);
            b.setBackground(Colors.FIELD_BACKGROUND);
            b.setForeground(Colors.TEXT_PRIMARY);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return b;
        }

        private void refresh() {
            SwingUtilities.invokeLater(() -> {
                items = ContentManager.list(dir, exts);
                applyFilter();
                stats.setText(ContentManager.stats(items));
            });
        }

        private void applyFilter() {
            String q = filter.getText().trim().toLowerCase();
            model.clear();
            for (ContentManager.ContentItem i : items) {
                if (!q.isEmpty() && !i.name.toLowerCase().contains(q)) {
                    continue;
                }
                model.addElement((i.enabled ? "[ON] " : "[OFF] ") + i.name + "  (" + ContentManager.sizeText(i.size) + ")");
            }
            stats.setText(ContentManager.stats(items));
        }

        private ContentManager.ContentItem selected() {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                return null;
            }
            String shown = model.get(idx);
            String q = filter.getText().trim().toLowerCase();
            for (ContentManager.ContentItem i : items) {
                if (!q.isEmpty() && !i.name.toLowerCase().contains(q)) {
                    continue;
                }
                String row = (i.enabled ? "[ON] " : "[OFF] ") + i.name + "  (" + ContentManager.sizeText(i.size) + ")";
                if (row.equals(shown)) {
                    return i;
                }
            }
            return null;
        }

        private void toggleSelected() {
            ContentManager.ContentItem i = selected();
            if (i == null) {
                JOptionPane.showMessageDialog(this, "Selecciona un elemento.", "Sin seleccion", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                ContentManager.setEnabled(i, !i.enabled);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void deleteSelected() {
            ContentManager.ContentItem i = selected();
            if (i == null) {
                JOptionPane.showMessageDialog(this, "Selecciona un elemento.", "Sin seleccion", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int r = JOptionPane.showConfirmDialog(this, "Eliminar " + i.name + "?", "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                ContentManager.delete(i);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void openFolder(File d) {
            try {
                d.mkdirs();
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(d);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildExplorePanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(Colors.BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBackground(Colors.BACKGROUND);
        JComboBox<String> loaderBox = new JComboBox<>(new String[]{"fabric", "forge", "vanilla", "quilt", "neoforge"});
        loaderBox.setBackground(Colors.FIELD_BACKGROUND);
        loaderBox.setForeground(Colors.TEXT_PRIMARY);
        bar.add(loaderBox, BorderLayout.WEST);
        JTextField query = new JTextField();
        query.setBackground(Colors.FIELD_BACKGROUND);
        query.setForeground(Colors.TEXT_PRIMARY);
        query.setCaretColor(Colors.NEON_GREEN);
        bar.add(query, BorderLayout.CENTER);
        JButton search = new JButton("Buscar");
        search.setBackground(Colors.NEON_GREEN);
        search.setForeground(java.awt.Color.BLACK);
        search.setFocusPainted(false);
        search.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        search.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bar.add(search, BorderLayout.EAST);
        p.add(bar, BorderLayout.NORTH);
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> results = new JList<>(model);
        results.setBackground(Colors.CARD_BG);
        results.setForeground(Colors.TEXT_PRIMARY);
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(results);
        scroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
        p.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setBackground(Colors.BACKGROUND);
        JButton install = new JButton("Instalar en esta instancia");
        install.setBackground(Colors.NEON_GREEN);
        install.setForeground(java.awt.Color.BLACK);
        install.setFocusPainted(false);
        install.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        install.setCursor(new Cursor(Cursor.HAND_CURSOR));
        final List<ModrinthAPI.ModResult> found = new ArrayList<>();
        search.addActionListener(e -> {
            String q = query.getText().trim();
            if (q.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe que buscar.", "Vacio", JOptionPane.WARNING_MESSAGE);
                return;
            }
            search.setEnabled(false);
            search.setText("Buscando...");
            new SwingWorker<List<ModrinthAPI.ModResult>, Void>() {
                @Override
                protected List<ModrinthAPI.ModResult> doInBackground() throws Exception {
                    return modrinth.search(q, instance.version, (String) loaderBox.getSelectedItem(), 20);
                }
                @Override
                protected void done() {
                    search.setEnabled(true);
                    search.setText("Buscar");
                    try {
                        found.clear();
                        found.addAll(get());
                        model.clear();
                        for (ModrinthAPI.ModResult r : found) {
                            model.addElement(r.title + " - " + r.downloads + " descargas");
                        }
                        if (found.isEmpty()) {
                            model.addElement("Sin resultados.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(InstanceDetailView.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        install.addActionListener(e -> {
            int idx = results.getSelectedIndex();
            if (idx < 0 || idx >= found.size()) {
                JOptionPane.showMessageDialog(this, "Selecciona un resultado.", "Sin seleccion", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ModrinthAPI.ModResult r = found.get(idx);
            install.setEnabled(false);
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String url = modrinth.latestVersionFileUrl(r.id, instance.version, (String) loaderBox.getSelectedItem());
                    if (url == null) {
                        throw new Exception("Sin archivo compatible con " + instance.version);
                    }
                    String name = url.substring(url.lastIndexOf("/") + 1);
                    File dest = new File(modsDir, name);
                    new com.ragsmc.launcher.download.DownloadManager().download(url, dest.toPath(), null);
                    publish("Instalado: " + name);
                    return null;
                }
                @Override
                protected void process(List<String> chunks) {
                    for (String c : chunks) {
                        JOptionPane.showMessageDialog(InstanceDetailView.this, c, "Modrinth", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                @Override
                protected void done() {
                    install.setEnabled(true);
                    try {
                        get();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(InstanceDetailView.this, "Fallo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        bottom.add(install);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildFilesTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(Colors.BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setBackground(Colors.CARD_BG);
        list.setForeground(Colors.TEXT_PRIMARY);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        File base = new File(instance.gameDir);
        File[] files = base.listFiles();
        if (files != null) {
            java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File f : files) {
                String size = f.isDirectory() ? "<carpeta>" : ContentManager.sizeText(f.length());
                model.addElement(f.getName() + "  |  " + size + "  |  " + new java.util.Date(f.lastModified()));
            }
        }
        if (model.isEmpty()) {
            model.addElement("(carpeta vacia)");
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
        p.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setBackground(Colors.BACKGROUND);
        JButton open = new JButton("Abrir carpeta");
        open.setBackground(Colors.FIELD_BACKGROUND);
        open.setForeground(Colors.TEXT_PRIMARY);
        open.setFocusPainted(false);
        open.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        open.addActionListener(e -> {
            try {
                base.mkdirs();
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(base);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        bottom.add(open);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildWorldsTab() {
        return dirListTab(new File(new File(instance.gameDir), "saves"), "Sin mundos guardados todavia.");
    }

    private JPanel buildShotsTab() {
        return dirListTab(new File(new File(instance.gameDir), "screenshots"), "Sin capturas todavia. F2 en el juego.");
    }

    private JPanel dirListTab(File dir, String emptyText) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(Colors.BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        DefaultListModel<String> model = new DefaultListModel<>();
        dir.mkdirs();
        File[] files = dir.listFiles();
        if (files != null) {
            java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File f : files) {
                model.addElement(f.getName());
            }
        }
        if (model.isEmpty()) {
            model.addElement(emptyText);
        }
        JList<String> list = new JList<>(model);
        list.setBackground(Colors.CARD_BG);
        list.setForeground(Colors.TEXT_PRIMARY);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildLogsTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(Colors.BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(java.awt.Color.BLACK);
        area.setForeground(new Color(0x00ff88));
        area.setFont(new Font("Consolas", Font.PLAIN, 11));
        File log = new File(new File(new File(instance.gameDir), "logs"), "latest.log");
        try {
            if (log.exists()) {
                byte[] data = java.nio.file.Files.readAllBytes(log.toPath());
                String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                String[] lines = text.split("\n");
                int from = Math.max(0, lines.length - 300);
                StringBuilder sb = new StringBuilder();
                for (int i = from; i < lines.length; i++) {
                    sb.append(lines[i]).append("\n");
                }
                area.setText(sb.toString());
                area.setCaretPosition(area.getDocument().getLength());
            } else {
                area.setText("Sin logs. Juega primero para generar latest.log.");
            }
        } catch (Exception ex) {
            area.setText("No se pudo leer el log: " + ex.getMessage());
        }
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }
}
