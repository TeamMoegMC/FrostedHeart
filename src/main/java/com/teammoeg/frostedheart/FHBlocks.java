/*
 * Copyright (c) 2022-2024 TeamMoeg
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
 *
 */

package com.teammoeg.frostedheart;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.ModelGen;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import com.teammoeg.frostedheart.base.item.FoodBlockItem;
import com.teammoeg.frostedheart.content.agriculture.RyeBlock;
import com.teammoeg.frostedheart.content.agriculture.WhiteTurnipBlock;
import com.teammoeg.frostedheart.content.agriculture.WolfBerryBushBlock;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.decoration.*;
import com.teammoeg.frostedheart.content.incubator.HeatIncubatorBlock;
import com.teammoeg.frostedheart.content.incubator.IncubatorBlock;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskBlock;
import com.teammoeg.frostedheart.content.research.blocks.MechCalcBlock;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeBlock;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerBlock;
import com.teammoeg.frostedheart.content.steamenergy.debug.DebugHeaterBlock;
import com.teammoeg.frostedheart.content.steamenergy.fountain.FountainBlock;
import com.teammoeg.frostedheart.content.steamenergy.fountain.FountainNozzleBlock;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaBlock;
import com.teammoeg.frostedheart.content.steamenergy.steamcore.SteamCoreBlock;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.content.town.house.HouseBlock;
import com.teammoeg.frostedheart.content.town.hunting.HuntingBaseBlock;
import com.teammoeg.frostedheart.content.town.hunting.HuntingCampBlock;
import com.teammoeg.frostedheart.content.town.mine.MineBaseBlock;
import com.teammoeg.frostedheart.content.town.mine.MineBlock;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseBlock;
import com.teammoeg.frostedheart.content.utility.incinerator.GasVentBlock;
import com.teammoeg.frostedheart.content.utility.incinerator.OilBurnerBlock;
import com.teammoeg.frostedheart.foundation.loot.FHLootGen;
import com.teammoeg.frostedheart.infrastructure.gen.FHBlockStateGen;
import com.teammoeg.frostedheart.util.constants.FHProps;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.teammoeg.frostedheart.FHMain.REGISTRATE;
import static com.teammoeg.frostedheart.infrastructure.gen.FHTagGen.*;
import static net.minecraft.world.level.block.Blocks.*;

public class FHBlocks {

