package blockchain.gui;

import blockchain.model.Block;
import blockchain.model.SynchronizedBlockchainWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;

import java.util.List;
import java.util.Map;

public class BlockchainVisTabPageController {
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
            Map<String, Block> blocks = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB());

            for (Block block : blocks.values()) {
                String curr = block.getCurrentHash();
                String prev = block.getPreviousHash();

                if (prev != "0") {
                    addEdge(curr, prev);
                } else {
                    if (g.getNode(curr) == null)
                        g.addNode(curr);
                }
                Node node = g.getNode(curr);
                node.setAttribute("ui.label", block.getIndex());
            }

            List<Block> mainBranch = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getMainBranch());
            for (Block block : mainBranch)
                g.getNode(block.getCurrentHash()).setAttribute("ui.class", "mainBranch");

        } catch (Exception e) {
            System.err.println("Error while generating graph\n" + e);
        }
    }

    private void addEdge(String curr, String prev) {
        if (g.getNode(prev) == null)
            g.addNode(prev);
        if (g.getNode(curr) == null)
            g.addNode(curr);
        if (g.getEdge(curr + "-" + prev) == null)
            g.addEdge(curr + "-" + prev, curr, prev);
    }

}
