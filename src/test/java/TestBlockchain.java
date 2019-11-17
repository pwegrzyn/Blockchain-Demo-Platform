import blockchain.model.Blockchain;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestBlockchain {

    @Ignore("Ignored to pass Travis build phase since we do not have access to openCL")
    @Test
    public void testInit() {
        Blockchain blockchain = new Blockchain();

        assertFalse(blockchain.getBlockDB().isEmpty());
        assertTrue(blockchain.getUnconfirmedTransactions().isEmpty());
        assertNotNull(blockchain.getLatestBlock());
        assertNull(blockchain.getHashOfLastNonMainBranchBlockReceived());
    }

}
