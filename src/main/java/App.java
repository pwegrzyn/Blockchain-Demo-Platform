import blockchain.config.Configuration;
import blockchain.model.*;
import blockchain.net.FullNode;
import blockchain.gui.AppGUI;
import blockchain.net.WalletNode;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {


    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {

        launch(args);
    }

    /*public static void addSampleBlocks(){

        for(int i = 0; i < 40; i++){
            TransactionInput input = new TransactionInput("prevhash", i - 1, 50.0, "fromAddress", "signature");
            TransactionOutput output = new TransactionOutput(50.0, "receiverAddress");
            List<TransactionInput> inputList = new LinkedList<>();
            inputList.add(input);
            List<TransactionOutput> outputList = new LinkedList<>();
            outputList.add(output);
            Transaction tx = new Transaction("transactionId" + i, TransactionType.REGULAR, inputList, outputList);
            List<Transaction> txList = new LinkedList<>();
            txList.add(tx);
            Block block = new Block(i, txList, "hash" + (i - 1), i, i);
            blockchain.getBlockList().add(block);
            blockchain.getBlockHashList().add(block.getCurrentHash());
        }
        System.out.println("ADD SAMPLE BLOCKS");
    }*/

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
