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

public class Transaction implements Serializable {

    private final String id;
    // Hash of the ID + inputs + outputs
    private final String hash;
    private final TransactionType type;
    // Any number of input transactions
    private final List<TransactionInput> inputs;
    // And at max 2 outputs (to recipient and leftover change to self)
    private final List<TransactionOutput> outputs;

    public Transaction(String id, TransactionType type, List<TransactionInput> inputs, List<TransactionOutput> outputs) {
        this.id = id;
        this.hash = calculateTransactionHash(id, inputs, outputs);
        this.type = type;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public static String calculateTransactionHash(String id, List<TransactionInput> inputs, List<TransactionOutput> outputs) {
        JsonObject jsonBlock = new JsonObject();
        jsonBlock.addProperty("id", id);
        JsonArray inputsArray = (JsonArray) new Gson().toJsonTree(inputs, new TypeToken<List<TransactionInput>>(){}.getType());
        JsonArray outputsArray = (JsonArray) new Gson().toJsonTree(outputs, new TypeToken<List<TransactionOutput>>(){}.getType());
        jsonBlock.add("inputs", inputsArray);
        jsonBlock.add("outputs", outputsArray);
        // Need to make sure keys are always in the right order in the JSON
        String unorderedJson = jsonBlock.toString();
        Gson gson = new Gson();
        TreeMap<String, Object> map = gson.fromJson(unorderedJson, TreeMap.class);
        return Sha256Proxy.calculateShaHash(gson.toJson(map));
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

    public String getCreatorAddr() {
        // special case if using the Genesis TX as an input
        if (this.type == TransactionType.GENESIS) {
            return "0";
        }
        Transaction inputTX = SynchronizedBlockchainWrapper.useBlockchain(blockchain ->
                blockchain.findTransactionInMainChain(this.inputs.get(0).getPreviousTransactionHash()));
        return inputTX.getOutputs().get(0).getReceiverAddress();
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
