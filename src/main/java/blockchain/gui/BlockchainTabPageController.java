package blockchain.gui;

import blockchain.model.*;
import javafx.beans.Observable;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class BlockchainTabPageController {

    private Blockchain blockchain;

    private ObservableList<String> hashBlockList;
    private SimpleObjectProperty<Block> latestBlock;

    @FXML
    private ListView<String> blockListView;
    @FXML
    private ListView<Transaction> txListView;

    @FXML
    private Label blockIndexLabel;
    @FXML
    private Label blockHashLabel;
    @FXML
    private Label txCountLabel;
    @FXML
    private Label prevBlockHashLabel;
    @FXML
    private Label nonceLabel;
    @FXML
    private Label timestampLabel;

    @FXML
    private Label txIndexLabel;
    @FXML
    private Label txHashLabel;
    @FXML
    private Label txTypeLabel;
    @FXML
    private TableView txInputTxTable;
    @FXML
    private TableView txOutputTxTable;

    @FXML
    private VBox blockPropertiesPane;
    @FXML
    private AnchorPane txPropertiesPane;

    private void updateSelectedBlock(String newSelectedHash) {
        Block newBlock = blockchain.findBlock(newSelectedHash);
        updateTransactionList(newBlock);
        blockIndexLabel.setText("" + newBlock.getIndex());
        blockHashLabel.setText(newBlock.getCurrentHash());
        txCountLabel.setText("" + newBlock.getTransactions().size());
        prevBlockHashLabel.setText(newBlock.getPreviousHash());
        nonceLabel.setText("" + newBlock.getNonce());
        timestampLabel.setText("" + newBlock.getTimestamp());
    }

    private void updateSelectedTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        txIndexLabel.setText(transaction.getId());
        txHashLabel.setText(transaction.getHash());
        txTypeLabel.setText(transaction.getType().toString());
        updateTxInputTable(transaction.getInputs());
        updateTxOutputTable(transaction.getOutputs());
    }

    private void updateTxInputTable(List<TransactionInput> transactions) {
        ObservableList<TableColumn<TransactionInput, String>> columns = txInputTxTable.getColumns();
        TableColumn<TransactionInput, String> txIdColumn = columns.get(0);
        TableColumn<TransactionInput, String> txIndexColumn = columns.get(1);
        TableColumn<TransactionInput, String> txValueColumn = columns.get(2);
        txIdColumn.setCellValueFactory(new PropertyValueFactory<TransactionInput, String>("previousTransactionHash"));
        txIndexColumn.setCellValueFactory(new PropertyValueFactory<TransactionInput, String>("previousTransactionOutputIndex"));
        txValueColumn.setCellValueFactory(new PropertyValueFactory<TransactionInput, String>("amount"));
        txInputTxTable.setItems(FXCollections.observableArrayList(transactions));
    }

    private void updateTxOutputTable(List<TransactionOutput> transactions) {
        ObservableList<TableColumn<TransactionOutput, String>> columns = txOutputTxTable.getColumns();
        TableColumn<TransactionOutput, String> txIndexColumn = columns.get(0);
        TableColumn<TransactionOutput, String> txValueColumn = columns.get(1);
        txIndexColumn.setCellValueFactory(new PropertyValueFactory<TransactionOutput, String>("receiverAddress"));
        txValueColumn.setCellValueFactory(new PropertyValueFactory<TransactionOutput, String>("amount"));
        txOutputTxTable.setItems(FXCollections.observableArrayList(transactions));
    }

    private void updateTransactionList(Block block) {
        List<Transaction> transactions = block.getTransactions();
        txListView.setItems(FXCollections.observableArrayList(transactions));
        txListView.setVisible(true);
    }

    private void setTransactionInfoVisibility(boolean visible) {
        txPropertiesPane.setVisible(visible);
    }

    private void clearTransactionTableView() {
        txListView.setItems(FXCollections.observableArrayList(new LinkedList<>()));
    }

    private void setBlockInfoVisibility(boolean visible) {
        blockPropertiesPane.setVisible(visible);
    }

    private void addListenerToTxListViewSelector() {
        this.txListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Transaction>() {
            @Override
            public void changed(ObservableValue<? extends Transaction> observable, Transaction oldValue, Transaction newValue) {
                updateSelectedTransaction(newValue);
                setTransactionInfoVisibility(true);
            }
        });
    }

    private void addListenerToBlockListViewSelector() {
        this.blockListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                clearTransactionTableView();
                setTransactionInfoVisibility(false);
                updateSelectedBlock(newValue);
                setBlockInfoVisibility(true);
            }

        });
    }

    private void addTxListViewCellFactory() {
        txListView.setCellFactory(lv -> new ListCell<Transaction>() {
            @Override
            public void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item.getId());
                }
            }
        });
    }

    public void setBlockchain(Blockchain blockchain) {
        System.out.println("blockchain in setBlockchain() has " + blockchain.getBlockDB().values().size() + " blocks");
        addTxListViewCellFactory();
        addListenerToBlockListViewSelector();
        addListenerToTxListViewSelector();
        this.blockchain = blockchain;
        this.hashBlockList = FXCollections.observableList(new LinkedList<>());
        this.latestBlock = blockchain.getLatestBlockObservable();
        this.blockListView.setItems(hashBlockList);

        ConcurrentMap<String, Block> latestDB = this.blockchain.getBlockDB();
        Block latest = this.latestBlock.get();
        if (latest != null) {
            do {
                this.hashBlockList.add(latest.getCurrentHash());
                if (latest.getIndex() == 0) break;
                latest = latestDB.get(latest.getPreviousHash());
            } while (latest.getIndex() > 0);
        }

        this.latestBlock.addListener((obs, ov, nv) -> {
            System.out.println("this.latestBlock.addListener");
            if (ov != null && nv.getCurrentHash().equals(ov.getCurrentHash()))
                return;
            ConcurrentMap<String, Block> blocksDB = this.blockchain.getBlockDB();
            Block block = nv;
            this.hashBlockList.clear();
            do {
                this.hashBlockList.add(block.getCurrentHash());
                if (block.getIndex() == 0) break;
                block = blocksDB.get(block.getPreviousHash());
            } while (block.getIndex() > 0);
            System.out.println("end this.latestBlock.addListener");
        });

        this.hashBlockList.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                if (!c.next()) return;
                if (c.wasAdded()) {
                    for (String s : c.getAddedSubList()) {
                        Block blockToAdd = blockchain.findBlock(s);
                        ObservableList<String> blocklist = blockListView.getItems();
                        if (blocklist.get(blocklist.size() - 1).equals(blockToAdd.getPreviousHash())) {
                            blocklist.add(blockToAdd.getCurrentHash());
                        }
                    }
                } else if (c.wasRemoved()) {

                }
            }
        });
    }

}
