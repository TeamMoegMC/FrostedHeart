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

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.ChordaMetaEvents;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IEMultiblocks;

@Mixin(IEMultiblocks.class)
public class IERegistrationMixin {

	public IERegistrationMixin() {
	}
	@Inject(at=@At("TAIL"),method="init",remap=false)
	private static void Ch$modConstruction(CallbackInfo cbi) {
		Chorda.LOGGER.info("Sending Immersive Engineering registry event...");
		//MinecraftForge.EVENT_BUS.post(new IERegistryEvent());
		ChordaMetaEvents.IE_REGISTRY.setFinished();;
		//System.out.println("IEInit called");
	}
}
