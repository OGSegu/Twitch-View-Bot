import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Controller {

    ViewBot viewBot;

    @FXML
    private Button startButton;

    @FXML
    private TextField channelNameField;

    @FXML
    public TextArea logArea;


    @FXML
    public void changeButton() {
        startButton.setText("5");
    }

    @FXML
    public void writeToLog(String text) {
        logArea.appendText(text + "\n");
    }


    @FXML
    private void start() {
        if (startButton.getText().equals("START")) {
            viewBot = new ViewBot(this);
            Thread viewBotThread = new Thread(viewBot::start);
            String target = channelNameField.getText();
            if (!viewBot.isChannelNameValid(target)) {
                channelNameField.getStyleClass().add("error");
                writeToLog("Wrong channel name. Try again");
            } else {
                channelNameField.getStyleClass().remove("error");
                viewBot.setTarget(target);
                viewBot.setThreads(50);
                viewBotThread.start();
                startButton.setText("STOP");
            }
        } else {
            stop();
        }
    }

    @FXML
    private void stop() {
        if (viewBot != null) {
            startButton.setText("START");
            writeToLog("Stopped");
        }
    }

    public Button getStartButton() {
        return startButton;
    }

    public TextField getChannelNameField() {
        return channelNameField;
    }

    public TextArea getLogArea() {
        return logArea;
    }
}
