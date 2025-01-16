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
import com.teammoeg.chorda.multiblock.CMultiblock;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks.Logic;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.chorda.util.ie.CMultiblockHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class T2GeneratorMultiblock extends CMultiblock {

    public T2GeneratorMultiblock() {

        super(new ResourceLocation(FHMain.MODID, "multiblocks/generator_t2"),
                new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(3, 7, 3), Logic.GENERATOR_T2);
    }


    @Override
    public float getManualScale() {
        return 14;
    }

    @Override
    public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        BlockPos master = this.getMasterFromOriginOffset();
        ChunkHeatData.removeTempAdjust(world, origin.offset(master));
        CMultiblockHelper.getBEHelper(world, origin.offset(master)).ifPresent(te -> {
            T2GeneratorState state = (T2GeneratorState) te.getState();
            if (state != null) {
            	if(state.manager!=null) {
            		state.manager.invalidate(world);
     
            	}
            }else
                FHMain.LOGGER.error("T2GeneratorState is null when disassembling T2GeneratorMultiblock.");
        });
        super.disassemble(world, origin, mirrored, clickDirectionAtCreation);
    }

}
