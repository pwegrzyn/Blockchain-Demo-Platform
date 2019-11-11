package blockchain.protocol;


import blockchain.model.*;
import blockchain.net.BlockBroadcastResult;
import blockchain.net.FullNode;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

public class AttackMiner extends Miner {
    public AttackMiner(FullNode node) {
        super(node);
    }

    @Override
    protected BlockBroadcastResult broadcastMinedBlock(Block newMinedBlock){
        this.fullNode.broadcastAttackInfo(newMinedBlock.getCurrentHash());
        return this.fullNode.broadcastNewBlock(newMinedBlock);
    }

    @Override
    protected List<Transaction> addTransactionsToBeMined() {
        Queue<Transaction> transactionQueue = new LinkedList<>(SynchronizedBlockchainWrapper.useBlockchain(Blockchain::getUnconfirmedTransactions));

        List<Transaction> transactionsToAdd = new LinkedList<>();
        while (transactionsToAdd.size() < MAX_TRANSACTIONS_PER_BLOCK) {
            Transaction unconfirmedTransaction;
            try {
                unconfirmedTransaction = transactionQueue.remove();
            } catch (NoSuchElementException e) {
                break;
            }

            // Case 1: transaction is already included in main branch, so probably other attacking host has already mined it
            // so that's why we will remove it
            if(checkIfTransactionIsAlreadyIncluded(transactionsToAdd, unconfirmedTransaction) ||
                    checkIfTransactionIsAlreadyInBlockchain(unconfirmedTransaction)){
                SynchronizedBlockchainWrapper.useBlockchain(b -> b.getUnconfirmedTransactions().remove(unconfirmedTransaction));
                continue;
            }

            // Case 2: transaction is either spent or is yet to be added (in the future blocks), that's why we don't want to remove it entirely
            if(checkTransactionForAlreadySpentInputs(transactionsToAdd, unconfirmedTransaction) ||
            checkTransactionHash(unconfirmedTransaction)){
                continue;
            }

            LOGGER.info("Tx (id: " + unconfirmedTransaction.getId() + ") has been definitely added to the new block - OK");
            transactionsToAdd.add(unconfirmedTransaction);
        }

        SynchronizedBlockchainWrapper.useBlockchain(b -> b.getUnconfirmedTransactions().removeAll(transactionsToAdd));

        return transactionsToAdd;
    }
}
