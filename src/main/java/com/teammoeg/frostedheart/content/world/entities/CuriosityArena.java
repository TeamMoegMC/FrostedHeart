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

package com.teammoeg.frostedheart.content.world.entities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 场地方块管理：细雪斑块与雪墙的放置、快照与恢复。
 * <p>
 * Manages every block the boss places (powder snow trails, maze walls) with a
 * snapshot of the previous state, and restores them safely (fingerprint
 * check: only restore if the current block is still the one we placed).
 */
public class CuriosityArena {

    /** 一次方块放置的快照 / A single placement snapshot. */
    public record PlacedBlock(BlockPos pos, BlockState previous, BlockState placed) {
    }

    /** 细雪斑块记录（滚动窗口） / Powder snow records (rolling window). */
    private final List<PlacedBlock> powderSnow = new ArrayList<>();
    /** 迷宫雪墙记录 / Maze wall records. */
    private final List<PlacedBlock> mazeWalls = new ArrayList<>();

    /**
     * 在指定位置放置细雪（仅替换空气或雪层）。
     * <p>
     * Places powder snow at the position, only replacing air or snow layers.
     *
     * @return 是否放置成功 / whether placement succeeded
     */
    public boolean placePowderSnow(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState current = level.getBlockState(pos);
        if (!current.isAir() && !current.is(Blocks.SNOW)) return false;
        BlockState placed = Blocks.POWDER_SNOW.defaultBlockState();
        level.setBlock(pos, placed, 3);
        powderSnow.add(new PlacedBlock(pos.immutable(), current, placed));
        return true;
    }

    /** 细雪斑块当前数量 / Current powder snow patch count. */
    public int powderSnowCount() {
        return powderSnow.size();
    }

    /** 恢复最早的细雪斑块（滚动窗口溢出时调用） / Restores the oldest powder snow block. */
    public void restoreOldestPowderSnow(ServerLevel level) {
        if (powderSnow.isEmpty()) return;
        restore(level, powderSnow.remove(0));
    }

    /**
     * 在指定位置放置雪墙（仅替换空气或雪层）。
     * <p>
     * Places a snow block wall piece, only replacing air or snow layers.
     *
     * @return 是否放置成功 / whether placement succeeded
     */
    public boolean placeWall(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState current = level.getBlockState(pos);
        if (!current.isAir() && !current.is(Blocks.SNOW)) return false;
        BlockState placed = Blocks.SNOW_BLOCK.defaultBlockState();
        level.setBlock(pos, placed, 3);
        mazeWalls.add(new PlacedBlock(pos.immutable(), current, placed));
        return true;
    }

    /** 恢复全部迷宫雪墙 / Restores all maze walls. */
    public void restoreMaze(ServerLevel level) {
        restoreAll(level, mazeWalls);
    }

    /** 恢复全部放置的方块（RESET/DISPERSED 时调用） / Restores everything. */
    public void restoreAll(ServerLevel level) {
        restoreAll(level, powderSnow);
        restoreAll(level, mazeWalls);
    }

    private void restoreAll(ServerLevel level, List<PlacedBlock> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            restore(level, list.get(i));
        }
        list.clear();
    }

    private void restore(ServerLevel level, PlacedBlock b) {
        if (level.isLoaded(b.pos()) && level.getBlockState(b.pos()) == b.placed()) {
            level.setBlock(b.pos(), b.previous(), 3);
        }
    }

    /** 是否仍有未恢复的方块 / Whether there are pending placed blocks. */
    public boolean hasPending() {
        return !powderSnow.isEmpty() || !mazeWalls.isEmpty();
    }

    public void save(CompoundTag tag) {
        tag.put("arena_powder", saveList(powderSnow));
        tag.put("arena_maze", saveList(mazeWalls));
    }

    private static ListTag saveList(List<PlacedBlock> list) {
        ListTag lt = new ListTag();
        for (PlacedBlock b : list) {
            CompoundTag t = new CompoundTag();
            t.putInt("x", b.pos().getX());
            t.putInt("y", b.pos().getY());
            t.putInt("z", b.pos().getZ());
            t.put("prev", NbtUtils.writeBlockState(b.previous()));
            t.put("placed", NbtUtils.writeBlockState(b.placed()));
            lt.add(t);
        }
        return lt;
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        powderSnow.clear();
        mazeWalls.clear();
        loadList(tag.getList("arena_powder", Tag.TAG_COMPOUND), provider, powderSnow);
        loadList(tag.getList("arena_maze", Tag.TAG_COMPOUND), provider, mazeWalls);
    }

    private static void loadList(ListTag lt, HolderLookup.Provider provider, List<PlacedBlock> out) {
        var lookup = provider.lookup(Registries.BLOCK).orElse(null);
        if (lookup == null) return;
        for (Tag tag : lt) {
            CompoundTag c = (CompoundTag) tag;
            BlockPos pos = new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"));
            BlockState prev = NbtUtils.readBlockState(lookup, c.getCompound("prev"));
            BlockState placed = NbtUtils.readBlockState(lookup, c.getCompound("placed"));
            out.add(new PlacedBlock(pos, prev, placed));
        }
    }
}
