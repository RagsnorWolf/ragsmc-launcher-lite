package com.ragsmc.launcher.minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public final class JavaVersionSelector {

    private JavaVersionSelector() {
    }

    // Cache for findJava results (1 hour TTL)
    private static final java.util.Map<Integer, java.util.Optional<String>> javaCache = new java.util.HashMap<>();
    private static final java.util.Map<Integer, Long> javaCacheTime = new java.util.HashMap<>();
    private static final long CACHE_TTL_MS = 3600_000; // 1 hour

    public static int requiredMajor(String mcVersion) {
        if (mcVersion == null) {
            return 17;
        }
        String v = mcVersion.trim();
        try {
            // 23w31a -> Java 17 (snapshot de 1.20.2)
            if (v.equals("23w31a")) {
                return 17;
            }
            // 1.21+ / 24w+ / 25w+ -> Java 21
            if (compare(v, "1.21") >= 0 || v.startsWith("1.22") || v.startsWith("24w") || v.startsWith("25w")) {
                return 21;
            }
            // 1.17 - 1.20.x -> Java 17
            if (compare(v, "1.17") >= 0) {
                return 17;
            }
        } catch (Exception ignored) {
        }
        // 1.16.5 y anteriores -> Java 8
        return 8;
    }

    private static int compare(String a, String b) {
        String[] pa = a.split("[.\\-]");
        String[] pb = b.split("[.\\-]");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int x = i < pa.length ? num(pa[i]) : 0;
            int y = i < pb.length ? num(pb[i]) : 0;
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public static Optional<String> findJava(int major) {
        // 1. Runtimes de Mojang (prioridad máxima)
        Optional<String> mojang = findMojangJava(major);
        if (mojang.isPresent()) {
            System.out.println("[JavaSelector] Java requerido: " + major + ", Java a usar: " + mojang.get() + " (Mojang runtime)");
            javaCache.put(major, mojang);
            javaCacheTime.put(major, System.currentTimeMillis());
            return mojang;
        }
        // 2. Runtimes propios de RagsMC (%APPDATA%/RagsMC/runtimes/java-<version>/)
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isEmpty()) {
            File ragsmcRuntime = new File(appdata, "RagsMC" + File.separator + "runtimes" + File.separator + "java-" + major);
            File bin = new File(ragsmcRuntime, "bin" + File.separator + "java" + (isWindows() ? ".exe" : ""));
            if (bin.isFile()) {
                try {
                    if (probeMajor(bin.getAbsolutePath()) == major) {
                        System.out.println("[JavaSelector] Java requerido: " + major + ", Java a usar: " + bin.getAbsolutePath() + " (RagsMC runtime)");
                        javaCache.put(major, Optional.of(bin.getAbsolutePath()));
                        javaCacheTime.put(major, System.currentTimeMillis());
                        return Optional.of(bin.getAbsolutePath());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        // 3. Adoptium / Eclipse Adoptium
        // 4. Oracle / Microsoft / Semeru
        // 5. JAVA_HOME / PATH / java.home
        List<String> candidates = new ArrayList<>();
        String[] roots = {
            "C:/Program Files/Eclipse Adoptium",
            "C:/Program Files/Java",
            "C:/Program Files (x86)/Java",
            "C:/Program Files/Microsoft",
            "C:/Program Files/Semeru",
            "/usr/lib/jvm",
            "/usr/local/lib/java"
        };
        for (String root : roots) {
            File dir = new File(root);
            File[] subs = dir.listFiles(File::isDirectory);
            if (subs == null) {
                continue;
            }
            for (File sub : subs) {
                File bin = new File(sub, "bin" + File.separator + "java" + (isWindows() ? ".exe" : ""));
                if (bin.isFile()) {
                    candidates.add(bin.getAbsolutePath());
                }
            }
        }
        String home = System.getProperty("java.home", "");
        if (!home.isEmpty()) {
            candidates.add(home + File.separator + "bin" + File.separator + "java" + (isWindows() ? ".exe" : ""));
        }
        for (String bin : candidates) {
            try {
                int found = probeMajor(bin);
                if (found == major || (major == 16 && found >= 16 && found <= 20)) {
                    System.out.println("[JavaSelector] Java requerido: " + major + ", Java a usar: " + bin + " (sistema)");
                    javaCache.put(major, Optional.of(bin));
                    javaCacheTime.put(major, System.currentTimeMillis());
                    return Optional.of(bin);
                }
            } catch (Exception ignored) {
            }
        }
        if (major == 8) {
            Optional<String> j8 = JavaManager.findJava8().map(f -> new File(f, "bin" + File.separator + "java" + (isWindows() ? ".exe" : "")).getAbsolutePath());
            if (j8.isPresent()) {
                System.out.println("[JavaSelector] Java requerido: 8, Java a usar: " + j8.get() + " (JavaManager)");
            }
            return j8;
        }
        System.out.println("[JavaSelector] Java requerido: " + major + ", NO ENCONTRADO");
        javaCache.put(major, Optional.empty());
        javaCacheTime.put(major, System.currentTimeMillis());
        return Optional.empty();
    }

    public static int majorOf(String javaBin) {
        try {
            return probeMajor(javaBin);
        } catch (Exception e) {
            return -1;
        }
    }

    public static Optional<String> findMojangJava(int major) {
        // Use exact paths for better reliability
        return findMojangJavaExact(major);
    }

    public static Optional<String> findMojangJavaExact(int major) {
        try {
            String appdata = System.getenv("APPDATA");
            File base = appdata != null && !appdata.isEmpty()
                    ? new File(appdata, ".minecraft" + File.separator + "runtime")
                    : new File(System.getProperty("user.home"), ".minecraft" + File.separator + "runtime");
            if (!base.isDirectory()) {
                return Optional.empty();
            }
            // Rutas exactas de runtimes de Mojang
            String[][] exactPaths = {
                {"jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe"},
                {"java-runtime-alpha", "windows-x64", "java-runtime-alpha", "bin", "javaw.exe"},
                {"java-runtime-beta", "windows-x64", "java-runtime-beta", "bin", "javaw.exe"},
                {"java-runtime-gamma", "windows-x64", "java-runtime-gamma", "bin", "javaw.exe"},
                {"java-runtime-delta", "windows-x64", "java-runtime-delta", "bin", "javaw.exe"}
            };
            for (String[] parts : exactPaths) {
                File bin = base;
                for (String p : parts) {
                    bin = new File(bin, p);
                }
                if (bin.isFile()) {
                    try {
                        int detectedMajor = readMojangRuntimeMajor(bin);
                        if (detectedMajor > 0 && detectedMajor == major) {
                            System.out.println("[JavaSelector] Encontrado en ruta exacta: " + bin.getAbsolutePath() + " (Java " + detectedMajor + ")");
                            return Optional.of(bin.getAbsolutePath());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // Fallback: escaneo recursivo
        System.out.println("[JavaSelector] Rutas exactas no funcionaron, probando escaneo recursivo...");
        return findMojangJavaRecursive(major);
    }

    public static Optional<String> findMojangJavaRecursive(int major) {
        try {
            String appdata = System.getenv("APPDATA");
            File base = appdata != null && !appdata.isEmpty()
                    ? new File(appdata, ".minecraft" + File.separator + "runtime")
                    : new File(System.getProperty("user.home"), ".minecraft" + File.separator + "runtime");
            if (!base.isDirectory()) {
                return Optional.empty();
            }
            List<File> bins = new ArrayList<>();
            collectJavaBins(base, bins, 5);
            for (File bin : bins) {
                try {
                    if (probeMajor(bin.getAbsolutePath()) == major) {
                        System.out.println("[JavaSelector] Encontrado en escaneo recursivo: " + bin.getAbsolutePath() + " (Java " + major + ")");
                        return Optional.of(bin.getAbsolutePath());
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

        private static int probeMajor(String javaBin) throws Exception {
        Process p = new ProcessBuilder(javaBin, "-version").redirectErrorStream(true).start();
        byte[] out = p.getInputStream().readAllBytes();
        p.waitFor();
        String s = new String(out);
        // Rechazar JVM de 32 bits
        String lower = s.toLowerCase();
        if (lower.contains("32-bit") || (lower.contains("x86") && !lower.contains("x64"))) {
            throw new Exception("JVM 32 bits rechazada");
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(\\d+)(?:\\.(\\d+))?\"").matcher(s);
        if (m.find()) {
            int first = Integer.parseInt(m.group(1));
            if (first == 1 && m.group(2) != null) {
                return Integer.parseInt(m.group(2));
            }
            return first;
        }
        throw new Exception("sin version");
    }

    private static void scanMojangRuntimes(java.util.Map<Integer, String> found) {
        try {
            String appdata = System.getenv("APPDATA");
            File runtimeBase = appdata != null && !appdata.isEmpty()
                    ? new File(appdata, ".minecraft" + File.separator + "runtime")
                    : new File(System.getProperty("user.home"), ".minecraft" + File.separator + "runtime");
            if (!runtimeBase.isDirectory()) {
                return;
            }
            // Rutas exactas de runtimes de Mojang (javaw.exe)
            String[][] exactPaths = {
                {"jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe"},
                {"java-runtime-alpha", "windows-x64", "java-runtime-alpha", "bin", "javaw.exe"},
                {"java-runtime-beta", "windows-x64", "java-runtime-beta", "bin", "javaw.exe"},
                {"java-runtime-gamma", "windows-x64", "java-runtime-gamma", "bin", "javaw.exe"},
                {"java-runtime-delta", "windows-x64", "java-runtime-delta", "bin", "javaw.exe"}
            };
            for (String[] parts : exactPaths) {
                File bin = runtimeBase;
                for (String p : parts) {
                    bin = new File(bin, p);
                }
                if (bin.isFile()) {
                    try {
                        int major = readMojangRuntimeMajor(bin);
                        if (major > 0) {
                            found.putIfAbsent(major, bin.getAbsolutePath());
                            System.out.println("[JavaScanner] Encontrado: Java " + major + " en " + bin.getAbsolutePath());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            // Fallback: escaneo recursivo general
            List<File> bins = new ArrayList<>();
            collectJavaBins(runtimeBase, bins, 4);
            for (File bin : bins) {
                try {
                    int major = probeMajor(bin.getAbsolutePath());
                    found.putIfAbsent(major, bin.getAbsolutePath());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static int readMojangRuntimeMajor(File javawExe) throws Exception {
        try {
            File runtimeDir = javawExe.getParentFile().getParentFile(); // .../jre-legacy/
            File releaseFile = new File(runtimeDir, "release");
            if (releaseFile.isFile()) {
                Properties props = new Properties();
                try (FileReader fr = new FileReader(releaseFile)) {
                    props.load(fr);
                }
                String version = props.getProperty("JAVA_VERSION", "");
                if (!version.isEmpty()) {
                    // Parsear "8", "17", "21", "17.0.8", etc.
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(version);
                    if (m.find()) {
                        return Integer.parseInt(m.group(1));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // Fallback: probe via java -version
        return probeMajor(javawExe.getAbsolutePath());
    }

    private static void collectJavaBins(File dir, List<File> out, int depth) {
        if (depth < 0) {
            return;
        }
        File direct = new File(dir, "bin" + File.separator + "java" + (isWindows() ? ".exe" : ""));
        if (direct.isFile()) {
            out.add(direct);
            return;
        }
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) {
            return;
        }
        for (File sub : subs) {
            collectJavaBins(sub, out, depth - 1);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static java.util.Map<Integer, String> scanAll() {
        java.util.Map<Integer, String> found = new java.util.TreeMap<>();
        List<String> roots = new ArrayList<>();
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty()) {
            roots.add(javaHome);
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String p : pathEnv.split(java.util.regex.Pattern.quote(File.separator.equals("/") ? ":" : ";"))) {
                File bin = new File(p.trim(), "java" + (isWindows() ? ".exe" : ""));
                if (bin.isFile()) {
                    try {
                        found.putIfAbsent(probeMajor(bin.getAbsolutePath()), bin.getAbsolutePath());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        scanMojangRuntimes(found);
        String[] baseDirs = {
            "C:/Program Files/Eclipse Adoptium",
            "C:/Program Files/Java",
            "C:/Program Files (x86)/Java",
            "C:/Program Files/Microsoft",
            "C:/Program Files/Semeru",
            "/usr/lib/jvm",
            "/usr/local/lib/java"
        };
        for (String base : baseDirs) {
            File dir = new File(base);
            File[] subs = dir.listFiles(File::isDirectory);
            if (subs == null) {
                continue;
            }
            for (File sub : subs) {
                File bin = new File(sub, "bin" + File.separator + "java" + (isWindows() ? ".exe" : ""));
                if (bin.isFile()) {
                    try {
                        found.putIfAbsent(probeMajor(bin.getAbsolutePath()), bin.getAbsolutePath());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        System.out.println("[JavaScanner] Total: " + found.size() + " JDKs detectados");
        return found;
    }
}