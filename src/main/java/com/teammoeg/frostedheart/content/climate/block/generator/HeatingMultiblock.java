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

package com.teammoeg.frostedheart.content.climate.block.generator;

import com.teammoeg.chorda.multiblock.CMultiblock;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public abstract class HeatingMultiblock extends CMultiblock {

	public HeatingMultiblock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, MultiblockRegistration<?> baseState) {
		super(loc, masterFromOrigin, triggerFromOrigin, size, baseState);
		// TODO Auto-generated constructor stub
	}
    @Override
    public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
    	BlockPos master = this.getMasterFromOriginOffset();
        ChunkHeatData.removeTempAdjust(world, getMasterPos(origin,mirrored,clickDirectionAtCreation).below(master.getY()));
        super.disassemble(world, origin, mirrored, clickDirectionAtCreation);
    }

}
