/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.online;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.SaveDuringSolveObservation;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Functional GameTest only; ordinary and load-server runs do not include this configuration. */
@Mixin(value = TiledThermalField.class, remap = false)
public abstract class PrivateSaveObservationMixin {
    @Inject(method = "advance", at = @At("RETURN"))
    private void privateSave$observe(long deadline, CallbackInfoReturnable<Boolean> result) {
        SaveDuringSolveObservation.privateAdvanceFinished((TiledThermalField) (Object) this);
    }
}
