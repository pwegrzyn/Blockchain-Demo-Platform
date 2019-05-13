package blockchain.controller;

import blockchain.model.Blockchain;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class InitController {

    private Stage stage;
    private AppController primaryController;
    private Blockchain blockchain;

    @FXML
    private void handleOkButton(ActionEvent event) {
        this.stage.close();
        this.primaryController.initTreeView(blockchain.getBlockList());
        this.primaryController.backToMainView();
    }

    @FXML
    private void handleCancelButton(ActionEvent event) {
        System.exit(0);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setPrimaryController(AppController primaryController) {
        this.primaryController = primaryController;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }
}
