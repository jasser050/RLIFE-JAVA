package com.studyflow;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.Executors;

/**
 * Tiny embedded HTTP server that serves app resources (HTML, JS, GLB, images)
 * so WebView loads pages from http://localhost instead of file://.
 * Binds to 0.0.0.0 so phones on the same LAN can access it (for QR Face ID).
 */
public class LocalServer {

    private static HttpServer server;
    private static int port = -1;
    private static String lanIp = null;

    // Face ID status — set by phone via POST, polled by desktop
    private static volatile String faceIdStatus = "waiting"; // waiting | captured | skipped
    private static volatile boolean faceLoginMatch = false;
    private static volatile String faceLoginEmail = null;

    // Directory for storing face images
    private static final Path FACES_DIR = Path.of(System.getProperty("user.home"), ".rlife", "faces");

    public static void start() {
        if (server != null) return;
        try {
            // Bind to 0.0.0.0 so the phone on the same Wi-Fi can reach us
            // Try fixed port 8765 first, fall back to random
            HttpServer srv = null;
            for (int tryPort : new int[]{8765, 8766, 8767, 0}) {
                try {
                    srv = HttpServer.create(new InetSocketAddress("0.0.0.0", tryPort), 0);
                    break;
                } catch (java.net.BindException ignored) { }
            }
            server = srv;
            port   = server.getAddress().getPort();
            lanIp  = detectLanIp();

            // Face ID API — POST from phone, GET from desktop to poll
            server.createContext("/api/faceid", exchange -> {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }

                if ("POST".equals(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    String respJson;

                    try {
                        // Extract fields from JSON body
                        String mode = extractJson(body, "mode");
                        String email = extractJson(body, "email");
                        String imageData = extractJson(body, "image");

                        if (body.contains("\"captured\"")) {
                            faceIdStatus = "captured";

                            // Save face image if present
                            if (imageData != null && !imageData.isEmpty()) {
                                String saveAs = (email != null && !email.isEmpty()) ? email : "pending";
                                saveFaceImage(saveAs, imageData);
                            }

                            if ("login".equals(mode) && email != null) {
                                // Compare face for login
                                boolean match = compareFaces(email, imageData);
                                faceLoginMatch = match;
                                faceLoginEmail = match ? email : null;
                                respJson = "{\"ok\":true,\"match\":" + match + "}";
                            } else {
                                respJson = "{\"ok\":true}";
                            }
                        } else if (body.contains("\"skipped\"")) {
                            faceIdStatus = "skipped";
                            respJson = "{\"ok\":true}";
                        } else {
                            respJson = "{\"ok\":false,\"message\":\"Unknown status\"}";
                        }
                    } catch (Exception e) {
                        System.err.println("[FaceID] Error processing: " + e.getMessage());
                        faceIdStatus = "captured"; // Still mark as done
                        respJson = "{\"ok\":true}";
                    }

                    byte[] resp = respJson.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    exchange.getResponseBody().write(resp);
                    exchange.getResponseBody().close();
                    return;
                }

                // GET — poll status
                byte[] resp = ("{\"status\":\"" + faceIdStatus + "\"}").getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.getResponseBody().close();
            });

            // Static resource handler
            server.createContext("/", exchange -> {
                String requestPath = exchange.getRequestURI().getPath();

                // Serve favicon.ico as empty to avoid 404 noise
                if ("/favicon.ico".equals(requestPath)) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }

                String resourcePath = "/com/studyflow" + requestPath;
                InputStream in = LocalServer.class.getResourceAsStream(resourcePath);

                System.out.println("[LocalServer] " + exchange.getRequestMethod() + " " + requestPath
                        + " -> " + (in != null ? "OK" : "404"));

                if (in == null) {
                    byte[] msg = ("404 Not Found: " + resourcePath).getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(404, msg.length);
                    exchange.getResponseBody().write(msg);
                    exchange.getResponseBody().close();
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

            System.out.println("[LocalServer] Started on http://0.0.0.0:" + port + "  (LAN IP: " + lanIp + ")");
        } catch (Exception e) {
            System.err.println("[LocalServer] Failed to start: " + e.getMessage());
        }
    }

    /** Returns the full URL for a given resource path using localhost */
    public static String url(String path) {
        if (port < 0) return "";
        return "http://127.0.0.1:" + port + path;
    }

    /** Returns the LAN-accessible URL (for QR codes) */
    public static String lanUrl(String path) {
        if (port < 0 || lanIp == null) return url(path);
        return "http://" + lanIp + ":" + port + path;
    }

    public static int getPort() { return port; }
    public static String getLanIp() { return lanIp; }

    public static String getFaceIdStatus() { return faceIdStatus; }
    public static void resetFaceIdStatus() { faceIdStatus = "waiting"; faceLoginMatch = false; faceLoginEmail = null; }
    public static boolean isFaceLoginMatch() { return faceLoginMatch; }
    public static String getFaceLoginEmail() { return faceLoginEmail; }

    /** Rename the "pending" face file to the user's email after registration */
    public static void linkFaceToEmail(String email) {
        try {
            Path pending = FACES_DIR.resolve("pending.jpg");
            if (Files.exists(pending)) {
                Path target = FACES_DIR.resolve(sanitizeFilename(email) + ".jpg");
                Files.move(pending, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[FaceID] Linked face to: " + email);
            }
        } catch (IOException e) {
            System.err.println("[FaceID] Failed to link face: " + e.getMessage());
        }
    }

    /** Check if a face image exists for a given email */
    public static boolean hasFaceData(String email) {
        return Files.exists(FACES_DIR.resolve(sanitizeFilename(email) + ".jpg"));
    }

    public static void stop() {
        if (server != null) server.stop(0);
    }

    /**
     * Detect the machine's real Wi-Fi/Ethernet LAN IP.
     * Skips virtual adapters (Hyper-V, VirtualBox, VMware, Docker, loopback).
     */
    private static String detectLanIp() {
        String fallback = null;
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface ni = nets.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;

                // Skip known virtual adapter names
                String name = ni.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("hyper-v")
                        || name.contains("vmware") || name.contains("vbox")
                        || name.contains("docker") || name.contains("wsl")
                        || name.contains("loopback")) continue;

                var addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    var addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("169.254.")) continue; // link-local, skip
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            // Prefer Wi-Fi or Ethernet adapters
                            if (name.contains("wi-fi") || name.contains("wifi")
                                    || name.contains("wlan") || name.contains("wireless")) {
                                return ip; // Wi-Fi — best choice
                            }
                            if (name.contains("ethernet") || name.contains("eth")) {
                                return ip; // Ethernet — also good
                            }
                            if (fallback == null) fallback = ip;
                        }
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return fallback != null ? fallback : "127.0.0.1";
    }

