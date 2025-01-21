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
		ChordaMetaEvents.ieRegistry.setFinished();;
		//System.out.println("IEInit called");
	}
}
