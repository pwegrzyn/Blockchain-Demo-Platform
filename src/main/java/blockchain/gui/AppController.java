package blockchain.gui;

import blockchain.model.Block;
import blockchain.model.Blockchain;
import blockchain.model.Transaction;
import blockchain.net.WalletNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.List;

public class AppController {

    private Stage primaryStage;
    private Scene primaryScene;
    private WalletNode node;
    private String currentTheme = "assets/css/defaulttheme.css";
    private Blockchain blockchain;
    private Block selectedBlock;
    private Transaction selectedTransaction;

    private ObservableList<String> hashBlockList;

    @FXML private ListView<String> blockListView;
    @FXML private ListView<Transaction> txListView;

    @FXML private Label blockIndexLabel;
    @FXML private Label blockHashLabel;
    @FXML private Label txCountLabel;
    @FXML private Label prevBlockHashLabel;
    @FXML private Label nonceLabel;
    @FXML private Label timestampLabel;

    @FXML private Label txIndexLabel;
    @FXML private Label txHashLabel;
    @FXML private Label txTypeLabel;
    @FXML private TableView txOutputTxTable;
    @FXML private TableView txInputTxTable;

    @FXML private AnchorPane blockPropertiesPane;
    @FXML private AnchorPane txPropertiesPane;

    // Possibly can add new themes for javaFX here
    private List<String> themesList = new LinkedList<String>(){
        {
            add("assets/css/defaulttheme.css");
        }
    };

    private void test(){
        this.blockHashLabel.setText("HASH123");
        this.blockIndexLabel.setText("5");
        this.prevBlockHashLabel.setText("HASH122");
        this.timestampLabel.setText("01:25:24");
        this.nonceLabel.setText("23");
    }

    private void updateSelectedBlock(String newSelectedHash){
        Block newBlock = blockchain.findBlock(newSelectedHash);
        selectedBlock = newBlock;
        selectedTransaction = null;
        updateTransactionList(newBlock);
        blockIndexLabel.setText("" + newBlock.getIndex());
        blockHashLabel.setText(newBlock.getCurrentHash());
        txCountLabel.setText("" + newBlock.getTransactions().size());
        prevBlockHashLabel.setText(newBlock.getPreviousHash());
        nonceLabel.setText("" + newBlock.getNonce());
        timestampLabel.setText("" + newBlock.getTimestamp());
    }

    private void updateTransactionList(Block block){
        List<Transaction> transactions = block.getTransactions();
        txListView.setItems(FXCollections.observableArrayList(transactions));
        txListView.setVisible(true);
    }

    private void setTransactionInfoVisibility(boolean visible){
        txPropertiesPane.setVisible(visible);
    }

    private void setBlockInfoVisibility(boolean visible){
        blockPropertiesPane.setVisible(visible);
    }

    public void setPrimaryStageElements(Stage primaryStage, Scene primaryScene) {
        this.primaryStage = primaryStage;
        this.primaryScene = primaryScene;
        this.primaryScene.getStylesheets().add(this.currentTheme);
        primaryStage.setTitle("Blockchain Demo Platform");
        test();
        this.blockListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                setTransactionInfoVisibility(false);
                updateSelectedBlock(newValue);
                setBlockInfoVisibility(true);
            }

        });
    }

    public void backToMainView() {
        primaryStage.setScene(primaryScene);
        primaryStage.setTitle("Blockchain Demo Platform");
        primaryStage.show();
    }

    public void init() {

    }

    public void setNode(WalletNode node) {
        this.node = node;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.hashBlockList = FXCollections.observableArrayList(blockchain.getBlockHashList());
        blockListView.setItems(hashBlockList);
        System.out.println("Added blockchain to AppController");
        System.out.println(blockchain.getBlockList().size() + " " + blockchain.getBlockHashList().size());
        this.hashBlockList.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                System.out.println("Changed");
                if(c.wasAdded()){
                    for(String s : c.getAddedSubList()){
                        Block blockToAdd = blockchain.findBlock(s);
                        ObservableList<String> blocklist = blockListView.getItems();
                        if(blocklist.get(blocklist.size() - 1).equals(blockToAdd.getPreviousHash())){
                            blocklist.add(blockToAdd.getCurrentHash());
                        }
                    }
                } else if(c.wasRemoved()){

                }
            }
        });
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }
}
