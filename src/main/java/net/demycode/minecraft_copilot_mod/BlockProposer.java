package net.demycode.minecraft_copilot_mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        float[][][][][] sourceArray = new float[1][1][16][16][16];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    sourceArray[0][0][i][j][k] = 0;
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    BlockState block = blockRegion[i][j][k];
                    String blockId = block.getBlock().defaultBlockState().toString();
                    blockId = blockId.replaceAll("Block\\{", "");
                    blockId = blockId.replaceAll("\\}", "");
                    blockId = blockId.replaceAll("\\[.*\\]", ""); // Remove block state
                    if (minecraftIdToCopilotId.containsKey(blockId)) {
                        int copilotId = minecraftIdToCopilotId.get(blockId);
                        sourceArray[0][0][i][j][k] = copilotId;
                    } else {
                        sourceArray[0][0][i][j][k] = minecraftIdToCopilotId.get("minecraft:air");
                    }
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
        Map<String, OnnxTensor> inputMap = new HashMap<>();
        Set<String> requestedOutputs = new HashSet<>();
        inputMap.put("input", tensorFromArray);
        requestedOutputs.add("output");
        Result result = null;
        try {
            result = session.run(inputMap, requestedOutputs);
        } catch (Exception e) {
            System.out.println("Failed to run session");
            e.printStackTrace();
        }
        Optional<OnnxValue> value = result.get("output");
        if (value.isPresent()) {
            OnnxValue onnxValue = value.get();
            float resultArray[][][][][] = new float[1][minecraftIdToCopilotId.size()][16][16][16];
            try {
                resultArray = (float[][][][][]) onnxValue.getValue();
            } catch (Exception e) {
                System.out.println("Failed to get value");
                e.printStackTrace();
            }
            this.resultBlockRegion = new BlockState[16][16][16];
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    for (int k = 0; k < 16; k++) {
                        int maxIndex = -1;
                        float max = -999999999;
                        for (int l = 0; l < minecraftIdToCopilotId.size(); l++) {
                            if (resultArray[0][l][i][j][k] > max) {
                                max = resultArray[0][l][i][j][k];
                                maxIndex = l;
                            }
                        }
                        if (maxIndex == -1) {
                            maxIndex = minecraftIdToCopilotId.get("minecraft:air");
                        }
                        String blockId = copilotIdToMinecraftId.get(maxIndex);
                        this.resultBlockRegion[i][j][k] = minecraftCopilotIdToDefaultBlockState.get(blockId);
                    }
                }
            }
        }
        System.out.println("BlockProposer finished");
    }
}
