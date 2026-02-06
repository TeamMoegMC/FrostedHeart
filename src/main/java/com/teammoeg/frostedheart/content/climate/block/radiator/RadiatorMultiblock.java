/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.block.radiator;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.block.generator.HeatingMultiblock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class RadiatorMultiblock extends HeatingMultiblock {
    public RadiatorMultiblock() {
        super(new ResourceLocation(FHMain.MODID, "multiblocks/heat_radiator"),
                new BlockPos(0, 0, 0), new BlockPos(0, 0, 0), new BlockPos(1, 3, 1),
                FHMultiblocks.Registration.RADIATOR);
    }

    @Override
    public float getManualScale() {
        return 16;
    }



}
