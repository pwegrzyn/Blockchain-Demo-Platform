package blockchain.gui;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppGUI {

    private static final Logger LOGGER = Logger.getLogger(AppGUI.class.getName());
    private Stage primaryStage;
    private AppController controller;

    public AppGUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void initApplication() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("views/mainView.fxml"));
        TabPane rootLayout = loader.load();
        Scene scene = new Scene(rootLayout, 1200, 750);
        this.primaryStage.setScene(scene);
        this.primaryStage.setResizable(false);
        java.awt.image.BufferedImage imageIcon = ImageIO.read(getClass().getClassLoader().getResource("assets/icons/coin.png"));
        this.primaryStage.getIcons().add(SwingFXUtils.toFXImage(imageIcon, null));
        this.controller = loader.getController();
        this.controller.setPrimaryStageElements(primaryStage, scene);
        showInitScreen();
    }

    public void update(){
        Platform.runLater(()-> {
            // Any Runnable can go here at any time during runtime
        });
    }

    public void showInitScreen() {
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("views/initView.fxml"));
            BorderPane layout = loader.load();
            Stage initStage = new Stage();
            Scene initScene = new Scene(layout,600,400);
            initStage.setScene(initScene);
            java.awt.image.BufferedImage imageIcon = ImageIO.read(getClass().getClassLoader().getResource("assets/icons/coin.png"));
            initStage.getIcons().add(SwingFXUtils.toFXImage(imageIcon, null));
            initStage.setTitle("Blockchain Demo Platform - Init");
            InitController initController = loader.getController();
            initController.setStage(initStage);
            initController.setPrimaryController(this.controller);
            initController.init();
            initStage.setResizable(false);
            initStage.show();
        }catch(IOException e){
            LOGGER.log(Level.SEVERE, "Error while loading init view!", e);
        }
    }

}
