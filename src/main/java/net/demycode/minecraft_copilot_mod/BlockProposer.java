package net.demycode.minecraft_copilot_mod;

import net.minecraft.world.level.block.state.BlockState;

public class BlockProposer extends Thread {
    private BlockState[][][] blockRegion = null;
    public BlockState[][][] resultBlockRegion = null;

    public BlockProposer(BlockState[][][] blockRegion) {
        this.blockRegion = blockRegion;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        resultBlockRegion = blockRegion;
    }
}
