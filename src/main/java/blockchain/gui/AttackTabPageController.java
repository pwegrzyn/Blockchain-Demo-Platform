package blockchain.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.util.logging.Logger;

public class AttackTabPageController {

    private static final Logger logger = Logger.getLogger(AttackTabPageController.class.getName());

    @FXML
    AnchorPane attackContainer;
    @FXML
    Label idleInfoText;
    @FXML
    Hyperlink startAttackHyperlink;

    public void init() {

        this.attackContainer.setVisible(false);
        this.attackContainer.setDisable(true);

    }

    public void startNewAttack() {

        this.idleInfoText.setVisible(false);
        this.idleInfoText.setDisable(true);
        this.startAttackHyperlink.setVisible(false);
        this.startAttackHyperlink.setDisable(true);
        this.attackContainer.setVisible(true);
        this.attackContainer.setDisable(false);


    }

}
