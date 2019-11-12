package blockchain.protocol;

import blockchain.crypto.Sha256Proxy;
import blockchain.model.*;
import blockchain.net.FullNode;

import java.util.*;
import java.util.logging.Logger;


public class AttackMiner extends Miner {

    private static final Logger LOGGER = Logger.getLogger(AttackMiner.class.getName());
    private String cancelledTxId;

    public AttackMiner(String cancelledTxId, FullNode node) {
        super(node);
        this.cancelledTxId = cancelledTxId;
        this.MAX_TRANSACTIONS_PER_BLOCK = 1;
    }

    // TODO: remove reward and fee txs
    @Override
    Block mineBlock() throws InterruptedException {
        String failInfoText = "Could not find block with given transaction in main branch in order to perform an attack.";

        // try to continue the work of other attackers
        Block latestBlockTmp = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB().get(b.getHashOfLastNonMainBranchBlockReceived()));
        // or if you are the first attacker...
        if (latestBlockTmp == null) {
            Block attackedBlock = getAttackedBlock(failInfoText);
            latestBlockTmp = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB().get(attackedBlock.getPreviousHash()));
        }
        // If you were still unable to find the valid latest attacker block then something is not right
        if (latestBlockTmp == null) {
            throw new IllegalStateException(failInfoText);
        }

        // Static typing gainz
        final Block latestBlock = latestBlockTmp;

        int newBlockIndex = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();
        /*Go through all the unconfirmed transactions and pick at most MAX_TRANSACTIONS_PER_BLOCK of them to be included
        in the next block */
        List<Transaction> transactionsToAdd = new LinkedList<>();

        SynchronizedBlockchainWrapper.useBlockchain(b -> {
            // Prioritize adding transactions that will become invalidated
            int indexOfAttackedBlock = b.getMainBranch().indexOf(getAttackedBlock(failInfoText));
            List<Block> invalidatedBlocks = new LinkedList<>(b.getMainBranch().subList(indexOfAttackedBlock, b.getMainBranch().size()));
            Collections.reverse(invalidatedBlocks);
            for (Block invalidBlock : invalidatedBlocks) {
                for (Transaction invalidTransaction : invalidBlock.getTransactions()) {
                    tryToAddUnconfirmedTransactions(transactionsToAdd, invalidTransaction);
                }
            }

            // Now you can try to add txs from the global pool of unconfirmed transactions
            while (transactionsToAdd.size() < MAX_TRANSACTIONS_PER_BLOCK) {
                try {
                    Transaction tx = b.getUnconfirmedTransactions().remove();
                    tryToAddUnconfirmedTransactions(transactionsToAdd, tx);
                } catch (NoSuchElementException e) {
                    break;
                }
            }
            return null;
        });

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
            Block potentialNewLatestBlock = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB().get(b.getHashOfLastNonMainBranchBlockReceived()));
            if (!previousHash.equals(potentialNewLatestBlock.getCurrentHash())) {
                LOGGER.info("While working on a block the miner received a new latest block from neighbours. Recycling TXs and starting again.");
                return null;
            }
        } while (nonce < 0);

        return new Block(newBlockIndex, transactionsToAdd, previousHash, nonce, currentTimestamp);
    }

    private Block getAttackedBlock(String failInfoText) {
        return SynchronizedBlockchainWrapper.useBlockchain(b -> {
            Transaction attackedTransaction = b.findTransactionInMainChainById(this.cancelledTxId);
            return b.getMainBranch().stream().filter(x -> x.getTransactions().contains(attackedTransaction)).findFirst().orElseThrow(() ->
                    new IllegalArgumentException(failInfoText));
        });
    }

    private void createTxToSelf(List<Transaction> transactionsToAdd) {
        //TODO create tx to self
    }

    private void tryToAddUnconfirmedTransactions(List<Transaction> transactionsToAdd, Transaction unconfirmedTransaction) {

        if (unconfirmedTransaction.getId().equals(cancelledTxId))
            return;

        if (transactionsToAdd.size() == MAX_TRANSACTIONS_PER_BLOCK) {
            return;
        }

        if (checkTransactionHash(unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner has invalid hash - aborting!");
            return;
        }

        if (checkIfTransactionIsAlreadyIncluded(transactionsToAdd, unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner is already included in the new block being mined - skipping!");
            return;
        }

        if (checkIfTransactionIsAlreadyInBlockchain(unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner is already included in the current blockchain - skipping!");
            return;
        }

        // Check if a tx added in an earlier iteration does not collide with our referenced outputs
        if (checkTransactionForAlreadySpentInputs(transactionsToAdd, unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner uses already used inputs! - skipping!");
            return;
        }

        // TODO: need to check if this tx does not reference a tx that was removed by the attackers
        // (because it was the targeted tx or a reward/fee tx

        LOGGER.info("AttackMiner: Tx (id: " + unconfirmedTransaction.getId() + ") has been definitely added to the new block - OK");
        transactionsToAdd.add(unconfirmedTransaction);
    }

}
