package blockchain.net;

import blockchain.model.Block;

/*Checks all transactions, all generated blocks, is able to mine new blocks and act as a Wallet*/
public class FullNode extends WalletNode {

    public FullNode(String clusterName) {
        super(clusterName);
    }

    public BlockBroadcastResult broadcastNewBlock(Block newBlock) {
        ProtocolMessage message=new ProtocolMessage(newBlock);
        this.broadcast(message);
        return new BlockBroadcastResult();
    }

    public void synchronizeWithOthers() {
        // TODO synchronize state with other fullNodes
    }
}
