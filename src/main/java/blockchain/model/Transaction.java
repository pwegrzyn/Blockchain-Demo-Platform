package blockchain.model;

public class Transaction {

    private final String sender;
    private final String receiver;
    private final double amount;
    private final long timestamp;
    private final String hash;
    private final String signature;

    public Transaction(String sender, String receiver, double amount, long timestamp, String hash, String signature) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = timestamp;
        this.hash = hash;
        this.signature = signature;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getHash() {
        return hash;
    }

    public String getSignature() {
        return signature;
    }
}
