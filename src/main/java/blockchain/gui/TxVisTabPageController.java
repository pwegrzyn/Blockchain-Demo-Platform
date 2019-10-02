package blockchain.gui;

import blockchain.model.Block;
import blockchain.model.SynchronizedBlockchainWrapper;
import blockchain.model.Transaction;
import blockchain.model.TransactionInput;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;

import java.util.List;
import java.util.logging.Logger;


public class TxVisTabPageController {
    private static final Logger LOGGER = Logger.getLogger(BlockchainVisTabPageController.class.getName());

    private SingleGraph txGraph;
    @FXML
    private VBox MainVBox;
    @FXML
    private Button updateGraphButton;

    public void init() {
        txGraph = new SingleGraph("txVis");
        FxViewer v = new FxViewer(txGraph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);

        v.enableAutoLayout();
        FxViewPanel panel = (FxViewPanel) v.addDefaultView(false, new FxGraphRenderer());
        panel.setPrefHeight(680);
        panel.setPrefWidth(1210);

        this.MainVBox.getChildren().add(panel);
        this.updateGraphButton.setOnAction(e -> drawGraph());

        drawGraph();
    }

    private void drawGraph() {
        try {
            txGraph.clear();
            txGraph.setAttribute("ui.antialias");
            txGraph.setAttribute("ui.quality");
            txGraph.setAttribute("ui.stylesheet", "url(src/main/resources/stylesheet/txGraph.css)");

            List<Block> blocks = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getMainBranch());

            for (Block block : blocks) {
                for (Transaction tx : block.getTransactions()) {
                    String currTx = tx.getHash();

                    if (txGraph.getNode(currTx) == null)
                        txGraph.addNode(currTx);

                    for (TransactionInput input : tx.getInputs()) {
                        addEdge(input.getPreviousTransactionHash(), currTx);
                    }

                    Node node = txGraph.getNode(currTx);
                    node.setAttribute("ui.label", tx.getId());
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error while generating tx graph");
        }
    }

    private void addEdge(String prev, String curr) {
        if (txGraph.getNode(prev) == null)
            txGraph.addNode(prev);
        if (txGraph.getEdge(prev + "-" + curr) == null)
            txGraph.addEdge(prev + "-" + curr, prev, curr, true);
    }

}
