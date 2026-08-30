/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalExchangeKernel;

/**
 * 编排物品内部、物品表面和玩家核心三个有限热容节点的守恒换热。
 * <p>
 * Conservative fixed-topology exchange between reservoir core, reservoir
 * surface, and player core nodes.
 */
public final class ThreeNodeWearableHeatExchange {
    public static final double MAX_SUBSTEP_SECONDS = 1.0D;

    private static final double PLAYER_CAPACITY_RATIO = 1.0D;
    private static final int MAX_SUBSTEP_COUNT = 1_000_000;

    private ThreeNodeWearableHeatExchange() {
    }

    /** Caller-owned output for allocation-free repeated exchange. */
    public static final class MutableResult {
        private ThermalExchangeKernel.Status status =
                ThermalExchangeKernel.Status.NUMERIC_DEGRADED;
        private double reservoirCoreTemperatureC;
        private double reservoirSurfaceTemperatureC;
        private double playerTemperatureC;

        public ThermalExchangeKernel.Status status() {
            return status;
        }

        public boolean applied() {
            return status == ThermalExchangeKernel.Status.APPLIED;
        }

        public double reservoirCoreTemperatureC() {
            return reservoirCoreTemperatureC;
        }

        public double reservoirSurfaceTemperatureC() {
            return reservoirSurfaceTemperatureC;
        }

        public double playerTemperatureC() {
            return playerTemperatureC;
        }

        private void set(
                ThermalExchangeKernel.Status nextStatus,
                double nextReservoirCoreTemperatureC,
                double nextReservoirSurfaceTemperatureC,
                double nextPlayerTemperatureC
        ) {
            status = nextStatus;
            reservoirCoreTemperatureC = nextReservoirCoreTemperatureC;
            reservoirSurfaceTemperatureC = nextReservoirSurfaceTemperatureC;
            playerTemperatureC = nextPlayerTemperatureC;
        }
    }

    /** Caller-owned kernel scratch for allocation-free repeated exchange. */
    public static final class Scratch {
        private final ThermalExchangeKernel.MutablePairResult pairResult =
                new ThermalExchangeKernel.MutablePairResult();
    }

    /**
     * 推进固定三节点拓扑。每个至多一秒的子步使用
     * core-surface 半步、surface-player 整步、core-surface 半步的对称分步。
     * <p>
     * Advances the fixed topology with a symmetric Strang sweep for every
     * substep of at most one second.
     */
    public static ThermalExchangeKernel.Status exchangeInto(
            WearableThermalProfile profile,
            double reservoirCoreTemperatureC,
            double reservoirSurfaceTemperatureC,
            double playerTemperatureC,
            double dtSeconds,
            MutableResult result,
            Scratch scratch
    ) {
        if (result == null || scratch == null) {
            throw new NullPointerException("result and scratch are required");
        }
        if (profile == null
                || !Double.isFinite(reservoirCoreTemperatureC)
                || !Double.isFinite(reservoirSurfaceTemperatureC)
                || !Double.isFinite(playerTemperatureC)
                || !Double.isFinite(dtSeconds)
                || dtSeconds < 0.0D) {
            return degraded(
                    reservoirCoreTemperatureC,
                    reservoirSurfaceTemperatureC,
                    playerTemperatureC,
                    result
            );
        }

        double rawSubstepCount = Math.ceil(dtSeconds / MAX_SUBSTEP_SECONDS);
        if (!Double.isFinite(rawSubstepCount)
                || rawSubstepCount > MAX_SUBSTEP_COUNT) {
            return degraded(
                    reservoirCoreTemperatureC,
                    reservoirSurfaceTemperatureC,
                    playerTemperatureC,
                    result
            );
        }
        int substepCount = Math.max(1, (int) rawSubstepCount);
        double substepSeconds = dtSeconds / substepCount;
        double halfSubstepSeconds = substepSeconds * 0.5D;
        double coreCapacity = profile.coreCapacityRatio();
        double surfaceCapacity = profile.surfaceCapacityRatio();
        double coreEnthalpy = coreCapacity * reservoirCoreTemperatureC;
        double surfaceEnthalpy = surfaceCapacity * reservoirSurfaceTemperatureC;
        double playerEnthalpy = PLAYER_CAPACITY_RATIO * playerTemperatureC;
        if (!Double.isFinite(coreEnthalpy)
                || !Double.isFinite(surfaceEnthalpy)
                || !Double.isFinite(playerEnthalpy)) {
            return degraded(
                    reservoirCoreTemperatureC,
                    reservoirSurfaceTemperatureC,
                    playerTemperatureC,
                    result
            );
        }

        for (int substep = 0; substep < substepCount; substep++) {
            if (!exchangePair(
                    coreEnthalpy,
                    coreCapacity,
                    surfaceEnthalpy,
                    surfaceCapacity,
                    profile.coreSurfaceTransferRatePerSecond(),
                    halfSubstepSeconds,
                    scratch
            )) {
                return degraded(
                        reservoirCoreTemperatureC,
                        reservoirSurfaceTemperatureC,
                        playerTemperatureC,
                        result
                );
            }
            coreEnthalpy = scratch.pairResult.enthalpyAJ();
            surfaceEnthalpy = scratch.pairResult.enthalpyBJ();

            if (!exchangePair(
                    surfaceEnthalpy,
                    surfaceCapacity,
                    playerEnthalpy,
                    PLAYER_CAPACITY_RATIO,
                    profile.playerTransferRatePerSecond(),
                    substepSeconds,
                    scratch
            )) {
                return degraded(
                        reservoirCoreTemperatureC,
                        reservoirSurfaceTemperatureC,
                        playerTemperatureC,
                        result
                );
            }
            surfaceEnthalpy = scratch.pairResult.enthalpyAJ();
            playerEnthalpy = scratch.pairResult.enthalpyBJ();

            if (!exchangePair(
                    coreEnthalpy,
                    coreCapacity,
                    surfaceEnthalpy,
                    surfaceCapacity,
                    profile.coreSurfaceTransferRatePerSecond(),
                    halfSubstepSeconds,
                    scratch
            )) {
                return degraded(
                        reservoirCoreTemperatureC,
                        reservoirSurfaceTemperatureC,
                        playerTemperatureC,
                        result
                );
            }
            coreEnthalpy = scratch.pairResult.enthalpyAJ();
            surfaceEnthalpy = scratch.pairResult.enthalpyBJ();
        }

        double nextCoreTemperature = coreEnthalpy / coreCapacity;
        double nextSurfaceTemperature = surfaceEnthalpy / surfaceCapacity;
        double nextPlayerTemperature = playerEnthalpy / PLAYER_CAPACITY_RATIO;
        if (!Double.isFinite(nextCoreTemperature)
                || !Double.isFinite(nextSurfaceTemperature)
                || !Double.isFinite(nextPlayerTemperature)) {
            return degraded(
                    reservoirCoreTemperatureC,
                    reservoirSurfaceTemperatureC,
                    playerTemperatureC,
                    result
            );
        }
        result.set(
                ThermalExchangeKernel.Status.APPLIED,
                nextCoreTemperature,
                nextSurfaceTemperature,
                nextPlayerTemperature
        );
        return ThermalExchangeKernel.Status.APPLIED;
    }

