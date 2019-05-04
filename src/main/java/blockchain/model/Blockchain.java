package blockchain.model;

import java.util.*;

public class Blockchain {

    private List<Block> blockList;
    private Queue<Transaction> unconfirmedTransactions;

    public Blockchain() {
        this.blockList = new LinkedList<>();
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

    public List<Block> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<Block> blockList) {
        this.blockList = blockList;
    }

    public Queue<Transaction> getUnconfirmedTransactions() {
        return unconfirmedTransactions;
    }

    public void setUnconfirmedTransactions(Queue<Transaction> unconfirmedTransactions) {
        this.unconfirmedTransactions = unconfirmedTransactions;
    }

    // The queue of unconfirmed transactions is sorted by the maximum possible fee for miners
    private static class MaximumFeeComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            // TODO compare transactions to find which one gives a bigger fee to the miner
            return 0;
        }
    }

}