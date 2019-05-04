package blockchain.proto;

import blockchain.model.Transaction;

public class Validator {

    public boolean verifySignature(Transaction transaction) {
        // TODO use ECDSA do verify the transaction's signature
        return true;
    }

    // TODO validation of incoming blocks
    // TODO validation of incoming transactions

}
