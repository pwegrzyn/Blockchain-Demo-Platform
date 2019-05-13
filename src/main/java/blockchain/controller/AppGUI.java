package blockchain.controller;

import blockchain.model.Blockchain;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppGUI {

    private static final Logger LOGGER = Logger.getLogger(AppGUI.class.getName());
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
        this.primaryStage.setResizable(false);
        this.controller = loader.getController();
        this.controller.setPrimaryStageElements(primaryStage, scene);
        showInitScreen();
    }

    public void update(){
        Platform.runLater(()-> {
            // Any Runnable can go here at any time during runtime
        });
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void showInitScreen() {
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("views/initView.fxml"));
            BorderPane layout = loader.load();
            Stage initStage = new Stage();
            Scene initScene = new Scene(layout,600,340);
            initStage.setScene(initScene);
            initStage.setTitle("Blockchain Demo Platform - Init");
            InitController initController = loader.getController();
            initController.setStage(initStage);
            initController.setPrimaryController(this.controller);
            initController.setBlockchain(this.blockchain);
            initController.init();
            initStage.setAlwaysOnTop(true);
            initStage.setResizable(false);
            initStage.show();
        }catch(IOException e){
            LOGGER.log(Level.SEVERE, "Error while loading init view!", e);
        }
    }

}
