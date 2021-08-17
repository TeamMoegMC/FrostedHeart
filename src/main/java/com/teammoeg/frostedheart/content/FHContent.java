/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.block.*;
import com.teammoeg.frostedheart.block.cropblock.RyeBlock;
import com.teammoeg.frostedheart.container.CrucibleContainer;
import com.teammoeg.frostedheart.container.GeneratorContainer;
import com.teammoeg.frostedheart.item.FHArmorMaterial;
import com.teammoeg.frostedheart.item.FHBaseArmorItem;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.item.FHSoupItem;
import com.teammoeg.frostedheart.multiblock.CrucibleMultiblock;
import com.teammoeg.frostedheart.multiblock.GeneratorMultiblock;
import com.teammoeg.frostedheart.tileentity.CrucibleTileEntity;
import com.teammoeg.frostedheart.tileentity.GeneratorTileEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.EquipmentSlotType;
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
        // Init Blocks
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

        // Init Items
        Item.Properties foodProps = new Item.Properties().group(FHMain.itemGroup);
        Item.Properties itemProps = new Item.Properties().group(FHMain.itemGroup);
        FHItems.energy_core = new FHBaseItem("energy_core", itemProps);
        FHItems.rye = new FHBaseItem("rye", itemProps);
        FHItems.generator_ash = new FHBaseItem("generator_ash", itemProps);

        FHItems.rye_bread = new FHBaseItem("rye_bread", new Item.Properties().group(FHMain.itemGroup).food(FHFoods.RYE_BREAD));
        FHItems.black_bread = new FHBaseItem("black_bread", properties.food(FHFoods.BLACK_BREAD));
        FHItems.vegetable_sawdust_soup = new FHSoupItem("vegetable_sawdust_soup", new Item.Properties().group(FHMain.itemGroup).maxStackSize(1).food(FHFoods.VEGETABLE_SAWDUST_SOUP), true);
        FHItems.rye_sawdust_porridge = new FHSoupItem("rye_sawdust_porridge", new Item.Properties().group(FHMain.itemGroup).maxStackSize(1).food(FHFoods.RYE_SAWDUST_PORRIDGE), true);
        FHItems.rye_porridge = new FHSoupItem("rye_porridge", new Item.Properties().group(FHMain.itemGroup).maxStackSize(1).food(FHFoods.RYE_SAWDUST_PORRIDGE), false);
        FHItems.vegetable_soup = new FHSoupItem("vegetable_soup", new Item.Properties().group(FHMain.itemGroup).maxStackSize(1).food(FHFoods.RYE_SAWDUST_PORRIDGE), false);

        FHItems.raw_hide = new FHBaseItem("raw_hide", itemProps);
        FHItems.hay_boots = new FHBaseArmorItem("hay_boots", FHArmorMaterial.HAY, EquipmentSlotType.FEET, itemProps);
        FHItems.hay_hat = new FHBaseArmorItem("hay_hat", FHArmorMaterial.HAY, EquipmentSlotType.HEAD, itemProps);
        FHItems.hay_jacket = new FHBaseArmorItem("hay_jacket", FHArmorMaterial.HAY, EquipmentSlotType.CHEST, itemProps);
        FHItems.hay_pants = new FHBaseArmorItem("hay_pants", FHArmorMaterial.HAY, EquipmentSlotType.LEGS, itemProps);

        FHItems.wool_boots = new FHBaseArmorItem("wool_boots", FHArmorMaterial.WOOL, EquipmentSlotType.FEET, itemProps);
        FHItems.wool_hat = new FHBaseArmorItem("wool_hat", FHArmorMaterial.WOOL, EquipmentSlotType.HEAD, itemProps);
        FHItems.wool_jacket = new FHBaseArmorItem("wool_jacket", FHArmorMaterial.WOOL, EquipmentSlotType.CHEST, itemProps);
        FHItems.wool_pants = new FHBaseArmorItem("wool_pants", FHArmorMaterial.WOOL, EquipmentSlotType.LEGS, itemProps);

        FHItems.hide_boots = new FHBaseArmorItem("hide_boots", FHArmorMaterial.HIDE, EquipmentSlotType.FEET, itemProps);
        FHItems.hide_hat = new FHBaseArmorItem("hide_hat", FHArmorMaterial.HIDE, EquipmentSlotType.HEAD, itemProps);
        FHItems.hide_jacket = new FHBaseArmorItem("hide_jacket", FHArmorMaterial.HIDE, EquipmentSlotType.CHEST, itemProps);
        FHItems.hide_pants = new FHBaseArmorItem("hide_pants", FHArmorMaterial.HIDE, EquipmentSlotType.LEGS, itemProps);

        // Init Multiblocks
        FHMultiblocks.GENERATOR = new GeneratorMultiblock();
        FHMultiblocks.CRUCIBLE = new CrucibleMultiblock();
    }

    public static void registerAll() {
        // Register multiblocks
        MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR);
        MultiblockHandler.registerMultiblock(FHMultiblocks.CRUCIBLE);
        // Register IE containers
        GuiHandler.register(GeneratorTileEntity.class, new ResourceLocation(FHMain.MODID, "generator"), GeneratorContainer::new);
        GuiHandler.register(CrucibleTileEntity.class, new ResourceLocation(FHMain.MODID, "crucible"), CrucibleContainer::new);
    }

}
