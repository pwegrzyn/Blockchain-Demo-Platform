package blockchain.protocol;

import blockchain.crypto.Sha256Proxy;
import blockchain.model.*;
import blockchain.net.FullNode;
import javafx.scene.SnapshotParameters;

import javax.sound.midi.SysexMessage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;


public class AttackMiner extends Miner {

    private static final Logger LOGGER = Logger.getLogger(AttackMiner.class.getName());
    private String cancelledTxId;
    // List of all the transactions that get removed in an attack (obviously contains the targeted TX
    // but also all TXs that reference this TX or reference a reward/fee tx
    private Set<Transaction> removedTransactions;

    public AttackMiner(String cancelledTxId, FullNode node) {
        super(node);
        this.cancelledTxId = cancelledTxId;
        this.MAX_TRANSACTIONS_PER_BLOCK = 1;
        this.removedTransactions = new HashSet<>();
    }

    @Override
    Block mineBlock() throws InterruptedException {
        String failInfoText = "Could not find block with given transaction in main branch in order to perform an attack.";

        LOGGER.info("Starting Attack Miner thread...");
        // try to continue the work of other attackers
        //Block latestBlockTmp = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB().get(b.getHashOfLastNonMainBranchBlockReceived()));
        String hashOfLastNonMainBranchBlockReceived = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getHashOfLastNonMainBranchBlockReceived);
        ConcurrentMap<String, Block> blockDB = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getBlockDB);
        Block latestBlockTmp = hashOfLastNonMainBranchBlockReceived == null ? null : blockDB.get(hashOfLastNonMainBranchBlockReceived);
        // or if you are the first attacker...
        if (latestBlockTmp == null) {
            LOGGER.info("Starting new attack branch...");
            Block attackedBlock = getAttackedBlock(failInfoText);
            latestBlockTmp = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB().get(attackedBlock.getPreviousHash()));
        } else {
            LOGGER.info("Continuing an existing attack branch...");
        }
        // If you were still unable to find the valid latest attacker block then something is not right
        if (latestBlockTmp == null) {
            throw new IllegalStateException(failInfoText);
        }

        // Static typing gainz
        final Block latestBlock = latestBlockTmp;

        int newBlockIndex = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();
        LOGGER.info("Trying to make a new block that follows the block with hash: " + previousHash);

        /*Go through all the unconfirmed transactions and pick at most MAX_TRANSACTIONS_PER_BLOCK of them to be included
        in the next block */
        List<Transaction> transactionsToAdd = new LinkedList<>();

        SynchronizedBlockchainWrapper.useBlockchain(b -> {
            // Prioritize adding transactions that will become invalidated
            int indexOfAttackedBlock = b.getMainBranch().indexOf(getAttackedBlock(failInfoText));
            List<Block> invalidatedBlocks = new LinkedList<>(b.getMainBranch().subList(0, indexOfAttackedBlock + 1));
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
            LOGGER.info("Attacking Miner creates an empty block with only his reward");
            // To make sure the polling Thread will have a chance to stop this AttackMiner
            // right it time after switching from the attack branch to the new main branch
            Thread.sleep(1000);
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
            //Block potentialNewLatestBlock = SynchronizedBlockchainWrapper.useBlockchain(b -> b.getBlockDB().get(b.getHashOfLastNonMainBranchBlockReceived()));
            String lastBlockHashFromAttackers = SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getHashOfLastNonMainBranchBlockReceived);
            Block potentialNewLatestBlock = hashOfLastNonMainBranchBlockReceived == null ? null : blockDB.get(lastBlockHashFromAttackers);
            if (potentialNewLatestBlock != null && !previousHash.equals(potentialNewLatestBlock.getCurrentHash())) {
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

    private void tryToAddUnconfirmedTransactions(List<Transaction> transactionsToAdd, Transaction unconfirmedTransaction) {

        if (transactionsToAdd.size() == MAX_TRANSACTIONS_PER_BLOCK) {
            return;
        }

        if (unconfirmedTransaction.getId().equals(cancelledTxId)) {
            this.removedTransactions.add(unconfirmedTransaction);
            return;
        }

        // REWARD and FEE will never be inside unconfirmedTransactions queue so we can
        // safely return if this tx is of any of these types because that means it comes from
        // the invalidated part of the blockchain
        if (unconfirmedTransaction.getType() == TransactionType.REWARD ||
            unconfirmedTransaction.getType() == TransactionType.FEE) {
            this.removedTransactions.add(unconfirmedTransaction);
            return;
        }

        if (this.removedTransactions.contains(unconfirmedTransaction)) {
            return;
        }

        boolean found = false;
        for (TransactionInput input : unconfirmedTransaction.getInputs()) {
            Transaction referencedTx = SynchronizedBlockchainWrapper.useBlockchain(b -> b.findTransaction(input.getPreviousTransactionHash()));
            if (this.removedTransactions.contains(referencedTx)) {
                this.removedTransactions.add(unconfirmedTransaction);
                found = true;
            }
        }
        if (found) {
            return;
        }

        if (checkTransactionHash(unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner has invalid hash - aborting!");
            this.removedTransactions.add(unconfirmedTransaction);
            return;
        }

        if (checkIfTransactionIsAlreadyIncluded(transactionsToAdd, unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner is already included in the new block being mined - skipping!");
            this.removedTransactions.add(unconfirmedTransaction);
            return;
        }

        if (checkIfTransactionIsAlreadyInBlockchain(unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner is already included in the current blockchain - skipping!");
            this.removedTransactions.add(unconfirmedTransaction);
            return;
        }

        // Check if a tx added in an earlier iteration does not collide with our referenced outputs
        if (checkTransactionForAlreadySpentInputs(transactionsToAdd, unconfirmedTransaction)) {
            LOGGER.warning("Newly added tx in miner uses already used inputs! - skipping!");
            this.removedTransactions.add(unconfirmedTransaction);
            return;
        }

        LOGGER.info("AttackMiner: Tx (id: " + unconfirmedTransaction.getId() + ") has been definitely added to the new block - OK");
        transactionsToAdd.add(unconfirmedTransaction);
    }

}
