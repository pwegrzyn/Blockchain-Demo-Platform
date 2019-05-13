package blockchain.presenter;

import blockchain.controller.AppController;
import blockchain.model.Blockchain;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AppGUI {

    private Stage primaryStage;
    private AppController controller;
    private Blockchain blockchain;

    public AppGUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void initApplication() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("views/mainView.fxml"));
        BorderPane rootLayout = loader.load();
        Scene scene = new Scene(rootLayout);
        this.primaryStage.setScene(scene);
        this.primaryStage.show();
        this.controller = loader.getController();
        this.controller.setPrimaryStageElements(primaryStage, scene);
        this.controller.initTreeView(blockchain.getBlockList());
    }

    public void update(){
        Platform.runLater(()-> {
            // Any Runnable can go here at any time during runtime
        });
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

}
