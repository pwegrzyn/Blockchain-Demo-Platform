package blockchain.gui;

import blockchain.model.Blockchain;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;


public class TxVisTabPageController {

    private Blockchain blockchain;
    @FXML private VBox MainVBox;

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void init() {
        demo();
    }

    private void demo() {
        try {
            MultiGraph g = new MultiGraph("mg");
            FxViewer v = new FxViewer(g, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
            DorogovtsevMendesGenerator gen = new DorogovtsevMendesGenerator();

            g.setAttribute("ui.antialias");
            g.setAttribute("ui.quality");
            g.setAttribute("ui.stylesheet", "graph {padding: 60px;}");

            v.enableAutoLayout();
            FxViewPanel panel = (FxViewPanel)v.addDefaultView(false, new FxGraphRenderer());
            panel.setPrefHeight(750);
            panel.setPrefWidth(1200);

            gen.addSink(g);
            gen.begin();
            for(int i = 0 ; i < 100 ; i++)
                gen.nextEvents();
            gen.end();
            gen.removeSink(g);

            this.MainVBox.getChildren().add(panel);
        } catch (Exception e) {
            // only a demo
        }
    }

}
