package widget.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import widget.ui.WeatherController;

public class WeatherApplication extends Application {
    private WeatherController controller;
    @Override public void start(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
        controller = new WeatherController(stage);
        Scene scene = new Scene(controller.view(), 380, 600);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/widget.css").toExternalForm());
        stage.setTitle("Weather Widget");
        // Keep the compact widget size and disable the native maximize/zoom action.
        stage.setResizable(false);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/cloudy.png")));
        stage.setScene(scene); stage.setMinWidth(340); stage.setMinHeight(540); stage.show();
        controller.start();
    }
    @Override public void stop() { if (controller != null) controller.close(); }
}
