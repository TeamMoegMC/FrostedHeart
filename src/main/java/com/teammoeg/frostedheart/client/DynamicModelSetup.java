package com.teammoeg.frostedheart.client;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.renderer.HeatPipeRenderer;
import com.teammoeg.frostedheart.client.renderer.MechCalcRenderer;
import com.teammoeg.frostedheart.client.renderer.T1GeneratorRenderer;
import com.teammoeg.frostedheart.client.renderer.T2GeneratorRenderer;

import blusunrize.immersiveengineering.client.render.tile.DynamicModel;
import blusunrize.immersiveengineering.client.render.tile.DynamicModel.ModelType;
import net.minecraft.util.ResourceLocation;

public class DynamicModelSetup {
	public static void setup() {
        T1GeneratorRenderer.FUEL = DynamicModel.createSided(
				new ResourceLocation(FHMain.MODID, "block/multiblocks/generator_fuel.obj"),
				"generator_t1_fuel", ModelType.IE_OBJ
		);
		 T2GeneratorRenderer.FUEL = DynamicModel.createSided(
					new ResourceLocation(FHMain.MODID, "block/multiblocks/generator_t2_fuel.obj"),
					"generator_t2_fuel", ModelType.IE_OBJ
			);
		 HeatPipeRenderer.RIM = DynamicModel.createSimple(
					new ResourceLocation(FHMain.MODID, "block/fluid_pipe/pipe_rim.obj"),
					"pipe_rim", ModelType.IE_OBJ
			);
		 MechCalcRenderer.MODEL=DynamicModel.createSided(
					new ResourceLocation(FHMain.MODID, "block/mechanical_calculator_movable.obj"),
					"mechanical_calculator_movable", ModelType.IE_OBJ
			);
	}
}
