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

import static com.simibubi.create.foundation.data.ModelGen.*;
import static com.teammoeg.frostedheart.util.FHProps.*;

import java.util.function.Function;
import java.util.function.Supplier;

import com.cannolicatfish.rankine.init.RankineBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.repack.registrate.util.entry.BlockEntry;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import com.teammoeg.frostedheart.base.item.FoodBlockItem;
import com.teammoeg.frostedheart.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.adventure.block.*;
import com.teammoeg.frostedheart.content.agriculture.RyeBlock;
import com.teammoeg.frostedheart.content.agriculture.WhiteTurnipBlock;
import com.teammoeg.frostedheart.content.agriculture.WolfBerryBushBlock;
import com.teammoeg.frostedheart.content.decoration.FHOreBlock;
import com.teammoeg.frostedheart.content.decoration.RelicChestBlock;
import com.teammoeg.frostedheart.content.decoration.oilburner.GasVentBlock;
import com.teammoeg.frostedheart.content.decoration.oilburner.OilBurnerBlock;
import com.teammoeg.frostedheart.content.decoration.oilburner.SmokeBlockT1;
import com.teammoeg.frostedheart.content.incubator.HeatIncubatorBlock;
import com.teammoeg.frostedheart.content.incubator.IncubatorBlock;
import com.teammoeg.frostedheart.content.steamenergy.DebugHeaterBlock;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeBlock;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerBlock;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaBlock;
import com.teammoeg.frostedheart.content.steamenergy.steamcore.SteamCoreBlock;
import com.teammoeg.frostedheart.research.machines.DrawingDeskBlock;
import com.teammoeg.frostedheart.research.machines.MechCalcBlock;

import com.teammoeg.frostedheart.town.Farm.FarmBlock;
import com.teammoeg.frostedheart.town.house.HouseBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static com.teammoeg.frostedheart.util.FHProps.*;

public class FHBlocks {
	static final DeferredRegister<Block> registry=DeferredRegister.create(ForgeRegistries.BLOCKS, FHMain.MODID);
    private static final CreateRegistrate REGISTRATE = FHMain.registrate.getValue()
        .itemGroup(() -> FHMain.itemGroup);
	public static <T extends Block> RegistryObject<T> register(String name,Supplier<T> block,String itemName,Function<T,Item> item){
		RegistryObject<T> blk=registry.register(name, block);
		FHItems.registry.register(itemName,()->item.apply(blk.get()));
		return blk;
	}
	public static <T extends Block> RegistryObject<T> register(String name,Supplier<T> block){

		return register(name, block,name,FHBlockItem::new);
	}
	public static <T extends Block> RegistryObject<T> register(String name,Supplier<T> block,Function<T,Item> item){

		return register(name, block,name,item);
	}
    public static RegistryObject<Block> generator_brick = register("generator_brick",()->new FHBaseBlock(stoneDecoProps));

