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

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.AssemblerLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.teammoeg.chorda.multiblock.components.IOwnerState;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.common.blocks.metal.CrafterPatternInventory;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.AssemblerLogic.State;

@Mixin(AssemblerLogic.class)
public class AssemblerLogicMixin {

	public AssemblerLogicMixin() {
	}
	@Inject(at = @At("HEAD"), remap = false, method = "tickServer")
	public void fh$tickServer(IMultiblockContext<State> context,CallbackInfo cbi) {
		for(CrafterPatternInventory i:context.getState().patterns) {
			if(i instanceof IOwnerTile iot) {
				iot.setStoredOwner(((IOwnerState)context.getState()).getOwner());
			}
		}
	}
}
