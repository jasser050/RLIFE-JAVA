package games;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class GeoGamesHub {
    private GeoGamesHub() {}

    public static void open(Stage owner) {
        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("🌍 Geography Games");

        VBox root = new VBox(24);
        root.setPadding(new Insets(26));
        root.setStyle("-fx-background-color: #0d1b2a;");

        Label title = new Label("🌍 Geography Games");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 32));

        FlowPane cards = new FlowPane();
        cards.setHgap(18);
        cards.setVgap(18);
        cards.setPrefWrapLength(820);
        cards.getChildren().addAll(
                card("🚩", "Flag Quiz", "flag", s -> FlagQuizGame.open(stage, () -> refresh(cards, stage))),
                card("🏙️", "Capital Quiz", "capital", s -> CapitalQuizGame.open(stage, () -> refresh(cards, stage))),
                card("🗺️", "Map Click", "map", s -> MapClickGame.open(stage, () -> refresh(cards, stage))),
                card("🃏", "Flag Memory", "memory", s -> FlagMemoryGame.open(stage, () -> refresh(cards, stage)))
        );

        Label champion = new Label("Champion Badge: " + BadgeManager.getBadge("champion"));
        champion.setTextFill(Color.web("#ffd54f"));
        champion.setFont(Font.font("System", FontWeight.BOLD, 16));
        Label streak = new Label("Student streak: " + readStudentStreak());
        streak.setTextFill(Color.web("#b0bec5"));

        root.getChildren().addAll(title, cards, champion, streak);
        stage.setScene(new Scene(root, 900, 650));
        stage.show();
    }

    private static StackPane card(String emoji, String name, String key, Consumer<Stage> action) {
        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER);
        Label icon = new Label(emoji);
        icon.setFont(Font.font(52));
        Label nameLb = new Label(name);
        nameLb.setTextFill(Color.WHITE);
        nameLb.setFont(Font.font("System", FontWeight.BOLD, 16));
        Label score = new Label("Best: " + ScoreManager.getHighScore(key));
        score.setTextFill(Color.web("#ffd54f"));
        Label badge = new Label(BadgeManager.getBadge(key));
        badge.setTextFill(Color.web("#80cbc4"));
        content.getChildren().addAll(icon, nameLb, score, badge);

        StackPane card = new StackPane(content);
        card.setPrefSize(180, 180);
        card.setStyle("-fx-background-color:#1b2a3b; -fx-background-radius:16;");
        ScaleTransition in = new ScaleTransition(Duration.millis(150), card);
        in.setToX(1.05); in.setToY(1.05);
        ScaleTransition out = new ScaleTransition(Duration.millis(150), card);
        out.setToX(1.0); out.setToY(1.0);
        card.setOnMouseEntered(e -> { out.stop(); in.playFromStart(); });
        card.setOnMouseExited(e -> { in.stop(); out.playFromStart(); });
        card.setOnMouseClicked(e -> action.accept((Stage) card.getScene().getWindow()));
        return card;
    }

    private static void refresh(FlowPane cards, Stage stage) {
        cards.getChildren().setAll(
                card("🚩", "Flag Quiz", "flag", s -> FlagQuizGame.open(stage, () -> refresh(cards, stage))),
                card("🏙️", "Capital Quiz", "capital", s -> CapitalQuizGame.open(stage, () -> refresh(cards, stage))),
                card("🗺️", "Map Click", "map", s -> MapClickGame.open(stage, () -> refresh(cards, stage))),
                card("🃏", "Flag Memory", "memory", s -> FlagMemoryGame.open(stage, () -> refresh(cards, stage)))
        );
    }

    private static int readStudentStreak() {
        String[] candidates = {"PlannerData", "com.studyflow.PlannerData", "com.studyflow.data.PlannerData"};
        for (String c : candidates) {
            try {
                Class<?> cls = Class.forName(c);
                Object inst;
                try {
                    Method m = cls.getMethod("getInstance");
                    inst = m.invoke(null);
                } catch (NoSuchMethodException e) {
                    inst = cls.getDeclaredConstructor().newInstance();
                }
                Method streak = cls.getMethod("getStudentStreak");
                Object out = streak.invoke(inst);
                if (out instanceof Number n) return n.intValue();
            } catch (Exception ignored) {}
        }
        return 0;
    }
}
