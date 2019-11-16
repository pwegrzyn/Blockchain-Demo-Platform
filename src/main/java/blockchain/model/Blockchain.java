package blockchain.model;

import blockchain.protocol.Validator;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;


public class Blockchain implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(Blockchain.class.getName());

    // last block of the main (longest) branch
    private SimpleObjectProperty<Block> latestBlock;

    // last blocks of all the other branches
    private List<Block> latestBlocksInOtherBranches;

    //All the blocks currently in the system
    private ConcurrentMap<String, Block> blockDB;

    // Mempool of unconfirmed transactions
    private Queue<Transaction> unconfirmedTransactions;

    // Last non-main branch block received (used in the 51% attack scenario)
    private String hashOfLastNonMainBranchBlockReceived;

    public Blockchain() {
        this.latestBlock = new SimpleObjectProperty<>();
        this.blockDB = new ConcurrentHashMap<>();
        this.unconfirmedTransactions = new PriorityQueue<>(new MaximumFeeComparator());
        this.latestBlocksInOtherBranches = new LinkedList<>();
        this.addBlock(Block.getGenesisBlock());
    }

    public Block getLatestBlock() {
        if (this.latestBlock == null) return null;
        return this.latestBlock.get();
    }

    public SimpleObjectProperty<Block> getLatestBlockObservable() {
        return this.latestBlock;
    }

    public Transaction findTransaction(String hash) {
        for (Block block : this.blockDB.values()) {
            Transaction result = block.findTransaction(hash);
            if (result != null) return result;
        }
        return null;
    }

    public Transaction findTransactionInMainChain(String hash) {
        List<Block> mainBranch = getMainBranch();
        for (Block block : mainBranch) {
            Transaction result = block.findTransaction(hash);
            if (result != null) return result;
        }
        return null;
    }

    public Transaction findTransactionInMainChainById(String id) {
        List<Block> mainBranch = getMainBranch();
        for (Block block : mainBranch) {
            Transaction result = block.findTransactionById(id);
            if (result != null) return result;
        }
        return null;
    }

    public List<Block> getMainBranch() {
        if (latestBlock.get() == null) return Collections.emptyList();
        List<Block> result = new LinkedList<>();
        String hashPtr = latestBlock.get().getCurrentHash();
        // Genesis Block has the string "0" as its previous hash field
        while (!hashPtr.equals("0")) {
            Block blockToAdd = blockDB.get(hashPtr);
            result.add(blockToAdd);
            hashPtr = blockToAdd.getPreviousHash();
        }
        return result;
    }

    public Block findBlock(String hash) {
        return this.blockDB.get(hash);
    }

    public Block findBlockInMainChain(String hash) {
        List<Block> mainBranch = getMainBranch();
        for (Block block : mainBranch) {
            if (block.getCurrentHash().equals(hash)) {
                return block;
            }
        }
        return null;
    }

    public void addBlock(Block newMinedBlock) {
        LOGGER.info("Adding new block to the blockchain");
        this.blockDB.put(newMinedBlock.getCurrentHash(), newMinedBlock);

        // If there's no main branch then start a new one
        if (this.latestBlock.get() == null) {
            this.latestBlock.set(newMinedBlock);
            LOGGER.info("Added new block because the main branch is empty");
            return;
        }

        // If a new block builds upon the last block of the main branch then update the latestBlock
        if (this.latestBlock.get().getCurrentHash().equals(newMinedBlock.getPreviousHash()) &&
            this.latestBlock.get().getIndex() + 1 == newMinedBlock.getIndex()) {
            updateUnconfirmedTransactions(newMinedBlock);
            this.latestBlock.set(newMinedBlock);
            LOGGER.info("New block builds on top of the main branch - everything OK");
            return;
        }

        // If it does build upon the last block of any other chain then update the latestBlock in that chain
        Block blockToRemove = null;
        for (Block latestBlockInNonMainBranch : this.latestBlocksInOtherBranches) {
            if (latestBlockInNonMainBranch.getCurrentHash().equals(newMinedBlock.getPreviousHash()) &&
                    latestBlockInNonMainBranch.getIndex() + 1 == newMinedBlock.getIndex()) {
                blockToRemove = latestBlockInNonMainBranch;
            }
        }
        if (blockToRemove != null) {
            this.latestBlocksInOtherBranches.remove(blockToRemove);
            this.latestBlocksInOtherBranches.add(newMinedBlock);
            LOGGER.info("New block continues a non-main branch");
            this.hashOfLastNonMainBranchBlockReceived = newMinedBlock.getCurrentHash();
            updateMainBranch(newMinedBlock);
            return;
        }

        // We only allow forks from the main branch, so the default case is to start a new branch
        Block foundParentInMainChain = this.findBlockInMainChain(newMinedBlock.getPreviousHash());
        if (foundParentInMainChain != null && !foundParentInMainChain.getCurrentHash().equals(this.latestBlock.get().getCurrentHash())) {
            this.latestBlocksInOtherBranches.add(newMinedBlock);
            LOGGER.info("Added block started a new fork from the main branch");
            this.hashOfLastNonMainBranchBlockReceived = newMinedBlock.getCurrentHash();
            updateMainBranch(newMinedBlock);
            return;
        }

        // Report failure when a fork from a non-main branch is detected
        Block foundParentGlobally = this.findBlock(newMinedBlock.getPreviousHash());
        if (foundParentGlobally != null && !this.getMainBranch().contains(foundParentGlobally)) {
            LOGGER.warning("When adding a new block a fork from a non-main branch was detected - not allowed!");
            this.blockDB.remove(newMinedBlock.getCurrentHash());
            return;
        }

        LOGGER.warning("addBlock() method empty pass!");

    }

    private void updateMainBranch(Block newMinedBlock) {
        int maxSizeSoFar = this.getMainBranch().size();
        Block longestChainStarter = this.latestBlock.get();
        for (Block latestBlockInOtherBranch : this.latestBlocksInOtherBranches) {
            int sizeOfNonMainBranch = this.getArbitraryBranch(latestBlockInOtherBranch).size();
            if (sizeOfNonMainBranch > maxSizeSoFar) {
                maxSizeSoFar = sizeOfNonMainBranch;
                longestChainStarter = latestBlockInOtherBranch;
            }
        }
        if (longestChainStarter != this.latestBlock.get()) {
            // Update the pool of unconfirmed transactions
            updateUnconfirmedTransactions(longestChainStarter);

            // the last main branch now becomes a fork
            this.latestBlocksInOtherBranches.add(this.latestBlock.get());
            // we got a new main branch
            this.latestBlock.set(longestChainStarter);
            this.latestBlocksInOtherBranches.remove(longestChainStarter);
        }
    }

    private void updateUnconfirmedTransactions(Block longestChainStarter) {
        List<Block> currentMainChain = this.getMainBranch();
        List<Block> newMainChain = this.getArbitraryBranch(longestChainStarter);

        // Remove from the queue all the transactions that will become confirmed
        List<Transaction> txsToRemoveFromQueue = new LinkedList<>();
        for (Transaction tx : this.getUnconfirmedTransactions()) {
            for (Block block : newMainChain) {
                if (block.findTransaction(tx.getHash()) != null) {
                    txsToRemoveFromQueue.add(tx);
                }
            }
        }
        this.getUnconfirmedTransactions().removeAll(txsToRemoveFromQueue);

        // Remove from the queue all the transactions that are already spent
        txsToRemoveFromQueue = new LinkedList<>();
        for(Block block : newMainChain){
            for(Transaction tx : block.getTransactions()){
                for(TransactionInput input : tx.getInputs()){

                    for(Transaction unconfirmedTransaction : this.getUnconfirmedTransactions()){
                        for(TransactionInput unconfirmedTxInput : unconfirmedTransaction.getInputs()){
                            String usedHash = input.getPreviousTransactionHash();
                            int usedIndex = input.getPreviousTransactionOutputIndex();

                            String unconfirmedUsedHash = unconfirmedTxInput.getPreviousTransactionHash();
                            int unconfirmedUsedIndex = unconfirmedTxInput.getPreviousTransactionOutputIndex();

                            if(usedHash.equals(unconfirmedUsedHash) && usedIndex == unconfirmedUsedIndex){
                                LOGGER.info("Removing transaction from unconfirmed transactions because it is already spent: " + unconfirmedTransaction);
                                txsToRemoveFromQueue.add(unconfirmedTransaction);
                            }
                        }
                    }

                }
            }
        }

        this.getUnconfirmedTransactions().removeAll(txsToRemoveFromQueue);

        // Add to the queue all transactions that will become unconfirmed
        Validator validator = new Validator();
        for (Block blockInCurrent : currentMainChain) {
            for (Transaction transactionInCurrent : blockInCurrent.getTransactions()) {
                if (transactionInCurrent.getType() != TransactionType.REGULAR)
                    continue;

                boolean foundInNew = false;
                for (Block blockInNew : newMainChain) {
                    if (blockInNew.findTransaction(transactionInCurrent.getHash()) != null) {
                        foundInNew = true;
                        break;
                    }
                }

                boolean isTxValid = false;
                try {
                    isTxValid = validator.validateNewIncomingTX(transactionInCurrent);
                } catch (Exception e) {
                }

                if (!foundInNew && isTxValid) {
                    this.unconfirmedTransactions.add(transactionInCurrent);
                }
            }
        }
    }

    public List<Block> getArbitraryBranch(Block startBlock) {
        if (startBlock == null) return Collections.emptyList();
        List<Block> result = new LinkedList<>();
        if (startBlock.getPreviousHash().equals("0")) {
            result.add(startBlock);
            return  result;
        }
        String hashPtr = startBlock.getCurrentHash();
        // Genesis Block has the string "0" as its previous hash field
        while (!hashPtr.equals("0")) {
            Block blockToAdd = this.blockDB.get(hashPtr);
            result.add(blockToAdd);
            hashPtr = blockToAdd.getPreviousHash();
        }
        return result;
    }

    public ConcurrentMap<String, Block> getBlockDB() {
        return this.blockDB;
    }

    public void setBlockDB(ConcurrentMap<String, Block> blockDB) {
        this.blockDB = blockDB;
    }

    public Queue<Transaction> getUnconfirmedTransactions() {
        return unconfirmedTransactions;
    }

    public void setUnconfirmedTransactions(Queue<Transaction> unconfirmedTransactions) {
        this.unconfirmedTransactions = unconfirmedTransactions;
    }

    public String getHashOfLastNonMainBranchBlockReceived() {
        return this.hashOfLastNonMainBranchBlockReceived;
    }

    // The queue of unconfirmed transactions is sorted by the maximum possible fee for miners
    private static class MaximumFeeComparator implements Comparator<Transaction>, Serializable {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            // TODO compare transactions to find which one gives a bigger fee to the miner

            return 0;
        }

    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        latestBlock = new SimpleObjectProperty<>((Block) stream.readObject());
        blockDB = (ConcurrentMap<String, Block>) stream.readObject();
        unconfirmedTransactions = (Queue<Transaction>) stream.readObject();
        hashOfLastNonMainBranchBlockReceived = (String) stream.readObject();
        latestBlocksInOtherBranches = (List<Block>) stream.readObject();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(latestBlock.get());
        stream.writeObject(blockDB);
        stream.writeObject(unconfirmedTransactions);
        stream.writeObject(hashOfLastNonMainBranchBlockReceived);
        stream.writeObject(latestBlocksInOtherBranches);
    }

}
