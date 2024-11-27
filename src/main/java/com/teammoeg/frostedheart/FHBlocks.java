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
import com.teammoeg.frostedheart.data.FHBlockStateGen;
import com.teammoeg.frostedheart.loot.FHLootGen;
import com.teammoeg.frostedheart.util.constants.FHProps;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.teammoeg.frostedheart.FHMain.FH_REGISTRATE;
import static net.minecraft.world.level.block.Blocks.*;

public class FHBlocks {

    static {
        FH_REGISTRATE.setCreativeTab(FHTabs.BASE_TAB);
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

    // snow block
//    public static RegistryObject<Block> CONDENSED_IRON_ORE_BLOCK = register("condensed_iron_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_COPPER_ORE_BLOCK = register("condensed_copper_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_GOLD_ORE_BLOCK = register("condensed_gold_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_ZINC_ORE_BLOCK = register("condensed_zinc_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_SILVER_ORE_BLOCK = register("condensed_silver_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_TIN_ORE_BLOCK = register("condensed_tin_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_PYRITE_ORE_BLOCK = register("condensed_pyrite_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_NICKEL_ORE_BLOCK = register("condensed_nickel_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> CONDENSED_LEAD_ORE_BLOCK = register("condensed_lead_ore_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));

    // sludge block
    public static RegistryObject<Block> IRON_SLUDGE_BLOCK = register("iron_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> COPPER_SLUDGE_BLOCK = register("copper_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> GOLD_SLUDGE_BLOCK = register("gold_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> ZINC_SLUDGE_BLOCK = register("zinc_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> SILVER_SLUDGE_BLOCK = register("silver_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> TIN_SLUDGE_BLOCK = register("tin_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> PYRITE_SLUDGE_BLOCK = register("pyrite_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> NICKEL_SLUDGE_BLOCK = register("nickel_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));
    public static RegistryObject<Block> LEAD_SLUDGE_BLOCK = register("lead_sludge_block", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK)));


    // Snow ore blocks
    public static final BlockEntry<Block> CONDENSED_IRON_ORE_BLOCK = FH_REGISTRATE.block("condensed_iron_ore_block", Block::new)
            .initialProperties(() -> SNOW_BLOCK)
            .tag(FHTags.Blocks.CONDENSED_ORES)
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .blockstate(FHBlockStateGen.simpleCubeAll("ore/condensed_iron_ore"))
            .simpleItem()
            .register();

    // Snow ores
    public static final BlockEntry<SnowLayerBlock> CONDENSED_IRON_ORE = FH_REGISTRATE.block("condensed_iron_ore", SnowLayerBlock::new)
            .initialProperties(() -> SNOW)
            .tag(FHTags.Blocks.CONDENSED_ORES)
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .loot((lt, block) -> lt.add(block, FHLootGen.buildSnowLootTable(block, CONDENSED_IRON_ORE_BLOCK.get(), FHItems.CONDENSED_BALL_IRON_ORE.get())))
            .blockstate(FHBlockStateGen.snowLayered("condensed_iron_ore", "condensed_iron_ore_block", "ore/condensed_iron_ore"))
            .item()
            .model(FHBlockStateGen.itemModelLayered("condensed_iron_ore", "ore/condensed_iron_ore"))
            .build()
            .register();

    // SNOW ORES

    // layered snow
//    public static RegistryObject<Block> CONDENSED_IRON_ORE = register("condensed_iron_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_COPPER_ORE = register("condensed_copper_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_GOLD_ORE = register("condensed_gold_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_ZINC_ORE = register("condensed_zinc_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_SILVER_ORE = register("condensed_silver_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_TIN_ORE = register("condensed_tin_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_PYRITE_ORE = register("condensed_pyrite_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_NICKEL_ORE = register("condensed_nickel_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_LEAD_ORE = register("condensed_lead_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));


    // sludge layer
    public static RegistryObject<Block> IRON_SLUDGE = register("iron_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> COPPER_SLUDGE = register("copper_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> GOLD_SLUDGE = register("gold_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> ZINC_SLUDGE = register("zinc_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> SILVER_SLUDGE = register("silver_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> TIN_SLUDGE = register("tin_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> PYRITE_SLUDGE = register("pyrite_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> NICKEL_SLUDGE = register("nickel_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> LEAD_SLUDGE = register("lead_sludge", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));

    // STONE ORES
    public static RegistryObject<Block> SILVER_ORE = register("silver_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(GOLD_ORE)));
    public static RegistryObject<Block> TIN_ORE = register("tin_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> PYRITE_ORE = register("pyrite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(IRON_ORE)));
    public static RegistryObject<Block> NICKEL_ORE = register("nickel_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(IRON_ORE)));
    public static RegistryObject<Block> LEAD_ORE = register("lead_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> HALITE_ORE = register("halite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> SYLVITE_ORE = register("sylvite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> MAGNESITE_ORE = register("magnesite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));

    // DEEPSLATE ORES
    public static RegistryObject<Block> DEEPSLATE_SILVER_ORE = register("deepslate_silver_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_GOLD_ORE)));
    public static RegistryObject<Block> DEEPSLATE_TIN_ORE = register("deepslate_tin_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_PYRITE_ORE = register("deepslate_pyrite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_IRON_ORE)));
    public static RegistryObject<Block> DEEPSLATE_NICKEL_ORE = register("deepslate_nickel_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_IRON_ORE)));
    public static RegistryObject<Block> DEEPSLATE_LEAD_ORE = register("deepslate_lead_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_HALITE_ORE = register("deepslate_halite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_SYLVITE_ORE = register("deepslate_sylvite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_MAGNESITE_ORE = register("deepslate_magnesite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));

    // ADDITIONAL SOILS
    public static RegistryObject<Block> PEAT = register("peat_block", () -> new Block(BlockBehaviour.Properties.copy(MUD)));
    public static RegistryObject<Block> ROTTEN_WOOD = register("rotten_wood_block", () -> new Block(BlockBehaviour.Properties.copy(MUD)));
    public static RegistryObject<Block> BAUXITE = register("bauxite_block", () -> new Block(BlockBehaviour.Properties.copy(GRAVEL)));
    public static RegistryObject<Block> KAOLIN = register("kaolin_block", () -> new Block(BlockBehaviour.Properties.copy(CLAY)));
    public static RegistryObject<Block> BURIED_MYCELIUM = register("buried_mycelium", () -> new Block(BlockBehaviour.Properties.copy(MYCELIUM)));
    public static RegistryObject<Block> BURIED_PODZOL = register("buried_podzol", () -> new Block(BlockBehaviour.Properties.copy(PODZOL)));

    // PERMAFROST
    public static RegistryObject<Block> DIRT_PERMAFROST = register("dirt_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(DIRT).strength(2.0F)));
    public static RegistryObject<Block> MUD_PERMAFROST = register("mud_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(MUD).strength(2.0F)));
    public static RegistryObject<Block> GRAVEL_PERMAFROST = register("gravel_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(GRAVEL).strength(2.0F)));
    public static RegistryObject<Block> SAND_PERMAFROST = register("sand_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(SAND).strength(2.0F)));
    public static RegistryObject<Block> RED_SAND_PERMAFROST = register("red_sand_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(RED_SAND).strength(2.0F)));
    public static RegistryObject<Block> CLAY_PERMAFROST = register("clay_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(CLAY).strength(2.0F)));
    public static RegistryObject<Block> PEAT_PERMAFROST = register("peat_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(MUD).strength(2.0F)));
    public static RegistryObject<Block> ROTTEN_WOOD_PERMAFROST = register("rotten_wood_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(MUD).strength(2.0F)));
    public static RegistryObject<Block> BAUXITE_PERMAFROST = register("bauxite_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(GRAVEL).strength(2.0F)));
    public static RegistryObject<Block> KAOLIN_PERMAFROST = register("kaolin_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(CLAY).strength(2.0F)));
    public static RegistryObject<Block> MYCELIUM_PERMAFROST = register("mycelium_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(MYCELIUM).strength(2.0F)));
    public static RegistryObject<Block> PODZOL_PERMAFROST = register("podzol_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(PODZOL).strength(2.0F)));
    public static RegistryObject<Block> ROOTED_DIRT_PERMAFROST = register("rooted_dirt_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(ROOTED_DIRT).strength(2.0F)));
    public static RegistryObject<Block> COARSE_DIRT_PERMAFROST = register("coarse_dirt_permafrost", () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(COARSE_DIRT).strength(2.0F)));

    /*
    copper
    aluminum
    steel
    electrum
    constantan
    iron
    cast_iron
    brass
    duralumin
    gold
    silver
    nickel
    lead
    titanium
    bronze
    invar
    tungstensteel
    zinc
    tin
    magnesium
    tungsten
     */

    // METAL BLOCKS
//    public static RegistryObject<Block> COPPER_BLOCK = register("copper_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> ALUMINUM_BLOCK = register("aluminum_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> STEEL_BLOCK = register("steel_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> ELECTRUM_BLOCK = register("electrum_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> CONSTANTAN_BLOCK = register("constantan_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
//    public static RegistryObject<Block> IRON_BLOCK = register("iron_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> CAST_IRON_BLOCK = register("cast_iron_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
//    public static RegistryObject<Block> BRASS_BLOCK = register("brass_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> DURALUMIN_BLOCK = register("duralumin_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
//    public static RegistryObject<Block> GOLD_BLOCK = register("gold_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> SILVER_BLOCK = register("silver_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> NICKEL_BLOCK = register("nickel_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> LEAD_BLOCK = register("lead_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> TITANIUM_BLOCK = register("titanium_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> BRONZE_BLOCK = register("bronze_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> INVAR_BLOCK = register("invar_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> TUNGSTEN_STEEL_BLOCK = register("tungstensteel_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
//    public static RegistryObject<Block> ZINC_BLOCK = register("zinc_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> TIN_BLOCK = register("tin_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> MAGNESIUM_BLOCK = register("magnesium_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> TUNGSTEN_BLOCK = register("tungsten_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));

    // Rankine Blocks
    /*
    生石灰块quicklime_block

    耐火砖块refractory_brick

    高耐火砖块high_refractory_bricks

    菱镁矿块magnesite_block

    氧化镁块magnesia_block

    积雪packed_snow

    积雪台阶

    殷钢块invar_block
     */

    // Resource Blocks
    public static RegistryObject<Block> MAGNESITE_BLOCK = register("magnesite_block", () -> new Block(BlockBehaviour.Properties.copy(CALCITE)));
    public static RegistryObject<Block> MAGNESIA_BLOCK = register("magnesia_block", () -> new Block(BlockBehaviour.Properties.copy(CALCITE)));
    public static RegistryObject<Block> QUICKLIME_BLOCK = register("quicklime_block", () -> new Block(BlockBehaviour.Properties.copy(CALCITE)));
    public static RegistryObject<Block> COPPER_GRAVEL = register("copper_gravel", () -> new FHBaseBlock(FHProps.ore_gravel));

    // Crop Blocks
    public static RegistryObject<Block> RYE_BLOCK = register("rye_block", () -> new RyeBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.cropProps));
    public static RegistryObject<Block> WOLFBERRY_BUSH_BLOCK = register("wolfberry_bush_block", () -> new WolfBerryBushBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.berryBushBlocks, 2), "wolfberries", t -> new FoodBlockItem(t, FHItems.createProps(), FHFoodProperties.WOLFBERRIES));
    public static RegistryObject<Block> WHITE_TURNIP_BLOCK = register("white_turnip_block", () -> new WhiteTurnipBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.cropProps), (block) -> new FoodBlockItem(block, new Item.Properties(), FHFoodProperties.WHITE_TURNIP));

    // Building Blocks
    public static RegistryObject<Block> GENERATOR_BRICK = register("generator_brick", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> GENERATOR_CORE_T1 = register("generator_core_t1", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> GENERATOR_AMPLIFIER_T1 = register("generator_amplifier_r1", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> DURALUMIN_SHEETMETAL = register("duralumin_sheetmetal", () -> new FHBaseBlock(FHProps.metalDecoProps));
    public static RegistryObject<Block> REFRACTORY_BRICKS = register("refractory_bricks", () -> new Block(BlockBehaviour.Properties.copy(BRICKS)));
    public static RegistryObject<Block> HIGH_REFRACTORY_BRICKS = register("high_refractory_bricks", () -> new Block(BlockBehaviour.Properties.copy(BRICKS)));
    public static RegistryObject<Block> PACKED_SNOW = register("packed_snow", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK).strength(1.0F)));
    public static RegistryObject<Block> PACKED_SNOW_SLAB = register("packed_snow_slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(SNOW_BLOCK).strength(1.0F)));

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
    public static RegistryObject<Block> WHALE_BLOCK = register("whale_block", () -> new HugeMushroomBlock(Block.Properties
            .of().mapColor(MapColor.COLOR_GRAY)
            .sound(SoundType.MUD)
            .requiresCorrectToolForDrops()
            .strength(45, 800)));
    public static RegistryObject<Block> WHALE_BELLY_BLOCK = register("whale_belly_block", () -> new HugeMushroomBlock(Block.Properties
            .of().mapColor(MapColor.COLOR_YELLOW)
            .sound(SoundType.SLIME_BLOCK)
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
    public static final BlockEntry<SteamCoreBlock> STEAM_CORE = FH_REGISTRATE.block("steam_core", SteamCoreBlock::new)
            .properties(t -> t.sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2, 10)
                    .noOcclusion())
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item(FHBlockItem::new)
            .transform(ModelGen.customItemModel())
            .register();

    // Town Building Blocks
    public static RegistryObject<Block> HOUSE = register("house", () -> new HouseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> WAREHOUSE = register("warehouse", () -> new WarehouseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> MINE = register("mine", () -> new MineBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> MINE_BASE = register("mine_base", () -> new MineBaseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> HUNTING_CAMP = register("hunting_camp", () -> new HuntingCampBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> HUNTING_BASE = register("hunting_base", () -> new HuntingBaseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));

    public static void init() { }
}