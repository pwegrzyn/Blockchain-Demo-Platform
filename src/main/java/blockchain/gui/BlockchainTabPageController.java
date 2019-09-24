package blockchain.gui;

import blockchain.model.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class BlockchainTabPageController {

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

    private String selectedBlockHash = "";

    @FXML
    private void initialize(){

        new Thread(() -> {
            while(SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain() == null){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            setBlockchain(SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain());
        }).start();
    }

    private void updateSelectedBlock(String newSelectedHash) {
        if (newSelectedHash == null) return;
        selectedBlockHash = newSelectedHash;
        Block newBlock = getBlockchain().findBlock(newSelectedHash);
        updateTransactionList(newBlock);
        blockIndexLabel.setText("" + newBlock.getIndex());
        blockHashLabel.setText(newBlock.getCurrentHash());
        txCountLabel.setText("" + newBlock.getTransactions().size());
        prevBlockHashLabel.setText(newBlock.getPreviousHash());
        nonceLabel.setText("" + newBlock.getNonce());
        timestampLabel.setText("" + newBlock.getTimestamp());
    }

    private void updateSelectedTransaction(Transaction transaction) {
        if (transaction == null) return;
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
                if(newValue == null || newValue.equals(selectedBlockHash)) return;
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
        addTxListViewCellFactory();
        addListenerToBlockListViewSelector();
        addListenerToTxListViewSelector();
        this.hashBlockList = FXCollections.observableList(new LinkedList<>());
        this.latestBlock = blockchain.getLatestBlockObservable();
        this.blockListView.setItems(hashBlockList);

        ConcurrentMap<String, Block> latestDB = getBlockchain().getBlockDB();
        Block latest = this.latestBlock.get();
        if (latest != null) {
            do {
                this.hashBlockList.add(latest.getCurrentHash());
                if (latest.getIndex() == 0) break;
                latest = latestDB.get(latest.getPreviousHash());
            } while (latest != null && latest.getIndex() >= 0);
        }

        this.latestBlock.addListener((obs, ov, nv) -> {
            String prevSel = this.blockListView.selectionModelProperty().getValue().getSelectedItem();
            if (ov != null && nv.getCurrentHash().equals(ov.getCurrentHash()))
                return;
            ConcurrentMap<String, Block> blocksDB = getBlockchain().getBlockDB();
            Block block = nv;
            Platform.runLater(() -> hashBlockList.clear());
            do {
                Block finalBlock = block;
                Platform.runLater(() -> hashBlockList.add(finalBlock.getCurrentHash()));
                if (block.getIndex() == 0) break;
                block = blocksDB.get(block.getPreviousHash());
            } while (block != null && block.getIndex() >= 0);
            Platform.runLater(() -> {
                if (hashBlockList.contains(prevSel))
                    this.blockListView.selectionModelProperty().getValue().select(prevSel);
            });
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

    private Blockchain getBlockchain(){
        return SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain();
    }

}
