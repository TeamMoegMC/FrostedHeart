package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

/**
 * Stateless formulas for the existing five-part body model.
 * <p>
 * Methods are ordered by the runtime calculation: shared conversions,
 * per-part passive exchange, evaporation, body integration, environmental
 * equivalent temperature, and compatibility helpers. Parameters sit next
 */
public class PlayerThermalModel {
    static final double CORE_REFERENCE_TEMPERATURE_C = 37.0D;
    static final double WHOLE_BODY_HEAT_CAPACITY_J_PER_K = 245_000.0D;
    static final double LEGACY_INSULATION_TO_RESISTANCE = 0.0002D;
    static final double GAMEPLAY_TIME_SCALE = 8.0D;
    static final double MAXIMUM_WORLD_WIND_M_PER_S = 19.444D;

    private PlayerThermalModel() {
    }

    static double partHeatCapacityJPerK(BodyPart part) {
        return WHOLE_BODY_HEAT_CAPACITY_J_PER_K * part.area;
    }

    static double bodyEnergyForTemperatureDeltaJ(double deltaC) {
        return deltaC * WHOLE_BODY_HEAT_CAPACITY_J_PER_K;
    }

    static double physiologicalSeconds(double elapsedSeconds, double temperatureSpeed) {
        return Math.max(0.0D, elapsedSeconds)
                * GAMEPLAY_TIME_SCALE
                * Math.max(0.0D, temperatureSpeed);
    }

    static double outdoorWindMPerS(double worldWind) {
        return clamp(worldWind, 0.0D, 100.0D)
                * 0.01D * MAXIMUM_WORLD_WIND_M_PER_S;
    }

    static double waterTemperatureC(double airTemperatureC) {
        return clamp(airTemperatureC, 0.0D, 35.0D);
    }

    /*
     * Passive environmental exchange parameters.
     * Temperatures are C; transfer coefficients are W/(m2*K).
     */
    private static final double REFERENCE_SKIN_TEMPERATURE_C = 33.0D;
    private static final double BODY_SURFACE_AREA_M2 = 1.8D;
    private static final double LONG_WAVE_COEFFICIENT_W_PER_M2_K = 4.7D;
    private static final double RADIATION_ABSORPTIVITY = 0.8D;
    private static final double WATER_COEFFICIENT_W_PER_M2_K = 100.0D;
    private static final double LAVA_TEMPERATURE_C = 1_000.0D;
    private static final double LAVA_COEFFICIENT_W_PER_M2_K = 150.0D;
    private static final double POWDER_SNOW_TEMPERATURE_C = -30.0D;
    private static final double POWDER_SNOW_COEFFICIENT_W_PER_M2_K = 25.0D;

    /**
     * Builds passive conductance paths for one body part.
     * <p>
     * Contact precedence is lava, water, powder snow, then remaining air.
     * Clothing thermal resistance affects every path; wind proof affects
     * forced convection, water resistance affects contact and Wet, and
     * radiant proof affects absorbed source/fire heat.
     */
    static void preparePart(PlayerTemperatureData data, HeatingDeviceContext context, BodyPart part,
                            double airTemperatureC, double radiantFluxWPerM2, double localWindMPerS,
                            double waterTemperatureC, double waterHeightRatio, double lavaHeightRatio,
                            boolean powderSnow, boolean wet) {
        // Fractions are exclusive and sum to one for this body part.
        double lava = bandFraction(part, lavaHeightRatio);
        double water = (1.0D - lava)
                * bandFraction(part, waterHeightRatio);
        double powder = (1.0D - lava - water)
                * (powderSnow ? 1.0D : 0.0D);
        double air = 1.0D - lava - water - powder;

        PartClothData clothing = context.clothing();
        data.fillClothDataByPart(context.getPlayer(), part, clothing);
        double hConvection = convectionCoefficientWPerM2K(
                airTemperatureC, localWindMPerS, clothing.windProof);
        double absorbedFluxWPerM2 = absorbedRadiantFluxWPerM2(
                radiantFluxWPerM2, clothing.radiantHeatProof);
        double hTotal = hConvection
                + LONG_WAVE_COEFFICIENT_W_PER_M2_K;
        double operativeTemperatureC = operativeTemperatureC(
                airTemperatureC, hConvection, hTotal, absorbedFluxWPerM2);
        double areaM2 = BODY_SURFACE_AREA_M2 * part.area;
        final double tissueResistanceM2KPerW = 0.04D;
        double pathResistance = tissueResistanceM2KPerW
                + clothing.thermalResistanceM2KPerW;
        double airConductanceWPerK = areaM2 * air
                / (pathResistance + 1.0D / hTotal);
        double contactProtection = 1.0D - clothing.waterResistance;
        double waterConductanceWPerK = mediumConductanceWPerK(
                areaM2, water, WATER_COEFFICIENT_W_PER_M2_K * contactProtection, pathResistance);
        double powderConductanceWPerK = mediumConductanceWPerK(
                areaM2, powder, POWDER_SNOW_COEFFICIENT_W_PER_M2_K * contactProtection, pathResistance);
        double lavaConductanceWPerK = mediumConductanceWPerK(
                areaM2, lava, LAVA_COEFFICIENT_W_PER_M2_K * (1.0D - clothing.radiantHeatProof), pathResistance);
        final double wetExchangeCoefficientWPerM2K = 12.0D;
        double wetConductanceWPerK = wet
                ? areaM2 * air * wetExchangeCoefficientWPerM2K
                * contactProtection : 0.0D;
        // Copy primitive results into the reusable context for integration.
        context.setPartEnvironment(part,
                airConductanceWPerK + waterConductanceWPerK
                        + powderConductanceWPerK
                        + lavaConductanceWPerK,
                airConductanceWPerK * operativeTemperatureC
                        + waterConductanceWPerK * waterTemperatureC
                        + powderConductanceWPerK
                        * POWDER_SNOW_TEMPERATURE_C
                        + lavaConductanceWPerK * LAVA_TEMPERATURE_C,
                wetConductanceWPerK, operativeTemperatureC, clothing.radiantHeatProof, air);
    }

