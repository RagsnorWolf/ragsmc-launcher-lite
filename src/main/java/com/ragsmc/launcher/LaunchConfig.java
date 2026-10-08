package com.ragsmc.launcher;

import java.util.ArrayList;
import java.util.List;

public class LaunchConfig {

    private String username;
    private String uuid;
    private String javaPath;
    private int memory = 4096;
    private Boolean dedicatedGpu = Boolean.FALSE;
    private String gameDir = "";
    private String version = "1.21.1";
    private String loader = "vanilla";
    private String loaderVersion;
    private List<String> libraries = new ArrayList<>();
    private String assetIndex;
    private String sharedDir = "";
    private String clientId = "0";
    private String accessToken = "null";
    private String userType = "mojang";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public Boolean getDedicatedGpu() {
        return dedicatedGpu;
    }

    public void setDedicatedGpu(Boolean dedicatedGpu) {
        this.dedicatedGpu = dedicatedGpu;
    }

    public String getGameDir() {
        return gameDir;
    }

    public void setGameDir(String gameDir) {
        this.gameDir = gameDir;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLoader() {
        return loader;
    }

    public void setLoader(String loader) {
        this.loader = loader;
    }

    public String getLoaderVersion() {
        return loaderVersion;
    }

    public void setLoaderVersion(String loaderVersion) {
        this.loaderVersion = loaderVersion;
    }

    public List<String> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<String> libraries) {
        this.libraries = libraries;
    }

    public String getAssetIndex() {
        return assetIndex;
    }

    public String getSharedDir() {
        return sharedDir;
    }

    public void setSharedDir(String sharedDir) {
        this.sharedDir = sharedDir;
    }

    public void setAssetIndex(String assetIndex) {
        this.assetIndex = assetIndex;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getUserType() {
        return userType;
    }

    private String server;
    private Integer serverPort;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    private String jvmArgs = "";
    private String gameArgs = "";
    private boolean mesaCpu = false;

    public String getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public String getGameArgs() {
        return gameArgs;
    }

    public void setGameArgs(String gameArgs) {
        this.gameArgs = gameArgs;
    }

    public boolean isMesaCpu() {
        return mesaCpu;
    }

    public void setMesaCpu(boolean mesaCpu) {
        this.mesaCpu = mesaCpu;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}