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