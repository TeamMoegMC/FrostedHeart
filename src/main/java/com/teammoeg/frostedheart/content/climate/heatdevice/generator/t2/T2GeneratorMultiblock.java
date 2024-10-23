/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHMultiblocks.Logic;
import com.teammoeg.frostedheart.base.multiblock.FHBaseMultiblock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class T2GeneratorMultiblock extends FHBaseMultiblock {

    public T2GeneratorMultiblock() {

        super(new ResourceLocation(FHMain.MODID, "multiblocks/generator_t2"),
                new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(3, 7, 3), Logic.GENERATOR_T2);
    }


    @Override
    public float getManualScale() {
        return 14;
    }

}
