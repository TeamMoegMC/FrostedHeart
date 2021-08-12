package com.teammoeg.frostedheart.mixin.survive;

import com.stereowalker.survive.registries.SurviveRegistryEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SurviveRegistryEvents.class)
public class SurviveRegistryEventsMixin {
    /**
     * Disables survive enchantments completely
     */
    @Inject(method = "registerEnchantments", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private static void cancelSurviveEnchantments(CallbackInfo ci) {
        ci.cancel();
    }
}
