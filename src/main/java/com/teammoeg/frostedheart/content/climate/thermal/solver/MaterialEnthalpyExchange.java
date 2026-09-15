/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;

/** Exact pair exchange within each linear sensible/latent energy segment. */
final class MaterialEnthalpyExchange {
    private MaterialEnthalpyExchange() {}

    static void exchange(ThermalCellArena arena, int first, int second,
            double conductanceWPerK, double dtSeconds, double referenceTemperatureC) {
        if (conductanceWPerK == 0 || dtSeconds == 0
                || arena.materialTransitionWaiting(first)
                || arena.materialTransitionWaiting(second)) return;

        double remainingSeconds = dtSeconds;
        // Each endpoint can cross its sensible onset and latent endpoint before
        // waiting for a world ACK. No global nonlinear iteration is involved.
        for (int segment = 0; segment < 6 && remainingSeconds > 0; segment++) {
            double delta = arena.temperatureC(first, referenceTemperatureC)
                    - arena.temperatureC(second, referenceTemperatureC);
            if (delta == 0) return;
            double direction = Math.signum(delta);
            double firstLimit = arena.materialEnergyLimitJ(first, -direction);
            double secondLimit = arena.materialEnergyLimitJ(second, direction);
            double limit = Math.min(firstLimit, secondLimit);
            if (limit == 0) return;

            // Selecting a branch may place an already supercooled state on its
            // plateau. Use the resulting temperatures for the actual exchange.
            delta = arena.temperatureC(first, referenceTemperatureC)
                    - arena.temperatureC(second, referenceTemperatureC);
            if (delta == 0 || Math.signum(delta) != direction) return;
            double slope = arena.energyTemperatureSlope(first) + arena.energyTemperatureSlope(second);
            double requested = slope == 0
                    ? Math.abs(delta) * conductanceWPerK * remainingSeconds
                    : Math.abs(delta) / slope * -Math.expm1(-conductanceWPerK * slope * remainingSeconds);
            double transferred = Math.min(requested, limit);
            if (transferred == 0) return;
            arena.addEnthalpyJ(first, -direction * transferred);
            arena.addEnthalpyJ(second, direction * transferred);
            if (transferred == requested) return;
            double elapsed = slope == 0
                    ? transferred / (conductanceWPerK * Math.abs(delta))
                    : -Math.log1p(-transferred * slope / Math.abs(delta)) / (conductanceWPerK * slope);
            remainingSeconds = Math.max(0, remainingSeconds - elapsed);
        }
    }
}
