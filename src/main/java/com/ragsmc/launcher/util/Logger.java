package com.ragsmc.launcher.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Logger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static File file;

    private Logger() {
    }

    public static synchronized void init(File logFile) {
        file = logFile;
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            String line = "=== RagsMC Lite inicio " + LocalDateTime.now().format(FMT) + " ===" + System.lineSeparator();
            Files.write(file.toPath(), line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    public static synchronized void info(String msg) {
        write("INFO", msg, null);
    }

    public static synchronized void error(String msg, Throwable t) {
        write("ERROR", msg, t);
    }

    private static void write(String level, String msg, Throwable t) {
        if (file == null) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(LocalDateTime.now().format(FMT)).append(" [").append(level).append("] ").append(msg)
                    .append(System.lineSeparator());
            if (t != null) {
                sb.append(t.toString()).append(System.lineSeparator());
                for (StackTraceElement el : t.getStackTrace()) {
                    sb.append("    at ").append(el.toString()).append(System.lineSeparator());
                    if (sb.length() > 8000) {
                        break;
                    }
                }
            }
            Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }
}