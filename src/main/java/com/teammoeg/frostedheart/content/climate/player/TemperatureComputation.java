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

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.bootstrap.reference.FHDamageSources;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.type.ISlotType;

import java.util.UUID;

public final class TemperatureComputation {
    public static final UUID ENV_TEMP_ATTRIBUTE_UUID = UUID.fromString(
            "95c1eab4-8f3a-4878-aaa7-a86722cdfb07");

    static final double CORE_REFERENCE_TEMPERATURE_C = 37.0D;
    static final double WHOLE_BODY_HEAT_CAPACITY_J_PER_K = 245_000.0D;
    static final double LEGACY_INSULATION_TO_RESISTANCE = 0.0002D;
    private static final double GAMEPLAY_TIME_SCALE = 8.0D;
    private static final double REFERENCE_SKIN_TEMPERATURE_C = 33.0D;
    private static final double BODY_SURFACE_AREA_M2 = 1.8D;
    private static final double TISSUE_RESISTANCE_M2_K_PER_W = 0.04D;
    private static final double LONG_WAVE_COEFFICIENT_W_PER_M2_K = 4.7D;
    private static final double REFERENCE_AIR_VELOCITY_M_PER_S = 0.1D;
    private static final double MAXIMUM_WORLD_WIND_M_PER_S = 19.444D;
    private static final double RADIATION_ABSORPTIVITY = 0.8D;
    private static final double WATER_COEFFICIENT_W_PER_M2_K = 100.0D;
    private static final double LAVA_TEMPERATURE_C = 1_000.0D;
    private static final double LAVA_COEFFICIENT_W_PER_M2_K = 150.0D;
    private static final double POWDER_SNOW_TEMPERATURE_C = -30.0D;
    private static final double POWDER_SNOW_COEFFICIENT_W_PER_M2_K = 25.0D;
    private static final double WET_EXCHANGE_COEFFICIENT_W_PER_M2_K = 12.0D;
    private static final double ON_FIRE_HEAT_POWER_W = 1_200.0D;
    private static final double BASAL_METABOLIC_POWER_W = 90.0D;
    private static final double WALKING_METABOLIC_POWER_W = 110.0D;
    private static final double SPRINTING_METABOLIC_POWER_W = 300.0D;
    private static final double REGULATION_START_DELTA_C = 0.2D;
    private static final double REGULATION_GAIN_W_PER_K = 180.0D;
    private static final double MAXIMUM_REGULATION_POWER_W = 300.0D;
    private static final double EVAPORATION_BASE_W_PER_M2_K_PA = 20.0D;
    private static final double EVAPORATION_WIND_W_PER_M2_K_PA = 12.0D;
    private static final double TORSO_HEAD_CONDUCTANCE_W_PER_K = 4.0D;
    private static final double TORSO_LEGS_CONDUCTANCE_W_PER_K = 3.0D;
    private static final double TORSO_HANDS_CONDUCTANCE_W_PER_K = 4.0D;
    private static final double LEGS_FEET_CONDUCTANCE_W_PER_K = 1.5D;
    private static final double JOULES_PER_EXHAUSTION = 20_000.0D;

    private TemperatureComputation() {
    }

    static double partHeatCapacityJPerK(BodyPart part) {
        return WHOLE_BODY_HEAT_CAPACITY_J_PER_K * part.area;
    }

    public static double bodyEnergyForTemperatureDeltaJ(double deltaC) {
        return deltaC * WHOLE_BODY_HEAT_CAPACITY_J_PER_K;
    }

