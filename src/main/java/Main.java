import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
            Scene content = FXMLLoader.load(getClass().getResource("sample.fxml"));
            content.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            stage.setScene(content);
            stage.show();
        }
    }
}
