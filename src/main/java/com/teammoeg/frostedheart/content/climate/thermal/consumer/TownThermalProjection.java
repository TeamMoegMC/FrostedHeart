/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.consumer;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Compact result produced while an existing town building scan visits its air.
 * Each aligned 4-cubed thermal Brick retains one deterministic air sample and
 * the number of interior voxels represented by it.
 */
public final class TownThermalProjection {
    private static final int BRICK_SHIFT = 2;
    private static final int BRICK_MASK = 3;
    private static final int REPRESENTATIVE_BITS = 6;
    private static final int REPRESENTATIVE_MASK = 63;

    private final Long2IntOpenHashMap packedGroups = new Long2IntOpenHashMap();
    private int voxelCount;

    public void include(BlockPos position) {
        Objects.requireNonNull(position, "position");
        int originX = position.getX() & ~BRICK_MASK;
        int originY = position.getY() & ~BRICK_MASK;
        int originZ = position.getZ() & ~BRICK_MASK;
        long groupKey = BlockPos.asLong(originX, originY, originZ);
        int representative = (position.getX() & BRICK_MASK)
                | ((position.getZ() & BRICK_MASK) << BRICK_SHIFT)
                | ((position.getY() & BRICK_MASK) << (BRICK_SHIFT * 2));
        int packed = packedGroups.get(groupKey);
        int weight = packed >>> REPRESENTATIVE_BITS;
        if (weight != 0) {
            representative = Math.min(representative,
                    packed & REPRESENTATIVE_MASK);
        }
        packedGroups.put(groupKey,
                ((weight + 1) << REPRESENTATIVE_BITS) | representative);
        voxelCount++;
    }

    public int groupCount() {
        return packedGroups.size();
    }

    public int voxelCount() {
        return voxelCount;
    }

    /** Returns group keys in stable coordinate order for reproducible averaging. */
    public long[] groupKeys() {
        long[] keys = packedGroups.keySet().toLongArray();
        LongArrays.quickSort(keys);
        return keys;
    }

    public int weight(long groupKey) {
        return packedGroups.get(groupKey) >>> REPRESENTATIVE_BITS;
    }

    public int representativeX(long groupKey) {
        int representative = packedGroups.get(groupKey) & REPRESENTATIVE_MASK;
        return BlockPos.getX(groupKey) + (representative & BRICK_MASK);
    }

    public int representativeY(long groupKey) {
        int representative = packedGroups.get(groupKey) & REPRESENTATIVE_MASK;
        return BlockPos.getY(groupKey)
                + ((representative >>> (BRICK_SHIFT * 2)) & BRICK_MASK);
    }

    public int representativeZ(long groupKey) {
        int representative = packedGroups.get(groupKey) & REPRESENTATIVE_MASK;
        return BlockPos.getZ(groupKey)
                + ((representative >>> BRICK_SHIFT) & BRICK_MASK);
    }
}
