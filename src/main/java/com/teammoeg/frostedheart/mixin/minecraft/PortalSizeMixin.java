package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.block.PortalSize;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalSize.class)
public class PortalSizeMixin {
    @Inject(at = @At("INVOKE"), method = "func_242974_d()I", cancellable = true)
    private void func_242974_d(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(0);
    }
}