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
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class RecursosView extends JPanel {

    private final JComboBox<String> instanceCombo;
    private final JTabbedPane tabs;
    private final ContentTabPanel modsTab;
    private final ContentTabPanel shadersTab;
    private final ContentTabPanel packsTab;
    private final ExplorePanel exploreTab;
    private List<InstanceStore.Instance> instances = new ArrayList<>();
    private boolean syncing = false;

    public RecursosView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Colors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Colors.BACKGROUND);
        JLabel title = new JLabel("RECURSOS", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Colors.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        JPanel instRow = new JPanel(new BorderLayout(8, 0));
        instRow.setBackground(Colors.BACKGROUND);
        JLabel instLabel = new JLabel("Instalacion:");
        instLabel.setForeground(Colors.TEXT_SECONDARY);
        instRow.add(instLabel, BorderLayout.WEST);
        instanceCombo = new JComboBox<>();
        instanceCombo.setBackground(Colors.FIELD_BACKGROUND);
        instanceCombo.setForeground(Colors.TEXT_PRIMARY);
        instanceCombo.setPreferredSize(new Dimension(280, 28));
        instanceCombo.addActionListener(e -> onInstancePicked());
        instRow.add(instanceCombo, BorderLayout.CENTER);
        header.add(instRow, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        tabs = new JTabbedPane();
        tabs.setBackground(Colors.BACKGROUND);
        tabs.setForeground(Colors.TEXT_PRIMARY);
        modsTab = new ContentTabPanel("mods", new String[]{".jar"});
        shadersTab = new ContentTabPanel("shaderpacks", new String[]{".zip"});
        packsTab = new ContentTabPanel("resourcepacks", new String[]{".zip"});
        exploreTab = new ExplorePanel();
        tabs.addTab("Mods", modsTab);
        tabs.addTab("Shaders", shadersTab);
        tabs.addTab("Resource Packs", packsTab);
        tabs.addTab("Explorar", exploreTab);
        add(tabs, BorderLayout.CENTER);
    }

    private void onInstancePicked() {
        if (syncing) {
            return;
        }
        refreshTabs();
    }

    private InstanceStore.Instance selectedInstance() {
        int idx = instanceCombo.getSelectedIndex();
        if (idx < 0 || idx >= instances.size()) {
            return null;
        }
        return instances.get(idx);
    }

    public void setInstances(final List<InstanceStore.Instance> list, final String selectId) {
        SwingUtilities.invokeLater(() -> {
            syncing = true;
            try {
                instances = new ArrayList<>(list);
                instanceCombo.removeAllItems();
                int sel = -1;
                for (int i = 0; i < list.size(); i++) {
                    instanceCombo.addItem(list.get(i).name);
                    if (list.get(i).id.equals(selectId)) {
                        sel = i;
                    }
                }
                if (sel >= 0) {
                    instanceCombo.setSelectedIndex(sel);
                } else if (!list.isEmpty()) {
                    instanceCombo.setSelectedIndex(0);
                }
            } finally {
                syncing = false;
            }
            refreshTabs();
        });
    }

    private void refreshTabs() {
        InstanceStore.Instance in = selectedInstance();
        File base = in != null ? new File(in.gameDir) : null;
        modsTab.setDir(base != null ? new File(base, "mods") : null);
        shadersTab.setDir(base != null ? new File(base, "shaderpacks") : null);
        packsTab.setDir(base != null ? new File(base, "resourcepacks") : null);
        exploreTab.setInstance(in);
    }

    private class ContentTabPanel extends JPanel {
        private File dir;
        private final String[] exts;
        private final DefaultListModel<String> model = new DefaultListModel<>();
        private final JList<String> list = new JList<>(model);
        private final JLabel stats = new JLabel("");
        private final JTextField filter = new JTextField();
        private List<ContentManager.ContentItem> items = new ArrayList<>();

        ContentTabPanel(String dirname, String[] exts) {
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
            open.addActionListener(e -> openFolder());
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
        }

        void setDir(File dir) {
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

        private File currentDir() {
            InstanceStore.Instance in = selectedInstance();
            if (in == null) {
                return null;
            }
            String sub = this == modsTab ? "mods" : (this == shadersTab ? "shaderpacks" : "resourcepacks");
            return new File(new File(in.gameDir), sub);
        }

        private void refresh() {
            SwingUtilities.invokeLater(() -> {
                File d = currentDir();
                items = d != null ? ContentManager.list(d, exts) : new ArrayList<>();
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

        private void openFolder() {
            File d = currentDir();
            if (d == null) {
                return;
            }
            try {
                d.mkdirs();
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(d);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        public void cleanup() {
            items.clear();
            model.clear();
        }
    }
    public void cleanup() {
        // Clear tabs, listeners, workers
        if (modsTab != null) modsTab.cleanup();
        if (shadersTab != null) shadersTab.cleanup();
        if (packsTab != null) packsTab.cleanup();
        if (exploreTab != null) exploreTab.cleanup();
    }
    private class ExplorePanel extends JPanel {
        private final ModrinthAPI modrinth = new ModrinthAPI();
        private final DefaultListModel<String> model = new DefaultListModel<>();
        private final JList<String> results = new JList<>(model);
        private final JTextField query = new JTextField();
        private final JComboBox<String> loaderBox = new JComboBox<>(new String[]{"fabric", "forge", "vanilla", "quilt", "neoforge"});
        private final JButton searchButton = new JButton("Buscar");
        private final JButton installButton = new JButton("Instalar en instancia");
        private final List<ModrinthAPI.ModResult> found = new ArrayList<>();
        private int searchOffset = 0;
        private String lastQuery = "";
        private String lastMcVer = "1.21.1";
        private String lastLoader = "fabric";

        ExplorePanel() {
            setLayout(new BorderLayout(6, 6));
            setBackground(Colors.BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            JPanel bar = new JPanel(new BorderLayout(6, 0));
            bar.setBackground(Colors.BACKGROUND);
            loaderBox.setBackground(Colors.FIELD_BACKGROUND);
            loaderBox.setForeground(Colors.TEXT_PRIMARY);
            bar.add(loaderBox, BorderLayout.WEST);
            query.setBackground(Colors.FIELD_BACKGROUND);
            query.setForeground(Colors.TEXT_PRIMARY);
            query.setCaretColor(Colors.NEON_GREEN);
            bar.add(query, BorderLayout.CENTER);
            searchButton.setBackground(Colors.NEON_GREEN);
            searchButton.setForeground(java.awt.Color.BLACK);
            searchButton.setFocusPainted(false);
            searchButton.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
            searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            searchButton.addActionListener(e -> doSearch());
            bar.add(searchButton, BorderLayout.EAST);
            add(bar, BorderLayout.NORTH);
            results.setBackground(Colors.CARD_BG);
            results.setForeground(Colors.TEXT_PRIMARY);
            results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            results.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            JScrollPane scroll = new JScrollPane(results);
            scroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
            add(scroll, BorderLayout.CENTER);
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            bottom.setBackground(Colors.BACKGROUND);
            installButton.setBackground(Colors.NEON_GREEN);
            installButton.setForeground(java.awt.Color.BLACK);
            installButton.setFocusPainted(false);
            installButton.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            installButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            installButton.addActionListener(e -> doInstall());
            bottom.add(installButton);
            JButton more = new JButton("Cargar mas");
            more.setBackground(Colors.FIELD_BACKGROUND);
            more.setForeground(Colors.TEXT_PRIMARY);
            more.setFocusPainted(false);
            more.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            more.setCursor(new Cursor(Cursor.HAND_CURSOR));
            more.addActionListener(e -> loadMore());
            bottom.add(more);
            add(bottom, BorderLayout.SOUTH);
        }

        void setInstance(InstanceStore.Instance in) {
        }

        private void doSearch() {
            String q = query.getText().trim();
            if (q.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe que buscar.", "Vacio", JOptionPane.WARNING_MESSAGE);
                return;
            }
            InstanceStore.Instance in = selectedInstance();
            String mcVer = in != null ? in.version : "1.21.1";
            searchButton.setEnabled(false);
            searchButton.setText("Buscando...");
            new SwingWorker<List<ModrinthAPI.ModResult>, Void>() {
                @Override
                protected List<ModrinthAPI.ModResult> doInBackground() throws Exception {
                    return modrinth.search(q, mcVer, (String) loaderBox.getSelectedItem(), 20);
                }
                @Override
                protected void done() {
                    searchButton.setEnabled(true);
                    searchButton.setText("Buscar");
                    try {
                        lastQuery = q;
                        lastMcVer = mcVer;
                        lastLoader = (String) loaderBox.getSelectedItem();
                        found.clear();
                        found.addAll(get());
                        searchOffset = found.size();
                        model.clear();
                        for (ModrinthAPI.ModResult r : found) {
                            model.addElement(r.title + " - " + r.downloads + " descargas");
                        }
                        if (found.isEmpty()) {
                            model.addElement("Sin resultados.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ExplorePanel.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private void loadMore() {
            if (lastQuery == null || lastQuery.isEmpty()) {
                return;
            }
            searchButton.setEnabled(false);
            new SwingWorker<java.util.List<ModrinthAPI.ModResult>, Void>() {
                @Override
                protected java.util.List<ModrinthAPI.ModResult> doInBackground() throws Exception {
                    return modrinth.search(lastQuery, lastMcVer, lastLoader, 20, searchOffset);
                }
                @Override
                protected void done() {
                    searchButton.setEnabled(true);
                    try {
                        java.util.List<ModrinthAPI.ModResult> results = get();
                        if (results.isEmpty()) {
                            JOptionPane.showMessageDialog(ExplorePanel.this, "No hay mas resultados.", "Explorar", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        searchOffset += results.size();
                        found.addAll(results);
                        for (ModrinthAPI.ModResult r : results) {
                            model.addElement(r.title + " - " + r.downloads + " descargas");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ExplorePanel.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private void doInstall() {
            int idx = results.getSelectedIndex();
            if (idx < 0 || idx >= found.size()) {
                JOptionPane.showMessageDialog(this, "Selecciona un resultado.", "Sin seleccion", JOptionPane.WARNING_MESSAGE);
                return;
            }
            InstanceStore.Instance in = selectedInstance();
            if (in == null) {
                JOptionPane.showMessageDialog(this, "Selecciona una instancia arriba.", "Sin instancia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ModrinthAPI.ModResult r = found.get(idx);
            installButton.setEnabled(false);
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String url = modrinth.latestVersionFileUrl(r.id, in.version, (String) loaderBox.getSelectedItem());
                    if (url == null) {
                        throw new Exception("Sin archivo compatible con " + in.version);
                    }
                    String name = url.substring(url.lastIndexOf("/") + 1);
                    File modsDir = new File(new File(in.gameDir), "mods");
                    modsDir.mkdirs();
                    new com.ragsmc.launcher.download.DownloadManager().download(url, new File(modsDir, name).toPath(), null);
                    publish("Instalado: " + name + " en " + in.name);
                    return null;
                }
                @Override
                protected void process(List<String> chunks) {
                    for (String c : chunks) {
                        JOptionPane.showMessageDialog(ExplorePanel.this, c, "Modrinth", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                @Override
                protected void done() {
                    installButton.setEnabled(true);
                    try {
                        get();
                        modsTab.refresh();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ExplorePanel.this, "Fallo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
        public void cleanup() {
            found.clear();
            model.clear();
        }
    }
}
