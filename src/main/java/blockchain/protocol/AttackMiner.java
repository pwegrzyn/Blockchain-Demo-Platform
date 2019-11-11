package blockchain.protocol;

import blockchain.crypto.Sha256Proxy;
import blockchain.model.*;
import blockchain.net.BlockBroadcastResult;
import blockchain.net.FullNode;
import blockchain.net.ProtocolMessage;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Logger;

public class AttackMiner extends Miner implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(AttackMiner.class.getName());
    private String cancelledTxId;
    private String lastAttackedBlockHash;
    private String hashBeforeAttackedBlock;

    public AttackMiner(FullNode node) {
        super(node);
        node.setAttackMiner(this);
    }

    @Override
    public void run() {
        try {
            startMining();
        } catch (InterruptedException e) {
            LOGGER.info("AttackMiner process has stopped.");
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException |
                UnsupportedEncodingException | NoSuchProviderException | InvalidKeySpecException e) {
            e.printStackTrace();
            LOGGER.severe("AttackMiner encountered a fatal error and has stopped running");
        }
    }

    public void setAttackTarget(String cancelledTxId, String previousBlockHash) {
        this.cancelledTxId = cancelledTxId;
        this.lastAttackedBlockHash = previousBlockHash;
        this.hashBeforeAttackedBlock = calculateHashBeforeAttackedBlock();
    }

    public void setAttackTarget(String cancelledTxId) {
        this.cancelledTxId = cancelledTxId;
        this.lastAttackedBlockHash = calculateHashBeforeAttackedBlock();
        this.hashBeforeAttackedBlock = this.lastAttackedBlockHash;
    }

    public void broadcastAttackData() {
        ProtocolMessage message = new ProtocolMessage(cancelledTxId, lastAttackedBlockHash, null);
        this.fullNode.broadcast(message);
    }

    // Probably should run in its own thread
    @Override
    protected void startMining() throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException,
            NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        this.isMining = true;
        while (isMining) {
            Block newMinedBlock = this.mineBlock();
            if (newMinedBlock == null)
                continue;

            /* Check if mined block is valid and broadcast to other nodes */
            if (validator.validateBlock(newMinedBlock)) {
                LOGGER.info("Valid attack block has been mined (nonce: " + newMinedBlock.getNonce() + ") - broadcasting to neighbours...");
                this.lastAttackedBlockHash = newMinedBlock.getCurrentHash();
                ProtocolMessage message = new ProtocolMessage(cancelledTxId, lastAttackedBlockHash, newMinedBlock);
                this.fullNode.broadcast(message);
            }

        }
    }

    @Override
    protected Block mineBlock() throws InterruptedException {
        Block latestBlock = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getBlockDB).get(lastAttackedBlockHash);
        int newBlockIndex = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();

        List<Transaction> transactionsToAdd = new LinkedList<>();

        addOneTxToPending(transactionsToAdd, latestBlock);

        if (transactionsToAdd.size() < 1) {
            createTxToSelf(transactionsToAdd);
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
            if (!previousHash.equals(lastAttackedBlockHash)) {
                LOGGER.info("While working on a block the AttackMiner received a new latest block from neighbours. Recycling TXs and starting again.");
                return null;
            }
        } while (nonce < 0);

        Block newBlock = new Block(newBlockIndex, transactionsToAdd, previousHash, nonce, currentTimestamp);
        return newBlock;
    }

    private void createTxToSelf(List<Transaction> transactionsToAdd) {
        //TODO create tx to self
    }

    private void addOneTxToPending(List<Transaction> transactionsToAdd, Block latestBlock) {
        Transaction unconfirmedTransaction;

        Block checkedBlock = latestBlock;
        List<Block> toCheck = new LinkedList<>();
        while (!checkedBlock.getCurrentHash().equals(hashBeforeAttackedBlock)) {
            toCheck.add(checkedBlock);
            checkedBlock = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getBlockDB).get(checkedBlock.getPreviousHash());
        }

        while (toCheck.size() > 0) {
            checkedBlock = toCheck.remove(toCheck.size() - 1);
            for (Transaction tx : checkedBlock.getTransactions())
                if (tx.getType() == TransactionType.REGULAR)
                    tryToAddUnconfirmedTransactions(transactionsToAdd, tx);
        }

        Iterator<Transaction> queueIterator = SynchronizedBlockchainWrapper
                .useBlockchain(b -> b.getUnconfirmedTransactions().iterator());
        while (queueIterator.hasNext()) {
            unconfirmedTransaction = queueIterator.next();
            tryToAddUnconfirmedTransactions(transactionsToAdd, unconfirmedTransaction);
            LOGGER.info("AttackMiner chose new transaction (id: " + unconfirmedTransaction.getId() + ") to add to the new block being mined");
        }
    }

    private void tryToAddUnconfirmedTransactions(List<Transaction> transactionsToAdd, Transaction unconfirmedTransaction) {

        if (unconfirmedTransaction.getId().equals(cancelledTxId)) {
            createTxToSelf(transactionsToAdd);
            return;
        }

        List<TransactionInput> inputs = unconfirmedTransaction.getInputs();
        List<TransactionOutput> outputs = unconfirmedTransaction.getOutputs();
        String id = unconfirmedTransaction.getId();
        if (!unconfirmedTransaction.getHash().equals(Transaction.calculateTransactionHash(id, inputs, outputs))) {
            LOGGER.warning("Newly added tx in AttackMiner has invalid hash - aborting!");
            return;
        }

        boolean txAlreadyIncluded = false;
        for (Transaction tx : transactionsToAdd) {
            if (unconfirmedTransaction.getHash().equals(tx.getHash())) {
                txAlreadyIncluded = true;
                break;
            }
        }
        if (txAlreadyIncluded) {
            LOGGER.warning("Newly added tx in AttackMiner is already included in the new block being mined - skipping!");
            return;
        }

        if (SynchronizedBlockchainWrapper
                .useBlockchain(b -> b.findTransactionInMainChain(unconfirmedTransaction.getHash()) != null)) {
            LOGGER.warning("Newly added tx in AttackMiner is already included in the current blockchain - skipping!");
            return;
        }

        // Check if a tx added in an earlier iteration does not collide with our referenced outputs
        boolean inputsReused = false;
        for (Transaction txToBeChecked : transactionsToAdd) {
            for (TransactionInput inputToCheck : unconfirmedTransaction.getInputs()) {
                for (TransactionInput inputToBeChecked : txToBeChecked.getInputs()) {
                    if (inputToCheck.getPreviousTransactionOutputIndex() == inputToBeChecked.getPreviousTransactionOutputIndex()
                            && inputToCheck.getPreviousTransactionHash().equals(inputToBeChecked.getPreviousTransactionHash())) {
                        inputsReused = true;
                        return;
                    }
                }
            }
        }
        if (inputsReused) {
            LOGGER.warning("Newly added tx in AttackMiner uses already used inputs! - skipping!");
            return;
        }

        LOGGER.info("AttackMiner: Tx (id: " + unconfirmedTransaction.getId() + ") has been definitely added to the new block - OK");
        transactionsToAdd.add(unconfirmedTransaction);
    }

    private String calculateHashBeforeAttackedBlock() {
        for (Block block : SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getMainBranch))
            for (Transaction tx : block.getTransactions())
                if (tx.getId().equals(cancelledTxId))
                    return block.getCurrentHash();

        throw new IllegalStateException("Could not find block containing " + cancelledTxId + " transaction");

    }

}
