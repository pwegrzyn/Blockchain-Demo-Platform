package blockchain.model;

import java.util.Objects;

public class TransactionInput {

    private final String previousTransactionHash;
    private final int previousTransactionOutputIndex;
    private final double amount;
    private final String fromAddress;
    private final String signature;

    public TransactionInput(String previousTransactionHash, int previousTransactionOutputIndex, double amount,
                            String fromAddress, String signature) {
        this.previousTransactionHash = previousTransactionHash;
        this.previousTransactionOutputIndex = previousTransactionOutputIndex;
        this.amount = amount;
        this.fromAddress = fromAddress;
        this.signature = signature;
    }

    public String getPreviousTransactionHash() {
        return previousTransactionHash;
    }

    public int getPreviousTransactionOutputIndex() {
        return previousTransactionOutputIndex;
    }

    public double getAmount() {
        return amount;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionInput that = (TransactionInput) o;
        return previousTransactionOutputIndex == that.previousTransactionOutputIndex &&
                Double.compare(that.amount, amount) == 0 &&
                Objects.equals(previousTransactionHash, that.previousTransactionHash) &&
                Objects.equals(fromAddress, that.fromAddress) &&
                Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(previousTransactionHash, previousTransactionOutputIndex, amount, fromAddress, signature);
    }

    @Override
    public String toString() {
        return "TransactionInput{" +
                "previousTransactionHash='" + previousTransactionHash + '\'' +
                ", previousTransactionOutputIndex=" + previousTransactionOutputIndex +
                ", amount=" + amount +
                ", fromAddress='" + fromAddress + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
