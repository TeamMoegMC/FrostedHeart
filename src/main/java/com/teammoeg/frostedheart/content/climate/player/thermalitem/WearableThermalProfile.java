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

/**
 * 可穿戴热库的不可变归一化热学参数。
 * <p>
 * Immutable normalized thermal parameters for a wearable reservoir.
 *
 * @param capacityRatio 总热容相对玩家归一化热容的比例 / total capacity relative to the normalized player capacity
 * @param surfaceCapacityFraction 表面节点占物品总热容的比例 / fraction of item capacity assigned to the surface node
 * @param coreSurfaceTransferRatePerSecond 内部与表面间单位温差传热率 / core-to-surface transfer rate per degree difference
 * @param playerTransferRatePerSecond 表面与玩家间单位温差传热率 / surface-to-player transfer rate per degree difference
 */
public record WearableThermalProfile(
        double capacityRatio,
        double surfaceCapacityFraction,
        double coreSurfaceTransferRatePerSecond,
        double playerTransferRatePerSecond
) {
    public static final double INVENTORY_ENVIRONMENT_MULTIPLIER = 0.5D;
    public static final double DROPPED_ENVIRONMENT_MULTIPLIER = 16.0D;

    public static final WearableThermalProfile WARM_STONE_DEFAULT =
            new WearableThermalProfile(0.10D, 0.20D, 6.1613e-5D, 1.2e-4D);
    public static final WearableThermalProfile HOT_WATER_BAG_DEFAULT =
            new WearableThermalProfile(0.25D, 0.20D, 9.2420e-4D, 8.0e-5D);

    public WearableThermalProfile {
        requirePositiveFinite(capacityRatio, "capacityRatio");
        if (!Double.isFinite(surfaceCapacityFraction)
                || surfaceCapacityFraction <= 0.0D
                || surfaceCapacityFraction >= 1.0D) {
            throw new IllegalArgumentException(
                    "surfaceCapacityFraction must be finite and between zero and one");
        }
        requirePositiveFinite(coreSurfaceTransferRatePerSecond,
                "coreSurfaceTransferRatePerSecond");
        requirePositiveFinite(playerTransferRatePerSecond,
                "playerTransferRatePerSecond");
    }

    public double coreCapacityRatio() {
        return capacityRatio * (1.0D - surfaceCapacityFraction);
    }

    public double surfaceCapacityRatio() {
        return capacityRatio * surfaceCapacityFraction;
    }

    public double inventoryEnvironmentTransferRatePerSecond() {
        return INVENTORY_ENVIRONMENT_MULTIPLIER * playerTransferRatePerSecond;
    }

    public double droppedEnvironmentTransferRatePerSecond() {
        return DROPPED_ENVIRONMENT_MULTIPLIER
                * inventoryEnvironmentTransferRatePerSecond();
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
