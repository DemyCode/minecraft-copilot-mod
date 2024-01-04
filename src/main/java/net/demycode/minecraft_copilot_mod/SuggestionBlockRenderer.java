// package net.demycode.minecraft_copilot_mod;

// import com.mojang.authlib.minecraft.client.MinecraftClient;

// import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
// import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
// import net.minecraft.world.level.block.entity.BlockEntity;
// import net.minecraft.world.level.block.state.BlockState;

// // FakeBlockRenderer.java
// public class SuggestionBlockRenderer extends BlockEntityRenderer<BlockEntity> {

//     public SuggestionBlockRenderernderer(BlockEntityRenderDispatcher dispatcher) {
//         super(dispatcher);
//     }

//     @Override
//     public void render(FakeBlockEntity blockEntity, float tickDelta, MatrixStack matrices,
//             VertexConsumerProvider vertexConsumers, int light, int overlay) {
//         matrices.push();

//         matrices.translate(64, 64, 64); // Set the position
//         matrices.scale(0.5f, 0.5f, 0.5f); // Scale the block (adjust as needed)

//         BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
//         BlockState blockState = Blocks.RED_CONCRETE.getDefaultState(); // Example block state

//         blockRenderManager.renderBlockAsEntity(blockState, matrices, vertexConsumers, light, overlay);

//         matrices.pop();
//     }
// }
