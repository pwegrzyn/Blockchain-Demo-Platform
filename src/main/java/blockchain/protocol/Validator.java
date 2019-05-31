package blockchain.protocol;

import blockchain.crypto.ECDSA;
import blockchain.model.Transaction;

import java.util.logging.Logger;


public class Validator {

    private static final Logger LOGGER = Logger.getLogger(Validator.class.getName());
    private final ECDSA ecdsa;

    public Validator() {
        this.ecdsa = new ECDSA();
    }

    public boolean verifySignature(Transaction transaction) {
        // TODO use ECDSA do verify the transaction's signature
        return true;
    }

    public boolean validatePublicKey(String publicKey) {
        return this.ecdsa.verifyPublicKeySize(publicKey);
    }

    public boolean validatePrivateKey(String privateKey) {
        return this.ecdsa.verifyPrivateKeySize(privateKey);
    }

    public boolean validateKeyPair(String privateKey, String publicKey) {
        if (!validatePrivateKey(privateKey) || !validatePublicKey(publicKey)) {
            return false;
        }
        try {
            return this.ecdsa.verifyKeys(privateKey, publicKey);
        } catch (Exception e) {
            LOGGER.severe("Fatal error occurred while validating provided user key-pair!");
            return false;
        }
    }

    // TODO validation of incoming blocks
    // TODO validation of incoming transactions

}
