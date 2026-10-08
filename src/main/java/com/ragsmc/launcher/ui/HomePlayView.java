package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.minecraft.InstanceStore;
import com.ragsmc.launcher.ui.theme.Colors;
import com.ragsmc.launcher.ui.theme.Images;
import com.ragsmc.launcher.util.SystemInfo;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class HomePlayView extends JPanel {

    private static final int[] RAM_STEPS = {512, 1024, 2048, 3072, 4096, 6144, 8192};

    private final JComboBox<String> profileCombo;
    private final JComboBox<String> instanceCombo;
    private final JLabel versionValue;
    private final JComboBox<String> ramCombo;
    private final JButton playButton;
    private final JButton createFirstButton;
    private final JLabel statusLabel;
    private final JLabel ramInfoLabel;
    private final JScrollPane mainScroll;
    private final BufferedImage background;
    private List<InstanceStore.Instance> instances = new ArrayList<>();
    private JLabel greetingLabel;
    private JLabel cardModsValue;
    private JLabel cardMundosValue;
    private JLabel cardPacksValue;
    private JLabel selTitleLabel;
    private JLabel selDetailLabel;
    private Runnable onShowInstallations;
    private Runnable onInstanceChanged;
    private Runnable onRamChanged;
    private Runnable onCreateFirst;
    private Runnable onPlay;
    private boolean syncing = false;

    public HomePlayView() {
        background = Images.image("background.jpg");
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        BgPanel content = new BgPanel(background);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        ImageIcon titleIcon = Images.icon("titulo.png", 420, 90);
        JLabel title;
        if (titleIcon != null) {
            title = new JLabel(titleIcon, SwingConstants.CENTER);
        } else {
            title = new JLabel("MINECRAFT", SwingConstants.CENTER) {
                @Override
                public void doLayout() {
                    super.doLayout();
                    int w = getWidth();
                    int h = getHeight();
                    if (w > 0 && h > 0) {
                        // Scale based on both width and height to prevent clipping
                        float sizeByWidth = Math.min(44f, Math.max(20f, w / 9.5f));
                        float sizeByHeight = Math.min(44f, Math.max(20f, h * 0.7f));
                        float size = Math.min(sizeByWidth, sizeByHeight);
                        setFont(getFont().deriveFont(size));
                    }
                }
            };
            title.setFont(new Font("Segoe UI", Font.BOLD, 44));
            title.setForeground(Colors.NEON_GREEN);
        }
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setHorizontalTextPosition(SwingConstants.CENTER);
        content.add(title);
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        JLabel subtitle = new JLabel("CREA - EXPLORA - SOBREVIVE", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(Colors.NEON_GREEN);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        greetingLabel = new JLabel("Hola!", SwingConstants.CENTER);
        greetingLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        greetingLabel.setForeground(Colors.TEXT_PRIMARY);
        greetingLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(greetingLabel);
        content.add(Box.createRigidArea(new Dimension(0, 4)));
        content.add(subtitle);
        content.add(Box.createRigidArea(new Dimension(0, 14)));
        JPanel javaCard = createEditionCard("JAVA EDITION");
        javaCard.setAlignmentX(CENTER_ALIGNMENT);
        content.add(javaCard);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(buildSummaryRow());
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(buildSelectedCard());
        content.add(Box.createRigidArea(new Dimension(0, 14)));
        JPanel config = new JPanel(new GridBagLayout());
        config.setOpaque(false);
        config.setAlignmentX(CENTER_ALIGNMENT);
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(4, 8, 4, 8);
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.gridx = 0;
        cc.gridy = 0;
        config.add(configLabel("PERFIL:"), cc);
        cc.gridx = 1;
        profileCombo = new JComboBox<>(new String[]{"RagsnorWolf"});
        styleCombo(profileCombo);
        config.add(profileCombo, cc);
        cc.gridx = 0;
        cc.gridy = 1;
        config.add(configLabel("INSTANCIA:"), cc);
        cc.gridx = 1;
        instanceCombo = new JComboBox<>();
        styleCombo(instanceCombo);
        instanceCombo.addActionListener(e -> onInstancePicked());
        config.add(instanceCombo, cc);
        cc.gridx = 0;
        cc.gridy = 2;
        config.add(configLabel("VERSION:"), cc);
        cc.gridx = 1;
        versionValue = new JLabel("(sin instancia)");
        versionValue.setForeground(Colors.NEON_GREEN);
        versionValue.setFont(new Font("Segoe UI", Font.BOLD, 13));
        versionValue.setPreferredSize(new Dimension(220, 28));
        config.add(versionValue, cc);
        cc.gridx = 0;
        cc.gridy = 3;
        config.add(configLabel("RAM:"), cc);
        cc.gridx = 1;
        ramCombo = new JComboBox<>(ramOptions());
        styleCombo(ramCombo);
        selectSuggestedRam();
        ramCombo.addActionListener(e -> onRamPicked());
        config.add(ramCombo, cc);
        content.add(config);
        long totalRam = SystemInfo.totalRamMb();
        ramInfoLabel = new JLabel("", SwingConstants.CENTER);
        ramInfoLabel.setForeground(Colors.TEXT_SECONDARY);
        ramInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ramInfoLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(ramInfoLabel);
        refreshRamInfo();
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        playButton = new JButton("JUGAR");
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 22));
        playButton.setBackground(Colors.NEON_GREEN);
        playButton.setForeground(Color.BLACK);
        playButton.setFocusPainted(false);
        playButton.setBorder(BorderFactory.createEmptyBorder(12, 60, 12, 60));
        playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playButton.setAlignmentX(CENTER_ALIGNMENT);
        playButton.addActionListener(e -> {
            if (onPlay != null) {
                onPlay.run();
            }
        });
        content.add(playButton);
        centerWrap(content);
        createFirstButton = new JButton("Crear primera instancia");
        createFirstButton.setBackground(Colors.FIELD_BACKGROUND);
        createFirstButton.setForeground(Colors.NEON_GREEN);
        createFirstButton.setFocusPainted(false);
        createFirstButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        createFirstButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createFirstButton.setAlignmentX(CENTER_ALIGNMENT);
        createFirstButton.setVisible(false);
        createFirstButton.addActionListener(e -> {
            if (onCreateFirst != null) {
                onCreateFirst.run();
            }
        });
        content.add(createFirstButton);
        statusLabel = new JLabel("Listo para jugar", SwingConstants.CENTER);
        statusLabel.setForeground(Colors.TEXT_SECONDARY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(statusLabel);
        content.add(Box.createRigidArea(new Dimension(0, 18)));
        mainScroll = new JScrollPane(content);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.getVerticalScrollBar().setUnitIncrement(24);
        add(mainScroll, BorderLayout.CENTER);
    }
    private void centerWrap(JPanel content) {
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        l.setForeground(Colors.TEXT_PRIMARY);
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private JLabel configLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Colors.TEXT_PRIMARY);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }



    private void onInstancePicked() {
        if (syncing) {
            return;
        }
        InstanceStore.Instance in = getSelectedInstance();
        if (in != null) {
            versionValue.setText(in.version + "  (" + in.loader + ")");
            setRamMb(in.ramMb);
        } else {
            versionValue.setText("(sin instancia)");
        }
        if (onInstanceChanged != null) {
            onInstanceChanged.run();
        }
    }

    private void onRamPicked() {
        if (syncing) {
            return;
        }
        refreshRamInfo();
        if (onRamChanged != null) {
            onRamChanged.run();
        }
    }

    private void refreshRamInfo() {
        long totalRam = SystemInfo.totalRamMb();
        ramInfoLabel.setText("Asignada: " + selectedRamMb() + " MB / " + totalRam + " MB totales");
    }

    private int selectedRamMb() {
        try {
            return Integer.parseInt(((String) ramCombo.getSelectedItem()).replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 2048;
        }
    }

    private static String[] ramOptions() {
        long total = SystemInfo.totalRamMb();
        long cap = SystemInfo.maxUsableRamMb();
        java.util.List<String> opts = new java.util.ArrayList<>();
        for (int s : RAM_STEPS) {
            if (s <= cap) {
                opts.add(s + " MB");
            }
        }
        for (long v = 9216; v <= cap; v += 1024) {
            opts.add(v + " MB");
        }
        if (opts.isEmpty()) {
            opts.add("512 MB");
        }
        return opts.toArray(new String[0]);
    }

    private void selectSuggestedRam() {
        int suggested = (int) Math.min(SystemInfo.suggestedRamMb(), SystemInfo.maxUsableRamMb());
        int best = 0;
        for (int i = 0; i < ramCombo.getItemCount(); i++) {
            try {
                int v = Integer.parseInt(ramCombo.getItemAt(i).replaceAll("[^0-9]", ""));
                if (v <= suggested) {
                    best = i;
                }
            } catch (Exception ignored) {
            }
        }
        ramCombo.setSelectedIndex(best);
    }

    private JPanel createEditionCard(String name) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(260, 110));
        card.setMaximumSize(new Dimension(260, 110));
        card.setOpaque(true);
        card.setBackground(Colors.CARD_ACTIVE);
        card.setBorder(BorderFactory.createLineBorder(Colors.NEON_GREEN, 2));
        card.setAlignmentX(CENTER_ALIGNMENT);
        JLabel label = new JLabel(name, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(Colors.NEON_GREEN);
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(Colors.FIELD_BACKGROUND);
        combo.setForeground(Colors.TEXT_PRIMARY);
        combo.setMaximumSize(new Dimension(220, 28));
        combo.setPreferredSize(new Dimension(220, 28));
    }

    public void scrollToTop() {
        SwingUtilities.invokeLater(() -> mainScroll.getVerticalScrollBar().setValue(0));
    }

    public void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            javax.swing.JScrollBar bar = mainScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    public void setOnPlay(Runnable onPlay) {
        this.onPlay = onPlay;
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
                boolean empty = list.isEmpty();
                createFirstButton.setVisible(empty);
                playButton.setEnabled(!empty);
                if (sel >= 0) {
                    instanceCombo.setSelectedIndex(sel);
                } else if (!list.isEmpty()) {
                    instanceCombo.setSelectedIndex(0);
                }
                InstanceStore.Instance in = getSelectedInstance();
                if (in != null) {
                    versionValue.setText(in.version + "  (" + in.loader + ")");
                    setRamMb(in.ramMb);
                } else {
                    versionValue.setText("(sin instancia)");
                }
                updateStats();
            } finally {
                syncing = false;
            }
        });
    }

    public InstanceStore.Instance getSelectedInstance() {
        int idx = instanceCombo.getSelectedIndex();
        if (idx < 0 || idx >= instances.size()) {
            return null;
        }
        return instances.get(idx);
    }

    public String getSelectedVersion() {
        InstanceStore.Instance in = getSelectedInstance();
        return in != null ? in.version : null;
    }

    public void setVersions(String[] versions, String selected) {
    }

    public void setOnDownloadVersion(Runnable r) {
    }

    public void setDownloading(final boolean downloading) {
    }

    public void setOnInstanceChanged(Runnable r) {
        this.onInstanceChanged = r;
    }

    public void setOnRamChanged(Runnable r) {
        this.onRamChanged = r;
    }

    public void setOnCreateFirst(Runnable r) {
        this.onCreateFirst = r;
    }

    public void setRamMb(final int ramMb) {
        SwingUtilities.invokeLater(() -> {
            syncing = true;
            try {
                int capped = (int) Math.min(ramMb, SystemInfo.maxUsableRamMb());
                int best = 0;
                for (int i = 0; i < ramCombo.getItemCount(); i++) {
                    try {
                        int v = Integer.parseInt(ramCombo.getItemAt(i).replaceAll("[^0-9]", ""));
                        if (v <= capped) {
                            best = i;
                        }
                    } catch (Exception ignored) {
                    }
                }
                ramCombo.setSelectedIndex(best);
                refreshRamInfo();
            } finally {
                syncing = false;
            }
        });
    }

    public void setLaunching(final boolean launching) {
        SwingUtilities.invokeLater(() -> {
            playButton.setEnabled(!launching && instanceCombo.getItemCount() > 0);
            playButton.setText(launching ? "LANZANDO..." : "JUGAR");
        });
    }

    public String getSelectedProfile() {
        return (String) profileCombo.getSelectedItem();
    }

    public String getSelectedRam() {
        return (String) ramCombo.getSelectedItem();
    }

    public JButton getPlayButton() {
        return playButton;
    }

    public void setStatus(final String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public void setProfiles(String[] names, String selected) {
        SwingUtilities.invokeLater(() -> {
            profileCombo.removeAllItems();
            for (String n : names) {
                profileCombo.addItem(n);
            }
            if (selected != null) {
                profileCombo.setSelectedItem(selected);
            }
        });
    }



    public void setProfile(final String username) {
        SwingUtilities.invokeLater(() -> {
            if (greetingLabel != null) {
                greetingLabel.setText(username != null && !username.isEmpty() ? "Hola, " + username + "!" : "Hola!");
            }
        });
    }

    public void setOnShowInstallations(Runnable r) {
        this.onShowInstallations = r;
    }

    public void setSummary(final int mods, final int mundos, final int packs, final String lastMundo) {
        SwingUtilities.invokeLater(() -> {
            if (cardModsValue != null) {
                cardModsValue.setText(mods + " mods");
            }
            if (cardMundosValue != null) {
                cardMundosValue.setText(mundos + " mundos" + (lastMundo != null ? " - ultimo: " + lastMundo : ""));
            }
            if (cardPacksValue != null) {
                cardPacksValue.setText(packs + " packs");
            }
        });
    }

    public void setSelectedCard(final String name, final String detail) {
        SwingUtilities.invokeLater(() -> {
            if (selTitleLabel != null) {
                selTitleLabel.setText(name != null ? name : "Sin seleccion");
            }
            if (selDetailLabel != null) {
                selDetailLabel.setText(detail != null ? detail : "");
            }
        });
    }

    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.gridx = 0;
        gc.gridy = 0;
        cardModsValue = new JLabel("0 mods");
        row.add(summaryCard("Mods", cardModsValue), gc);
        gc.gridx = 1;
        cardMundosValue = new JLabel("0 mundos");
        row.add(summaryCard("Mundos", cardMundosValue), gc);
        gc.gridx = 2;
        cardPacksValue = new JLabel("0 packs");
        row.add(summaryCard("Packs", cardPacksValue), gc);
        return row;
    }

    private JPanel summaryCard(String title, JLabel value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Colors.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        card.setPreferredSize(new Dimension(150, 76));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(Colors.TEXT_PRIMARY);
        t.setAlignmentX(CENTER_ALIGNMENT);
        value.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        value.setForeground(Colors.TEXT_SECONDARY);
        value.setAlignmentX(CENTER_ALIGNMENT);
        card.add(t);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(value);
        return card;
    }

    private JPanel buildSelectedCard() {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Colors.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.NEON_GREEN, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(560, 110));
        card.setAlignmentX(CENTER_ALIGNMENT);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(Colors.CARD_BG);
        JLabel head = new JLabel("INSTALACION SELECCIONADA");
        head.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        head.setForeground(Colors.TEXT_SECONDARY);
        head.setAlignmentX(LEFT_ALIGNMENT);
        selTitleLabel = new JLabel("Sin seleccion");
        selTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        selTitleLabel.setForeground(Colors.TEXT_PRIMARY);
        selTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        selDetailLabel = new JLabel("");
        selDetailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selDetailLabel.setForeground(Colors.TEXT_SECONDARY);
        selDetailLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(head);
        text.add(selTitleLabel);
        text.add(selDetailLabel);
        card.add(text, BorderLayout.CENTER);
        JButton cfg = new JButton("Configuraciones");
        cfg.setBackground(Colors.FIELD_BACKGROUND);
        cfg.setForeground(Colors.TEXT_PRIMARY);
        cfg.setFocusPainted(false);
        cfg.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        cfg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cfg.addActionListener(e -> {
            if (onShowInstallations != null) {
                onShowInstallations.run();
            }
        });
        card.add(cfg, BorderLayout.EAST);
        return card;
    }



    private void updateStats() {
        // No stats panel anymore
    }

    private static class BgPanel extends JPanel {
        private final BufferedImage bg;

        BgPanel(BufferedImage bg) {
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (bg != null) {
                int w = getWidth();
                int h = getHeight();
                if (w > 0 && h > 0) {
                    double scale = Math.max(w / (double) bg.getWidth(), h / (double) bg.getHeight());
                    int dw = (int) (bg.getWidth() * scale);
                    int dh = (int) (bg.getHeight() * scale);
                    int dx = (w - dw) / 2;
                    int dy = (h - dh) / 2;
                    g.drawImage(bg, dx, dy, dw, dh, null);
                    g.setColor(new Color(6, 8, 7, 190));
                    ((java.awt.Graphics2D) g).setComposite(AlphaComposite.SrcOver);
                    g.fillRect(0, 0, w, h);
                }
            } else {
                g.setColor(Colors.BACKGROUND);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }
    }
}
