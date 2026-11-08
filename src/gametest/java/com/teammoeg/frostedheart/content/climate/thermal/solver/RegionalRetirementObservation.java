/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import net.minecraft.core.BlockPos;

/** Test-only regional evidence for source-free retirement; never changes solver state. */
public final class RegionalRetirementObservation {
    private static volatile boolean enabled;
    private static long lastTick;
    private static volatile String latest = "not_observed";

    private RegionalRetirementObservation() { }

    public static void begin() { lastTick = -200; latest = "waiting"; enabled = true; }
    public static void end() { enabled = false; }
    public static String summary() { return latest; }

    static void observe(RegionalAirRuntime runtime, ThermalCellArena arena, long tick) {
        if (!enabled || tick < lastTick + 200) return;
        lastTick = tick;
        for (var region : runtime.observedRegions()) {
            var field = region.accepted().field();
            if (field == null) continue;
            int airActive = 0, materialActive = 0, latent = 0, waiting = 0, missingBackground = 0;
            double maxAir = 0, maxMaterial = 0, worstH = 0, worstNatural = 0, worstTemperature = 0;
            long worstPosition = 0;
            for (int slot : region.scope.cells) {
                int page = arena.pageSlot(slot);
                if (!region.environmentNaturalC.containsKey(page)) { missingBackground++; continue; }
                double natural = region.environmentNaturalC.get(page);
                boolean active = field.activeAtSlot(slot, natural, .5);
                if (!arena.isMaterialCell(slot)) {
                    if (active) airActive++;
                    var component = field.airLayout().componentForSlot(slot);
                    if (component == null) continue;
                    long mask = component.airBlocks;
                    while (mask != 0) {
                        int block = Long.numberOfTrailingZeros(mask); mask &= mask - 1;
                        for (int corner = 0; corner < 8; corner++)
                            maxAir = Math.max(maxAir, Math.abs(field.vertexPerturbationC(component, block, corner, natural)));
                    }
                } else {
                    if (active) materialActive++;
                    double temperature = field.materialTemperatureAtSlotC(slot), residual = Math.abs(temperature - natural);
                    if (residual > maxMaterial) {
                        maxMaterial = residual; worstNatural = natural; worstTemperature = temperature;
                        worstPosition = BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
                    }
                }
            }
            for (int body = 0; body < field.materialCount(); body++) {
                var transition = field.materialLaw(body).transition(field.materialBranch(body));
                if (transition != null && transition.progress(field.materialEnthalpyJ(body)) > 0) latent++;
                if (field.materialWaiting(body)) waiting++;
                worstH = Math.max(worstH, Math.abs(field.materialEnthalpyJ(body)));
            }
            latest = "tick=" + tick + ",region=" + region.id + ",sampleTick=" + field.sampleTick()
                    + ",minimum=" + field.minimumQuality() + ",cells=" + region.scope.cells.length
                    + ",activeAirCells=" + airActive + ",activeMaterials=" + materialActive
                    + ",latentBodies=" + latent + ",waitingBodies=" + waiting + ",missingBackground=" + missingBackground
                    + ",maxAirPerturbationC=" + maxAir + ",maxMaterialResidualC=" + maxMaterial
                    + ",worstMaterial=" + BlockPos.of(worstPosition) + ",worstMaterialC=" + worstTemperature
                    + ",worstMaterialNaturalC=" + worstNatural + ",maxAbsMaterialH=" + worstH;
            FHMain.LOGGER.info("THERMAL_RETIREMENT_REGION,{}", latest);
        }
    }
}