    static void updatePlayer(
            ServerPlayer player,
            PlayerTemperatureData data,
            int intervalTicks
    ) {
        HeatingDeviceContext context = data.thermalContext();
        ThermalEnvironmentSample thermalSample = context.environmentSample();
        BlockPos samplePosition = BlockPos.containing(
                player.getX(), player.getEyeY(), player.getZ());
        double sampledAirTemperatureC =
                MinecraftThermalInput.gameplayPlayerEnvironment(
                        player,
                        WorldTemperature.naturalAir(
                                player.level(), samplePosition),
                        thermalSample);
        double airTemperatureC = applyEnvironmentAttribute(
                player, sampledAirTemperatureC);
        if (player.hasEffect(FHMobEffects.SAUNA.get())) {
            airTemperatureC = Math.max(airTemperatureC, 80.0D);
        }
        double outdoorWindMPerS = clamp(
                WorldTemperature.wind(player.level()), 0.0D, 100.0D)
                * 0.01D * MAXIMUM_WORLD_WIND_M_PER_S;
        boolean canSeeSky = player.level().canSeeSky(samplePosition);
        double localWindMPerS = canSeeSky ? outdoorWindMPerS : 0.0D;
        double relativeHumidity = clamp(
                WorldClimate.getHumidity(player.level()) / 50.0D,
                0.0D, 1.0D);
        double waterTemperatureC = clamp(
                airTemperatureC, 0.0D, 35.0D);
        double bodyHeight = Math.max(0.01D, player.getBbHeight());
        double waterHeightRatio = clamp(
                player.getFluidHeight(FluidTags.WATER) / bodyHeight,
                0.0D, 1.0D);
        double lavaHeightRatio = clamp(
                player.getFluidHeight(FluidTags.LAVA) / bodyHeight,
                0.0D, 1.0D);
        boolean powderSnow = player.isInPowderSnow;
        boolean onFire = player.isOnFire();
        boolean wet = player.hasEffect(FHMobEffects.WET.get());
        boolean frozen = player.hasEffect(FHMobEffects.INSULATION.get())
                || player.isCreative()
                || player.isSpectator()
                || player.getAbilities().invulnerable;
        double elapsedSeconds = Math.max(0.0D, intervalTicks / 20.0D);
        double physiologicalSeconds = elapsedSeconds * GAMEPLAY_TIME_SCALE
                * Math.max(0.0D, FHConfig.SERVER.CLIMATE.tempSpeed.get());

        LazyOptional<WaterLevelCapability> waterLevel =
                FHCapabilities.PLAYER_WATER_LEVEL.getCapability(player);
        boolean waterAvailable = waterLevel
                .map(value -> value.getWaterLevel() > 0).orElse(false);
        double coreTemperatureC = data.getAbsoluteCoreBodyTemp();
        double regulation = frozen ? 0.0D : data.getDifficulty().heat_unit;
        double shiveringPowerW = player.getFoodData().getFoodLevel() > 0
                ? Math.min(MAXIMUM_REGULATION_POWER_W,
                Math.max(0.0D,
                        CORE_REFERENCE_TEMPERATURE_C
                                - REGULATION_START_DELTA_C
                                - coreTemperatureC)
                        * REGULATION_GAIN_W_PER_K) * regulation
                : 0.0D;
        double sweatingRequestedW = waterAvailable
                ? Math.min(MAXIMUM_REGULATION_POWER_W,
                Math.max(0.0D,
                        coreTemperatureC
                                - CORE_REFERENCE_TEMPERATURE_C
                                - REGULATION_START_DELTA_C)
                        * REGULATION_GAIN_W_PER_K) * regulation
                : 0.0D;

        context.reset(player, physiologicalSeconds, data);

        double positiveWetRequestW = 0.0D;
        double exposedAreaM2 = 0.0D;
        for (BodyPart part : BodyPart.VALUES) {
            preparePart(
                    data,
                    context,
                    part,
                    airTemperatureC,
                    thermalSample.radiantFluxWPerM2(),
                    localWindMPerS,
                    waterTemperatureC,
                    waterHeightRatio,
                    lavaHeightRatio,
                    powderSnow,
                    wet);
            double temperatureC = data.getAbsoluteBodyTempByPart(part);
            positiveWetRequestW += Math.max(0.0D,
                    context.getWetConductanceWPerK(part)
                            * (temperatureC
                            - context.getOperativeTemperatureC(part)));
            exposedAreaM2 += BODY_SURFACE_AREA_M2 * part.area
                    * context.getAirFraction(part);
        }

        if (!frozen) {
            equipmentHeating(player, context);
        }

        double evaporationCapacityW = evaporationCapacityW(
                airTemperatureC,
                relativeHumidity,
                localWindMPerS,
                exposedAreaM2);
        double evaporationRequestW = positiveWetRequestW
                + sweatingRequestedW;
        double evaporationScale = evaporationRequestW > 0.0D
                ? Math.min(1.0D,
                evaporationCapacityW / evaporationRequestW)
                : 0.0D;
        double sweatingAppliedW = sweatingRequestedW * evaporationScale;
        double sharedBodyPowerW = BASAL_METABOLIC_POWER_W
                + movementPowerW(player) + shiveringPowerW
                - sweatingAppliedW;
        double integratedEnergyJ = 0.0D;

        for (BodyPart part : BodyPart.VALUES) {
            double temperatureC = data.getAbsoluteBodyTempByPart(part);
            double wetConductance = context.getWetConductanceWPerK(part);
            if (wetConductance * (temperatureC
                    - context.getOperativeTemperatureC(part)) > 0.0D) {
                wetConductance *= evaporationScale;
            }
            double totalConductance =
                    context.getDryConductanceWPerK(part) + wetConductance;
            double weightedBoundary =
                    context.getWeightedBoundaryWPerK(part)
                            + wetConductance
                            * context.getOperativeTemperatureC(part);
            double activePowerW = sharedBodyPowerW * part.area
                    + context.getPowerW(part);
            if (onFire && lavaHeightRatio <= 0.0D) {
                activePowerW += ON_FIRE_HEAT_POWER_W * part.area
                        * (1.0D - context.getRadiantHeatProof(part));
            }

            double deltaEnergyJ;
            if (totalConductance > 0.0D) {
                double passiveEquilibriumC = weightedBoundary
                        / totalConductance;
                double forcedEquilibriumC = passiveEquilibriumC
                        + activePowerW / totalConductance;
                double capacityJPerK = partHeatCapacityJPerK(part);
                double alpha = -Math.expm1(-totalConductance
                        * physiologicalSeconds / capacityJPerK);
                deltaEnergyJ = capacityJPerK
                        * (forcedEquilibriumC - temperatureC) * alpha;
                data.setFeelTempByPart(
                        part, (float) passiveEquilibriumC);
            } else {
                deltaEnergyJ = activePowerW * physiologicalSeconds;
                data.setFeelTempByPart(part, (float) airTemperatureC);
            }
            if (!frozen) {
                data.addBodyEnergyJ(part, deltaEnergyJ);
                integratedEnergyJ += deltaEnergyJ;
            }
        }

        if (!frozen && physiologicalSeconds > 0.0D) {
            transfer(data, BodyPart.TORSO, BodyPart.HEAD,
                    TORSO_HEAD_CONDUCTANCE_W_PER_K,
                    physiologicalSeconds);
            transfer(data, BodyPart.TORSO, BodyPart.LEGS,
                    TORSO_LEGS_CONDUCTANCE_W_PER_K,
                    physiologicalSeconds);
            transfer(data, BodyPart.TORSO, BodyPart.HANDS,
                    TORSO_HANDS_CONDUCTANCE_W_PER_K,
                    physiologicalSeconds);
            transfer(data, BodyPart.LEGS, BodyPart.FEET,
                    LEGS_FEET_CONDUCTANCE_W_PER_K,
                    physiologicalSeconds);
        }

        double equivalentEnvironmentC = environmentalEquivalentTemperatureC(
                airTemperatureC,
                thermalSample.radiantFluxWPerM2(),
                localWindMPerS,
                waterTemperatureC,
                waterHeightRatio,
                lavaHeightRatio,
                powderSnow,
                onFire);
        byte statusFlags = statusFlags(
                frozen, wet, waterHeightRatio, lavaHeightRatio,
                powderSnow, onFire);
        data.applyThermalObservation(
                equivalentEnvironmentC,
                !frozen && physiologicalSeconds > 0.0D
                        ? integratedEnergyJ / physiologicalSeconds : 0.0D,
                sampledAirTemperatureC,
                thermalSample.radiantFluxWPerM2(),
                outdoorWindMPerS,
                localWindMPerS,
                canSeeSky,
                statusFlags);

        if (!frozen) {
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

    private static double applyEnvironmentAttribute(
            ServerPlayer player,
            double sampledAirTemperatureC
    ) {
        AttributeInstance attribute = player.getAttribute(
                FHAttributes.ENV_TEMPERATURE.get());
        if (attribute == null || !Double.isFinite(sampledAirTemperatureC)) {
            return sampledAirTemperatureC;
        }
        attribute.removeModifier(ENV_TEMP_ATTRIBUTE_UUID);
        attribute.addTransientModifier(new AttributeModifier(
                ENV_TEMP_ATTRIBUTE_UUID,
                "player environment temperature",
                sampledAirTemperatureC,
                AttributeModifier.Operation.ADDITION));
        return player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
    }

    private static void preparePart(
            PlayerTemperatureData data,
            HeatingDeviceContext context,
            BodyPart part,
            double airTemperatureC,
            double radiantFluxWPerM2,
            double localWindMPerS,
            double waterTemperatureC,
            double waterHeightRatio,
            double lavaHeightRatio,
            boolean powderSnow,
            boolean wet
    ) {
        double lava = bandFraction(part, lavaHeightRatio);
        double water = (1.0D - lava)
                * bandFraction(part, waterHeightRatio);
        double powder = (1.0D - lava - water)
                * (powderSnow ? 1.0D : 0.0D);
        double air = 1.0D - lava - water - powder;

        PartClothData clothing = context.clothing();
        data.fillClothDataByPart(context.getPlayer(), part, clothing);
        double hNatural = naturalConvectionCoefficientWPerM2K(
                REFERENCE_SKIN_TEMPERATURE_C, airTemperatureC);
        double hConvection = Math.max(hNatural,
                forcedConvectionCoefficientWPerM2K(
                        localWindMPerS
                                * (1.0D - clothing.windProof)));
        double absorbedFluxWPerM2 = Math.max(0.0D, radiantFluxWPerM2)
                * RADIATION_ABSORPTIVITY
                * (1.0D - clothing.radiantHeatProof);
        double hTotal = hConvection
                + LONG_WAVE_COEFFICIENT_W_PER_M2_K;
        double operativeTemperatureC =
                (hConvection * airTemperatureC
                        + LONG_WAVE_COEFFICIENT_W_PER_M2_K
                        * (airTemperatureC + absorbedFluxWPerM2
                        / LONG_WAVE_COEFFICIENT_W_PER_M2_K))
                        / hTotal;
        double areaM2 = BODY_SURFACE_AREA_M2 * part.area;
        double pathResistance = TISSUE_RESISTANCE_M2_K_PER_W
                + clothing.thermalResistanceM2KPerW;
        double airConductanceWPerK = areaM2 * air
                / (pathResistance + 1.0D / hTotal);
        double contactProtection = 1.0D
                - clothing.waterResistance;
        double waterConductanceWPerK = mediumConductanceWPerK(
                areaM2, water,
                WATER_COEFFICIENT_W_PER_M2_K * contactProtection,
                pathResistance);
        double powderConductanceWPerK = mediumConductanceWPerK(
                areaM2, powder,
                POWDER_SNOW_COEFFICIENT_W_PER_M2_K
                        * contactProtection,
                pathResistance);
        double lavaConductanceWPerK = mediumConductanceWPerK(
                areaM2, lava,
                LAVA_COEFFICIENT_W_PER_M2_K
                        * (1.0D - clothing.radiantHeatProof),
                pathResistance);
        double wetConductanceWPerK = wet
                ? areaM2 * air * WET_EXCHANGE_COEFFICIENT_W_PER_M2_K
                * contactProtection : 0.0D;
        context.setPartEnvironment(
                part,
                airConductanceWPerK + waterConductanceWPerK
                        + powderConductanceWPerK + lavaConductanceWPerK,
                airConductanceWPerK * operativeTemperatureC
                        + waterConductanceWPerK * waterTemperatureC
                        + powderConductanceWPerK
                        * POWDER_SNOW_TEMPERATURE_C
                        + lavaConductanceWPerK * LAVA_TEMPERATURE_C,
                wetConductanceWPerK,
                operativeTemperatureC,
                clothing.radiantHeatProof,
                air);
    }

    private static double environmentalEquivalentTemperatureC(
            double airTemperatureC,
            double radiantFluxWPerM2,
            double localWindMPerS,
            double waterTemperatureC,
            double waterHeightRatio,
            double lavaHeightRatio,
            boolean powderSnow,
            boolean onFire
    ) {
        double hNatural = naturalConvectionCoefficientWPerM2K(
                REFERENCE_SKIN_TEMPERATURE_C, airTemperatureC);
        double hConvection = Math.max(hNatural,
                forcedConvectionCoefficientWPerM2K(localWindMPerS));
        double airLossFlux = (hConvection
                + LONG_WAVE_COEFFICIENT_W_PER_M2_K)
                * (REFERENCE_SKIN_TEMPERATURE_C - airTemperatureC)
                - RADIATION_ABSORPTIVITY
                * Math.max(0.0D, radiantFluxWPerM2);
        double lossFluxWPerM2 = 0.0D;
        for (BodyPart part : BodyPart.VALUES) {
            double lava = bandFraction(part, lavaHeightRatio);
            double water = (1.0D - lava)
                    * bandFraction(part, waterHeightRatio);
            double powder = (1.0D - lava - water)
                    * (powderSnow ? 1.0D : 0.0D);
            double air = 1.0D - lava - water - powder;
            lossFluxWPerM2 += part.area * (
                    air * airLossFlux
                            + water * WATER_COEFFICIENT_W_PER_M2_K
                            * (REFERENCE_SKIN_TEMPERATURE_C
                            - waterTemperatureC)
                            + lava * LAVA_COEFFICIENT_W_PER_M2_K
                            * (REFERENCE_SKIN_TEMPERATURE_C
                            - LAVA_TEMPERATURE_C)
                            + powder
                            * POWDER_SNOW_COEFFICIENT_W_PER_M2_K
                            * (REFERENCE_SKIN_TEMPERATURE_C
                            - POWDER_SNOW_TEMPERATURE_C));
        }
        if (onFire && lavaHeightRatio <= 0.0D) {
            lossFluxWPerM2 -= ON_FIRE_HEAT_POWER_W
                    / BODY_SURFACE_AREA_M2;
        }
        double hReference = Math.max(hNatural,
                forcedConvectionCoefficientWPerM2K(
                        REFERENCE_AIR_VELOCITY_M_PER_S))
                + LONG_WAVE_COEFFICIENT_W_PER_M2_K;
        return REFERENCE_SKIN_TEMPERATURE_C
                - lossFluxWPerM2 / hReference;
    }

    private static double movementPowerW(ServerPlayer player) {
        if (player.getVehicle() != null) return 0.0D;
        if (player.isSprinting()) return SPRINTING_METABOLIC_POWER_W;
        return player.getDeltaMovement().horizontalDistanceSqr() > 0.001D
                ? WALKING_METABOLIC_POWER_W : 0.0D;
    }

    private static void transfer(
            PlayerTemperatureData data,
            BodyPart first,
            BodyPart second,
            double conductanceWPerK,
            double physiologicalSeconds
    ) {
        double firstTemperatureC = data.getAbsoluteBodyTempByPart(first);
        double secondTemperatureC = data.getAbsoluteBodyTempByPart(second);
        double differenceK = firstTemperatureC - secondTemperatureC;
        double requestedJ = conductanceWPerK * differenceK
                * physiologicalSeconds;
        double firstCapacity = partHeatCapacityJPerK(first);
        double secondCapacity = partHeatCapacityJPerK(second);
        double equalizingJ = differenceK
                / (1.0D / firstCapacity + 1.0D / secondCapacity);
        double transferJ = Math.copySign(
                Math.min(Math.abs(requestedJ), Math.abs(equalizingJ)),
                requestedJ);
        data.addBodyEnergyJ(first, -transferJ);
        data.addBodyEnergyJ(second, transferJ);
    }

    private static double mediumConductanceWPerK(
            double areaM2,
            double fraction,
            double coefficientWPerM2K,
            double pathResistanceM2KPerW
    ) {
        if (!(fraction > 0.0D) || !(coefficientWPerM2K > 0.0D)) {
            return 0.0D;
        }
        return areaM2 * fraction
                / (pathResistanceM2KPerW
                + 1.0D / coefficientWPerM2K);
    }

    private static double bandFraction(BodyPart part, double heightRatio) {
        return clamp((heightRatio - part.immersionLower)
                        / (part.immersionUpper - part.immersionLower),
                0.0D, 1.0D);
    }

    private static double evaporationCapacityW(
            double airTemperatureC,
            double relativeHumidity,
            double localWindMPerS,
            double exposedAreaM2
    ) {
        double vaporPressureDifferenceKPa = Math.max(0.0D,
                saturationVaporPressureKPa(
                        REFERENCE_SKIN_TEMPERATURE_C)
                        - clamp(relativeHumidity, 0.0D, 1.0D)
                        * saturationVaporPressureKPa(airTemperatureC));
        double coefficientWPerM2KPa = EVAPORATION_BASE_W_PER_M2_K_PA
                + EVAPORATION_WIND_W_PER_M2_K_PA
                * Math.sqrt(Math.max(0.0D, localWindMPerS));
        return exposedAreaM2 * coefficientWPerM2KPa
                * vaporPressureDifferenceKPa;
    }

    private static double saturationVaporPressureKPa(double temperatureC) {
        double boundedC = clamp(temperatureC, -100.0D, 100.0D);
        return 0.61078D * Math.exp(
                17.2694D * boundedC / (boundedC + 237.29D));
    }

    private static double naturalConvectionCoefficientWPerM2K(
            double skinTemperatureC,
            double airTemperatureC
    ) {
        return 2.38D * Math.pow(
                Math.abs(skinTemperatureC - airTemperatureC), 0.25D);
    }

    private static double forcedConvectionCoefficientWPerM2K(
            double velocityMPerS
    ) {
        return 12.1D * Math.sqrt(Math.max(0.0D, velocityMPerS));
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static byte statusFlags(
            boolean frozen,
            boolean wet,
            double waterHeightRatio,
            double lavaHeightRatio,
            boolean powderSnow,
            boolean onFire
    ) {
        int flags = frozen ? 1 : 0;
        if (wet) flags |= 1 << 1;
        if (waterHeightRatio > 0.0D) flags |= 1 << 2;
        if (lavaHeightRatio > 0.0D) flags |= 1 << 3;
        if (powderSnow) flags |= 1 << 4;
        if (onFire) flags |= 1 << 5;
        return (byte) flags;
    }

    private static void equipmentHeating(
            ServerPlayer player,
            HeatingDeviceContext context
    ) {
        if (CompatModule.isCuriosLoaded()) {
            for (Pair<ISlotType, ItemStack> entry
                    : CuriosCompat.getAllCuriosAndSlotsIfVisible(player)) {
                ItemStack stack = entry.getSecond();
                BodyHeatingCapability heating = FHCapabilities
                        .EQUIPMENT_HEATING.getCapability(stack).orElse(null);
                if (heating != null) {
                    heating.tickHeating(
                            context.curiosSlot(entry.getFirst()),
                            stack,
                            context);
                }
            }
        }
        for (EquipmentSlot equipmentSlot : HeatingDeviceSlot.EQUIPMENT_SLOTS) {
            ItemStack stack = player.getItemBySlot(equipmentSlot);
            BodyHeatingCapability heating = FHCapabilities
                    .EQUIPMENT_HEATING.getCapability(stack).orElse(null);
            if (heating != null) {
                heating.tickHeating(
                        HeatingDeviceSlot.vanilla(equipmentSlot),
                        stack,
                        context);
            }
        }
    }

    protected static void burning(
            ServerPlayer player,
            PlayerTemperatureData data
    ) {
        if (player.isOnFire() || player.isInLava()) return;
        RandomSource random = player.getRandom();
        float hottest = data.getHighestFeelTemp();
        if (hottest > 250.0F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 4.0F);
        } else if (hottest > 200.0F && random.nextFloat() < 0.75F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 3.0F);
        } else if (hottest > 150.0F && random.nextFloat() < 0.5F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 2.0F);
        } else if (hottest > 100.0F && random.nextFloat() < 0.25F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 1.0F);
        }

