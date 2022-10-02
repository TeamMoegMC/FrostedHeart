package com.teammoeg.frostedheart.mixin.chisel;

import net.minecraftforge.event.world.BiomeLoadingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.common.init.ChiselWorldGen;

@Mixin(ChiselWorldGen.class)
public class MixinChiselWorldGen{
    @Inject(at = @At("HEAD"), method = "registerWorldGen", cancellable = true, remap = false)
    private static void init(BiomeLoadingEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
