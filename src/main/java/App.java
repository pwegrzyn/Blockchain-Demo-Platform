import blockchain.config.Configuration;
import blockchain.crypto.ECDSA;
import blockchain.model.Block;
import blockchain.model.Blockchain;
import blockchain.model.Transaction;
import blockchain.model.TransactionType;
import blockchain.net.FullNode;
import blockchain.gui.AppGUI;
import blockchain.net.WalletNode;
import blockchain.util.Utils;
import javafx.application.Application;
import javafx.stage.Stage;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {


    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String CLUSTER_NAME = "test_net";
    private static WalletNode node;
    private static Blockchain blockchain;

    public static void main(String[] args) {

        blockchain = new Blockchain();
        addBlocks();

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

    public static void addBlocks(){
        for(int i = 0; i < 40; i++){
            Transaction tx = new Transaction("transactionId" + i, TransactionType.REGULAR, new LinkedList<>(), new LinkedList<>());
            List<Transaction> txList = new LinkedList<>();
            txList.add(tx);
            Block block = new Block(i, txList, "hash" + (i - 1), i, i);
            blockchain.getBlockList().add(block);
            blockchain.getBlockHashList().add(block.getCurrentHash());
        }
        System.out.println("Add block");
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            AppGUI gui = new AppGUI(primaryStage);
            gui.setNode(node);
            gui.initApplication();
            gui.injectBlockchain(blockchain);
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
