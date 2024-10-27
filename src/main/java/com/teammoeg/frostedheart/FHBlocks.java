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

import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import com.teammoeg.frostedheart.base.item.FoodBlockItem;
import com.teammoeg.frostedheart.content.agriculture.RyeBlock;
import com.teammoeg.frostedheart.content.agriculture.WhiteTurnipBlock;
import com.teammoeg.frostedheart.content.agriculture.WolfBerryBushBlock;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.decoration.BoneBlock;
import com.teammoeg.frostedheart.content.decoration.OddMark;
import com.teammoeg.frostedheart.content.decoration.PackageBlock;
import com.teammoeg.frostedheart.content.decoration.PebbleBlock;
import com.teammoeg.frostedheart.content.decoration.RelicChestBlock;
import com.teammoeg.frostedheart.content.decoration.SmallGarage;
import com.teammoeg.frostedheart.content.decoration.SmokeBlockT1;
import com.teammoeg.frostedheart.content.decoration.WoodenBox;
import com.teammoeg.frostedheart.content.decoration.bloodBlock;
import com.teammoeg.frostedheart.content.incubator.HeatIncubatorBlock;
import com.teammoeg.frostedheart.content.incubator.IncubatorBlock;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskBlock;
import com.teammoeg.frostedheart.content.research.blocks.MechCalcBlock;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeBlock;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerBlock;
import com.teammoeg.frostedheart.content.steamenergy.fountain.FountainBlock;
import com.teammoeg.frostedheart.content.steamenergy.fountain.FountainNozzleBlock;
import com.teammoeg.frostedheart.content.steamenergy.debug.DebugHeaterBlock;
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

import com.teammoeg.frostedheart.util.constants.FHProps;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static com.teammoeg.frostedheart.FHMain.FH_REGISTRATE;
import static net.minecraft.world.level.block.Blocks.*;

public class FHBlocks {
    public static void init() {

    }

