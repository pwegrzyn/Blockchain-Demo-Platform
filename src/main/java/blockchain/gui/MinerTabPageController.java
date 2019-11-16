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
import org.apache.commons.lang3.time.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    // Which means that miner is OFF by default

    @FXML
    private Label lastCalculatedHashLabel;

    @FXML
    private Label gpuInfo;

    @FXML
    private Label totalRunningTime;

    private StopWatch stopWatch = new StopWatch();

    private FullNode node;
    private long refreshTimeInSeconds = 1;

    @FXML
    public void initialize() {
        String foundGPU = extractCardName();
        if (foundGPU != null) {
            this.gpuInfo.setText(foundGPU);
        } else {
            this.gpuInfo.setText("Could not extract the GPU card name :(");
        }

        addListenerToMinerToggleButton();
        Timeline updateControlsTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshTimeInSeconds), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                updateLastHashLabel();
                updateStopWatch();
            }
        }));

        updateControlsTimeline.setCycleCount(Timeline.INDEFINITE);
        updateControlsTimeline.play();
    }

    public ToggleButton getIsMinerOnToggle() {
        return this.isMinerOnToggle;
    }

    public void stopMiner() {
        if (node.getMinerThread() == null || node.getMinerThread().isShutdown()) {
            logger.info("Trying to shutdown an already stopped Miner Thread!");
            return;
        }
        logger.info("Shutting down Miner Thread");
        node.getMinerThread().shutdownNow();
        setLabelToRedOFF(minerToggleLabel);
        this.isMinerOnToggle.setSelected(false);
        this.stopWatch.suspend();
    }

    private void updateLastHashLabel() {
        if (System.getProperty("lastCalculatedHash")!= null && !System.getProperty("lastCalculatedHash").equals("")) {
            lastCalculatedHashLabel.setText(System.getProperty("lastCalculatedHash"));
        } else {
            lastCalculatedHashLabel.setText("-");
        }
    }

    private void updateStopWatch() {
        this.totalRunningTime.setText(this.stopWatch.toString());
    }

    private void addListenerToMinerToggleButton(){
        this.isMinerOnToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue == newValue) return;

            if(newValue){
                setLabelToGreenON(minerToggleLabel);
                logger.info("Starting miner process");
                node.setMinerThread(Executors.newSingleThreadExecutor());
                node.getMinerThread().submit(new Miner(node));
                if (this.stopWatch.isSuspended()) {
                    this.stopWatch.resume();
                } else {
                    this.stopWatch.start();
                }
            } else {
                logger.info("Shutting down miner process");
                this.stopMiner();
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

    // this only works on Windows
    private String extractCardName() {
        String foundInfo = null;
        try {
            String filePath = "./dxdiag_output.txt";
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "dxdiag", "/t", filePath);
            Process p = pb.start();
            p.waitFor();
            try(BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while((line = br.readLine()) != null){
                    if(line.trim().startsWith("Card name:")) {
                        foundInfo = line.trim().split(":")[1].trim();
                    }
                }
            }
            new File(filePath).delete();
        } catch (Exception e) {
            logger.warning("Failed to extract GPU card name!");
        }
        return foundInfo;
    }

}
