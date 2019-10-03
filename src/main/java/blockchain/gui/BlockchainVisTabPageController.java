package blockchain.gui;

import blockchain.model.Block;
import blockchain.model.SynchronizedBlockchainWrapper;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BlockchainVisTabPageController {
    private static final Logger LOGGER = Logger.getLogger(BlockchainVisTabPageController.class.getName());

    private SingleGraph bcGraph;
    @FXML
    private VBox MainVBox;

    public void init() {
        bcGraph = new SingleGraph("bcVis");

        FxViewer v = new FxViewer(bcGraph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);

        bcGraph.setAttribute("ui.antialias");
        bcGraph.setAttribute("ui.quality");
        bcGraph.setAttribute("ui.stylesheet", "url(src/main/resources/stylesheet/bcGraph.css)");
        bcGraph.setAutoCreate(true);

        v.enableAutoLayout();
        FxViewPanel panel = (FxViewPanel) v.addDefaultView(false, new FxGraphRenderer());
        panel.setPrefHeight(730);
        panel.setPrefWidth(1210);

        this.MainVBox.getChildren().add(panel);

        Thread thread = new Thread(() -> drawGraph());
        thread.start();
    }

    private void drawGraph() {
        while (true) {
            try {
                Map<String, Block> blocks = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB());

                for (Block block : blocks.values()) {
                    String curr = block.getCurrentHash();
                    String prev = block.getPreviousHash();

                    if (prev != "0") {
                        addEdge(prev, curr);
                    } else {
                        if (bcGraph.getNode(curr) == null)
                            bcGraph.addNode(curr);
                    }

                    Node node = bcGraph.getNode(curr);
                    node.removeAttribute("ui.class");
                    node.setAttribute("ui.label", "block-" + block.getIndex());
                }

                List<Block> mainBranch = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getMainBranch());
                for (Block block : mainBranch)
                    bcGraph.getNode(block.getCurrentHash()).setAttribute("ui.class", "mainBranch");

            } catch (Exception e) {
                LOGGER.warning("Error while generating blockchain graph");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void addEdge(String prev, String curr) {
        if (bcGraph.getNode(prev) == null)
            bcGraph.addNode(prev);
        if (bcGraph.getNode(curr) == null)
            bcGraph.addNode(curr);
        if (bcGraph.getEdge(prev + "-" + curr) == null)
            bcGraph.addEdge(prev + "-" + curr, prev, curr, true);
    }

}
