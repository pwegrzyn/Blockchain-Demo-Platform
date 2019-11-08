package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.config.Mode;
import blockchain.model.*;
import blockchain.net.FullNode;
import blockchain.net.WalletNode;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class AppController {

    private Stage primaryStage;
    private Scene primaryScene;
    private WalletNode node;
    private String currentTheme = "assets/css/defaulttheme.css";

    @FXML
    private SummaryTabPageController summaryTabPageController;
    @FXML
    private BlockchainTabPageController blockchainTabPageController;
    @FXML
    private TransactionsTabPageController transactionsTabPageController;
    @FXML
    private WalletTabPageController walletTabPageController;
    @FXML
    private MinerTabPageController minerTabPageController;
    @FXML
    private TxVisTabPageController txVisTabPageController;
    @FXML
    private BlockchainVisTabPageController bcVisTabPageController;
    @FXML
    private Tab minerTab;
    @FXML
    private AttackTabPageController attackTabPageController;

    // Possibly can add new themes for javaFX here
    private List<String> themesList = new LinkedList<String>() {
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

        if (ButtonType.OK.equals(closeResponse.get())) {
            // All pre-exit stuff the app needs to do should go here
            this.node.disconnect();
        } else {
            event.consume();
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

        blockchainTabPageController.init();

        // Init the wallet tab controller
        walletTabPageController.init();
        walletTabPageController.setNode(node);

        // Init the summary tab controller
        summaryTabPageController.setNode(node);
        summaryTabPageController.init();

        // Init the graph electron demo
        this.txVisTabPageController.init();

        // Init the blockchain graph electron
        this.bcVisTabPageController.init();

        this.transactionsTabPageController.init();

        // Disable the miner tab if Running Mode is WALLET
        if (Configuration.getInstance().getNodeRunningMode() != Mode.FULL) {
            this.minerTab.setDisable(true);
        } else {
            minerTabPageController.setNode((FullNode) this.node);
        }

        // Init the majority attack scene
        attackTabPageController.init();

    }

    public void setNode(WalletNode node) {
        this.node = node;
    }

    // Test method to check if the Blockchain gui is working
    private void addSampleBlocks() {
        String previousHash = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getLatestBlock().getCurrentHash());
        for (int i = 1; i < 40; i++) {
            TransactionInput input = new TransactionInput("prevhash", i - 1, "fromAddress", "signature");
            TransactionOutput output = new TransactionOutput(new BigDecimal(50.0), "receiverAddress");
            List<TransactionInput> inputList = new LinkedList<>();
            inputList.add(input);
            List<TransactionOutput> outputList = new LinkedList<>();
            outputList.add(output);
            Transaction tx = new Transaction("transactionId" + i, TransactionType.REGULAR, inputList, outputList);
            List<Transaction> txList = new LinkedList<>();
            txList.add(tx);
            Block block = new Block(i, txList, previousHash, i, i);
            previousHash = block.getCurrentHash();
            SynchronizedBlockchainWrapper.useBlockchain(b -> {b.addBlock(block); return null;});
        }
    }

    private void addSampleTransactions() {
        for (int i = 0; true; i++) {
            TransactionInput input = new TransactionInput("prevhash", i - 1, "fromAddress", "signature");
            TransactionOutput output = new TransactionOutput(new BigDecimal(50.0), "receiverAddress");
            List<TransactionInput> inputList = new LinkedList<>();
            inputList.add(input);
            List<TransactionOutput> outputList = new LinkedList<>();
            outputList.add(output);
            Transaction tx = new Transaction("transactionId" + i, TransactionType.REGULAR, inputList, outputList);

            SynchronizedBlockchainWrapper.useBlockchain(b -> b.getUnconfirmedTransactions().add(tx));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
    }

}
