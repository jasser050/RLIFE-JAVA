package com.studyflow;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Tiny embedded HTTP server that serves app resources (HTML, JS, GLB, images)
 * so WebView loads pages from http://localhost instead of file://.
 * This lets ES modules (Spline viewer, Three.js) load from CDN without
 * the security restrictions that file:// context imposes.
 */
public class LocalServer {

    private static HttpServer server;
    private static int port = -1;

    public static void start() {
        if (server != null) return;
        try {
            // Bind to port 0 → OS picks a free port automatically
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port   = server.getAddress().getPort();

            server.createContext("/", exchange -> {
                String requestPath = exchange.getRequestURI().getPath();

                // Map URL path → classpath resource
                // e.g. /views/spline.html → /com/studyflow/views/spline.html
                String resourcePath = "/com/studyflow" + requestPath;
                InputStream in = LocalServer.class.getResourceAsStream(resourcePath);

                if (in == null) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }

                byte[] bytes = in.readAllBytes();
                in.close();

                String mime = mimeFor(requestPath);
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            });

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            System.out.println("[LocalServer] Started on http://127.0.0.1:" + port);
        } catch (Exception e) {
            System.err.println("[LocalServer] Failed to start: " + e.getMessage());
        }
    }

    /** Returns the full URL for a given resource path, e.g. "/views/spline.html" */
    public static String url(String path) {
        if (port < 0) return "";
        return "http://127.0.0.1:" + port + path;
    }

    public static void stop() {
        if (server != null) server.stop(0);
    }

    private static String mimeFor(String path) {
        if (path.endsWith(".html"))  return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))   return "text/css";
        if (path.endsWith(".js"))    return "application/javascript";
        if (path.endsWith(".glb"))   return "model/gltf-binary";
        if (path.endsWith(".gltf"))  return "model/gltf+json";
        if (path.endsWith(".png"))   return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg"))   return "image/svg+xml";
        return "application/octet-stream";
    }
}
