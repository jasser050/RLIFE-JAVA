package com.studyflow.controllers;

import com.studyflow.models.Rating;
import com.studyflow.services.AIRatingAnalysisService;
import com.studyflow.services.RatingService;
import com.studyflow.utils.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;

/**
 * RatingStarsController
 * ─────────────────────────────────────────────────────────────────────────
 * Construit le composant étoiles pour chaque deck card.
 *
 * Usage dans FlashcardsFeaturesController.buildDeckCard() :
 *
 *   RatingStarsController rsc = new RatingStarsController();
 *   HBox stars = rsc.buildStarsRow(deck.getIdDeck(),
 *                                   deck.getTitre(),
 *                                   deck.getMatiere(),
 *                                   card);   // card = le VBox parent
 *   body.getChildren().add(stars);  // Ajouter avant le bouton Open
 */
public class RatingStarsController {

    private final RatingService         ratingService  = new RatingService();
    private final AIRatingAnalysisService aiService    = new AIRatingAnalysisService();

    // Délai anti-spam : si l'étudiant note plusieurs decks rapidement,
    // on n'appelle l'IA qu'une fois après 3 secondes d'inactivité.
    private static PauseTransition aiDebounceTimer;

    private final int    currentUserId;
    private final String currentUserName;

    public RatingStarsController() {
        var user = UserSession.getInstance().getCurrentUser();
        this.currentUserId   = user != null ? user.getId()   : 1;
        this.currentUserName = user != null ? user.getUsername() : "Étudiant";
    }

    // ─────────────────────────────────────────────────────────────────────
    //  COMPOSANT PRINCIPAL : buildStarsRow
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Retourne un HBox avec :
     * - Un label "Mon avis :"
     * - 5 étoiles cliquables
     * - Un label texte (ex. "Moyen")
     *
     * @param deckId      id du deck
     * @param deckName    titre du deck (pour le prompt IA)
     * @param deckSubject matière du deck (pour le prompt IA)
     * @param cardParent  le VBox de la carte (pour positionner le toast)
     */
    public VBox buildStarsRow(int deckId, String deckName, String deckSubject,
                               VBox cardParent) {

        // Charger le rating existant depuis la DB
        Rating existing = ratingService.getRatingByUserAndDeck(currentUserId, deckId);
        int currentStars = existing != null ? existing.getStars() : 0;

        // ── Label "Mon avis" ─────────────────────────────────────────────
        Label headerLbl = new Label("Mon avis :");
        headerLbl.setStyle(
                "-fx-text-fill:#64748B;-fx-font-size:10px;-fx-font-weight:700;");

        // ── 5 étoiles ────────────────────────────────────────────────────
        Label[] stars = new Label[5];
        HBox starsRow = new HBox(3);
        starsRow.setAlignment(Pos.CENTER_LEFT);

        Label ratingLabel = new Label(currentStars > 0
                ? getRatingText(currentStars) : "Non noté");
        ratingLabel.setStyle("-fx-text-fill:#64748B;-fx-font-size:10px;-fx-font-style:italic;");

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            Label star = new Label("★");
            star.setStyle(buildStarStyle(starIndex <= currentStars, currentStars));
            star.setOnMouseEntered(e -> highlightUpTo(stars, starIndex));
            star.setOnMouseExited (e -> resetHighlight(stars, currentStars));
            star.setOnMouseClicked(e -> {
                handleStarClick(deckId, deckName, deckSubject, starIndex,
                        stars, ratingLabel, cardParent);
            });
            stars[i] = star;
            starsRow.getChildren().add(star);
        }
        starsRow.getChildren().add(ratingLabel);

        // ── Wrapper vertical ─────────────────────────────────────────────
        VBox wrapper = new VBox(4, headerLbl, starsRow);
        wrapper.setPadding(new Insets(6, 0, 2, 0));
        // Empêche le clic sur les étoiles de propager vers "openDeck"
        wrapper.setOnMouseClicked(javafx.event.Event::consume);

        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CLIC SUR UNE ÉTOILE
    // ─────────────────────────────────────────────────────────────────────

