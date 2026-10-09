package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.hardware.GpuDetector;
import com.ragsmc.launcher.hardware.GpuDedicatedHelper;
import com.ragsmc.launcher.hardware.MesaDownloader;
import com.ragsmc.launcher.ui.theme.Colors;
import com.ragsmc.launcher.util.SystemInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class SettingsView extends JPanel {

    private JTextField javaPathField;
    private DefaultListModel<String> javaListModel;
    private JList<String> javaList;
    private final Map<Integer, String> javaPaths = new TreeMap<>();
    private JComboBox<String> ramCombo;
    private JLabel hwLabel;
    private JSlider globalRamSlider;
    private JLabel globalRamLabel;
    private JCheckBox fullscreenBox;
    private JTextField widthField;
    private JTextField heightField;
    private JCheckBox hideOnLaunchBox;
    private JCheckBox gpuBox;
    private JCheckBox mesaBox;
    private JTextField dnsField;
    private JTextField jvmArgsField;
    private JTextField gameArgsField;
    private JLabel updateStatus;
    private Consumer<SettingsData> onSave;
    private Runnable onRefreshJava;
    private Runnable onApplyRamAll;
    private Consumer<Integer> onDownloadJava;
    private com.ragsmc.launcher.ui.MainWindow window;

    public static class SettingsData {
        public String javaPath = "";
        public int ramMb = 4096;
        public int width = 854;
        public int height = 480;
        public boolean fullscreen = false;
        public boolean hideOnLaunch = true;
        public boolean gpuDedicated = false;
        public boolean mesaCpu = false;
        public String dnsOverride = "";
        public String jvmArgs = "";
        public String gameArgs = "";
    }

    public SettingsView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Colors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("AJUSTES", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Colors.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Colors.BACKGROUND);
        tabs.setForeground(Colors.TEXT_PRIMARY);
        tabs.addTab("Java", javaTab());
        tabs.addTab("Memoria", memoryTab());
        tabs.addTab("Pantalla", screenTab());
        tabs.addTab("Avanzado", advancedTab());
        tabs.addTab("Actualizaciones", updatesTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel javaTab() {
        JPanel form = vbox();
        form.add(sectionLabel("Java detectada"));
        javaListModel = new DefaultListModel<>();
        javaList = new JList<>(javaListModel);
        javaList.setBackground(Colors.CARD_BG);
        javaList.setForeground(Colors.TEXT_PRIMARY);
        javaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        javaList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        javaList.setVisibleRowCount(4);
        JScrollPane javaScroll = new JScrollPane(javaList);
        javaScroll.setAlignmentX(LEFT_ALIGNMENT);
        javaScroll.setMaximumSize(new Dimension(560, 110));
        javaScroll.setPreferredSize(new Dimension(560, 110));
        javaScroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
        form.add(javaScroll);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        JPanel javaButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        javaButtons.setBackground(Colors.BACKGROUND);
        javaButtons.setAlignmentX(LEFT_ALIGNMENT);
        JButton refresh = new JButton("Refrescar");
        styleGhost(refresh);
        refresh.addActionListener(e -> {
            if (onRefreshJava != null) {
                onRefreshJava.run();
            }
        });
        JButton useSelected = new JButton("Usar seleccionada");
        styleGhost(useSelected);
        useSelected.addActionListener(e -> useSelectedJava());
        javaButtons.add(refresh);
        javaButtons.add(useSelected);
        form.add(javaButtons);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(sectionLabel("Ruta de Java personalizada"));
        javaPathField = new JTextField();
        styleField(javaPathField);
        form.add(labeledField("Ruta Java:", javaPathField));
        JLabel javaHint = new JLabel("Vacio = deteccion automatica por version.");
        javaHint.setForeground(Colors.TEXT_SECONDARY);
        javaHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        javaHint.setAlignmentX(LEFT_ALIGNMENT);
        form.add(javaHint);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(sectionLabel("Descargar Java (Adoptium)"));
        JPanel dlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dlRow.setBackground(Colors.BACKGROUND);
        dlRow.setAlignmentX(LEFT_ALIGNMENT);
        for (int v : new int[]{8, 17, 21, 25}) {
            JButton dl = new JButton("Java " + v);
            styleGhost(dl);
            final int ver = v;
            dl.addActionListener(e -> {
                if (onDownloadJava != null) {
                    onDownloadJava.accept(ver);
                }
            });
            dlRow.add(dl);
        }
        form.add(dlRow);
        return wrapScroll(form);
    }

    private static String mesaErrorText(String detail) {
        return "No se pudo instalar Mesa3D.\n" + detail + "\n\n"
            + "Podes intentarlo de nuevo o descargar manualmente desde:\n"
            + "https://github.com/pal1000/mesa-dist-win/releases";
    }

    private void useSelectedJava() {
        int idx = javaList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un Java de la lista.", "Sin seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String entry = javaListModel.get(idx);
        for (Map.Entry<Integer, String> en : javaPaths.entrySet()) {
            if (entry.contains("Java " + en.getKey())) {
                javaPathField.setText(en.getValue());
                return;
            }
        }
    }

    private JPanel memoryTab() {
        JPanel form = vbox();
        form.add(sectionLabel("RAM global de versiones"));
        globalRamSlider = new JSlider(512, (int) com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb(), 4096);
        globalRamSlider.setBackground(Colors.BACKGROUND);
        globalRamSlider.setForeground(Colors.NEON_GREEN);
        globalRamSlider.setMajorTickSpacing(1024);
        globalRamSlider.setPaintLabels(false);
        globalRamSlider.setAlignmentX(LEFT_ALIGNMENT);
        globalRamSlider.setMaximumSize(new Dimension(480, 50));
        globalRamLabel = new JLabel("4096 MB");
        globalRamLabel.setForeground(Colors.TEXT_PRIMARY);
        globalRamLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        globalRamLabel.setAlignmentX(LEFT_ALIGNMENT);
        globalRamSlider.addChangeListener(e -> {
            int v = globalRamSlider.getValue();
            globalRamLabel.setText(v + " MB" + (v > maxUsable() ? "  (excede el 75% recomendado)" : ""));
        });
        form.add(globalRamLabel);
        form.add(globalRamSlider);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        ramCombo = new JComboBox<>(ramOptions());
        styleCombo(ramCombo);
        form.add(labeledCombo("RAM:", ramCombo));
        hwLabel = new JLabel();
        hwLabel.setForeground(Colors.TEXT_SECONDARY);
        hwLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hwLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(hwLabel);
        refreshHardwareInfo();
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        JButton applyAll = new JButton("Aplicar a todas las instancias");
        applyAll.setBackground(Colors.NEON_GREEN);
        applyAll.setForeground(java.awt.Color.BLACK);
        applyAll.setFocusPainted(false);
        applyAll.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        applyAll.setAlignmentX(LEFT_ALIGNMENT);
        applyAll.addActionListener(e -> {
            if (onApplyRamAll != null) {
                onApplyRamAll.run();
            }
        });
        form.add(applyAll);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        JButton save = new JButton("Guardar");
        save.setBackground(Colors.NEON_GREEN);
        save.setForeground(java.awt.Color.BLACK);
        save.setFocusPainted(false);
        save.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        save.setAlignmentX(LEFT_ALIGNMENT);
        save.addActionListener(e -> onSaveClicked());
        form.add(save);
        return wrapScroll(form);
    }
    private JPanel screenTab() {
        JPanel form = vbox();
        form.add(sectionLabel("Pantalla"));
        widthField = new JTextField("854");
        styleField(widthField);
        form.add(labeledField("Ancho:", widthField));
        heightField = new JTextField("480");
        styleField(heightField);
        form.add(labeledField("Alto:", heightField));
        fullscreenBox = new JCheckBox("Pantalla completa");
        fullscreenBox.setBackground(Colors.BACKGROUND);
        fullscreenBox.setForeground(Colors.TEXT_PRIMARY);
        fullscreenBox.setAlignmentX(LEFT_ALIGNMENT);
        form.add(fullscreenBox);
        form.add(Box.createRigidArea(new Dimension(0, 15)));
        JButton save = new JButton("Guardar");
        save.setBackground(Colors.NEON_GREEN);
        save.setForeground(java.awt.Color.BLACK);
        save.setFocusPainted(false);
        save.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        save.setAlignmentX(LEFT_ALIGNMENT);
        save.addActionListener(e -> onSaveClicked());
        form.add(save);
        return wrapScroll(form);
    }

    private JPanel advancedTab() {
        JPanel form = vbox();
        form.add(sectionLabel("GPU detectada"));
        JPanel gpuRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        gpuRow.setBackground(Colors.BACKGROUND);
        gpuRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel gpuNameLabel = new JLabel("GPU: Detectando...");
        gpuNameLabel.setForeground(Colors.TEXT_PRIMARY);
        gpuNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel gpuGlLabel = new JLabel("OpenGL max: -");
        gpuGlLabel.setForeground(Colors.TEXT_SECONDARY);
        gpuGlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JButton redetectBtn = new JButton("Re-detectar GPU");
        styleGhost(redetectBtn);
        redetectBtn.addActionListener(e -> {
            redetectBtn.setEnabled(false);
            new SwingWorker<GpuDetector.GpuInfo, Void>() {
                @Override
                protected GpuDetector.GpuInfo doInBackground() throws Exception {
                    return GpuDetector.forceRedetect();
                }
                @Override
                protected void done() {
                    redetectBtn.setEnabled(true);
                    try {
                        GpuDetector.GpuInfo info = get();
                        gpuNameLabel.setText("GPU: " + info.name);
                        gpuGlLabel.setText("OpenGL max: " + info.openglMax);
                    } catch (Exception ex) {
                        gpuNameLabel.setText("GPU: Error al detectar");
                    }
                }
            }.execute();
        });
        gpuRow.add(gpuNameLabel);
        gpuRow.add(Box.createRigidArea(new Dimension(16, 0)));
        gpuRow.add(gpuGlLabel);
        gpuRow.add(Box.createRigidArea(new Dimension(16, 0)));
        gpuRow.add(redetectBtn);
        form.add(gpuRow);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        // Trigger initial detection
        new SwingWorker<GpuDetector.GpuInfo, Void>() {
            @Override
            protected GpuDetector.GpuInfo doInBackground() throws Exception {
                return GpuDetector.detect();
            }
            @Override
            protected void done() {
                try {
                    GpuDetector.GpuInfo info = get();
                    gpuNameLabel.setText("GPU: " + info.name);
                    gpuGlLabel.setText("OpenGL max: " + info.openglMax);
                } catch (Exception ignored) {
                    gpuNameLabel.setText("GPU: No detectada");
                }
            }
        }.execute();
        form.add(sectionLabel("Comportamiento"));
        hideOnLaunchBox = new JCheckBox("Ocultar launcher al iniciar Minecraft", true);
        styleCheck(hideOnLaunchBox);
        form.add(hideOnLaunchBox);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(sectionLabel("Video"));
        gpuBox = new JCheckBox("Forzar GPU dedicada (laptops con 2 GPUs)");
        styleCheck(gpuBox);
        // Check for dedicated GPU but ALWAYS enable the toggle
        boolean hasDedicated = false;
        try {
            hasDedicated = com.ragsmc.launcher.hardware.GpuDedicatedHelper.hasDedicatedGpu();
        } catch (Exception ignored) {
        }
        final boolean finalHasDedicated = hasDedicated;
        if (!finalHasDedicated) {
            gpuBox.setToolTipText("ADVERTENCIA: Solo se detecto 1 GPU. Forzar GPU dedicada no tendra efecto pero podes activarlo.");
        }
        gpuBox.addActionListener(e -> {
            if (gpuBox.isSelected()) {
                if (!finalHasDedicated) {
                    int r = JOptionPane.showConfirmDialog(this,
                        "Solo se detecto 1 GPU. Forzar GPU dedicada no tendra efecto visual. \u00bfDeseas continuar de todos modos?",
                        "Advertencia",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    if (r != JOptionPane.YES_OPTION) {
                        gpuBox.setSelected(false);
                        return;
                    }
                }
                com.ragsmc.launcher.hardware.GpuDedicatedHelper.enableForAllKnownJava();
                if (window != null) {
                    window.getConsoleView().appendLine("Forzar GPU dedicada: ACTIVADO para todos los Java detectados");
                }
            } else {
                com.ragsmc.launcher.hardware.GpuDedicatedHelper.disableForAllKnownJava();
                if (window != null) {
                    window.getConsoleView().appendLine("Forzar GPU dedicada: DESACTIVADO");
                }
            }
        });
        form.add(gpuBox);
        mesaBox = new JCheckBox("Forzar CPU - render por software (Mesa, GPUs muy viejas)");
        styleCheck(mesaBox);
        mesaBox.setEnabled(true);
        mesaBox.setToolTipText("Renderiza por software (CPU). Solo para GPUs muy viejas.");
        mesaBox.addActionListener(e -> {
            if (mesaBox.isSelected()) {
                int r = JOptionPane.showConfirmDialog(this,
                    "Modo Mesa: Minecraft correra por software (CPU). Es MUY lento pero funciona en GPUs que no soportan OpenGL 3.2.\n\n" +
                    "El launcher descargara e instalara Mesa3D automaticamente (~70 MB).\n" +
                    "Deseas continuar?",
                    "Confirmar modo Mesa",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) {
                    mesaBox.setSelected(false);
                    return;
                }
                // Descargar + instalar Mesa en background con progreso y Cancelar
                final javax.swing.JDialog mesaDlg = new javax.swing.JDialog((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(SettingsView.this), "Instalando Mesa3D", true);
                mesaDlg.setLayout(new java.awt.BorderLayout(10, 10));
                mesaDlg.setSize(440, 170);
                mesaDlg.setLocationRelativeTo(SettingsView.this);
                final javax.swing.JLabel mesaLabel = new javax.swing.JLabel("Conectando...");
                final javax.swing.JProgressBar mesaBar = new javax.swing.JProgressBar(0, 100);
                mesaBar.setStringPainted(true);
                mesaBar.setIndeterminate(true);
                final javax.swing.JButton mesaCancel = new javax.swing.JButton("Cancelar");
                javax.swing.JPanel mesaTop = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));
                mesaTop.add(mesaLabel, java.awt.BorderLayout.NORTH);
                mesaDlg.add(mesaTop, java.awt.BorderLayout.NORTH);
                mesaDlg.add(mesaBar, java.awt.BorderLayout.CENTER);
                mesaDlg.add(mesaCancel, java.awt.BorderLayout.SOUTH);
                final java.util.concurrent.atomic.AtomicBoolean mesaCancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
                mesaCancel.addActionListener(ev -> {
                    mesaCancelled.set(true);
                    mesaCancel.setEnabled(false);
                    mesaCancel.setText("Cancelando...");
                });
                javax.swing.SwingWorker<java.io.File, String> mesaWorker = new javax.swing.SwingWorker<java.io.File, String>() {
                    @Override
                    protected java.io.File doInBackground() throws Exception {
                        final long[] lastUi = {0};
                        return com.ragsmc.launcher.hardware.MesaDownloader.download((msg, frac) -> {
                            if (mesaCancelled.get()) {
                                throw new RuntimeException("cancelado por el usuario");
                            }
                            long now = System.nanoTime();
                            if (now - lastUi[0] > 200000000L || frac >= 1.0) {
                                lastUi[0] = now;
                                publish(msg + "|" + frac);
                            }
                        });
                    }
                    @Override
                    protected void process(java.util.List<String> chunks) {
                        for (String c : chunks) {
                            int sep = c.lastIndexOf("|");
                            if (sep < 0) {
                                continue;
                            }
                            try {
                                String msg = c.substring(0, sep);
                                double frac = Double.parseDouble(c.substring(sep + 1));
                                if (frac >= 0) {
                                    mesaBar.setIndeterminate(false);
                                    mesaBar.setValue((int) (frac * 100));
                                } else {
                                    mesaBar.setIndeterminate(true);
                                }
                                mesaLabel.setText(msg.length() > 64 ? msg.substring(0, 64) + "..." : msg);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    @Override
                    protected void done() {
                        mesaDlg.dispose();
                        try {
                            java.io.File mesaDir = get();
                            java.util.Map<Integer, String> found = com.ragsmc.launcher.minecraft.JavaVersionSelector.scanAll();
                            int copied = 0;
                            int total = 0;
                            for (String path : found.values()) {
                                total++;
                                java.io.File jd = new java.io.File(path).getParentFile();
                                if (jd != null && com.ragsmc.launcher.hardware.MesaDownloader.copyDllsTo(jd, mesaDir) > 0) {
                                    copied++;
                                }
                            }
                            if (window != null) {
                                window.getConsoleView().appendLine("Mesa: DLLs copiadas a " + copied + "/" + total + " JVMs");
                            }
                            JOptionPane.showMessageDialog(SettingsView.this,
                                "Mesa instalado correctamente. Minecraft correra por software.",
                                "Mesa activado", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                            java.lang.Throwable cause = ex.getCause();
                            String all = msg + (cause != null && cause.getMessage() != null ? " " + cause.getMessage() : "");
                            if (all.contains("cancelado")) {
                                if (window != null) {
                                    window.getConsoleView().appendLine("Instalacion de Mesa cancelada.");
                                }
                            } else {
                                JOptionPane.showMessageDialog(SettingsView.this,
                                    mesaErrorText(all),
                                    "Error al instalar Mesa", JOptionPane.ERROR_MESSAGE);
                            }
                            mesaBox.setSelected(false);
                        }
                    }
                };
                mesaWorker.execute();
                mesaDlg.setVisible(true);
            } else {
                // Desinstalar Mesa: borra DLLs de cada Java + carpeta mesa3d
                try {
                    com.ragsmc.launcher.hardware.MesaDownloader.uninstall();
                    if (window != null) {
                        window.getConsoleView().appendLine("Mesa: desinstalado. Minecraft vuelve a OpenGL normal.");
                    }
                } catch (Exception ex) {
                    if (window != null) {
                        window.getConsoleView().appendLine("Mesa: error al desinstalar: " + ex.getMessage());
                    }
                    com.ragsmc.launcher.hardware.MesaDownloader.removeFromAllJava();
                }
            }
        });
        form.add(mesaBox);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(sectionLabel("Red"));
        dnsField = new JTextField();
        styleField(dnsField);
        form.add(labeledField("DNS Override:", dnsField));
        JLabel dnsHint = new JLabel("IP de DNS personalizado. Vacio = DNS del sistema.");
        dnsHint.setForeground(Colors.TEXT_SECONDARY);
        dnsHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dnsHint.setAlignmentX(LEFT_ALIGNMENT);
        form.add(dnsHint);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(sectionLabel("Argumentos extra"));
        jvmArgsField = new JTextField();
        styleField(jvmArgsField);
        form.add(labeledField("JVM:", jvmArgsField));
        gameArgsField = new JTextField();
        styleField(gameArgsField);
        form.add(labeledField("Juego:", gameArgsField));
        form.add(Box.createRigidArea(new Dimension(0, 15)));
        JButton save = new JButton("Guardar");
        save.setBackground(Colors.NEON_GREEN);
        save.setForeground(java.awt.Color.BLACK);
        save.setFocusPainted(false);
        save.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        save.setAlignmentX(LEFT_ALIGNMENT);
        save.addActionListener(e -> onSaveClicked());
        form.add(save);
        return wrapScroll(form);
    }

    private JPanel updatesTab() {
        JPanel form = vbox();
        form.add(sectionLabel("Actualizaciones"));
        JLabel ver = new JLabel("Version actual: 1.0.0 (RagsMC Lite)");
        ver.setForeground(Colors.TEXT_PRIMARY);
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ver.setAlignmentX(LEFT_ALIGNMENT);
        form.add(ver);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel state = new JLabel("Estado: sin comprobar");
        state.setForeground(Colors.TEXT_SECONDARY);
        state.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        state.setAlignmentX(LEFT_ALIGNMENT);
        form.add(state);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        JButton check = new JButton("Verificar actualizaciones");
        check.setBackground(Colors.FIELD_BACKGROUND);
        check.setForeground(Colors.TEXT_PRIMARY);
        check.setFocusPainted(false);
        check.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        check.setAlignmentX(LEFT_ALIGNMENT);
        check.addActionListener(e -> {
            check.setEnabled(false);
            state.setText("Estado: comprobando...");
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return new com.ragsmc.launcher.update.AutoUpdater().latestVersion("1.0.0");
                }
                @Override
                protected void done() {
                    check.setEnabled(true);
                    try {
                        String latest = get();
                        if (latest == null || latest.equals("1.0.0")) {
                            state.setText("Estado: actualizado (1.0.0)");
                        } else {
                            state.setText("Estado: nueva version disponible: " + latest);
                        }
                    } catch (Exception ex) {
                        state.setText("Estado: sin servidor de actualizaciones configurado.");
                    }
                }
            }.execute();
        });
        form.add(check);
        return wrapScroll(form);
    }

    private JPanel vbox() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Colors.BACKGROUND);
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return form;
    }

    private JPanel wrapScroll(JPanel form) {
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBackground(Colors.BACKGROUND);
        scroll.getViewport().setBackground(Colors.BACKGROUND);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Colors.BACKGROUND);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    private static String[] ramOptions() {
        long total = SystemInfo.totalRamMb();
        long cap = SystemInfo.maxUsableRamMb();
        java.util.List<String> opts = new java.util.ArrayList<>();
        int[] steps = {512, 1024, 2048, 3072, 4096, 6144, 8192};
        for (int s : steps) {
            if (s <= cap) {
                opts.add(s + " MB");
            }
        }
        for (long x = 9216; x <= cap; x += 1024) {
            opts.add(x + " MB");
        }
        if (opts.isEmpty()) {
            opts.add("512 MB");
        }
        return opts.toArray(new String[0]);
    }

    private static long maxUsable() {
        long total = SystemInfo.totalRamMb();
        return com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb();
    }

    public void refreshHardwareInfo() {
        long total = SystemInfo.totalRamMb();
        long free = SystemInfo.freeRamMb();
        hwLabel.setText("Sistema: " + total + " MB totales / " + free + " MB libres / maximo usable " + maxUsable() + " MB  |  " + SystemInfo.osName());
    }

    public void setDetectedJavas(final Map<Integer, String> found) {
        SwingUtilities.invokeLater(() -> {
            javaPaths.clear();
            javaPaths.putAll(found);
            javaListModel.clear();
            for (Map.Entry<Integer, String> e : found.entrySet()) {
                String tag = (e.getKey() == 8 || e.getKey() == 17 || e.getKey() == 21) ? "  [Recomendada]" : "";
                javaListModel.addElement("Java " + e.getKey() + tag + "  -  " + e.getValue());
            }
            if (javaListModel.isEmpty()) {
                javaListModel.addElement("Sin Java detectado. Instala Temurin desde https://adoptium.net/");
            }
        });
    }

    public void setOnRefreshJava(Runnable r) {
        this.onRefreshJava = r;
    }

    public void setOnDownloadJava(Consumer<Integer> r) {
        this.onDownloadJava = r;
    }

    public void setOnApplyRamAll(Runnable r) {
        this.onApplyRamAll = r;
    }

    private void onSaveClicked() {
        int ram = parseRam();
        long maxUsable = SystemInfo.maxUsableRamMb();
        if (ram > maxUsable) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Elegiste " + ram + " MB pero el maximo usable es " + maxUsable + " MB. Guardar igual?",
                    "RAM alta", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if (onSave != null) {
            SettingsData d = collectData(ram);
            onSave.accept(d);
            JOptionPane.showMessageDialog(this, "Ajustes guardados.", "OK", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private SettingsData collectData(int ram) {
        SettingsData d = new SettingsData();
        d.javaPath = javaPathField.getText().trim();
        d.ramMb = ram;
        try {
            d.width = Integer.parseInt(widthField.getText().trim());
            d.height = Integer.parseInt(heightField.getText().trim());
        } catch (Exception ex) {
            d.width = 854;
            d.height = 480;
        }
        d.fullscreen = fullscreenBox.isSelected();
        d.hideOnLaunch = hideOnLaunchBox.isSelected();
        d.gpuDedicated = gpuBox.isSelected();
        d.mesaCpu = mesaBox.isSelected();
        d.dnsOverride = dnsField.getText().trim();
        d.jvmArgs = jvmArgsField.getText().trim();
        d.gameArgs = gameArgsField.getText().trim();
        return d;
    }

    private int parseRam() {
        try {
            Object sel = ramCombo.getSelectedItem();
            if (sel == null) {
                return globalRamSlider.getValue();
            }
            String digits = sel.toString().replaceAll("[^0-9]", "");
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return globalRamSlider.getValue();
        }
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(Colors.NEON_GREEN);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel labeledField(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Colors.BACKGROUND);
        row.setMaximumSize(new Dimension(560, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setForeground(Colors.TEXT_PRIMARY);
        l.setPreferredSize(new Dimension(90, 28));
        row.add(l, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel labeledCombo(String label, JComboBox<String> combo) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Colors.BACKGROUND);
        row.setMaximumSize(new Dimension(560, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setForeground(Colors.TEXT_PRIMARY);
        l.setPreferredSize(new Dimension(90, 28));
        row.add(l, BorderLayout.WEST);
        row.add(combo, BorderLayout.CENTER);
        return row;
    }

    private void styleField(JTextField f) {
        f.setBackground(Colors.FIELD_BACKGROUND);
        f.setForeground(Colors.TEXT_PRIMARY);
        f.setCaretColor(Colors.NEON_GREEN);
        f.setMaximumSize(new Dimension(560, 28));
    }

    private void styleCombo(JComboBox<String> c) {
        c.setBackground(Colors.FIELD_BACKGROUND);
        c.setForeground(Colors.TEXT_PRIMARY);
    }

    private void styleGhost(JButton b) {
        b.setBackground(Colors.FIELD_BACKGROUND);
        b.setForeground(Colors.TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void styleCheck(JCheckBox c) {
        c.setBackground(Colors.BACKGROUND);
        c.setForeground(Colors.TEXT_PRIMARY);
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setFocusPainted(false);
    }

    public void setOnSave(Consumer<SettingsData> onSave) {
        this.onSave = onSave;
    }

    public void loadValues(final String javaPath, final int ramMb, final int width, final int height, final boolean fullscreen) {
        SwingUtilities.invokeLater(() -> {
            javaPathField.setText(javaPath != null ? javaPath : "");
            boolean found = false;
            for (int i = 0; i < ramCombo.getItemCount(); i++) {
                try {
                    int v = Integer.parseInt(ramCombo.getItemAt(i).replaceAll("[^0-9]", ""));
                    if (v == ramMb) {
                        ramCombo.setSelectedIndex(i);
                        found = true;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            if (!found) {
                ramCombo.setSelectedIndex(ramCombo.getItemCount() - 1);
            }
            globalRamSlider.setValue((int) Math.min(Math.max(ramMb, 512), com.ragsmc.launcher.util.SystemInfo.maxUsableRamMb()));
            widthField.setText(String.valueOf(width));
            heightField.setText(String.valueOf(height));
            fullscreenBox.setSelected(fullscreen);
            refreshHardwareInfo();
        });
    }

    public void loadAdvanced(final boolean hideOnLaunch, final boolean gpu, final boolean mesa, final String dns, final String jvmArgs, final String gameArgs) {
        SwingUtilities.invokeLater(() -> {
            hideOnLaunchBox.setSelected(hideOnLaunch);
            gpuBox.setSelected(gpu);
            mesaBox.setSelected(mesa);
            dnsField.setText(dns != null ? dns : "");
            jvmArgsField.setText(jvmArgs != null ? jvmArgs : "");
            gameArgsField.setText(gameArgs != null ? gameArgs : "");
        });
    }

    public void cleanup() {
        // Clear any pending workers, listeners
        javaListModel = null;
        javaList = null;
        javaPaths.clear();
    }

    public void setWindow(com.ragsmc.launcher.ui.MainWindow window) {
        this.window = window;
    }
}
