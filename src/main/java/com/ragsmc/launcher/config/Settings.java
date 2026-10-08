package com.ragsmc.launcher.config;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    public String username = "";
    public String uuid = "";
    public int ramMb = 4096;
    public String javaPath = "";
    public String gameDir = "";
    public String downloadMirror = "official";
    public boolean compatibilityMode = false;
    public boolean debugMode = false;
    public String lastVersion = "1.21.1";
    public String lastLoader = "fabric";
    public int skinServerPort = 48471;
    public boolean hideOnLaunch = true;
    public boolean gpuDedicated = false;
    public boolean mesaCpu = false;
    public boolean gpuWarnSuppressed = false;
    public String dnsOverride = "";
    public String customJvmArgs = "";
    public String customGameArgs = "";
    public List<AccountEntry> accounts = new ArrayList<>();

    public static class AccountEntry {
        public String id = "";
        public String username = "";
        public String uuid = "";
        public String type = "offline";
        public boolean selected = false;
    }
}