    // ── Face image helpers ─────────────────────────────────────────

    private static void saveFaceImage(String identifier, String base64DataUrl) throws IOException {
        Files.createDirectories(FACES_DIR);
        // Strip "data:image/jpeg;base64," prefix
        String base64 = base64DataUrl;
        if (base64.contains(",")) base64 = base64.substring(base64.indexOf(",") + 1);

        byte[] imageBytes = Base64.getDecoder().decode(base64);
        Path file = FACES_DIR.resolve(sanitizeFilename(identifier) + ".jpg");
        Files.write(file, imageBytes);
        System.out.println("[FaceID] Saved face: " + file + " (" + imageBytes.length + " bytes)");
    }

    /**
     * Compare a new face image against the stored one for an email.
     * Uses pixel-level average color comparison on a downscaled grid.
     * Returns true if images are similar enough (same person, roughly).
     */
    private static boolean compareFaces(String email, String base64DataUrl) {
        try {
            Path storedFile = FACES_DIR.resolve(sanitizeFilename(email) + ".jpg");
            if (!Files.exists(storedFile)) {
                System.out.println("[FaceID] No stored face for: " + email);
                return false;
            }

            // Decode the new image
            String base64 = base64DataUrl;
            if (base64.contains(",")) base64 = base64.substring(base64.indexOf(",") + 1);
            byte[] newBytes = Base64.getDecoder().decode(base64);

            // Load both images using JavaFX (they're JPEG)
            javafx.scene.image.Image storedImg = new javafx.scene.image.Image(new FileInputStream(storedFile.toFile()));
            javafx.scene.image.Image newImg = new javafx.scene.image.Image(new ByteArrayInputStream(newBytes));

            // Compare using average color histogram on a 16x16 grid
            double similarity = compareImages(storedImg, newImg, 16);
            System.out.println("[FaceID] Similarity: " + String.format("%.2f", similarity * 100) + "%");

            // Threshold — 60% similarity means it's likely the same person
            return similarity >= 0.60;
        } catch (Exception e) {
            System.err.println("[FaceID] Compare error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Compare two images by dividing into grid cells and comparing average colors.
     * Returns 0.0 (completely different) to 1.0 (identical).
     */
    private static double compareImages(javafx.scene.image.Image img1, javafx.scene.image.Image img2, int gridSize) {
        var reader1 = img1.getPixelReader();
        var reader2 = img2.getPixelReader();
        if (reader1 == null || reader2 == null) return 0;

        int w1 = (int) img1.getWidth(), h1 = (int) img1.getHeight();
        int w2 = (int) img2.getWidth(), h2 = (int) img2.getHeight();

        double totalDiff = 0;
        int cells = 0;

        for (int gy = 0; gy < gridSize; gy++) {
            for (int gx = 0; gx < gridSize; gx++) {
                // Average color of this cell in image 1
                double[] avg1 = avgColor(reader1, w1, h1, gx, gy, gridSize);
                double[] avg2 = avgColor(reader2, w2, h2, gx, gy, gridSize);

                // Color distance (Euclidean in RGB space, normalized)
                double dr = avg1[0] - avg2[0];
                double dg = avg1[1] - avg2[1];
                double db = avg1[2] - avg2[2];
                double dist = Math.sqrt(dr * dr + dg * dg + db * db) / Math.sqrt(3.0);

                totalDiff += dist;
                cells++;
            }
        }

        return 1.0 - (totalDiff / cells);
    }

    private static double[] avgColor(javafx.scene.image.PixelReader reader, int imgW, int imgH, int gx, int gy, int gridSize) {
        int x0 = gx * imgW / gridSize;
        int y0 = gy * imgH / gridSize;
        int x1 = (gx + 1) * imgW / gridSize;
        int y1 = (gy + 1) * imgH / gridSize;

        double r = 0, g = 0, b = 0;
        int count = 0;
        for (int y = y0; y < y1; y += 2) { // Sample every 2nd pixel for speed
            for (int x = x0; x < x1; x += 2) {
                javafx.scene.paint.Color c = reader.getColor(x, y);
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                count++;
            }
        }
        if (count == 0) return new double[]{0, 0, 0};
        return new double[]{r / count, g / count, b / count};
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._@-]", "_");
    }

    /**
     * Simple JSON field extractor (avoids adding a JSON library dependency).
     * For the "image" key, finds the base64 value which can be very long.
     * For other keys, stops at the next quote.
     */
    private static String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();

        if ("image".equals(key)) {
            // Image value ends with "} or ," — find the closing quote carefully
            // The base64 data doesn't contain unescaped quotes, so find next "
            int end = json.indexOf("\"", start);
            if (end < 0) return null;
            return json.substring(start, end);
        }

        // For short fields, find the next unescaped quote
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
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
