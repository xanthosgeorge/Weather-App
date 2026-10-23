package widget.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.application.ColorScheme;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;
import widget.api.OpenMeteoClient;
import widget.model.WeatherData;
import widget.repository.WeatherRepository;
import widget.service.*;
import java.io.File;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class WeatherController implements AutoCloseable {
    private final Stage stage;
    private final SettingsService settings = new SettingsService();
    private final WeatherRepository repository = new WeatherRepository(Path.of(System.getProperty("user.home"), ".weather-widget", "history.sqlite"));
    private final WeatherService service = new WeatherService(new OpenMeteoClient(), repository);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "weather-worker"); t.setDaemon(true); return t; });
    private final StackPane root = new StackPane();
    private final Region backdrop = new Region();
    private final Rectangle overlay = new Rectangle();
    private final VBox content = new VBox(6);
    private final TextField search = new TextField();
    private final Button go = new Button("Search");
    private final Label location = label("Your weather", "location"), clock = label("Search for a city to begin", "muted"), temperature = label("—°", "temperature"), condition = label("A little clarity for your day", "condition"), feels = label("", "muted"), status = label("", "status");
    private final Label humidity = label("—", "metric-value"), wind = label("—", "metric-value"), uv = label("—", "metric-value");
    private final ImageView icon = new ImageView();
    private final HBox forecast = new HBox(8);
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Timeline ticker;
    private ZoneId zone;
    private Image background;
    private boolean closed;

    public WeatherController(Stage stage) {
        this.stage = stage;
        root.getStyleClass().addAll("widget", "desktop-widget");
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty()); clip.heightProperty().bind(root.heightProperty());
        clip.setArcWidth(32); clip.setArcHeight(32); root.setClip(clip);
        backdrop.setBackground(Background.EMPTY);
        overlay.widthProperty().bind(root.widthProperty()); overlay.heightProperty().bind(root.heightProperty());
        overlay.setMouseTransparent(true); backdrop.setMouseTransparent(true);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true); scroll.setFitToHeight(true); scroll.getStyleClass().add("weather-scroll");
        HBox windowHeader = new WidgetWindow().header(stage, settings);
        windowHeader.setPadding(new Insets(12, 16, 0, 16));
        VBox frame = new VBox(windowHeader, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().addAll(backdrop, overlay, frame);
        content.setPadding(new Insets(16)); content.setAlignment(Pos.TOP_CENTER);
        search.setPromptText("Search city"); search.setAccessibleText("City name"); HBox.setHgrow(search, Priority.ALWAYS);
        search.setOnAction(e -> load(search.getText())); go.setOnAction(e -> load(search.getText()));
        HBox searchRow = new HBox(8, search, go);
        Button history = new Button("Recent"), appearance = new Button("Appearance");
        history.setOnAction(e -> history()); appearance.setOnAction(e -> appearance());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tools = new HBox(8, spacer, history, appearance); tools.setAlignment(Pos.CENTER_LEFT);
        icon.setFitWidth(92); icon.setFitHeight(78); icon.setPreserveRatio(true);
        icon.setImage(new Image(getClass().getResourceAsStream("/assets/cloudy.png")));
        HBox hero = new HBox(8, temperature, icon); hero.setAlignment(Pos.CENTER);
        HBox metrics = new HBox(8, metric("Humidity", humidity), metric("Wind · km/h", wind), metric("UV · daily max", uv));
        forecast.setAlignment(Pos.CENTER);
        progress.setMaxSize(16, 16); progress.setVisible(false); progress.setManaged(false);
        status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        HBox feedback = new HBox(8, progress, status); feedback.setAlignment(Pos.CENTER_LEFT);
        Hyperlink credit = new Hyperlink("Weather by Open-Meteo"); credit.getStyleClass().add("muted");
        credit.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://open-meteo.com/")); } catch (Exception ignored) {} });
        Region bottom = new Region(); VBox.setVgrow(bottom, Priority.ALWAYS);
        content.getChildren().addAll(tools, searchRow, location, clock, hero, condition, feels, metrics, label("NEXT HOURS", "eyebrow"), forecast, feedback, bottom, credit);
        ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock())); ticker.setCycleCount(Animation.INDEFINITE);
        Platform.getPreferences().colorSchemeProperty().addListener((o, a, b) -> applyAppearance());
        root.widthProperty().addListener((o,a,b) -> updateBackground()); root.heightProperty().addListener((o,a,b) -> updateBackground());
        restoreBackground(); applyAppearance();
    }
    public Parent view() { return root; }
    public void start() { ticker.play(); String last = settings.get("location", ""); if (!last.isBlank()) { search.setText(last); load(last); } }
    private static Label label(String text, String style) { Label l = new Label(text); l.getStyleClass().add(style); return l; }
    private VBox metric(String title, Label value) {
        VBox box = new VBox(6, label(title, "muted"), value); box.getStyleClass().add("card"); box.setAlignment(Pos.CENTER); box.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(box, Priority.ALWAYS); return box;
    }
    private <T> void background(Callable<T> task, Consumer<T> success) {
        worker.submit(() -> {
            try { T value = task.call(); Platform.runLater(() -> { if (!closed) success.accept(value); }); }
            catch (Exception e) { Platform.runLater(() -> { if (!closed) { busy(false); status.setText(e.getMessage() == null ? "Something went wrong. Please retry." : e.getMessage()); } }); }
        });
    }
    private void load(String query) {
        if (query == null || query.isBlank()) { status.setText("Enter a city to search."); search.requestFocus(); return; }
        busy(true); status.setText("Updating weather…");
        background(() -> service.search(query), result -> {
            busy(false); WeatherData d = result.weather(); zone = d.zone(); updateClock();
            settings.put("location", d.query()); location.setText(d.location()); temperature.setText(Math.round(d.temperature()) + "°");
            condition.setText(WeatherData.condition(d.code())); feels.setText(d.feelsLike() == null ? "Feels like unavailable" : "Feels like " + Math.round(d.feelsLike()) + "°");
            humidity.setText(Math.round(d.humidity()) + "%"); wind.setText(String.format("%.0f", d.wind())); uv.setText(d.uv() == null ? "—" : String.format("%.1f", d.uv()));
            icon.setImage(new Image(getClass().getResourceAsStream("/assets/" + WeatherData.icon(d.code()) + ".png")));
            forecast.getChildren().clear();
            for (WeatherData.Hour h : d.forecast()) {
                ImageView small = new ImageView(new Image(getClass().getResourceAsStream("/assets/" + WeatherData.icon(h.code()) + ".png"))); small.setFitWidth(28); small.setFitHeight(26); small.setPreserveRatio(true);
                VBox cell = new VBox(6, label(h.time().format(DateTimeFormatter.ofPattern("HH:mm")), "muted"), small, label(Math.round(h.temperature()) + "°", "hour-temp"));
                cell.setAlignment(Pos.CENTER); cell.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(cell, Priority.ALWAYS); forecast.getChildren().add(cell);
            }
            status.setText(result.warning().isEmpty() ? "Updated " + d.observed().format(DateTimeFormatter.ofPattern("HH:mm")) + " · local time" : result.warning());
        });
    }
    private void busy(boolean busy) { go.setDisable(busy); search.setDisable(busy); progress.setVisible(busy); progress.setManaged(busy); }
    private void updateClock() { if (zone != null) clock.setText(ZonedDateTime.now(zone).format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))); }
    private Dialog<Void> dialog(String title) {
        Dialog<Void> d = new Dialog<>(); d.initOwner(stage); d.setTitle(title); d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/widget.css").toExternalForm());
        d.getDialogPane().getStyleClass().add("widget"); if (isLight()) d.getDialogPane().getStyleClass().add("light"); return d;
    }
    private void history() {
        status.setText("Loading recent searches…");
        background(repository::recent, rows -> {
            status.setText(""); Dialog<Void> d = dialog("Recent searches"); VBox list = new VBox(8); list.setPrefWidth(300);
            if (rows.isEmpty()) list.getChildren().add(new Label("Your searches will appear here."));
            for (WeatherRepository.Recent r : rows) {
                Button b = new Button(r.query() + "\n" + r.searches() + " searches · average " + String.format("%.1f°C", r.average())); b.setMaxWidth(Double.MAX_VALUE);
                b.setOnAction(e -> { d.close(); search.setText(r.query()); load(r.query()); }); list.getChildren().add(b);
            }
            ScrollPane scroll = new ScrollPane(list); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(Math.min(450, 60 * Math.max(1, rows.size()))); d.getDialogPane().setContent(scroll); d.show();
        });
    }
    private void appearance() {
        Dialog<Void> d = dialog("Appearance"); VBox form = new VBox(12); form.setPrefWidth(300);
        ComboBox<String> theme = new ComboBox<>(); theme.getItems().addAll("Dark", "Light", "System"); theme.setValue(settings.get("theme", "Dark"));
        theme.setOnAction(e -> { settings.put("theme", theme.getValue()); applyAppearance(); d.getDialogPane().getStyleClass().remove("light"); if (isLight()) d.getDialogPane().getStyleClass().add("light"); });
        Label fileState = new Label(background == null ? "Default background" : "Custom image"); fileState.setWrapText(true);
        Button choose = new Button("Choose Image…"), reset = new Button("Reset Background");
        choose.setOnAction(e -> {
            FileChooser picker = new FileChooser(); picker.setTitle("Choose background image"); picker.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = picker.showOpenDialog(d.getDialogPane().getScene().getWindow());
            if (file != null) {
                fileState.setText("Loading image…");
                background(() -> readImage(file.toPath()), image -> { background = image; settings.put("background", file.getAbsolutePath()); updateBackground(); fileState.setText("Custom image: " + file.getName()); });
            }
        });
        reset.setOnAction(e -> { background = null; settings.put("background", ""); updateBackground(); fileState.setText("Default background"); });
        Slider opacity = new Slider(0, 1, settings.number("overlay", .65)); opacity.setAccessibleText("Background overlay intensity");
        opacity.valueProperty().addListener((o,a,b) -> { settings.setNumber("overlay", b.doubleValue()); applyAppearance(); });
        Slider blur = new Slider(0, 1, settings.number("blur", .15)); blur.setAccessibleText("Background blur intensity");
        blur.valueProperty().addListener((o,a,b) -> { settings.setNumber("blur", b.doubleValue()); updateBackground(); });
        form.getChildren().addAll(new Label("Theme"), theme, new Label("Background · Default / Custom Image"), fileState, new HBox(8, choose, reset), new Label("Overlay intensity"), opacity, new Label("Blur intensity"), blur, label("System follows JavaFX OS appearance; dark is the fallback.", "muted"));
        d.getDialogPane().setContent(form); d.show();
    }
    private Image readImage(Path path) throws Exception {
        if (!java.nio.file.Files.isRegularFile(path)) throw new IllegalArgumentException("Background image is missing. Choose another image.");
        try (var in = java.nio.file.Files.newInputStream(path)) {
            Image image = new Image(in, 1600, 1600, true, true);
            if (image.isError() || image.getWidth() <= 0) throw new IllegalArgumentException("Invalid background. Choose a PNG or JPEG image.");
            return image;
        }
    }
    private void restoreBackground() {
        String path = settings.get("background", "");
        if (!path.isBlank()) background(() -> readImage(Path.of(path)), image -> { background = image; updateBackground(); });
    }
    private boolean isLight() { return settings.get("theme", "Dark").equals("Light") || settings.get("theme", "Dark").equals("System") && !Platform.getPreferences().isEmpty() && Platform.getPreferences().getColorScheme() == ColorScheme.LIGHT; }
    private void applyAppearance() {
        root.getStyleClass().removeAll("light", "custom"); if (isLight()) root.getStyleClass().add("light"); if (background != null) root.getStyleClass().add("custom");
        overlay.setFill(isLight() ? Color.rgb(241,243,245, settings.number("overlay", .65)) : Color.rgb(23,27,33, settings.number("overlay", .65)));
        overlay.setVisible(background != null);
    }
    private void updateBackground() {
        backdrop.setBackground(background == null ? Background.EMPTY : new Background(new BackgroundImage(background, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(100,100,true,true,false,true))));
        backdrop.setEffect(new GaussianBlur(settings.number("blur", .15) * 30)); applyAppearance();
    }
    @Override public void close() { closed = true; ticker.stop(); worker.shutdownNow(); }
}
