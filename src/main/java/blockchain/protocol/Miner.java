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

    private static final Logger LOGGER = Logger.getLogger(Miner.class.getName());
    private Validator validator;
    private FullNode fullNode;
    private boolean isMining;
    private static final int MAX_TRANSACTIONS_PER_BLOCK = 5;
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
                BlockBroadcastResult result = this.fullNode.broadcastNewBlock(newMinedBlock);
                if (result.isConfirmed()) {
                    /*All is cool, other nodes confirm the validity of the block, add it to your local blockchain*/
                    SynchronizedBlockchainWrapper.useBlockchain(b -> {
                        b.addBlock(newMinedBlock);
                        return null;
                    });
                } else {
                /*Synchronize your blockchain state with other nodes, if a new block was added after the sync, then
                stop working on your current block and recycle its transactions to the unconfirmed transaction pool*/
                    this.fullNode.synchronizeWithOthers();
                }
            }

        }
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
        List<Transaction> transactionsToAdd = new LinkedList<>();
        for (int i = 0; i < MAX_TRANSACTIONS_PER_BLOCK; i++) {
            Transaction unconfirmedTransaction;
            try {
                unconfirmedTransaction = SynchronizedBlockchainWrapper
                        .useBlockchain(b -> b.getUnconfirmedTransactions().remove());
                LOGGER.info("Miner chose new transaction (id: " + unconfirmedTransaction.getId() + ") to add to the new block being mined");
            } catch (NoSuchElementException e) {
                break;
            }

            List<TransactionInput> inputs = unconfirmedTransaction.getInputs();
            List<TransactionOutput> outputs = unconfirmedTransaction.getOutputs();
            String id = unconfirmedTransaction.getId();
            if (!unconfirmedTransaction.getHash().equals(Transaction.calculateTransactionHash(id, inputs, outputs))) {
                LOGGER.warning("Newly added tx in miner has invalid hash - aborting!");
                continue;
            }

            boolean txAlreadyIncluded = false;
            for (Transaction tx : transactionsToAdd) {
                if (unconfirmedTransaction.getHash().equals(tx.getHash())) {
                    txAlreadyIncluded = true;
                    break;
                }
            }
            if (txAlreadyIncluded) {
                LOGGER.warning("Newly added tx in miner is already included in the new block being mined - skipping!");
                continue;
            }

            if (SynchronizedBlockchainWrapper
                    .useBlockchain(b -> b.findTransactionInMainChain(unconfirmedTransaction.getHash()) != null)) {
                LOGGER.warning("Newly added tx in miner is already included in the current blockchain - skipping!");
                continue;
            }

            // Check if a tx added in an earlier iteration does not collide with our referenced outputs
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
            if (inputsReused) {
                LOGGER.warning("Newly added tx in miner uses already used inputs! - skipping!");
                continue;
            }

            LOGGER.info("Tx (id: " + unconfirmedTransaction.getId() + ") has been definitely added to the new block - OK");
            transactionsToAdd.add(unconfirmedTransaction);
        }
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

    /*Some transactions can have more value in the inputs than in the outputs (as a sum). The leftover value is treated
    as a voluntary fee, which transaction creators can add as an incentive for miners to add their transactions
    to the blockchain quicker. We need to go through all the transactions and add to the block a new transaction,
    which takes all the leftovers as inputs at sends to it the miners address;*/
    private void assignLeftoverValueAsFee(List<Transaction> proposedTransactions, int blockIndex) {
        BigDecimal amount = new BigDecimal(0);

        for (Transaction tx : proposedTransactions)
            amount = amount.add(tx.getFee());

        TransactionOutput feesOutput = new TransactionOutput(amount, Configuration.getInstance().getPublicKey());
        List<TransactionOutput> feeOutputs = new LinkedList<>();
        feeOutputs.add(feesOutput);

        Transaction feesTransaction = new Transaction("FeeId" + blockIndex, TransactionType.FEE,
                new LinkedList<>(), feeOutputs);

        proposedTransactions.add(feesTransaction);
    }

    private void addRewardTransaction(List<Transaction> proposedTransactions, int blockIndex) {
        BigDecimal amount = Configuration.getInstance().getBlockRewardValue();

        TransactionOutput rewardOutput = new TransactionOutput(amount, Configuration.getInstance().getPublicKey());
        List<TransactionOutput> rewardOutputs = new LinkedList<>();
        rewardOutputs.add(rewardOutput);

        Transaction rewardTransaction = new Transaction("RewardId" + blockIndex, TransactionType.REWARD,
                new LinkedList<>(), rewardOutputs);

        proposedTransactions.add(rewardTransaction);
    }

}
