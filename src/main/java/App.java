import blockchain.model.Block;
import blockchain.model.Blockchain;
import blockchain.net.Node;
import blockchain.controller.AppGUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {


    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static Blockchain blockchain;

    public static void main(String[] args) {

        blockchain = new Blockchain();
        blockchain.setBlockList(blockListDemo());

        addBlockToBlockListTask();

        Node node = new Node("test_net");
        launch(args);
    }

    private static void addBlockToBlockListTask() {
        new Thread(() -> {
            int ind = 6;
            try {
                while(true){
                    Thread.sleep(5000);
                    blockchain.getBlockList().add(new Block(ind++, Collections.emptyList(), "prevHash" + (ind-1), ind * 23, ind * 101));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static List<Block> blockListDemo() {
        Block block1 = new Block(1, Collections.emptyList(), "prevHash0", 515, 101);
        Block block2 = new Block(2, Collections.emptyList(), "prevHash1", 3654, 202);
        Block block3 = new Block(3, Collections.emptyList(), "prevHash2", 9, 303);
        Block block4 = new Block(4, Collections.emptyList(), "prevHash3", 7544632, 404);
        Block block5 = new Block(5, Collections.emptyList(), "prevHash4", 61, 505);
        return new ArrayList<Block>() {{
            add(block1);
            add(block2);
            add(block3);
            add(block4);
            add(block5);
        }};
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            AppGUI gui = new AppGUI(primaryStage);
            gui.setBlockchain(blockchain);
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
