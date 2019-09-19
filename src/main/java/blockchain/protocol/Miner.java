package blockchain.protocol;

import blockchain.crypto.Sha256Proxy;
import blockchain.model.*;
import blockchain.net.BlockBroadcastResult;
import blockchain.net.FullNode;

import java.io.UnsupportedEncodingException;
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
    private Blockchain blockchain;
    private Validator validator;
    private FullNode fullNode;
    private boolean isMining;
    private static final int MAX_TRANSACTIONS_PER_BLOCK = 5;
    // TODO fetch mining difficulty from the properties file
    private static final int MINING_DIFFICULTY = 4;

    public Miner(FullNode node) {
        this.blockchain = node.getBlockchain();
        this.validator = new Validator();
        this.isMining = false;
        this.fullNode = node;
    }


    public void run() {
        try {
            startMining();
        } catch (InterruptedException | NoSuchAlgorithmException | InvalidKeyException | SignatureException |
                UnsupportedEncodingException | NoSuchProviderException | InvalidKeySpecException e) {
           e.printStackTrace();
           LOGGER.severe("Miner encountered a fatal error and has stopped running");
        }
    }

    // Probably should run in its own thread
    public void startMining() throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException,
            NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        this.isMining = true;
        while (isMining) {
            Block latestBlock = this.blockchain.getLatestBlock();
            String latestHash = latestBlock.getCurrentHash();
            int latestIndex = latestBlock.getIndex();

            Block newMinedBlock = this.mineBlock();
            if (newMinedBlock == null)
                continue;
            BlockBroadcastResult result = this.fullNode.broadcastNewBlock(newMinedBlock);
            if (result.isConfirmed()) {
                /*All is cool, other nodes confirm the validity of the block, add it to your local blockchain*/
                this.blockchain.addBlock(newMinedBlock);
            } else {
                /*Synchronize your blockchain state with other nodes, if a new block was added after the sync, then
                stop working on your current block and recycle its transactions to the unconfirmed transaction pool*/
                this.fullNode.synchronizeWithOthers();
                this.blockchain.recycleInvalidBlock(newMinedBlock);
            }

        }
    }

    public void stopMining() {
        this.isMining = false;
    }

    private Block mineBlock() throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException,
            NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        Block latestBlock = this.blockchain.getLatestBlock();
        int newBlockIndex = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();

        /*Go through all the unconfirmed transactions and pick at most MAX_TRANSACTIONS_PER_BLOCK of them to be included
        in the next block (TODO: Some of the logic here probably belongs to the Validator class)*/
        List<Transaction> transactionsToAdd = new LinkedList<>();
        for (int i = 0; i < MAX_TRANSACTIONS_PER_BLOCK; i++) {
            Transaction unconfirmedTransaction;
            try {
                unconfirmedTransaction = this.blockchain.getUnconfirmedTransactions().remove();
            } catch (NoSuchElementException e) {
                break;
            }

            List<TransactionInput> inputs = unconfirmedTransaction.getInputs();
            List<TransactionOutput> outputs = unconfirmedTransaction.getOutputs();
            String id = unconfirmedTransaction.getId();
            if (!unconfirmedTransaction.getHash().equals(Transaction.calculateTransactionHash(id, inputs, outputs))) {
                continue;
            }

            boolean txAlreadyIncluded = false;
            for (Transaction tx : transactionsToAdd) {
                if (unconfirmedTransaction.getHash().equals(tx.getHash())) {
                    txAlreadyIncluded = true;
                    break;
                }
            }
            if (txAlreadyIncluded) continue;

            if (this.blockchain.findTransaction(unconfirmedTransaction.getHash()) != null) continue;

//            if (!this.validator.verifySignature(this.blockchain, unconfirmedTransaction)) continue;

            transactionsToAdd.add(unconfirmedTransaction);
        }
        if (transactionsToAdd.size() < 1) {
            LOGGER.info("Not enough transactions to begin mining a new block!");
            Thread.sleep(2000);
            return null;
        }

        assignLeftoverValueAsFee(transactionsToAdd);

        addRewardTransaction(transactionsToAdd);

        int nonce = -1;
        long currentTimestamp;
        StringBuilder target = new StringBuilder("");
        for (int i = 0; i < MINING_DIFFICULTY; i++) {
            target.append('0');
        }
        while (target.length() != 64) {
            target.append('F');
        }
        String targetStr = target.toString();
        do {
            currentTimestamp = System.currentTimeMillis();
            nonce = Sha256Proxy.searchForNonce(Block.getStringToHash(newBlockIndex, transactionsToAdd,
                    previousHash, currentTimestamp), targetStr);
            if (nonce < 0) {
            /*A new block may have been added to the chain while we were hashing, if so then we need to put back
            all the non-included txs back to the queue of unconfirmed transactions  */
                Block potentialNewLatestBlock = this.blockchain.getLatestBlock();
                if (potentialNewLatestBlock.getIndex() >= newBlockIndex || !potentialNewLatestBlock.getCurrentHash().equals(previousHash)) {
                    for (Transaction tx : transactionsToAdd) {
                        if (potentialNewLatestBlock.findTransaction(tx.getHash()) == null) {
                            this.blockchain.getUnconfirmedTransactions().add(tx);
                        }
                    }
                    return null;
                }
            }
        } while (nonce < 0);
        System.out.println("Block mined [nonce: " + nonce+"]");

        Block newBlock = new Block(newBlockIndex, transactionsToAdd, previousHash, nonce, currentTimestamp);
        return newBlock;
    }

    /*Some transactions can have more value in the inputs than in the outputs (as a sum). The leftover value is treated
    as a voluntary fee, which transaction creators can add as an incentive for miners to add their transactions
    to the blockchain quicker. We need to go through all the transactions and add to the block a new transaction,
    which takes all the leftovers as inputs at sends to it the miners address;*/
    private void assignLeftoverValueAsFee(List<Transaction> proposedTransactions) {
        // TODO extracting leftover value from transactions
    }

    private void addRewardTransaction(List<Transaction> proposedTransactions) {
        // TODO add reward tx
    }

}
