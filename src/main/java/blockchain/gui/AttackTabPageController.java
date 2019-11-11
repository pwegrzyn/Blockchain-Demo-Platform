package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.model.Blockchain;
import blockchain.model.SynchronizedBlockchainWrapper;
import blockchain.model.Transaction;
import blockchain.net.FullNode;
import blockchain.net.ProtocolMessage;
import blockchain.net.WalletNode;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class AttackTabPageController {

    private static final Logger logger = Logger.getLogger(AttackTabPageController.class.getName());

    @FXML
    AnchorPane foundAttacksContainer;
    @FXML
    AnchorPane newAttackContainer;
    @FXML
    AnchorPane currentAttackContainer;
    @FXML
    AnchorPane noAttacksFoundContainer;
    @FXML
    Hyperlink startAttackHyperlink;
    @FXML
    TextField subNetworkIdTextField;
    @FXML
    TextField cancelledTxTextField;
    @FXML
    Button createNewAttackButton;
    @FXML
    Button cancelNewAttackButton;
    @FXML
    Button joinAttackButton;
    @FXML
    Label foundSubnetworkIdLabel;
    @FXML
    Label foundCancelledTxIdLabel;

    private Timeline infoListener;
    private WalletNode node;
    private ExecutorService attackInformerThread;
    private ExecutorService attackInformerReceiverThread;
    private AppController appController;

    public void init() {
        // set up event handlers for all the events in this scene
        this.startAttackHyperlink.setOnMouseClicked(e -> {
            startNewAttack();
        });
        this.createNewAttackButton.setOnMouseClicked(e -> {
            createNewAttackHelper();
        });
        this.cancelNewAttackButton.setOnMouseClicked(e -> {
            this.infoListener.play();
        });
        this.joinAttackButton.setOnMouseClicked(e -> {
            showCurrentAttack();
        });

        this.foundAttacksContainer.setVisible(false);
        this.newAttackContainer.setVisible(false);
        this.currentAttackContainer.setVisible(false);
        this.noAttacksFoundContainer.setVisible(true);

        pollAttackInfo();
    }

    public void setNode(WalletNode node)  {
        this.node = node;
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    private void startNewAttack() {
        this.infoListener.pause();
        this.foundAttacksContainer.setVisible(false);
        this.newAttackContainer.setVisible(true);
        this.currentAttackContainer.setVisible(false);
        this.noAttacksFoundContainer.setVisible(false);
    }

    private void createNewAttackHelper() {
        if (!validateNewAttackParameters()) {
            return;
        }

        // Start the attack informer thread
        this.attackInformerThread = Executors.newSingleThreadExecutor();
        this.attackInformerThread.submit(new AttackInformerThread(this.subNetworkIdTextField.getText(), this.cancelledTxTextField.getText()));

        showCurrentAttack();
    }

    private void showFoundAttacks() {
        List<String> foundAttackInfo = this.node.getAttackInfoList();
        // lets assume for now that only one attack can be happening at once and that only one attack will be done in
        // the whole lifecycle of the network
        String[] attackInfoSplit = foundAttackInfo.get(0).split(";");
        this.foundCancelledTxIdLabel.setText(attackInfoSplit[1]);
        this.foundSubnetworkIdLabel.setText(attackInfoSplit[0]);

        this.foundAttacksContainer.setVisible(true);
        this.newAttackContainer.setVisible(false);
        this.currentAttackContainer.setVisible(false);
        this.noAttacksFoundContainer.setVisible(false);
    }

    private void showCurrentAttack() {
        // Start the attack informer receiver thread but only if we were not the ones who started this attack
        if (this.attackInformerThread == null) {
            List<String> foundAttackInfo = this.node.getAttackInfoList();
            String[] attackInfoSplit = foundAttackInfo.get(0).split(";");
            this.attackInformerReceiverThread = Executors.newSingleThreadExecutor();
            this.attackInformerReceiverThread.submit(new AttackInformerReceiverThread(attackInfoSplit[0], attackInfoSplit[1]));
        }

        // TODO: all the stuff associated with a an actual attack (either when starting a new one or joining an existing one) goes here;
        // TODO: here will come the code with starting a new custom miner probably
        this.appController.getMinerTabPageController().stopMiner();

        this.foundAttacksContainer.setVisible(false);
        this.newAttackContainer.setVisible(false);
        this.currentAttackContainer.setVisible(true);
        this.noAttacksFoundContainer.setVisible(false);
    }

    private void pollAttackInfo() {
         this.infoListener = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (AttackTabPageController.this.node.getAttackInfoList().isEmpty()) {
                    AttackTabPageController.this.foundAttacksContainer.setVisible(false);
                    AttackTabPageController.this.newAttackContainer.setVisible(false);
                    AttackTabPageController.this.currentAttackContainer.setVisible(false);
                    AttackTabPageController.this.noAttacksFoundContainer.setVisible(true);
                } else {
                    AttackTabPageController.this.infoListener.stop();
                    showFoundAttacks();
                }
            }
        }));
        this.infoListener.setCycleCount(Timeline.INDEFINITE);
        this.infoListener.play();
    }

    private boolean validateNewAttackParameters() {
        String subnetworkId = this.subNetworkIdTextField.getText();
        String cancelledTxId = this.cancelledTxTextField.getText();

        if (subnetworkId.length() == 0) {
            showAlert("Attack Creation Failed", "Subnetwork ID cannot be empty!", Alert.AlertType.WARNING);
            return false;
        }

        if (cancelledTxId.length() == 0) {
            showAlert("Attack Creation Failed", "Cancelled TX ID cannot be empty!", Alert.AlertType.WARNING);
            return false;
        }

        Blockchain blockchain = SynchronizedBlockchainWrapper.javaFxReadOnlyBlockchain();
        Transaction foundTransaction = blockchain.findTransactionInMainChainById(cancelledTxId);
        if (foundTransaction == null) {
            showAlert("Attack Creation Failed", "Cancelled TX does not exist in the current main branch of the Blockchain!", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        String title = "Majority Attack";
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Pings all other members of the original network with the info that an attack is present so they have the possibility to join
    private class AttackInformerThread extends Thread {

        String subnetworkId;
        String cancelledTxId;

        AttackInformerThread(String subnetworkId, String cancelledTxId) {
            this.subnetworkId = subnetworkId;
            this.cancelledTxId = cancelledTxId;
        }

        @Override
        public void run() {
            pingLoop();
        }

        void pingLoop() {
            while(true) {
                String messageStr = this.subnetworkId + ";" + this.cancelledTxId;
                node.broadcastAttackInfo(messageStr);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.warning("Attack Informer Thread got interrupted!");
                }
            }
        }

    }

    // We have to make sure that if the original creator of the attack disconnects (either on purpose or through power loss)
    // some other node takes over from him the task of pinging all other network members that an attack is present so they can join.
    // In this version the node with the lowest public key is selected as the new informer.
    private class AttackInformerReceiverThread extends AttackInformerThread {

        AttackInformerReceiverThread(String subnetworkId, String cancelledTxId) {
            super(subnetworkId, cancelledTxId);
        }

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.warning("Attack Informer Receiver Thread got interrupted!");
                }

                Long lastHeartbeatTime = node.getAttackLastHeartbeat();
                if (lastHeartbeatTime == null || lastHeartbeatTime == 0) {
                    logger.warning("Failed to get last heartbeat!");
                    continue;
                }
                Long currentTime = System.currentTimeMillis();
                // If we do not get any message from the current Informer within 5 seconds we assume he died
                if (currentTime - lastHeartbeatTime > 5000) {
                    // Check if I am the node who is supposed to become the new Informer
                    List<String> connectedUsers = node.getConnectedNodes();
                    if (connectedUsers == null) {
                        logger.warning("Attack Informer Receiver tried to get the connected Users list but failed");
                        continue;
                    }

                    String myPublicKey = Configuration.getInstance().getPublicKey();
                    boolean amITheLowest = true;
                    for (String publicKey : connectedUsers) {
                        if (publicKey.equals(myPublicKey)) {
                            continue;
                        }

                        switch (myPublicKey.compareTo(publicKey)) {
                            case 1:
                                amITheLowest = false;
                                break;
                            case -1:
                            case 0:
                            default:
                                break;
                        }

                    }
                    if (amITheLowest) {
                        logger.info("Previous Attack Informer died - this node now is the new informer");
                        pingLoop();
                    } else {
                        logger.info("Previous Attack Informed died - this node has not been chosen as the new informer");
                    }
                }
            }
        }
    }

}
