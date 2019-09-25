package blockchain.net;

import blockchain.model.Transaction;


/*Can only create transactions and query other FullNodes*/
public class WalletNode extends Node {

    public WalletNode(String clusterName) {
        super(clusterName);
    }

    public void broadcastNewTransaction(Transaction transaction) {
        // TODO broadcast the tx to the network
        return;
    }

}
