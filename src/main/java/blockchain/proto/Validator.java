package blockchain.proto;

import blockchain.model.Block;
import blockchain.model.Transaction;

public class Validator {

    public boolean verifySignature(Transaction transaction) {
        // TODO use ECDSA do verify the transaction's signature
        return true;
    }

}
