package blockchain.gui;

import blockchain.model.Block;
import blockchain.model.SynchronizedBlockchainWrapper;
import blockchain.model.Transaction;
import blockchain.model.TransactionInput;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.apache.commons.io.IOUtils;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class TxVisTabPageController {
    private static final Logger LOGGER = Logger.getLogger(BlockchainVisTabPageController.class.getName());
    private static final String STYLESHEET_PATH = "stylesheet/txGraph.css";

    private SingleGraph txGraph;
    @FXML
    private VBox MainVBox;

    public void init() {
        txGraph = new SingleGraph("txVis");

        FxViewer v = new FxViewer(txGraph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);

        txGraph.setAttribute("ui.antialias");
        txGraph.setAttribute("ui.quality");
        txGraph.setAttribute("ui.stylesheet", readStyleSheet());
        txGraph.setAutoCreate(true);

        v.enableAutoLayout();
        FxViewPanel panel = (FxViewPanel) v.addDefaultView(false, new FxGraphRenderer());
        panel.setPrefHeight(730);
        panel.setPrefWidth(1210);

        this.MainVBox.getChildren().add(panel);

        Thread thread = new Thread(() -> drawGraph());
        thread.start();
    }

    private String readStyleSheet() {
        try {
            InputStream inputStream = TxVisTabPageController.class.getClassLoader().getResourceAsStream(STYLESHEET_PATH);
            if(inputStream == null) throw new IOException();
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            LOGGER.warning("Could not load the stylesheet");
            return "";
        }
    }

    private void drawGraph() {
        while (true) {
            try {
                Map<String, Transaction> transactionDB = new HashMap<>();
                for (Block block : SynchronizedBlockchainWrapper.useBlockchain(b -> b.getMainBranch())) {
                    for (Transaction tx : block.getTransactions())
                        transactionDB.put(tx.getHash(), tx);
                }

                //check if existing transactions should still exist
                Stream<Node> nodes = txGraph.nodes();
                nodes.forEach(e -> {
                    if (e == null) return;
                    String label = e.getId();
                    if (transactionDB.containsKey(label)) {
                        transactionDB.remove(label);
                    } else {
                        txGraph.removeNode(e);
                    }
                });

                // add new transactions
                for (Transaction tx : transactionDB.values()) {
                    String currTx = tx.getHash();
                    Node node = txGraph.getNode(currTx);
                    if (node == null)
                        node = txGraph.addNode(currTx);
                    else
                        node = txGraph.getNode(currTx);
                    node.setAttribute("ui.label", tx.getId());
                    for (TransactionInput input : tx.getInputs()) {
                        addEdge(input.getPreviousTransactionHash(), currTx);
                    }
                }

                txGraph.edges().forEach(e -> {e.setAttribute("layout.weight", 30);});

                Thread.sleep(5000);
            } catch (Exception e) {
                LOGGER.warning("Error while generating tx graph");
            }
        }
    }

    private void addEdge(String prev, String curr) {
        if (prev.equals("")) return;
        if (txGraph.getNode(prev) == null)
            txGraph.addNode(prev);
        if (txGraph.getEdge(prev + "-" + curr) == null)
            txGraph.addEdge(prev + "-" + curr, prev, curr, true);
    }

}
