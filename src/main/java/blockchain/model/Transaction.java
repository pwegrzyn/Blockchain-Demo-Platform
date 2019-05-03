package blockchain.model;

import java.util.List;
import java.util.Objects;

public class Transaction {

    private final String id;
    private final String hash;
    private final TransactionType type;
    // Any number of input transactions
    private final List<TransactionInput> inputs;
    // And at max 2 outputs (to recipient and leftover change to self)
    private final List<TransactionOutput> outputs;

    public Transaction(String id, String hash, TransactionType type, List<TransactionInput> inputs, List<TransactionOutput> outputs) {
        this.id = id;
        this.hash = hash;
        this.type = type;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public String getId() {
        return id;
    }

    public String getHash() {
        return hash;
    }

    public TransactionType getType() {
        return type;
    }

    public List<TransactionInput> getInputs() {
        return inputs;
    }

    public List<TransactionOutput> getOutputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id.equals(that.id) &&
                hash.equals(that.hash) &&
                type == that.type &&
                Objects.equals(inputs, that.inputs) &&
                Objects.equals(outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hash, type, inputs, outputs);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", hash='" + hash + '\'' +
                ", type=" + type +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }
}
