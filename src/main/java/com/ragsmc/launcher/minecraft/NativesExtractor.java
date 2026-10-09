package com.ragsmc.launcher.minecraft;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NativesExtractor {

    private NativesExtractor() {
    }

    public static String currentOs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "osx";
        }
        return "linux";
    }

    public static String currentArch() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.contains("64")) {
            return "64";
        }
        return "32";
    }

    public static int extract(File nativesJar, File destDir) throws IOException {
        destDir.mkdirs();
        int count = 0;
        String os = currentOs();
        try (ZipFile zip = new ZipFile(nativesJar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || name.contains("META-INF")) {
                    continue;
                }
                if (!matchesOs(name, os)) {
                    continue;
                }
                File out = new File(destDir, new File(name).getName());
                try (InputStream in = zip.getInputStream(e);
                        OutputStream fileOut = Files.newOutputStream(out.toPath())) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        fileOut.write(buf, 0, n);
                    }
                }
                count++;
            }
        }
        return count;
    }

    private static boolean matchesOs(String name, String os) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (os.equals("windows")) {
            return lower.endsWith(".dll");
        }
        if (os.equals("osx")) {
            return lower.endsWith(".dylib") || lower.endsWith(".jnilib");
        }
        return lower.endsWith(".so");
    }
}