package com.ragsmc.launcher.download;

public class DownloadStats {

    public volatile String phase = "";
    public volatile String fileName = "";
    public volatile long bytesDone = 0;
    public volatile long bytesTotal = -1;
    public volatile int filesDone = 0;
    public volatile int filesTotal = -1;
    public volatile int libsDone = 0;
    public volatile int libsTotal = -1;
    public volatile int assetsDone = 0;
    public volatile int assetsTotal = -1;
    public volatile int nativesDone = 0;
    public volatile int nativesTotal = -1;
    public volatile long startNanos = System.nanoTime();

    public synchronized void addBytes(long n) {
        bytesDone += n;
    }

    public double speedMBs() {
        double secs = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        if (secs < 0.5) {
            return 0.0;
        }
        return (bytesDone / 1048576.0) / secs;
    }

    public String etaText() {
        if (bytesTotal <= 0) {
            return "--";
        }
        double speed = speedMBs();
        if (speed < 0.05) {
            return "--";
        }
        long remainBytes = Math.max(0, bytesTotal - bytesDone);
        double remainSecs = (remainBytes / 1048576.0) / speed;
        long m = (long) (remainSecs / 60);
        long s = (long) (remainSecs % 60);
        if (m > 0) {
            return m + " min " + s + " s";
        }
        return s + " s";
    }

    public int percent() {
        if (bytesTotal <= 0) {
            return -1;
        }
        return (int) Math.max(0, Math.min(100, bytesDone * 100 / bytesTotal));
    }

    public String elapsedText() {
        long secs = (System.nanoTime() - startNanos) / 1_000_000_000L;
        if (secs < 0) {
            secs = 0;
        }
        long m = secs / 60;
        long r = secs % 60;
        if (m > 0) {
            return m + " minutos " + r + " segundos";
        }
        return r + " segundos";
    }

    public String mbText() {
        double done = bytesDone / 1048576.0;
        if (bytesTotal > 0) {
            return String.format("%.1f / %.1f MB", done, bytesTotal / 1048576.0);
        }
        return String.format("%.1f MB", done);
    }
}