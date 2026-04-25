package com.studyflow.services;

import com.studyflow.models.Deck;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.*;

/**
 * DeckQRCodeService  (v12 — RLIFE branding + Interactive HTML page)
 *
 * ✅ Works from ANY network — no WiFi restriction
 * ✅ Uses GitHub Gist (free, unlimited, no site limits)
 * ✅ One Gist per deck, updated automatically
 * ✅ App logo displayed in the center of the QR code
 * ✅ Fully interactive HTML page: tabs, ratings, share, study tips
 */
public class DeckQRCodeService {

    // ─────────────────────────────────────────────────────────────────────
    // 🔑 PASTE YOUR GITHUB TOKEN HERE (needs only "gist" scope)
    private static final String GITHUB_TOKEN = resolveGithubToken();
    // ─────────────────────────────────────────────────────────────────────

    private static final int    QR_SIZE   = 500;
    private static final String LOGO_PATH = "/com/studyflow/images/logo.png";

    private static final java.util.Map<Integer, String> urlCache  = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Integer, String> gistCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<Integer> uploading =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private static final java.io.File GIST_CACHE_FILE =
            new java.io.File(System.getProperty("user.home"), ".studyflow_gists");

    private static final String[] DECK_COLORS = {
            "7C3AED", "059669", "DC2626", "2563EB", "D97706",
            "DB2777", "0891B2", "65A30D", "9333EA", "4338CA"
    };

    private static String resolveGithubToken() {
        String envToken = System.getenv("STUDYFLOW_GITHUB_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken.trim();
        }

        String propertyToken = System.getProperty("studyflow.github.token");
        if (propertyToken != null && !propertyToken.isBlank()) {
            return propertyToken.trim();
        }

        return "";
    }

    private static String getQRColor(Deck deck) {
        return DECK_COLORS[Math.abs(deck.getIdDeck()) % DECK_COLORS.length];
    }

