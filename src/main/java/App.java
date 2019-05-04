import blockchain.crypto.ECDSA;
import blockchain.crypto.Hash;
import blockchain.net.Node;
import blockchain.presenter.AppGUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class App extends Application {

    public static void main(String[] args) {
        Properties properties = loadProperties();
        Node node = new Node("test_net");
        launch(args);
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

    private static Properties loadProperties() {
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.err.println("Unable to find config.properties!");
                return null;
            }
            prop.load(input);
            System.out.println("Configuration loaded successfully.");
            return  prop;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
