package blockchain.protocol;

import blockchain.config.Configuration;
import blockchain.crypto.Sha256Proxy;
import blockchain.model.*;
import blockchain.net.BlockBroadcastResult;
import blockchain.net.FullNode;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public class Miner extends Thread {

    protected static final Logger LOGGER = Logger.getLogger(Miner.class.getName());
    protected Validator validator;
    protected FullNode fullNode;
    protected boolean isMining;
    protected static final int MAX_TRANSACTIONS_PER_BLOCK = 5;
    // TODO fetch mining difficulty from the properties file
    private static final int MINING_DIFFICULTY = 4;

    public Miner(FullNode node) {
        this.validator = new Validator();
        this.isMining = false;
        this.fullNode = node;
    }

    public void run() {
        try {
            startMining();
        } catch (InterruptedException e) {
            LOGGER.info("Miner process has stopped.");
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException |
                UnsupportedEncodingException | NoSuchProviderException | InvalidKeySpecException e) {
            e.printStackTrace();
            LOGGER.severe("Miner encountered a fatal error and has stopped running");
        }
    }

    // Probably should run in its own thread
    private void startMining() throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException,
            NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        this.isMining = true;
        while (isMining) {
            Block newMinedBlock = this.mineBlock();
            if (newMinedBlock == null)
                continue;

            /* Check if mined block is valid and broadcast to other nodes */
            if (validator.validateBlock(newMinedBlock)) {
                LOGGER.info("Valid block has been mined (nonce: " + newMinedBlock.getNonce() + ") - broadcasting to neighbours...");
                BlockBroadcastResult result = broadcastMinedBlock(newMinedBlock);
                if (result.isConfirmed()) {
                    /*All is cool, other nodes confirm the validity of the block, add it to your local blockchain*/
                    SynchronizedBlockchainWrapper.useBlockchain(b -> {
                        b.addBlock(newMinedBlock);
                        return null;
                    });
                }
            }

        }
    }

    protected BlockBroadcastResult broadcastMinedBlock(Block newMinedBlock) {
        return this.fullNode.broadcastNewBlock(newMinedBlock);
    }

    public void stopMining() {
        this.isMining = false;
    }

    private Block mineBlock() throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException,
            NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        Block latestBlock = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getLatestBlock);
        int newBlockIndex = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();

        /*Go through all the unconfirmed transactions and pick at most MAX_TRANSACTIONS_PER_BLOCK of them to be included
        in the next block */
        List<Transaction> transactionsToAdd = addTransactionsToBeMined();

        if (transactionsToAdd.size() < 1) {
            Thread.sleep(2000);
            return null;
        }

        assignLeftoverValueAsFee(transactionsToAdd, newBlockIndex);

        addRewardTransaction(transactionsToAdd, newBlockIndex);

        int nonce = -1;
        long currentTimestamp;
        StringBuilder target = new StringBuilder("");
        for (int i = 0; i < MINING_DIFFICULTY; i++) {
            target.append('0');
        }
        while (target.length() < 64) {
            target.append('F');
        }
        String targetStr = target.toString();
        do {
            currentTimestamp = System.currentTimeMillis();
            nonce = Sha256Proxy.searchForNonce(Block.getStringToHash(newBlockIndex, transactionsToAdd,
                    previousHash, currentTimestamp), targetStr);

            /* Check if a new block has appeared in blockchain during mining */
            Block potentialNewLatestBlock = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getLatestBlock);
            if (!previousHash.equals(potentialNewLatestBlock.getCurrentHash())) {
                LOGGER.info("While working on a block the miner received a new latest block from neighbours. Recycling TXs and starting again.");
                for (Transaction tx : transactionsToAdd) {
                    if (potentialNewLatestBlock.findTransaction(tx.getHash()) == null) {
                        SynchronizedBlockchainWrapper
                                .useBlockchain(b -> {
                                    b.getUnconfirmedTransactions().add(tx);
                                    return null;
                                });
                    }
                }
                return null;
            }
        } while (nonce < 0);

        Block newBlock = new Block(newBlockIndex, transactionsToAdd, previousHash, nonce, currentTimestamp);
        System.setProperty("lastCalculatedHash", newBlock.getCurrentHash());
        return newBlock;
    }

    protected List<Transaction> addTransactionsToBeMined() {
        List<Transaction> transactionsToAdd = new LinkedList<>();
        while (transactionsToAdd.size() < MAX_TRANSACTIONS_PER_BLOCK) {
            Transaction unconfirmedTransaction;
            try {
                unconfirmedTransaction = SynchronizedBlockchainWrapper
                        .useBlockchain(b -> b.getUnconfirmedTransactions().remove());
            } catch (NoSuchElementException e) {
                break;
            }

            LOGGER.info("Miner chose new transaction (id: " + unconfirmedTransaction.getId() + ") to add to the new block being mined");

            if (checkTransactionHash(unconfirmedTransaction)) {
                LOGGER.warning("Newly added tx in miner has invalid hash - aborting!");
                continue;
            }

            if (checkIfTransactionIsAlreadyIncluded(transactionsToAdd, unconfirmedTransaction)) {
                LOGGER.warning("Newly added tx in miner is already included in the new block being mined - skipping!");
                continue;
            }

            if (checkIfTransactionIsAlreadyInBlockchain(unconfirmedTransaction)) {
                LOGGER.warning("Newly added tx in miner is already included in the current blockchain - skipping!");
                continue;
            }

            // Check if a tx added in an earlier iteration does not collide with our referenced outputs
            if (checkTransactionForAlreadySpentInputs(transactionsToAdd, unconfirmedTransaction)) {
                LOGGER.warning("Newly added tx in miner uses already used inputs! - skipping!");
                continue;
            }

            LOGGER.info("Tx (id: " + unconfirmedTransaction.getId() + ") has been definitely added to the new block - OK");
            transactionsToAdd.add(unconfirmedTransaction);
        }
        return transactionsToAdd;
    }

    protected boolean checkTransactionForAlreadySpentInputs(List<Transaction> transactionsToAdd, Transaction unconfirmedTransaction) {
        boolean inputsReused = false;
        for (Transaction txToBeChecked : transactionsToAdd) {
            for (TransactionInput inputToCheck : unconfirmedTransaction.getInputs()) {
                for (TransactionInput inputToBeChecked : txToBeChecked.getInputs()) {
                    if (inputToCheck.getPreviousTransactionOutputIndex() == inputToBeChecked.getPreviousTransactionOutputIndex()
                            && inputToCheck.getPreviousTransactionHash().equals(inputToBeChecked.getPreviousTransactionHash())) {
                        inputsReused = true;
                        break;
                    }
                }
            }
        }
        return inputsReused;
    }

    protected Boolean checkIfTransactionIsAlreadyInBlockchain(Transaction unconfirmedTransaction) {
        return SynchronizedBlockchainWrapper
                .useBlockchain(b -> b.findTransactionInMainChain(unconfirmedTransaction.getHash()) != null);
    }

    protected boolean checkIfTransactionIsAlreadyIncluded(List<Transaction> transactionsToAdd, Transaction unconfirmedTransaction) {
        boolean txAlreadyIncluded = false;
        for (Transaction tx : transactionsToAdd) {
            if (unconfirmedTransaction.getHash().equals(tx.getHash())) {
                txAlreadyIncluded = true;
                break;
            }
        }
        return txAlreadyIncluded;
    }

    protected boolean checkTransactionHash(Transaction unconfirmedTransaction) {
        List<TransactionInput> inputs = unconfirmedTransaction.getInputs();
        List<TransactionOutput> outputs = unconfirmedTransaction.getOutputs();
        String id = unconfirmedTransaction.getId();
        return !unconfirmedTransaction.getHash().equals(Transaction.calculateTransactionHash(id, inputs, outputs));
    }

    /*Some transactions can have more value in the inputs than in the outputs (as a sum). The leftover value is treated
    as a voluntary fee, which transaction creators can add as an incentive for miners to add their transactions
    to the blockchain quicker. We need to go through all the transactions and add to the block a new transaction,
    which takes all the leftovers as inputs at sends to it the miners address;*/
    private void assignLeftoverValueAsFee(List<Transaction> proposedTransactions, int blockIndex) {
        BigDecimal amount = new BigDecimal(0);

        for (Transaction tx : proposedTransactions)
            amount = amount.add(tx.getFee());

        // Don't create empty fee tx
        if (amount.compareTo(new BigDecimal(0)) == 0) return;

        TransactionOutput feesOutput = new TransactionOutput(amount, Configuration.getInstance().getPublicKey());
        List<TransactionOutput> feeOutputs = new LinkedList<>();
        feeOutputs.add(feesOutput);

        Transaction feesTransaction = new Transaction("Fee_" + blockIndex, TransactionType.FEE,
                new LinkedList<>(), feeOutputs);

        proposedTransactions.add(feesTransaction);
    }

    private void addRewardTransaction(List<Transaction> proposedTransactions, int blockIndex) {
        BigDecimal amount = Configuration.getInstance().getBlockRewardValue();

        TransactionOutput rewardOutput = new TransactionOutput(amount, Configuration.getInstance().getPublicKey());
        List<TransactionOutput> rewardOutputs = new LinkedList<>();
        rewardOutputs.add(rewardOutput);

        Transaction rewardTransaction = new Transaction("Reward_" + blockIndex, TransactionType.REWARD,
                new LinkedList<>(), rewardOutputs);

        proposedTransactions.add(rewardTransaction);
    }

}
