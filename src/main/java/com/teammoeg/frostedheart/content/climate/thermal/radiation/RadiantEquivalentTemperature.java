/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.radiation;

/**
 * Shared conversion from absorbed radiant flux to an equivalent air-temperature delta.
 *
 * <p>The value deliberately uses the player-facing absorption and transfer constants:
 * {@code deltaT = q * 0.8 / 6.0}. It is an equivalent boundary temperature, not the
 * player's body-energy integration formula.</p>
 */
public final class RadiantEquivalentTemperature {
    public static final double PLAYER_RADIATION_ABSORPTIVITY = 0.8D;
    public static final double PLAYER_RADIATION_TRANSFER_W_PER_M2_K = 6.0D;

    private RadiantEquivalentTemperature() {
    }

    /**
     * Converts non-negative radiant flux in {@code W/m2} to an equivalent {@code degC}
     * delta. Non-finite and non-positive flux has no radiant contribution.
     */
    public static double deltaC(double radiantFluxWPerM2) {
        if (!(radiantFluxWPerM2 > 0.0D) || !Double.isFinite(radiantFluxWPerM2)) {
            return 0.0D;
        }
        double delta = radiantFluxWPerM2
                * PLAYER_RADIATION_ABSORPTIVITY
                / PLAYER_RADIATION_TRANSFER_W_PER_M2_K;
        return Double.isFinite(delta) ? delta : Double.MAX_VALUE;
    }

    /**
     * Adds the equivalent radiant delta to absolute air temperature in {@code degC}.
     * Invalid air samples use a neutral {@code 0 degC} fallback so the pure model never
     * emits a non-finite target; runtime sampling remains responsible for rejecting a
     * missing environment before it writes ItemStack state.
     */
    public static double effectiveEnvironmentTemperatureC(
            double airTemperatureC,
            double radiantFluxWPerM2
    ) {
        double air = Double.isFinite(airTemperatureC) ? airTemperatureC : 0.0D;
        double result = air + deltaC(radiantFluxWPerM2);
        if (Double.isFinite(result)) {
            return result;
        }
        return result < 0.0D ? -Double.MAX_VALUE : Double.MAX_VALUE;
    }
}
