package net.demycode.minecraft_copilot_mod;

import java.io.FileReader;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.SessionOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MinecraftCopilotMod.MODID)
public class MinecraftCopilotMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "minecraft_copilot_mod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public BlockPos lastPos = null;
    public BlockState lastState = null;

    public BlockProposer blockProposer = null;
    public String remoteModelPath = "https://minecraft-copilot-models.s3.amazonaws.com/best_model.onnx";
    public String remoteJsonIdToIntPath = "https://minecraft-copilot-models.s3.amazonaws.com/unique_blocks_dict.json";
    public String localModelPath = "model.onnx";
    public String localJsonIdToIntPath = "id_to_int.json";
    public Map<String, Integer> minecraftIdToCopilotId = null;
    public Map<Integer, String> copilotIdToMinecraftId = null;
    public Map<String, BlockState> minecraftIdToDefaultBlockState = null;
    public ModelDownloader modelDownloader = new ModelDownloader(remoteModelPath, remoteJsonIdToIntPath, localModelPath,
            localJsonIdToIntPath);

    public OrtSession session = null;
    public OrtEnvironment env = OrtEnvironment.getEnvironment();

    public MinecraftCopilotMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        modEventBus.addListener(this::onClientSetup);
        forgeEventBus.addListener(this::placedBlockEvent);
        forgeEventBus.addListener(this::renderLevel);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        LOGGER.info("HELLO FROM MINECRAFT COPILOT CLIENT");
        minecraftIdToDefaultBlockState = new java.util.HashMap<String, BlockState>();
        ForgeRegistries.BLOCKS.forEach((block) -> {
            String blockId = ForgeRegistries.BLOCKS.getKey(block).toString();
            blockId = blockId.replaceAll("\\[.*\\]", ""); // Remove block state
            minecraftIdToDefaultBlockState.put(blockId, block.defaultBlockState());
        });
        LOGGER.info(minecraftIdToDefaultBlockState.toString());
        modelDownloader = new ModelDownloader(remoteModelPath, remoteJsonIdToIntPath, localModelPath,
                localJsonIdToIntPath);
        modelDownloader.start();
    }

    public void placedBlockEvent(BlockEvent.EntityPlaceEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (this.modelDownloader.isAlive()) {
            mc.player.sendSystemMessage(Component.literal("Minecraft Copilot: Model download still in progress"));
            return;
        }
        if (!this.modelDownloader.isDownloaded) {
            mc.player.sendSystemMessage(Component.literal("Minecraft Copilot: Model download failed"));
            return;
        }
        int gpuDeviceId = 0; // The GPU device ID to execute on
        if (session == null) {
            SessionOptions sessionOptions = new OrtSession.SessionOptions();
            try {
                sessionOptions.addCUDA(gpuDeviceId);
            } catch (Exception e) {
                System.out.println("Failed to add CUDA");
                mc.player.sendSystemMessage(
                        Component.literal("Minecraft Copilot: CUDA not available. Performance will be degraded."));
                e.printStackTrace();
            }
            try {
                session = env.createSession("model.onnx", sessionOptions);
            } catch (Exception e) {
                System.out.println("Failed to create session");
                mc.player.sendSystemMessage(
                        Component.literal("Minecraft Copilot: Failed to create session."));
                e.printStackTrace();
            } finally {
                sessionOptions.close();
                System.out.println("Session created");
                mc.player.sendSystemMessage(
                        Component.literal("Minecraft Copilot: ONNX session created. Ready to assist."));
            }
        }
        if (minecraftIdToCopilotId == null || copilotIdToMinecraftId == null) {
            Gson gson = new Gson();
            FileReader reader;
            try {
                reader = new FileReader(this.localJsonIdToIntPath);
            } catch (Exception e) {
                System.out.println("Failed to read json file");
                mc.player.sendSystemMessage(
                        Component.literal("Minecraft Copilot: Failed to read json file."));
                e.printStackTrace();
                return;
            }
            minecraftIdToCopilotId = gson.fromJson(reader, new TypeToken<Map<String, Integer>>() {
            }.getType());
            copilotIdToMinecraftId = new java.util.HashMap<Integer, String>();
            for (Map.Entry<String, Integer> entry : minecraftIdToCopilotId.entrySet()) {
                copilotIdToMinecraftId.put(entry.getValue(), entry.getKey());
            }
        }

        BlockPos pos = event.getPos();
        BlockState state = mc.level.getBlockState(pos);
        lastPos = pos;
        lastState = state;
        BlockState[][][] blockRegionInput = new BlockState[16][16][16];
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    blockRegionInput[x][y][z] = mc.level.getBlockState(pos.offset(x, y, z));
                }
            }
        }
        if (blockProposer != null && blockProposer.isAlive()) {
            blockProposer.interrupt();
        }
        blockProposer = new BlockProposer(blockRegionInput,
                this.session,
                this.env,
                this.minecraftIdToCopilotId,
                this.copilotIdToMinecraftId,
                this.minecraftIdToDefaultBlockState);
        blockProposer.start();
    }

    // https://github.com/AdvancedXRay/XRay-Mod/blob/main/src/main/java/pro/mikey/xray/xray/Render.java#L16
    public void renderLevel(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)
            return;
        if (lastPos == null || lastState == null || mc.level == null || mc.player == null
                || mc.getCameraEntity() == null)
            return;
        if (blockProposer == null || blockProposer.isAlive())
            return;

        BlockState bs = lastState;
        BlockPos bp = lastPos;
        BlockRenderDispatcher renderer = mc.getBlockRenderer();
        ModelData modelData = renderer.getBlockModel(bs).getModelData(mc.level, bp, bs,
                mc.level.getModelDataManager().getAt(bp));

        Vec3 view = mc.getEntityRenderDispatcher().camera.getPosition();
        PoseStack matrix = event.getPoseStack();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    matrix.pushPose();
                    matrix.translate(-view.x() + bp.getX() + x, -view.y() + bp.getY() + y,
                            -view.z() + bp.getZ() + z);
                    renderer.renderSingleBlock(
                            blockProposer.resultBlockRegion[x][y][z],
                            matrix,
                            mc.renderBuffers().crumblingBufferSource(),
                            15728880,
                            OverlayTexture.NO_OVERLAY,
                            modelData,
                            RenderType.translucent());
                    matrix.popPose();
                }
            }
        }
    }
}
