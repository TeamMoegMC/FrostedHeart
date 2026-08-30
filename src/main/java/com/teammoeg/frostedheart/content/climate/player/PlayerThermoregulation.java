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
    private static final double BASAL_METABOLIC_POWER_W = 90.0D;
    private static final double WALKING_METABOLIC_POWER_W = 110.0D;
    private static final double SPRINTING_METABOLIC_POWER_W = 300.0D;
    private static final double REGULATION_START_DELTA_C = 0.2D;
    private static final double REGULATION_GAIN_W_PER_K = 180.0D;
    private static final double MAXIMUM_REGULATION_POWER_W = 300.0D;
    private static final double JOULES_PER_EXHAUSTION = 20_000.0D;

    private PlayerThermoregulation() {
    }

    static double shiveringPowerW(double coreTemperatureC, double regulationMultiplier, boolean foodAvailable) {
        if (!foodAvailable) return 0.0D;
        return regulationPowerW(
                PlayerThermalModel.CORE_REFERENCE_TEMPERATURE_C
                        - REGULATION_START_DELTA_C
                        - coreTemperatureC,
                regulationMultiplier);
    }

    static double sweatingPowerW(double coreTemperatureC, double regulationMultiplier, boolean waterAvailable) {
        if (!waterAvailable) return 0.0D;
        return regulationPowerW(
                coreTemperatureC
                        - PlayerThermalModel.CORE_REFERENCE_TEMPERATURE_C
                        - REGULATION_START_DELTA_C,
                regulationMultiplier);
    }

    private static double regulationPowerW(double temperatureErrorK, double regulationMultiplier) {
        return Math.min(MAXIMUM_REGULATION_POWER_W,
                Math.max(0.0D, temperatureErrorK)
                        * REGULATION_GAIN_W_PER_K)
                * regulationMultiplier;
    }

    static double sharedBodyPowerW(ServerPlayer player, double shiveringPowerW, double sweatingAppliedW) {
        return BASAL_METABOLIC_POWER_W
                + movementPowerW(player)
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
