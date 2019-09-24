package blockchain.model;

import java.util.function.Function;

public class SynchronizedBlockchainWrapper {

    // One blockchain per JVM
    private static Blockchain blockchain;


    synchronized public static <T> T useBlockchain(Function<Blockchain, T> func){
        return func.apply(blockchain);
    }

    synchronized public static void setBlockchain(Blockchain blockchain){
        SynchronizedBlockchainWrapper.blockchain = blockchain;
    }

    public static Blockchain javaFxReadOnlyBlockchain(){
        return blockchain;
    }

}
