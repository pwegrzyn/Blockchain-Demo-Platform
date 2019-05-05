package blockchain.net;

import blockchain.model.Block;

/*Checks all transactions, all generated blocks, is able to mine new blocks and act as a Wallet*/
public class FullNode extends Node {

    public FullNode(String clusterName) {
        super(clusterName);
    }

    public BlockBroadcastResult broadcastNewBlock(Block newBlock) {
        // TODO broadcast the block to the network
        return null;
    }

    public void synchronizeWithOthers() {
        // TODO synchronize state with other fullNodes
    }
}
