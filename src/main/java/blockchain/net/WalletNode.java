package blockchain.net;

/*Can only create transactions and query other FullNodes*/
public class WalletNode extends Node {

    public WalletNode(String clusterName) {
        super(clusterName);
    }

}
