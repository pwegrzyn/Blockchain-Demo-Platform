package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.model.*;
import blockchain.net.WalletNode;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class WalletTabPageController {

    @FXML private TableView inputsTableView;
    @FXML private Label balanceLabel;
    @FXML private TextField newTransactionAddressLabel;
    @FXML private TextField newTransactionAmountLabel;
    @FXML private Button newTransactionButton;

    private LinkedList<Transaction> myRemainingTransactions;

    private Double balance;

    private WalletNode node;

    private Blockchain blockchain;

    private Configuration configuration = Configuration.getInstance();

    public void init(){
        newTransactionButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                String transactionId = "transaction";
                TransactionType type = TransactionType.REGULAR;
                List<TransactionInput> inputList = selectInputTransactions();
                List<TransactionOutput> outputList = generateOutputTransactions();

                Transaction newTransaction = new Transaction(transactionId, type, inputList, outputList);
                node.broadcastNewTransaction(newTransaction);
            }});
        initializeRemainingTransactionList();
        initializeUserBalanceLabel();
        initializeTransactionsTable();
    }

    private List<TransactionOutput> generateOutputTransactions() {
        return null;
    }

    private List<TransactionInput> selectInputTransactions() {
        return null;
    }

    private void initializeUserBalanceLabel(){
        String userPublicKey = configuration.getPublicKey();
        Double result = 0.0;

        for(Transaction transaction : myRemainingTransactions){
            for(TransactionOutput tx : transaction.getOutputs()){
                if(tx.getReceiverAddress().equals(userPublicKey)){
                    result += tx.getAmount();
                }
            }
        }

        balance = result;
        balanceLabel.setText(balance.toString());
    }

    private void initializeRemainingTransactionList(){
        String userPublicKey = configuration.getPublicKey();

        LinkedList<Transaction> allTransactionsToMe = new LinkedList<>();

        // Add transactions addressed to me

        for(Block block : blockchain.getMainBranch()){
            for(Transaction transaction : block.getTransactions()){
                if(transaction.getOutputs().stream().anyMatch(transactionOutput -> transactionOutput.getReceiverAddress().equals(userPublicKey))){
                    allTransactionsToMe.add(transaction);
                }
                transaction.getInputs()
                        .stream()
                        .map(TransactionInput::getPreviousTransactionHash)
                        .forEach(hash -> {
                            LinkedList<Transaction> toBeRemovedTransactions = new LinkedList<>();
                            for(Transaction tx : allTransactionsToMe){
                                if(tx.getHash().equals(hash)){
                                    toBeRemovedTransactions.add(tx);
                                }
                            }
                            allTransactionsToMe.removeAll(toBeRemovedTransactions);
                        });
            }
        }

        myRemainingTransactions = allTransactionsToMe;

        /*List<Transaction> transactions =
                blockchain
                        .getBlockList()
                        .stream()
                        .flatMap(x -> x.getTransactions().stream())
                        .filter(transaction -> transaction
                                    .getOutputs()
                                    .stream()
                                    .anyMatch(transactionOutput -> transactionOutput.getReceiverAddress().equals(userPublicKey)))

        .collect(Collectors.toList());*/
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


        inputsTableView.setItems(FXCollections.observableArrayList(myRemainingTransactions));

    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void setNode(WalletNode node) {
        this.node = node;
    }
}
