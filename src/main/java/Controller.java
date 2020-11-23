import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Controller {

    FileChooser fileChooser = new FileChooser();
    ViewBot viewBot = new ViewBot(this);
    {
        fileChooser.setTitle("Choose proxy file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
    }

    @FXML
    private Button startButton;

    @FXML
    private TextField channelNameField;

    @FXML
    public TextArea logArea;

    @FXML
    public Button loadProxiesButton;

    @FXML
    private Slider slider;

    @FXML
    private Label labelViewers;

    @FXML
    private Label viewCount;


    public void initialize() {
        slider.valueProperty().addListener(((observable, oldValue, newValue) ->
                labelViewers.setText(String.valueOf(newValue.intValue()))
                ));
    }

    @FXML
    public void addCount() {
        viewCount.setText(String.valueOf(Integer.parseInt(viewCount.getText()) + 1));
    }

    @FXML
    public void resetCount() {
        viewCount.setText("0");
    }

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
            if (viewBot.getFullProxyList().size() == 0) {
                writeToLog("Proxy not loaded");
                return;
            }
            String target = channelNameField.getText();
            if (!viewBot.isChannelNameValid(target)) {
                channelNameField.getStyleClass().add("error");
                writeToLog("Wrong channel name. Try again");
                return;
            }
            Thread viewBotThread = new Thread(viewBot::start);
            channelNameField.getStyleClass().remove("error");
            viewBot.setTarget(target);
            int threads = Integer.parseInt(labelViewers.getText());
            viewBot.setThreads(threads);
            viewBot.setThreadPoolExecutor(new ThreadPoolExecutor(0,
                    500,
                    10L,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadPoolExecutor.CallerRunsPolicy()));
            viewBotThread.start();
            startButton.setText("STOP");
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

    @FXML
    private void loadProxy() {
        File file = fileChooser.showOpenDialog(Main.mainStage);
        if (file != null)
            viewBot.loadProxy(file);
        else
            writeToLog("File was not found");
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
