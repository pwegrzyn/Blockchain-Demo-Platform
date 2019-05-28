package blockchain.model;

import java.io.Serializable;

public enum TransactionType implements Serializable {

    // Transaction composed of leftover outputs from transactions included in a new-mined block by a miner
    FEE,
    // Transaction with a set value for the creator of a new block
    REWARD,
    // Special case for the first transaction in the blockchain
    GENESIS,
    // Regular transaction made by users (with arbitrarily many inputs and at most 2 outputs)
    REGULAR

}
