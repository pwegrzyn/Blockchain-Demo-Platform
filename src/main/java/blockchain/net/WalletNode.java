package blockchain.net;

import blockchain.model.Transaction;


/*Can only create transactions and query other FullNodes*/
public class WalletNode extends Node {

    public WalletNode(String clusterName) {
        super(clusterName);
    }

    public void broadcastNewTransaction(Transaction transaction) {
        ProtocolMessage message = new ProtocolMessage(transaction);
        this.broadcast(message);
    }

}
