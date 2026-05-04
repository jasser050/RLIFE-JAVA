package com.studyflow.controllers;

import com.studyflow.models.Deck;
import com.studyflow.models.Flashcard;
import com.studyflow.models.Rating;
import com.studyflow.services.AIRatingAnalysisService;
import com.studyflow.services.AITranslationService;
import com.studyflow.services.DeckQRCodeService;
import com.studyflow.services.DeckService;
import com.studyflow.services.FlashcardService;
import com.studyflow.services.RatingService;
import com.studyflow.services.SpeechService;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class FlashcardsFeaturesController implements Initializable {

    // ── VIEW 1 — Deck list ────────────────────────────────────────────────
    @FXML protected VBox      listView;
    @FXML protected StackPane formHost;
    @FXML protected Label     totalDecksLabel;
    @FXML protected Label     masteredLabel;
    @FXML protected Label     dueReviewLabel;
    @FXML protected Label     streakLabel;
    @FXML protected Label     reviewedTodayLabel;
    @FXML protected Label     remainingTodayLabel;
    @FXML protected Label     accuracyLabel;
    @FXML protected HBox      deckStudyRow;
    @FXML protected VBox      decksColumn;
    @FXML protected FlowPane  decksGrid;
    @FXML protected VBox      dueDecksBox;
    @FXML protected VBox      quickStudyPanel;

    @FXML protected HBox             searchBoxDeck;
    @FXML protected TextField        searchDeckField;
    @FXML protected Label            searchDeckCounter;
    @FXML protected Button           clearDeckSearchBtn;
    @FXML protected ComboBox<String> sortDeckCombo;
    @FXML protected HBox             activeFiltersBar;
    @FXML protected HBox             filterChipsBox;
    @FXML protected Label            deckResultCountLabel;
    @FXML protected Button           clearAllFiltersBtn;
    @FXML protected VBox             emptySearchState;
    @FXML protected Label            emptySearchLabel;
    @FXML protected Button           resetSearchBtn;
    @FXML protected ToggleButton     gridViewBtn;
    @FXML protected ToggleButton     listViewBtn;
    @FXML protected Button           tabAll, tabInProgress, tabMastered;
    @FXML protected FontIcon         searchIconDeck;

    // ── VIEW 2 — Flashcard list ───────────────────────────────────────────
    @FXML protected VBox             flashcardsView;
    @FXML protected HBox             deckHeroPane;
    @FXML protected StackPane        deckIconBox;
    @FXML protected FontIcon         deckIconGlyph;
    @FXML protected Label            deckTitleLabel;
    @FXML protected Label            deckSubLabel;
    @FXML protected Label            fcTotalLabel;
    @FXML protected Label            fcMasteredLabel;
    @FXML protected Label            fcLearningLabel;
    @FXML protected Label            fcNewLabel;
    @FXML protected Label            fcCountLabel;
    @FXML protected HBox             searchBoxFc;
    @FXML protected TextField        searchFcField;
    @FXML protected ComboBox<String> searchScopeFc;
    @FXML protected Label            fcMatchCount;
    @FXML protected Button           clearFcSearchBtn;
    @FXML protected ComboBox<String> filterEtat;
    @FXML protected ComboBox<String> filterDifficulty;
    @FXML protected ComboBox<String> sortFcCombo;
    @FXML protected HBox             fcFiltersBar;
    @FXML protected HBox             fcChipsBox;
    @FXML protected Label            fcFilterResultCount;
    @FXML protected Button           clearFcFiltersBtn;
    @FXML protected FlowPane         flashcardsGrid;

    // ── VIEW 2B — Rating page ───────────────────────────────────────────
    @FXML protected VBox             ratingView;
    @FXML protected Label            ratingDeckTitleLabel;
    @FXML protected Label            ratingDeckMetaLabel;
    @FXML protected Label            selectedStarsLabel;
    @FXML protected Label            ratingStatusLabel;
    @FXML protected TextArea         ratingCommentArea;
    @FXML protected Button           ratingStarBtn1;
    @FXML protected Button           ratingStarBtn2;
    @FXML protected Button           ratingStarBtn3;
    @FXML protected Button           ratingStarBtn4;
    @FXML protected Button           ratingStarBtn5;

    // ── VIEW 3 — Form ─────────────────────────────────────────────────────
    @FXML protected VBox             formView;
    @FXML protected Label            formTitle;
    @FXML protected Label            formCompletionLabel;
    @FXML protected TextField        fieldTitre;
    @FXML protected TextArea         fieldQuestion;
    @FXML protected TextArea         fieldReponse;
    @FXML protected TextArea         fieldDescription;
    @FXML protected Label            titleCharCount;
    @FXML protected Label            questionCharCount;
    @FXML protected Label            answerCharCount;
    @FXML protected ComboBox<String> combDifficulte;
    @FXML protected ComboBox<String> combEtat;

    // ── Global state ──────────────────────────────────────────────────────
    protected final DeckService           deckService        = new DeckService();
    protected final FlashcardService      flashcardService   = new FlashcardService();
    protected final AITranslationService  translationService = new AITranslationService();
    protected final AIRatingAnalysisService aiRatingAnalysisService = new AIRatingAnalysisService();
    protected final SpeechService         speechService      = new SpeechService();
    protected final RatingService         ratingService      = new RatingService();
    protected List<Deck>      allDecks;
    protected Deck            currentDeck;
    protected Deck            ratingDeck;
    protected List<Flashcard> currentFlashcards;
    protected Flashcard       selectedFlashcard = null;
    protected int             currentUserId;
    protected int             selectedRatingStars = 0;

    private final Map<String, AITranslationService.TranslationResult> translationCache = new ConcurrentHashMap<>();
    private static final LinkedHashMap<String, String> SUPPORTED_LANGUAGES = new LinkedHashMap<>();
    static {
        SUPPORTED_LANGUAGES.put("en", "English");
        SUPPORTED_LANGUAGES.put("fr", "Francais");
        SUPPORTED_LANGUAGES.put("ar", "Arabic");
        SUPPORTED_LANGUAGES.put("es", "Spanish");
        SUPPORTED_LANGUAGES.put("de", "German");
    }

    private String  activeDeckTab  = "all";
    private boolean isDeckGridMode = true;
    private boolean formViewLoaded = false;
    private static final double DECK_CARD_WIDTH = 260;
    private static final double DECK_GRID_GAP = 16;
    private static final int DESKTOP_DECK_COLUMNS = 3;
    private static final double DECK_SCROLLBAR_WIDTH = 18;
    private double currentDeckCardWidth = DECK_CARD_WIDTH;

    private static final String[] COLORS = {"primary","success","warning","accent","danger"};
    private static final String[] ICONS  = {
            "fth-database","fth-trending-up","fth-terminal",
            "fth-grid","fth-wifi","fth-cpu","fth-hard-drive","fth-check-square"
    };

    // ═════════════════════════════════════════════════════════════════════
    //  INIT
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (formViewLoaded) return;

        currentUserId = UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getId() : 1;

        loadAddEditFormView();

        if (combDifficulte != null)
            combDifficulte.setItems(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        if (combEtat != null)
            combEtat.setItems(FXCollections.observableArrayList("new", "learning", "mastered"));
        if (filterEtat != null) {
            filterEtat.setItems(FXCollections.observableArrayList("All Status", "new", "learning", "mastered"));
            filterEtat.setValue("All Status");
        }
        if (filterDifficulty != null) {
            filterDifficulty.setItems(FXCollections.observableArrayList("All Levels", "Easy", "Medium", "Hard"));
            filterDifficulty.setValue("All Levels");
        }
        if (searchScopeFc != null) {
            searchScopeFc.setItems(FXCollections.observableArrayList(
                    "All fields", "Title only", "Question", "Answer"));
            searchScopeFc.setValue("All fields");
        }
        if (sortFcCombo != null) {
            sortFcCombo.setItems(FXCollections.observableArrayList(
                    "Default", "Title A-Z", "Title Z-A",
                    "Easy first", "Hard first", "New first", "Mastered first"));
            sortFcCombo.setValue("Default");
        }
        if (sortDeckCombo != null) {
            sortDeckCombo.setItems(FXCollections.observableArrayList(
                    "Newest", "Oldest", "Title A-Z", "Title Z-A", "Subject A-Z", "Most cards"));
            sortDeckCombo.setValue("Newest");
        }

        styleTextArea(fieldQuestion);
        styleTextArea(fieldReponse);
        styleTextArea(fieldDescription);

        setupDeckSearch();
        setupFlashcardSearch();
        setupFormCharCounters();
        setupDeckTabs();
        setupViewModeToggles();
        setupRatingView();
        goToList();
        refreshDeckList();
    }

    private void loadAddEditFormView() {
        if (formHost == null || formViewLoaded) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studyflow/views/FlashcardsAddEdit.fxml"));
            loader.setController(this);
            formViewLoaded = true;
            VBox loadedForm = loader.load();
            formHost.getChildren().setAll(loadedForm);
            formView = loadedForm;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FlashcardsAddEdit.fxml", e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ① DECK SEARCH
    // ═════════════════════════════════════════════════════════════════════

    private void setupDeckSearch() {
        if (searchDeckField == null) return;
        if (searchBoxDeck != null) {
            searchBoxDeck.setOnMouseClicked(e -> searchDeckField.requestFocus());
        }
        searchDeckField.textProperty().addListener((obs, oldV, newV) -> onDeckSearchChanged(newV));
        searchDeckField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (searchBoxDeck == null) return;
            if (isFocused) {
                searchBoxDeck.setStyle(
                        "-fx-background-color:#0F172A;-fx-border-color:#7C3AED;" +
                                "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:0 14 0 12;" +
                                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.35),12,0,0,0);");
                if (searchIconDeck != null) searchIconDeck.setIconColor(Color.web("#A78BFA"));
            } else {
                searchBoxDeck.setStyle(
                        "-fx-background-color:#0F172A;-fx-border-color:#334155;" +
                                "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:0 14 0 12;" +
                                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),8,0,0,2);");
                if (searchIconDeck != null) searchIconDeck.setIconColor(Color.web("#475569"));
            }
        });
        if (clearDeckSearchBtn != null) clearDeckSearchBtn.setOnAction(e -> searchDeckField.clear());
        if (sortDeckCombo != null) sortDeckCombo.valueProperty().addListener((obs, o, n) -> applyDeckFilters());
        if (clearAllFiltersBtn != null) clearAllFiltersBtn.setOnAction(e -> resetAllDeckFilters());
        if (resetSearchBtn != null) resetSearchBtn.setOnAction(e -> resetAllDeckFilters());
    }

    private void onDeckSearchChanged(String text) {
        boolean hasText = text != null && !text.isBlank();
        if (searchDeckCounter != null)
            searchDeckCounter.setText(hasText ? text.trim().length() + " chars" : "");
        if (clearDeckSearchBtn != null) {
            clearDeckSearchBtn.setVisible(hasText);
            clearDeckSearchBtn.setManaged(hasText);
        }
        applyDeckFilters();
    }

    private void resetAllDeckFilters() {
        if (searchDeckField != null) searchDeckField.clear();
        activeDeckTab = "all";
        updateTabStyles();
        applyDeckFilters();
    }

    // ─── Deck tabs ────────────────────────────────────────────────────────
    private void setupDeckTabs() {
        wireTab(tabAll,        "all");
        wireTab(tabInProgress, "inprogress");
        wireTab(tabMastered,   "mastered");
    }

    private void wireTab(Button btn, String tabId) {
        if (btn == null) return;
        btn.setOnAction(e -> { activeDeckTab = tabId; updateTabStyles(); applyDeckFilters(); });
    }

    private void updateTabStyles() {
        styleTab(tabAll,        "all");
        styleTab(tabInProgress, "inprogress");
        styleTab(tabMastered,   "mastered");
    }

    private void styleTab(Button btn, String tabId) {
        if (btn == null) return;
        if (tabId.equals(activeDeckTab)) btn.getStyleClass().setAll("filter-tab", "active");
        else                             btn.getStyleClass().setAll("filter-tab");
    }

    // ─── View mode toggles ────────────────────────────────────────────────
    private void setupViewModeToggles() {
        if (gridViewBtn != null) {
            gridViewBtn.setSelected(true);
            updateViewModeStyles();
            gridViewBtn.setOnAction(e -> { isDeckGridMode = true;  updateViewModeStyles(); applyDeckFilters(); });
        }
        if (listViewBtn != null)
            listViewBtn.setOnAction(e -> { isDeckGridMode = false; updateViewModeStyles(); applyDeckFilters(); });
    }

    private void setupRatingView() {
        updateRatingSelection(0);
        if (ratingCommentArea != null) {
            ratingCommentArea.setStyle("-fx-control-inner-background:#1E293B;-fx-background-color:#1E293B;" +
                    "-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                    "-fx-border-color:#334155;-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;");
        }
        clearRatingViewState();
    }

    private void updateViewModeStyles() {
        styleViewBtn(gridViewBtn,  isDeckGridMode);
        styleViewBtn(listViewBtn, !isDeckGridMode);
        updateDeckLayout();
    }

    private void updateDeckLayout() {
        if (decksColumn == null || decksGrid == null) {
            return;
        }

        currentDeckCardWidth = DECK_CARD_WIDTH;
        double threeCardsWidth = (DECK_CARD_WIDTH * DESKTOP_DECK_COLUMNS)
                + (DECK_GRID_GAP * (DESKTOP_DECK_COLUMNS - 1));
        double decksViewportWidth = threeCardsWidth + DECK_SCROLLBAR_WIDTH;

        if (isDeckGridMode) {
            decksColumn.setPrefWidth(decksViewportWidth);
            decksColumn.setMinWidth(decksViewportWidth);
            decksColumn.setMaxWidth(decksViewportWidth);
            HBox.setHgrow(decksColumn, Priority.NEVER);
            decksGrid.setPrefWrapLength(threeCardsWidth);
            decksGrid.setMaxWidth(threeCardsWidth);
        } else {
            decksColumn.setPrefWidth(Region.USE_COMPUTED_SIZE);
            decksColumn.setMinWidth(Region.USE_COMPUTED_SIZE);
            decksColumn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(decksColumn, Priority.ALWAYS);
            decksGrid.setPrefWrapLength(0);
            decksGrid.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void styleViewBtn(ToggleButton btn, boolean active) {
        if (btn == null) return;
        String color = active ? "#A78BFA" : "#64748B";
        String bg    = active ? "rgba(124,58,237,0.2)" : "transparent";
        btn.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:8;-fx-cursor:hand;-fx-padding:5 9;");
        if (btn.getGraphic() instanceof FontIcon fi) fi.setIconColor(Color.web(color));
    }

    // ─── Apply deck filters ───────────────────────────────────────────────
    private void applyDeckFilters() {
        if (allDecks == null) return;
        String query = searchDeckField == null ? "" :
                safe(searchDeckField.getText()).trim().toLowerCase(Locale.ROOT);
        Stream<Deck> stream = allDecks.stream();
        if (!query.isBlank())
            stream = stream.filter(d -> deckSearchText(d).contains(query));
        if ("inprogress".equals(activeDeckTab))
            stream = stream.filter(d ->
                    flashcardService.getByDeck(d.getIdDeck()).stream()
                            .anyMatch(fc -> "learning".equals(fc.getEtat())));
        else if ("mastered".equals(activeDeckTab))
            stream = stream.filter(d -> {
                List<Flashcard> cards = flashcardService.getByDeck(d.getIdDeck());
                return !cards.isEmpty() && cards.stream().allMatch(fc -> "mastered".equals(fc.getEtat()));
            });
        List<Deck> filtered = stream.sorted(getDeckComparator()).toList();
        updateDeckFilterChips(query);
        if (deckResultCountLabel != null)
            deckResultCountLabel.setText(filtered.size() + " result" + (filtered.size() != 1 ? "s" : ""));
        boolean isEmpty    = filtered.isEmpty();
        boolean hasFilters = !query.isBlank() || !"all".equals(activeDeckTab);
        if (emptySearchState != null) {
            emptySearchState.setVisible(isEmpty);
            emptySearchState.setManaged(isEmpty);
            if (isEmpty && emptySearchLabel != null)
                emptySearchLabel.setText(hasFilters ? "No decks match your filters" : "No decks available yet");
        }
        displayDecks(filtered);
    }

    private void updateDeckFilterChips(String query) {
        if (filterChipsBox == null || activeFiltersBar == null) return;
        filterChipsBox.getChildren().clear();
        boolean hasAny = false;
        if (!query.isBlank()) {
            filterChipsBox.getChildren().add(makeChip("Search \"" + query + "\"",
                    e -> searchDeckField.clear()));
            hasAny = true;
        }
        if (!"all".equals(activeDeckTab)) {
            String tabLabel = switch (activeDeckTab) {
                case "inprogress" -> "In Progress";
                case "mastered"   -> "Mastered";
                default           -> activeDeckTab;
            };
            filterChipsBox.getChildren().add(makeChip("🏷 " + tabLabel,
                    e -> { activeDeckTab = "all"; updateTabStyles(); applyDeckFilters(); }));
            hasAny = true;
        }
        activeFiltersBar.setVisible(hasAny);
        activeFiltersBar.setManaged(hasAny);
    }

    private HBox makeChip(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onRemove) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color:rgba(124,58,237,0.15);-fx-background-radius:20;-fx-padding:3 6 3 10;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill:#A78BFA;-fx-font-size:10px;-fx-font-weight:700;");
        Button rm = new Button("×");
        rm.setStyle("-fx-background-color:rgba(167,139,250,0.25);-fx-text-fill:#A78BFA;" +
                "-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:100;-fx-cursor:hand;-fx-padding:1 5;");
        rm.setOnAction(onRemove);
        chip.getChildren().addAll(lbl, rm);
        return chip;
    }

    private Comparator<Deck> getDeckComparator() {
        String v = sortDeckCombo == null ? "Newest" : safe(sortDeckCombo.getValue());
        Comparator<Deck> byDate    = Comparator.comparing(Deck::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Deck> byTitle   = Comparator.comparing(d -> safe(d.getTitre()).toLowerCase(Locale.ROOT));
        Comparator<Deck> bySubject = Comparator.comparing(d -> safe(d.getMatiere()).toLowerCase(Locale.ROOT));
        return switch (v) {
            case "Oldest"      -> byDate.thenComparingInt(Deck::getIdDeck);
            case "Title A-Z"   -> byTitle.thenComparingInt(Deck::getIdDeck);
            case "Title Z-A"   -> byTitle.reversed().thenComparingInt(Deck::getIdDeck);
            case "Subject A-Z" -> bySubject.thenComparingInt(Deck::getIdDeck);
            case "Most cards"  -> Comparator.comparingInt((Deck d) -> flashcardService.getByDeck(d.getIdDeck()).size()).reversed();
            default            -> byDate.reversed().thenComparingInt(Deck::getIdDeck);
        };
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ② FLASHCARD SEARCH
    // ═════════════════════════════════════════════════════════════════════

    private void setupFlashcardSearch() {
        if (searchFcField != null) {
            if (searchBoxFc != null) {
                searchBoxFc.setOnMouseClicked(e -> searchFcField.requestFocus());
            }
            searchFcField.textProperty().addListener((obs, o, n) -> onFcSearchChanged(n));
            searchFcField.focusedProperty().addListener((obs, w, isFocused) -> {
                if (searchBoxFc == null) return;
                String border = isFocused ? "#7C3AED" : "#1E293B";
                String glow   = isFocused
                        ? "dropshadow(gaussian,rgba(124,58,237,0.35),12,0,0,0)"
                        : "dropshadow(gaussian,rgba(0,0,0,0.2),6,0,0,2)";
                searchBoxFc.setStyle("-fx-background-color:#0F172A;-fx-border-color:" + border +
                        ";-fx-border-radius:14;-fx-background-radius:14;-fx-effect:" + glow + ";");
            });
        }
        if (clearFcSearchBtn  != null) clearFcSearchBtn.setOnAction(e -> searchFcField.clear());
        if (searchScopeFc     != null) searchScopeFc.valueProperty().addListener((obs, o, n) -> refreshFlashcards());
        if (filterEtat        != null) filterEtat.valueProperty().addListener((obs, o, n) -> refreshFlashcards());
        if (filterDifficulty  != null) filterDifficulty.valueProperty().addListener((obs, o, n) -> refreshFlashcards());
        if (sortFcCombo       != null) sortFcCombo.valueProperty().addListener((obs, o, n) -> refreshFlashcards());
        if (clearFcFiltersBtn != null) clearFcFiltersBtn.setOnAction(e -> resetFcFilters());
    }

    private void onFcSearchChanged(String text) {
        boolean hasText = text != null && !text.isBlank();
        if (clearFcSearchBtn != null) { clearFcSearchBtn.setVisible(hasText); clearFcSearchBtn.setManaged(hasText); }
        refreshFlashcards();
    }

    protected void resetFcFilters() {
        if (searchFcField    != null) searchFcField.clear();
        if (filterEtat       != null) filterEtat.setValue("All Status");
        if (filterDifficulty != null) filterDifficulty.setValue("All Levels");
        if (sortFcCombo      != null) sortFcCombo.setValue("Default");
    }

    protected void refreshFlashcards() {
        if (currentDeck == null) return;
        String kw    = searchFcField    != null ? searchFcField.getText().trim()    : "";
        String scope = searchScopeFc    != null ? safe(searchScopeFc.getValue())    : "All fields";
        String etat  = filterEtat       != null ? safe(filterEtat.getValue())       : "All Status";
        String diff  = filterDifficulty != null ? safe(filterDifficulty.getValue()) : "All Levels";

        List<Flashcard> base = flashcardService.getByDeck(currentDeck.getIdDeck());
        Stream<Flashcard> stream = base.stream();

        if (!kw.isBlank()) {
            String kwLow = kw.toLowerCase(Locale.ROOT);
            stream = stream.filter(fc -> switch (scope) {
                case "Title only" -> safe(fc.getTitre()).toLowerCase().contains(kwLow);
                case "Question"   -> safe(fc.getQuestion()).toLowerCase().contains(kwLow);
                case "Answer"     -> safe(fc.getReponse()).toLowerCase().contains(kwLow);
                default           -> (safe(fc.getTitre()) + " " + safe(fc.getQuestion())
                        + " " + safe(fc.getReponse()) + " " + safe(fc.getDescription()))
                        .toLowerCase().contains(kwLow);
            });
        }
        if (!"All Status".equals(etat))
            stream = stream.filter(fc -> etat.equals(fc.getEtat()));
        if (!"All Levels".equals(diff)) {
            int diffNum = switch (diff) { case "Medium" -> 2; case "Hard" -> 3; default -> 1; };
            stream = stream.filter(fc -> fc.getNiveauDifficulte() == diffNum);
        }
        stream = sortFlashcards(stream, safe(sortFcCombo != null ? sortFcCombo.getValue() : "Default"));
        currentFlashcards = stream.toList();

        int total    = currentFlashcards.size();
        int mastered = (int) currentFlashcards.stream().filter(fc -> "mastered".equals(fc.getEtat())).count();
        int learning = (int) currentFlashcards.stream().filter(fc -> "learning".equals(fc.getEtat())).count();
        int newCards = (int) currentFlashcards.stream().filter(fc -> "new".equals(fc.getEtat())).count();

        if (fcCountLabel    != null) fcCountLabel.setText(total + " cards");
        if (fcTotalLabel    != null) fcTotalLabel   .setText(String.valueOf(total));
        if (fcMasteredLabel != null) fcMasteredLabel.setText(String.valueOf(mastered));
        if (fcLearningLabel != null) fcLearningLabel.setText(String.valueOf(learning));
        if (fcNewLabel      != null) fcNewLabel     .setText(String.valueOf(newCards));
        if (fcMatchCount != null)
            fcMatchCount.setText(!kw.isBlank() ? total + " match" + (total != 1 ? "es" : "") : "");

        updateFcFilterChips(kw, etat, diff);
        displayFlashcards(currentFlashcards);
    }

    private Stream<Flashcard> sortFlashcards(Stream<Flashcard> stream, String sortVal) {
        Comparator<Flashcard> byTitle = Comparator.comparing(fc -> safe(fc.getTitre()).toLowerCase());
        return switch (sortVal) {
            case "Title A-Z"      -> stream.sorted(byTitle);
            case "Title Z-A"      -> stream.sorted(byTitle.reversed());
            case "Easy first"     -> stream.sorted(Comparator.comparingInt(Flashcard::getNiveauDifficulte));
            case "Hard first"     -> stream.sorted(Comparator.comparingInt(Flashcard::getNiveauDifficulte).reversed());
            case "New first"      -> stream.sorted(Comparator.comparing(fc ->
                    switch (fc.getEtat()) { case "new" -> 0; case "learning" -> 1; default -> 2; }));
            case "Mastered first" -> stream.sorted(Comparator.comparing(fc ->
                    switch (fc.getEtat()) { case "mastered" -> 0; case "learning" -> 1; default -> 2; }));
            default               -> stream;
        };
    }

    private void updateFcFilterChips(String kw, String etat, String diff) {
        if (fcChipsBox == null || fcFiltersBar == null) return;
        fcChipsBox.getChildren().clear();
        boolean hasAny = false;
        if (!kw.isBlank())              { fcChipsBox.getChildren().add(makeChip("Search \"" + kw + "\"", e -> searchFcField.clear())); hasAny = true; }
        if (!"All Status".equals(etat)) { fcChipsBox.getChildren().add(makeChip("🏷 " + etat,  e -> filterEtat.setValue("All Status"))); hasAny = true; }
        if (!"All Levels".equals(diff)) { fcChipsBox.getChildren().add(makeChip("⚡ " + diff, e -> filterDifficulty.setValue("All Levels"))); hasAny = true; }
        if (fcFilterResultCount != null)
            fcFilterResultCount.setText(currentFlashcards != null
                    ? currentFlashcards.size() + " result" + (currentFlashcards.size() != 1 ? "s" : "") : "");
        fcFiltersBar.setVisible(hasAny);
        fcFiltersBar.setManaged(hasAny);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ③ FORM CHAR COUNTERS
    // ═════════════════════════════════════════════════════════════════════

    private void setupFormCharCounters() {
        wireCounter(fieldTitre,    titleCharCount,    80);
        wireCounter(fieldQuestion, questionCharCount, 500);
        wireCounter(fieldReponse,  answerCharCount,   500);
        if (fieldTitre    != null) fieldTitre.textProperty().addListener((o, ov, nv) -> updateFormCompletion());
        if (fieldQuestion != null) fieldQuestion.textProperty().addListener((o, ov, nv) -> updateFormCompletion());
        if (fieldReponse  != null) fieldReponse.textProperty().addListener((o, ov, nv) -> updateFormCompletion());
        if (combDifficulte != null) combDifficulte.valueProperty().addListener((o, ov, nv) -> updateFormCompletion());
        if (combEtat != null) combEtat.valueProperty().addListener((o, ov, nv) -> updateFormCompletion());
    }

    private void wireCounter(javafx.scene.control.TextInputControl field, Label counter, int max) {
        if (field == null || counter == null) return;
        field.textProperty().addListener((obs, o, n) -> {
            int len = n == null ? 0 : n.length();
            int pct = (int)((len * 100.0) / max);
            counter.setText(len + "/" + max);
            String color = pct >= 100 ? "#FB7185" : pct >= 80 ? "#FBBF24" : "#334155";
            counter.setStyle("-fx-text-fill:" + color + ";-fx-font-size:10px;");
        });
    }

    private void updateFormCompletion() {
        int filled = 0;
        if (fieldTitre     != null && !fieldTitre.getText().isBlank())   filled++;
        if (fieldQuestion  != null && !fieldQuestion.getText().isBlank()) filled++;
        if (fieldReponse   != null && !fieldReponse.getText().isBlank())  filled++;
        if (combDifficulte != null && combDifficulte.getValue() != null)  filled++;
        if (combEtat       != null && combEtat.getValue() != null)         filled++;
        int pct = (filled * 100) / 5;
        if (formCompletionLabel != null) {
            formCompletionLabel.setText(pct + "% complete");
            String color = pct == 100 ? "#34D399" : pct >= 60 ? "#FBBF24" : "#64748B";
            formCompletionLabel.setStyle("-fx-background-color:#1E293B;-fx-text-fill:" + color +
                    ";-fx-font-size:11px;-fx-font-weight:700;-fx-padding:6 14;-fx-background-radius:20;");
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═════════════════════════════════════════════════════════════════════

    private void goToList() {
        show(listView);
        hide(flashcardsView);
        hide(ratingView);
        hide(formView);
        if (formHost != null) { formHost.setVisible(false); formHost.setManaged(false); }
    }

    protected void goToFlashcards() {
        hide(listView);
        show(flashcardsView);
        hide(ratingView);
        hide(formView);
        if (formHost != null) { formHost.setVisible(false); formHost.setManaged(false); }
    }

    protected void goToForm() {
        hide(listView);
        hide(flashcardsView);
        hide(ratingView);
        show(formView);
        if (formHost != null) { formHost.setVisible(true); formHost.setManaged(true); }
    }

    protected void goToRatingView() {
        hide(listView);
        hide(flashcardsView);
        hide(formView);
        show(ratingView);
        if (formHost != null) { formHost.setVisible(false); formHost.setManaged(false); }
    }

    private void show(VBox v) { if (v != null) { v.setVisible(true);  v.setManaged(true);  } }
    private void hide(VBox v) { if (v != null) { v.setVisible(false); v.setManaged(false); } }

    // ═════════════════════════════════════════════════════════════════════
    //  VIEW 1 — DECK LIST
    // ═════════════════════════════════════════════════════════════════════

    @FXML public void goBackToList() {
        currentDeck = null;
        goToList();
        refreshDeckList();
    }

    @FXML
    protected void openDeck() {
        Deck deck = resolveActionDeck();
        if (deck == null) {
            showInfo("No deck is available yet.");
            return;
        }
        openDeck(deck);
    }

    @FXML
    protected void openQrCode() {
        Deck deck = resolveActionDeck();
        if (deck == null) {
            showInfo("No deck is available yet.");
            return;
        }
        Thread qrThread = new Thread(
                () -> DeckQRCodeService.showQRCode(deck, flashcardService),
                "qr-gen-" + deck.getIdDeck()
        );
        qrThread.setDaemon(true);
        qrThread.start();
    }

    @FXML
    protected void openRatingView() {
        Deck deck = resolveActionDeck();
        if (deck == null) {
            showInfo("No deck is available yet.");
            return;
        }
        openRatingPage(deck);
    }

    private void refreshDeckList() {
        allDecks = deckService.getAll();
        int mastered = flashcardService.countByEtat(currentUserId, "mastered");
        int total    = allDecks.size();
        int learning = flashcardService.countByEtat(currentUserId, "learning");
        int newCards = flashcardService.countByEtat(currentUserId, "new");

        if (totalDecksLabel     != null) totalDecksLabel    .setText(String.valueOf(total));
        if (masteredLabel       != null) masteredLabel       .setText(String.valueOf(mastered));
        if (dueReviewLabel      != null) dueReviewLabel      .setText(String.valueOf(learning + newCards));
        if (streakLabel         != null) streakLabel         .setText("0");
        if (reviewedTodayLabel  != null) reviewedTodayLabel  .setText(String.valueOf(mastered));
        if (remainingTodayLabel != null) remainingTodayLabel .setText(String.valueOf(newCards + learning));
        if (accuracyLabel != null) {
            int reviewed = mastered + learning;
            accuracyLabel.setText(reviewed > 0 ? (int)((mastered * 100.0) / reviewed) + "%" : "0%");
        }
        applyDeckFilters();
        displayDueDecks();
    }

    private Deck resolveActionDeck() {
        if (currentDeck != null) {
            return currentDeck;
        }
        if (allDecks == null || allDecks.isEmpty()) {
            refreshDeckList();
        }
        return (allDecks == null || allDecks.isEmpty()) ? null : allDecks.get(0);
    }

    private String deckSearchText(Deck deck) {
        return String.join(" ",
                safe(deck.getTitre()), safe(deck.getMatiere()), safe(deck.getNiveau()),
                safe(deck.getDescription()), String.valueOf(deck.getIdDeck()),
                deck.getDateCreation() == null ? "" : deck.getDateCreation().toString()
        ).toLowerCase(Locale.ROOT);
    }

    private void displayDueDecks() {
        if (dueDecksBox == null) return;
        dueDecksBox.getChildren().clear();
        int i = 0;
        for (Deck deck : allDecks.stream().limit(4).toList()) {
            String color = COLORS[i % COLORS.length];
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(10, 12, 10, 12));
            String sN = "-fx-background-color:#1E293B;-fx-background-radius:10;-fx-cursor:hand;";
            String sH = "-fx-background-color:#334155;-fx-background-radius:10;-fx-cursor:hand;";
            item.setStyle(sN);
            item.setOnMouseEntered(e -> item.setStyle(sH));
            item.setOnMouseExited (e -> item.setStyle(sN));
            Region colorBar = new Region();
            colorBar.setPrefWidth(4); colorBar.setPrefHeight(32);
            colorBar.setStyle("-fx-background-color:" + hex(color) + ";-fx-background-radius:2;");
            VBox txt = new VBox(2);
            HBox.setHgrow(txt, Priority.ALWAYS);
            Label n = new Label(deck.getTitre());
            n.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:12px;-fx-font-weight:600;");
            n.setMaxWidth(150);
            Label s = new Label(deck.getMatiere());
            s.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
            txt.getChildren().addAll(n, s);
            StackPane play = new StackPane();
            play.setPrefSize(28, 28);
            play.setStyle("-fx-background-color:rgba(139,92,246,0.2);-fx-background-radius:100;");
            FontIcon pi = new FontIcon("fth-play");
            pi.setIconSize(12); pi.setIconColor(Color.web(hex(color)));
            play.getChildren().add(pi);
            play.setOnMouseClicked(e -> openDeck(deck));
            item.getChildren().addAll(colorBar, txt, play);
            dueDecksBox.getChildren().add(item);
            i++;
        }
    }

    private void displayDecks(List<Deck> list) {
        decksGrid.getChildren().clear();
        if (list.isEmpty()) return;
        if (isDeckGridMode) {
            decksGrid.setHgap(16); decksGrid.setVgap(16);
            for (int i = 0; i < list.size(); i++)
                decksGrid.getChildren().add(buildDeckCard(list.get(i), COLORS[i % COLORS.length], ICONS[i % ICONS.length]));
        } else {
            decksGrid.setHgap(0); decksGrid.setVgap(0);
            VBox listContainer = new VBox(8);
            listContainer.setFillWidth(true);
            listContainer.prefWidthProperty().bind(decksGrid.widthProperty().subtract(4));
            for (int i = 0; i < list.size(); i++) {
                HBox row = buildDeckListRow(list.get(i), COLORS[i % COLORS.length]);
                row.prefWidthProperty().bind(listContainer.widthProperty());
                listContainer.getChildren().add(row);
            }
            decksGrid.getChildren().add(listContainer);
        }
    }

    // ─── List row ─────────────────────────────────────────────────────────
    private HBox buildDeckListRow(Deck deck, String color) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setPrefWidth(Double.MAX_VALUE);
        String sN = "-fx-background-color:#0F172A;-fx-background-radius:12;-fx-cursor:hand;";
        String sH = "-fx-background-color:#1E293B;-fx-background-radius:12;-fx-cursor:hand;";
        row.setStyle(sN);
        row.setOnMouseEntered(e -> row.setStyle(sH));
        row.setOnMouseExited (e -> row.setStyle(sN));
        Region dot = new Region();
        dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color:" + hex(color) + ";-fx-background-radius:100;");
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(deck.getTitre());
        title.setStyle("-fx-text-fill:#F8FAFC;-fx-font-weight:700;-fx-font-size:13px;");
        Label sub = new Label(deck.getMatiere() + " • " + deck.getNiveau());
        sub.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        info.getChildren().addAll(title, sub);
        int count = flashcardService.getByDeck(deck.getIdDeck()).size();
        Label badge = new Label(count + " cards");
        badge.setStyle("-fx-background-color:rgba(139,92,246,0.12);-fx-text-fill:#A78BFA;" +
                "-fx-font-size:10px;-fx-font-weight:700;-fx-padding:3 8;-fx-background-radius:20;");
        Button ratingBtn = chipBtn("⭐ Rating", "rgba(236,72,153,0.15)", "#F472B6", "rgba(236,72,153,0.28)");
        ratingBtn.setOnAction(e -> openRatingPage(deck));

        // ── QR Code button ────────────────────────────────────────────────
        Button qrBtn = chipBtn("📱 QR", "rgba(56,189,248,0.15)", "#38BDF8", "rgba(56,189,248,0.28)");
        qrBtn.setOnAction(e -> launchQRCode(deck));
        // ─────────────────────────────────────────────────────────────────

        Button open = new Button("Open →");
        open.setStyle("-fx-background-color:rgba(124,58,237,0.15);-fx-text-fill:#A78BFA;" +
                "-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;");
        open.setOnAction(e -> openDeck(deck));
        row.getChildren().addAll(dot, info, badge, ratingBtn, qrBtn, open);
        row.setOnMouseClicked(e -> {
            Node target = e.getTarget() instanceof Node node ? node : null;
            if (!isInsideButton(target, open)
                    && !isInsideButton(target, ratingBtn)
                    && !isInsideButton(target, qrBtn)) {
                openDeck(deck);
            }
        });
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  DECK CARD
    // ═════════════════════════════════════════════════════════════════════

    private VBox buildDeckCard(Deck deck, String color, String iconLit) {
        VBox card = new VBox(0);
        card.setPrefWidth(currentDeckCardWidth);
        card.setMinWidth(currentDeckCardWidth);
        card.setMaxWidth(currentDeckCardWidth);
        String sN = "-fx-background-color:#0F172A;-fx-background-radius:16;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),12,0,0,4);";
        String sH = "-fx-background-color:#1E293B;-fx-background-radius:16;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),20,0,0,8);";
        card.setStyle(sN);
        card.setOnMouseEntered(e -> card.setStyle(sH));
        card.setOnMouseExited (e -> card.setStyle(sN));

        StackPane header = buildDeckHeader(deck, color, iconLit);

        VBox body = new VBox(6);
        body.setPadding(new Insets(14));

        Label name = new Label(deck.getTitre());
        name.setStyle("-fx-text-fill:#F8FAFC;-fx-font-weight:700;-fx-font-size:13px;");
        name.setWrapText(true);

        Label sub = new Label(deck.getMatiere() + " • " + deck.getNiveau());
        sub.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");

        int count = flashcardService.getByDeck(deck.getIdDeck()).size();
        Label countBadge = new Label(count + " card" + (count != 1 ? "s" : ""));
        countBadge.setStyle("-fx-background-color:rgba(139,92,246,0.15);-fx-text-fill:#A78BFA;" +
                "-fx-font-size:10px;-fx-font-weight:700;-fx-padding:3 8;-fx-background-radius:20;");

        body.getChildren().addAll(name, sub, countBadge);

        if (deck.getDescription() != null && !deck.getDescription().isEmpty()) {
            Label desc = new Label(deck.getDescription());
            desc.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");
            desc.setWrapText(true);
            desc.setMaxHeight(30);
            body.getChildren().add(desc);
        }

        Button ratingBtn = new Button("⭐ Rating");
        ratingBtn.setStyle("-fx-background-color:rgba(236,72,153,0.15);-fx-text-fill:#F472B6;" +
                "-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:8;" +
                "-fx-cursor:hand;-fx-padding:7 14;-fx-max-width:Infinity;");
        ratingBtn.setMaxWidth(Double.MAX_VALUE);
        ratingBtn.setOnAction(e -> openRatingPage(deck));

        // ── QR Code button ────────────────────────────────────────────────
        Button qrBtn = new Button("📱 QR Code");
        qrBtn.setStyle("-fx-background-color:rgba(56,189,248,0.15);-fx-text-fill:#38BDF8;" +
                "-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:8;" +
                "-fx-cursor:hand;-fx-padding:7 14;-fx-max-width:Infinity;");
        qrBtn.setMaxWidth(Double.MAX_VALUE);
        qrBtn.setOnAction(e -> launchQRCode(deck));
        // ─────────────────────────────────────────────────────────────────

        Button open = new Button("Open Deck →");
        open.setStyle("-fx-background-color:" + grad(color) + ";-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:8;" +
                "-fx-cursor:hand;-fx-padding:7 14;-fx-max-width:Infinity;");
        open.setMaxWidth(Double.MAX_VALUE);
        open.setOnAction(e -> openDeck(deck));

        VBox btnBox = new VBox(8, ratingBtn, qrBtn, open);
        btnBox.setPadding(new Insets(0, 14, 14, 14));

        card.getChildren().addAll(header, body, btnBox);
        card.setOnMouseClicked(e -> {
            Node target = e.getTarget() instanceof Node node ? node : null;
            if (!isInsideButton(target, open)
                    && !isInsideButton(target, ratingBtn)
                    && !isInsideButton(target, qrBtn)) {
                openDeck(deck);
            }
        });
        return card;
    }

    // ── QR launcher (daemon thread so UI stays responsive) ───────────────
    private void launchQRCode(Deck deck) {
        Thread t = new Thread(
                () -> DeckQRCodeService.showQRCode(deck, flashcardService),
                "qr-gen-" + deck.getIdDeck()
        );
        t.setDaemon(true);
        t.start();
    }

    private StackPane buildDeckHeader(Deck deck, String color, String iconLit) {
        StackPane header = new StackPane();
        header.setPrefHeight(110);
        header.setStyle("-fx-background-color:" + grad(color) + ";-fx-background-radius:16 16 0 0;");
        double cardWidth = currentDeckCardWidth;
        String imagePath = deck.getImage();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try {
                String raw = imagePath.trim();
                Image img;
                if (raw.startsWith("http://") || raw.startsWith("https://")) {
                    img = new Image(raw, cardWidth, 110, true, true, true);
                } else {
                    File f = new File(raw);
                    if (!f.exists() && !raw.startsWith("/")) f = new File(System.getProperty("user.dir"), raw);
                    img = f.exists() ? new Image(f.toURI().toString(), cardWidth, 110, true, true, true) : null;
                }
                if (img != null && !img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(cardWidth); iv.setFitHeight(110); iv.setPreserveRatio(false); iv.setSmooth(true);
                    header.getChildren().add(iv);
                    return header;
                }
            } catch (Exception ignored) {}
        }
        FontIcon ico = new FontIcon(iconLit);
        ico.setIconSize(30); ico.setIconColor(Color.WHITE);
        header.getChildren().add(ico);
        return header;
    }

    protected void openRatingPage(Deck deck) {
        ratingDeck = deck;
        Rating existing = ratingService.getRatingByUserAndDeck(currentUserId, deck.getIdDeck());

        if (ratingDeckTitleLabel != null) {
            ratingDeckTitleLabel.setText(deck.getTitre());
        }
        if (ratingDeckMetaLabel != null) {
            ratingDeckMetaLabel.setText(deck.getMatiere() + " • " + deck.getNiveau());
        }
        if (ratingCommentArea != null) {
            ratingCommentArea.setText(existing != null && existing.getComment() != null ? existing.getComment() : "");
        }
        if (ratingStatusLabel != null) {
            ratingStatusLabel.setText(existing == null
                    ? "Select a rating from 1 to 5 and submit."
                    : "Update your rating for this deck.");
            ratingStatusLabel.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        }

        updateRatingSelection(existing != null ? existing.getStars() : 0);
        goToRatingView();
    }

    @FXML
    protected void handleRatingBack() {
        clearRatingViewState();
        goToList();
        refreshDeckList();
    }

    @FXML protected void selectRating1() { updateRatingSelection(1); }
    @FXML protected void selectRating2() { updateRatingSelection(2); }
    @FXML protected void selectRating3() { updateRatingSelection(3); }
    @FXML protected void selectRating4() { updateRatingSelection(4); }
    @FXML protected void selectRating5() { updateRatingSelection(5); }

    @FXML
    protected void submitDeckRating() {
        if (ratingDeck == null) {
            return;
        }
        if (selectedRatingStars < 1 || selectedRatingStars > 5) {
            if (ratingStatusLabel != null) {
                ratingStatusLabel.setText("Please choose a rating between 1 and 5.");
                ratingStatusLabel.setStyle("-fx-text-fill:#FB7185;-fx-font-size:11px;-fx-font-weight:700;");
            }
            return;
        }

        Rating rating = new Rating(currentUserId, ratingDeck.getIdDeck(), selectedRatingStars);
        rating.setDeckName(ratingDeck.getTitre());
        rating.setDeckSubject(ratingDeck.getMatiere());
        if (ratingCommentArea != null) {
            rating.setComment(safe(ratingCommentArea.getText()).trim());
        }
        ratingService.upsertRating(rating);

        String studentName = UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getUsername()
                : "Student";
        List<Rating> allRatings = ratingService.getAllRatingsByUser(currentUserId);
        Thread aiThread = new Thread(() ->
                aiRatingAnalysisService.analyzeRatings(currentUserId, studentName, allRatings),
                "rating-ai-analysis");
        aiThread.setDaemon(true);
        aiThread.start();

        if (ratingStatusLabel != null) {
            ratingStatusLabel.setText("Your rating was saved successfully. AI analysis has been triggered.");
            ratingStatusLabel.setStyle("-fx-text-fill:#34D399;-fx-font-size:11px;-fx-font-weight:700;");
        }

        refreshDeckList();
        goToList();
    }

    protected void updateRatingSelection(int stars) {
        selectedRatingStars = stars;
        styleRatingButton(ratingStarBtn1, 1, stars);
        styleRatingButton(ratingStarBtn2, 2, stars);
        styleRatingButton(ratingStarBtn3, 3, stars);
        styleRatingButton(ratingStarBtn4, 4, stars);
        styleRatingButton(ratingStarBtn5, 5, stars);
        if (selectedStarsLabel != null) {
            selectedStarsLabel.setText(stars == 0 ? "No rating selected" : ratingMeaning(stars));
        }
    }

    private void styleRatingButton(Button button, int buttonValue, int selectedValue) {
        if (button == null) {
            return;
        }
        boolean selected = buttonValue <= selectedValue && selectedValue > 0;
        String background = selected ? "rgba(236,72,153,0.24)" : "#1E293B";
        String border = selected ? "#F472B6" : "#334155";
        String textColor = selected ? "#F472B6" : "#94A3B8";
        button.setStyle("-fx-background-color:" + background + ";" +
                "-fx-text-fill:" + textColor + ";" +
                "-fx-border-color:" + border + ";" +
                "-fx-border-radius:12;-fx-background-radius:12;" +
                "-fx-border-width:1.5;-fx-font-size:18px;-fx-font-weight:700;" +
                "-fx-cursor:hand;-fx-padding:10 16;");
    }

    private void clearRatingViewState() {
        ratingDeck = null;
        updateRatingSelection(0);
        if (ratingDeckTitleLabel != null) {
            ratingDeckTitleLabel.setText("Deck rating");
        }
        if (ratingDeckMetaLabel != null) {
            ratingDeckMetaLabel.setText("Choose a deck from My Flashcards.");
        }
        if (ratingCommentArea != null) {
            ratingCommentArea.clear();
        }
        if (ratingStatusLabel != null) {
            ratingStatusLabel.setText("Rate a deck from the list to continue.");
            ratingStatusLabel.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        }
    }

    private String ratingMeaning(int stars) {
        return switch (stars) {
            case 1 -> "1 star - Very difficult";
            case 2 -> "2 stars - Difficult";
            case 3 -> "3 stars - Medium";
            case 4 -> "4 stars - Easy";
            case 5 -> "5 stars - Very easy";
            default -> "No rating selected";
        };
    }

    private boolean isInsideButton(Node target, Button button) {
        Node current = target;
        while (current != null) {
            if (current == button) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  VIEW 2 — OPEN DECK
    // ═════════════════════════════════════════════════════════════════════

    private int getDeckColorIndex(Deck deck) { return Math.abs(deck.getIdDeck()) % COLORS.length; }
    private int getDeckIconIndex(Deck deck)  { return Math.abs(deck.getIdDeck()) % ICONS.length;  }

    private void openDeck(Deck deck) {
        currentDeck = deck;

        if (deckTitleLabel != null) deckTitleLabel.setText(deck.getTitre());
        if (deckSubLabel   != null) deckSubLabel  .setText(deck.getMatiere() + " • " + deck.getNiveau());

        int ci = getDeckColorIndex(deck);
        int ii = getDeckIconIndex(deck);
        String colorKey = COLORS[ci];
        String iconLit  = ICONS[ii];
        String hexColor = hex(colorKey);
        String gradient = grad(colorKey);
        String glowRgb  = glowRgba(colorKey);

        if (deckIconGlyph != null) {
            deckIconGlyph.setIconLiteral(iconLit);
            deckIconGlyph.setIconColor(Color.WHITE);
        }

        if (deckHeroPane != null) {
            buildHeroContent(deck, gradient, glowRgb, hexColor, iconLit, colorKey);
        }

        if (searchFcField    != null) searchFcField.clear();
        if (filterEtat       != null) filterEtat.setValue("All Status");
        if (filterDifficulty != null) filterDifficulty.setValue("All Levels");
        if (sortFcCombo      != null) sortFcCombo.setValue("Default");

        goToFlashcards();
        refreshFlashcards();
    }

    private void buildHeroContent(Deck deck, String gradient, String glowRgb,
                                  String hexColor, String iconLit, String colorKey) {

        deckHeroPane.getChildren().clear();
        deckHeroPane.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#0F172A,#130926,#0F172A);" +
                        "-fx-background-radius:18;-fx-border-color:#1E293B;" +
                        "-fx-border-radius:18;-fx-border-width:1;" +
                        "-fx-min-height:168;-fx-max-height:190;"
        );

        StackPane thumbPane = new StackPane();
        thumbPane.setPrefSize(130, 130);
        thumbPane.setMaxSize(130, 130);
        thumbPane.setMinSize(130, 130);

        String imgPath = deck.getImage();
        boolean imageLoaded = false;
        if (imgPath != null && !imgPath.trim().isEmpty()) {
            try {
                String raw = imgPath.trim();
                Image img;
                if (raw.startsWith("http://") || raw.startsWith("https://")) {
                    img = new Image(raw, 130, 130, false, true, true);
                } else {
                    File f = new File(raw);
                    if (!f.exists()) f = new File(System.getProperty("user.dir"), raw);
                    if (!f.exists()) throw new Exception("not found");
                    img = new Image(f.toURI().toString(), 130, 130, false, true);
                }
                ImageView iv = new ImageView(img);
                iv.setFitWidth(130); iv.setFitHeight(130); iv.setPreserveRatio(false);
                Rectangle clip = new Rectangle(130, 130);
                clip.setArcWidth(36); clip.setArcHeight(36);
                iv.setClip(clip);
                Region overlay = new Region();
                overlay.setStyle("-fx-background-color:rgba(0,0,0,0.25);-fx-background-radius:18 0 0 18;");
                thumbPane.getChildren().addAll(iv, overlay);
                imageLoaded = true;
            } catch (Exception ignored) {}
        }
        if (!imageLoaded) {
            thumbPane.setStyle("-fx-background-color:" + gradient + ";-fx-background-radius:18 0 0 18;");
            FontIcon ico = new FontIcon(iconLit);
            ico.setIconSize(28); ico.setIconColor(Color.WHITE);
            thumbPane.getChildren().add(ico);
        }

        VBox centerBox = new VBox(8);
        HBox.setHgrow(centerBox, Priority.ALWAYS);
        centerBox.setPadding(new Insets(14, 18, 14, 18));
        centerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(deck.getTitre());
        titleLbl.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:18px;-fx-font-weight:800;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(Double.MAX_VALUE);

        Label subLbl = new Label(deck.getMatiere() + " • " + deck.getNiveau());
        subLbl.setStyle(
                "-fx-background-color:rgba(124,58,237,0.18);-fx-text-fill:#A78BFA;" +
                        "-fx-font-size:11px;-fx-font-weight:700;" +
                        "-fx-padding:3 10;-fx-background-radius:20;"
        );

        Label helperLbl = new Label("Review, edit or generate new cards for this deck.");
        helperLbl.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        helperLbl.setWrapText(true);
        helperLbl.setMaxWidth(Double.MAX_VALUE);

        if (deck.getDescription() != null && !deck.getDescription().isEmpty()) {
            Label descLbl = new Label(deck.getDescription());
            descLbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");
            descLbl.setWrapText(true);
            descLbl.setMaxWidth(Double.MAX_VALUE);
            centerBox.getChildren().addAll(titleLbl, subLbl, descLbl, helperLbl);
        } else {
            centerBox.getChildren().addAll(titleLbl, subLbl, helperLbl);
        }

        HBox statsBar = new HBox(8);
        statsBar.setAlignment(Pos.CENTER_LEFT);
        statsBar.getChildren().addAll(
                buildCompactStatBox(fcTotalLabel,    "Total",    "#A78BFA"),
                buildCompactStatBox(fcMasteredLabel, "Mastered", "#34D399"),
                buildCompactStatBox(fcLearningLabel, "Learning", "#FBBF24"),
                buildCompactStatBox(fcNewLabel,      "New",      "#94A3B8")
        );
        centerBox.getChildren().add(statsBar);

        VBox btnBox = new VBox(12);
        btnBox.setAlignment(Pos.TOP_RIGHT);
        btnBox.setPadding(new Insets(14, 16, 14, 0));
        btnBox.setPrefWidth(215);

        HBox utilityRow = new HBox(10);
        utilityRow.setAlignment(Pos.CENTER_RIGHT);

        Button backBtn = createHeroGhostButton("Back", "fth-arrow-left", "#94A3B8",
                "#1E293B", "#334155", "#F8FAFC", "#7C3AED");
        backBtn.setOnAction(e -> goBackToList());

        Button addBtn = createHeroPrimaryButton(
                "Add Flashcards", "Create cards manually", "fth-plus", gradient, glowRgb);
        addBtn.setOnAction(e -> showAddFlashcardForm());

        Button aiBtn = createHeroPrimaryButton(
                "Generate with AI", "Auto-build cards faster", "fth-cpu",
                "linear-gradient(to right,#047857,#10B981)", "rgba(16,185,129,0.45)");
        aiBtn.setOnAction(e -> showAIGenerator());

        String pdfPath = deck.getPdf();
        if (pdfPath != null && !pdfPath.trim().isEmpty()) {
            Button pdfBtn = createHeroGhostButton("PDF", "fth-file-text", "#F472B6",
                    "rgba(244,114,182,0.12)", "#F472B6", "#FBCFE8", "#F472B6");
            pdfBtn.setOnAction(e -> {
                try { Desktop.getDesktop().open(new File(pdfPath)); } catch (Exception ignored) {}
            });
            utilityRow.getChildren().add(pdfBtn);
        }

        utilityRow.getChildren().add(backBtn);
        btnBox.getChildren().addAll(utilityRow, addBtn, aiBtn);
        deckHeroPane.getChildren().addAll(thumbPane, centerBox, btnBox);
    }

    private VBox buildCompactStatBox(Label existingLabel, String caption, String color) {
        VBox box = new VBox(1);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-background-color:rgba(15,23,42,0.7);-fx-border-color:#1E293B;" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:5 12;"
        );
        if (existingLabel != null) {
            existingLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:15px;-fx-font-weight:800;");
            box.getChildren().add(existingLabel);
        } else {
            Label val = new Label("0");
            val.setStyle("-fx-text-fill:" + color + ";-fx-font-size:15px;-fx-font-weight:800;");
            box.getChildren().add(val);
        }
        Label cap = new Label(caption);
        cap.setStyle("-fx-text-fill:#475569;-fx-font-size:9px;-fx-font-weight:700;");
        box.getChildren().add(cap);
        return box;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  VIEW 2 — FLASHCARD CARDS
    // ═════════════════════════════════════════════════════════════════════

    private void displayFlashcards(List<Flashcard> list) {
        flashcardsGrid.getChildren().clear();
        if (list.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            FontIcon ico = new FontIcon("fth-inbox");
            ico.setIconSize(40); ico.setIconColor(Color.web("#334155"));
            Label lbl = new Label("No flashcards match.\nTry different filters.");
            lbl.setStyle("-fx-text-fill:#64748B;-fx-font-size:13px;-fx-text-alignment:center;");
            lbl.setWrapText(true);
            empty.getChildren().addAll(ico, lbl);
            flashcardsGrid.getChildren().add(empty);
            return;
        }
        for (Flashcard fc : list) flashcardsGrid.getChildren().add(buildFlashcardCard(fc));
    }

    private VBox buildFlashcardCard(Flashcard fc) {
        return buildInteractiveFlashcardCard(fc);
    }

    private VBox buildInteractiveFlashcardCard(Flashcard fc) {
        String etatColor = switch (fc.getEtat()) {
            case "mastered" -> "#34D399";
            case "learning" -> "#FBBF24";
            default -> "#94A3B8";
        };
        String diffColor = switch (fc.getNiveauDifficulte()) {
            case 3 -> "#FB7185";
            case 2 -> "#FBBF24";
            default -> "#34D399";
        };

        VBox card = new VBox(0);
        card.setPrefWidth(316);
        card.setMaxWidth(316);
        String normalStyle = "-fx-background-color:#0F172A;-fx-background-radius:14;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),10,0,0,3);";
        String hoverStyle = "-fx-background-color:#1E293B;-fx-background-radius:14;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),18,0,0,6);";
        card.setStyle(normalStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));

        Region stripe = new Region();
        stripe.setPrefHeight(4);
        stripe.setStyle("-fx-background-color:" + etatColor + ";-fx-background-radius:14 14 0 0;");

        VBox body = new VBox(12);
        body.setPadding(new Insets(16));

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(fc.getTitre());
        title.setStyle("-fx-text-fill:#F8FAFC;-fx-font-weight:700;-fx-font-size:13px;");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label diffBadge = new Label(fc.getDifficultyLabel());
        diffBadge.setStyle("-fx-background-color:rgba(0,0,0,0.3);-fx-text-fill:" + diffColor +
                ";-fx-font-size:9px;-fx-font-weight:700;-fx-padding:2 7;-fx-background-radius:20;");
        titleRow.getChildren().addAll(title, diffBadge);

        Label qLabel = new Label("Q: " + fc.getQuestion());
        qLabel.setStyle("-fx-text-fill:#CBD5E1;-fx-font-size:11px;");
        qLabel.setWrapText(true);
        qLabel.setMaxHeight(42);

        Label aLabel = new Label("A: " + fc.getReponse());
        aLabel.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");
        aLabel.setWrapText(true);
        aLabel.setMaxHeight(42);

        Label etatBadge = new Label(fc.getEtat().toUpperCase());
        etatBadge.setStyle("-fx-background-color:rgba(0,0,0,0.25);-fx-text-fill:" + etatColor +
                ";-fx-font-size:9px;-fx-font-weight:700;-fx-padding:2 7;-fx-background-radius:20;");

        VBox contentPanel = new VBox(8);
        contentPanel.setPadding(new Insets(12));
        contentPanel.setStyle(
                "-fx-background-color:rgba(15,23,42,0.72);" +
                        "-fx-border-color:#1E293B;-fx-border-radius:12;-fx-background-radius:12;"
        );
        contentPanel.getChildren().addAll(qLabel, aLabel);

        Button toolsBtn = chipBtn("Translate & Audio", "rgba(124,58,237,0.18)", "#C4B5FD", "rgba(124,58,237,0.32)");
        toolsBtn.setGraphic(makeIcon("fth-globe", 11, "#C4B5FD"));
        toolsBtn.setOnAction(e -> showTranslationPopup(fc));

        HBox utilityRow = new HBox(10);
        utilityRow.setAlignment(Pos.CENTER_LEFT);
        Region utilitySpacer = new Region();
        HBox.setHgrow(utilitySpacer, Priority.ALWAYS);
        utilityRow.getChildren().addAll(etatBadge, utilitySpacer, toolsBtn);

        FlowPane actions = new FlowPane();
        actions.setHgap(8);
        actions.setVgap(8);
        actions.setPrefWrapLength(260);
        Button editBtn = chipBtn("Edit", "rgba(99,102,241,0.15)", "#818CF8", "rgba(99,102,241,0.3)");
        Button delBtn = chipBtn("Delete", "rgba(244,63,94,0.15)", "#FB7185", "rgba(244,63,94,0.3)");
        Button mastBtn;
        if ("mastered".equals(fc.getEtat())) {
            mastBtn = chipBtn("Unlearn", "rgba(251,191,36,0.15)", "#FBBF24", "rgba(251,191,36,0.3)");
            mastBtn.setOnAction(e -> {
                flashcardService.updateEtat(fc.getIdFlashcard(), "learning");
                refreshFlashcards();
            });
        } else {
            mastBtn = chipBtn("Mastered", "rgba(52,211,153,0.15)", "#34D399", "rgba(52,211,153,0.3)");
            mastBtn.setOnAction(e -> {
                flashcardService.updateEtat(fc.getIdFlashcard(), "mastered");
                refreshFlashcards();
            });
        }
        editBtn.setOnAction(e -> openEditForm(fc));
        delBtn.setOnAction(e -> handleDelete(fc));
        actions.getChildren().addAll(mastBtn, editBtn, delBtn);

        body.getChildren().addAll(titleRow, contentPanel, utilityRow, actions);
        card.getChildren().addAll(stripe, body);
        return card;
    }

    private void showTranslationPopup(Flashcard fc) {
        Node anchor = flashcardsView != null ? flashcardsView : flashcardsGrid;
        if (anchor == null || anchor.getScene() == null) {
            showInfo("Open a flashcard deck before using translation tools.");
            return;
        }

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(anchor.getScene().getWindow());

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(2,6,23,0.82);-fx-padding:28;");

        VBox panel = new VBox(18);
        panel.setMaxWidth(560);
        panel.setPadding(new Insets(24));
        panel.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#0F172A,#111827,#0B1220);" +
                        "-fx-border-color:#334155;-fx-border-width:1;" +
                        "-fx-border-radius:24;-fx-background-radius:24;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.55),28,0,0,10);"
        );
        panel.setOnMouseClicked(e -> e.consume());
        overlay.setOnMouseClicked(e -> dialog.close());

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBadge = new StackPane(makeIcon("fth-globe", 18, "#C4B5FD"));
        iconBadge.setPrefSize(40, 40);
        iconBadge.setStyle(
                "-fx-background-color:rgba(124,58,237,0.16);" +
                        "-fx-border-color:rgba(196,181,253,0.25);" +
                        "-fx-border-radius:14;-fx-background-radius:14;"
        );

        VBox headerText = new VBox(3);
        Label titleLbl = new Label("Translate and Listen");
        titleLbl.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:18px;-fx-font-weight:800;");
        Label subtitleLbl = new Label("Translate this flashcard and play audio in the selected language.");
        subtitleLbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");
        subtitleLbl.setWrapText(true);
        headerText.getChildren().addAll(titleLbl, subtitleLbl);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = createHeroGhostButton("", "fth-x", "#94A3B8",
                "rgba(15,23,42,0.95)", "#334155", "#F8FAFC", "#7C3AED");
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(iconBadge, headerText, headerSpacer, closeBtn);

        VBox originalPanel = new VBox(8);
        originalPanel.setPadding(new Insets(14));
        originalPanel.setStyle(
                "-fx-background-color:rgba(15,23,42,0.78);" +
                        "-fx-border-color:#1E293B;-fx-border-radius:16;-fx-background-radius:16;"
        );
        Label originalTitle = new Label("Original flashcard");
        originalTitle.setStyle("-fx-text-fill:#C4B5FD;-fx-font-size:11px;-fx-font-weight:700;");
        Label originalQuestion = new Label("Q: " + safe(fc.getQuestion()));
        originalQuestion.setWrapText(true);
        originalQuestion.setStyle("-fx-text-fill:#E2E8F0;-fx-font-size:12px;");
        Label originalAnswer = new Label("A: " + safe(fc.getReponse()));
        originalAnswer.setWrapText(true);
        originalAnswer.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
        originalPanel.getChildren().addAll(originalTitle, originalQuestion, originalAnswer);

        VBox toolPanel = new VBox(12);
        toolPanel.setPadding(new Insets(14));
        toolPanel.setStyle(
                "-fx-background-color:rgba(8,15,28,0.95);" +
                        "-fx-border-color:#1E293B;-fx-border-radius:16;-fx-background-radius:16;"
        );

        Label languageTitle = new Label("Choose translation language");
        languageTitle.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:12px;-fx-font-weight:700;");

        FlowPane languageButtons = new FlowPane();
        languageButtons.setHgap(8);
        languageButtons.setVgap(8);

        Label translationStatus = new Label("Select a language to translate this flashcard.");
        translationStatus.setWrapText(true);
        translationStatus.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");

        HBox audioRow = new HBox(10);
        audioRow.setAlignment(Pos.CENTER_LEFT);
        Button listenQuestionBtn = chipBtn("Listen Question", "rgba(14,165,233,0.15)", "#38BDF8", "rgba(14,165,233,0.28)");
        listenQuestionBtn.setGraphic(makeIcon("fth-volume-2", 11, "#38BDF8"));
        Button listenAnswerBtn = chipBtn("Listen Answer", "rgba(16,185,129,0.15)", "#34D399", "rgba(16,185,129,0.28)");
        listenAnswerBtn.setGraphic(makeIcon("fth-headphones", 11, "#34D399"));
        audioRow.getChildren().addAll(listenQuestionBtn, listenAnswerBtn);

        VBox translatedPanel = new VBox(8);
        translatedPanel.setPadding(new Insets(14));
        translatedPanel.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,rgba(30,41,59,0.9),rgba(17,24,39,0.95));" +
                        "-fx-border-color:rgba(167,139,250,0.25);" +
                        "-fx-border-radius:16;-fx-background-radius:16;"
        );

        Label translatedSectionTitle = new Label("Translated preview");
        translatedSectionTitle.setStyle("-fx-text-fill:#DDD6FE;-fx-font-size:11px;-fx-font-weight:700;");
        Label translatedTitle = new Label();
        translatedTitle.setWrapText(true);
        translatedTitle.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:13px;-fx-font-weight:700;");
        setNodeVisible(translatedTitle, false);

        Label translatedQuestion = new Label();
        translatedQuestion.setWrapText(true);
        translatedQuestion.setStyle("-fx-text-fill:#E2E8F0;-fx-font-size:12px;");
        setNodeVisible(translatedQuestion, false);

        Label translatedAnswer = new Label();
        translatedAnswer.setWrapText(true);
        translatedAnswer.setStyle("-fx-text-fill:#C4B5FD;-fx-font-size:12px;");
        setNodeVisible(translatedAnswer, false);

        translatedPanel.getChildren().addAll(
                translatedSectionTitle, translationStatus,
                translatedTitle, translatedQuestion, translatedAnswer
        );

        final String[] activeLanguageCode = {null};
        for (Map.Entry<String, String> entry : SUPPORTED_LANGUAGES.entrySet()) {
            String languageCode = entry.getKey();
            String languageLabel = entry.getValue();
            Button langBtn = chipBtn(languageCode.toUpperCase() + " • " + languageLabel,
                    "rgba(30,41,59,0.95)", "#E2E8F0", "rgba(51,65,85,0.95)");
            langBtn.setOnAction(e -> {
                activeLanguageCode[0] = languageCode;
                loadTranslation(fc, languageCode, languageLabel, translationStatus,
                        translatedTitle, translatedQuestion, translatedAnswer);
            });
            languageButtons.getChildren().add(langBtn);
        }

        listenQuestionBtn.setOnAction(e -> {
            String languageCode = activeLanguageCode[0];
            String text = fc.getQuestion();
            if (languageCode != null) {
                AITranslationService.TranslationResult translated = translationCache.get(translationCacheKey(fc, languageCode));
                if (translated != null) text = translated.question();
            }
            speechService.speakAsync(text, speechLanguageTag(languageCode));
        });

        listenAnswerBtn.setOnAction(e -> {
            String languageCode = activeLanguageCode[0];
            String text = fc.getReponse();
            if (languageCode != null) {
                AITranslationService.TranslationResult translated = translationCache.get(translationCacheKey(fc, languageCode));
                if (translated != null) text = translated.answer();
            }
            speechService.speakAsync(text, speechLanguageTag(languageCode));
        });

        toolPanel.getChildren().addAll(languageTitle, languageButtons, audioRow);
        panel.getChildren().addAll(header, originalPanel, toolPanel, translatedPanel);
        overlay.getChildren().add(panel);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  VIEW 3 — FORM (abstract)
    // ═════════════════════════════════════════════════════════════════════

    @FXML public abstract void showAddFlashcardForm();
    @FXML public void showAIGenerator() { showInfo("AI generator is not available on this screen."); }
    @FXML public abstract void handleSave();
    @FXML public abstract void handleCancel();
    protected abstract void handleDelete(Flashcard fc);
    protected abstract void openEditForm(Flashcard fc);
    protected abstract boolean validateForm();
    protected abstract void clearForm();

    // ═════════════════════════════════════════════════════════════════════
    //  TRANSLATION
    // ═════════════════════════════════════════════════════════════════════

    private void loadTranslation(Flashcard fc, String languageCode, String languageLabel,
                                 Label statusLabel, Label titleLabel, Label questionLabel, Label answerLabel) {
        String cacheKey = translationCacheKey(fc, languageCode);
        AITranslationService.TranslationResult cached = translationCache.get(cacheKey);
        if (cached != null) {
            applyTranslation(cached, statusLabel, titleLabel, questionLabel, answerLabel);
            return;
        }

        statusLabel.setText("Translating to " + languageLabel + "...");
        setNodeVisible(titleLabel, false);
        setNodeVisible(questionLabel, false);
        setNodeVisible(answerLabel, false);

        Thread worker = new Thread(() -> {
            try {
                AITranslationService.TranslationResult result =
                        translationService.translateFlashcard(fc, languageCode, languageLabel);
                translationCache.put(cacheKey, result);
                Platform.runLater(() -> applyTranslation(result, statusLabel, titleLabel, questionLabel, answerLabel));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Translation failed: " + e.getMessage());
                    setNodeVisible(titleLabel, false);
                    setNodeVisible(questionLabel, false);
                    setNodeVisible(answerLabel, false);
                });
            }
        }, "flashcard-translation");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyTranslation(AITranslationService.TranslationResult result,
                                  Label statusLabel, Label titleLabel, Label questionLabel, Label answerLabel) {
        statusLabel.setText(result.languageLabel() + " ready.");
        titleLabel.setText(result.title());
        questionLabel.setText("Q: " + result.question());
        answerLabel.setText("A: " + result.answer());
        setNodeVisible(titleLabel, true);
        setNodeVisible(questionLabel, true);
        setNodeVisible(answerLabel, true);
    }

    private void setNodeVisible(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private String translationCacheKey(Flashcard fc, String languageCode) {
        return fc.getIdFlashcard() + "|" + safe(fc.getTitre()) + "|" + languageCode;
    }

    private String speechLanguageTag(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) return "en-US";
        return switch (languageCode) {
            case "fr" -> "fr-FR";
            case "ar" -> "ar-SA";
            case "es" -> "es-ES";
            case "de" -> "de-DE";
            default -> "en-US";
        };
    }

    // ═════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════

    protected int difficultyFromLabel(String label) {
        return switch (label) { case "Medium" -> 2; case "Hard" -> 3; default -> 1; };
    }

    private void styleTextArea(TextArea ta) {
        if (ta == null) return;
        ta.setStyle("-fx-control-inner-background:#1E293B;-fx-background-color:#1E293B;" +
                "-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                "-fx-border-color:#334155;-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;");
    }

    private Button chipBtn(String text, String bg, String fg, String bgH) {
        String s  = "-fx-background-color:" + bg  + ";-fx-text-fill:" + fg + ";-fx-font-size:10px;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:5 12;";
        String sh = "-fx-background-color:" + bgH + ";-fx-text-fill:" + fg + ";-fx-font-size:10px;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:5 12;";
        Button b = new Button(text);
        b.setStyle(s);
        b.setOnMouseEntered(e -> b.setStyle(sh));
        b.setOnMouseExited (e -> b.setStyle(s));
        return b;
    }

    private Button createHeroGhostButton(String text, String iconLiteral, String baseTextColor,
                                         String baseBackground, String baseBorder,
                                         String hoverTextColor, String hoverBorder) {
        String baseStyle = "-fx-background-color:" + baseBackground + ";" +
                "-fx-text-fill:" + baseTextColor + ";-fx-font-size:11px;-fx-font-weight:700;" +
                "-fx-background-radius:12;-fx-border-radius:12;-fx-border-width:1;" +
                "-fx-border-color:" + baseBorder + ";-fx-cursor:hand;-fx-padding:8 12;";
        String hoverStyle = "-fx-background-color:#334155;" +
                "-fx-text-fill:" + hoverTextColor + ";-fx-font-size:11px;-fx-font-weight:700;" +
                "-fx-background-radius:12;-fx-border-radius:12;-fx-border-width:1;" +
                "-fx-border-color:" + hoverBorder + ";-fx-cursor:hand;-fx-padding:8 12;";
        Button button = new Button(text);
        button.setGraphic(makeIcon(iconLiteral, 12, baseTextColor));
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> {
            button.setStyle(hoverStyle);
            if (button.getGraphic() instanceof FontIcon fi) fi.setIconColor(Color.web(hoverTextColor));
        });
        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            if (button.getGraphic() instanceof FontIcon fi) fi.setIconColor(Color.web(baseTextColor));
        });
        return button;
    }

    private Button createHeroPrimaryButton(String title, String subtitle, String iconLiteral,
                                           String background, String glowRgb) {
        String baseStyle = "-fx-background-color:" + background + ";" +
                "-fx-background-radius:16;-fx-cursor:hand;-fx-padding:12 14;" +
                "-fx-effect:dropshadow(gaussian," + glowRgb + ",12,0,0,4);";
        String hoverStyle = "-fx-background-color:" + background + ";" +
                "-fx-background-radius:16;-fx-cursor:hand;-fx-padding:12 14;" +
                "-fx-effect:dropshadow(gaussian," + glowRgb + ",18,0,0,6);" +
                "-fx-scale-x:1.02;-fx-scale-y:1.02;";

        StackPane iconWrap = new StackPane(makeIcon(iconLiteral, 16, "white"));
        iconWrap.setPrefSize(34, 34);
        iconWrap.setStyle("-fx-background-color:rgba(255,255,255,0.16);-fx-background-radius:12;");

        VBox textBox = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:800;");
        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.72);-fx-font-size:10px;-fx-font-weight:600;");
        textBox.getChildren().addAll(titleLbl, subtitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(10, iconWrap, textBox, spacer, makeIcon("fth-arrow-right", 12, "white"));
        content.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefWidth(190);
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        return button;
    }

    private FontIcon makeIcon(String literal, int size, String color) {
        FontIcon fi = new FontIcon(literal);
        fi.setIconSize(size);
        fi.setIconColor(Color.web(color));
        return fi;
    }

    @FXML public void goBackToFlashcards() { goToFlashcards(); refreshFlashcards(); }
    @FXML public void openNotesWorkspace() { MainController.loadContentInMainArea("views/Notes.fxml"); }

    protected void alert(String msg) { new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait(); }
    protected void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private String safe(String v) { return v == null ? "" : v; }

    private String hex(String c) {
        return switch (c) {
            case "primary" -> "#A78BFA"; case "success" -> "#34D399";
            case "warning" -> "#FBBF24"; case "danger"  -> "#FB7185";
            case "accent"  -> "#FB923C"; default        -> "#94A3B8";
        };
    }

    private String grad(String c) {
        return switch (c) {
            case "primary" -> "linear-gradient(to bottom right,#7C3AED,#8B5CF6)";
            case "success" -> "linear-gradient(to bottom right,#059669,#10B981)";
            case "warning" -> "linear-gradient(to bottom right,#D97706,#F59E0B)";
            case "danger"  -> "linear-gradient(to bottom right,#DC2626,#F43F5E)";
            case "accent"  -> "linear-gradient(to bottom right,#EA580C,#F97316)";
            default        -> "linear-gradient(to bottom right,#475569,#64748B)";
        };
    }

    private String glowRgba(String c) {
        return switch (c) {
            case "primary" -> "rgba(124,58,237,0.55)";
            case "success" -> "rgba(5,150,105,0.55)";
            case "warning" -> "rgba(217,119,6,0.55)";
            case "danger"  -> "rgba(220,38,38,0.55)";
            case "accent"  -> "rgba(234,88,12,0.55)";
            default        -> "rgba(71,85,105,0.45)";
        };
    }
}
