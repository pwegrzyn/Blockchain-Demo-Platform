package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.config.Mode;
import blockchain.model.*;
import blockchain.net.Node;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;


public class SummaryTabPageController {

    @FXML private ListView<String> usersListView;
    @FXML private Label nodeTypeLabel;
    @FXML private Label userCountLabel;
    @FXML private Label blockCountLabel;
    @FXML private Label transactionCountLabel;
    @FXML private Label currencyAmountLabel;
    @FXML private TextField publicKeyTextField;
    @FXML private TextField privateKeyTextField;

    private static final Logger LOGGER = Logger.getLogger(SummaryTabPageController.class.getName());
    private Node node;
    private Configuration config;
    private static final long refreshTimeInSeconds = 1;

    public void setNode(Node node){
        this.node = node;
    }

    public void init(){
        this.config = Configuration.getInstance();
        this.publicKeyTextField.setText(config.getPublicKey());
        this.privateKeyTextField.setText(config.getPrivateKey());
        this.nodeTypeLabel.setText(Configuration.getInstance().getNodeRunningMode() == Mode.WALLET ? "Wallet node" : "Full node");

        // Copy public address to clipboard on selection
        this.usersListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String newValue = usersListView.getSelectionModel().getSelectedItem();
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(newValue);
                clipboard.setContent(content);
            }
        });


        updateDynamicControls();
        // Init refreshing thread
        // WARNING: This method is super-hacky, and should be avoided
        Timeline updateControlsTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshTimeInSeconds), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                updateDynamicControls();
            }
        }));
        updateControlsTimeline.setCycleCount(Timeline.INDEFINITE);
        updateControlsTimeline.play();
    }

    private void updateDynamicControls() {
        String currentUserCount = calculateUserCountByView().toString();
        if (!currentUserCount.equals("-1")) {
            this.userCountLabel.setText(currentUserCount);
        }

        this.blockCountLabel.setText(calculateBlockCount().toString());
        this.transactionCountLabel.setText(calculateTransactionCount().toString());
        this.currencyAmountLabel.setText(calculateCurrencyAmount().toString());

        List<String> connectedUsers = this.node.getConnectedNodes();
        if (connectedUsers != null) {
            String myAddr = null;
            for (String user : connectedUsers) {
                if (user.equals(Configuration.getInstance().getPublicKey())) {
                    myAddr = user;
                    break;
                }
            }
            connectedUsers.remove(myAddr);
            this.usersListView.setItems(FXCollections.observableArrayList(connectedUsers));
        }
    }

    private Integer calculateUserCountByActions() {
        Integer users = 0;
        Set<String> userSet = new HashSet<>();

        for(Block block : SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain().getMainBranch()){
            for(Transaction transaction : block.getTransactions()){
                for(TransactionOutput transactionOutput : transaction.getOutputs()){
                    String newUser = transactionOutput.getReceiverAddress();

                    if(!userSet.contains(newUser)){
                        userSet.add(newUser);
                        users++;
                    }
                }
            }
        }
        return users;
    }

    private Integer calculateUserCountByView() {
        return this.node.countNodes();
    }

    private Integer calculateBlockCount(){
        return SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain().getBlockDB().values().size();
    }

    private Integer calculateTransactionCount(){
        Integer transactions = 0;

        for(Block block : SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain().getMainBranch()){
            transactions += block.getTransactions().size();
        }

        return transactions;
    }

    private BigDecimal calculateCurrencyAmount(){
        Map<Transaction, List<Integer>> unusedTransactions = new HashMap<>();
        Map<String, Transaction> hashTransactionMap = new HashMap<>();

        List<Block> currentBlockchain = SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain().getMainBranch();
        Collections.reverse(currentBlockchain);
        for(Block block : currentBlockchain) {
            for(Transaction transaction : block.getTransactions()){
                hashTransactionMap.put(transaction.getHash(), transaction);

                List<Integer> indexes = new LinkedList<>();

                for(int i = 0; i < transaction.getOutputs().size(); i++){
                    indexes.add(i);
                }

                unusedTransactions.put(transaction, indexes);

                for(TransactionInput transactionInput : transaction.getInputs()){
                    String referencedHash = transactionInput.getPreviousTransactionHash();
                    Transaction referencedTransaction = hashTransactionMap.get(referencedHash);

                    if(unusedTransactions.containsKey(referencedTransaction)){
                        Integer indexToBeRemoved = transactionInput.getPreviousTransactionOutputIndex();
                        unusedTransactions.get(referencedTransaction).remove(indexToBeRemoved);
                    }
                }
            }
        }

        BigDecimal sum = new BigDecimal(0.0);

        for(Map.Entry<Transaction, List<Integer>> entry : unusedTransactions.entrySet()){
            for(Integer i : entry.getValue()){
                sum = sum.add(entry.getKey().getOutputs().get(i).getAmount());
            }
        }

        return sum;
    }

}
