package blockchain.gui;

import blockchain.model.Blockchain;
import blockchain.model.SynchronizedBlockchainWrapper;
import blockchain.model.Transaction;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.util.Duration;

import java.util.stream.Collectors;

public class TransactionsTabPageController {

    private final int refreshTimeInSeconds = 1;

    @FXML
    private ListView<String> txList;

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
        this.txList.setItems(FXCollections.observableArrayList(
                SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getUnconfirmedTransactions)
                        .stream()
                        .map(this::generateTxInfo)
                        .collect(Collectors.toList())));
    }

    private String generateTxInfo(Transaction tx) {
        StringBuilder result = new StringBuilder("");

        result.append("FROM: ");
        String txCreator = tx.getCreatorAddr();
        result.append(txCreator, 0, 5).append("...").append(txCreator.substring(txCreator.length() - 5)).append("   |   ");

        result.append("TO: ");
        String toAddr = tx.getOutputs().get(0).getReceiverAddress();
        result.append(toAddr, 0, 5).append("...").append(toAddr.substring(toAddr.length() - 5)).append("   |   ");

        result.append("AMOUNT: ");
        result.append(tx.getOutputs().get(0).getAmount()).append("    |   ");

        result.append("ID: ");
        result.append(tx.getId());

        return result.toString();
    }

}
