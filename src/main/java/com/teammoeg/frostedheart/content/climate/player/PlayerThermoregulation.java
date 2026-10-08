/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;

/** Owns metabolism, shivering, sweating, and their food/water costs. */
public final class PlayerThermoregulation {
    private static final double BASAL_METABOLIC_POWER_W = 30.625D;
    private static final double WALKING_METABOLIC_POWER_W = 30.625D;
    private static final double SPRINTING_METABOLIC_POWER_W = 0.0D;
    private static final double MILD_COLD_POWER_W = 61.25D;
    private static final double MODERATE_COLD_POWER_W = 91.875D;
    private static final double SEVERE_COLD_POWER_W = 122.5D;
    private static final double MILD_HEAT_COOLING_W = 30.625D;
    private static final double SEVERE_HEAT_COOLING_W = 61.25D;
    private static final double JOULES_PER_EXHAUSTION = 5_444.444D;

    private PlayerThermoregulation() {
    }

    static double shiveringPowerW(double coreTemperatureC, double regulationMultiplier, boolean foodAvailable) {
        if (!foodAvailable) return 0.0D;
        double belowNormalC = PlayerThermalModel.CORE_REFERENCE_TEMPERATURE_C - coreTemperatureC;
        if (belowNormalC > 1.0D) return SEVERE_COLD_POWER_W * regulationMultiplier;
        if (belowNormalC > 0.5D) return MODERATE_COLD_POWER_W * regulationMultiplier;
        return belowNormalC > 0.1D ? MILD_COLD_POWER_W * regulationMultiplier : 0.0D;
    }

    static double sweatingPowerW(double coreTemperatureC, double regulationMultiplier, boolean waterAvailable) {
        if (!waterAvailable) return 0.0D;
        double aboveNormalC = coreTemperatureC - PlayerThermalModel.CORE_REFERENCE_TEMPERATURE_C;
        if (aboveNormalC > 0.5D) return SEVERE_HEAT_COOLING_W * regulationMultiplier;
        return aboveNormalC > 0.1D ? MILD_HEAT_COOLING_W * regulationMultiplier : 0.0D;
    }

    static double sharedBodyPowerW(ServerPlayer player, double difficultyMultiplier,
                                   double shiveringPowerW, double sweatingAppliedW) {
        return (BASAL_METABOLIC_POWER_W + movementPowerW(player)) * difficultyMultiplier
                + shiveringPowerW
                - sweatingAppliedW;
    }

    private static double movementPowerW(ServerPlayer player) {
        if (player.getVehicle() != null) return 0.0D;
        if (player.isSprinting()) return SPRINTING_METABOLIC_POWER_W;
        return player.getDeltaMovement().horizontalDistanceSqr() > 0.001D
                ? WALKING_METABOLIC_POWER_W : 0.0D;
    }

    static void consumeResources(ServerPlayer player, LazyOptional<WaterLevelCapability> waterLevel, boolean frozen,
                                 double physiologicalSeconds, double shiveringPowerW, double sweatingAppliedW) {
        if (frozen) return;
        if (shiveringPowerW > 0.0D) {
            player.causeFoodExhaustion((float) (
                    shiveringPowerW * physiologicalSeconds
                            / JOULES_PER_EXHAUSTION));
        }
        if (sweatingAppliedW > 0.0D) {
            float exhaustion = (float) (
                    sweatingAppliedW * physiologicalSeconds
                            / JOULES_PER_EXHAUSTION);
            waterLevel.ifPresent(value ->
                    value.addExhaustion(player, exhaustion));
        }
    }
}
