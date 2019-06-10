package blockchain.gui;

import blockchain.model.Blockchain;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class BlockchainVisTabPageController {

    private Blockchain blockchain;
    @FXML private VBox MainVBox;

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void init() {

    }

}
