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

import static com.teammoeg.frostedheart.util.FHProps.berryBushBlocks;
import static com.teammoeg.frostedheart.util.FHProps.cropProps;
import static com.teammoeg.frostedheart.util.FHProps.metalDecoProps;
import static com.teammoeg.frostedheart.util.FHProps.ore_gravel;
import static com.teammoeg.frostedheart.util.FHProps.stoneDecoProps;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import com.teammoeg.frostedheart.base.item.FoodBlockItem;
import com.teammoeg.frostedheart.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.agriculture.RyeBlock;
import com.teammoeg.frostedheart.content.agriculture.WhiteTurnipBlock;
import com.teammoeg.frostedheart.content.agriculture.WolfBerryBushBlock;
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
import com.teammoeg.frostedheart.research.machines.DrawingDeskBlock;
import com.teammoeg.frostedheart.research.machines.MechCalcBlock;

import com.teammoeg.frostedheart.town.house.HouseBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;

public class FHBlocks {


    public static Block generator_brick = new FHBaseBlock("generator_brick", stoneDecoProps, FHBlockItem::new);

    public static Block generator_core_t1 = new FHBaseBlock("generator_core_t1", stoneDecoProps, FHBlockItem::new);
    public static Block generator_amplifier_r1 = new FHBaseBlock("generator_amplifier_r1", stoneDecoProps, FHBlockItem::new);
    public static Block rye_block = new RyeBlock("rye_block", WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, cropProps, FHBlockItem::new);
    public static Block wolfberry_bush_block = new WolfBerryBushBlock("wolfberry_bush_block", WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, berryBushBlocks, 2);
    public static Block white_turnip_block = new WhiteTurnipBlock("white_turnip_block", WorldTemperature.COLD_RESIST_GROW_TEMPERATURE, cropProps, ((block, properties) -> new FoodBlockItem(block, properties, FHFoods.WHITE_TURNIP)));
    public static Block copper_gravel = new FHBaseBlock("copper_gravel", ore_gravel, FHBlockItem::new);
    public static Block relic_chest = new RelicChestBlock("relic_chest");
    public static Block incubator1 = new IncubatorBlock("incubator", stoneDecoProps, FHTileTypes.INCUBATOR);
    public static Block incubator2 = new HeatIncubatorBlock("heat_incubator", metalDecoProps, FHTileTypes.INCUBATOR2);
    //        public static Block access_control = new AccessControlBlock("access_control", FHBlockItem::new);
//        public static Block gate = new FHBaseBlock("gate", AbstractBlock.Properties.from(Blocks.BEDROCK), FHBlockItem::new);
    public static Block fluorite_ore;
    public static Block halite_ore;
    public static Block heat_pipe = new HeatPipeBlock("heat_pipe", Block.Properties
            .create(Material.ROCK).sound(SoundType.WOOD)
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(1, 5)
            .notSolid(), FHBlockItem::new);

    public static Block debug_heater = new DebugHeaterBlock("debug_heater", Block.Properties
            .create(Material.ROCK).sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid(), FHBlockItem::new);
    public static Block charger = new ChargerBlock("charger", Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid(), FHBlockItem::new);
    public static Block oilburner = new OilBurnerBlock("oil_burner", Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid(), FHBlockItem::new);
    public static Block gasvent = new GasVentBlock("gas_vent", Block.Properties
            .create(Material.IRON)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid(), FHBlockItem::new);
    public static Block drawing_desk = new DrawingDeskBlock("drawing_desk", Block.Properties
            .create(Material.WOOD)
            .sound(SoundType.WOOD)
            .harvestTool(ToolType.AXE)
            .hardnessAndResistance(2, 6)
            .notSolid(), FHBlockItem::new);
    public static Block smoket1 = new SmokeBlockT1("smoke_block_t1", Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid(), FHBlockItem::new);
    /*public static Block high_strength_concrete=new FHBaseBlock("high_strength_concrete", Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(45, 800)
            .harvestLevel(3)
            ,FHBlockItem::new);*/
    public static Block mech_calc = new MechCalcBlock("mechanical_calculator", Block.Properties
            .create(Material.IRON)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid(), FHBlockItem::new);
    public static Block sauna = new SaunaBlock("sauna_vent", Block.Properties
            .create(Material.ROCK)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10)
            .notSolid(), FHBlockItem::new);
    public static Block house = new HouseBlock("house", Block.Properties
            .create(Material.WOOD)
            .sound(SoundType.WOOD)
            .setRequiresTool()
            .harvestTool(ToolType.AXE)
            .hardnessAndResistance(2, 6)
            .notSolid(), FHBlockItem::new);
    public static void init() {
    }
}