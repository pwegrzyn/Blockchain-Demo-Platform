package blockchain.presenter;

import blockchain.controller.AppController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AppGUI {

    private Stage primaryStage;
    private AppController controller;

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
        System.out.println("GUI initialized successfully.");
    }

    public void update(){
        Platform.runLater(()-> {
            // Any Runnable can go here at any time during runtime
        });
    }

}
