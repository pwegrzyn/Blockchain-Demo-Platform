package blockchain.gui;

import blockchain.model.SynchronizedBlockchainWrapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class TransactionsTabPageController {

    private final int refreshTimeInSeconds = 1;

    void init() {

        new Thread(() -> {
            while(SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain() == null){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            updateListView();
            Timeline updateControlsTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshTimeInSeconds), event -> updateListView()));
            updateControlsTimeline.setCycleCount(Timeline.INDEFINITE);
            updateControlsTimeline.play();
        }).start();

    }

    private void updateListView() {
    }

}
