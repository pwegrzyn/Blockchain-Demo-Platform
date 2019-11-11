package blockchain.net;

import blockchain.model.Block;
import blockchain.model.Transaction;

import java.io.Serializable;


public class ProtocolMessage implements Serializable {

    private final MessageType type;
    private final Block block;
    private final Transaction transaction;
    // format: subnetworkId;idOfCancelledTx
    private final String attackInfo;

    public ProtocolMessage(Block block) {
        this.type = MessageType.NEW_BLOCK;
        this.block = block;
        this.transaction = null;
        this.attackInfo = null;
    }

    public ProtocolMessage(Transaction transaction) {
        this.type = MessageType.NEW_TRANSACTION;
        this.block = null;
        this.transaction = transaction;
        this.attackInfo = null;
    }

    public ProtocolMessage(String attackInfo) {
        this.type = MessageType.ATTACK_INFO;
        this.block = null;
        this.transaction = null;
        this.attackInfo = attackInfo;
    }

    public enum MessageType {
        NEW_TRANSACTION,
        NEW_BLOCK,
        ATTACK_INFO;
    }

    public MessageType getType() {
        return type;
    }

    public Block getBlock() {
        return block;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getAttackInfo() {
        return attackInfo;
    }

}
