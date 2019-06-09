import blockchain.gui.AppGUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;


public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {

            AppGUI gui = new AppGUI(primaryStage);
            gui.initApplication();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while initializing the GUI!", e);
            System.exit(1);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }

}
