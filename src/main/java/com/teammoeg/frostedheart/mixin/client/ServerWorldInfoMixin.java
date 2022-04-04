package com.teammoeg.frostedheart.mixin.client;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.storage.ServerWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorldInfo.class)
public class ServerWorldInfoMixin {

    /**
     * Reference: https://github.com/CorgiTaco/ShutupExperimentalSettings/blob/master/src/main/java/corgitaco/shutupexperimentalsettings/mixin/client/MixinServerWorldInfo.java
     * @reason Shut up experimental settings
     */
    @Inject(method = "getLifecycle", at = @At("HEAD"), cancellable = true)
    private void forceStableLifeCycle(CallbackInfoReturnable<Lifecycle> cir) {
        cir.setReturnValue(Lifecycle.stable());
    }
}
