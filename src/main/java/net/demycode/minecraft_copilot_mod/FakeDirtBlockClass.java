// // FakeDirtBlock.java
// package net.demycode.minecraft_copilot_mod;

// import net.minecraft.world.level.block.Block;
// import net.minecraft.world.level.block.Block;

// import net.minecraft.world.level.block.entity.BlockEntity;
// import net.minecraft.world.level.block.state.BlockBehaviour;

// public class FakeDirtBlock extends Block {
//     @Override
//     public BlockEntity createBlockEntity(BlockView world) {
//         return new FakeDirtBlockEntity();
//     }
// }

// import net.minecraft.block.BlockState;
// import net.minecraft.block.material.Material;
// import net.minecraft.item.BlockItem;
// import net.minecraft.item.Item;
// import net.minecraft.item.ItemGroup;
// import net.minecraft.util.math.BlockPos;
// import net.minecraft.world.IBlockReader;

// public class FakeDirtBlock extends Block {

//     public FakeDirtBlock() {
//         // Set basic properties for the block (you can adjust these)
//         super(Block.Properties.create(Material.EARTH).hardnessAndResistance(0.5F));

//         // Optionally, you can set additional properties, such as light level
//         this.setLightLevel((state) -> 7);
//     }
// }
