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

package com.teammoeg.frostedheart.client;

import blusunrize.immersiveengineering.client.render.tile.DynamicModel;
import blusunrize.immersiveengineering.client.render.tile.DynamicModel.ModelType;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.renderer.HeatPipeRenderer;
import com.teammoeg.frostedheart.client.renderer.MechCalcRenderer;
import com.teammoeg.frostedheart.client.renderer.T1GeneratorRenderer;
import com.teammoeg.frostedheart.client.renderer.T2GeneratorRenderer;
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
        MechCalcRenderer.MODEL = DynamicModel.createSided(
                new ResourceLocation(FHMain.MODID, "block/mechanical_calculator_movable.obj"),
                "mechanical_calculator_movable", ModelType.IE_OBJ
        );
    }
}
