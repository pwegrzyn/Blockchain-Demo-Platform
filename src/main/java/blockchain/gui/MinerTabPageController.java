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

public class MinerTabPageController {

    @FXML
    private Label minerToggleLabel;

    @FXML
    private ToggleButton isMinerOnToggle;
    // Toggle is false by default
    // Toggle has "TURN ON" message by default

    @FXML
    private Label lastCalculatedHashLabel;

    private FullNode node;
    private long refreshTimeInSeconds = 1;

    private ExecutorService minerThread;

    @FXML
    public void initialize() {
        minerThread = Executors.newSingleThreadExecutor();
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
            if(oldValue == newValue || newValue == null) return;
            if(!newValue){
                System.out.println("Shutting down miner process");
                minerThread.shutdownNow();
                minerThread = Executors.newSingleThreadExecutor();
                setLabelToOFF(isMinerOnToggle);
                setLabelToON(minerToggleLabel);
            } else {
                setLabelToON(isMinerOnToggle);
                setLabelToOFF(minerToggleLabel);
                minerThread.submit(new Miner(node));
                System.out.println("Starting miner process");   // todo change to log messages
            }
        });
    }

    private void setLabelToON(Labeled label){
        label.setText(label.getText().replace("OFF", "ON"));
        label.setTextFill(Color.web("#00FF00"));
    }
    private void setLabelToOFF(Labeled label){
        label.setText(label.getText().replace("ON", "OFF"));
        label.setTextFill(Color.web("#FF0000"));
    }

    public void setNode(FullNode node) {
        this.node = node;
    }

}
