package blockchain.net;

import blockchain.model.Block;

import java.util.concurrent.ExecutorService;

/*Checks all transactions, all generated blocks, is able to mine new blocks and act as a Wallet*/
public class FullNode extends WalletNode {

    private ExecutorService minerThread;

    public FullNode(String clusterName) {
        super(clusterName);
    }

    public BlockBroadcastResult broadcastNewBlock(Block newBlock) {
        ProtocolMessage message = new ProtocolMessage(newBlock);
        this.broadcast(message);
        return new BlockBroadcastResult();
    }

    public void synchronizeWithOthers() {
        // TODO synchronize state with other fullNodes
    }

    public ExecutorService getMinerThread() {
        return minerThread;
    }

    public void setMinerThread(ExecutorService minerThread) {
        this.minerThread = minerThread;
    }
}
