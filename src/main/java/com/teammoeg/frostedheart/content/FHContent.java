package com.teammoeg.frostedheart.content;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.block.*;
import com.teammoeg.frostedheart.block.cropblock.RyeBlock;
import com.teammoeg.frostedheart.container.CrucibleContainer;
import com.teammoeg.frostedheart.container.GeneratorContainer;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.multiblock.CrucibleMultiblock;
import com.teammoeg.frostedheart.multiblock.GeneratorMultiblock;
import com.teammoeg.frostedheart.tileentity.CrucibleTileEntity;
import com.teammoeg.frostedheart.tileentity.GeneratorTileEntity;
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

    public static void populate() {
        // Init block
        Block.Properties stoneDecoProps = Block.Properties.create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10);

        Block.Properties cropProps = AbstractBlock.Properties.create(Material.PLANTS)
                .doesNotBlockMovement()
                .tickRandomly()
                .zeroHardnessAndResistance()
                .sound(SoundType.CROP);

        FHBlocks.Multi.generator = new GeneratorMultiblockBlock("generator", FHTileTypes.GENERATOR_T1);
        FHBlocks.Multi.crucible = new CrucibleBlock("crucible", FHTileTypes.CRUCIBLE);

        FHBlocks.generator_brick = new FHBaseBlock("generator_brick", stoneDecoProps, FHBlockItem::new);
        FHBlocks.generator_core_t1 = new GeneratorCoreBlock("generator_core_t1", stoneDecoProps, FHBlockItem::new);
        FHBlocks.generator_amplifier_r1 = new FHBaseBlock("generator_amplifier_r1", stoneDecoProps, FHBlockItem::new);
        FHBlocks.burning_chamber_core = new FHBaseBlock("burning_chamber_core", stoneDecoProps, FHBlockItem::new);
        FHBlocks.burning_chamber = new FHBaseBlock("burning_chamber", stoneDecoProps, FHBlockItem::new);
        FHBlocks.rye_block = new RyeBlock("rye_block", -10, cropProps, FHBlockItem::new);
        FHBlocks.electrolyzer = new ElectrolyzerBlock("electrolyzer_block", FHBlockItem::new);


        Item.Properties properties = new Item.Properties().group(FHMain.itemGroup);
        FHItems.energy_core = new FHBaseItem("energy_core", properties);
        FHItems.rye = new FHBaseItem("rye", properties);
        FHItems.rye_bread = new FHBaseItem("rye_bread", properties.food((new Food.Builder()).hunger(5).saturation(0.6F).build()));
        FHItems.generator_ash = new FHBaseItem("generator_ash", properties);

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
        GuiHandler.register(CrucibleTileEntity.class, new ResourceLocation(FHMain.MODID, "crucible"), CrucibleContainer::new);
    }

}
