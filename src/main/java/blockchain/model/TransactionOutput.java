package blockchain.model;

import java.util.Objects;

public class TransactionOutput {

    private final double amount;
    private final String receiverAddress;

    public TransactionOutput(double amount, String receiverAddress) {
        this.amount = amount;
        this.receiverAddress = receiverAddress;
    }

    public double getAmount() {
        return amount;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionOutput that = (TransactionOutput) o;
        return Double.compare(that.amount, amount) == 0 &&
                Objects.equals(receiverAddress, that.receiverAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, receiverAddress);
    }

    @Override
    public String toString() {
        return "TransactionOutput{" +
                "amount=" + amount +
                ", receiverAddress='" + receiverAddress + '\'' +
                '}';
    }

}
