package blockchain.net;

import blockchain.model.Blockchain;
import blockchain.model.Transaction;


/*Can only create transactions and query other FullNodes*/
public class WalletNode extends Node {

    public WalletNode(String clusterName, Blockchain blockchain) {
        super(clusterName, blockchain);
    }

    public void broadcastNewTransaction(Transaction transaction) {
        // TODO broadcast the tx to the network
        return;
    }

}
