package com.ragsmc.launcher.util;

import java.io.File;

public final class AppDirs {

    private AppDirs() {
    }

    public static File dataDir() {
        String appdata = System.getenv("APPDATA");
        File base;
        if (appdata != null && !appdata.isEmpty()) {
            base = new File(appdata, "RagsMC");
        } else {
            base = new File(System.getProperty("user.home"), ".ragsmc");
        }
        base.mkdirs();
        return base;
    }

    public static File gameDir() {
        File dir = dataDir();
        dir.mkdirs();
        return dir;
    }

    public static File legacyMinecraftDir() {
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isEmpty()) {
            return new File(appdata, ".minecraft");
        }
        return new File(System.getProperty("user.home"), ".minecraft");
    }

    public static File instancesDir() {
        File dir = new File(gameDir(), "ragsmc_instances");
        dir.mkdirs();
        return dir;
    }

    public static File instancesFile() {
        return new File(dataDir(), "instances.json");
    }

    public static File versionsDir() {
        return new File(gameDir(), "versions");
    }

    public static File librariesDir() {
        return new File(gameDir(), "libraries");
    }

    public static File assetsDir() {
        return new File(gameDir(), "assets");
    }

    public static File skinsDir() {
        File dir = new File(gameDir(), "skins");
        dir.mkdirs();
        return dir;
    }

    public static File cacheDir() {
        File dir = new File(gameDir(), "ragsmc_cache");
        dir.mkdirs();
        return dir;
    }
}