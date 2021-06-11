package controller;

import config.Config;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class ControllerConfig {

    @FXML
    private CheckBox startWhenLive;

    @FXML
    private CheckBox stopWhenOffline;

    @FXML
    private TextField stopAfterHs;

    @FXML
    private TextField repeatEveryMinutes;


    @FXML
    void initialize() {
        startWhenLive.setSelected(Config.startWhenLiveValue);
        stopWhenOffline.setSelected(Config.stopWhenOfflineValue);
        stopAfterHs.setText(String.valueOf(Config.stopAfterHsValue));
        repeatEveryMinutes.setText(String.valueOf(Config.repeatEveryMinutesValue));
        stopAfterHs.setDisable(Config.stopWhenOfflineValue);
        stopWhenOffline.selectedProperty().addListener((observable, oldValue, newValue) -> stopAfterHs.setDisable(newValue));
    }

    @FXML
    void saveAllOptions() {
        synchronized (this) {
            Config.startWhenLiveValue = startWhenLive.isSelected();
            Config.stopWhenOfflineValue = stopWhenOffline.isSelected();
            try {
                Config.stopAfterHsValue = Integer.parseInt(stopAfterHs.getText());
                Config.repeatEveryMinutesValue = Integer.parseInt(repeatEveryMinutes.getText());
            } catch (Exception e) {
                System.err.println("Failed to parse int, set it to default value");
            }
        }
    }
}
