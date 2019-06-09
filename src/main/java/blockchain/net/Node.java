package blockchain.net;

import blockchain.config.Configuration;
import blockchain.model.Blockchain;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.Protocol;
import org.jgroups.util.Util;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Node {

    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
    private JChannel channel;
    private String clusterName;
    protected Blockchain blockchain;

    public Node(String clusterName, Blockchain blockchain) {
        this.blockchain = blockchain;
        System.setProperty("java.net.preferIPv4Stack", "true");
        this.clusterName = clusterName;
        try {
            this.channel = new JChannel(getProtocolStack()).name(this.clusterName);
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
        if (channel.getView().getMembers().size() > 1)
            channel.disconnect();
        else
            channel.close();
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
                    new FD_ALL(),
                    new VERIFY_SUSPECT(),
                    new BARRIER(),
                    new NAKACK2(),
                    new UNICAST3(),
                    new STABLE(),
                    new GMS(),
                    new UFC(),
                    new MFC(),
                    new FRAG2(),
                    new STATE(),
                    new SEQUENCER(),
                    new FLUSH()};
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
        }

        @Override
        public void getState(OutputStream outputStream) throws Exception {
            LOGGER.info("Providing state to newly connected node.");
            synchronized (Node.this.blockchain) {
                try(ObjectOutputStream objectStream = new ObjectOutputStream(new BufferedOutputStream(outputStream, 32768))) {
                    objectStream.writeObject(Node.this.blockchain);
                }
            }
        }

        @Override
        public void setState(InputStream inputStream) throws Exception {
            LOGGER.info("Receiving state from existing nodes.");
            Blockchain receivedState;
            try(ObjectInputStream objectStream = new ObjectInputStream(inputStream)) {
                receivedState = (Blockchain) objectStream.readObject();
            }
            Node.this.blockchain = receivedState;
        }

    }

}
