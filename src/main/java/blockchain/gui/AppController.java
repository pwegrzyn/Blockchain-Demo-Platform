package blockchain.gui;

import blockchain.model.*;
import blockchain.net.WalletNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.List;

public class AppController {

    private Stage primaryStage;
    private Scene primaryScene;
    private WalletNode node;
    private String currentTheme = "assets/css/defaulttheme.css";

    @FXML private BlockchainTabPageController blockchainTabPageController;

    // Possibly can add new themes for javaFX here
    private List<String> themesList = new LinkedList<String>(){
        {
            add("assets/css/defaulttheme.css");
        }
    };


    public void setPrimaryStageElements(Stage primaryStage, Scene primaryScene) {
        this.primaryStage = primaryStage;
        this.primaryScene = primaryScene;
        this.primaryScene.getStylesheets().add(this.currentTheme);
        primaryStage.setTitle("Blockchain Demo Platform");
    }

    public void backToMainView() {
        primaryStage.setScene(primaryScene);
        primaryStage.setTitle("Blockchain Demo Platform");
        primaryStage.show();
    }

    public void init() {
        this.blockchainTabPageController.setBlockchain(this.node.getBlockchain());
        // Test the blockchain gui by adding some dummy blocks
        addSampleBlocks();
    }


    public void setNode(WalletNode node) {
        this.node = node;
    }

    // Test method to check if the Blockchain gui is working, delete it later
    private void addSampleBlocks(){
        for(int i = 0; i < 40; i++){
            TransactionInput input = new TransactionInput("prevhash", i - 1,
                    50.0, "fromAddress", "signature");
            TransactionOutput output = new TransactionOutput(50.0, "receiverAddress");
            List<TransactionInput> inputList = new LinkedList<>();
            inputList.add(input);
            List<TransactionOutput> outputList = new LinkedList<>();
            outputList.add(output);
            Transaction tx = new Transaction("transactionId" + i, TransactionType.REGULAR, inputList, outputList);
            List<Transaction> txList = new LinkedList<>();
            txList.add(tx);
            Block block = new Block(i, txList, "hash" + (i - 1), i, i);
            this.node.getBlockchain().getBlockList().add(block);
            this.node.getBlockchain().getBlockHashList().add(block.getCurrentHash());
        }
    }

}