    static final DeferredRegister<Block> registry = DeferredRegister.create(ForgeRegistries.BLOCKS, FHMain.MODID);

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, String itemName, Function<T, Item> item) {
        RegistryObject<T> blk = registry.register(name, block);
        FHItems.registry.register(itemName, () -> item.apply(blk.get()));
        return blk;
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
        return register(name, block, name, FHBlockItem::new);
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, Function<T, Item> item) {
        return register(name, block, name, item);
    }

    // SNOW ORES
    public static RegistryObject<Block> CONDENSED_IRON_ORE = register("condensed_iron_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_COPPER_ORE = register("condensed_copper_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_GOLD_ORE = register("condensed_gold_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_ZINC_ORE = register("condensed_zinc_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_SILVER_ORE = register("condensed_silver_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_CASSITERITE_ORE = register("condensed_cassiterite_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_PYRITE_ORE = register("condensed_pyrite_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_PENTLANDITE_ORE = register("condensed_pentlandite_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));
    public static RegistryObject<Block> CONDENSED_GALENA_ORE = register("condensed_galena_ore", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(SNOW)));

    // STONE ORES
    public static RegistryObject<Block> SILVER_ORE = register("silver_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(GOLD_ORE)));
    public static RegistryObject<Block> CASSITERITE_ORE = register("cassiterite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> PYRITE_ORE = register("pyrite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(IRON_ORE)));
    public static RegistryObject<Block> PENTLANDITE_ORE = register("pentlandite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(IRON_ORE)));
    public static RegistryObject<Block> GALENA_ORE = register("galena_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> HALITE_ORE = register("halite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> POTASH_ORE = register("potash_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));
    public static RegistryObject<Block> MAGNESITE_ORE = register("magnesite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(COPPER_ORE)));

    // DEEPSLATE ORES
    public static RegistryObject<Block> DEEPSLATE_SILVER_ORE = register("deepslate_silver_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_GOLD_ORE)));
    public static RegistryObject<Block> DEEPSLATE_CASSITERITE_ORE = register("deepslate_cassiterite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_PYRITE_ORE = register("deepslate_pyrite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_IRON_ORE)));
    public static RegistryObject<Block> DEEPSLATE_PENTLANDITE_ORE = register("deepslate_pentlandite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_IRON_ORE)));
    public static RegistryObject<Block> DEEPSLATE_GALENA_ORE = register("deepslate_galena_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_HALITE_ORE = register("deepslate_halite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_POTASH_ORE = register("deepslate_potash_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));
    public static RegistryObject<Block> DEEPSLATE_MAGNESITE_ORE = register("deepslate_magnesite_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(DEEPSLATE_COPPER_ORE)));

    // ADDITIONAL SOILS
    public static RegistryObject<Block> PEAT = register("peat_block", () -> new Block(BlockBehaviour.Properties.copy(MUD)));
    public static RegistryObject<Block> ROTTEN_WOOD = register("rotten_wood_block", () -> new Block(BlockBehaviour.Properties.copy(MUD)));
    public static RegistryObject<Block> BAUXITE = register("bauxite_block", () -> new Block(BlockBehaviour.Properties.copy(GRAVEL)));
    public static RegistryObject<Block> KAOLIN = register("kaolin_block", () -> new Block(BlockBehaviour.Properties.copy(CLAY)));

    // PERMAFROST
    public static RegistryObject<Block> DIRT_PERMAFROST = register("dirt_permafrost", () -> new Block(BlockBehaviour.Properties.copy(DIRT).strength(2.0F)));
    public static RegistryObject<Block> MYCELIUM_PERMAFROST = register("mycelium_permafrost", () -> new Block(BlockBehaviour.Properties.copy(MYCELIUM).strength(2.0F)));
    public static RegistryObject<Block> PODZOL_PERMAFROST = register("podzol_permafrost", () -> new Block(BlockBehaviour.Properties.copy(PODZOL).strength(2.0F)));
    public static RegistryObject<Block> ROOTED_DIRT_PERMAFROST = register("rooted_dirt_permafrost", () -> new Block(BlockBehaviour.Properties.copy(ROOTED_DIRT).strength(2.0F)));
    public static RegistryObject<Block> COARSE_DIRT_PERMAFROST = register("coarse_dirt_permafrost", () -> new Block(BlockBehaviour.Properties.copy(COARSE_DIRT).strength(2.0F)));
    public static RegistryObject<Block> MUD_PERMAFROST = register("mud_permafrost", () -> new Block(BlockBehaviour.Properties.copy(MUD).strength(2.0F)));
    public static RegistryObject<Block> GRAVEL_PERMAFROST = register("gravel_permafrost", () -> new Block(BlockBehaviour.Properties.copy(GRAVEL).strength(2.0F)));
    public static RegistryObject<Block> SAND_PERMAFROST = register("sand_permafrost", () -> new Block(BlockBehaviour.Properties.copy(SAND).strength(2.0F)));
    public static RegistryObject<Block> RED_SAND_PERMAFROST = register("red_sand_permafrost", () -> new Block(BlockBehaviour.Properties.copy(RED_SAND).strength(2.0F)));
    public static RegistryObject<Block> CLAY_PERMAFROST = register("clay_permafrost", () -> new Block(BlockBehaviour.Properties.copy(CLAY).strength(2.0F)));
    public static RegistryObject<Block> PEAT_PERMAFROST = register("peat_permafrost", () -> new Block(BlockBehaviour.Properties.copy(MUD).strength(2.0F)));
    public static RegistryObject<Block> ROTTEN_WOOD_PERMAFROST = register("rotten_wood_permafrost", () -> new Block(BlockBehaviour.Properties.copy(MUD).strength(2.0F)));
    public static RegistryObject<Block> BAUXITE_PERMAFROST = register("bauxite_permafrost", () -> new Block(BlockBehaviour.Properties.copy(GRAVEL).strength(2.0F)));
    public static RegistryObject<Block> KAOLIN_PERMAFROST = register("kaolin_permafrost", () -> new Block(BlockBehaviour.Properties.copy(CLAY).strength(2.0F)));

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
    public static RegistryObject<Block> TUNGSTENSTEEL_BLOCK = register("tungstensteel_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
    public static RegistryObject<Block> ZINC_BLOCK = register("zinc_block", () -> new Block(BlockBehaviour.Properties.copy(IRON_BLOCK)));
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

    public static RegistryObject<Block> QUICKLIME_BLOCK = register("quicklime_block", () -> new Block(BlockBehaviour.Properties.copy(CALCITE)));
    public static RegistryObject<Block> REFRACTORY_BRICKS = register("refractory_bricks", () -> new Block(BlockBehaviour.Properties.copy(BRICKS)));
    public static RegistryObject<Block> HIGH_REFRACTORY_BRICKS = register("high_refractory_bricks", () -> new Block(BlockBehaviour.Properties.copy(BRICKS)));
    public static RegistryObject<Block> MAGNESITE_BLOCK = register("magnesite_block", () -> new Block(BlockBehaviour.Properties.copy(CALCITE)));
    public static RegistryObject<Block> MAGNESIA_BLOCK = register("magnesia_block", () -> new Block(BlockBehaviour.Properties.copy(CALCITE)));
    public static RegistryObject<Block> PACKED_SNOW = register("packed_snow", () -> new Block(BlockBehaviour.Properties.copy(SNOW_BLOCK).strength(1.0F)));
    public static RegistryObject<Block> PACKED_SNOW_SLAB = register("packed_snow_slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(SNOW_BLOCK).strength(1.0F)));


    // Resource Blocks
    public static RegistryObject<Block> copper_gravel = register("copper_gravel", () -> new FHBaseBlock(FHProps.ore_gravel));

    // Crop Blocks
    public static RegistryObject<Block> rye_block = register("rye_block", () -> new RyeBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.cropProps));
    public static RegistryObject<Block> wolfberry_bush_block = register("wolfberry_bush_block", () -> new WolfBerryBushBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.berryBushBlocks, 2), "wolfberries", t -> new FoodBlockItem(t, FHItems.createProps(), FHFoods.WOLFBERRIES));
    public static RegistryObject<Block> white_turnip_block = register("white_turnip_block", () -> new WhiteTurnipBlock(WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, FHProps.cropProps), (block) -> new FoodBlockItem(block, new Item.Properties(), FHFoods.WHITE_TURNIP));

    // Building Blocks
    public static RegistryObject<Block> generator_brick = register("generator_brick", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> generator_core_t1 = register("generator_core_t1", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> generator_amplifier_r1 = register("generator_amplifier_r1", () -> new FHBaseBlock(FHProps.stoneDecoProps));
    public static RegistryObject<Block> DURALUMIN_SHEETMETAL = register("sheetmetal_duralumin", () -> new FHBaseBlock(FHProps.metalDecoProps));

    // Decoration Blocks
    public static RegistryObject<Block> blood_block = register("blood_block", () -> new bloodBlock(FHProps.stoneProps));
    public static RegistryObject<Block> bone_block = register("bone_block", () -> new BoneBlock(FHProps.grassProps));
    public static RegistryObject<Block> small_garage = register("small_garage", () -> new SmallGarage(FHProps.grassProps));
    public static RegistryObject<Block> package_block = register("package_block", () -> new PackageBlock(FHProps.woodenProps));
    public static RegistryObject<Block> pebble_block = register("pebble_block", () -> new PebbleBlock(FHProps.stoneProps));
    public static RegistryObject<Block> odd_mark = register("odd_mark", () -> new OddMark(FHProps.redStoneProps));
    public static RegistryObject<Block> wooden_box = register("wooden_box", () -> new WoodenBox(FHProps.woodenProps));
    public static RegistryObject<Block> makeshift_generator_broken = register("makeshift_generator_broken", () -> new FHBaseBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(45, 800)));

    public static RegistryObject<Block> broken_plate = register("broken_plate", () -> new FHBaseBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(45, 800)));

    // Machine Blocks
    public static RegistryObject<Block> relic_chest = register("relic_chest", RelicChestBlock::new);
    public static RegistryObject<Block> incubator1 = register("incubator", () -> new IncubatorBlock(FHProps.stoneDecoProps, FHTileTypes.INCUBATOR));
    public static RegistryObject<Block> incubator2 = register("heat_incubator", () -> new HeatIncubatorBlock(FHProps.metalDecoProps, FHTileTypes.INCUBATOR2));
    public static RegistryObject<Block> heat_pipe = register("heat_pipe", () -> new HeatPipeBlock(Block.Properties
            .of().mapColor(MapColor.STONE).sound(SoundType.WOOD)
            .strength(1, 5)
            .noOcclusion()));
    public static RegistryObject<Block> debug_heater = register("debug_heater", () -> new DebugHeaterBlock(Block.Properties
            .of().mapColor(MapColor.STONE).sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> charger = register("charger", () -> new ChargerBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> oilburner = register("oil_burner", () -> new OilBurnerBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> gasvent = register("gas_vent", () -> new GasVentBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> drawing_desk = register("drawing_desk", () -> new DrawingDeskBlock(Block.Properties
            .of().mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2, 6)
            .noOcclusion()));
    public static RegistryObject<Block> smoket1 = register("smoke_block_t1", () -> new SmokeBlockT1(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> mech_calc = register("mechanical_calculator", () -> new MechCalcBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> sauna = register("sauna_vent", () -> new SaunaBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> fountain = register("fountain_base", () -> new FountainBlock(Block.Properties
            .of().mapColor(MapColor.STONE)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static RegistryObject<Block> fountain_nozzle = register("fountain_nozzle", () -> new FountainNozzleBlock(Block.Properties
            .of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10)
            .noOcclusion()));
    public static final BlockEntry<SteamCoreBlock> steam_core = FH_REGISTRATE.block("steam_core", SteamCoreBlock::new)
            .properties(t -> t.sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2, 10)
                    .noOcclusion())
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item(FHBlockItem::new)
            .transform(ModelGen.customItemModel())
            .register();

    // Town Building Blocks
    public static RegistryObject<Block> house = register("house", () -> new HouseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> warehouse = register("warehouse", () -> new WarehouseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> mine = register("mine", () -> new MineBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> mine_base = register("mine_base", () -> new MineBaseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> hunting_camp = register("hunting_camp", () -> new HuntingCampBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
    public static RegistryObject<Block> hunting_base = register("hunting_base", () -> new HuntingBaseBlock(AbstractTownWorkerBlock.TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY));
}