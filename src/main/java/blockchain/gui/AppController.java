package blockchain.gui;

import blockchain.model.*;
import blockchain.net.WalletNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class AppController {

    private Stage primaryStage;
    private Scene primaryScene;
    private WalletNode node;
    private String currentTheme = "assets/css/defaulttheme.css";

    @FXML private SummaryTabPageController summaryTabPageController;
    @FXML private BlockchainTabPageController blockchainTabPageController;
    @FXML private TransactionsTabPageController transactionsTabPageController;
    @FXML private WalletTabPageController walletTabPageController;
    @FXML private MinerTabPageController minerTabPageController;

    // Possibly can add new themes for javaFX here
    private List<String> themesList = new LinkedList<String>(){
        {
            add("assets/css/defaulttheme.css");
        }
    };

    // Close-confirmation handler
    private EventHandler<WindowEvent> confirmCloseEventHandler = event -> {
        Alert closeConfirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to exit?"
        );
        Button exitButton = (Button) closeConfirmation.getDialogPane().lookupButton(
                ButtonType.OK
        );
        exitButton.setText("Exit");
        closeConfirmation.setHeaderText("Confirm Exit");
        closeConfirmation.initModality(Modality.APPLICATION_MODAL);
        closeConfirmation.initOwner(this.primaryStage);
        Optional<ButtonType> closeResponse = closeConfirmation.showAndWait();
        if (!ButtonType.OK.equals(closeResponse.get())) {
            // All pre-exit stuff the app needs to do should go here
            event.consume();
            this.node.disconnect();
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
        // Pin the close-confirmation hook
        this.primaryStage.setOnCloseRequest(confirmCloseEventHandler);

        // Get the initial blockchain if this node is the first one or get the blockchain which was taken from other
        // existing nodes otherwise
        this.blockchainTabPageController.setBlockchain(this.node.getBlockchain());
        this.walletTabPageController.setBlockchain(this.node.getBlockchain());

        // Test the blockchain gui by adding some dummy blocks
        addSampleBlocks();

        // Init the wallet tab controller
        walletTabPageController.init();
        walletTabPageController.setNode(node);
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
