package com.studyflow.utils;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class EmbeddedMeetingBrowser {
    private static final ExecutorService INIT_EXECUTOR = Executors.newSingleThreadExecutor(new BrowserThreadFactory());
    private static final Object APP_LOCK = new Object();
    private static CompletableFuture<CefApp> appFuture;

    private EmbeddedMeetingBrowser() {
    }

    public static CompletableFuture<BrowserSession> attach(SwingNode target, String url, Consumer<String> statusConsumer) {
        updateStatus(statusConsumer, "Starting embedded meeting...");
        return app().thenCompose(app -> createSession(app, target, url, statusConsumer));
    }

    private static CompletableFuture<CefApp> app() {
        synchronized (APP_LOCK) {
            if (appFuture == null) {
                appFuture = CompletableFuture.supplyAsync(EmbeddedMeetingBrowser::buildApp, INIT_EXECUTOR);
            }
            return appFuture;
        }
    }

    private static CefApp buildApp() {
        try {
            File installDir = new File(System.getProperty("user.home"), ".rlife/jcef");
            if (!installDir.exists() && !installDir.mkdirs()) {
                throw new IOException("Failed to prepare JCEF install directory.");
            }

            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(installDir);
            builder.addJcefArgs(
                    "--autoplay-policy=no-user-gesture-required",
                    "--enable-media-stream",
                    "--disable-features=HardwareMediaKeyHandling"
            );

            CefSettings settings = builder.getCefSettings();
            settings.windowless_rendering_enabled = false;
            settings.persist_session_cookies = true;
            settings.cache_path = new File(installDir, "cache").getAbsolutePath();
            settings.root_cache_path = settings.cache_path;

            return builder.build();
        } catch (IOException | UnsupportedPlatformException | InterruptedException | CefInitializationException ex) {
            throw new CompletionException(ex);
        }
    }

    private static CompletableFuture<BrowserSession> createSession(CefApp app, SwingNode target, String url, Consumer<String> statusConsumer) {
        CompletableFuture<BrowserSession> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                CefClient client = app.createClient();
                client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                    @Override
                    public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
                        browser.loadURL(targetUrl);
                        return true;
                    }
                });
                client.addLoadHandler(new CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadStart(CefBrowser browser, CefFrame frame, org.cef.network.CefRequest.TransitionType transitionType) {
                        if (frame.isMain()) {
                            updateStatus(statusConsumer, "Joining Jitsi room...");
                        }
                    }

                    @Override
                    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                        if (frame.isMain()) {
                            updateStatus(statusConsumer, "");
                        }
                    }

                    @Override
                    public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                        if (frame.isMain()) {
                            updateStatus(statusConsumer, "Meeting failed to load: " + safe(errorText));
                        }
                    }
                });
                client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                    @Override
                    public void onStatusMessage(CefBrowser browser, String value) {
                        if (value != null && !value.isBlank()) {
                            updateStatus(statusConsumer, value);
                        }
                    }
                });

                CefBrowser browser = client.createBrowser(url, false, false);
                browser.createImmediately();
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(browser.getUIComponent(), BorderLayout.CENTER);
                panel.revalidate();
                panel.repaint();

                BrowserSession session = new BrowserSession(client, browser, panel);
                Platform.runLater(() -> target.setContent(panel));
                future.complete(session);
            } catch (RuntimeException ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private static void updateStatus(Consumer<String> statusConsumer, String message) {
        if (statusConsumer == null) {
            return;
        }
        Platform.runLater(() -> statusConsumer.accept(message == null ? "" : message));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class BrowserSession {
        private static final String STOP_MEETING_SCRIPT = """
                (function() {
                    try {
                        if (window.meetApi) {
                            try { window.meetApi.executeCommand('toggleAudio'); } catch (e) {}
                            try { window.meetApi.executeCommand('toggleVideo'); } catch (e) {}
                            try { window.meetApi.executeCommand('hangup'); } catch (e) {}
                            try { window.meetApi.dispose(); } catch (e) {}
                            window.meetApi = null;
                        }
                    } catch (e) {}
                })();
                """;

        private final CefClient client;
        private final CefBrowser browser;
        private final JPanel panel;

        private BrowserSession(CefClient client, CefBrowser browser, JPanel panel) {
            this.client = client;
            this.browser = browser;
            this.panel = panel;
        }

        public void reload() {
            SwingUtilities.invokeLater(browser::reload);
        }

        public void dispose(SwingNode target) {
            CountDownLatch cleanupLatch = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                try {
                    browser.executeJavaScript(STOP_MEETING_SCRIPT, browser.getURL(), 0);
                    try {
                        TimeUnit.MILLISECONDS.sleep(250);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    cleanupLatch.countDown();
                }
            });
            try {
                cleanupLatch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            SwingUtilities.invokeLater(() -> {
                panel.removeAll();
                browser.close(true);
                client.dispose();
            });
            Platform.runLater(() -> target.setContent(null));
        }
    }

    private static final class BrowserThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "embedded-meeting-browser");
            thread.setDaemon(true);
            return thread;
        }
    }
}
