package widget.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import widget.service.SettingsService;

/** Desktop window behavior, independent of weather and data loading. */
final class WidgetWindow {
    private double dragX;
    private double dragY;

    HBox header(Stage stage, SettingsService settings) {
        Label title = new Label("WEATHER");
        title.getStyleClass().add("eyebrow");
        StackPane handle = new StackPane(title);
        handle.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        handle.setMaxWidth(Double.MAX_VALUE);
        handle.setCursor(Cursor.MOVE);
        HBox.setHgrow(handle, Priority.ALWAYS);
        Tooltip.install(handle, new Tooltip("Drag to move widget"));
        handle.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragX = e.getScreenX() - stage.getX();
                dragY = e.getScreenY() - stage.getY();
                e.consume();
            }
        });
        handle.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                stage.setX(e.getScreenX() - dragX);
                stage.setY(e.getScreenY() - dragY);
                e.consume();
            }
        });
        ToggleButton pin = new ToggleButton("Pin");
        pin.setSelected(Boolean.parseBoolean(settings.get("alwaysOnTop", "false")));
        stage.setAlwaysOnTop(pin.isSelected());
        pin.setTooltip(new Tooltip("Keep widget above other windows"));
        pin.setAccessibleText("Always on top");
        pin.setOnAction(e -> {
            stage.setAlwaysOnTop(pin.isSelected());
            settings.put("alwaysOnTop", Boolean.toString(pin.isSelected()));
        });
        Button minimize = button("−", "Minimize widget");
        minimize.setOnAction(e -> stage.setIconified(true));
        Button close = button("×", "Close widget");
        close.getStyleClass().add("window-close");
        close.setOnAction(e -> stage.close());
        HBox bar = new HBox(6, handle, pin, minimize, close);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.getStyleClass().add("window-bar");
        stage.setOnShown(e -> restorePosition(stage, settings));
        stage.setOnHiding(e -> {
            settings.put("windowX", Double.toString(stage.getX()));
            settings.put("windowY", Double.toString(stage.getY()));
        });
        return bar;
    }

    private Button button(String glyph, String description) {
        Button button = new Button(glyph);
        button.setAccessibleText(description);
        button.setTooltip(new Tooltip(description));
        return button;
    }

    private void restorePosition(Stage stage, SettingsService settings) {
        try {
            double x = Double.parseDouble(settings.get("windowX", "NaN"));
            double y = Double.parseDouble(settings.get("windowY", "NaN"));
            if (!Double.isFinite(x) || !Double.isFinite(y)) return;
            // Clamp into an available display when monitor arrangements have changed.
            Screen display = Screen.getScreens().stream()
                    .filter(s -> s.getVisualBounds().intersects(x, y, stage.getWidth(), stage.getHeight()))
                    .findFirst().orElse(Screen.getPrimary());
            Rectangle2D bounds = display.getVisualBounds();
            stage.setX(Math.max(bounds.getMinX(), Math.min(x, bounds.getMaxX() - stage.getWidth())));
            stage.setY(Math.max(bounds.getMinY(), Math.min(y, bounds.getMaxY() - stage.getHeight())));
        } catch (NumberFormatException ignored) {
            // Invalid saved coordinates leave the platform's default position intact.
        }
    }
}
