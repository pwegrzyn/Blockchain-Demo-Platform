package blockchain.model;

import blockchain.crypto.Sha256Proxy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class Block implements Serializable {

    private final int index;
    private final List<Transaction> transactions;
    private final String previousHash;
    private final String currentHash;
    private final int nonce;
    private final long timestamp;

    public Block(int index, List<Transaction> transactions, String previousHash,
                 int nonce, long timestamp) {
        this.index = index;
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.currentHash = calculateBlockHash(index, transactions, previousHash, timestamp, nonce);
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public Transaction findTransaction(String hashToFind) {
        for (Transaction tx : this.transactions) {
            if (tx.getHash().equals(hashToFind)) {
                return tx;
            }
        }
        return null;
    }

    public Transaction findTransactionById(String id) {
        for (Transaction tx : this.transactions) {
            if (tx.getId().equals(id)) {
                return tx;
            }
        }
        return null;
    }

    public static Block getGenesisBlock() {
        String txId = "0";
        int index = 0;
        int nonce = 0;
        String previousHash = "0";
        String receiver = "0";
        BigDecimal amount = new BigDecimal(50.0);
        long timestamp = 0;
        List<TransactionOutput> genesisTransactionOutputs = new LinkedList<>();
        genesisTransactionOutputs.add(new TransactionOutput(amount, receiver));
        Transaction genesisTransaction = new Transaction(txId, TransactionType.GENESIS,
                new LinkedList<>(), genesisTransactionOutputs);
        List<Transaction> initialTransactionList = new LinkedList<>();
        initialTransactionList.add(genesisTransaction);
        Block genesisBlock = new Block(index, initialTransactionList, previousHash, nonce, timestamp);
        return genesisBlock;
    }

    public static String calculateBlockHash(int index, List<Transaction> transactions, String previousHash, long timestamp,
                                            int nonce) {
        return Sha256Proxy.calculateShaHashWithNonce(Block.getStringToHash(index, transactions, previousHash, timestamp), nonce);
    }

    public static String getStringToHash(int index, List<Transaction> transactions, String previousHash, long timestamp) {
        JsonObject jsonBlock = new JsonObject();
        jsonBlock.addProperty("index", index);
        jsonBlock.addProperty("previousHash", previousHash);
        jsonBlock.addProperty("timestamp", timestamp);
        JsonArray transactionsArray = (JsonArray) new Gson().toJsonTree(transactions, new TypeToken<List<Transaction>>() {
        }.getType());
        jsonBlock.add("transactions", transactionsArray);
        // Need to make sure keys are always in the right order in the JSON
        String unorderedJson = jsonBlock.toString();
        Gson gson = new Gson();
        TreeMap<String, Object> map = gson.fromJson(unorderedJson, TreeMap.class);
        return gson.toJson(map);
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
                timestamp == block.timestamp &&
                Objects.equals(transactions, block.transactions) &&
                Objects.equals(previousHash, block.previousHash) &&
                currentHash.equals(block.currentHash);
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
