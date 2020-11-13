import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Controller {

    ViewBot viewBot = new ViewBot();

    @FXML
    private Button startButton;

    @FXML
    private TextField channelNameField;

    @FXML
    private TextArea logArea;

    @FXML
    public void changeButton() {
        startButton.setText("5");;
    }

    @FXML
    public void writeLog() {
        logArea.appendText("lol\n");
    }

    @FXML
    private void checkChannelName() {
        String target = channelNameField.getText();
        if (target.isBlank() && target.isEmpty()) {
            channelNameField.getStyleClass().add("error");
        } else {
            channelNameField.getStyleClass().remove("error");
        }
    }
}
