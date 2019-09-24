package blockchain.gui;

import blockchain.net.FullNode;
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

public class MinerTabPageController {

    @FXML
    private Label minerToggleLabel;

    @FXML
    private ToggleButton minerToggleButton; // Off by default!

    @FXML
    private Label lastCalculatedHashLabel;

    private FullNode node;
    private long refreshTimeInSeconds = 1;

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
        this.minerToggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue == newValue || newValue == null) return;
            if(newValue){
                setLabelToOFF(minerToggleButton);
                setLabelToON(minerToggleLabel);
            } else {
                setLabelToON(minerToggleButton);
                setLabelToOFF(minerToggleLabel);
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
