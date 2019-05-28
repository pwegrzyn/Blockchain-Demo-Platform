package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.config.Mode;
import blockchain.model.Blockchain;
import blockchain.net.WalletNode;
import blockchain.protocol.Validator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;


public class InitController {

    private Stage stage;
    private AppController primaryController;
    private Blockchain blockchain;
    private WalletNode node;

    public void init() {
        this.ModeChoiceBox.setItems(FXCollections.observableArrayList(Mode.WALLET, Mode.FULL));
        this.AutoGenerateKeysCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue){
                InitController.this.PublicKeyInput.setDisable(true);
                InitController.this.PublicKeyInput.setText("");
                InitController.this.PrivateKeyInput.setDisable(true);
                InitController.this.PrivateKeyInput.setText("");
            } else {
                InitController.this.PublicKeyInput.setDisable(false);
                InitController.this.PrivateKeyInput.setDisable(false);
            }
        });
    }

    @FXML
    private CheckBox AutoGenerateKeysCheckbox;

    @FXML
    private ChoiceBox<Mode> ModeChoiceBox;

    @FXML
    private TextField PrivateKeyInput;

    @FXML
    private TextField PublicKeyInput;

    @FXML
    private void handleOkButton(ActionEvent event) {
        if(ModeChoiceBox.getValue() == null) {
            return;
        }
        if(!AutoGenerateKeysCheckbox.isSelected() && !areKeysValid()) {
            return;
        }

        Configuration config = Configuration.getInstance();
        if(AutoGenerateKeysCheckbox.isSelected()) {
            config.setShouldAutoGenerateKeys(true);
        } else {
            config.setShouldAutoGenerateKeys(false);
            config.setPublicKey(PublicKeyInput.getCharacters().toString());
            config.setPrivateKey(PrivateKeyInput.getCharacters().toString());
        }
        config.setNodeRunningMode(this.ModeChoiceBox.getValue());

        this.stage.close();
        this.primaryController.setNode(this.node);
        this.primaryController.init();
        this.primaryController.backToMainView();
    }

    @FXML
    private void handleCancelButton(ActionEvent event) {
        System.exit(0);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setPrimaryController(AppController primaryController) {
        this.primaryController = primaryController;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void setNode(WalletNode node) { this.node = node; }

    private boolean areKeysValid() {
        Validator validator = new Validator();
        return validator.validateKeyPair(this.PrivateKeyInput.getCharacters().toString(),
                this.PublicKeyInput.getCharacters().toString());
    }
}
