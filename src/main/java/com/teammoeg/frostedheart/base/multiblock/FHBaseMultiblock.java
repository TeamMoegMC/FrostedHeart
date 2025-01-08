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

package com.teammoeg.frostedheart.base.multiblock;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.ie.DisassembleListener;
import com.teammoeg.frostedheart.compat.ie.FHMultiblockHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public abstract class FHBaseMultiblock extends IETemplateMultiblock {
    public FHBaseMultiblock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, MultiblockRegistration<?> baseState) {
        super(loc, masterFromOrigin, triggerFromOrigin, size, baseState);
    }

	@Override
    public boolean canBeMirrored() {
        return false;
    }

	@Override
	public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        BlockPos master = this.getMasterFromOriginOffset();
        FHMultiblockHelper.getBEHelper(world, origin.offset(master)).ifPresent(te -> {
            if (te.getState() instanceof DisassembleListener lis) {
            	lis.onDisassemble(this, te);
            }else
                FHMain.LOGGER.error("State is null when disassembling Multiblock.");
        });
        super.disassemble(world, origin, mirrored, clickDirectionAtCreation);
	}

}
