package blockchain.model;

import blockchain.util.Utils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class TransactionOutput implements Serializable {

    private final BigDecimal amount;
    private final String receiverAddress;
    private final String uuid;

    public TransactionOutput(BigDecimal amount, String receiverAddress) {
        this.amount = amount;
        this.receiverAddress = receiverAddress;
        this.uuid = Utils.generateRandomString(16);
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
        return amount.equals(that.amount) &&
                receiverAddress.equals(that.receiverAddress) &&
                uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, receiverAddress, uuid);
    }

    @Override
    public String toString() {
        return "TransactionOutput{" +
                "amount=" + amount +
                ", receiverAddress='" + receiverAddress + '\'' +
                '}';
    }

}
