package blockchain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class TransactionOutput implements Serializable {

    private final BigDecimal amount;
    private final String receiverAddress;

    public TransactionOutput(BigDecimal amount, String receiverAddress) {
        this.amount = amount;
        this.receiverAddress = receiverAddress;
    }

    public BigDecimal getAmount() {
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
        return that.amount.equals(this.amount) &&
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
