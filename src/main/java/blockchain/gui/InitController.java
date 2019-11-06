package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.config.Mode;
import blockchain.crypto.ECDSA;
import blockchain.model.Blockchain;
import blockchain.model.SynchronizedBlockchainWrapper;
import blockchain.net.FullNode;
import blockchain.net.WalletNode;
import blockchain.protocol.Validator;
import blockchain.util.Utils;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;


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
        this.AutoGenerateKeysCheckbox.setSelected(true);
        this.ModeChoiceBox.setValue(Mode.FULL);
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
    private void handleOkButton(ActionEvent event) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, UnsupportedEncodingException, SignatureException, InvalidKeyException {
        if(ModeChoiceBox.getValue() == null) {
            String header = "Input Error";
            String content = "You have not chosen the mode (Wallet or Full)";
            showAlert(header, content, Alert.AlertType.WARNING);
            return;
        }
        if(!AutoGenerateKeysCheckbox.isSelected() && !areKeysValid()) {
            String header = "Input Error";
            String content = "The provided keys are not valid";
            showAlert(header, content, Alert.AlertType.WARNING);
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

        // Generate keys if necessary
        if (config.isShouldAutoGenerateKeys()) {
            ECDSA ecdsa = new ECDSA();
            KeyPair keyPair = ecdsa.generateKeyPair();
            config.setPublicKey(Utils.bytesToHexStr(keyPair.getPublic().getEncoded()));
            config.setPrivateKey(Utils.bytesToHexStr(keyPair.getPrivate().getEncoded()));
        }

        // Initialize the node and blockchain (do it in a separate thread so as to not stall the main JavaFX thread)
        ProgressForm pForm = new ProgressForm();
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws InterruptedException {

                // Step 1
                Thread.sleep(200);
                // Actually create the blockchain object
                SynchronizedBlockchainWrapper.setBlockchain(new Blockchain());
                updateProgress(1,4);

                // Step 2
                // Init the connection with the cluster
                WalletNode node = null;
                switch(Configuration.getInstance().getNodeRunningMode()) {
                    case FULL:
                        node = new FullNode(CLUSTER_NAME);
                        break;
                    case WALLET:
                        node = new WalletNode(CLUSTER_NAME);
                        break;
                }
                updateProgress(2, 4);

                // Step 3
                Thread.sleep(500);
                InitController.this.primaryController.setNode(node);
                updateProgress(3, 4);

                // Step 4
                Thread.sleep(200);
                // Initialize the main app view
                InitController.this.primaryController.init();
                updateProgress(4, 4);
                return null ;
            }
        };

        pForm.activateProgressBar(task);
        task.setOnSucceeded(e -> {
            pForm.getDialogStage().close();
            this.stage.close();
            this.primaryController.backToMainView();
        });
        pForm.getDialogStage().show();
        new Thread(task).start();
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

    private void showAlert(String header, String content, Alert.AlertType type) {
        String title = "BDP Initialization";
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Initialization Progress Stage
    private static class ProgressForm {

        private final Stage dialogStage;
        private final ProgressBar pb = new ProgressBar();

        public ProgressForm() {
            dialogStage = new Stage();
            dialogStage.setMinHeight(200);
            dialogStage.setMinWidth(400);
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.setResizable(false);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Please wait");

            final Label label = new Label();
            label.setText("Establishing connections with other nodes...");
            label.setFont(new Font(15));

            pb.setProgress(-1F);

            final VBox vb = new VBox();
            vb.setSpacing(15);
            vb.setAlignment(Pos.CENTER);
            vb.getChildren().addAll(label, pb);

            Scene scene = new Scene(vb);
            dialogStage.setScene(scene);
        }

        public void activateProgressBar(final Task<?> task)  {
            pb.progressProperty().bind(task.progressProperty());
            dialogStage.show();
        }

        public Stage getDialogStage() {
            return dialogStage;
        }
    }

}
