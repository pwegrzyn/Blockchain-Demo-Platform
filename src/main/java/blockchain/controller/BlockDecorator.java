package blockchain.controller;

import blockchain.model.Block;

public class BlockDecorator {

    private Block block;

    public BlockDecorator(Block block){
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return "Block #" + block.getIndex();
    }
}
