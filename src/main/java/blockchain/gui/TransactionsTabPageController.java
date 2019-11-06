package blockchain.gui;

import blockchain.model.SynchronizedBlockchainWrapper;
import blockchain.model.Transaction;
import blockchain.model.TransactionInput;
import blockchain.model.TransactionOutput;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class TransactionsTabPageController {

    private final int refreshTimeInSeconds = 1;

    @FXML
    private TableView transactionsTableView;

    @FXML
    private TableView inputsTableView;

    @FXML
    private TableView outputsTableView;

    private Transaction selectedTransaction = null;

    void init() {
        initTransactionsTableView();
        initInputsTableView();
        initOutputsTableView();

        new Thread(() -> {
            while(SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain() == null){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            updateTransactions();
            Timeline updateControlsTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshTimeInSeconds), event -> updateTransactions()));
            updateControlsTimeline.setCycleCount(Timeline.INDEFINITE);
            updateControlsTimeline.play();
        }).start();
    }

    private void updateTransactions() {
        Queue<Transaction> unconfirmedTransactions = SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain().getUnconfirmedTransactions();

        if(!unconfirmedTransactions.contains(selectedTransaction)){
            selectedTransaction = null;
            updateInputs(Collections.emptyList());
            updateOutputs(Collections.emptyList());
        }

        transactionsTableView.setItems(FXCollections.observableArrayList(unconfirmedTransactions));
    }

    private void updateInputsAndOutputsBasedOnSelectedTransaction(Transaction selectedTransaction) {
        updateInputs(selectedTransaction.getInputs());
        updateOutputs(selectedTransaction.getOutputs());
    }

    private void updateInputs(List<TransactionInput> inputs){
        inputsTableView.setItems(FXCollections.observableArrayList(inputs));
    }

    private void updateOutputs(List<TransactionOutput> outputs){
        outputsTableView.setItems(FXCollections.observableArrayList(outputs));
    }

    private void initTransactionsTableView() {
        transactionsTableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Transaction>() {
            @Override
            public void changed(ObservableValue<? extends Transaction> observable, Transaction oldValue, Transaction newValue) {
                if(newValue == null) return;
                selectedTransaction = newValue;
                updateInputsAndOutputsBasedOnSelectedTransaction(selectedTransaction);
            }
        });

        ObservableList<TableColumn<Transaction, String>> columns = transactionsTableView.getColumns();
        TableColumn<Transaction, String> indexColumn = columns.get(0);
        TableColumn<Transaction, String> hashColumn = columns.get(1);

        indexColumn.setCellValueFactory(new PropertyValueFactory<Transaction, String>("id"));
        hashColumn.setCellValueFactory(new PropertyValueFactory<Transaction, String>("hash"));
    }

    private void initInputsTableView() {
        ObservableList<TableColumn<TransactionInput, String>> columns = inputsTableView.getColumns();
        TableColumn<TransactionInput, String> usedTxHashColumn = columns.get(0);
        TableColumn<TransactionInput, String> indexColumn = columns.get(1);
        TableColumn<TransactionInput, String> senderColumn = columns.get(2);

        usedTxHashColumn.setCellValueFactory(new PropertyValueFactory<TransactionInput, String>("previousTransactionHash"));
        indexColumn.setCellValueFactory(new PropertyValueFactory<TransactionInput, String>("previousTransactionOutputIndex"));
        senderColumn.setCellValueFactory(new PropertyValueFactory<TransactionInput, String>("fromAddress"));
        senderColumn.setCellFactory(column -> new TableCell<TransactionInput, String>()
        {
            @Override
            protected void updateItem(String item, boolean empty)
            {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item.equals("0") || item.equals("1") || item.equals("2")) {
                    setText("<system>");
                } else {
                    setText(item);
                }
            }
        });
    }

    private void initOutputsTableView() {
        ObservableList<TableColumn<TransactionOutput, String>> columns = outputsTableView.getColumns();
        TableColumn<TransactionOutput, String> idColumn = columns.get(0);
        TableColumn<TransactionOutput, String> amountColumn = columns.get(1);
        TableColumn<TransactionOutput, String> receiverColumn = columns.get(2);

        idColumn.setCellValueFactory(new PropertyValueFactory<TransactionOutput, String>("uuid"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<TransactionOutput, String>("amount"));
        receiverColumn.setCellValueFactory(new PropertyValueFactory<TransactionOutput, String>("receiverAddress"));
    }

}
