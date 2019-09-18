package blockchain.net;

import blockchain.config.Configuration;
import blockchain.config.Mode;
import blockchain.model.Blockchain;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class Node {

    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
    private static final int STATE_TRANSFER_BUFFER_SIZE = 1048576;
    private JChannel channel;
    private String clusterName;
    protected Blockchain blockchain;

    public Node(String clusterName, Blockchain blockchain) {
        this.blockchain = blockchain;
        System.setProperty("java.net.preferIPv4Stack", "true");
        this.clusterName = clusterName;
        try {
            this.channel = new JChannel(false);
            this.channel.name(Configuration.getInstance().getPublicKey());

            ProtocolStack protocolStack = new ProtocolStack();
            this.channel.setProtocolStack(protocolStack);
            Protocol[] protocols = getProtocolStack();
            for (Protocol protocol : protocols) {
                protocolStack.addProtocol(protocol);
            }
            protocolStack.init();

            this.channel.setDiscardOwnMessages(true);
            this.channel.setReceiver(new MessageReceiver());
            this.channel.connect(clusterName);
            this.channel.getState(null, 0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not create blockchain's channel", e);
            System.exit(1);
        }
        LOGGER.fine("Connection with the network established successfully.");
    }

    public void broadcast(ProtocolMessage msg) {
        try {
            org.jgroups.Message message = new Message(null, Util.objectToByteBuffer(msg));
            channel.send(message);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while broadcasting ProtocolMessage", e);
        }
    }

    public void disconnect() {
//        if (channel.getView().getMembers().size() > 1)
//            channel.disconnect();
//        else
//            channel.close();
        Util.close(this.channel);
        LOGGER.info("Disconnected successfully.");
    }

    public int countNodes() {
        return this.channel.getView().getMembers().size();
    }

    public List<String> getConnectedNodes() {
        return this.channel.getView().getMembers().stream().map(m -> m.toString()).collect(Collectors.toList());
    }

    public Blockchain getBlockchain() {
        return this.blockchain;
    }

    private Protocol[] getProtocolStack() {
        Protocol[] protocolStack = new Protocol[0];
        try {
            protocolStack = new Protocol[]{
                    new UDP().setValue("mcast_group_addr", InetAddress.getByName(Configuration.getInstance()
                            .getMcast_addr())),
                    new PING(),
                    new MERGE3(),
                    new FD_SOCK(),
                    new FD_ALL()
                            .setValue("timeout", 12000)
                            .setValue("interval", 3000),
                    new VERIFY_SUSPECT(),
                    new BARRIER(),
                    new NAKACK2(),
                    new UNICAST3(),
                    new STABLE(),
                    new GMS(),
                    new UFC(),
                    new MFC(),
                    new FRAG2(),
                    new STATE_TRANSFER()};
        } catch (UnknownHostException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while creating the protocol stack", e);
            System.exit(1);
        }
        return protocolStack;
    }

    private class MessageReceiver extends ReceiverAdapter {

        @Override
        public void receive(Message msg) {
            super.receive(msg);
            try {
                ProtocolMessage message = (ProtocolMessage) Util.objectFromByteBuffer(msg.getBuffer());
                if (message.getType() == ProtocolMessage.MessageType.NEW_BLOCK &&
                        Configuration.getInstance().getNodeRunningMode() == Mode.WALLET) {

                } else if (message.getType() == ProtocolMessage.MessageType.NEW_TRANSACTION &&
                        Configuration.getInstance().getNodeRunningMode() == Mode.FULL) {

                } else if (message.getType() == ProtocolMessage.MessageType.NEW_BLOCK &&
                        Configuration.getInstance().getNodeRunningMode() == Mode.FULL) {

                } else if (message.getType() == ProtocolMessage.MessageType.NEW_TRANSACTION &&
                        Configuration.getInstance().getNodeRunningMode() == Mode.WALLET) {

                }
            } catch (Exception e) {
                LOGGER.warning("Error while receiving message from neighbours!");
                return;
            }
        }

        @Override
        public void getState(OutputStream outputStream) throws Exception {
            LOGGER.info("Providing state to newly connected node.");
            synchronized (Node.this.blockchain) {
                try(ObjectOutputStream objectStream = new ObjectOutputStream(new BufferedOutputStream(outputStream,
                        STATE_TRANSFER_BUFFER_SIZE))) {
                    objectStream.writeObject(Node.this.blockchain);
                }
            }
            LOGGER.info("Provided state.");
        }

        @Override
        public void setState(InputStream inputStream) throws Exception {
            LOGGER.info("Receiving state from existing nodes.");
            synchronized (Node.this.blockchain) {
                Blockchain receivedState;
                try(ObjectInputStream objectStream = new ObjectInputStream(inputStream)) {
                    receivedState = (Blockchain) objectStream.readObject();
                }
                Node.this.blockchain = receivedState;
            }
            LOGGER.info("Received state.");
        }

    }

}
