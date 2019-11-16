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
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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

    private LinkedList<Transaction> observableRemainingTransactions = new LinkedList<>();

    private LinkedList<Transaction> modelRemainingTransactions;

    private WalletNode node;

    private long refreshTimeInSeconds = 1;

    public void init() {
        initializeTransactionsTable();
        setAddTransactionHandler();
        addBalanceRefresher();
    }

    private void setAddTransactionHandler() {
        addTransactionButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                modelRemainingTransactions = myRemainingTransactions();

                if(isNewTransactionIncorrect()) return;

                String transactionId = Utils.generateRandomString(8);
                TransactionType type = TransactionType.REGULAR;
                List<TransactionInput> inputList = selectInputTransactions();

                logger.info("Inputs for new transaction: " + inputList.stream().map(x -> x.getPreviousTransactionHash() + "[" + x.getPreviousTransactionOutputIndex() + "]").collect(Collectors.joining(", ")));

                BigDecimal spentAmount = amountOfInputTransactions(inputList);
                List<TransactionOutput> outputList = generateOutputTransactions(spentAmount);

                Transaction newTransaction = new Transaction(transactionId, type, inputList, outputList);
                newTransaction.setCreator(Configuration.getInstance().getPublicKey());
                node.broadcastNewTransaction(newTransaction);
                SynchronizedBlockchainWrapper.useBlockchain(blockchain -> blockchain.getUnconfirmedTransactions().add(newTransaction));

                showAlert("New Transaction Success!", "The transaction has been broadcasted to other nodes. Please wait for miners validate it .", Alert.AlertType.INFORMATION);
                transactionAddressLabel.setText("");
                transactionAmountLabel.setText("");
                transactionFee.setText("");
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

        if (transactionAddressLabel.getText().equals(Configuration.getInstance().getPublicKey())) {
            showAlert("New Transaction Error", "Cannot send transaction to own address!", Alert.AlertType.WARNING);
            return true;
        }

        if(!transactionAddressLabel.getText().matches("[A-Za-z0-9]+")){
            showAlert("New Transaction Error", "Only alphanumeric characters allowed!", Alert.AlertType.WARNING);
            return true;
        }

        if(transactionAddressLabel.getText().length() != 176){
            showAlert("New Transaction Error", "New transaction address length is incorrect!", Alert.AlertType.WARNING);
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

        BigDecimal newTransactionAmount = new BigDecimal(0.0);
        try {
            newTransactionAmount = new BigDecimal(transactionAmountLabel.getText());
        } catch (NumberFormatException e) {
            showAlert("New Transaction Error", "New transaction amount is not a valid number!", Alert.AlertType.WARNING);
            return true;
        }

        if(newTransactionAmount.compareTo(new BigDecimal(0.0)) <= 0){
            showAlert("New Transaction Error", "New transaction amount cannot be lower or equal to 0!", Alert.AlertType.WARNING);
            return true;
        }

        if(getBalance(true).compareTo(newTransactionAmount) < 0) {
            showAlert("New Transaction Error", "Cannot create transaction that costs more than you own!", Alert.AlertType.WARNING);
            return true;
        }

        return false;
    }

    private boolean isFeeIncorrect() {
        BigDecimal newFeeAmount = new BigDecimal(0.0);
        try {
            newFeeAmount = new BigDecimal(getTransactionFee());
        } catch (NumberFormatException e) {
            showAlert("New Transaction Error", "New transaction fee is not a valid number!", Alert.AlertType.WARNING);
            return true;
        }

        if (newFeeAmount.compareTo(new BigDecimal(0)) < 0) {
            showAlert("New Transaction Error", "Fee cannot be negative!", Alert.AlertType.WARNING);
            return true;
        }

        if (newFeeAmount.add(new BigDecimal(transactionAmountLabel.getText())).compareTo(getBalance(true)) > 0) {
            showAlert("New Transaction Error", "Can't spend more than you own (including fee)!", Alert.AlertType.WARNING);
            return true;
        }

        return false;

    }

    private BigDecimal amountOfInputTransactions(List<TransactionInput> inputList) {
        Map<String, BigDecimal> hashAmountTransactionsMap =
                myRemainingTransactions()
                        .stream()
                        .collect(Collectors.toMap(Transaction::getHash,
                                t -> getOutputTransactionsAddressedToMeForGivenTransaction(t).getAmount()));

        return inputList
                .stream()
                .map(input -> hashAmountTransactionsMap.get(input.getPreviousTransactionHash()))
                .reduce((x, y) -> x.add(y))
                .orElse(new BigDecimal(0.0));
    }

    private List<TransactionOutput> generateOutputTransactions(BigDecimal spentAmount) {
        BigDecimal transactionCost = new BigDecimal(transactionAmountLabel.getText());
        BigDecimal remainingAmount = spentAmount.subtract(transactionCost).subtract(new BigDecimal(getTransactionFee()));

        List<TransactionOutput> outputs = new LinkedList<>();
        outputs.add(new TransactionOutput(transactionCost, transactionAddressLabel.getText()));

        // Return rest of founds to sender
        if (remainingAmount.compareTo(new BigDecimal(0)) != 0)
            outputs.add(new TransactionOutput(remainingAmount, Configuration.getInstance().getPublicKey()));

        return outputs;
    }

    private synchronized List<TransactionInput> selectInputTransactions() {
        BigDecimal transactionCost = new BigDecimal(transactionAmountLabel.getText()).add(new BigDecimal(getTransactionFee()));

        LinkedList<Transaction> gatheredTransactionsToBeUsed = new LinkedList<>();

        while(valueOfTransactionsToMe(gatheredTransactionsToBeUsed).compareTo(transactionCost) < 0){
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
        PrivateKey myPrivateKey = ecdsa.strToPrivateKey(Configuration.getInstance().getPrivateKey());
        byte[] generatedSignature = ecdsa.generateSignature(txInputHash, myPrivateKey);
        return Utils.bytesToHexStr(generatedSignature);
    }

    private BigDecimal valueOfTransactionsToMe(LinkedList<Transaction> gatheredTransactions) {
        return gatheredTransactions
                .stream()
                .map(
                        t -> t.getOutputs()
                                .stream()
                                .filter(out -> out.getReceiverAddress().equals(Configuration.getInstance().getPublicKey()) || out.getReceiverAddress().equals("0"))
                                .map(TransactionOutput::getAmount)
                                .reduce(BigDecimal::add)
                                .orElse(new BigDecimal(0.0)))
                .reduce(BigDecimal::add)
                .orElse(new BigDecimal(0.0));
    }

    private TransactionOutput getOutputTransactionsAddressedToMeForGivenTransaction(Transaction transaction){
        return transaction
                .getOutputs()
                .stream()
                .filter(t -> t.getReceiverAddress().equals(Configuration.getInstance().getPublicKey()) || t.getReceiverAddress().equals("0"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find transaction output to this client"));
    }

    private void addBalanceRefresher(){
        Timeline updateControlsTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshTimeInSeconds), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                observableRemainingTransactions = myRemainingTransactions();
                balanceLabel.setText("" + getBalance(false));
                inputsTableView.setItems(FXCollections.observableArrayList(observableRemainingTransactions));
            }
        }));
        updateControlsTimeline.setCycleCount(Timeline.INDEFINITE);
        updateControlsTimeline.play();
    }

    private LinkedList<Transaction> myRemainingTransactions(){
        String userPublicKey = Configuration.getInstance().getPublicKey();

        Map<TransactionOutput, Transaction> allTransactionsToMe = new HashMap<>();

        // Add transactions addressed to me
        List<Block> currentBlockchain = SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain().getMainBranch();

        for(Block block : currentBlockchain) {
            for(Transaction transaction : block.getTransactions()){
                for(TransactionOutput output : transaction.getOutputs()){
                    if(output.getReceiverAddress().equals(userPublicKey) || output.getReceiverAddress().equals("0")){
                        allTransactionsToMe.put(output, transaction);
                    }
                }
            }
        }

        Map<TransactionOutput, Transaction> result = new HashMap<>(allTransactionsToMe);

        // Delete used transactions
        for(Block block : currentBlockchain){
            for(Transaction transaction : block.getTransactions()){
                for(TransactionInput input : transaction.getInputs()){
                    String usedTxHash = input.getPreviousTransactionHash();
                    int usedTxIndex = input.getPreviousTransactionOutputIndex();

                    for(Transaction myTransaction : allTransactionsToMe.values()){
                        if(myTransaction.getHash().equals(usedTxHash)){
                            TransactionOutput usedOutput = myTransaction.getOutputs().get(usedTxIndex);
                            result.remove(usedOutput);
                        }
                    }

                }
            }
        }

        return result.values().stream().distinct().collect(Collectors.toCollection(LinkedList::new));
    }

    private BigDecimal getBalance(boolean useModel) {
        String userPublicKey = Configuration.getInstance().getPublicKey();
        BigDecimal result = new BigDecimal(0.0);

        LinkedList<Transaction> transactionsToUse;
        if (useModel) {
            transactionsToUse = modelRemainingTransactions;
        } else {
            transactionsToUse = observableRemainingTransactions;
        }
        for(Transaction transaction : transactionsToUse){
            for(TransactionOutput tx : transaction.getOutputs()){
                if(tx.getReceiverAddress().equals(userPublicKey) || tx.getReceiverAddress().equals("0")){
                    result = result.add(tx.getAmount());
                }
            }
        }
        return result;
    }

    private void initializeTransactionsTable(){
        String userPublicKey = Configuration.getInstance().getPublicKey();

        ObservableList<TableColumn<Transaction, String>> columns = inputsTableView.getColumns();
        TableColumn<Transaction, String> txIndexColumn = columns.get(0);
        TableColumn<Transaction, String> txValueColumn = columns.get(1);
        txIndexColumn.setCellValueFactory(new PropertyValueFactory<Transaction, String>("id"));
        txValueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue()
                .getOutputs()
                .stream()
                .filter(tx -> tx.getReceiverAddress()
                        .equals(userPublicKey) || tx.getReceiverAddress().equals("0"))
                .findFirst()
                .get()
                .getAmount())));


        inputsTableView.setItems(FXCollections.observableArrayList(observableRemainingTransactions));
    }

    public void setNode(WalletNode node) {
        this.node = node;
    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        String title = "Wallet Error";
        if (type.equals(Alert.AlertType.INFORMATION))
            title="Wallet Information";
        Alert alert = new Alert(type);
        try {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            java.awt.image.BufferedImage imageIcon = ImageIO.read(getClass().getClassLoader().getResource("assets/icons/coin.png"));
            alertStage.getIcons().add(SwingFXUtils.toFXImage(imageIcon, null));
        } catch (IOException e) {
            logger.warning("Error while setting icon for alert!");
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String getTransactionFee() {
        if (transactionFee.getText().length() > 0)
            return transactionFee.getText();
        return "0";
    }
}