    static double exposedAreaM2(BodyPart part, double airFraction) {
        return BODY_SURFACE_AREA_M2 * part.area * airFraction;
    }

    private static double convectionCoefficientWPerM2K(
            double airTemperatureC, double localWindMPerS, double windProof) {
        double hNatural = naturalConvectionCoefficientWPerM2K(REFERENCE_SKIN_TEMPERATURE_C, airTemperatureC);
        double hForced = forcedConvectionCoefficientWPerM2K(localWindMPerS * (1.0D - windProof));
        return Math.max(hNatural, hForced);
    }

    private static double absorbedRadiantFluxWPerM2(double radiantFluxWPerM2, double radiantHeatProof) {
        return Math.max(0.0D, radiantFluxWPerM2)
                * RADIATION_ABSORPTIVITY
                * (1.0D - radiantHeatProof);
    }

    private static double operativeTemperatureC(
            double airTemperatureC, double hConvection, double hTotal, double absorbedFluxWPerM2) {
        return (hConvection * airTemperatureC
                + LONG_WAVE_COEFFICIENT_W_PER_M2_K
                * (airTemperatureC + absorbedFluxWPerM2
                / LONG_WAVE_COEFFICIENT_W_PER_M2_K))
                / hTotal;
    }

    private static double mediumConductanceWPerK(
            double areaM2, double fraction, double coefficientWPerM2K, double pathResistanceM2KPerW) {
        if (!(fraction > 0.0D) || !(coefficientWPerM2K > 0.0D)) {
            return 0.0D;
        }
        return areaM2 * fraction
                / (pathResistanceM2KPerW
                + 1.0D / coefficientWPerM2K);
    }

    static double bandFraction(BodyPart part, double heightRatio) {
        return clamp((heightRatio - part.immersionLower)
                        / (part.immersionUpper
                        - part.immersionLower),
                0.0D, 1.0D);
    }

    private static double naturalConvectionCoefficientWPerM2K(double skinTemperatureC, double airTemperatureC) {
        return 2.38D * Math.pow(Math.abs(skinTemperatureC - airTemperatureC), 0.25D);
    }

    private static double forcedConvectionCoefficientWPerM2K(double velocityMPerS) {
        return 12.1D * Math.sqrt(Math.max(0.0D, velocityMPerS));
    }

    /* Evaporation capacity coefficients, in W/(m2*K*kPa). */
    private static final double EVAPORATION_BASE_W_PER_M2_K_PA = 20.0D;
    private static final double EVAPORATION_WIND_W_PER_M2_K_PA = 12.0D;

    /**
     * Shares one environmental evaporation ceiling between Wet cooling and
     * sweating. A result of one accepts every request; zero permits none.
     */
    static double evaporationScale(double airTemperatureC, double relativeHumidity, double localWindMPerS,
                                   double exposedAreaM2, double evaporationRequestW) {
        if (!(evaporationRequestW > 0.0D)) return 0.0D;
        return Math.min(1.0D, evaporationCapacityW(
                airTemperatureC, relativeHumidity, localWindMPerS, exposedAreaM2) / evaporationRequestW);
    }

