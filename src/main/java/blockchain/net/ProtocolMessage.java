package blockchain.net;

import blockchain.model.Block;
import blockchain.model.Transaction;


public class ProtocolMessage {

    private final MessageType type;
    private final Block block;
    private final Transaction transaction;

    public ProtocolMessage(Block block) {
        this.type = MessageType.NEW_BLOCK;
        this.block = block;
        this.transaction = null;
    }

    public ProtocolMessage(Transaction transaction) {
        this.type = MessageType.NEW_TRANSACTION;
        this.block = null;
        this.transaction = transaction;
    }

    public enum MessageType {
        NEW_TRANSACTION,
        NEW_BLOCK;
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

}
