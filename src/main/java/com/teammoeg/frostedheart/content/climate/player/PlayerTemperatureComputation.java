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

import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.util.LazyOptional;

import java.util.UUID;

/**
 * Coordinates one complete player-temperature update.
 * <p>
 * Domain ownership is explicit:
 * {@link PlayerThermalEnvironment} samples inputs,
 * {@link PlayerEquipmentHeating} contributes equipment power,
 * {@link PlayerThermoregulation} owns metabolism and resource costs,
 * {@link PlayerThermalModel} owns heat-balance formulas, and
 * {@link PlayerThermalInjury} owns direct climate injury.
 * <p>
 * Values remain method-local primitives. The update allocates no carrier
 * object, collection, or second player state.
 */
public final class PlayerTemperatureComputation {
    public static final UUID ENV_TEMP_ATTRIBUTE_UUID = UUID.fromString("95c1eab4-8f3a-4878-aaa7-a86722cdfb07");

    // Existing package-facing anchors retained for player state and clothing.
    static final double CORE_REFERENCE_TEMPERATURE_C =
            PlayerThermalModel.CORE_REFERENCE_TEMPERATURE_C;
    static final double WHOLE_BODY_HEAT_CAPACITY_J_PER_K =
            PlayerThermalModel.WHOLE_BODY_HEAT_CAPACITY_J_PER_K;
    static final double LEGACY_INSULATION_TO_RESISTANCE =
            PlayerThermalModel.LEGACY_INSULATION_TO_RESISTANCE;

    private PlayerTemperatureComputation() {
    }

    static double partHeatCapacityJPerK(BodyPart part) {
        return PlayerThermalModel.partHeatCapacityJPerK(part);
    }

    public static double bodyEnergyForTemperatureDeltaJ(double deltaC) {
        return PlayerThermalModel.bodyEnergyForTemperatureDeltaJ(deltaC);
    }

