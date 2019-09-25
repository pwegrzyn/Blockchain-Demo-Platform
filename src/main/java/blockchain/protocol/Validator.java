package blockchain.protocol;

import blockchain.crypto.ECDSA;
import blockchain.model.*;
import blockchain.util.Utils;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.logging.Logger;


public class Validator {

    private static final Logger LOGGER = Logger.getLogger(Validator.class.getName());
    private final ECDSA ecdsa;

    public Validator() {
        this.ecdsa = new ECDSA();
    }

    /*
     Check if all inputs in a given transaction have a valid signature
     */
    public boolean verifySignature(Transaction tx) throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeySpecException, UnsupportedEncodingException, SignatureException, InvalidKeyException {
        for (TransactionInput txInput : tx.getInputs()) {
            byte[] signature = Utils.hexStringToByteArray(txInput.getSignature());
            String txInputDataHash = TransactionInput.calculateHash(txInput.getPreviousTransactionHash(),
                    txInput.getPreviousTransactionOutputIndex(), txInput.getFromAddress());

            Transaction referencedTx = SynchronizedBlockchainWrapper.useBlockchain(b -> b.findTransactionInMainChain(txInput.getPreviousTransactionHash()));
            TransactionOutput referencedTxOutput = referencedTx.getOutputs().get(txInput.getPreviousTransactionOutputIndex());
            PublicKey creatorPublicKey = ecdsa.strToPublicKey(referencedTxOutput.getReceiverAddress());

            if (!ecdsa.checkSignature(txInputDataHash, creatorPublicKey, signature)) {
                LOGGER.warning("TX signatures verification failed!");
                return false;
            }
        }
        return true;
    }

    /*
     Validate a ECDSA public key
     */
    public boolean validatePublicKey(String publicKey) {
        return this.ecdsa.verifyPublicKeySize(publicKey);
    }

    /*
     Validate a ECDSA private key
     */
    public boolean validatePrivateKey(String privateKey) {
        return this.ecdsa.verifyPrivateKeySize(privateKey);
    }

    /*
     Validate a given ECDSA key pair
     */
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

    /*
     Validate a new incoming block (including all the transaction within it)
     */
    public boolean validateNewIncomingBlock(Block block) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        // Check if block is the last one (previous index + 1)
        int lastBlockIndexInBlockchain = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getLatestBlock().getIndex());
        if (block.getIndex() != lastBlockIndexInBlockchain + 1) {
            LOGGER.warning("Incoming block validation failed: bad index");
            return false;
        }

        // The previous block is correct (previous hash of it == block.previousHash)
        String hashOfPrevBlock = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getLatestBlock().getCurrentHash());
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
        if (!validateTXsInBlock(block)) {
            LOGGER.warning("Incoming block validation failed: bad transaction in the block");
            return false;
        }

        // Only one reward TX;
        int rewardTx = 0;
        for (Transaction tx : block.getTransactions()) {
            if (tx.getType() == TransactionType.REWARD) {
                rewardTx++;
            }
        }
        if (rewardTx > 1) {
            LOGGER.warning("Incoming block validation failed: more than one REWARD tx");
            return false;
        }

        return true;
    }

    /*
     Validate a new incoming transaction
     */
    public boolean validateNewIncomingTX(Transaction transaction) throws NoSuchAlgorithmException,
            UnsupportedEncodingException, SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        // The tx is not already present in the pool
        for (Transaction tx : SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getUnconfirmedTransactions)) {
            if (tx.getHash().equals(transaction.getHash())) {
                LOGGER.warning("Incoming tx validation failed: tx already present in pool");
                return false;
            }
        }

        // All the rest of the standard TX validation checks
        if (!validateTX(transaction)) {
            LOGGER.warning("Incoming tx validation failed: rest of checks");
            return false;
        }

        return true;
    }

    /*
     Validate all transactions coming in a new block
     */
    private boolean validateTXsInBlock(Block block) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        for (Transaction tx: block.getTransactions()) {
            if (!validateTX(tx)) {
                return false;
            }
        }
        return true;
    }

    /*
     Validate a transaction coming in a new block
     */
    private boolean validateTX(Transaction tx) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        // The transaction hash must be correct (calculated transaction hash == transaction.hash)
        String hashOfTx = Transaction.calculateTransactionHash(tx.getId(), tx.getInputs(), tx.getOutputs());
        if (!tx.getHash().equals(hashOfTx)) {
            LOGGER.warning("TX validation failed: bad hash");
            return false;
        }

        // The signatures of all input transactions must be valid
        if (!verifySignature(tx)) {
            return false;
        }

        // The sum of input transactions must be greater than or equal to output transactions (greater if fee is present)
        double inputsSum = 0.0;
        for (TransactionInput txInput : tx.getInputs()) {
            TransactionOutput txOutput = SynchronizedBlockchainWrapper
                    .useBlockchain(b -> b.findTransactionInMainChain(txInput.getPreviousTransactionHash()))
                    .getOutputs().get(txInput.getPreviousTransactionOutputIndex());
            inputsSum += txOutput.getAmount();
        }
        double outputsSum = 0.0;
        for (TransactionOutput txOutput : tx.getOutputs()) {
            outputsSum += txOutput.getAmount();
        }
        if (inputsSum < outputsSum) {
            LOGGER.warning("TX validation failed: inputs less than outputs");
            return false;
        }

        // The transaction isn't already in the blockchain (in the main branch)
        if (SynchronizedBlockchainWrapper.useBlockchain(b -> b.findTransactionInMainChain(tx.getHash())) != null) {
            LOGGER.warning("TX validation failed: tx already in blockchain");
            return false;
        }

        // All input transactions must be unspent in the blockchain
        if (!checkIfAllInputsAreUnspent(tx)) {
            LOGGER.warning("TX validation failed: contains spent inputs");
            return false;
        }

        return true;
    }

    /*
     Check if all inputs in a given transaction are unspent
     */
    private boolean checkIfAllInputsAreUnspent(Transaction txToCheck) {
        List<Block> mainBranch = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getMainBranch);
        for (TransactionInput txToCheckInput : txToCheck.getInputs()) {
            for (Block block : mainBranch) {
                for (Transaction tx : block.getTransactions()) {
                    for (TransactionInput txInput : tx.getInputs()) {
                        if (txInput.getPreviousTransactionHash().equals(txToCheckInput.getPreviousTransactionHash()) &&
                            txInput.getPreviousTransactionOutputIndex() == txToCheckInput.getPreviousTransactionOutputIndex()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

}
