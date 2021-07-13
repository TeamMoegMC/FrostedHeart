package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.client.ClientProxy;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.client.screen.GeneratorScreen;
import com.teammoeg.frostedheart.common.block.FHBaseBlock;
import com.teammoeg.frostedheart.common.block.FHBlockItem;
import com.teammoeg.frostedheart.common.block.GeneratorCoreBlock;
import com.teammoeg.frostedheart.common.block.GeneratorMultiblockBlock;
import com.teammoeg.frostedheart.common.block.cropblock.LeekBlock;
import com.teammoeg.frostedheart.common.block.cropblock.RyeBlock;
import com.teammoeg.frostedheart.common.container.GeneratorContainer;
import com.teammoeg.frostedheart.common.multiblock.GeneratorMultiblock;
import com.teammoeg.frostedheart.common.tile.GeneratorTileEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ToolType;

import java.util.ArrayList;
import java.util.List;

public class FHContent {

    public static List<Block> registeredFHBlocks = new ArrayList<>();
    public static List<Item> registeredFHItems = new ArrayList<>();
    public static List<Fluid> registeredFHFluids = new ArrayList<>();

    public static class Multiblocks {

        public static Block generator;
    }

    public static class Blocks {
        public static Block generator_brick;
        public static Block generator_core_t1;
        public static Block generator_amplifier_r1;
        public static Block leek_block;
        public static Block rye_block;
    }

    public static class Items {

    }

    public static class Fluids {

    }

    public static void populate() {
        // Init block
        FHContent.Multiblocks.generator = new GeneratorMultiblockBlock("generator", FHTileTypes.GENERATOR_T1);

        Block.Properties stoneDecoProps = Block.Properties.create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10);
        Block.Properties CropProps =
                AbstractBlock.Properties.create(Material.PLANTS).doesNotBlockMovement().tickRandomly().zeroHardnessAndResistance().sound(SoundType.CROP);

        FHContent.Blocks.generator_brick = new FHBaseBlock("generator_brick", stoneDecoProps, FHBlockItem::new);
        FHContent.Blocks.generator_core_t1 = new GeneratorCoreBlock("generator_core_t1", stoneDecoProps, FHBlockItem::new);
        FHContent.Blocks.generator_amplifier_r1 = new FHBaseBlock("generator_amplifier_r1", stoneDecoProps, FHBlockItem::new);
        FHContent.Blocks.leek_block = new LeekBlock("leek_block", CropProps, FHBlockItem::new);
        FHContent.Blocks.rye_block = new RyeBlock("rye_block", CropProps, FHBlockItem::new);

        // Init multiblocks
        FHMultiblocks.GENERATOR = new GeneratorMultiblock();
    }

    public static void registerAll() {
        // Register multiblocks
        MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR);
        // Register containers
        GuiHandler.register(GeneratorTileEntity.class, new ResourceLocation(FHMain.MODID, "generator"), GeneratorContainer::new);
        // Register screens
        ((ClientProxy) ImmersiveEngineering.proxy).registerScreen(new ResourceLocation(FHMain.MODID, "generator"), GeneratorScreen::new);

    }

}
