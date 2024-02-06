package net.demycode.minecraft_copilot_mod;

import java.util.Map;

import ai.onnxruntime.*;
import ai.onnxruntime.OrtSession.Result;
import net.minecraft.world.level.block.state.BlockState;

public class BlockProposer extends Thread {
    private BlockState[][][] blockRegion = null;
    public BlockState[][][] resultBlockRegion = null;
    public OrtSession session = null;
    public OrtEnvironment env = null;
    public Result results = null;

    public BlockProposer(BlockState[][][] blockRegion, OrtSession session, OrtEnvironment env) {
        this.blockRegion = blockRegion;
        this.session = session;
        this.env = env;
    }

    @Override
    public void run() {
        float[][] sourceArray = new float[28][28]; // assume your data is loaded into a float array
        OnnxTensor tensorFromArray = null;
        try {
            tensorFromArray = OnnxTensor.createTensor(this.env, sourceArray);
        } catch (Exception e) {
            System.out.println("Failed to create tensor");
            return;
        }
        Map<String, OnnxTensor> inputs = Map.of("input", tensorFromArray);
        try {
            this.results = session.run(inputs);
        } catch (Exception e) {
            System.out.println("Failed to run session");
        }
        resultBlockRegion = blockRegion;
    }
}
