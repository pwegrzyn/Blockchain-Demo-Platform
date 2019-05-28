package blockchain.protocol;

import blockchain.model.Transaction;

public class Validator {

    public boolean verifySignature(Transaction transaction) {
        // TODO use ECDSA do verify the transaction's signature
        return true;
    }

    public boolean validatePublicKey(String publicKey) {
        // Finish if keys format is set
        return true;
    }

    public boolean validatePrivateKey(String privateKey) {
        // Finish if keys format is set
        return true;
    }

    public boolean validateKeyPair(String privateKey, String publicKey) {
        if (!validatePrivateKey(privateKey) || !validatePublicKey(publicKey)) {
            return false;
        }
        // TODO Use bouncyCastle to validate the key pair
        return true;
    }

    // TODO validation of incoming blocks
    // TODO validation of incoming transactions

}
