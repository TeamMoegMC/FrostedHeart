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

package com.teammoeg.frostedheart.content.climate.heatdevice.radiator;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.chorda.multiblock.CMultiblock;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class RadiatorMultiblock extends CMultiblock {
    public RadiatorMultiblock() {
        super(new ResourceLocation(FHMain.MODID, "multiblocks/heat_radiator"),
                new BlockPos(0, 0, 0), new BlockPos(0, 0, 0), new BlockPos(1, 3, 1),
                FHMultiblocks.Logic.RADIATOR);
    }

    @Override
    public float getManualScale() {
        return 16;
    }

    @Override
    public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        BlockPos master = this.getMasterFromOriginOffset();
        ChunkHeatData.removeTempAdjust(world, origin.offset(master));
        super.disassemble(world, origin, mirrored, clickDirectionAtCreation);
    }

}
