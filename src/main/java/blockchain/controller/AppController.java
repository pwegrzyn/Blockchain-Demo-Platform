package blockchain.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class AppController {

    private Stage primaryStage;
    private Scene primaryScene;

    @FXML
    private AnchorPane mainPane;

    public void setPrimaryStageElements(Stage primaryStage, Scene primaryScene) {
        this.primaryStage = primaryStage;
        this.primaryScene = primaryScene;
        primaryStage.setTitle("Blockchain Demo Platform");
    }

    @FXML
    private void handleShowInfo(ActionEvent event)  {
        System.out.println("Showing Info");
    }


    public void backToMainView() {
        primaryStage.setScene(primaryScene);
        primaryStage.setTitle("Blockchain Demo Platform");
        primaryStage.show();
    }
}
