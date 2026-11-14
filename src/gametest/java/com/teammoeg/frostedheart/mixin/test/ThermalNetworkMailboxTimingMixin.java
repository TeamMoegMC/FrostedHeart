/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.test;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalNetworkWorkerTiming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ThermalDimensionMailbox.class, remap = false)
public abstract class ThermalNetworkMailboxTimingMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lcom/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/engine/ThermalDimensionEngine;processWorkRound()V"), remap = false)
    private void thermalLoad$measureEngineRound(ThermalDimensionEngine engine) { ThermalNetworkWorkerTiming.engine(engine); }
}