 /*   public static Block blood_block = new bloodBlock("blood_block", stoneProps, FHBlockItem::new);
    public static Block bone_block = new BoneBlock("bone_block", grassProps, FHBlockItem::new);
    //public static Block desk = new DeskBlock("desk", redStoneProps, FHBlockItem::new);
    public static Block small_garage = new SmallGarage("small_garage", grassProps, FHBlockItem::new);
    public static Block package_block = new PackageBlock("package_block", woodenProps, FHBlockItem::new);
    public static Block pebble_block = new PebbleBlock("pebble_block", stoneProps, FHBlockItem::new);
    public static Block odd_mark = new OddMark("odd_mark", redStoneProps, FHBlockItem::new);
    public static Block wooden_box = new WoodenBox("wooden_box", woodenProps, FHBlockItem::new);*/
    public static RegistryObject<Block> generator_core_t1 = register("generator_core_t1",()->new FHBaseBlock( stoneDecoProps));
    public static RegistryObject<Block> generator_amplifier_r1 = register("generator_amplifier_r1",()->new FHBaseBlock( stoneDecoProps));
    public static RegistryObject<Block> rye_block = register("rye_block",()->new RyeBlock( WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, cropProps));
    public static RegistryObject<Block> wolfberry_bush_block = register("wolfberry_bush_block",()->new WolfBerryBushBlock( WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, berryBushBlocks, 2),"wolfberries",t->new FoodBlockItem(t, FHItems.createProps(), FHFoods.WOLFBERRIES));
    public static RegistryObject<Block> white_turnip_block = register("white_turnip_block",()->new WhiteTurnipBlock( WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, cropProps),(block) -> new FoodBlockItem(block, new Item.Properties().group(FHMain.itemGroup), FHFoods.WHITE_TURNIP));
    public static RegistryObject<Block> copper_gravel = register("copper_gravel",()->new FHBaseBlock( ore_gravel));
    public static RegistryObject<Block> relic_chest = register("relic_chest",()->new RelicChestBlock());
    public static RegistryObject<Block> incubator1 = register("incubator",()->new IncubatorBlock( stoneDecoProps, FHTileTypes.INCUBATOR));
    public static RegistryObject<Block> incubator2 = register("heat_incubator",()->new HeatIncubatorBlock( metalDecoProps, FHTileTypes.INCUBATOR2));
    //        public static RegistryObject<Block> access_control = new AccessControlBlock("access_control");
//        public static RegistryObject<Block> gate = new FHBaseBlock("gate", AbstractBlock.Properties.from(Blocks.BEDROCK));
    public static RegistryObject<Block> fluorite_ore =register("fluorite_ore",()->new FHOreBlock( RankineBlocks.DEF_ORE.harvestLevel(3)));
    public static RegistryObject<Block> halite_ore=register("halite_ore",()->new FHOreBlock(RankineBlocks.DEF_ORE.harvestLevel(2)));
    public static RegistryObject<Block> heat_pipe = register("heat_pipe",()->new HeatPipeBlock( Block.Properties
            .create(Material.ROCK).sound(SoundType.WOOD)
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(1, 5)
            .notSolid()));

    public static RegistryObject<Block> debug_heater = register("debug_heater",()->new DebugHeaterBlock( Block.Properties
            .create(Material.ROCK).sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid()));
    public static RegistryObject<Block> charger = register("charger",()->new ChargerBlock( Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid()));
    public static RegistryObject<Block> oilburner = register("oil_burner",()->new OilBurnerBlock( Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid()));
    public static RegistryObject<Block> gasvent = register("gas_vent",()->new GasVentBlock( Block.Properties
            .create(Material.IRON)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid()));
    public static RegistryObject<Block> drawing_desk = register("drawing_desk",()->new DrawingDeskBlock( Block.Properties
            .create(Material.WOOD)
            .sound(SoundType.WOOD)
            .harvestTool(ToolType.AXE)
            .hardnessAndResistance(2, 6)
            .notSolid()));
    public static RegistryObject<Block> smoket1 = register("smoke_block_t1",()->new SmokeBlockT1(Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid()));
    /*public static RegistryObject<Block> high_strength_concrete=register("high_strength_concrete",()->new FHBaseBlock( Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(45, 800)
            .harvestLevel(3)
            ,FHBlockItem::new));*/
    public static RegistryObject<Block> mech_calc = register("mechanical_calculator",()->new MechCalcBlock( Block.Properties
            .create(Material.IRON)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid()));
    public static RegistryObject<Block> sauna = register("sauna_vent",()->new SaunaBlock( Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid()));
    public static final BlockEntry<SteamCoreBlock> steam_core = REGISTRATE.block("steam_core", SteamCoreBlock::new)
        .initialProperties(Material.IRON)
        .properties(t->t
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid())
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .transform(customItemModel())
        .register();
    public static RegistryObject<Block> house = register("house",()->new HouseBlock( Block.Properties
            .notSolid(), FHBlockItem::new);
    public static Block house = new HouseBlock("house", Block.Properties
            .create(Material.WOOD)
            .sound(SoundType.WOOD)
            .setRequiresTool()
            .harvestTool(ToolType.AXE)
            .hardnessAndResistance(2, 6)
            .notSolid(), FHBlockItem::new);
    public static Block farm = new FarmBlock("farm_block", Block.Properties
            .create(Material.WOOD)
            .sound(SoundType.WOOD)
            .setRequiresTool()
            .harvestTool(ToolType.AXE)
            .hardnessAndResistance(2, 6)
            .notSolid(), FHBlockItem::new);
    public static void init() {
    }
}