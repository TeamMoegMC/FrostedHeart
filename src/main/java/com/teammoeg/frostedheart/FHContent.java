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

package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.block.*;
import com.teammoeg.frostedheart.block.cropblock.RyeBlock;
import com.teammoeg.frostedheart.block.cropblock.WhiteTurnipBlock;
import com.teammoeg.frostedheart.container.CrucibleContainer;
import com.teammoeg.frostedheart.container.GeneratorContainer;
import com.teammoeg.frostedheart.container.RadiatorContainer;
import com.teammoeg.frostedheart.item.*;
import com.teammoeg.frostedheart.multiblock.CrucibleMultiblock;
import com.teammoeg.frostedheart.multiblock.GeneratorMultiblock;
import com.teammoeg.frostedheart.multiblock.GeneratorMultiblockT2;
import com.teammoeg.frostedheart.multiblock.SteamTurbineMultiblock;
import com.teammoeg.frostedheart.recipe.*;
import com.teammoeg.frostedheart.steamenergy.*;
import com.teammoeg.frostedheart.tileentity.CrucibleTileEntity;
import com.teammoeg.frostedheart.tileentity.SteamTurbineTileEntity;
import com.teammoeg.frostedheart.tileentity.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.tileentity.T2GeneratorTileEntity;
import com.teammoeg.frostedheart.util.FHUtils;
import net.minecraft.block.AbstractBlock.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.teammoeg.frostedheart.util.FHProps.*;

public class FHContent {

    public static List<Block> registeredFHBlocks = new ArrayList<>();
    public static List<Item> registeredFHItems = new ArrayList<>();
    public static List<Fluid> registeredFHFluids = new ArrayList<>();

    public static class FHBlocks {
        public static void init() {
        }

