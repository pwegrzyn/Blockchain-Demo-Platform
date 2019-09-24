package blockchain.model;

import blockchain.crypto.Sha256Proxy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class TransactionInput implements Serializable {

    private final String previousTransactionHash;
    private final int previousTransactionOutputIndex;
    private final String fromAddress;
    // the signature is made be taking the hash of the fields above and signing it with a my private key -
    // this way other nodes can verify than this transaction was made indeed by the owner of the public key
    private final String signature;

    public TransactionInput(String previousTransactionHash, int previousTransactionOutputIndex,
                            String fromAddress, String signature) {
        this.previousTransactionHash = previousTransactionHash;
        this.previousTransactionOutputIndex = previousTransactionOutputIndex;
        this.fromAddress = fromAddress;
        this.signature = signature;
    }

    // Hash of the TxInput data, later signed with private key of the person creating this input
    public static String calculateHash(String previousTransactionHash, int previousTransactionOutputIndex, String fromAddress) {
        JsonObject jsonBlock = new JsonObject();
        jsonBlock.addProperty("prevTxHash", previousTransactionHash);
        jsonBlock.addProperty("prevTxOutputIndex", previousTransactionOutputIndex);
        jsonBlock.addProperty("fromAddr", fromAddress);
        // Need to make sure keys are always in the right order in the JSON
        String unorderedJson = jsonBlock.toString();
        Gson gson = new Gson();
        TreeMap<String, Object> map = gson.fromJson(unorderedJson, TreeMap.class);
        return Sha256Proxy.calculateShaHash(gson.toJson(map));
    }

    public String getPreviousTransactionHash() {
        return previousTransactionHash;
    }

    public int getPreviousTransactionOutputIndex() {
        return previousTransactionOutputIndex;
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
                previousTransactionHash.equals(that.previousTransactionHash) &&
                fromAddress.equals(that.fromAddress) &&
                signature.equals(that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(previousTransactionHash, previousTransactionOutputIndex, fromAddress, signature);
    }

    @Override
    public String toString() {
        return "TransactionInput{" +
                "previousTransactionHash='" + previousTransactionHash + '\'' +
                ", previousTransactionOutputIndex=" + previousTransactionOutputIndex +
                ", fromAddress='" + fromAddress + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
