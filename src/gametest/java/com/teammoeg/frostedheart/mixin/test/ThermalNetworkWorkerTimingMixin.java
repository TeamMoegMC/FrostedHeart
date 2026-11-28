/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.test;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalNetworkWorkerTiming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ThermalWorkerPool.class, remap = false)
public abstract class ThermalNetworkWorkerTimingMixin {
    @Redirect(method = "workerLoop", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"), remap = false)
    private void thermalLoad$measureChargedTask(Runnable task) { ThermalNetworkWorkerTiming.task(task); }
}
