package com.teammoeg.frostedheart.mixin.electrodynamics;

import electrodynamics.prefab.tile.components.type.ComponentFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComponentFluidHandler.class)
public class ComponentFluidHandlerMixin {
    /**
     * Fixes the NullPointerException when filling fluids not defined in Electrodynamics
     */
    @Inject(method = "fill", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void inject$fill(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (((ComponentFluidHandlerAccess) this).getFluids().containsKey(resource.getFluid())) {
            cir.setReturnValue(0);
        }
    }
}