    /** Runs one complete server-side thermal step for a player. */
    public static void updatePlayer(ServerPlayer player, PlayerTemperatureData data, int intervalTicks) {
        HeatingDeviceContext context = data.thermalContext();
        ThermalEnvironmentSample sample = context.environmentSample();

        // 1. Sample physical Air and entity-local environmental inputs once.
        BlockPos samplePosition = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        double sampledAirTemperatureC = PlayerThermalEnvironment.sampleAirTemperatureC(
                player, samplePosition, sample);
        double airTemperatureC = PlayerThermalEnvironment.localAirTemperatureC(
                player, sampledAirTemperatureC);

        // canSeeSky is the initial low-cost gate between outdoor and indoor wind.
        double outdoorWindMPerS = PlayerThermalEnvironment.outdoorWindMPerS(player);
        boolean canSeeSky = player.level().canSeeSky(samplePosition);
        double localWindMPerS = canSeeSky ? outdoorWindMPerS : 0.0D;
        double relativeHumidity = PlayerThermalEnvironment.relativeHumidity(player);

        // 2. Read contact media and establish this integration interval.
        double bodyHeight = Math.max(0.01D, player.getBbHeight());
        double waterHeightRatio = PlayerThermalEnvironment.fluidHeightRatio(
                player, FluidTags.WATER, bodyHeight);
        double lavaHeightRatio = PlayerThermalEnvironment.fluidHeightRatio(
                player, FluidTags.LAVA, bodyHeight);
        double waterTemperatureC = PlayerThermalModel.waterTemperatureC(airTemperatureC);

        boolean powderSnow = player.isInPowderSnow;
        boolean onFire = player.isOnFire();
        boolean wet = player.hasEffect(FHMobEffects.WET.get());
        boolean frozen = PlayerThermalEnvironment.isBodyFrozen(player);

        // Convert real elapsed time into the gameplay-accelerated body time step.
        double elapsedSeconds = Math.max(0.0D, intervalTicks / 20.0D);
        double physiologicalSeconds = PlayerThermalModel.physiologicalSeconds(
                elapsedSeconds, FHConfig.SERVER.CLIMATE.tempSpeed.get());
        context.reset(player, elapsedSeconds, physiologicalSeconds, data);

        // 3. Build all five passive air/contact paths before active power.
        // Wet and sweat later share the same environmental evaporation limit.
        double wetCoolingRequestW = 0.0D;
        double exposedAreaM2 = 0.0D;

        for (BodyPart part : BodyPart.VALUES) {
            PlayerThermalModel.preparePart(data, context, part,
                    airTemperatureC, sample.radiantFluxWPerM2(), localWindMPerS,
                    waterTemperatureC, waterHeightRatio, lavaHeightRatio, powderSnow, wet);

            wetCoolingRequestW += Math.max(0.0D,
                    context.getWetConductanceWPerK(part)
                            * (context.getBodyTemperatureC(part) - context.getOperativeTemperatureC(part)));
            exposedAreaM2 += PlayerThermalModel.exposedAreaM2(part, context.getAirFraction(part));
        }

        // 4. Collect regulation, movement, and equipment as active watts.
        LazyOptional<WaterLevelCapability> waterLevel =
                FHCapabilities.PLAYER_WATER_LEVEL.getCapability(player);
        boolean waterAvailable = waterLevel.map(value -> value.getWaterLevel() > 0).orElse(false);

        // Frozen players still receive observations but skip body regulation and heating.
        double regulationMultiplier = frozen ? 0.0D : data.getDifficulty().heat_unit;
        double coreTemperatureC = data.getAbsoluteCoreBodyTemp();
        double shiveringPowerW = PlayerThermoregulation.shiveringPowerW(
                coreTemperatureC, regulationMultiplier, player.getFoodData().getFoodLevel() > 0);
        double sweatingRequestedW = PlayerThermoregulation.sweatingPowerW(
                coreTemperatureC, regulationMultiplier, waterAvailable);

        if (!frozen && physiologicalSeconds > 0.0D) {
            PlayerEquipmentHeating.collect(player, context);
        }

        // Scale both Wet cooling and sweating against one evaporation ceiling.
        double evaporationScale = PlayerThermalModel.evaporationScale(
                airTemperatureC, relativeHumidity, localWindMPerS,
                exposedAreaM2, wetCoolingRequestW + sweatingRequestedW);
        double sweatingAppliedW = sweatingRequestedW * evaporationScale;
        double sharedBodyPowerW = PlayerThermoregulation.sharedBodyPowerW(
                player, shiveringPowerW, sweatingAppliedW);

        // Integrate passive exchange, active power, and internal body transfer.
        double integratedEnergyJ = PlayerThermalModel.integrateBody(data, context,
                airTemperatureC, onFire, lavaHeightRatio, sharedBodyPowerW,
                evaporationScale, physiologicalSeconds, frozen);

        // 5. Publish observations and charge only actual regulation work.
        double environmentTemperatureC = PlayerThermalModel.environmentalEquivalentTemperatureC(
                airTemperatureC, sample.radiantFluxWPerM2(), localWindMPerS,
                waterTemperatureC, waterHeightRatio, lavaHeightRatio, powderSnow, onFire);
        double averageBodyPowerW = !frozen && physiologicalSeconds > 0.0D
                ? integratedEnergyJ / physiologicalSeconds : 0.0D;
        data.applyThermalObservation(environmentTemperatureC, averageBodyPowerW,
                sampledAirTemperatureC, sample.radiantFluxWPerM2(),
                outdoorWindMPerS, localWindMPerS, canSeeSky);

        // Resource costs follow applied regulation, not the original request.
        PlayerThermoregulation.consumeResources(
                player, waterLevel, frozen, physiologicalSeconds, shiveringPowerW, sweatingAppliedW);
    }

    /** Applies direct hot or cold injury on the existing injury cadence. */
    public static void burning(ServerPlayer player, PlayerTemperatureData data) {
        PlayerThermalInjury.apply(player, data);
    }

    /*public static double feelTemperature(double dryTemperatureC, double relativeHumidity, double relativeWindSpeed) {
        return PlayerThermalModel.feelTemperature(dryTemperatureC, relativeHumidity, relativeWindSpeed);
    }*/
}
