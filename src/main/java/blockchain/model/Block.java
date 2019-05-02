package blockchain.model;

import blockchain.crypto.Hash;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Block {

    private final int index;
    private final List<Transaction> transactions;
    private final String previousHash;
    private final String currentHash;
    private final int nonce;
    private final long timestamp;

    public Block(int index, List<Transaction> transactions, String previousHash, String currentHash,
                 int nonce, long timestamp) {
        this.index = index;
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public static Block getGenesisBlock() {
        int index = 0;
        int nonce = 0;
        String previousHash = "0";
        String receiver = "1";
        String sender = "0";
        double amount = 50.0;
        long timestamp = 0;
        String transactionHash = "0";
        String transactionSignature = "0";
        Transaction genesisTransaction = new Transaction(sender, receiver, amount, timestamp, transactionHash,
                transactionSignature);
        List<Transaction> initialTransactionList = new LinkedList<>();
        initialTransactionList.add(genesisTransaction);
        String currentHash = calculateBlockHash(index, initialTransactionList, previousHash, timestamp, nonce);
        Block genesisBlock = new Block(index, initialTransactionList, previousHash, currentHash, nonce, timestamp);
        return genesisBlock;
    }

    public static String calculateBlockHash(int index, List<Transaction> transactions, String previousHash, long timestamp,
                                     int nonce) {
        JsonObject jsonBlock = new JsonObject();
        jsonBlock.addProperty("index", index);
        jsonBlock.addProperty("previousHash", previousHash);
        jsonBlock.addProperty("timestamp", timestamp);
        jsonBlock.addProperty("nonce", nonce);
        JsonArray transactionsArray = (JsonArray) new Gson().toJsonTree(transactions, new TypeToken<List<Transaction>>(){}.getType());
        jsonBlock.add("transactions", transactionsArray);
        return Hash.hashSHA256(jsonBlock.getAsString());
    }

    public int getIndex() {
        return index;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public int getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return index == block.index &&
                nonce == block.nonce &&
                Objects.equals(transactions, block.transactions) &&
                Objects.equals(previousHash, block.previousHash) &&
                Objects.equals(currentHash, block.currentHash) &&
                Objects.equals(timestamp, block.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, transactions, previousHash, currentHash, nonce, timestamp);
    }

    @Override
    public String toString() {
        return "Block{" +
                "index=" + index +
                ", transactions=" + transactions +
                ", previousHash='" + previousHash + '\'' +
                ", currentHash='" + currentHash + '\'' +
                ", nonce=" + nonce +
                ", timestamp=" + timestamp +
                '}';
    }
}
