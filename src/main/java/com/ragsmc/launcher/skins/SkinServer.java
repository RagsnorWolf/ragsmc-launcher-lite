package com.ragsmc.launcher.skins;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.Executors;

public class SkinServer {

    private HttpServer server;
    private int port;
    private final File skinsDir;

    public SkinServer(File skinsDir) {
        this.skinsDir = skinsDir;
    }

    public void start() throws IOException {
        port = 40000 + new Random().nextInt(10000);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/skins/", this::handleSkin);
        server.createContext("/profile/", this::handleProfile);
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleSkin(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring("/skins/".length()).replace(".png", "");
        if (uuid.contains("/") || uuid.contains("\\") || uuid.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }
        File skinFile = new File(skinsDir, uuid + ".png");
        if (skinFile.exists() && skinFile.isFile()) {
            byte[] data = Files.readAllBytes(skinFile.toPath());
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, data.length);
            OutputStream out = exchange.getResponseBody();
            out.write(data);
            out.close();
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
        exchange.close();
    }

    private void handleProfile(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring("/profile/".length());
        if (uuid.contains("/") || uuid.contains("\\") || uuid.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }
        String json = "{\"id\":\"" + uuid + "\",\"name\":\"offline\"}";
        byte[] data = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        OutputStream out = exchange.getResponseBody();
        out.write(data);
        out.close();
        exchange.close();
    }
}