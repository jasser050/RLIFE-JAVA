package com.studyflow.utils;

import javafx.application.Platform;
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

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class EmbeddedMeetingBrowser {
    private static final ExecutorService INIT_EXECUTOR = Executors.newSingleThreadExecutor(new BrowserThreadFactory());
    private static final Object APP_LOCK = new Object();
    private static CompletableFuture<CefApp> appFuture;
    private static int openSessionCount;

    private EmbeddedMeetingBrowser() {
    }

    public static CompletableFuture<BrowserSession> openWindow(String title, String url, Consumer<String> statusConsumer) {
        updateStatus(statusConsumer, "Starting meeting window...");
        return app().thenCompose(app -> createWindowSession(app, title, url, statusConsumer));
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
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--disable-background-networking",
                    "--disable-component-update",
                    "--disable-sync",
                    "--disable-push-api",
                    "--disable-notifications",
                    "--suppress-message-center-popups",
                    "--disable-breakpad",
                    "--disable-domain-reliability",
                    "--disable-client-side-phishing-detection",
                    "--disable-features=HardwareMediaKeyHandling,MediaRouter,PushMessaging,NotificationTriggers",
                    "--autoplay-policy=no-user-gesture-required",
                    "--enable-media-stream",
                    "--disable-session-crashed-bubble",
                    "--disable-restore-session-state",
                    "--metrics-recording-only"
            );

            CefSettings settings = builder.getCefSettings();
            settings.windowless_rendering_enabled = false;
            settings.persist_session_cookies = false;
            settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE;
            File cacheDir = Files.createTempDirectory("rlife-meeting-jcef").toFile();
            cacheDir.deleteOnExit();
            settings.cache_path = cacheDir.getAbsolutePath();
            settings.root_cache_path = settings.cache_path;

            return builder.build();
        } catch (IOException | UnsupportedPlatformException | InterruptedException | CefInitializationException ex) {
            throw new CompletionException(ex);
        }
    }

    private static CompletableFuture<BrowserSession> createWindowSession(CefApp app, String title, String url, Consumer<String> statusConsumer) {
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

                JFrame frame = new JFrame(safe(title).isBlank() ? "Project Meeting" : title);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setPreferredSize(new Dimension(1320, 860));
                frame.setMinimumSize(new Dimension(1080, 720));
                frame.setLayout(new BorderLayout());
                frame.add(panel, BorderLayout.CENTER);

                BrowserSession session = new BrowserSession(client, browser, panel, frame, statusConsumer);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        session.dispose();
                    }
                });

                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                synchronized (APP_LOCK) {
                    openSessionCount++;
                }
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
                        try {
                            const stopTracks = (stream) => {
                                if (!stream || !stream.getTracks) return;
                                stream.getTracks().forEach(track => {
                                    try { track.stop(); } catch (e) {}
                                });
                            };
                            if (window.streams && Array.isArray(window.streams)) {
                                window.streams.forEach(stopTracks);
                                window.streams = [];
                            }
                            try {
                                document.querySelectorAll('video, audio').forEach(node => {
                                    try {
                                        stopTracks(node.srcObject);
                                        node.srcObject = null;
                                    } catch (e) {}
                                });
                            } catch (e) {}
                            try {
                                if (window.APP && window.APP.conference && window.APP.conference._room) {
                                    const localTracks = window.APP.conference._room.localTracks || [];
                                    localTracks.forEach(track => {
                                        try { track.dispose(); } catch (e) {}
                                        try {
                                            if (track.stream) {
                                                stopTracks(track.stream);
                                            }
                                        } catch (e) {}
                                    });
                                }
                            } catch (e) {}
                        } catch (e) {}
                    } catch (e) {}
                })();
                """;

        private final CefClient client;
        private final CefBrowser browser;
        private final JPanel panel;
        private final JFrame frame;
        private final Consumer<String> statusConsumer;
        private volatile boolean disposed;

        private BrowserSession(CefClient client, CefBrowser browser, JPanel panel, JFrame frame, Consumer<String> statusConsumer) {
            this.client = client;
            this.browser = browser;
            this.panel = panel;
            this.frame = frame;
            this.statusConsumer = statusConsumer;
        }

        public void reload() {
            if (disposed) {
                return;
            }
            SwingUtilities.invokeLater(browser::reload);
        }

        public void focus() {
            if (disposed) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                frame.setVisible(true);
                frame.setState(JFrame.NORMAL);
                frame.toFront();
                frame.requestFocus();
            });
        }

        public boolean isClosed() {
            return disposed;
        }

        public void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
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
                frame.setVisible(false);
                frame.dispose();
                browser.close(true);
                client.dispose();
                releaseAppIfUnused();
            });
            updateStatus(statusConsumer, "Meeting window closed.");
        }
    }

    private static void releaseAppIfUnused() {
        CompletableFuture<CefApp> futureToDispose = null;
        synchronized (APP_LOCK) {
            if (openSessionCount > 0) {
                openSessionCount--;
            }
            if (openSessionCount == 0) {
                futureToDispose = appFuture;
                appFuture = null;
            }
        }
        if (futureToDispose == null) {
            return;
        }
        futureToDispose.thenAccept(app -> {
            try {
                app.dispose();
            } catch (RuntimeException ignored) {
                // Best effort shutdown to release camera/mic owned by the browser process.
            }
        });
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
