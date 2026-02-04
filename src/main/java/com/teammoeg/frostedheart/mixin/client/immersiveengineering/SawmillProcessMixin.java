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

package com.teammoeg.frostedheart.mixin.client.immersiveengineering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.sawmill.SawmillProcess;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

@Mixin(SawmillProcess.class)
public abstract class SawmillProcessMixin {

	public SawmillProcessMixin() {
	}
	@Inject(at=@At("TAIL"),method="getRelativeProcessStep",remap=false,cancellable=true)
	public void fh$getRelativeProcessStep(Level level,CallbackInfoReturnable<Float> cbi)
	{
		cbi.setReturnValue(Mth.clamp(cbi.getReturnValueF(), 0f, 1f));
	}
}
