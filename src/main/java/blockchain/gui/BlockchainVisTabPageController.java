package blockchain.gui;

import blockchain.model.Block;
import blockchain.model.SynchronizedBlockchainWrapper;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BlockchainVisTabPageController {
    private static final Logger LOGGER = Logger.getLogger(BlockchainVisTabPageController.class.getName());
    private static final String STYLESHEET_PATH = "stylesheet/bcGraph.css";

    private SingleGraph bcGraph;
    @FXML
    private VBox MainVBox;

    public void init() {
        bcGraph = new SingleGraph("bcVis");

        FxViewer v = new FxViewer(bcGraph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);

        bcGraph.setAttribute("ui.antialias");
        bcGraph.setAttribute("ui.quality");
        bcGraph.setAttribute("ui.stylesheet", readStyleSheet());
        bcGraph.setAutoCreate(true);

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
            InputStream inputStream = BlockchainVisTabPageController.class.getClassLoader().getResourceAsStream(STYLESHEET_PATH);
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

                Thread.sleep(5000);
            } catch (Exception e) {
                LOGGER.warning("Error while generating blockchain graph");
            }
        }
    }

    private void addEdge(String prev, String curr) {
        if (bcGraph.getNode(curr) == null)
            bcGraph.addNode(curr);
        if (prev.equals("0")) return;
        if (bcGraph.getNode(prev) == null)
            bcGraph.addNode(prev);
        if (bcGraph.getEdge(prev + "-" + curr) == null)
            bcGraph.addEdge(prev + "-" + curr, prev, curr, true);
    }

}