    // ── Load gist cache from disk ─────────────────────────────────────────
    private static void loadGistCache() {
        if (!GIST_CACHE_FILE.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(GIST_CACHE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 3);
                if (parts.length == 3) {
                    int deckId = Integer.parseInt(parts[0]);
                    gistCache.put(deckId, parts[1]);
                    urlCache.put(deckId, parts[2]);
                }
            }
        } catch (Exception e) {
            System.err.println("[Gist] Cache load error: " + e.getMessage());
        }
    }

    // ── Save gist cache to disk ───────────────────────────────────────────
    private static void saveGistCache() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(GIST_CACHE_FILE))) {
            for (var entry : gistCache.entrySet()) {
                String url = urlCache.get(entry.getKey());
                if (url != null) pw.println(entry.getKey() + "=" + entry.getValue() + "=" + url);
            }
        } catch (Exception e) {
            System.err.println("[Gist] Cache save error: " + e.getMessage());
        }
    }

    // ── Public entry point ────────────────────────────────────────────────
    public static void showQRCode(Deck deck, FlashcardService flashcardService) {
        if (uploading.contains(deck.getIdDeck())) return;
        uploading.add(deck.getIdDeck());

        if (gistCache.isEmpty()) loadGistCache();

        Thread bgThread = new Thread(() -> {
            try {
                String color = getQRColor(deck);

                int cardCount = 0;
                try { cardCount = flashcardService.getByDeck(deck.getIdDeck()).size(); }
                catch (Exception ignored) {}

                String pageUrl = urlCache.get(deck.getIdDeck());
                if (pageUrl == null) {
                    String html = buildHtmlPage(deck, cardCount);
                    pageUrl = uploadToGist(deck, html);
                    if (pageUrl != null) {
                        urlCache.put(deck.getIdDeck(), pageUrl);
                        saveGistCache();
                    }
                }

                if (pageUrl == null) {
                    javafx.application.Platform.runLater(() ->
                            showError("❌ GitHub Gist upload failed.\n\nMake sure GITHUB_TOKEN is set with 'gist' scope.\nGet token at: github.com/settings/tokens/new"));
                    return;
                }

                // ECC=H allows up to 30% coverage for the logo overlay
                String qrUrl = "https://api.qrserver.com/v1/create-qr-code/"
                        + "?data="   + URLEncoder.encode(pageUrl, StandardCharsets.UTF_8)
                        + "&size="   + QR_SIZE + "x" + QR_SIZE
                        + "&format=png&ecc=H&margin=2"
                        + "&color="  + color
                        + "&bgcolor=FFFFFF";

                Image qrImage = new Image(qrUrl, QR_SIZE, QR_SIZE, true, true);
                if (qrImage.isError()) {
                    qrImage = new Image("https://api.qrserver.com/v1/create-qr-code/"
                            + "?data=" + URLEncoder.encode(pageUrl, StandardCharsets.UTF_8)
                            + "&size=" + QR_SIZE + "x" + QR_SIZE + "&ecc=H&margin=2&format=png",
                            QR_SIZE, QR_SIZE, true, true);
                }

                Image logoImage = null;
                try {
                    var stream = DeckQRCodeService.class.getResourceAsStream(LOGO_PATH);
                    if (stream != null) logoImage = new Image(stream, 90, 90, true, true);
                } catch (Exception ignored) {}

                final Image finalQr   = qrImage;
                final Image finalLogo = logoImage;
                final String finalUrl = pageUrl;

                javafx.application.Platform.runLater(() ->
                        openPopup(deck, finalQr, finalLogo, color, finalUrl));

            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                        showError("QR error: " + ex.getMessage()));
            } finally {
                uploading.remove(deck.getIdDeck());
            }
        }, "qr-gist-thread");
        bgThread.setDaemon(true);
        bgThread.start();
    }

    // ── Upload HTML to GitHub Gist ────────────────────────────────────────
    private static synchronized String uploadToGist(Deck deck, String html) {
        try {
            if (GITHUB_TOKEN.isBlank()) {
                System.err.println("[Gist] Missing token. Set STUDYFLOW_GITHUB_TOKEN or -Dstudyflow.github.token.");
                return null;
            }

            HttpClient client = HttpClient.newHttpClient();

            String escaped = html
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String existingGistId = gistCache.get(deck.getIdDeck());
            String responseBody;
            int statusCode;

            if (existingGistId != null) {
                System.out.println("[Gist] Updating gist: " + existingGistId);
                String body = "{\"files\":{\"index.html\":{\"content\":\"" + escaped + "\"}}}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/gists/" + existingGistId))
                        .header("Authorization", "Bearer " + GITHUB_TOKEN)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                statusCode = resp.statusCode();
                responseBody = resp.body();
            } else {
                System.out.println("[Gist] Creating new gist for deck: " + deck.getIdDeck());
                String body = "{\"description\":\"RLIFE Deck: " + deck.getTitre() + "\"," +
                        "\"public\":true," +
                        "\"files\":{\"index.html\":{\"content\":\"" + escaped + "\"}}}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/gists"))
                        .header("Authorization", "Bearer " + GITHUB_TOKEN)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                statusCode = resp.statusCode();
                responseBody = resp.body();
            }

            if (statusCode != 200 && statusCode != 201) {
                System.err.println("[Gist] Failed: " + responseBody.substring(0, Math.min(300, responseBody.length())));
                return null;
            }

            String gistId = extractJsonField(responseBody, "id");
            if (gistId == null) return null;

            gistCache.put(deck.getIdDeck(), gistId);

            String user = extractGistUser(responseBody);
            String rawUrl = "https://gist.githack.com/" + user + "/" + gistId + "/raw/index.html";
            System.out.println("[Gist] URL: " + rawUrl);
            return rawUrl;

        } catch (Exception e) {
            System.err.println("[Gist] Upload error: " + e.getMessage());
            return null;
        }
    }

    // ── Extract GitHub username ───────────────────────────────────────────
    private static String extractGistUser(String json) {
        int ownerIdx = json.indexOf("\"owner\"");
        if (ownerIdx < 0) return "unknown";
        String sub = json.substring(ownerIdx);
        String login = extractJsonField(sub, "login");
        return login != null ? login : "unknown";
    }

    // ── JSON field extractor ──────────────────────────────────────────────
    private static String extractJsonField(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon < 0) return null;
        int start = json.indexOf("\"", colon + 1);
        if (start < 0) return null;
        int end = json.indexOf("\"", start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    // ── JavaFX Popup ──────────────────────────────────────────────────────
    private static void openPopup(Deck deck, Image qrImage, Image logoImage,
                                  String hexColor, String pageUrl) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setStyle(
                "-fx-background-color:#0F172A;" +
                        "-fx-background-radius:20;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.65),30,0,0,8);"
        );

        Label titleLbl = new Label(deck.getTitre());
        titleLbl.setStyle("-fx-text-fill:#" + hexColor + ";-fx-font-size:15px;-fx-font-weight:800;");
        titleLbl.setAlignment(Pos.CENTER);
        titleLbl.setMaxWidth(280);
        titleLbl.setWrapText(true);

        Label urlLbl = new Label("🌐 " + pageUrl);
        urlLbl.setStyle("-fx-text-fill:#64748B;-fx-font-size:9px;-fx-font-family:monospace;");
        urlLbl.setMaxWidth(280);
        urlLbl.setWrapText(true);
        urlLbl.setAlignment(Pos.CENTER);

        StackPane qrCard = new StackPane();
        qrCard.setAlignment(Pos.CENTER);
        qrCard.setStyle(
                "-fx-background-color:white;-fx-background-radius:16;-fx-padding:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(" + hexToRgb(hexColor) + ",0.5),22,0,0,4);"
        );

        ImageView qrIv = new ImageView(qrImage);
        qrIv.setFitWidth(290);
        qrIv.setFitHeight(290);
        qrIv.setPreserveRatio(true);
        qrIv.setSmooth(true);
        qrCard.getChildren().add(qrIv);

        // ── Logo overlay centered on QR ────────────────────────────────────
        if (logoImage != null) {
            Circle logoBg = new Circle(38);
            logoBg.setFill(Color.WHITE);
            logoBg.setStroke(Color.WHITE);
            logoBg.setStrokeWidth(6);

            ImageView logoIv = new ImageView(logoImage);
            logoIv.setFitWidth(60);
            logoIv.setFitHeight(60);
            logoIv.setPreserveRatio(true);
            logoIv.setSmooth(true);

            Circle clip = new Circle(30);
            clip.setCenterX(30);
            clip.setCenterY(30);
            logoIv.setClip(clip);

            StackPane logoOverlay = new StackPane(logoBg, logoIv);
            logoOverlay.setAlignment(Pos.CENTER);
            qrCard.getChildren().add(logoOverlay);
        }

        Button closeBtn = new Button("✕   Close");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        String sNormal = "-fx-background-color:rgba(255,255,255,0.04);-fx-text-fill:#64748B;" +
                "-fx-font-size:12px;-fx-font-weight:700;-fx-background-radius:10;" +
                "-fx-cursor:hand;-fx-padding:9 0;" +
                "-fx-border-color:#1E293B;-fx-border-radius:10;-fx-border-width:1;";
        String sHover = "-fx-background-color:rgba(220,38,38,0.1);-fx-text-fill:#FB7185;" +
                "-fx-font-size:12px;-fx-font-weight:700;-fx-background-radius:10;" +
                "-fx-cursor:hand;-fx-padding:9 0;" +
                "-fx-border-color:#FB7185;-fx-border-radius:10;-fx-border-width:1;";
        closeBtn.setStyle(sNormal);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(sHover));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle(sNormal));
        closeBtn.setOnAction(e -> popup.close());

        root.getChildren().addAll(titleLbl, urlLbl, qrCard, closeBtn);

        Scene scene = new Scene(root, 340, 460);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // FULLY INTERACTIVE HTML PAGE — RLIFE branding
    // ══════════════════════════════════════════════════════════════════════
    private static String buildHtmlPage(Deck deck, int cardCount) {
        String title   = safe(deck.getTitre());
        String subject = safe(deck.getMatiere());
        String level   = safe(deck.getNiveau());
        String desc    = safe(deck.getDescription());

        // Accent colors by level
        String accent, accentDark, accentRgb;
        String lvl = level.toLowerCase();
        if (lvl.contains("begin") || lvl.contains("débu") || lvl.contains("easy") || lvl.contains("facile")) {
            accent = "#10B981"; accentDark = "#059669"; accentRgb = "16,185,129";
        } else if (lvl.contains("advan") || lvl.contains("avancé") || lvl.contains("hard") || lvl.contains("difficile")) {
            accent = "#EF4444"; accentDark = "#DC2626"; accentRgb = "239,68,68";
        } else {
            accent = "#8B5CF6"; accentDark = "#7C3AED"; accentRgb = "139,92,246";
        }

        String levelBadgeBg, levelBadgeColor;
        if (lvl.contains("begin") || lvl.contains("débu") || lvl.contains("easy") || lvl.contains("facile")) {
            levelBadgeBg = "rgba(16,185,129,0.15)"; levelBadgeColor = "#10B981";
        } else if (lvl.contains("advan") || lvl.contains("avancé") || lvl.contains("hard") || lvl.contains("difficile")) {
            levelBadgeBg = "rgba(239,68,68,0.15)"; levelBadgeColor = "#EF4444";
        } else {
            levelBadgeBg = "rgba(139,92,246,0.15)"; levelBadgeColor = "#8B5CF6";
        }

        String createdDate = java.time.LocalDate.now().toString();
        int studyMinutes   = cardCount * 2;
        int progressPct    = Math.min(98, Math.max(8, cardCount * 8));

        String descHtml = desc.isBlank()
                ? "<p style='color:#475569;font-style:italic;font-size:14px;text-align:center;padding:12px 0;'>No description provided.</p>"
                : "<p style='color:#94a3b8;font-size:15px;line-height:1.8;'>" + escHtml(desc) + "</p>";

        // ── CSS ───────────────────────────────────────────────────────────
        String css =
                "*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent;}" +
                        "body{background:#060d1a;color:#f1f5f9;font-family:'Inter',system-ui,sans-serif;min-height:100vh;overflow-x:hidden;}" +
                        ".bg-mesh{position:fixed;inset:0;pointer-events:none;z-index:0;" +
                        "background:radial-gradient(ellipse 80% 60% at 20% -10%," + accent + "20 0%,transparent 60%)," +
                        "radial-gradient(ellipse 60% 50% at 80% 110%," + accentDark + "18 0%,transparent 60%);}" +
                        ".bg-grid{position:fixed;inset:0;pointer-events:none;z-index:0;opacity:.035;" +
                        "background-image:linear-gradient(" + accent + " 1px,transparent 1px),linear-gradient(90deg," + accent + " 1px,transparent 1px);" +
                        "background-size:40px 40px;}" +
                        ".particle{position:fixed;border-radius:50%;background:" + accent + ";pointer-events:none;animation:float linear infinite;opacity:0;}" +
                        ".page{max-width:480px;margin:0 auto;padding:22px 16px 90px;position:relative;z-index:1;}" +

                        // Top bar
                        ".topbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:24px;}" +
                        ".logo-wrap{display:flex;align-items:center;gap:10px;}" +
                        ".logo-ring{width:40px;height:40px;border-radius:50%;background:linear-gradient(135deg," + accent + "," + accentDark + ");display:flex;align-items:center;justify-content:center;font-size:20px;font-weight:900;color:#fff;box-shadow:0 0 0 0 rgba(" + accentRgb + ",.4);animation:pulseRing 3s infinite;}" +
                        ".logo-name{font-size:18px;font-weight:900;letter-spacing:.1em;background:linear-gradient(90deg," + accent + ",#c4b5fd,#fff);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;}" +
                        ".live-badge{display:flex;align-items:center;gap:6px;padding:6px 14px;border:1px solid rgba(255,255,255,.08);border-radius:100px;background:rgba(255,255,255,.03);}" +
                        ".live-dot{width:7px;height:7px;border-radius:50%;background:" + accent + ";animation:blink 2s infinite;}" +
                        ".live-text{font-size:10px;letter-spacing:.14em;text-transform:uppercase;color:#64748b;}" +

                        // Hero
                        ".hero{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);border-radius:24px;overflow:hidden;margin-bottom:14px;}" +
                        ".hero-bar{height:4px;background:linear-gradient(90deg,transparent," + accent + " 30%," + accentDark + " 70%,transparent);}" +
                        ".hero-body{padding:22px;}" +
                        ".hero-top{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:18px;}" +
                        ".deck-icon{width:52px;height:52px;border-radius:16px;background:linear-gradient(135deg," + accent + "30," + accentDark + "50);border:1px solid " + accent + "40;display:flex;align-items:center;justify-content:center;font-size:26px;}" +
                        ".level-pill{padding:5px 12px;border-radius:100px;font-size:11px;font-weight:600;letter-spacing:.06em;background:" + levelBadgeBg + ";color:" + levelBadgeColor + ";border:1px solid " + levelBadgeColor + "30;}" +
                        ".hero-title{font-size:21px;font-weight:800;line-height:1.2;margin-bottom:5px;}" +
                        ".hero-sub{font-size:13px;color:#64748b;margin-bottom:18px;}" +
                        ".hero-sub b{color:" + accent + ";font-weight:500;}" +

                        // Stats
                        ".stats-row{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-bottom:18px;}" +
                        ".stat{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.06);border-radius:14px;padding:12px 6px;text-align:center;cursor:pointer;transition:all .2s;}" +
                        ".stat:hover{border-color:" + accent + "70;background:rgba(" + accentRgb + ",.09);transform:translateY(-2px);}" +
                        ".stat-v{font-size:22px;font-weight:800;color:" + accent + ";line-height:1;}" +
                        ".stat-l{font-size:10px;color:#475569;text-transform:uppercase;letter-spacing:.08em;margin-top:3px;}" +

                        // Progress
                        ".prog-row{display:flex;justify-content:space-between;font-size:11px;color:#475569;margin-bottom:6px;}" +
                        ".prog-bar{height:7px;background:rgba(255,255,255,.06);border-radius:100px;overflow:hidden;}" +
                        ".prog-fill{height:100%;border-radius:100px;background:linear-gradient(90deg," + accent + "," + accentDark + ");width:0;transition:width 1.6s cubic-bezier(.4,0,.2,1);position:relative;overflow:hidden;}" +
                        ".prog-fill::after{content:'';position:absolute;inset:0;background:linear-gradient(90deg,transparent,rgba(255,255,255,.28),transparent);animation:shimmer 2.2s infinite;}" +

                        // Tabs
                        ".tabs{display:flex;gap:3px;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.07);border-radius:14px;padding:4px;margin-bottom:14px;}" +
                        ".tab{flex:1;padding:9px 4px;border-radius:10px;font-size:11px;font-weight:600;text-align:center;cursor:pointer;transition:all .2s;color:#475569;border:none;background:transparent;}" +
                        ".tab.active{background:" + accent + ";color:#fff;box-shadow:0 3px 12px rgba(" + accentRgb + ",.45);}" +
                        ".tab:not(.active):hover{background:rgba(255,255,255,.06);color:#94a3b8;}" +
                        ".panel{display:none;animation:fadeUp .3s ease;}" +
                        ".panel.show{display:block;}" +

                        // Cards
                        ".card{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.07);border-radius:18px;padding:16px 18px;margin-bottom:12px;}" +
                        ".ctitle{font-size:10px;text-transform:uppercase;letter-spacing:.14em;color:" + accent + ";font-weight:700;margin-bottom:12px;display:flex;align-items:center;gap:6px;}" +
                        ".ctitle-bar{width:3px;height:13px;border-radius:2px;background:" + accent + ";display:inline-block;}" +

                        // Info rows
                        ".irow{display:flex;align-items:center;justify-content:space-between;padding:9px 0;border-bottom:1px solid rgba(255,255,255,.05);}" +
                        ".irow:last-child{border-bottom:none;}" +
                        ".ilabel{font-size:13px;color:#64748b;display:flex;align-items:center;gap:8px;}" +
                        ".iicon{width:28px;height:28px;border-radius:8px;background:rgba(" + accentRgb + ",.12);display:flex;align-items:center;justify-content:center;font-size:13px;}" +
                        ".ival{font-size:13px;font-weight:600;color:#e2e8f0;}" +

                        // Rating
                        ".stars-row{display:flex;justify-content:center;gap:10px;margin:14px 0 8px;}" +
                        ".star{font-size:38px;cursor:pointer;transition:transform .15s,filter .15s;filter:grayscale(1) brightness(.35);user-select:none;}" +
                        ".star.on,.star:hover{filter:none;}" +
                        ".star.on{animation:starPop .25s ease;}" +
                        ".rmsg{font-size:14px;color:#64748b;text-align:center;min-height:22px;transition:all .3s;}" +
                        ".submit-btn{width:100%;margin-top:14px;padding:13px;border-radius:13px;" +
                        "background:linear-gradient(135deg," + accent + "," + accentDark + ");" +
                        "color:#fff;font-size:14px;font-weight:700;border:none;cursor:pointer;" +
                        "box-shadow:0 4px 18px rgba(" + accentRgb + ",.35);transition:all .2s;opacity:.45;pointer-events:none;}" +
                        ".submit-btn.ready{opacity:1;pointer-events:all;}" +
                        ".submit-btn.ready:hover{transform:translateY(-2px);box-shadow:0 6px 24px rgba(" + accentRgb + ",.5);}" +
                        ".thanks-box{display:none;padding:14px;background:rgba(16,185,129,.1);border:1px solid rgba(16,185,129,.25);border-radius:14px;text-align:center;color:#10B981;font-weight:600;font-size:14px;}" +
                        ".comment-area{width:100%;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.1);border-radius:12px;padding:11px 13px;color:#f1f5f9;font-size:14px;font-family:inherit;resize:vertical;min-height:80px;margin-top:10px;outline:none;transition:border .2s;}" +
                        ".comment-area:focus{border-color:" + accent + "60;}" +
                        ".comment-area::placeholder{color:#334155;}" +

                        // Action buttons
                        ".act-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;}" +
                        ".act-btn{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:7px;" +
                        "padding:15px 10px;border-radius:16px;border:1px solid rgba(255,255,255,.08);" +
                        "background:rgba(255,255,255,.04);cursor:pointer;transition:all .2s;text-decoration:none;}" +
                        ".act-btn:hover{border-color:" + accent + "60;background:rgba(" + accentRgb + ",.08);transform:translateY(-2px);}" +
                        ".act-btn:active{transform:translateY(0);}" +
                        ".act-icon{width:42px;height:42px;border-radius:12px;background:rgba(" + accentRgb + ",.15);display:flex;align-items:center;justify-content:center;font-size:20px;}" +
                        ".act-label{font-size:12px;font-weight:600;color:#94a3b8;}" +

                        // Share
                        ".url-box{display:flex;align-items:center;gap:8px;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.08);border-radius:12px;padding:10px 13px;margin-top:10px;}" +
                        ".url-txt{flex:1;font-size:11px;font-family:monospace;color:#64748b;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}" +
                        ".copy-btn{padding:7px 14px;border-radius:8px;background:" + accent + ";color:#fff;font-size:11px;font-weight:700;border:none;cursor:pointer;flex-shrink:0;transition:all .15s;}" +
                        ".copy-btn:active{transform:scale(.95);}" +

                        // Study tip rows
                        ".tip-row{display:flex;gap:12px;align-items:flex-start;padding:8px 0;border-bottom:1px solid rgba(255,255,255,.05);}" +
                        ".tip-row:last-child{border-bottom:none;}" +
                        ".tip-icon{width:34px;height:34px;border-radius:10px;background:rgba(" + accentRgb + ",.14);display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0;}" +
                        ".tip-title{font-size:13px;font-weight:600;color:#e2e8f0;margin-bottom:2px;}" +
                        ".tip-sub{font-size:12px;color:#64748b;line-height:1.5;}" +

                        // Floating CTA
                        ".fcta{position:fixed;bottom:0;left:0;right:0;z-index:100;padding:14px 16px;" +
                        "background:linear-gradient(0deg,#060d1a 65%,transparent);" +
                        "display:flex;justify-content:center;}" +
                        ".cta-btn{display:inline-flex;align-items:center;gap:10px;padding:14px 32px;border-radius:100px;" +
                        "background:linear-gradient(135deg," + accent + "," + accentDark + ");" +
                        "color:#fff;font-size:15px;font-weight:700;border:none;cursor:pointer;" +
                        "box-shadow:0 8px 28px rgba(" + accentRgb + ",.5);transition:all .2s;}" +
                        ".cta-btn:hover{transform:translateY(-2px);box-shadow:0 12px 36px rgba(" + accentRgb + ",.6);}" +

                        // Footer
                        ".footer{text-align:center;padding-top:22px;padding-bottom:16px;}" +
                        ".brand{font-size:15px;font-weight:900;letter-spacing:.2em;text-transform:uppercase;" +
                        "background:linear-gradient(90deg," + accent + ",#c4b5fd,#fff," + accent + ");background-size:300%;-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;" +
                        "animation:brandShine 5s linear infinite;}" +
                        ".brand-sub{font-size:11px;color:#1e293b;margin-top:5px;}" +

                        // Animations
                        "@keyframes blink{0%,100%{opacity:1}50%{opacity:.2}}" +
                        "@keyframes shimmer{0%{transform:translateX(-100%)}100%{transform:translateX(400%)}}" +
                        "@keyframes float{0%{transform:translateY(100vh);opacity:0}10%{opacity:.8}90%{opacity:.4}100%{transform:translateY(-5vh);opacity:0}}" +
                        "@keyframes fadeUp{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}" +
                        "@keyframes starPop{0%{transform:scale(1)}50%{transform:scale(1.4)}100%{transform:scale(1.18)}}" +
                        "@keyframes brandShine{0%{background-position:0%}100%{background-position:300%}}" +
                        "@keyframes pulseRing{0%{box-shadow:0 0 0 0 rgba(" + accentRgb + ",.5)}70%{box-shadow:0 0 0 10px rgba(" + accentRgb + ",0)}100%{box-shadow:0 0 0 0 rgba(" + accentRgb + ",0)}}";

        // ── HTML ──────────────────────────────────────────────────────────
        return "<!DOCTYPE html><html lang='en'><head>" +
                "<meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>" +
                "<meta name='theme-color' content='" + accent + "'>" +
                "<title>" + escHtml(title) + " — RLIFE</title>" +
                "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap' rel='stylesheet'>" +
                "<style>" + css + "</style></head><body>" +
                "<div class='bg-mesh'></div><div class='bg-grid'></div>" +
                "<div class='page'>" +

                // ── Top bar ──
                "<div class='topbar'>" +
                "<div class='logo-wrap'><div class='logo-ring'>R</div><span class='logo-name'>RLIFE</span></div>" +
                "<div class='live-badge'><div class='live-dot'></div><span class='live-text'>Live deck</span></div>" +
                "</div>" +

                // ── Hero ──
                "<div class='hero'><div class='hero-bar'></div><div class='hero-body'>" +
                "<div class='hero-top'><div class='deck-icon'>📚</div><div class='level-pill'>" + escHtml(level) + "</div></div>" +
                "<div class='hero-title'>" + escHtml(title) + "</div>" +
                "<div class='hero-sub'><b>" + escHtml(subject) + "</b> &bull; " + createdDate + "</div>" +
                "<div class='stats-row'>" +
                "<div class='stat'><div class='stat-v'>" + cardCount + "</div><div class='stat-l'>Cards</div></div>" +
                "<div class='stat'><div class='stat-v' id='ratingDisp'>—</div><div class='stat-l'>My rating</div></div>" +
                "<div class='stat'><div class='stat-v'>🔥</div><div class='stat-l'>Active</div></div>" +
                "</div>" +
                "<div class='prog-row'><span>Deck completion</span><span>" + cardCount + " cards ready</span></div>" +
                "<div class='prog-bar'><div class='prog-fill' id='pf'></div></div>" +
                "</div></div>" +

                // ── Tabs ──
                "<div class='tabs'>" +
                "<button class='tab active' onclick='showTab(\"overview\",this)'>Overview</button>" +
                "<button class='tab' onclick='showTab(\"rate\",this)'>Rate ⭐</button>" +
                "<button class='tab' onclick='showTab(\"details\",this)'>Details</button>" +
                "<button class='tab' onclick='showTab(\"share\",this)'>Share</button>" +
                "</div>" +

                // ════════════════ TAB: Overview ════════════════
                "<div class='panel show' id='tab-overview'>" +
                "<div class='card'><div class='ctitle'><div class='ctitle-bar'></div>About this deck</div>" + descHtml + "</div>" +
                "<div class='card'><div class='ctitle'><div class='ctitle-bar'></div>Quick actions</div>" +
                "<div class='act-grid'>" +
                "<a class='act-btn' onclick='copyLink()'><div class='act-icon'>🔗</div><span class='act-label'>Copy link</span></a>" +
                "<a class='act-btn' onclick='shareNative()'><div class='act-icon'>📤</div><span class='act-label'>Share</span></a>" +
                "<a class='act-btn' onclick='switchTab(\"rate\")'><div class='act-icon'>⭐</div><span class='act-label'>Rate deck</span></a>" +
                "<a class='act-btn' onclick='switchTab(\"details\")'><div class='act-icon'>📋</div><span class='act-label'>Details</span></a>" +
                "</div></div>" +
                "</div>" +

                // ════════════════ TAB: Rate ════════════════
                "<div class='panel' id='tab-rate'>" +
                "<div class='card'><div class='ctitle'><div class='ctitle-bar'></div>Rate this deck</div>" +
                "<p style='font-size:13px;color:#64748b;text-align:center;'>How useful was this deck for you?</p>" +
                "<div class='stars-row' id='starsRow'>" +
                "<span class='star' data-v='1' onclick='rate(1)'>⭐</span>" +
                "<span class='star' data-v='2' onclick='rate(2)'>⭐</span>" +
                "<span class='star' data-v='3' onclick='rate(3)'>⭐</span>" +
                "<span class='star' data-v='4' onclick='rate(4)'>⭐</span>" +
                "<span class='star' data-v='5' onclick='rate(5)'>⭐</span>" +
                "</div>" +
                "<div class='rmsg' id='rmsg'>Tap a star to rate</div>" +
                "</div>" +
                "<div class='card' id='commentCard'><div class='ctitle'><div class='ctitle-bar'></div>Leave a comment</div>" +
                "<p style='font-size:13px;color:#64748b;'>Share your feedback about this deck</p>" +
                "<textarea class='comment-area' id='commentBox' placeholder='This deck really helped me with...' maxlength='280'></textarea>" +
                "<div style='text-align:right;font-size:11px;color:#334155;margin-top:4px;' id='charCount'>0 / 280</div>" +
                "</div>" +
                "<button class='submit-btn' id='submitBtn' onclick='submitRating()'>Submit rating</button>" +
                "<div class='thanks-box' id='thanksBox'></div>" +
                "</div>" +

                // ════════════════ TAB: Details ════════════════
                "<div class='panel' id='tab-details'>" +
                "<div class='card'><div class='ctitle'><div class='ctitle-bar'></div>Deck info</div>" +
                "<div class='irow'><span class='ilabel'><div class='iicon'>📚</div>Title</span><span class='ival'>" + escHtml(title) + "</span></div>" +
                "<div class='irow'><span class='ilabel'><div class='iicon'>🎯</div>Subject</span><span class='ival'>" + escHtml(subject) + "</span></div>" +
                "<div class='irow'><span class='ilabel'><div class='iicon'>📊</div>Level</span><span class='ival' style='color:" + levelBadgeColor + ";'>" + escHtml(level) + "</span></div>" +
                "<div class='irow'><span class='ilabel'><div class='iicon'>🃏</div>Cards</span><span class='ival'>" + cardCount + " flashcards</span></div>" +
                "<div class='irow'><span class='ilabel'><div class='iicon'>📅</div>Created</span><span class='ival'>" + createdDate + "</span></div>" +
                "<div class='irow'><span class='ilabel'><div class='iicon'>⏱️</div>Est. study time</span><span class='ival'>~" + studyMinutes + " min</span></div>" +
                "</div>" +
                "<div class='card'><div class='ctitle'><div class='ctitle-bar'></div>Study tips</div>" +
                "<div class='tip-row'><div class='tip-icon'>🔁</div><div><div class='tip-title'>Spaced repetition</div><div class='tip-sub'>Review weak cards more often for better long-term retention.</div></div></div>" +
                "<div class='tip-row'><div class='tip-icon'>🧠</div><div><div class='tip-title'>Active recall</div><div class='tip-sub'>Cover the answer and try to recall it before flipping.</div></div></div>" +
                "<div class='tip-row'><div class='tip-icon'>⏰</div><div><div class='tip-title'>Short sessions</div><div class='tip-sub'>Study in 20-min blocks to maximize focus and retention.</div></div></div>" +
                "</div>" +
                "</div>" +

                // ════════════════ TAB: Share ════════════════
                "<div class='panel' id='tab-share'>" +
                "<div class='card'><div class='ctitle'><div class='ctitle-bar'></div>Share this deck</div>" +
                "<p style='font-size:13px;color:#64748b;'>Anyone with the link can view this deck</p>" +
                "<div class='url-box'><span class='url-txt' id='urlTxt'>Loading...</span><button class='copy-btn' onclick='copyLink()' id='copyBtn'>Copy</button></div>" +
                "</div>" +
                "<div class='card'><div class='ctitle'><div class='ctitle-bar'></div>Share via</div>" +
                "<div class='act-grid'>" +
                "<a class='act-btn' id='waBtn' href='#'><div class='act-icon'>💬</div><span class='act-label'>WhatsApp</span></a>" +
                "<a class='act-btn' id='tgBtn' href='#'><div class='act-icon'>✈️</div><span class='act-label'>Telegram</span></a>" +
                "<a class='act-btn' id='twBtn' href='#'><div class='act-icon'>🐦</div><span class='act-label'>Twitter</span></a>" +
                "<a class='act-btn' onclick='shareNative()'><div class='act-icon'>📱</div><span class='act-label'>More...</span></a>" +
                "</div></div>" +
                "</div>" +

                // ── Footer ──
                "<div class='footer'><div class='brand'>RLIFE</div><div class='brand-sub'>Flash your knowledge · Powered by RLIFE</div></div>" +
                "</div>" + // end .page

                // ── Floating CTA ──
                "<div class='fcta'><button class='cta-btn' onclick='window.scrollTo({top:0,behavior:\"smooth\"})'>🚀 Start studying</button></div>" +

                // ══════════════════════════════════════════════
                // JAVASCRIPT
                // ══════════════════════════════════════════════
                "<script>" +
                "var URL=window.location.href;" +
                "var MSGS=['','Terrible :(','Not great...','It was okay 👌','Pretty good! 👍','Amazing! 🎉'];" +
                "var chosen=0;" +

                // On load
                "window.addEventListener('load',function(){" +
                "  setTimeout(function(){var p=document.getElementById('pf');if(p)p.style.width='" + progressPct + "%';},400);" +
                "  var u=document.getElementById('urlTxt');if(u)u.textContent=URL;" +
                "  var enc=encodeURIComponent(URL);" +
                "  var msg=encodeURIComponent('Check out this RLIFE deck: " + escHtml(title) + " ');" +
                "  var wa=document.getElementById('waBtn');if(wa)wa.href='https://wa.me/?text='+msg+enc;" +
                "  var tg=document.getElementById('tgBtn');if(tg)tg.href='https://t.me/share/url?url='+enc+'&text='+msg;" +
                "  var tw=document.getElementById('twBtn');if(tw)tw.href='https://twitter.com/intent/tweet?url='+enc+'&text='+msg;" +
                "  spawnParticles();" +
                "  var saved=localStorage.getItem('rlife_rating');if(saved)restoreRating(parseInt(saved));" +
                "});" +

                // Tab switching
                "function showTab(name,btn){" +
                "  document.querySelectorAll('.panel').forEach(function(p){p.classList.remove('show');});" +
                "  document.querySelectorAll('.tab').forEach(function(t){t.classList.remove('active');});" +
                "  var p=document.getElementById('tab-'+name);if(p)p.classList.add('show');" +
                "  if(btn)btn.classList.add('active');" +
                "}" +
                "function switchTab(name){" +
                "  var idx={'overview':0,'rate':1,'details':2,'share':3}[name]||0;" +
                "  var tabs=document.querySelectorAll('.tab');" +
                "  if(tabs[idx])showTab(name,tabs[idx]);" +
                "  window.scrollTo({top:0,behavior:'smooth'});" +
                "}" +

                // Star rating
                "function rate(v){" +
                "  chosen=v;" +
                "  document.querySelectorAll('.star').forEach(function(s){" +
                "    s.classList.toggle('on',parseInt(s.dataset.v)<=v);" +
                "  });" +
                "  var m=document.getElementById('rmsg');if(m)m.textContent=MSGS[v];" +
                "  var rd=document.getElementById('ratingDisp');if(rd)rd.textContent=v+'/5';" +
                "  var sb=document.getElementById('submitBtn');" +
                "  if(sb){sb.classList.add('ready');sb.textContent='Submit '+v+'-star rating 🚀';}" +
                "}" +

                // Char counter
                "var cb=document.getElementById('commentBox');" +
                "if(cb)cb.addEventListener('input',function(){var cc=document.getElementById('charCount');if(cc)cc.textContent=cb.value.length+' / 280';});" +

                // Submit
                "function submitRating(){" +
                "  if(!chosen)return;" +
                "  localStorage.setItem('rlife_rating',chosen);" +
                "  var sb=document.getElementById('submitBtn');" +
                "  var tb=document.getElementById('thanksBox');" +
                "  var cc=document.getElementById('commentCard');" +
                "  var sr=document.getElementById('starsRow');" +
                "  if(sb)sb.style.display='none';" +
                "  if(cc)cc.style.display='none';" +
                "  if(sr)sr.style.pointerEvents='none';" +
                "  if(tb){tb.style.display='block';tb.textContent='You gave this deck '+chosen+' star'+(chosen>1?'s':'')+' — thank you! 🎉';}" +
                "}" +

                // Restore saved rating
                "function restoreRating(v){" +
                "  if(v<1||v>5)return;" +
                "  rate(v);" +
                "  var sb=document.getElementById('submitBtn');" +
                "  var tb=document.getElementById('thanksBox');" +
                "  var cc=document.getElementById('commentCard');" +
                "  if(sb)sb.style.display='none';" +
                "  if(cc)cc.style.display='none';" +
                "  if(tb){tb.style.display='block';tb.textContent='You already rated this deck '+v+' star'+(v>1?'s':'')+' ⭐';}" +
                "}" +

                // Copy link
                "function copyLink(){" +
                "  navigator.clipboard.writeText(URL).then(function(){" +
                "    var b=document.getElementById('copyBtn');if(b){b.textContent='Copied!';setTimeout(function(){b.textContent='Copy';},2000);}" +
                "  }).catch(function(){var ta=document.createElement('textarea');ta.value=URL;document.body.appendChild(ta);ta.select();document.execCommand('copy');document.body.removeChild(ta);});" +
                "}" +

                // Native share
                "function shareNative(){" +
                "  if(navigator.share){navigator.share({title:'" + escHtml(title) + "',text:'Check out this RLIFE deck!',url:URL});}else{copyLink();}" +
                "}" +

                // Floating particles
                "function spawnParticles(){" +
                "  for(var i=0;i<15;i++){" +
                "    (function(i){setTimeout(function(){" +
                "      var p=document.createElement('div');p.className='particle';" +
                "      var sz=2+Math.random()*3;" +
                "      p.style.cssText='left:'+Math.random()*100+'vw;width:'+sz+'px;height:'+sz+'px;" +
                "        animation-duration:'+(9+Math.random()*14)+'s;animation-delay:'+(Math.random()*8)+'s;';" +
                "      document.body.appendChild(p);" +
                "    },i*350);})(i);" +
                "  }" +
                "}" +
                "</script>" +
                "</body></html>";
    }

    // ── Utils ─────────────────────────────────────────────────────────────
    private static String hexToRgb(String hex) {
        try {
            return Integer.parseInt(hex.substring(0, 2), 16) + "," +
                    Integer.parseInt(hex.substring(2, 4), 16) + "," +
                    Integer.parseInt(hex.substring(4, 6), 16);
        } catch (Exception e) { return "124,58,237"; }
    }

    private static void showError(String msg) {
        new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, msg,
                javafx.scene.control.ButtonType.OK).showAndWait();
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
}
