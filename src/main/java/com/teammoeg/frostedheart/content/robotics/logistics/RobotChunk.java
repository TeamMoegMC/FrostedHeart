/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.content.robotics.logistics.workers.INetworkCore;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RobotChunk {
    Map<ChunkSectionPos,BlockPos> networks = new HashMap<>();
    public LogisticNetwork getNetworkFor(World world,BlockPos actual) {
    	INetworkCore core = FHUtils.getExistingTileEntity(world, networks.get(new ChunkSectionPos(actual)), INetworkCore.class);
    	if(core==null)
    		return null;
    	return core.getNetwork();
    };
}
