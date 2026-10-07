package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.minecraft.InstanceStore;
import com.ragsmc.launcher.ui.theme.Colors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JSlider;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class InstallationsView extends JPanel {

    private final JPanel cardsPanel;
    private final JLabel countLabel;
    private final JFrame owner;
    private java.util.List<InstanceStore.Instance> current = new java.util.ArrayList<>();
    private java.util.List<String> knownVersions = new java.util.ArrayList<>();
    private Consumer<InstanceStore.Instance> onPlay;
    private Consumer<NewInstance> onCreate;
    private Consumer<InstanceStore.Instance> onDuplicate;
    private Consumer<InstanceStore.Instance> onDelete;

    public static class NewInstance {
        public String name = "";
        public String description = "";
        public String version = "";
        public String loader = "vanilla";
        public int ramMb = 4096;
        public String iconPath = "";
        public String bgPath = "";
    }

    public InstallationsView(JFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout(10, 10));
        setBackground(Colors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Colors.BACKGROUND);
        JLabel title = new JLabel("Instalaciones", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Colors.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setBackground(Colors.BACKGROUND);
        countLabel = new JLabel("");
        countLabel.setForeground(Colors.TEXT_SECONDARY);
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        headerRight.add(countLabel);
        JButton nueva = new JButton("+ Nueva");
        nueva.setBackground(Colors.NEON_GREEN);
        nueva.setForeground(Color.BLACK);
        nueva.setFocusPainted(false);
        nueva.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        nueva.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nueva.addActionListener(e -> openCreateDialog());
        headerRight.add(nueva);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(Colors.BACKGROUND);
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBackground(Colors.BACKGROUND);
        scroll.getViewport().setBackground(Colors.BACKGROUND);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
    }

    private void openCreateDialog() {
        JDialog dlg = new JDialog(owner, "Nueva instalacion", true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(480, 560);
        dlg.setLocationRelativeTo(this);
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Colors.BACKGROUND);
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        JPanel mediaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        mediaRow.setBackground(Colors.BACKGROUND);
        mediaRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel iconPreview = new JLabel("Sin icono");
        iconPreview.setPreferredSize(new Dimension(64, 64));
        iconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        iconPreview.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
        iconPreview.setForeground(Colors.TEXT_SECONDARY);
        final String[] iconPath = {""};
        JButton bgBtn = new JButton("Poner fondo");
        styleGhost(bgBtn);
        final String[] bgPath = {""};
        final JLabel bgLabel = new JLabel("Sin fondo");
        bgLabel.setForeground(Colors.TEXT_SECONDARY);
        bgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        bgBtn.addActionListener(e -> {
            JFileChooser fc2 = new JFileChooser();
            fc2.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagen", "png", "jpg", "jpeg"));
            if (fc2.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                bgPath[0] = fc2.getSelectedFile().getAbsolutePath();
                bgLabel.setText(fc2.getSelectedFile().getName());
            }
        });
        mediaRow.add(bgBtn);
        mediaRow.add(bgLabel);
        JButton iconBtn = new JButton("Poner icono");
        styleGhost(iconBtn);
        iconBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagen", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                iconPath[0] = fc.getSelectedFile().getAbsolutePath();
                javax.swing.ImageIcon ic = com.ragsmc.launcher.ui.theme.Images.icon("logo_small.png", 1, 1);
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(fc.getSelectedFile());
                    if (img != null) {
                        java.awt.Image scaled = img.getScaledInstance(64, 64, java.awt.Image.SCALE_SMOOTH);
                        iconPreview.setIcon(new javax.swing.ImageIcon(scaled));
                        iconPreview.setText("");
                    }
                } catch (Exception ex) {
                    iconPreview.setText("Error");
                }
            }
        });
        mediaRow.add(iconPreview);
        mediaRow.add(iconBtn);
        form.add(mediaRow);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(fieldRow("Nombre:", new JTextField("Mi instalacion")));
        JTextField nameField = (JTextField) ((JPanel) form.getComponent(2)).getComponent(1);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel descLabel = new JLabel("Descripcion (opcional):");
        descLabel.setForeground(Colors.TEXT_PRIMARY);
        descLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(descLabel);
        javax.swing.JTextArea descArea = new javax.swing.JTextArea(2, 30);
        descArea.setBackground(Colors.FIELD_BACKGROUND);
        descArea.setForeground(Colors.TEXT_PRIMARY);
        descArea.setCaretColor(Colors.NEON_GREEN);
        descArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setAlignmentX(LEFT_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(440, 56));
        form.add(descScroll);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        // Filtrar solo versiones permitidas
        List<String> allowed = new ArrayList<>();
        for (String v : knownVersions) {
            if (com.ragsmc.launcher.minecraft.AllowedVersions.isAllowed(v)) {
                allowed.add(v);
            }
        }
        if (allowed.isEmpty()) {
            allowed.addAll(com.ragsmc.launcher.minecraft.AllowedVersions.ALLOWED);
        }
        String[] versions = allowed.toArray(new String[0]);
        JComboBox<String> versionCombo = new JComboBox<>(versions);
        styleCombo(versionCombo);
        form.add(comboRow("Version:", versionCombo));
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        JComboBox<String> loaderCombo = new JComboBox<>(new String[]{"vanilla", "fabric", "forge", "quilt", "neoforge"});
        styleCombo(loaderCombo);
        form.add(comboRow("Loader:", loaderCombo));
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel ramLabel = new JLabel("RAM: 4096 MB");
        ramLabel.setForeground(Colors.TEXT_PRIMARY);
        ramLabel.setAlignmentX(LEFT_ALIGNMENT);
        long totalRam = com.ragsmc.launcher.util.SystemInfo.totalRamMb();
        int cap = (int) com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb();
        cap = Math.max(cap, 512);
        JSlider ramSlider = new JSlider(512, cap, Math.min(4096, cap));
        ramSlider.setBackground(Colors.BACKGROUND);
        ramSlider.setForeground(Colors.NEON_GREEN);
        ramSlider.setMajorTickSpacing(1024);
        ramSlider.setPaintLabels(false);
        ramSlider.setAlignmentX(LEFT_ALIGNMENT);
        ramSlider.setMaximumSize(new Dimension(440, 50));
        final int capFinal = cap;
        ramSlider.addChangeListener(e -> ramLabel.setText("RAM: " + ramSlider.getValue() + " MB (maximo " + capFinal + " MB)"));
        ramLabel.setText("RAM: " + ramSlider.getValue() + " MB (maximo " + cap + " MB)");
        form.add(ramLabel);
        form.add(ramSlider);
        dlg.add(form, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(Colors.BACKGROUND);
        JButton cancel = new JButton("Cancelar");
        styleGhost(cancel);
        cancel.addActionListener(e -> dlg.dispose());
        JButton create = new JButton("Crear instalacion");
        create.setBackground(Colors.NEON_GREEN);
        create.setForeground(Color.BLACK);
        create.setFocusPainted(false);
        create.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        create.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Escribe un nombre.", "Falta el nombre", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (onCreate != null) {
                NewInstance ni = new NewInstance();
                ni.name = name;
                ni.description = descArea.getText().trim();
                ni.version = (String) versionCombo.getSelectedItem();
                ni.loader = (String) loaderCombo.getSelectedItem();
                ni.ramMb = ramSlider.getValue();
                ni.iconPath = iconPath[0];
                ni.bgPath = bgPath[0];
                onCreate.accept(ni);
            }
            dlg.dispose();
        });
        buttons.add(cancel);
        buttons.add(create);
        dlg.add(buttons, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel fieldRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Colors.BACKGROUND);
        row.setMaximumSize(new Dimension(400, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setForeground(Colors.TEXT_PRIMARY);
        l.setPreferredSize(new Dimension(70, 28));
        field.setBackground(Colors.FIELD_BACKGROUND);
        field.setForeground(Colors.TEXT_PRIMARY);
        field.setCaretColor(Colors.NEON_GREEN);
        row.add(l, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel comboRow(String label, JComboBox<String> combo) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Colors.BACKGROUND);
        row.setMaximumSize(new Dimension(400, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setForeground(Colors.TEXT_PRIMARY);
        l.setPreferredSize(new Dimension(70, 28));
        row.add(l, BorderLayout.WEST);
        row.add(combo, BorderLayout.CENTER);
        return row;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(Colors.FIELD_BACKGROUND);
        combo.setForeground(Colors.TEXT_PRIMARY);
    }

    private void styleGhost(JButton b) {
        b.setBackground(Colors.FIELD_BACKGROUND);
        b.setForeground(Colors.TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    public void setKnownVersions(List<String> versions) {
        this.knownVersions = new java.util.ArrayList<>(versions);
    }

    public void setOnPlay(Consumer<InstanceStore.Instance> onPlay) {
        this.onPlay = onPlay;
    }

    public void setOnCreate(Consumer<NewInstance> onCreate) {
        this.onCreate = onCreate;
    }

    public void setOnDuplicate(Consumer<InstanceStore.Instance> onDuplicate) {
        this.onDuplicate = onDuplicate;
    }

    public void setOnDelete(Consumer<InstanceStore.Instance> onDelete) {
        this.onDelete = onDelete;
    }

    public void setInstances(final List<InstanceStore.Instance> list) {
        SwingUtilities.invokeLater(() -> {
            current = new java.util.ArrayList<>(list);
            cardsPanel.removeAll();
            for (InstanceStore.Instance in : list) {
                cardsPanel.add(instanceCard(in));
                cardsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            if (list.isEmpty()) {
                JLabel empty = new JLabel("Sin instalaciones. Crea una con + Nueva o pulsa JUGAR.", SwingConstants.CENTER);
                empty.setForeground(Colors.TEXT_SECONDARY);
                empty.setAlignmentX(CENTER_ALIGNMENT);
                cardsPanel.add(empty);
            }
            countLabel.setText(list.size() + " instalada(s)");
            cardsPanel.revalidate();
            cardsPanel.repaint();
        });
    }

    private JPanel instanceCard(InstanceStore.Instance in) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Colors.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(900, 76));
        card.setAlignmentX(LEFT_ALIGNMENT);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(Colors.CARD_BG);
        JLabel name = new JLabel(in.name);
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setForeground(Colors.TEXT_PRIMARY);
        name.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel(in.version + "  -  " + in.loader + "  -  " + (in.ramMb / 1024) + " GB" + (in.lastPlayed != null && !in.lastPlayed.equals("Nunca") ? "  -  " + in.lastPlayed : ""));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(Colors.TEXT_SECONDARY);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        text.add(name);
        text.add(sub);
        card.add(text, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setBackground(Colors.CARD_BG);
        JButton play = new JButton("Jugar");
        play.setBackground(Colors.NEON_GREEN);
        play.setForeground(Color.BLACK);
        play.setFocusPainted(false);
        play.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        play.setCursor(new Cursor(Cursor.HAND_CURSOR));
        play.addActionListener(e -> {
            if (onPlay != null) {
                onPlay.accept(in);
            }
        });
        JButton edit = new JButton("Editar");
        styleGhost(edit);
        edit.addActionListener(e -> editDialog(in));
        JButton dup = new JButton("Duplicar");
        styleGhost(dup);
        dup.addActionListener(e -> {
            if (onDuplicate != null) {
                onDuplicate.accept(in);
            }
        });
        JButton del = new JButton("Eliminar");
        styleGhost(del);
        del.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this, "Eliminar " + in.name + " y su carpeta?", "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.YES_OPTION && onDelete != null) {
                onDelete.accept(in);
            }
        });
        JButton content = new JButton("Contenido");
        styleGhost(content);
        content.addActionListener(e -> {
            if (onContent != null) {
                onContent.accept(in);
            }
        });
        buttons.add(play);
        buttons.add(content);
        buttons.add(edit);
        buttons.add(dup);
        buttons.add(del);
        card.add(buttons, BorderLayout.EAST);
        return card;
    }

    private void editDialog(InstanceStore.Instance in) {
        String name = JOptionPane.showInputDialog(this, "Nombre:", in.name);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        int cap = (int) com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb();
        java.util.List<String> ramOpts = new java.util.ArrayList<>();
        for (String r0 : new String[]{"1024", "2048", "3072", "4096", "6144", "8192"}) {
            if (Integer.parseInt(r0) <= cap) {
                ramOpts.add(r0);
            }
        }
        for (int r0 = 9216; r0 <= cap; r0 += 1024) {
            ramOpts.add(String.valueOf(r0));
        }
        if (ramOpts.isEmpty()) {
            ramOpts.add("1024");
        }
        String[] rams = ramOpts.toArray(new String[0]);
        String ram = (String) JOptionPane.showInputDialog(this, "RAM (MB):", "Editar",
                JOptionPane.PLAIN_MESSAGE, null, rams, String.valueOf(in.ramMb));
        if (ram == null) {
            return;
        }
        in.name = name.trim();
        try {
            in.ramMb = (int) Math.min(Integer.parseInt(ram), com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb());
        } catch (Exception ignored) {
        }
        if (editHandler != null) {
            editHandler.accept(in);
        }
    }

    private Consumer<InstanceStore.Instance> editHandler;
    private Consumer<InstanceStore.Instance> onContent;

    public void setOnContent(Consumer<InstanceStore.Instance> onContent) {
        this.onContent = onContent;
    }

    public void setOnEdit(Consumer<InstanceStore.Instance> onEdit) {
        this.editHandler = onEdit;
    }

    // Compatibilidad con cableado anterior
    public void addInstallation(final String name) {
    }

    public void clearList() {
    }

    public String getSelectedInstallation() {
        return null;
    }

    public String getNewName() {
        return "";
    }

    public String getNewVersion() {
        return "1.21.1";
    }

    public String getNewLoader() {
        return "vanilla";
    }

    public void setOnRefreshNeeded(Runnable r) {
    }
}