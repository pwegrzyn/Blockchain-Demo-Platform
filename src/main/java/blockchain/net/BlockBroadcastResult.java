package blockchain.net;

/*After broadcasting a new proposed block to the network we wait to get a response of other nodes what do they think about it.
If the majority of nodes response positively this means we can add it to our local chain, else we need to abandon this block.
This class hold the results of this voting process*/
public class BlockBroadcastResult {

    public boolean isConfirmed() {
        // TODO check the result of the voting process
        return true;
    }

}
