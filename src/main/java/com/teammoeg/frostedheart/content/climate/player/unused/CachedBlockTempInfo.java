/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.player.unused;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

public final class CachedBlockTempInfo {
    //全局使用的缓存容器
    private static final ConcurrentHashMap<BlockState, CachedBlockTempInfo> GLOBAL_CACHE = new ConcurrentHashMap<>(256);

    public static CachedBlockTempInfo get(BlockState state) {
        return GLOBAL_CACHE.get(state);
    }

    public static CachedBlockTempInfo putIfAbsent(BlockState state, CachedBlockTempInfo info) {
        return GLOBAL_CACHE.putIfAbsent(state, info);
    }

    public static void clear() {
        GLOBAL_CACHE.clear();
    }

    // ---------- 缓存条目字段 ----------
    final VoxelShape shape;
    final List<AABB> aabbList;
    final float temperature;
    final boolean isFull;
    final boolean isEmpty;


    public CachedBlockTempInfo(VoxelShape shape, float temperature) {
        this.shape = shape;
        this.temperature = temperature;
        this.isFull = (shape == SurroundingTemperatureSimulator.FULL);
        this.isEmpty = (shape == SurroundingTemperatureSimulator.EMPTY);
        this.aabbList = (!isFull && !isEmpty) ? shape.toAabbs() : Collections.emptyList();
    }
}