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

package com.teammoeg.frostedheart.content.robotics.logistics.core;

import com.teammoeg.chorda.multiblock.CMultiblock;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.util.LazyOptional;

public class LogisticCoreMultiblock extends CMultiblock {

	public LogisticCoreMultiblock() {
		super(new ResourceLocation(FHMain.MODID, "multiblocks/logistic_core"),
            new BlockPos(0, 0, 0), new BlockPos(0, 1, 0), new BlockPos(1, 3, 1), FHMultiblocks.Registration.LOGISTIC_CORE
    );
	}


    @Override
    public float getManualScale() {
        return 16;
    }
}
