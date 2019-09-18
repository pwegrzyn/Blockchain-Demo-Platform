package blockchain.net;

import blockchain.model.Block;
import blockchain.model.Blockchain;

/*Checks all transactions, all generated blocks, is able to mine new blocks and act as a Wallet*/
public class FullNode extends WalletNode {

    public FullNode(String clusterName, Blockchain blockchain) {
        super(clusterName, blockchain);
    }

    public BlockBroadcastResult broadcastNewBlock(Block newBlock) {
        // TODO broadcast the block to the network
        return new BlockBroadcastResult();
    }

    public void synchronizeWithOthers() {
        // TODO synchronize state with other fullNodes
    }
}