    /**
     * 返回玩家节点的瞬时温度变化率 {@code g_sp * (T_surface - T_player)}。
     * <p>
     * Returns the exact instantaneous player-node temperature rate.
     */
    public static double playerTemperatureRatePerSecond(
            WearableThermalProfile profile,
            double reservoirSurfaceTemperatureC,
            double playerTemperatureC
    ) {
        if (profile == null
                || !Double.isFinite(reservoirSurfaceTemperatureC)
                || !Double.isFinite(playerTemperatureC)) {
            return Double.NaN;
        }
        return profile.playerTransferRatePerSecond()
                * (reservoirSurfaceTemperatureC - playerTemperatureC);
    }

    /**
     * 按 {@code k = r * a * (1 - a) * ln(2) / t_half} 计算内外传热率。
     * 半衰期只适用于玩家和环境均隔绝的内部/表面两节点系统。
     * <p>
     * Computes the core-surface transfer rate from the isolated two-node
     * half-life. The half-life does not describe the coupled three-node model.
     */
    public static double coreSurfaceTransferRatePerSecondFromHalfLife(
            double capacityRatio,
            double surfaceCapacityFraction,
            double halfLifeSeconds
    ) {
        requireCapacityInputs(capacityRatio, surfaceCapacityFraction);
        requirePositiveFinite(halfLifeSeconds, "halfLifeSeconds");
        return capacityRatio
                * surfaceCapacityFraction
                * (1.0D - surfaceCapacityFraction)
                * Math.log(2.0D)
                / halfLifeSeconds;
    }

    /**
     * 按 {@code t_half = r * a * (1 - a) * ln(2) / k} 反算隔绝两节点半衰期。
     * <p>
     * Computes the isolated core-surface two-node half-life from its transfer rate.
     */
    public static double coreSurfaceHalfLifeSeconds(
            double capacityRatio,
            double surfaceCapacityFraction,
            double transferRatePerSecond
    ) {
        requireCapacityInputs(capacityRatio, surfaceCapacityFraction);
        requirePositiveFinite(transferRatePerSecond, "transferRatePerSecond");
        return capacityRatio
                * surfaceCapacityFraction
                * (1.0D - surfaceCapacityFraction)
                * Math.log(2.0D)
                / transferRatePerSecond;
    }

    private static boolean exchangePair(
            double enthalpyA,
            double capacityA,
            double enthalpyB,
            double capacityB,
            double conductance,
            double dtSeconds,
            Scratch scratch
    ) {
        return ThermalExchangeKernel.exchangePairInto(
                enthalpyA,
                capacityA,
                enthalpyB,
                capacityB,
                conductance,
                dtSeconds,
                scratch.pairResult
        ) == ThermalExchangeKernel.Status.APPLIED;
    }

    private static ThermalExchangeKernel.Status degraded(
            double reservoirCoreTemperatureC,
            double reservoirSurfaceTemperatureC,
            double playerTemperatureC,
            MutableResult result
    ) {
        result.set(
                ThermalExchangeKernel.Status.NUMERIC_DEGRADED,
                reservoirCoreTemperatureC,
                reservoirSurfaceTemperatureC,
                playerTemperatureC
        );
        return ThermalExchangeKernel.Status.NUMERIC_DEGRADED;
    }

    private static void requireCapacityInputs(
            double capacityRatio,
            double surfaceCapacityFraction
    ) {
        requirePositiveFinite(capacityRatio, "capacityRatio");
        if (!Double.isFinite(surfaceCapacityFraction)
                || surfaceCapacityFraction <= 0.0D
                || surfaceCapacityFraction >= 1.0D) {
            throw new IllegalArgumentException(
                    "surfaceCapacityFraction must be finite and between zero and one");
        }
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
