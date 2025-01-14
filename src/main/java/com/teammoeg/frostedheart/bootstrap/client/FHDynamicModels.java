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

package com.teammoeg.frostedheart.bootstrap.client;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorRenderer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorRenderer;
import com.teammoeg.frostedheart.content.research.blocks.MechCalcRenderer;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.model.DynamicBlockModelReference;

public class FHDynamicModels {
    public static void setup() {
        T1GeneratorRenderer.FUEL = DynamicBlockModelReference.getModelCached(FHMain.MODID, "block/multiblocks/generator_fuel").register();
        T2GeneratorRenderer.FUEL = DynamicBlockModelReference.getModelCached(FHMain.MODID, "block/multiblocks/generator_t2_fuel").register();
        MechCalcRenderer.MODEL   = DynamicBlockModelReference.getModelCached(FHMain.MODID, "block/mechanical_calculator_movable").register();
    }
}
