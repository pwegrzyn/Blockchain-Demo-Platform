package blockchain.gui;

import blockchain.config.Configuration;
import blockchain.model.Block;
import blockchain.model.Transaction;
import blockchain.model.TransactionInput;
import blockchain.model.TransactionOutput;
import blockchain.net.Node;
import blockchain.net.WalletNode;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.*;

public class SummaryTabPageController {

    @FXML private ListView usersListView;
    @FXML private Label nodeTypeLabel;
    @FXML private Label userCountLabel;
    @FXML private Label blockCountLabel;
    @FXML private Label transactionCountLabel;
    @FXML private Label currencyAmountLabel;
    @FXML private TextField publicKeyTextField;
    @FXML private TextField privateKeyTextField;

    private Node node;
    private Configuration config;

    public void setNode(Node node){
        this.node = node;
    }

    public void init(){
        config = Configuration.getInstance();
        this.publicKeyTextField.setText(config.getPublicKey());
        this.privateKeyTextField.setText(config.getPrivateKey());
        this.nodeTypeLabel.setText(node instanceof WalletNode ? "Wallet node" : "Full node");
        this.userCountLabel.setText(calculateUserCount().toString());
        this.blockCountLabel.setText(calculateBlockCount().toString());
        this.transactionCountLabel.setText(calculateTransactionCount().toString());
        this.currencyAmountLabel.setText(calculateCurrencyAmount().toString());
    }

    private Integer calculateUserCount(){
        Integer users = 0;
        Set<String> userSet = new HashSet<>();

        for(Block block : node.getBlockchain().getMainBranch()){
            for(Transaction transaction : block.getTransactions()){
                for(TransactionOutput transactionOutput : transaction.getOutputs()){
                    String newUser = transactionOutput.getReceiverAddress();

                    if(!userSet.contains(newUser)){
                        userSet.add(newUser);
                        users++;
                    }
                }
            }
        }
        return users;
    }

    private Integer calculateBlockCount(){
        return node.getBlockchain().getBlockDB().values().size();
    }

    private Integer calculateTransactionCount(){
        Integer transactions = 0;

        for(Block block : node.getBlockchain().getMainBranch()){
            transactions += block.getTransactions().size();
        }

        return transactions;
    }

    private Double calculateCurrencyAmount(){
        Map<Transaction, List<Integer>> unusedTransactions = new HashMap<>();
        Map<String, Transaction> hashTransactionMap = new HashMap<>();

        for(Block block : node.getBlockchain().getMainBranch()){
            for(Transaction transaction : block.getTransactions()){
                hashTransactionMap.put(transaction.getHash(), transaction);

                List<Integer> indexes = new LinkedList<>();

                for(int i = 0; i < transaction.getOutputs().size(); i++){
                    indexes.add(i);
                }

                unusedTransactions.put(transaction, indexes);

                for(TransactionInput transactionInput : transaction.getInputs()){
                    String referencedHash = transactionInput.getPreviousTransactionHash();
                    Transaction referencedTransaction = hashTransactionMap.get(referencedHash);

                    if(unusedTransactions.containsKey(referencedTransaction)){
                        Integer indexToBeRemoved = transactionInput.getPreviousTransactionOutputIndex();
                        unusedTransactions.get(referencedTransaction).remove(indexToBeRemoved);
                    }
                }
            }
        }

        Double sum = 0.0;

        for(Map.Entry<Transaction, List<Integer>> entry : unusedTransactions.entrySet()){
            for(Integer i : entry.getValue()){
                sum += entry.getKey().getOutputs().get(i).getAmount();
            }
        }

        return sum;
    }

}
