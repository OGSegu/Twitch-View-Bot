package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import viewbot.ViewBot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class ControllerMain {
    private static final FileChooser fileChooser = new FileChooser();
    private ViewBot viewBot;
    private LinkedBlockingQueue<String> proxyQueue = new LinkedBlockingQueue<>();

    static {
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
    public void writeToLog(String text) {
        logArea.appendText(text + "\n");
    }


    @FXML
    private void start() {
        if (!startButton.getText().equals("START")) {
            viewBot.stop();
            startButton.setText("START");
        } else {
            if (proxyQueue.isEmpty()) {
                writeToLog("Proxy not loaded");
                return;
            }
            String target = channelNameField.getText();
            if (!isChannelValid(target)) {
                channelNameField.getStyleClass().add("error");
                writeToLog("Wrong channel . Try again");
                return;
            }
            channelNameField.getStyleClass().remove("error");

            viewBot = new ViewBot(this, proxyQueue, target);
            viewBot.setThreads(Integer.parseInt(labelViewers.getText()));
            Thread prepareToStartThread = new Thread(viewBot::prepareToStart);
            startButton.setText("STOP");
            prepareToStartThread.start();
            try {
                prepareToStartThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Thread startThread = new Thread(viewBot::start);
            startThread.start();
        }
    }

    private boolean isChannelValid(String target) {
        return !target.isBlank() && !target.isEmpty();
    }

    @FXML
    public void stopViewBot() {
        if (viewBot != null) {
            startButton.setText("START");
            resetCount();
            cleanLogArea();
            writeToLog("Stopped");
        }
    }


    @FXML
    private void loadProxy() {
        File file = fileChooser.showOpenDialog(Main.mainStage);
        if (file != null) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                String proxy;
                proxyQueue = new LinkedBlockingQueue<>(100000);
                while ((proxy = bufferedReader.readLine()) != null) {
                    proxyQueue.add(proxy);
                }
            } catch (IOException e) {
                System.out.println("Something went wrong");
            }
            writeToLog("Proxy loaded: " + proxyQueue.size());
        } else {
            writeToLog("File was not found");
        }
    }

    @FXML
    private void openConfig() throws IOException {
        Scene scene = FXMLLoader.load(getClass().getResource("/config.fxml"));
        Stage stage = new Stage();
        stage.setTitle("Config");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    public void cleanLogArea() {
        logArea.clear();
    }

    public Button getStartButton() {
        return startButton;
    }
}
