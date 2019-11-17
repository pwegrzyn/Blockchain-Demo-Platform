import blockchain.model.Transaction;
import blockchain.model.TransactionInput;
import blockchain.model.TransactionOutput;
import blockchain.model.TransactionType;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TestTransaction {

    @Ignore("Ignored to pass Travis build phase since we do not have access to openCL")
    @Test
    public void testInit() {
        String id = "foo";
        TransactionType type = TransactionType.REWARD;
        List<TransactionInput> inputs = Collections.emptyList();
        List<TransactionOutput> outputs = Collections.emptyList();

        Transaction transaction = new Transaction(id, type, inputs, outputs);

        assertEquals(transaction.getId(), id);
        assertEquals(transaction.getType(), type);
        assertEquals(transaction.getInputs(), inputs);
        assertEquals(transaction.getOutputs(), outputs);
        assertTrue(transaction.getHash().length() > 0);

        assertEquals(transaction.getCreatorAddr(), "1");
        assertEquals(transaction.getFee(), new BigDecimal(0));
    }

}
