import blockchain.model.Block;
import blockchain.model.Transaction;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TestBlock {

    @Ignore("Ignored to pass Travis build phase since we do not have access to openCL")
    @Test
    public void testInit() {
        int index = 1;
        List<Transaction> txList = Collections.emptyList();
        String previousHash = "ala";
        int nonce = 2;
        long timestamp = 42;

        Block block = new Block(index, txList, previousHash, nonce, timestamp);

        assertEquals(block.getIndex(), index);
        assertEquals(block.getTransactions(), txList);
        assertEquals(block.getPreviousHash(), previousHash);
        assertEquals(block.getNonce(), nonce);
        assertEquals(block.getTimestamp(), timestamp);
        assertTrue(block.getCurrentHash().length() > 0);
    }

}
