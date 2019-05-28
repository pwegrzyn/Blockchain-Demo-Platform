package blockchain.controller;

import blockchain.model.Block;
import blockchain.model.Blockchain;
import blockchain.net.WalletNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AppController {

    private Stage primaryStage;
    private Scene primaryScene;
    private WalletNode node;

    @FXML
    private AnchorPane mainPane;

    @FXML
    private TreeView<BlockDecorator> treeView;

    @FXML
    private Label infoLabel;

    @FXML
    private MenuItem refreshMenuItem;

    public void setPrimaryStageElements(Stage primaryStage, Scene primaryScene) {
        this.primaryStage = primaryStage;
        this.primaryScene = primaryScene;
        addHandleToRefreshMenuitem();
        primaryStage.setTitle("Blockchain Demo Platform");
    }

    public void backToMainView() {
        primaryStage.setScene(primaryScene);
        primaryStage.setTitle("Blockchain Demo Platform");
        primaryStage.show();
    }

    public void init() {
        this.initTreeView();
    }

    public void updateTreeView() {
        treeView.setVisible(false);
        initTreeView();
        treeView.setVisible(true);
    }

    public void setNode(WalletNode node) {
        this.node = node;
    }

    @FXML
    private void handleShowInfo(ActionEvent event) {
        System.out.println("Showing Info");
    }

    private void initTreeView() {

        List<Block> blockList = this.node.getBlockchain().getBlockList();
        if (blockList.size() < 1) {
            return;
        }
        Block firstBlock = blockList.get(0);
        BlockDecorator rootBlockDecorator = new BlockDecorator(firstBlock);
        TreeItem<BlockDecorator> rootItem = new TreeItem<BlockDecorator>(rootBlockDecorator);

        ArrayList<TreeItem<BlockDecorator>> blockchainTail = blockListToTreeItem(blockList.subList(1, blockList.size()));

        rootItem.getChildren().addAll(blockchainTail);

        treeView.setRoot(rootItem);
        addListenerToTreeView(treeView);
    }

    private ArrayList<TreeItem<BlockDecorator>> blockListToTreeItem(List<Block> blockList) {
        ArrayList<TreeItem<BlockDecorator>> resultList = new ArrayList<>();

        for (Block block : blockList) {
            TreeItem<BlockDecorator> item = blockToItem(block);
            resultList.add(item);
        }

        return resultList;
    }

    private TreeItem<BlockDecorator> blockToItem(Block block) {
        BlockDecorator blockDecorator = blockToBlockDecorator(block);
        return blockDecoratorToTreeItem(blockDecorator);
    }

    private TreeItem<BlockDecorator> blockDecoratorToTreeItem(BlockDecorator blockDecorator) {
        return new TreeItem<>(blockDecorator);
    }

    private BlockDecorator blockToBlockDecorator(Block block) {
        return new BlockDecorator(block);
    }

    private void addListenerToTreeView(TreeView<BlockDecorator> treeView) {
        treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem> observable, TreeItem oldValue, TreeItem newValue) {
                TreeItem item = observable.getValue();

                if (item == null) return;

                BlockDecorator value = (BlockDecorator) item.getValue();

                infoLabel.setText(value.getBlock().toString());
            }
        });
    }

    private void addHandleToRefreshMenuitem() {
        refreshMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource().equals(refreshMenuItem)) {
                    updateTreeView();
                }
            }
        });
    }
}