    static {
        REGISTRATE.setCreativeTab(FHTabs.BASE_TAB);
    }

    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FHMain.MODID);

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, String itemName, Function<T, Item> item) {
        RegistryObject<T> blk = BLOCKS.register(name, block);
        FHItems.ITEMS.register(itemName, () -> item.apply(blk.get()));
        return blk;
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
        return register(name, block, name, FHBlockItem::new);
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, Function<T, Item> item) {
        return register(name, block, name, item);
    }

    // Registrate style

    // Condensed ore blocks
    public static final BlockEntry<Block> CONDENSED_IRON_ORE_BLOCK = REGISTRATE.block("condensed_iron_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_iron_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_IRON_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    // next
    public static final BlockEntry<Block> CONDENSED_COPPER_ORE_BLOCK = REGISTRATE.block("condensed_copper_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_copper_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_COPPER_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CONDENSED_GOLD_ORE_BLOCK = REGISTRATE.block("condensed_gold_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_gold_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_GOLD_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CONDENSED_ZINC_ORE_BLOCK = REGISTRATE.block("condensed_zinc_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_zinc_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_ZINC_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CONDENSED_SILVER_ORE_BLOCK = REGISTRATE.block("condensed_silver_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_silver_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_SILVER_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CONDENSED_TIN_ORE_BLOCK = REGISTRATE.block("condensed_tin_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_tin_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_TIN_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CONDENSED_PYRITE_ORE_BLOCK = REGISTRATE.block("condensed_pyrite_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_pyrite_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_PYRITE_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CONDENSED_NICKEL_ORE_BLOCK = REGISTRATE.block("condensed_nickel_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_nickel_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_NICKEL_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CONDENSED_LEAD_ORE_BLOCK = REGISTRATE.block("condensed_lead_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_lead_ore"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.CONDENSED_BALL_LEAD_ORE.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();

    // sludge blocks
    public static final BlockEntry<Block> IRON_SLUDGE_BLOCK = REGISTRATE.block("iron_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/iron_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.IRON_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> COPPER_SLUDGE_BLOCK = REGISTRATE.block("copper_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/copper_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.COPPER_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> GOLD_SLUDGE_BLOCK = REGISTRATE.block("gold_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/gold_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.GOLD_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> ZINC_SLUDGE_BLOCK = REGISTRATE.block("zinc_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/zinc_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.ZINC_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    // next
    public static final BlockEntry<Block> SILVER_SLUDGE_BLOCK = REGISTRATE.block("silver_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/silver_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.SILVER_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> TIN_SLUDGE_BLOCK = REGISTRATE.block("tin_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/tin_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.TIN_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> PYRITE_SLUDGE_BLOCK = REGISTRATE.block("pyrite_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/pyrite_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.PYRITE_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> NICKEL_SLUDGE_BLOCK = REGISTRATE.block("nickel_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/nickel_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.NICKEL_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();
    public static final BlockEntry<Block> LEAD_SLUDGE_BLOCK = REGISTRATE.block("lead_sludge_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/lead_sludge"))
            .loot((lt, block) -> lt.add(block, lt.createSingleItemTableWithSilkTouch(block, FHItems.LEAD_SLURRY.get(), ConstantValue.exactly(4))))
            .simpleItem()
            .register();

    // Condensed ores
    public static final BlockEntry<SnowLayerBlock> CONDENSED_IRON_ORE = REGISTRATE.block("condensed_iron_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_IRON_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_IRON_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_iron_ore", "condensed_iron_ore_block", "ore/condensed_iron_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_iron_ore", "ore/condensed_iron_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_COPPER_ORE = REGISTRATE.block("condensed_copper_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_COPPER_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_COPPER_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_copper_ore", "condensed_copper_ore_block", "ore/condensed_copper_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_copper_ore", "ore/condensed_copper_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_GOLD_ORE = REGISTRATE.block("condensed_gold_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_GOLD_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_GOLD_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_gold_ore", "condensed_gold_ore_block", "ore/condensed_gold_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_gold_ore", "ore/condensed_gold_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_ZINC_ORE = REGISTRATE.block("condensed_zinc_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_ZINC_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_ZINC_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_zinc_ore", "condensed_zinc_ore_block", "ore/condensed_zinc_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_zinc_ore", "ore/condensed_zinc_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_SILVER_ORE = REGISTRATE.block("condensed_silver_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_SILVER_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_SILVER_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_silver_ore", "condensed_silver_ore_block", "ore/condensed_silver_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_silver_ore", "ore/condensed_silver_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_TIN_ORE = REGISTRATE.block("condensed_tin_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_TIN_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_TIN_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_tin_ore", "condensed_tin_ore_block", "ore/condensed_tin_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_tin_ore", "ore/condensed_tin_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_PYRITE_ORE = REGISTRATE.block("condensed_pyrite_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_PYRITE_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_PYRITE_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_pyrite_ore", "condensed_pyrite_ore_block", "ore/condensed_pyrite_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_pyrite_ore", "ore/condensed_pyrite_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_NICKEL_ORE = REGISTRATE.block("condensed_nickel_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_NICKEL_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_NICKEL_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_nickel_ore", "condensed_nickel_ore_block", "ore/condensed_nickel_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_nickel_ore", "ore/condensed_nickel_ore"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> CONDENSED_LEAD_ORE = REGISTRATE.block("condensed_lead_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_LEAD_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_LEAD_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_lead_ore", "condensed_lead_ore_block", "ore/condensed_lead_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_lead_ore", "ore/condensed_lead_ore"))
            .build()
            .register();

    // Sludge
    public static final BlockEntry<SnowLayerBlock> IRON_SLUDGE = REGISTRATE.block("iron_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, IRON_SLUDGE_BLOCK.get(), FHItems.IRON_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("iron_sludge", "iron_sludge_block", "ore/iron_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("iron_sludge", "ore/iron_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> COPPER_SLUDGE = REGISTRATE.block("copper_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, COPPER_SLUDGE_BLOCK.get(), FHItems.COPPER_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("copper_sludge", "copper_sludge_block", "ore/copper_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("copper_sludge", "ore/copper_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> GOLD_SLUDGE = REGISTRATE.block("gold_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, GOLD_SLUDGE_BLOCK.get(), FHItems.GOLD_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("gold_sludge", "gold_sludge_block", "ore/gold_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("gold_sludge", "ore/gold_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> ZINC_SLUDGE = REGISTRATE.block("zinc_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, ZINC_SLUDGE_BLOCK.get(), FHItems.ZINC_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("zinc_sludge", "zinc_sludge_block", "ore/zinc_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("zinc_sludge", "ore/zinc_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> SILVER_SLUDGE = REGISTRATE.block("silver_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, SILVER_SLUDGE_BLOCK.get(), FHItems.SILVER_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("silver_sludge", "silver_sludge_block", "ore/silver_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("silver_sludge", "ore/silver_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> TIN_SLUDGE = REGISTRATE.block("tin_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, TIN_SLUDGE_BLOCK.get(), FHItems.TIN_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("tin_sludge", "tin_sludge_block", "ore/tin_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("tin_sludge", "ore/tin_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> PYRITE_SLUDGE = REGISTRATE.block("pyrite_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, PYRITE_SLUDGE_BLOCK.get(), FHItems.PYRITE_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("pyrite_sludge", "pyrite_sludge_block", "ore/pyrite_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("pyrite_sludge", "ore/pyrite_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> NICKEL_SLUDGE = REGISTRATE.block("nickel_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, NICKEL_SLUDGE_BLOCK.get(), FHItems.NICKEL_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("nickel_sludge", "nickel_sludge_block", "ore/nickel_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("nickel_sludge", "ore/nickel_sludge"))
            .build()
            .register();
    public static final BlockEntry<SnowLayerBlock> LEAD_SLUDGE = REGISTRATE.block("lead_sludge", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.SLUDGE.tag)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, LEAD_SLUDGE_BLOCK.get(), FHItems.LEAD_SLURRY.get())))
            .blockstate(FHBlockStateGen.snowLayered("lead_sludge", "lead_sludge_block", "ore/lead_sludge"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("lead_sludge", "ore/lead_sludge"))
            .build()
            .register();

    // Stone ores
    public static final BlockEntry<DropExperienceBlock> SILVER_ORE = REGISTRATE.block("silver_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.GOLD_ORE)
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.ORES)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/silver_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_SILVER.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .transform(tagBlockAndItem("ores/silver", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> TIN_ORE = REGISTRATE.block("tin_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/tin_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_TIN.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/tin", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> PYRITE_ORE = REGISTRATE.block("pyrite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.IRON_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/pyrite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_PYRITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/pyrite", "ores/iron", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> NICKEL_ORE = REGISTRATE.block("nickel_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.IRON_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/nickel_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_NICKEL.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/nickel", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> LEAD_ORE = REGISTRATE.block("lead_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/lead_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_LEAD.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/lead", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> HALITE_ORE = REGISTRATE.block("halite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/halite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_HALITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/halite", "ores/salt", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> SYLVITE_ORE = REGISTRATE.block("sylvite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/sylvite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_SYLVITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/sylvite", "ores/potash", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> MAGNESITE_ORE = REGISTRATE.block("magnesite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/magnesite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_MAGNESITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/magnesite", "ores/magnesium", "ores_in_ground/stone"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    // Deepslate ores
    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_SILVER_ORE = REGISTRATE.block("deepslate_silver_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_GOLD_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_silver_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_SILVER.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/silver", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_TIN_ORE = REGISTRATE.block("deepslate_tin_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_tin_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_TIN.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/tin", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_PYRITE_ORE = REGISTRATE.block("deepslate_pyrite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_pyrite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_PYRITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/pyrite", "ores/iron", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_NICKEL_ORE = REGISTRATE.block("deepslate_nickel_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_nickel_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_NICKEL.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/nickel", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_LEAD_ORE = REGISTRATE.block("deepslate_lead_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_lead_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_LEAD.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/lead", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_HALITE_ORE = REGISTRATE.block("deepslate_halite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_halite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_HALITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/halite", "ores/salt", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_SYLVITE_ORE = REGISTRATE.block("deepslate_sylvite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_sylvite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_SYLVITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/sylvite", "ores/potash", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    public static final BlockEntry<DropExperienceBlock> DEEPSLATE_MAGNESITE_ORE = REGISTRATE.block("deepslate_magnesite_ore", DropExperienceBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE_COPPER_ORE)
            .properties(p -> p.mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/deepslate_magnesite_ore"))
            .loot((lt, b) -> lt.add(b,
                    RegistrateBlockLootTables.createSilkTouchDispatchTable(b,
                            lt.applyExplosionDecay(b, LootItem.lootTableItem(FHItems.RAW_MAGNESITE.get())
                                    .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))))))
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(Tags.Blocks.ORES)
            .transform(tagBlockAndItem("ores/magnesite", "ores/magnesium", "ores_in_ground/deepslate"))
            .tag(Tags.Items.ORES)
            .build()
            .register();

    // Additional soil, registrate
    public static final BlockEntry<Block> PEAT = REGISTRATE.block("peat_block", Block::new)
            .initialProperties(() -> MUD)
            .properties(p -> p.mapColor(MapColor.COLOR_BLACK))
            .blockstate(FHBlockStateGen.simpleCubeAll("sediment/peat_block"))
            .loot((lt, b) -> lt.add(b,
                    lt.createSingleItemTableWithSilkTouch(b, FHItems.PEAT.get(), ConstantValue.exactly(4))))
            .tag(FHTags.Blocks.SOIL.tag)
            .transform(tagBlockAndItem("peat"))
            .build()
            .register();

    public static final BlockEntry<Block> ROTTEN_WOOD = REGISTRATE.block("rotten_wood_block", Block::new)
            .initialProperties(() -> MUD)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN))
            .blockstate(FHBlockStateGen.simpleCubeAll(new ResourceLocation("block/oak_log")))
            .loot((lt, b) -> lt.add(b,
                    lt.createSingleItemTableWithSilkTouch(b, FHItems.ROTTEN_WOOD.get(), ConstantValue.exactly(4))))
            .tag(FHTags.Blocks.SOIL.tag)
            .transform(tagBlockAndItem("rotten_wood"))
            .build()
            .register();

    public static final BlockEntry<Block> BAUXITE = REGISTRATE.block("bauxite_block", Block::new)
            .initialProperties(() -> GRAVEL)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE))
            .blockstate(FHBlockStateGen.simpleCubeAll("sediment/bauxite_block"))
            .loot((lt, b) -> lt.add(b,
                    lt.createSingleItemTableWithSilkTouch(b, FHItems.BAUXITE.get(), ConstantValue.exactly(4))))
            .tag(FHTags.Blocks.SOIL.tag)
            .transform(tagBlockAndItem("bauxite"))
            .build()
            .register();

    public static final BlockEntry<Block> KAOLIN = REGISTRATE.block("kaolin_block", Block::new)
            .initialProperties(() -> CLAY)
            .properties(p -> p.mapColor(MapColor.SNOW))
            .blockstate(FHBlockStateGen.simpleCubeAll("sediment/kaolin_block"))
            .loot((lt, b) -> lt.add(b,
                    lt.createSingleItemTableWithSilkTouch(b, FHItems.KAOLIN.get(), ConstantValue.exactly(4))))
            .tag(FHTags.Blocks.SOIL.tag)
            .transform(tagBlockAndItem("kaolin"))
            .build()
            .register();

    public static final BlockEntry<Block> BURIED_MYCELIUM = REGISTRATE.block("buried_mycelium", Block::new)
            .initialProperties(() -> MYCELIUM)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
            .blockstate(FHBlockStateGen.simpleCubeAll("sediment/buried_mycelium"))
            .tag(FHTags.Blocks.SOIL.tag)
            .transform(tagBlockAndItem("mycelium"))
            .build()
            .register();

    public static final BlockEntry<Block> BURIED_PODZOL = REGISTRATE.block("buried_podzol", Block::new)
            .initialProperties(() -> PODZOL)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN))
            .blockstate(FHBlockStateGen.simpleCubeAll("sediment/buried_podzol"))
            .tag(FHTags.Blocks.SOIL.tag)
            .transform(tagBlockAndItem("podzol"))
            .build()
            .register();

    // Permafrost, registrate
    public static final BlockEntry<SnowyDirtBlock> DIRT_PERMAFROST = REGISTRATE.block("dirt_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> DIRT)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> MUD_PERMAFROST = REGISTRATE.block("mud_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> MUD)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> GRAVEL_PERMAFROST = REGISTRATE.block("gravel_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> GRAVEL)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> SAND_PERMAFROST = REGISTRATE.block("sand_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> SAND)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .tag(BlockTags.SAND)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> RED_SAND_PERMAFROST = REGISTRATE.block("red_sand_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> RED_SAND)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .tag(BlockTags.SAND)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> CLAY_PERMAFROST = REGISTRATE.block("clay_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> CLAY)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> PEAT_PERMAFROST = REGISTRATE.block("peat_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> MUD)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

//    public static final BlockEntry<SnowyDirtBlock> ROTTEN_WOOD_PERMAFROST = REGISTRATE.block("rotten_wood_permafrost", SnowyDirtBlock::new)
//            .initialProperties(() -> MUD)
//            .properties(p -> p.strength(2.0F))
//            .blockstate((ctx, prov) -> {
//                prov.getVariantBuilder(ctx.getEntry())
//                        .partialState().with(SnowyDirtBlock.SNOWY, false)
//                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
//                        .addModel()
//                        .partialState().with(SnowyDirtBlock.SNOWY, true)
//                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
//                        .addModel();
//            })
//            .tag(FHTags.Blocks.PERMAFROST.tag)
//            .tag(BlockTags.MUSHROOM_GROW_BLOCK)
//            .item()
//            .tag(FHTags.Items.PERMAFROST.tag)
//            .build()
//            .register();

    public static final BlockEntry<SnowyDirtBlock> BAUXITE_PERMAFROST = REGISTRATE.block("bauxite_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> GRAVEL)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> KAOLIN_PERMAFROST = REGISTRATE.block("kaolin_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> CLAY)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> MYCELIUM_PERMAFROST = REGISTRATE.block("mycelium_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> MYCELIUM)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .tag(BlockTags.MUSHROOM_GROW_BLOCK)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> PODZOL_PERMAFROST = REGISTRATE.block("podzol_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> PODZOL)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .tag(BlockTags.MUSHROOM_GROW_BLOCK)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> ROOTED_DIRT_PERMAFROST = REGISTRATE.block("rooted_dirt_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> ROOTED_DIRT)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    public static final BlockEntry<SnowyDirtBlock> COARSE_DIRT_PERMAFROST = REGISTRATE.block("coarse_dirt_permafrost", SnowyDirtBlock::new)
            .initialProperties(() -> COARSE_DIRT)
            .properties(p -> p.strength(2.0F))
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.getEntry())
                        .partialState().with(SnowyDirtBlock.SNOWY, false)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName())))
                        .addModel()
                        .partialState().with(SnowyDirtBlock.SNOWY, true)
                        .modelForState().modelFile(prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "_snow")))
                        .addModel();
            })
            .tag(FHTags.Blocks.PERMAFROST.tag)
            .item()
            .tag(FHTags.Items.PERMAFROST.tag)
            .build()
            .register();

    // Metal blocks, registrate
    // TODO: Uncomment when textures are ready
    /*
    public static final BlockEntry<Block> ALUMINUM_BLOCK = REGISTRATE.block("aluminum_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/aluminum"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Aluminum")
            .register();

    public static final BlockEntry<Block> STEEL_BLOCK = REGISTRATE.block("steel_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/steel"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Steel")
            .register();

    public static final BlockEntry<Block> ELECTRUM_BLOCK = REGISTRATE.block("electrum_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/electrum"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Electrum")
            .register();

    public static final BlockEntry<Block> CONSTANTAN_BLOCK = REGISTRATE.block("constantan_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/constantan"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Constantan")
            .register();

    public static final BlockEntry<Block> CAST_IRON_BLOCK = REGISTRATE.block("cast_iron_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/cast_iron"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Cast Iron")
            .register();

    public static final BlockEntry<Block> DURALUMIN_BLOCK = REGISTRATE.block("duralumin_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/duralumin"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Duralumin")
            .register();

    public static final BlockEntry<Block> SILVER_BLOCK = REGISTRATE.block("silver_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/silver"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Silver")
            .register();

    public static final BlockEntry<Block> NICKEL_BLOCK = REGISTRATE.block("nickel_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/nickel"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Nickel")
            .register();

    public static final BlockEntry<Block> LEAD_BLOCK = REGISTRATE.block("lead_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/lead"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Lead")
            .register();

    public static final BlockEntry<Block> TITANIUM_BLOCK = REGISTRATE.block("titanium_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/titanium"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Titanium")
            .register();

    public static final BlockEntry<Block> BRONZE_BLOCK = REGISTRATE.block("bronze_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/bronze"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Bronze")
            .register();

    public static final BlockEntry<Block> INVAR_BLOCK = REGISTRATE.block("invar_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/invar"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Invar")
            .register();

    public static final BlockEntry<Block> TUNGSTEN_STEEL_BLOCK = REGISTRATE.block("tungstensteel_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_DIAMOND_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/tungstensteel"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Tungsten Steel")
            .register();

    public static final BlockEntry<Block> TIN_BLOCK = REGISTRATE.block("tin_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/tin"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Tin")
            .register();

    public static final BlockEntry<Block> MAGNESIUM_BLOCK = REGISTRATE.block("magnesium_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/magnesium"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Magnesium")
            .register();

    public static final BlockEntry<Block> TUNGSTEN_BLOCK = REGISTRATE.block("tungsten_block", Block::new)
            .initialProperties(() -> IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_DIAMOND_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .transform(tagBlockAndItem("storage_blocks/tungsten"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .lang("Block of Tungsten")
            .register();
    */

    // Resource Blocks, registrate
    public static final BlockEntry<Block> MAGNESITE_BLOCK = REGISTRATE.block("magnesite_block", Block::new)
            .initialProperties(() -> RAW_GOLD_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_PINK).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .lang("Block of Raw Magnesite")
            .transform(tagBlockAndItem("storage_blocks/raw_magenesite"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .register();

    public static final BlockEntry<Block> MAGNESIA_BLOCK = REGISTRATE.block("magnesia_block", Block::new)
            .initialProperties(() -> RAW_GOLD_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_PINK).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .lang("Block of Raw Magnesia")
            .transform(tagBlockAndItem("storage_blocks/raw_magenesia"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .register();

    public static final BlockEntry<Block> QUICKLIME_BLOCK = REGISTRATE.block("quicklime_block", Block::new)
            .initialProperties(() -> RAW_GOLD_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_PINK).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .lang("Block of Quicklime")
            .transform(tagBlockAndItem("storage_blocks/quicklime"))
            .tag(Tags.Items.STORAGE_BLOCKS)
            .build()
            .register();
    public static RegistryObject<Block> COPPER_GRAVEL = register("copper_gravel", () -> new FHBaseBlock(FHProps.ore_gravel));

    // Crop Blocks
    public static RegistryObject<Block> RYE_BLOCK = register("rye_block", () -> new RyeBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.cropProps));
    public static RegistryObject<Block> WOLFBERRY_BUSH_BLOCK = register("wolfberry_bush_block", () -> new WolfBerryBushBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.berryBushBlocks, 2), "wolfberries", t -> new FoodBlockItem(t, FHItems.createProps(), FHFoodProperties.WOLFBERRIES));
    public static RegistryObject<Block> WHITE_TURNIP_BLOCK = register("white_turnip_block", () -> new WhiteTurnipBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.cropProps), (block) -> new FoodBlockItem(block, new Item.Properties(), FHFoodProperties.WHITE_TURNIP));

    // Building Blocks
    public static RegistryObject<Block> GENERATOR_BRICK = register("generator_brick", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> GENERATOR_CORE_T1 = register("generator_core_t1", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> GENERATOR_AMPLIFIER_T1 = register("generator_amplifier_r1", () -> new FHBaseBlock(FHProps.stoneDecoProps));

    public static final BlockEntry<Block> DURALUMIN_SHEETMETAL = REGISTRATE.block("duralumin_sheetmetal", Block::new)
            .properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2, 2))
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(tagBlockAndItem("sheetmetals"))
            .build()
            .register();

    public static final BlockEntry<Block> REFRACTORY_BRICKS = REGISTRATE.block("refractory_bricks", Block::new)
            .initialProperties(() -> BRICKS)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .simpleItem()
            .register();

    public static final BlockEntry<Block> HIGH_REFRACTORY_BRICKS = REGISTRATE.block("high_refractory_bricks", Block::new)
            .initialProperties(() -> BRICKS)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .simpleItem()
            .register();

    public static final BlockEntry<Block> PACKED_SNOW = REGISTRATE.block("packed_snow", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .properties(p -> p.strength(0.5F))
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .tag(BlockTags.SNOW)
            .tag(FHTags.Blocks.SNOW_MOVEMENT.tag)
            .simpleItem()
            .register();

    public static final BlockEntry<SlabBlock> PACKED_SNOW_SLAB = REGISTRATE.block("packed_snow_slab", SlabBlock::new)
            .initialProperties(() -> SNOW_BLOCK)
            .properties(p -> p.strength(0.5F))
            .blockstate((c, p) -> {
                p.slabBlock(c.get(), p.modLoc("block/packed_snow"), p.modLoc("block/packed_snow"));
            })
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .tag(BlockTags.SNOW)
            .tag(BlockTags.SLABS)
            .tag(FHTags.Blocks.SNOW_MOVEMENT.tag)
            .simpleItem()
            .register();

    public static final BlockEntry<HugeMushroomBlock> WHALE_BLOCK = REGISTRATE.block("whale_block", HugeMushroomBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .friction(0.8F)
                    .sound(SoundType.MUD)
            )
            .blockstate((c, p) -> {
                p.getExistingMultipartBuilder(c.get());
            })
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(BlockTags.SWORD_EFFICIENT)
            .simpleItem()
            .register();

    public static final BlockEntry<HugeMushroomBlock> WHALE_BELLY_BLOCK = REGISTRATE.block("whale_belly_block", HugeMushroomBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW)
                    .requiresCorrectToolForDrops()
                    .friction(0.8F)
                    .sound(SoundType.MUD)
            )
            .blockstate((c, p) -> {
                p.getExistingMultipartBuilder(c.get());
            })
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .tag(BlockTags.SWORD_EFFICIENT)
            .simpleItem()
            .register();

    // Decoration Blocks
    public static RegistryObject<Block> BLOOD_BLOCK = register("blood_block", () -> new bloodBlock(FHProps.stoneProps));
    public static RegistryObject<Block> BONE_BLOCK = register("bone_block", () -> new BoneBlock(FHProps.grassProps));
    public static RegistryObject<Block> SMALL_GARAGE = register("small_garage", () -> new SmallGarage(FHProps.grassProps));
    public static RegistryObject<Block> PACKAGE_BLOCK = register("package_block", () -> new PackageBlock(FHProps.woodenProps));
    public static RegistryObject<Block> PEBBLE_BLOCK = register("pebble_block", () -> new PebbleBlock(FHProps.stoneProps));
    public static RegistryObject<Block> ODD_MARK = register("odd_mark", () -> new OddMark(FHProps.redStoneProps));
    public static RegistryObject<Block> WOODEN_BOX = register("wooden_box", () -> new WoodenBox(FHProps.woodenProps));
    public static RegistryObject<Block> MAKESHIFT_GENERATOR_BROKEN = register("makeshift_generator_broken", () -> new FHBaseBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(45, 800)));

    public static RegistryObject<Block> BROKEN_PLATE = register("broken_plate", () -> new FHBaseBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(45, 800)));

    // Machine Blocks
    public static RegistryObject<Block> RELIC_CHEST = register("relic_chest", RelicChestBlock::new);
    public static RegistryObject<Block> INCUBATOR = register("incubator", () -> new IncubatorBlock(FHProps.stoneDecoProps, FHBlockEntityTypes.INCUBATOR));
    public static RegistryObject<Block> HEAT_INCUBATOR = register("heat_incubator", () -> new HeatIncubatorBlock(FHProps.metalDecoProps, FHBlockEntityTypes.INCUBATOR2));
    public static RegistryObject<Block> HEAT_PIPE = register("heat_pipe", () -> new HeatPipeBlock(Block.Properties
            .of().mapColor(MapColor.STONE).sound(SoundType.WOOD)
            .strength(1, 5)
            .noOcclusion()));
    public static RegistryObject<Block> DEBUG_HEATER = register("debug_heater", () -> new DebugHeaterBlock(Block.Properties
            .of().mapColor(MapColor.STONE).sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> CHARGER = register("charger", () -> new ChargerBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> OIL_BURNER = register("oil_burner", () -> new OilBurnerBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> GAS_VENT = register("gas_vent", () -> new GasVentBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> DRAWING_DESK = register("drawing_desk", () -> new DrawingDeskBlock(Block.Properties
            .of().mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2, 6)
            .noOcclusion()));
    public static RegistryObject<Block> SMOKE_BLOCK_T1 = register("smoke_block_t1", () -> new SmokeBlockT1(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> MECHANICAL_CALCULATOR = register("mechanical_calculator", () -> new MechCalcBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> SAUNA_VENT = register("sauna_vent", () -> new SaunaBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> FOUNTAIN_BASE = register("fountain_base", () -> new FountainBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> FOUNTAIN_NOZZLE = register("fountain_nozzle", () -> new FountainNozzleBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static final BlockEntry<SteamCoreBlock> STEAM_CORE = REGISTRATE.block("steam_core", SteamCoreBlock::new)
            .properties(t -> t.sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2, 10)
                    .noOcclusion())
            .blockstate((c, p) -> p.getExistingVariantBuilder(c.get()))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .tag(FHTags.Blocks.METAL_MACHINES.tag)
            .item()
            .transform(ModelGen.customItemModel())
            .register();

    // Town blocks, registrate
    public static final BlockEntry<HouseBlock> HOUSE = REGISTRATE.block("house", HouseBlock::new)
            .properties(t -> AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY)
            .tag(FHTags.Blocks.TOWN_BLOCKS.tag)
            .simpleItem()
            .lang("House")
            .register();

    public static final BlockEntry<WarehouseBlock> WAREHOUSE = REGISTRATE.block("warehouse", WarehouseBlock::new)
            .properties(t -> AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY)
            .tag(FHTags.Blocks.TOWN_BLOCKS.tag)
            .transform(axeOnly())
            .simpleItem()
            .lang("Warehouse")
            .register();

    public static final BlockEntry<MineBlock> MINE = REGISTRATE.block("mine", MineBlock::new)
            .properties(t -> AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY)
            .tag(FHTags.Blocks.TOWN_BLOCKS.tag)
            .transform(axeOnly())
            .simpleItem()
            .lang("Mining Camp")
            .register();

    public static final BlockEntry<MineBaseBlock> MINE_BASE = REGISTRATE.block("mine_base", MineBaseBlock::new)
            .properties(t -> AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY)
            .tag(FHTags.Blocks.TOWN_BLOCKS.tag)
            .transform(axeOnly())
            .simpleItem()
            .lang("Mining Base")
            .register();

    public static final BlockEntry<HuntingCampBlock> HUNTING_CAMP = REGISTRATE.block("hunting_camp", HuntingCampBlock::new)
            .properties(t -> AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY)
            .tag(FHTags.Blocks.TOWN_BLOCKS.tag)
            .transform(axeOnly())
            .simpleItem()
            .lang("Hunting Camp")
            .register();

    public static final BlockEntry<HuntingBaseBlock> HUNTING_BASE = REGISTRATE.block("hunting_base", HuntingBaseBlock::new)
            .properties(t -> AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY)
            .tag(FHTags.Blocks.TOWN_BLOCKS.tag)
            .transform(axeOnly())
            .simpleItem()
            .lang("Hunting Base")
            .register();

    public static void init() { }
}