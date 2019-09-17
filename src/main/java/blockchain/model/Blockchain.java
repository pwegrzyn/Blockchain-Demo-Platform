package blockchain.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class Blockchain implements Serializable {

    // FIXME: get rid of Observables for serialization in JGroups State Transfer
    private SimpleObjectProperty<Block> latestBlock;
    private ConcurrentMap<String, Block> blockDB;
    // Mempool of unconfirmed transactions
    private Queue<Transaction> unconfirmedTransactions;

    public Blockchain() {
        this.blockDB = new ConcurrentHashMap<>();
        this.unconfirmedTransactions = new PriorityQueue<>(new MaximumFeeComparator());
    }

    public Block getLatestBlock() {
        if (this.latestBlock == null) return null;
        return this.latestBlock.get();
    }

    public Transaction findTransaction(String hash) {
        for (Block block : this.blockDB.values()) {
            Transaction result = block.findTransaction(hash);
            if (result != null) return result;
        }
        return null;
    }

    public List<Block> getMainBranch() {
        if (this.latestBlock == null) return Collections.emptyList();
        List<Block> result = new LinkedList<>();
        String hashPtr = this.latestBlock.get().getCurrentHash();
        // Genesis Block has the string "0" as its previous hash field
        while (!hashPtr.equals("0")) {
            Block blockToAdd = this.blockDB.get(hashPtr);
            result.add(blockToAdd);
            hashPtr = blockToAdd.getPreviousHash();
        }
        return result;
    }

    public Block findBlock(String hash) {
        return this.blockDB.get(hash);
    }

    public void recycleInvalidBlock(Block newMinedBlock) {
        /*The passed block was marked as invalid by the network, we need to extract all the txs included in it and add them
        to the pool of unconfirmed txs again*/
        // TODO recycling invalid block
    }

    public void addBlock(Block newMinedBlock) {
        this.blockDB.put(newMinedBlock.getCurrentHash(), newMinedBlock);
        if (this.latestBlock == null) {
            this.latestBlock = new SimpleObjectProperty<>(newMinedBlock);
            return;
        }
        if (this.latestBlock.get().getCurrentHash().equals(newMinedBlock.getPreviousHash())) {
            this.latestBlock.set(newMinedBlock);
        }
        //TODO verify this
        if (this.latestBlock.get().getIndex() < newMinedBlock.getIndex()) {
            this.latestBlock.set(newMinedBlock);
        }
        return;
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

    // The queue of unconfirmed transactions is sorted by the maximum possible fee for miners
    private static class MaximumFeeComparator implements Comparator<Transaction>, Serializable {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            // TODO compare transactions to find which one gives a bigger fee to the miner
            return 0;
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        latestBlock = new SimpleObjectProperty<>((Block)stream.readObject());
        System.out.println("readObject " + latestBlock);
        blockDB = (ConcurrentMap<String, Block>) stream.readObject();
        unconfirmedTransactions = (Queue<Transaction>) stream.readObject();
        System.out.println("readObject " + latestBlock);
        System.out.println("blockDB  " + blockDB);
        System.out.println("unconfirmedTransactions " + unconfirmedTransactions);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(latestBlock.get());
        stream.writeObject(blockDB);
        stream.writeObject(unconfirmedTransactions);
        System.out.println("writeObject " + latestBlock.get());
        System.out.println("blockDB  " + blockDB);
        System.out.println("unconfirmedTransactions " + unconfirmedTransactions);
    }

}
