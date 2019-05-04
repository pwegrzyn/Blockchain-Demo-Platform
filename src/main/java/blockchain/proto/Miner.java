package blockchain.proto;

import blockchain.model.*;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class Miner {

    private Blockchain blockchain;
    private Validator validator;
    private static final int MAX_TRANSACTIONS_PER_BLOCK = 5;
    private static final int MINING_DIFFICULTY = 4;

    public Miner(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.validator = new Validator();
    }

    public Block mineBlock() {
        Block latestBlock = this.blockchain.getLatestBlock();
        int newBlockIndex = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();

        /*Go through all the unconfirmed transactions and pick at most MAX_TRANSACTIONS_PER_BLOCK of them to be included
        in the next block (NOTE: Some of the logic here probably belongs to the Validator class?)*/
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
            if(!unconfirmedTransaction.getHash().equals(Transaction.calculateTransactionHash(id, inputs, outputs))) {
                continue;
            }

            boolean txAlreadyIncluded = false;
            for (Transaction tx : transactionsToAdd) {
                if(unconfirmedTransaction.getHash().equals(tx.getHash())) {
                    txAlreadyIncluded = true;
                    break;
                }
            }
            if(txAlreadyIncluded) continue;

            if(this.blockchain.findTransaction(unconfirmedTransaction.getHash()) != null) continue;

            if(!this.validator.verifySignature(unconfirmedTransaction)) continue;
        }
        if(transactionsToAdd.size() < 1) {
            System.out.println("Miner: Not enough transactions to begin mining a new block!");
            return null;
        }

        assignLeftoverValueAsFee(transactionsToAdd);

        addRewardTransaction(transactionsToAdd);

        int nonce = 0;
        long currentTimestamp = System.currentTimeMillis();
        // Upgrade to use GPUHashSolver if ready
        while(!Block.calculateBlockHash(newBlockIndex, transactionsToAdd, previousHash, currentTimestamp, nonce)
                .substring(0, 4).equals("0000")) {
            /*A new block may have been added to the chain while we were hashing, if so then we need to put back
            all the non-included txs back to the queue of unconfirmed transactions  */
            Block potentialNewLatestBlock = this.blockchain.getLatestBlock();
            if(potentialNewLatestBlock.getIndex() >= newBlockIndex || !potentialNewLatestBlock.getCurrentHash().equals(previousHash)) {
                for (Transaction tx : transactionsToAdd) {
                    if (potentialNewLatestBlock.findTransaction(tx.getHash()) == null) {
                            this.blockchain.getUnconfirmedTransactions().add(tx);
                    }
                }
                return null;
            }
            nonce++;
        }

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
