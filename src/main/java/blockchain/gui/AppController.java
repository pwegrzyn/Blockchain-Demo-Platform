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
    private Blockchain blockchain;

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

    }


    public void setNode(WalletNode node) {
        this.node = node;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
        blockchainTabPageController.setBlockchain(blockchain);
    }
}
