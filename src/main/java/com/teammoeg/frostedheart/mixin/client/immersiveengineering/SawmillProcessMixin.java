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