        float coldest = data.getLowestFeelTemp();
        if (coldest < -250.0F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 4.0F);
        } else if (coldest < -200.0F && random.nextFloat() < 0.75F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 3.0F);
        } else if (coldest < -150.0F && random.nextFloat() < 0.5F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 2.0F);
        } else if (coldest < -100.0F && random.nextFloat() < 0.25F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 1.0F);
        }
    }

    // Existing compatibility probe only. Gameplay radiation uses updatePlayer.
    static float radiantBodyTemperatureDelta(
            double radiantFluxWPerM2,
            int updateIntervalTicks
    ) {
        if (!(radiantFluxWPerM2 > 0.0D) || updateIntervalTicks <= 0) {
            return 0.0F;
        }
        return (float) (radiantFluxWPerM2 * 0.7D * 0.8D
                * (updateIntervalTicks / 20.0D) / 5_000.0D);
    }

    static float radiantFeelingTemperatureDelta(double radiantFluxWPerM2) {
        return radiantFluxWPerM2 > 0.0D
                ? (float) (radiantFluxWPerM2 * 0.8D / 6.0D) : 0.0F;
    }

    public static double feelTemperature(
            double dryTemperatureC,
            double relativeHumidity,
            double relativeWindSpeed
    ) {
        double vaporPressure = relativeHumidity * 6.105D * Math.exp(
                17.27D * dryTemperatureC
                        / (237.7D + dryTemperatureC));
        return dryTemperatureC + 0.33D * vaporPressure
                - 0.7D * relativeWindSpeed * 35.0D - 4.0D;
    }
}
