package com.teammoeg.frostedheart.mixin.client;

import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;
import net.minecraft.client.gui.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastGui.class)
public class ToastGuiMixin {

    @Inject(method = "add", at = @At(value = "HEAD"), cancellable = true)
    public void disable(IToast toastIn, CallbackInfo ci) {
        if (toastIn instanceof TutorialToast) {
            ci.cancel();
        }
    }

}
