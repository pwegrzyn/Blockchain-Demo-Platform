package blockchain.protocol;

import blockchain.crypto.ECDSA;
import blockchain.model.Block;
import blockchain.model.Blockchain;
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

    public boolean validateNewIncomingBlock(Blockchain blockchain, Block block) {
        // Check if block is the last one (previous index + 1)
        int lastBlockIndexInBlockchain = blockchain.getLatestBlock().getIndex();
        if (block.getIndex() != lastBlockIndexInBlockchain + 1) {
            LOGGER.warning("Incoming block validation failed: bad index");
            return false;
        }

        // The previous block is correct (previous hash of it == block.previousHash)
        String hashOfPrevBlock = blockchain.getLatestBlock().getCurrentHash();
        if (!block.getPreviousHash().equals(hashOfPrevBlock)) {
            LOGGER.warning("Incoming block validation failed: bad previous hash");
            return false;
        }

        // The hash must be valid (calculated block hash == block.hash)
        String hashOfNewBlock = Block.calculateBlockHash(block.getIndex(), block.getTransactions(),
                block.getPreviousHash(), block.getTimestamp(), block.getNonce());
        if (!block.getCurrentHash().equals(hashOfNewBlock)) {
            LOGGER.warning("Incoming block validation failed: bad current hash");
            return false;
        }

        // All transactions inside the block must be valid

        // The sum of output transactions are equal the sum of input transactions + reward for miner

        // Check if no double spending is present

        // Only one FEE tx and one REWARD tx

        return true;
    }

    public boolean validateNewIncomingTransaction(Blockchain blockchain, Transaction transaction) {
        return true;
    }

}