        public static Block generator_brick = new FHBaseBlock("generator_brick", stoneDecoProps, FHBlockItem::new);
        public static Block generator_core_t1 = new GeneratorCoreBlock("generator_core_t1", stoneDecoProps, FHBlockItem::new);
        public static Block generator_amplifier_r1 = new FHBaseBlock("generator_amplifier_r1", stoneDecoProps, FHBlockItem::new);
        public static Block burning_chamber_core = new FHBaseBlock("burning_chamber_core", stoneDecoProps, FHBlockItem::new);
        public static Block burning_chamber = new FHBaseBlock("burning_chamber", stoneDecoProps, FHBlockItem::new);
        public static Block rye_block = new RyeBlock("rye_block", -10, cropProps, FHBlockItem::new);
        public static Block white_turnip_block = new WhiteTurnipBlock("white_turnip_block", -10, cropProps, FHBlockItem::new);
        public static Block heat_pipe = new HeatPipeBlock("heat_pipe", stoneDecoProps, FHBlockItem::new);
        public static Block debug_heater = new DebugHeaterBlock("debug_heater", Block.Properties
                .create(Material.ROCK).sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10)
                .notSolid()
                .setLightLevel(s -> 15), FHBlockItem::new);
        public static Block charger = new ChargerBlock("charger", Block.Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10)
                .notSolid(), FHBlockItem::new);
        public static Block radiator = new RadiatorBlock("heat_radiator", Properties
                .create(Material.ROCK)
                .hardnessAndResistance(2.0F, 20.0F)
                .notSolid()
                .setLightLevel(FHUtils.getLightValueLit(15)), FHBlockItem::new);
    }

    public static class FHItems {
        public static void init() {
        }

        public static Item energy_core = new FHBaseItem("energy_core", itemProps);
        public static Item rye = new FHBaseItem("rye", itemProps);
        public static Item generator_ash = new FHBaseItem("generator_ash", itemProps);
        public static Item rye_flour = new FHBaseItem("rye_flour", itemProps);
        public static Item raw_rye_bread = new FHBaseItem("raw_rye_bread", itemProps);
        public static Item rye_bread = new FHBaseItem("rye_bread", new Item.Properties().group(FHMain.itemGroup).food(FHFoods.RYE_BREAD));
        public static Item black_bread = new FHBaseItem("black_bread", new Item.Properties().group(FHMain.itemGroup).food(FHFoods.BLACK_BREAD));
        public static Item vegetable_sawdust_soup = new FHSoupItem("vegetable_sawdust_soup", new Item.Properties().maxStackSize(1).group(FHMain.itemGroup).food(FHFoods.VEGETABLE_SAWDUST_SOUP), true);
        public static Item rye_sawdust_porridge = new FHSoupItem("rye_sawdust_porridge", new Item.Properties().maxStackSize(1).group(FHMain.itemGroup).food(FHFoods.RYE_SAWDUST_PORRIDGE), true);
        public static Item rye_porridge = new FHSoupItem("rye_porridge", new Item.Properties().maxStackSize(1).group(FHMain.itemGroup).food(FHFoods.RYE_SAWDUST_PORRIDGE), false);
        public static Item vegetable_soup = new FHSoupItem("vegetable_soup", new Item.Properties().maxStackSize(1).group(FHMain.itemGroup).food(FHFoods.VEGETABLE_SAWDUST_SOUP), false);
        public static Item steam_bottle = new SteamBottleItem("steam_bottle", new Item.Properties().group(FHMain.itemGroup).maxStackSize(1).containerItem(Items.GLASS_BOTTLE));
        public static Item raw_hide = new FHBaseItem("raw_hide", itemProps);
        public static Item hay_boots = new FHBaseArmorItem("hay_boots", FHArmorMaterial.HAY, EquipmentSlotType.FEET, itemProps);
        public static Item hay_hat = new FHBaseArmorItem("hay_hat", FHArmorMaterial.HAY, EquipmentSlotType.HEAD, itemProps);
        public static Item hay_jacket = new FHBaseArmorItem("hay_jacket", FHArmorMaterial.HAY, EquipmentSlotType.CHEST, itemProps);
        public static Item hay_pants = new FHBaseArmorItem("hay_pants", FHArmorMaterial.HAY, EquipmentSlotType.LEGS, itemProps);
        public static Item wool_boots = new FHBaseArmorItem("wool_boots", FHArmorMaterial.WOOL, EquipmentSlotType.FEET, itemProps);
        public static Item wool_hat = new FHBaseArmorItem("wool_hat", FHArmorMaterial.WOOL, EquipmentSlotType.HEAD, itemProps);
        public static Item wool_jacket = new FHBaseArmorItem("wool_jacket", FHArmorMaterial.WOOL, EquipmentSlotType.CHEST, itemProps);
        public static Item wool_pants = new FHBaseArmorItem("wool_pants", FHArmorMaterial.WOOL, EquipmentSlotType.LEGS, itemProps);
        public static Item hide_boots = new FHBaseArmorItem("hide_boots", FHArmorMaterial.HIDE, EquipmentSlotType.FEET, itemProps);
        public static Item hide_hat = new FHBaseArmorItem("hide_hat", FHArmorMaterial.HIDE, EquipmentSlotType.HEAD, itemProps);
        public static Item hide_jacket = new FHBaseArmorItem("hide_jacket", FHArmorMaterial.HIDE, EquipmentSlotType.CHEST, itemProps);
        public static Item hide_pants = new FHBaseArmorItem("hide_pants", FHArmorMaterial.HIDE, EquipmentSlotType.LEGS, itemProps);
        public static Item heater_vest = new HeaterVestItem("heater_vest", itemProps);
    }

    public static class FHMultiblocks {
        public static IETemplateMultiblock GENERATOR = new GeneratorMultiblock();
        public static IETemplateMultiblock CRUCIBLE = new GeneratorMultiblockT2();
        public static IETemplateMultiblock STEAMTURBINE = new SteamTurbineMultiblock();
        public static IETemplateMultiblock GENERATOR_T2 = new CrucibleMultiblock();
        public static Block generator = new GeneratorMultiblockBlock("generator", FHTileTypes.GENERATOR_T1);
        public static Block generator_t2 = new HeatedGeneratorMultiBlock("generator_t2", FHTileTypes.GENERATOR_T2);
        public static Block crucible = new CrucibleBlock("crucible", FHTileTypes.CRUCIBLE);
        public static Block steam_turbine = new SteamTurbineBlock("steam_turbine", FHTileTypes.STEAMTURBINE);

        public static void init() {
            MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR);
            MultiblockHandler.registerMultiblock(FHMultiblocks.CRUCIBLE);
            MultiblockHandler.registerMultiblock(FHMultiblocks.STEAMTURBINE);
            MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR_T2);
        }
    }

    public static class FHTileTypes {
        public static final DeferredRegister<TileEntityType<?>> REGISTER = DeferredRegister.create(
                ForgeRegistries.TILE_ENTITIES, FHMain.MODID);

        public static final RegistryObject<TileEntityType<T1GeneratorTileEntity>> GENERATOR_T1 = REGISTER.register(
                "generator", makeType(() -> new T1GeneratorTileEntity(1, 2, 1), () -> FHMultiblocks.generator)
        );
        public static final RegistryObject<TileEntityType<CrucibleTileEntity>> CRUCIBLE = REGISTER.register(
                "crucible", makeType(() -> new CrucibleTileEntity(), () -> FHMultiblocks.crucible)
        );
        public static final RegistryObject<TileEntityType<SteamTurbineTileEntity>> STEAMTURBINE = REGISTER.register(
                "steam_turbine", makeType(() -> new SteamTurbineTileEntity(), () -> FHMultiblocks.steam_turbine)
        );

        public static final RegistryObject<TileEntityType<HeatPipeTileEntity>> HEATPIPE = REGISTER.register(
                "heat_pipe", makeType(() -> new HeatPipeTileEntity(), () -> FHBlocks.heat_pipe)
        );
        public static final RegistryObject<TileEntityType<DebugHeaterTileEntity>> DEBUGHEATER = REGISTER.register(
                "debug_heater", makeType(() -> new DebugHeaterTileEntity(), () -> FHBlocks.debug_heater)
        );
        public static final RegistryObject<TileEntityType<ChargerTileEntity>> CHARGER = REGISTER.register(
                "charger", makeType(() -> new ChargerTileEntity(), () -> FHBlocks.charger)
        );

        public static final RegistryObject<TileEntityType<RadiatorTileEntity>> RADIATOR = REGISTER.register(
                "heat_radiator", makeType(() -> new RadiatorTileEntity(), () -> FHBlocks.radiator));

        public static final RegistryObject<TileEntityType<T2GeneratorTileEntity>> GENERATOR_T2 = REGISTER.register(
                "generator_t2", makeType(() -> new T2GeneratorTileEntity(2, 4, 2), () -> FHMultiblocks.generator_t2)
        );

        private static <T extends TileEntity> Supplier<TileEntityType<T>> makeType(Supplier<T> create, Supplier<Block> valid) {
            return makeTypeMultipleBlocks(create, () -> ImmutableSet.of(valid.get()));
        }

        private static <T extends TileEntity> Supplier<TileEntityType<T>> makeTypeMultipleBlocks(Supplier<T> create, Supplier<Collection<Block>> valid) {
            return () -> new TileEntityType<>(create, ImmutableSet.copyOf(valid.get()), null);
        }

    }

    public static class FHRecipes {
        public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
                ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID
        );

        static {
            GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("generator", GeneratorRecipeSerializer::new);
            CrucibleRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("crucible", CrucibleRecipeSerializer::new);
            ChargerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("charger", ChargerRecipeSerializer::new);
        }

        public static void registerRecipeTypes() {
            GeneratorRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":generator");
            CrucibleRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":crucible");
            ChargerRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":charger");
        }
    }

    public static void registerContainers() {
        GuiHandler.register(T1GeneratorTileEntity.class, new ResourceLocation(FHMain.MODID, "generator"), GeneratorContainer::new);
        GuiHandler.register(CrucibleTileEntity.class, new ResourceLocation(FHMain.MODID, "crucible"), CrucibleContainer::new);
        GuiHandler.register(RadiatorTileEntity.class, new ResourceLocation(FHMain.MODID, "radiator"), RadiatorContainer::new);
    }
}
