package blockchain.protocol;

import blockchain.model.Block;
import blockchain.model.Blockchain;
import blockchain.model.SynchronizedBlockchainWrapper;
import blockchain.net.FullNode;

import java.util.logging.Logger;


public class AttackMiner extends Miner {

    private static final Logger LOGGER = Logger.getLogger(AttackMiner.class.getName());
    private String cancelledTxId;

    public AttackMiner(String cancelledTxId, FullNode node) {
        super(node);
        this.cancelledTxId = cancelledTxId;
    }

    @Override
    Block mineBlock() throws InterruptedException {
        Block latestBlock = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getBlockDB).get(previousBlockHash);
        int newBlockIndex = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();

        /*Go through all the unconfirmed transactions and pick at most MAX_TRANSACTIONS_PER_BLOCK of them to be included
        in the next block */
        List<Transaction> transactionsToAdd = new LinkedList<>();
        Transaction unconfirmedTransaction;

        // TODO: reversed order of block traversal
        // TODO: add memeory to start choosing txs based on what the attacker group has already mined
        Block checkedBlock = latestBlock;
        while (!checkedBlock.getCurrentHash().equals(hashBeforeAttackedBlock)) {
            for (Transaction tx : checkedBlock.getTransactions()) {
                tryToAddUnconfirmedTransactions(transactionsToAdd, tx);
            }
        }

        Iterator<Transaction> queueIterator = SynchronizedBlockchainWrapper
                .useBlockchain(b -> b.getUnconfirmedTransactions().iterator());
        while (queueIterator.hasNext()) {
            unconfirmedTransaction = queueIterator.next();
            tryToAddUnconfirmedTransactions(transactionsToAdd, unconfirmedTransaction);
            LOGGER.info("AttackMiner chose new transaction (id: " + unconfirmedTransaction.getId() + ") to add to the new block being mined");
        }


        if (transactionsToAdd.size() < 1) {
            //TODO
//            createTxToSelf(transactionsToAdd);
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
            if (!previousHash.equals(previousBlockHash)) {
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

    private void tryToAddUnconfirmedTransactions(List<Transaction> transactionsToAdd, Transaction unconfirmedTransaction) {

        if (unconfirmedTransaction.getId().equals(cancelledTxId))
            return;

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

        // TODO: Add check, where we check if the removed transaction is in the past history of referenced txs

        LOGGER.info("AttackMiner: Tx (id: " + unconfirmedTransaction.getId() + ") has been definitely added to the new block - OK");
        transactionsToAdd.add(unconfirmedTransaction);
    }

}
