package blockchain.gui;

import blockchain.net.FullNode;
import blockchain.protocol.Miner;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class MinerTabPageController {

    private static final Logger logger = Logger.getLogger(MinerTabPageController.class.getName());

    @FXML
    private Label minerToggleLabel;

    @FXML
    private ToggleButton isMinerOnToggle;
    // Toggle is false by default
    // Toggle has "TURN ON" message by default
    // Which means that miner is OFF by default

    @FXML
    private Label lastCalculatedHashLabel;

    private FullNode node;
    private long refreshTimeInSeconds = 1;

    private ExecutorService minerThread;

    @FXML
    public void initialize() {
        addListenerToMinerToggleButton();
        Timeline updateControlsTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshTimeInSeconds), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                updateLastHashLabel();
            }
        }));
        updateControlsTimeline.setCycleCount(Timeline.INDEFINITE);
        updateControlsTimeline.play();
    }

    private void updateLastHashLabel() {
        lastCalculatedHashLabel.setText(System.getProperty("lastCalculatedHash"));
    }

    private void addListenerToMinerToggleButton(){
        this.isMinerOnToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue == newValue) return;

            if(newValue){
                setLabelToGreenON(minerToggleLabel);
                logger.info("Starting miner process");
                minerThread = Executors.newSingleThreadExecutor();
                minerThread.submit(new Miner(node));
            } else {
                logger.info("Shutting down miner process");
                minerThread.shutdownNow();
                setLabelToRedOFF(minerToggleLabel);
            }
        });
    }

    private void setLabelToGreenON(Labeled label){
        label.setText(label.getText().replace("OFF", "ON"));
        label.setTextFill(Color.web("#00FF00"));
    }
    private void setLabelToRedOFF(Labeled label){
        label.setText(label.getText().replace("ON", "OFF"));
        label.setTextFill(Color.web("#FF0000"));
    }

    public void setNode(FullNode node) {
        this.node = node;
    }

}
