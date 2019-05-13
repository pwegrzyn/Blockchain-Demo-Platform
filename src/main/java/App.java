import blockchain.model.Block;
import blockchain.model.Blockchain;
import blockchain.net.Node;
import blockchain.presenter.AppGUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class App extends Application {


    private static Blockchain blockchain;

    public static void main(String[] args) {

        blockchain = new Blockchain();
        blockchain.setBlockList(blockListDemo());

        addBlockToBlockListTask();

        Properties properties = loadProperties();
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
    public void start(Stage primaryStage) throws Exception {
        AppGUI gui = new AppGUI(primaryStage);
        gui.setBlockchain(blockchain);
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
