package main;

import blockchain.gui.AppGUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class App extends Application {
    private static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$s] %5$s %n");
        LOGGER = Logger.getLogger(App.class.getName());
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "A fatal error occurred during runtime!", e);
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        AppGUI gui = new AppGUI(primaryStage);
        gui.initApplication();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }

}
