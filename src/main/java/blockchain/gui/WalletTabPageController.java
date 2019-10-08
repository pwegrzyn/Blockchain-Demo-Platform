package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.crypto.ECDSA;
import blockchain.model.*;
import blockchain.net.WalletNode;
import blockchain.util.Utils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// TODO add logging to methods

public class WalletTabPageController {

    private static final Logger logger = Logger.getLogger(WalletTabPageController.class.getName());
    private final ECDSA ecdsa = new ECDSA();

    @FXML private TableView inputsTableView;
    @FXML private Label balanceLabel;
    @FXML private TextField transactionAddressLabel;
    @FXML private TextField transactionAmountLabel;
    @FXML private TextField transactionFee;
    @FXML private Button addTransactionButton;

    private LinkedList<Transaction> observableRemainingTransactions;

    private LinkedList<Transaction> modelRemainingTransactions;

    private WalletNode node;

    private Configuration configuration = Configuration.getInstance();

    private long refreshTimeInSeconds = 1;

    public void init(){
        initializeTransactionsTable();
        setAddTransactionHandler();
        addBalanceRefresher();
    }

    private void setAddTransactionHandler() {
        addTransactionButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if(isNewTransactionIncorrect()) return;

                modelRemainingTransactions = myRemainingTransactions();
                String transactionId = Utils.generateRandomString(32);
                TransactionType type = TransactionType.REGULAR;
                List<TransactionInput> inputList = selectInputTransactions();
                double spentAmount = amountOfInputTransactions(inputList);
                List<TransactionOutput> outputList = generateOutputTransactions(spentAmount);

                Transaction newTransaction = new Transaction(transactionId, type, inputList, outputList);
                node.broadcastNewTransaction(newTransaction);
                SynchronizedBlockchainWrapper.useBlockchain(blockchain -> blockchain.getUnconfirmedTransactions().add(newTransaction));
            }});
    }

    private boolean isNewTransactionIncorrect() {
        return isAddressIncorrect() || isAmountIncorrect() || isFeeIncorrect();
    }

    private boolean isAddressIncorrect() {

        if(transactionAddressLabel.getText().length() == 0){
            showAlert("New Transaction Error", "New transaction address field is empty!", Alert.AlertType.WARNING);
            return true;
        }

        // TODO Check transaction

        return false;
    }

    private boolean isAmountIncorrect() {
        if(transactionAmountLabel.getText().length() == 0){
            showAlert("New Transaction Error", "New transaction amount field is empty!", Alert.AlertType.WARNING);
            return true;
        }

        double newTransactionAmount = Double.parseDouble(transactionAmountLabel.getText());

        if(newTransactionAmount <= 0){
            showAlert("New Transaction Error", "New transaction amount cannot be lower or equal to 0!", Alert.AlertType.WARNING);
            return true;
        }

        if(getBalance() < newTransactionAmount){
            showAlert("New Transaction Error", "Cannot create transaction that costs more than you own!", Alert.AlertType.WARNING);
            return true;
        }

        return false;
    }

    private boolean isFeeIncorrect() {
        if (this.transactionFee.getText().length() == 0) {
            return false;
        }

        try {
            double newFeeAmount = Double.parseDouble(transactionFee.getText());
        } catch (NumberFormatException e) {
            showAlert("New Transaction Error", "New transaction fee is not a valid number!", Alert.AlertType.WARNING);
            return true;
        }

        return false;

        // TODO check fee amount
    }

    private double amountOfInputTransactions(List<TransactionInput> inputList) {
        Map<String, Double> hashAmountTransactionsMap =
                myRemainingTransactions()
                        .stream()
                        .collect(Collectors.toMap(Transaction::getHash,
                                t -> getOutputTransactionsAddressedToMeForGivenTransaction(t).getAmount()));

        return inputList
                .stream()
                .map(input -> hashAmountTransactionsMap.get(input.getPreviousTransactionHash()))
                .reduce((x, y) -> x + y)
                .orElse(0.0);
    }

    private List<TransactionOutput> generateOutputTransactions(double spentAmount) {
        double transactionCost = Double.parseDouble(transactionAmountLabel.getText());
        double remainingAmount = spentAmount - transactionCost;

         return Arrays.asList(
                new TransactionOutput(transactionCost, transactionAddressLabel.getText()),
                new TransactionOutput(remainingAmount, configuration.getPublicKey())
        );
    }

    private synchronized List<TransactionInput> selectInputTransactions() {
        double transactionCost = Double.parseDouble(transactionAmountLabel.getText());

        LinkedList<Transaction> gatheredTransactionsToBeUsed = new LinkedList<>();

        while(valueOfTransactionsToMe(gatheredTransactionsToBeUsed) < transactionCost){
            gatheredTransactionsToBeUsed.add(modelRemainingTransactions.pollFirst());
        }

        return gatheredTransactionsToBeUsed
                .stream()
                .map(
                        t -> {
                            try {
                                return new TransactionInput(
                                        t.getHash(),
                                        t.getOutputs().indexOf(getOutputTransactionsAddressedToMeForGivenTransaction(t)),
                                        t.getCreatorAddr(),
                                        generateTXSignature(t));
                            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException
                                    | UnsupportedEncodingException | SignatureException | InvalidKeyException e) {
                                logger.warning("ECDSA exception during signature generation in wallet!");
                                return null;
                            }
                        })
                .collect(Collectors.toList());
    }

    private String generateTXSignature(Transaction transaction) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException,
            UnsupportedEncodingException, SignatureException, InvalidKeyException {
        String txInputHash = TransactionInput.calculateHash(transaction.getHash(),
                transaction.getOutputs().indexOf(getOutputTransactionsAddressedToMeForGivenTransaction(transaction)), transaction.getCreatorAddr());
        PrivateKey myPrivateKey = ecdsa.strToPrivateKey(configuration.getPrivateKey());
        byte[] generatedSignature = ecdsa.generateSignature(txInputHash, myPrivateKey);
        return Utils.bytesToHexStr(generatedSignature);
    }

    private double valueOfTransactionsToMe(LinkedList<Transaction> gatheredTransactions) {
        return gatheredTransactions
                .stream()
                .map(
                        t -> t.getOutputs()
                                .stream()
                                .filter(out -> out.getReceiverAddress().equals(configuration.getPublicKey()) || out.getReceiverAddress().equals("0"))
                                .map(TransactionOutput::getAmount)
                                .reduce((x, y) -> x + y)
                                .orElse(0.0))
                .reduce((x, y) -> x + y)
                .orElse(0.0);
    }

    private TransactionOutput getOutputTransactionsAddressedToMeForGivenTransaction(Transaction transaction){
        return transaction
                .getOutputs()
                .stream()
                .filter(t -> t.getReceiverAddress().equals(configuration.getPublicKey()) || t.getReceiverAddress().equals("0"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find transaction output to this client"));
    }

    private void addBalanceRefresher(){
        Timeline updateControlsTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshTimeInSeconds), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                balanceLabel.setText("" + getBalance());
                observableRemainingTransactions = myRemainingTransactions();
            }
        }));
        updateControlsTimeline.setCycleCount(Timeline.INDEFINITE);
        updateControlsTimeline.play();
    }

    private synchronized LinkedList<Transaction> myRemainingTransactions(){
        String userPublicKey = configuration.getPublicKey();

        LinkedList<Transaction> allTransactionsToMe = new LinkedList<>();

        // Add transactions addressed to me

        for(Block block : SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain().getMainBranch()){
            for(Transaction transaction : block.getTransactions()){
                if(transaction.getOutputs().stream().anyMatch(transactionOutput -> transactionOutput.getReceiverAddress().equals(userPublicKey)
                    || transactionOutput.getReceiverAddress().equals("0"))){ // Genesis block
                    allTransactionsToMe.add(transaction);
                }

                transaction.getInputs()
                        .stream()
                        .map(TransactionInput::getPreviousTransactionHash)
                        .forEach(hash -> {
                            LinkedList<Transaction> myUsedTransactions = new LinkedList<>();
                            for(Transaction tx : allTransactionsToMe){
                                if(tx.getHash().equals(hash)){
                                    myUsedTransactions.add(tx);
                                }
                            }
                            allTransactionsToMe.removeAll(myUsedTransactions);
                        });
            }
        }

        return allTransactionsToMe;
    }

    private double getBalance() {
        String userPublicKey = configuration.getPublicKey();
        double result = 0.0;


        for(Transaction transaction : observableRemainingTransactions){
            for(TransactionOutput tx : transaction.getOutputs()){
                if(tx.getReceiverAddress().equals(userPublicKey) || tx.getReceiverAddress().equals("0")){
                    result += tx.getAmount();
                }
            }
        }
        return result;
    }

    private void initializeTransactionsTable(){
        String userPublicKey = configuration.getPublicKey();

        ObservableList<TableColumn<Transaction, String>> columns = inputsTableView.getColumns();
        TableColumn<Transaction, String> txIndexColumn = columns.get(0);
        TableColumn<Transaction, String> txValueColumn = columns.get(1);
        txIndexColumn.setCellValueFactory(new PropertyValueFactory<Transaction, String>("hash"));
        txValueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue()
                .getOutputs()
                .stream()
                .filter(tx -> tx.getReceiverAddress()
                        .equals(userPublicKey))
                .findFirst()
                .get()
                .getAmount())));


        inputsTableView.setItems(FXCollections.observableArrayList(observableRemainingTransactions));
    }

    public void setNode(WalletNode node) {
        this.node = node;
    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        String title = "Wallet";
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