    private static double evaporationCapacityW(
            double airTemperatureC, double relativeHumidity, double localWindMPerS, double exposedAreaM2) {
        double vaporPressureDifferenceKPa = Math.max(0.0D,
                saturationVaporPressureKPa(REFERENCE_SKIN_TEMPERATURE_C)
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

    /* Active fire power and conservative internal conductance, in W or W/K. */
    private static final double ON_FIRE_HEAT_POWER_W = 1_200.0D;
    private static final double TORSO_HEAD_CONDUCTANCE_W_PER_K = 4.0D;
    private static final double TORSO_LEGS_CONDUCTANCE_W_PER_K = 3.0D;
    private static final double TORSO_HANDS_CONDUCTANCE_W_PER_K = 4.0D;
    private static final double LEGS_FEET_CONDUCTANCE_W_PER_K = 1.5D;

    /**
     * Integrates all five independent environmental paths, then performs
     * conservative internal transfer in one fixed pair order.
     */
    static double integrateBody(PlayerTemperatureData data, HeatingDeviceContext context,
                                double airTemperatureC, boolean onFire, double lavaHeightRatio,
                                double sharedBodyPowerW, double evaporationScale,
                                double physiologicalSeconds, boolean frozen) {
        double integratedEnergyJ = 0.0D;
        for (BodyPart part : BodyPart.VALUES) {
            double deltaEnergyJ = integratePart(data, context, part,
                    airTemperatureC, onFire, lavaHeightRatio,
                    sharedBodyPowerW, evaporationScale, physiologicalSeconds);
            if (!frozen) {
                data.addBodyEnergyJ(part, deltaEnergyJ);
                integratedEnergyJ += deltaEnergyJ;
            }
        }

        if (!frozen && physiologicalSeconds > 0.0D) {
            transferInternalBodyEnergy(data, physiologicalSeconds);
        }
        return integratedEnergyJ;
    }

    private static double integratePart(PlayerTemperatureData data, HeatingDeviceContext context, BodyPart part,
                                        double airTemperatureC, boolean onFire, double lavaHeightRatio,
                                        double sharedBodyPowerW, double evaporationScale,
                                        double physiologicalSeconds) {
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

        double passiveEquilibriumC = totalConductance > 0.0D
                ? weightedBoundary / totalConductance
                : airTemperatureC;
        data.setFeelTempByPart(part, (float) passiveEquilibriumC);
        return closedFormEnergyChangeJ(temperatureC, passiveEquilibriumC,
                totalConductance, activePowerW, partHeatCapacityJPerK(part), physiologicalSeconds);
    }

    /**
     * Exact step for a linear heat balance. Passive exchange approaches its
     * equilibrium exponentially, so water or lava cannot overshoot it.
     */
    static double closedFormEnergyChangeJ(double temperatureC, double passiveEquilibriumC,
                                          double conductanceWPerK, double activePowerW,
                                          double capacityJPerK, double seconds) {
        if (!(conductanceWPerK > 0.0D)) {
            return activePowerW * seconds;
        }
        double forcedEquilibriumC = passiveEquilibriumC
                + activePowerW / conductanceWPerK;
        double alpha = -Math.expm1(
                -conductanceWPerK * seconds / capacityJPerK);
        return capacityJPerK
                * (forcedEquilibriumC - temperatureC) * alpha;
    }

    /** Transfers equal and opposite energy without crossing pair equilibrium. */
    private static void transferInternalBodyEnergy(PlayerTemperatureData data, double physiologicalSeconds) {
        transfer(data, BodyPart.TORSO, BodyPart.HEAD, TORSO_HEAD_CONDUCTANCE_W_PER_K, physiologicalSeconds);
        transfer(data, BodyPart.TORSO, BodyPart.LEGS, TORSO_LEGS_CONDUCTANCE_W_PER_K, physiologicalSeconds);
        transfer(data, BodyPart.TORSO, BodyPart.HANDS, TORSO_HANDS_CONDUCTANCE_W_PER_K, physiologicalSeconds);
        transfer(data, BodyPart.LEGS, BodyPart.FEET, LEGS_FEET_CONDUCTANCE_W_PER_K, physiologicalSeconds);
    }

    private static void transfer(PlayerTemperatureData data, BodyPart first, BodyPart second,
                                 double conductanceWPerK, double physiologicalSeconds) {
        double firstTemperatureC = data.getAbsoluteBodyTempByPart(first);
        double secondTemperatureC = data.getAbsoluteBodyTempByPart(second);
        double differenceK = firstTemperatureC - secondTemperatureC;
        double requestedJ = conductanceWPerK * differenceK
                * physiologicalSeconds;
        double firstCapacity = partHeatCapacityJPerK(first);
        double secondCapacity = partHeatCapacityJPerK(second);
        double equalizingJ = differenceK
                / (1.0D / firstCapacity + 1.0D / secondCapacity);
        double transferJ = Math.copySign(Math.min(Math.abs(requestedJ), Math.abs(equalizingJ)), requestedJ);
        data.addBodyEnergyJ(first, -transferJ);
        data.addBodyEnergyJ(second, transferJ);
    }

    /* Still-air reference used only by the player-facing environment value. */
    private static final double REFERENCE_AIR_VELOCITY_M_PER_S = 0.1D;

    /**
     * Converts immediate environmental exchange into an equivalent still-air
     * Celsius value. Clothing, metabolism, and equipment are intentionally
     * excluded because this is an environment observation, not body safety.
     */
    static double environmentalEquivalentTemperatureC(
            double airTemperatureC, double radiantFluxWPerM2, double localWindMPerS,
            double waterTemperatureC, double waterHeightRatio, double lavaHeightRatio,
            boolean powderSnow, boolean onFire) {
        double hNatural = naturalConvectionCoefficientWPerM2K(REFERENCE_SKIN_TEMPERATURE_C, airTemperatureC);
        double hConvection = Math.max(hNatural,
                forcedConvectionCoefficientWPerM2K(localWindMPerS));
        double airLossFlux = airLossFluxWPerM2(airTemperatureC, radiantFluxWPerM2, hConvection);
        double lossFluxWPerM2 = 0.0D;
        for (BodyPart part : BodyPart.VALUES) {
            lossFluxWPerM2 += part.area * partLossFluxWPerM2(
                    part, airLossFlux, waterTemperatureC, waterHeightRatio, lavaHeightRatio, powderSnow);
        }
        if (onFire && lavaHeightRatio <= 0.0D) {
            lossFluxWPerM2 -= ON_FIRE_HEAT_POWER_W
                    / BODY_SURFACE_AREA_M2;
        }
        double hReference = Math.max(hNatural,
                forcedConvectionCoefficientWPerM2K(REFERENCE_AIR_VELOCITY_M_PER_S))
                + LONG_WAVE_COEFFICIENT_W_PER_M2_K;
        return REFERENCE_SKIN_TEMPERATURE_C
                - lossFluxWPerM2 / hReference;
    }

    private static double airLossFluxWPerM2(double airTemperatureC, double radiantFluxWPerM2, double hConvection) {
        return (hConvection + LONG_WAVE_COEFFICIENT_W_PER_M2_K)
                * (REFERENCE_SKIN_TEMPERATURE_C - airTemperatureC)
                - RADIATION_ABSORPTIVITY
                * Math.max(0.0D, radiantFluxWPerM2);
    }

    private static double partLossFluxWPerM2(BodyPart part, double airLossFluxWPerM2,
                                             double waterTemperatureC, double waterHeightRatio,
                                             double lavaHeightRatio, boolean powderSnow) {
        double lava = bandFraction(part, lavaHeightRatio);
        double water = (1.0D - lava)
                * bandFraction(part, waterHeightRatio);
        double powder = (1.0D - lava - water)
                * (powderSnow ? 1.0D : 0.0D);
        double air = 1.0D - lava - water - powder;
        return air * airLossFluxWPerM2
                + water * WATER_COEFFICIENT_W_PER_M2_K
                * (REFERENCE_SKIN_TEMPERATURE_C - waterTemperatureC)
                + lava * LAVA_COEFFICIENT_W_PER_M2_K
                * (REFERENCE_SKIN_TEMPERATURE_C - LAVA_TEMPERATURE_C)
                + powder * POWDER_SNOW_COEFFICIENT_W_PER_M2_K
                * (REFERENCE_SKIN_TEMPERATURE_C
                - POWDER_SNOW_TEMPERATURE_C);
    }

    static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static double feelTemperature(double dryTemperatureC, double relativeHumidity, double relativeWindSpeed) {
        double vaporPressure = relativeHumidity * 6.105D * Math.exp(
                17.27D * dryTemperatureC
                        / (237.7D + dryTemperatureC));
        return dryTemperatureC + 0.33D * vaporPressure
                - 0.7D * relativeWindSpeed * 35.0D - 4.0D;
    }
}
