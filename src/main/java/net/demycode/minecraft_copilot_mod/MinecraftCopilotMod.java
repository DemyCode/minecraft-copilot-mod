package net.demycode.minecraft_copilot_mod;

import org.slf4j.Logger;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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

    public BlockProposer blockProposer = new BlockProposer(null);

    public MinecraftCopilotMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        modEventBus.addListener(this::commonSetup);
        forgeEventBus.addListener(this::placedBlockEvent);
        forgeEventBus.addListener(this::displayFakeDirtBlock);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        LOGGER.info("HELLO FROM CLIENT SETUP");
        LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    public void placedBlockEvent(BlockEvent.EntityPlaceEvent event) {
        LOGGER.info("Block placed");
        LOGGER.info("BLOCK >> {}", event.getState().getBlock());
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getCameraEntity() == null || mc.level == null)
            return;
        LOGGER.info(event.getEntity().toString());
        LOGGER.info(mc.player.toString());
        lastPos = event.getPos();
        lastState = event.getState();

        BlockState[][][] blockRegion = new BlockState[16][16][16];
        for (int x = 0; x < 16; x++)
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    blockRegion[x][y][z] = Blocks.DIRT.defaultBlockState();

        blockProposer.interrupt();
        blockProposer = new BlockProposer(blockRegion);
        blockProposer.start();
    }

    // https://github.com/AdvancedXRay/XRay-Mod/blob/main/src/main/java/pro/mikey/xray/xray/Render.java#L16
    public void displayFakeDirtBlock(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)
            return;
        if (lastPos == null || lastState == null || mc.level == null || mc.player == null
                || mc.getCameraEntity() == null)
            return;
        if (blockProposer.isAlive())
            return;

        BlockState bs = lastState;
        BlockPos bp = lastPos;
        BlockRenderDispatcher renderer = Minecraft.getInstance().getBlockRenderer();
        ModelData modelData = renderer.getBlockModel(bs).getModelData(mc.level, bp, bs,
                mc.level.getModelDataManager().getAt(bp));

        Vec3 view = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
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
