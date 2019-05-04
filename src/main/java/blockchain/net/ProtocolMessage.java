package blockchain.net;

import blockchain.model.Block;
import blockchain.model.Transaction;

public class ProtocolMessage {

    final MessageType type;
    final Block block;
    final Transaction transaction;

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
}
