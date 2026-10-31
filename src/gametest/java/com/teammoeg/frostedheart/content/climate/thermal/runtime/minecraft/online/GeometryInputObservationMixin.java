/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.online;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.GeometryInputProductionObservation;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FrozenAirGeometry;
import java.util.BitSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FrozenAirGeometry.class, remap = false)
public abstract class GeometryInputObservationMixin {
    @Shadow @Final private ThermalCellArena arena;
    @Inject(method = "retainCells", at = @At("RETURN"))
    private void geometryInput$prepared(BitSet selected, CallbackInfo ci) {
        GeometryInputProductionObservation.prepared((FrozenAirGeometry) (Object) this, arena);
    }
}
