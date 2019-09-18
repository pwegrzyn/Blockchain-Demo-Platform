package blockchain.protocol;

import blockchain.crypto.ECDSA;
import blockchain.model.*;
import blockchain.util.Utils;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Logger;


public class Validator {

    private static final Logger LOGGER = Logger.getLogger(Validator.class.getName());
    private final ECDSA ecdsa;

    public Validator() {
        this.ecdsa = new ECDSA();
    }

    // check if all inputs in the transaction have a valid signature
    public boolean verifySignature(Blockchain blockchain, Transaction tx) throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeySpecException, UnsupportedEncodingException, SignatureException, InvalidKeyException {
        for (TransactionInput txInput : tx.getInputs()) {
            byte[] signature = Utils.hexStringToByteArray(txInput.getSignature());
            String txInputDataHash = TransactionInput.calculateHash(txInput.getPreviousTransactionHash(),
                    txInput.getPreviousTransactionOutputIndex(), txInput.getAmount(), txInput.getFromAddress());

            Transaction referencedTx = blockchain.findTransaction(txInput.getPreviousTransactionHash());
            TransactionOutput referencedTxOutput = referencedTx.getOutputs().get(txInput.getPreviousTransactionOutputIndex());
            PublicKey creatorPublicKey = ecdsa.strToPublicKey(referencedTxOutput.getReceiverAddress());

            if (!ecdsa.checkSignature(txInputDataHash, creatorPublicKey, signature)) {
                LOGGER.warning("TX signatures verification failed!");
                return false;
            }
        }
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

    public boolean validateNewIncomingBlock(Blockchain blockchain, Block block) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
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
        if (!validateTXsInBlock(blockchain, block)) {
            LOGGER.warning("Incoming block validation failed: bad transaction in the block");
            return false;
        }

        // The sum of output transactions are equal the sum of input transactions + reward for miner

        // Check if no double spending is present

        // Only one FEE tx and one REWARD tx

        return true;
    }

    public boolean validateNewIncomingTX(Blockchain blockchain, Transaction transaction) {
        // TODO
        return true;
    }

    private boolean validateTXsInBlock(Blockchain blockchain, Block block) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        for (Transaction tx: block.getTransactions()) {
            if (!validateTX(blockchain, tx)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateTX(Blockchain blockchain, Transaction tx) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        // The transaction hash must be correct (calculated transaction hash == transaction.hash)
        String hashOfTx = Transaction.calculateTransactionHash(tx.getId(), tx.getInputs(), tx.getOutputs());
        if (!tx.getHash().equals(hashOfTx)) {
            LOGGER.warning("TX validation failed: bad hash");
            return false;
        }

        // The signatures of all input transactions must be valid
        if (!verifySignature(blockchain, tx)) {
            return false;
        }

        // The sum of input transactions must be greater than or equal to output transactions (greater if fee is present)
        // TODO

        return true;
    }

}
