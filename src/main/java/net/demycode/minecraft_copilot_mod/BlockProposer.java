package net.demycode.minecraft_copilot_mod;

import java.util.Map;
import java.util.Optional;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import net.minecraft.world.level.block.state.BlockState;

public class BlockProposer extends Thread {
    public BlockState[][][] blockRegion = null;
    public BlockState[][][] resultBlockRegion = null;
    public OrtSession session;
    public OrtEnvironment env;
    public Map<String, Integer> minecraftIdToCopilotId;
    public Map<Integer, String> copilotIdToMinecraftId;
    public Map<String, BlockState> minecraftCopilotIdToDefaultBlockState;

    public BlockProposer(BlockState[][][] blockRegion, OrtSession session, OrtEnvironment env,
            Map<String, Integer> minecraftIdToCopilotId, Map<Integer, String> copilotIdToMinecraftId,
            Map<String, BlockState> minecraftCopilotIdToDefaultBlockState) {
        this.blockRegion = blockRegion;
        this.session = session;
        this.env = env;
        this.minecraftIdToCopilotId = minecraftIdToCopilotId;
        this.copilotIdToMinecraftId = copilotIdToMinecraftId;
        this.minecraftCopilotIdToDefaultBlockState = minecraftCopilotIdToDefaultBlockState;
    }

    @Override
    public void run() {
        // Init array
        // batch size, channels, height, width, depth
        float[][][][][] sourceArray = new float[1][minecraftIdToCopilotId.size()][16][16][16];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    for (int l = 0; l < minecraftIdToCopilotId.size(); l++) {
                        sourceArray[0][l][i][j][k] = 0;
                    }
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    BlockState block = blockRegion[i][j][k];
                    String blockId = block.getBlock().getName().getString();
                    blockId = blockId.replaceAll("\\[.*\\]", "");
                    int idToFill = minecraftIdToCopilotId.get("minecraft:air");
                    if (minecraftIdToCopilotId.containsKey(blockId)) {
                        idToFill = minecraftIdToCopilotId.get(blockId);
                    }
                    sourceArray[0][idToFill][i][j][k] = 1;
                }
            }
        }
        OnnxTensor tensorFromArray = null;
        try {
            tensorFromArray = OnnxTensor.createTensor(this.env, sourceArray);
        } catch (Exception e) {
            System.out.println("Failed to create tensor");
            e.printStackTrace();
        }
        Map<String, OnnxTensor> inputs = Map.of("input", tensorFromArray);
        Result result;
        try {
            result = session.run(inputs);
        } catch (Exception e) {
            System.out.println("Failed to run session");
            e.printStackTrace();
        }
        Optional<OnnxValue> value = result.get("output");
        System.out.println(value);
        if (value.isPresent()) {
            OnnxValue onnxValue = value.get();
            float resultArray[][][][][] = new float[1][minecraftIdToCopilotId.size()][16][16][16];
            try {
                resultArray = (float[][][][][]) onnxValue.getValue();
            } catch (Exception e) {
                System.out.println("Failed to get value");
            }
            this.resultBlockRegion = new BlockState[16][16][16];
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    for (int k = 0; k < 16; k++) {
                        int maxIndex = 0;
                        float max = 0;
                        for (int l = 0; l < minecraftIdToCopilotId.size(); l++) {
                            if (resultArray[0][l][i][j][k] > max) {
                                max = resultArray[0][l][i][j][k];
                                maxIndex = l;
                            }
                        }
                        String blockId = copilotIdToMinecraftId.get(maxIndex);
                        BlockState block = minecraftCopilotIdToDefaultBlockState.get(blockId);
                        this.resultBlockRegion[i][j][k] = block;
                    }
                }
            }
        }
    }
}
