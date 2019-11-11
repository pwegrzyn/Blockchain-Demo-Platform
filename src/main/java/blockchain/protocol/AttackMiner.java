package blockchain.protocol;

import blockchain.model.Block;
import blockchain.net.FullNode;

import java.util.logging.Logger;


public class AttackMiner extends Miner {

    private static final Logger LOGGER = Logger.getLogger(AttackMiner.class.getName());
    private String cancelledTxId;

    public AttackMiner(String cancelledTxId, FullNode node) {
        super(node);
        this.cancelledTxId = cancelledTxId;
    }

    @Override
    Block mineBlock() throws InterruptedException {
        return null;
    }

}
