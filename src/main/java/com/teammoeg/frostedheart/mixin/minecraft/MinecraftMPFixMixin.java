package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class MinecraftMPFixMixin {
	@Inject(at = @At("HEAD"), method = "allowsMultiplayer", cancellable = true)
	public void allowMP(CallbackInfoReturnable<Boolean> callbackInfo) {
		callbackInfo.setReturnValue(true);
	}

	@Inject(at = @At("HEAD"), method = "allowsChat", cancellable = true)

	public void allowChat(CallbackInfoReturnable<Boolean> callbackInfo) {
		callbackInfo.setReturnValue(true);
	}
}
