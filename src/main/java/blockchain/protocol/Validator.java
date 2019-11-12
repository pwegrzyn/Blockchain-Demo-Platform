package blockchain.protocol;

import blockchain.config.Configuration;
import blockchain.crypto.ECDSA;
import blockchain.model.*;
import blockchain.util.Utils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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

            // genesis block tx case
            if (referencedTxOutput.getReceiverAddress().equals("0")) {
                return true;
            }

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
    public boolean validateBlock(Block block) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {

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

        // The hash must represent a valid difficulty level
        int currentDifficulty = Configuration.getInstance().getMiningDifficulty();
        for (int i = 0; i < currentDifficulty; i++) {
            if (block.getCurrentHash().charAt(i) != '0') {
                LOGGER.warning("Incoming block has an invalid difficulty level for its hash!");
                return false;
            }
        }

        // All transactions inside the block must be valid
        if (!validateTXsInBlock(block)) {
            LOGGER.warning("Incoming block validation failed: bad transaction in the block");
            return false;
        }

        // Only one reward and fee TX;
        int rewardTx = 0, feeTx = 0;
        for (Transaction tx : block.getTransactions()) {
            if (tx.getType() == TransactionType.REWARD)
                rewardTx++;
            else if (tx.getType() == TransactionType.FEE)
                feeTx++;
        }
        if (rewardTx > 1) {
            LOGGER.warning("Incoming block validation failed: more than one REWARD tx");
            return false;
        } else if (feeTx > 1) {
            LOGGER.warning("Incoming block validation failed: more than one FEE tx");
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

        if (transaction.getType() != TransactionType.REGULAR) {
            return false;
        }

        return true;
    }

    /*
     Validate all transactions coming in a new block
     */
    private boolean validateTXsInBlock(Block block) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        for (Transaction tx : block.getTransactions()) {
            if (!validateTX(tx)) {
                return false;
            }

            // If Fee tx check its correctness
            if (tx.getType() == TransactionType.FEE && !validateFeeTx(tx, block.getTransactions())) {
                return false;
            }

            // If Reward tx check its correctness
            if (tx.getType() == TransactionType.REWARD && !validateRewardTx(tx)) {
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

        // If it is Fee or Reward tx end verification here
        if (tx.getType() == TransactionType.FEE || tx.getType() == TransactionType.REWARD)
            return true;

        // The signatures of all input transactions must be valid
        if (!verifySignature(tx)) {
            return false;
        }

        // The sum of input transactions must be greater than or equal to output transactions (greater if fee is present)
        BigDecimal inputsSum = new BigDecimal(0.0);
        for (TransactionInput txInput : tx.getInputs()) {
            TransactionOutput txOutput = SynchronizedBlockchainWrapper
                    .useBlockchain(b -> b.findTransactionInMainChain(txInput.getPreviousTransactionHash()))
                    .getOutputs().get(txInput.getPreviousTransactionOutputIndex());
            inputsSum = inputsSum.add(txOutput.getAmount());
        }
        BigDecimal outputsSum = new BigDecimal(0.0);
        for (TransactionOutput txOutput : tx.getOutputs()) {
            outputsSum = outputsSum.add(txOutput.getAmount());
        }
        if (inputsSum.compareTo(outputsSum) < 0) {
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

    /*
     Validate value of FEE transaction
     */
    private boolean validateFeeTx(Transaction feeTx, List<Transaction> transactions) {
        BigDecimal amount = new BigDecimal(0);

        for (Transaction tx : transactions) {
            amount = amount.add(tx.getFee());
        }

        if (feeTx.getInputs().size() != 0) {
            LOGGER.warning("TX validation failed: fee tx cannot contain any inputs");
            return false;
        }

        List<TransactionOutput> outputs = feeTx.getOutputs();
        if (outputs.size() != 1) {
            LOGGER.warning("TX validation failed: fee tx can contain only one output");
            return false;
        }

        if (outputs.get(0).getAmount().compareTo(amount) != 0) {
            LOGGER.warning("TX validation failed: fee tx contains incorrect value");
            return false;
        }

        return true;
    }

    /*
    Validate REWARD transaction
    */
    private boolean validateRewardTx(Transaction rewardTx) {
        BigDecimal amount = Configuration.getInstance().getBlockRewardValue();

        if (rewardTx.getInputs().size() != 0) {
            LOGGER.warning("TX validation failed: reward tx cannot contain any inputs");
            return false;
        }

        List<TransactionOutput> outputs = rewardTx.getOutputs();
        if (outputs.size() != 1) {
            LOGGER.warning("TX validation failed: reward tx can contain only one output");
            return false;
        }

        if (outputs.get(0).getAmount().compareTo(amount) != 0) {
            LOGGER.warning("TX validation failed: reward tx contains incorrect value");
            return false;
        }

        return true;
    }

}
