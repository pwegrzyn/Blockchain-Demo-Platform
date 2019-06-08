package blockchain.gui;

import blockchain.model.Block;
import blockchain.model.Transaction;
import blockchain.net.WalletNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AppController {

    private Stage primaryStage;
    private Scene primaryScene;
    private WalletNode node;
    private String currentTheme = "assets/css/defaulttheme.css";

    @FXML
    private ListView<Block> blockListView;

    @FXML
    private ListView<Transaction> txListView;

    @FXML
    private Label blockIndexLabel;

    @FXML
    private Label blockHashLabel;

    @FXML
    private Label txCountLabel;

    @FXML
    private Label prevBlockHashLabel;

    @FXML
    private Label nonceLabel;

    @FXML
    private Label timestampLabel;

    @FXML
    private Label txIndexLabel;

    @FXML
    private Label txHashLabel;

    @FXML
    private Label txTypeLabel;

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

    public void setPrimaryStageElements(Stage primaryStage, Scene primaryScene) {
        this.primaryStage = primaryStage;
        this.primaryScene = primaryScene;
        this.primaryScene.getStylesheets().add(this.currentTheme);
        primaryStage.setTitle("Blockchain Demo Platform");
        test();
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

}