    private void handleStarClick(int deckId, String deckName, String deckSubject,
                                  int stars, Label[] starLabels,
                                  Label ratingLabel, VBox cardParent) {
        // 1. Mettre à jour l'UI immédiatement
        resetHighlight(starLabels, stars);
        ratingLabel.setText(getRatingText(stars));
        ratingLabel.setStyle("-fx-text-fill:" + getRatingColor(stars)
                + ";-fx-font-size:10px;-fx-font-weight:700;");

        // 2. Sauvegarder en DB
        Rating rating = new Rating(currentUserId, deckId, stars);
        rating.setDeckName(deckName);
        rating.setDeckSubject(deckSubject);
        ratingService.upsertRating(rating);

        // 3. Déclencher l'analyse IA en arrière-plan (avec debounce 3s)
        if (stars <= 3) {
            triggerAIAnalysisDebounced(cardParent);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DEBOUNCE → ANALYSE IA
    // ─────────────────────────────────────────────────────────────────────

    private void triggerAIAnalysisDebounced(VBox cardParent) {
        // Annuler le timer précédent
        if (aiDebounceTimer != null) aiDebounceTimer.stop();

        aiDebounceTimer = new PauseTransition(Duration.seconds(3));
        aiDebounceTimer.setOnFinished(e -> {
            // Lancer l'analyse dans un thread séparé pour ne pas bloquer l'UI
            new Thread(() -> {
                List<Rating> allRatings = ratingService.getAllRatingsByUser(currentUserId);
                aiService.analyzeRatings(currentUserId, currentUserName, allRatings);

                // Vérifier si une notification a été créée → afficher toast
                boolean hasNewNotif = checkHasNewPendingNotification();
                if (hasNewNotif) {
                    Platform.runLater(() -> showToastOnCard(cardParent,
                            "Votre enseignant a été notifié.\nUn deck personnalisé sera créé pour vous."));
                }
            }, "AI-Rating-Thread").start();
        });
        aiDebounceTimer.play();
    }

    /** Vérifie si une notification récente (< 10s) existe pour cet étudiant. */
    private boolean checkHasNewPendingNotification() {
        try (var c = com.studyflow.utils.DataSource.getInstance().getConnection();
             var ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM admin_notifications " +
                     "WHERE student_id=? AND status='pending' " +
                     "AND created_at >= NOW() - INTERVAL 10 SECOND")) {
            ps.setInt(1, currentUserId);
            var rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception ignored) {}
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UI HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private void highlightUpTo(Label[] stars, int upTo) {
        for (int i = 0; i < stars.length; i++) {
            boolean lit = i < upTo;
            stars[i].setStyle(buildStarStyle(lit, upTo));
        }
    }

    private void resetHighlight(Label[] stars, int currentRating) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setStyle(buildStarStyle(i < currentRating, currentRating));
        }
    }

    private String buildStarStyle(boolean filled, int ratingLevel) {
        String color = filled ? getRatingColor(ratingLevel) : "#334155";
        return "-fx-font-size:15px;" +
               "-fx-text-fill:" + color + ";" +
               "-fx-cursor:hand;" +
               "-fx-padding:0 1;";
    }

    private String getRatingColor(int stars) {
        return switch (stars) {
            case 1 -> "#FB7185";
            case 2 -> "#FB923C";
            case 3 -> "#FBBF24";
            case 4 -> "#34D399";
            case 5 -> "#A78BFA";
            default -> "#64748B";
        };
    }

    private String getRatingText(int stars) {
        return switch (stars) {
            case 1 -> "Très difficile";
            case 2 -> "Difficile";
            case 3 -> "Moyen";
            case 4 -> "Facile";
            case 5 -> "Très facile";
            default -> "Non noté";
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    //  TOAST NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────

    private void showToastOnCard(VBox cardParent, String message) {
        if (cardParent == null || cardParent.getScene() == null) return;
        var win = cardParent.getScene().getWindow();

        Popup popup = new Popup();
        VBox container = new VBox(6);
        container.setStyle(
                "-fx-background-color:#1E293B;" +
                "-fx-border-color:#A78BFA;" +
                "-fx-border-width:1.5;" +
                "-fx-border-radius:12;" +
                "-fx-background-radius:12;" +
                "-fx-padding:12 18;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.5),18,0,0,4);");
        container.setMaxWidth(360);

        Label icon = new Label("🔔  Ton enseignant a été notifié !");
        icon.setStyle("-fx-text-fill:#A78BFA;-fx-font-size:12px;-fx-font-weight:700;");

        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");
        msg.setWrapText(true);

        container.getChildren().addAll(icon, msg);
        popup.getContent().add(container);
        popup.show(win,
                win.getX() + (win.getWidth()  - 360) / 2.0,
                win.getY() +  win.getHeight() - 110);

        FadeTransition fade = new FadeTransition(Duration.millis(500), container);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> popup.hide());
        new SequentialTransition(
                new PauseTransition(Duration.seconds(3)),
                fade
        ).play();
    }
}
