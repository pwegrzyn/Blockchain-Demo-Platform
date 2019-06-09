package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.config.Mode;
import blockchain.model.Blockchain;
import blockchain.net.FullNode;
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
    private static final String CLUSTER_NAME = "test_net";

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
            String header = "Input Error";
            String content = "You have not chosen the mode (Wallet or Full)";
            reportGenerateDialog(header, content, Alert.AlertType.WARNING);
            return;
        }
        if(!AutoGenerateKeysCheckbox.isSelected() && !areKeysValid()) {
            String header = "Input Error";
            String content = "The provided keys are not valid";
            reportGenerateDialog(header, content, Alert.AlertType.WARNING);
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

        // Initialize the node and blockchain
        Blockchain blockchain = new Blockchain();
        WalletNode node = null;
        switch(Configuration.getInstance().getNodeRunningMode()) {
            case FULL:
                node = new FullNode(CLUSTER_NAME, blockchain);
                break;
            case WALLET:
                node = new WalletNode(CLUSTER_NAME, blockchain);
                break;
        }

        // Pass the control to the main controller of the app
        this.stage.close();
        this.primaryController.setNode(node);
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

    private boolean areKeysValid() {
        Validator validator = new Validator();
        return validator.validateKeyPair(this.PrivateKeyInput.getCharacters().toString(),
                this.PublicKeyInput.getCharacters().toString());
    }

    private void reportGenerateDialog(String header, String content, Alert.AlertType type) {
        String title = "BDP Initialization";
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
