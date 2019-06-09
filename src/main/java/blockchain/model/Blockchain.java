package blockchain.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.Serializable;
import java.util.*;


public class Blockchain implements Serializable {

    // Arbitrarily sorted list of blocks in the block-dag (main branch as well as all past forks)
    private List<Block> blockList;
    // List of block hashes added to the blockchain
    private ObservableList<String> blockHashList;
    // Mempool of unconfirmed transactions
    private Queue<Transaction> unconfirmedTransactions;

    public Blockchain() {
        this.blockList = new LinkedList<>();
        this.blockHashList = FXCollections.observableList(new LinkedList<>());
        this.unconfirmedTransactions = new PriorityQueue<>(new MaximumFeeComparator());
    }

    public Block getLatestBlock() {
        //TODO fetching the last block in the blockchain
        return null;
    }

    public Transaction findTransaction(String hash) {
        for(Block block : this.blockList) {
            Transaction result = block.findTransaction(hash);
            if(result != null) return result;
        }
        return null;
    }

    public Block findBlock(String hash){
        for(Block block : this.blockList){
            if(block.getCurrentHash().equals(hash)){
                return block;
            }
        }
        return null;
    }

    public void recycleInvalidBlock(Block newMinedBlock) {
        /*The passed block was marked as invalid by the network, we need to extract all the txs included in it and add them
        to the pool of unconfirmed txs again*/
        // TODO recycling invalid block
    }

    public void addBlock(Block newMinedBlock) {
        // TODO adding new block to the blockchain (need to handle chain branching)
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<Block> blockList) {
        this.blockList = blockList;
    }

    public List<String> getBlockHashList() {
        return blockHashList;
    }

    public Queue<Transaction> getUnconfirmedTransactions() {
        return unconfirmedTransactions;
    }

    public void setUnconfirmedTransactions(Queue<Transaction> unconfirmedTransactions) {
        this.unconfirmedTransactions = unconfirmedTransactions;
    }

    // The queue of unconfirmed transactions is sorted by the maximum possible fee for miners
    private static class MaximumFeeComparator implements Comparator<Transaction>, Serializable {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            // TODO compare transactions to find which one gives a bigger fee to the miner
            return 0;
        }
    }

}
