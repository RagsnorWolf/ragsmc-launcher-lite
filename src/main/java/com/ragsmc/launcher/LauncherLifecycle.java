package com.ragsmc.launcher;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;

public final class LauncherLifecycle {

    private LauncherLifecycle() {
    }

    public static void launchAndManage(JFrame window, Supplier<Process> starter, Consumer<String> log, Runnable onDone) {
        launchAndManage(window, starter, log, onDone, true);
    }

    public static void launchAndManage(JFrame window, Supplier<Process> starter, Consumer<String> log, Runnable onDone, boolean hide) {
        final long t0 = System.nanoTime();
        if (hide) {
            window.setVisible(false);
        }
        com.ragsmc.launcher.util.Logger.info("T ventana oculta en " + ((System.nanoTime() - t0) / 1000000) + "ms");
        new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                Process process;
                try {
                    process = starter.get();
                } catch (Exception e) {
                    publish("ERROR: " + e.getMessage());
                    return -1;
                }
                com.ragsmc.launcher.util.Logger.info("T proceso MC iniciado en " + ((System.nanoTime() - t0) / 1000000) + "ms desde click");
                publish("Minecraft en ejecucion. El launcher volvera al cerrar el juego.");
                final Process p = process;
                final boolean[] first = {true};
                final long tPipe = System.nanoTime();
                Thread pipe = new Thread(() -> {
                    try {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = p.getInputStream().read(buf)) != -1) {
                            final String chunk = new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                            if (first[0]) {
                                first[0] = false;
                                com.ragsmc.launcher.util.Logger.info("T primera salida MC en " + ((System.nanoTime() - tPipe) / 1000000) + "ms desde proceso iniciado");
                            }
                            publish(chunk);
                        }
                    } catch (Exception ignored) {
                    }
                }, "mc-pipe");
                pipe.setDaemon(true);
                pipe.start();
                try {
                    return p.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String c : chunks) {
                    try {
                        log.accept(c);
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            protected void done() {
                int code = -1;
                try {
                    code = get();
                } catch (Exception ignored) {
                }
                final int exitCode = code;
                SwingUtilities.invokeLater(() -> {
                    if (exitCode != 0 && exitCode != -1) {
                        try {
                            log.accept("Minecraft termino con codigo " + exitCode);
                        } catch (Exception ignored) {
                        }
                    }
                    window.setVisible(true);
                    try {
                        window.toFront();
                        window.requestFocus();
                    } catch (Exception ignored) {
                    }
                    if (onDone != null) {
                        try {
                            onDone.run();
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
        }.execute();
    }

    public static void showError(JFrame window, String title, String message) {
        try {
            window.setVisible(true);
        } catch (Exception ignored) {
        }
        JOptionPane.showMessageDialog(window, message != null ? message : "Error desconocido", title, JOptionPane.ERROR_MESSAGE);
    }
}