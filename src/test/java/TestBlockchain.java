import blockchain.model.Blockchain;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestBlockchain {

    @Test
    public void testInit() {
        Blockchain blockchain = new Blockchain();

        assertFalse(blockchain.getBlockDB().isEmpty());
        assertTrue(blockchain.getUnconfirmedTransactions().isEmpty());
        assertNotNull(blockchain.getLatestBlock());
        assertNull(blockchain.getHashOfLastNonMainBranchBlockReceived());
    }

}
