package controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class Main {

    public static Stage mainStage;

    public static void main(String[] args) {
        UI.main(args);
    }

    public static class UI extends Application {

        public static void main(String[] args) {
            Application.launch();
        }

        @Override
        public void start(Stage stage) throws Exception {
            mainStage = stage;
            stage.setTitle("Twitch View Bot 0.1");
            Scene content = FXMLLoader.load(getResource("/sample.fxml", "Scene file cannot be null"));
            content.getStylesheets().add(getResource("/styles.css", "Style file cannot be null").toExternalForm());
            stage.setScene(content);
            stage.setResizable(false);
            stage.show();
            stage.setOnCloseRequest(e -> System.exit(0));
        }

        private URL getResource(String name, String message) {
            return Objects.requireNonNull(getClass().getResource(name), message);
        }

    }

}
