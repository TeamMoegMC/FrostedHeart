package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.common.block.*;
import com.teammoeg.frostedheart.common.block.cropblock.RyeBlock;
import com.teammoeg.frostedheart.common.container.CrucibleContainer;
import com.teammoeg.frostedheart.common.container.GeneratorContainer;
import com.teammoeg.frostedheart.common.item.FHBaseItem;
import com.teammoeg.frostedheart.common.multiblock.CrucibleMultiblock;
import com.teammoeg.frostedheart.common.multiblock.GeneratorMultiblock;
import com.teammoeg.frostedheart.common.tile.CrucibleTile;
import com.teammoeg.frostedheart.common.tile.GeneratorTileEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Food;
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
        public static Block crucible;
    }

    public static class Blocks {
        public static Block generator_brick;
        public static Block generator_core_t1;
        public static Block generator_amplifier_r1;
        public static Block rye_block;
        public static Block electrolyzer;
        public static Block burning_chamber_core;
        public static Block burning_chamber;
    }

    public static class Items {
        public static Item energy_core;
        public static Item rye;
        public static Item rye_bread;
        public static Item generator_ash;
    }

    public static class Fluids {

    }

    public static void populate() {
        // Init block
        FHContent.Multiblocks.generator = new GeneratorMultiblockBlock("generator", FHTileTypes.GENERATOR_T1);
        FHContent.Multiblocks.crucible = new CrucibleBlock("crucible", FHTileTypes.CRUCIBLE);
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
        FHContent.Blocks.burning_chamber_core = new FHBaseBlock("burning_chamber_core", stoneDecoProps, FHBlockItem::new);
        FHContent.Blocks.burning_chamber = new FHBaseBlock("burning_chamber", stoneDecoProps, FHBlockItem::new);
        FHContent.Blocks.rye_block = new RyeBlock("rye_block", -10, CropProps, FHBlockItem::new);
        FHContent.Blocks.electrolyzer = new ElectrolyzerBlock("electrolyzer_block", FHBlockItem::new);


        Item.Properties properties = new Item.Properties().group(FHMain.itemGroup);
        FHContent.Items.energy_core = new FHBaseItem("energy_core", properties);
        FHContent.Items.rye = new FHBaseItem("rye", properties);
        FHContent.Items.rye_bread = new FHBaseItem("rye_bread", properties.food((new Food.Builder()).hunger(5).saturation(0.6F).build()));
        Items.generator_ash = new FHBaseItem("generator_ash", properties);

        // Init multiblocks
        FHMultiblocks.GENERATOR = new GeneratorMultiblock();
        FHMultiblocks.CRUCIBLE = new CrucibleMultiblock();
    }

    public static void registerAll() {
        // Register multiblocks
        MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR);
        MultiblockHandler.registerMultiblock(FHMultiblocks.CRUCIBLE);
        // Register containers
        GuiHandler.register(GeneratorTileEntity.class, new ResourceLocation(FHMain.MODID, "generator"), GeneratorContainer::new);
        GuiHandler.register(CrucibleTile.class, new ResourceLocation(FHMain.MODID, "crucible"), CrucibleContainer::new);
    }

}
