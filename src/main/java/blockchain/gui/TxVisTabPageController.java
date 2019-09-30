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

    private SingleGraph g;
    @FXML
    private VBox MainVBox;
    @FXML
    private Button updateGraphButton;

    private static String stylesheet =
            "" +
                    "graph {" +
                    "   padding: 60px;" +
                    "}" +
                    "node {" +
                    "   text-offset: -10,-3;" +
                    "}" +
                    "node.mainBranch {" +
                    "   fill-color: red;" +
                    "}";

    public void init() {
        g = new SingleGraph("blockchain");

        FxViewer v = new FxViewer(g, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);

        g.setAttribute("ui.antialias");
        g.setAttribute("ui.quality");
        g.setAttribute("ui.stylesheet", stylesheet);
        g.setAutoCreate(true);

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
            g.clear();
            List<Block> blocks = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getMainBranch());

            for (Block block : blocks) {
                for (Transaction tx : block.getTransactions()) {
                    String currTx = tx.getHash();

                    if (g.getNode(currTx) == null)
                        g.addNode(currTx);

                    for (TransactionInput input : tx.getInputs()) {
                        addEdge(input.getPreviousTransactionHash(), currTx);
                    }

                    Node node = g.getNode(currTx);
                    node.setAttribute("ui.label", tx.getId());
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Error while generating graph");
        }
    }

    private void addEdge(String prev, String curr) {
        if (g.getNode(prev) == null)
            g.addNode(prev);
        if (g.getEdge(prev + "-" + curr) == null)
            g.addEdge(prev + "-" + curr, prev, curr);
    }

}
