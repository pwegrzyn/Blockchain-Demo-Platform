import blockchain.config.Configuration;
import blockchain.model.Blockchain;
import blockchain.net.FullNode;
import blockchain.gui.AppGUI;
import blockchain.net.WalletNode;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {


    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String CLUSTER_NAME = "test_net";
    private static WalletNode node;

    public static void main(String[] args) {

        Blockchain blockchain = new Blockchain();

        switch(Configuration.getInstance().getNodeRunningMode()) {
            case FULL:
                node = new FullNode(CLUSTER_NAME, blockchain);
                break;
            case WALLET:
                node = new WalletNode(CLUSTER_NAME, blockchain);
                break;
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            AppGUI gui = new AppGUI(primaryStage);
            gui.setNode(node);
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
