package com.teammoeg.frostedheart;

import static com.teammoeg.frostedheart.util.FHProps.berryBushBlocks;
import static com.teammoeg.frostedheart.util.FHProps.cropProps;
import static com.teammoeg.frostedheart.util.FHProps.ore_gravel;
import static com.teammoeg.frostedheart.util.FHProps.stoneDecoProps;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import com.teammoeg.frostedheart.base.item.FoodBlockItem;
import com.teammoeg.frostedheart.content.agriculture.RyeBlock;
import com.teammoeg.frostedheart.content.agriculture.WhiteTurnipBlock;
import com.teammoeg.frostedheart.content.agriculture.WolfBerryBushBlock;
import com.teammoeg.frostedheart.content.cmupdate.CMUpdateBlock;
import com.teammoeg.frostedheart.content.decoration.RelicChestBlock;
import com.teammoeg.frostedheart.content.decoration.oilburner.OilBurnerBlock;
import com.teammoeg.frostedheart.content.decoration.oilburner.SmokeBlockT1;
import com.teammoeg.frostedheart.content.steamenergy.DebugHeaterBlock;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeBlock;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerBlock;
import com.teammoeg.frostedheart.research.machines.DrawingDeskBlock;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;

public class FHBlocks {
        public static Block cmupdate = new CMUpdateBlock("cmupdate",Block.Properties.create(Material.ROCK), FHBlockItem::new);
		public static void init() {
        }

        public static Block generator_brick = new FHBaseBlock("generator_brick", stoneDecoProps, FHBlockItem::new);
        public static Block generator_core_t1 = new FHBaseBlock("generator_core_t1", stoneDecoProps, FHBlockItem::new);
        public static Block generator_amplifier_r1 = new FHBaseBlock("generator_amplifier_r1", stoneDecoProps, FHBlockItem::new);
        public static Block rye_block = new RyeBlock("rye_block", -10, cropProps, FHBlockItem::new);
        public static Block wolfberry_bush_block = new WolfBerryBushBlock("wolfberry_bush_block", -100, berryBushBlocks, 10);
        public static Block white_turnip_block = new WhiteTurnipBlock("white_turnip_block", -10, cropProps, ((block, properties) -> new FoodBlockItem(block, properties, FHFoods.WHITE_TURNIP)));
        public static Block copper_gravel = new FHBaseBlock("copper_gravel", ore_gravel, FHBlockItem::new);
        public static Block relic_chest = new RelicChestBlock("relic_chest");
        //        public static Block access_control = new AccessControlBlock("access_control", FHBlockItem::new);
//        public static Block gate = new FHBaseBlock("gate", AbstractBlock.Properties.from(Blocks.BEDROCK), FHBlockItem::new);
        public static Block fluorite_ore;

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
        public static Block oilburner=new OilBurnerBlock("oil_burner", Block.Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10)
                .notSolid(), FHBlockItem::new);
        public static Block drawing_desk=new DrawingDeskBlock("drawing_desk", Block.Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10)
                .notSolid(), FHBlockItem::new);
        public static Block smoket1=new SmokeBlockT1("smoke_block_t1", Block.Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10)
                .notSolid(), FHBlockItem::new);
        public static Block high_strength_concrete=new FHBaseBlock("high_strength_concrete", Block.Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(45, 800)
                .harvestLevel(3)
                ,FHBlockItem::new);
    }