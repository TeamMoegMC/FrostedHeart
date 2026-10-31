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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import net.minecraft.core.BlockPos;
import java.util.Arrays;

/**
 * Inputs accepted once by the source ledger and consumed by one numerical cut.
 * A nonempty interval carries W; an equal-endpoint record is a signed J impulse.
 * Records retain their target identity even when a later source event removes its port.
 */
public final class CutLoadBuffer {
    private static final long[] NO_LONGS = new long[0];
    private static final int[] NO_INTS = new int[0];
    private static final double[] NO_DOUBLES = new double[0];
    private static final AirLoadTable.Load[] NO_LOADS = new AirLoadTable.Load[0];
    public static final CutLoadBuffer EMPTY = new CutLoadBuffer(true);
    private long[] targets;
    private int[] generations;
    private long[] starts;
    private long[] ends;
    private double[] amounts;
    private int size;
    private double pendingEnergyJ;
    private AirLoadTable.Load[] frozenAirLoads;
    private long[] materialBlocks;
    private int[] materialProfiles;
    private long[] materialIncarnations;
    private long[] sourceIds;
    private int[] sourceGenerations;
    private long recordingSourceId;
    private int recordingSourceGeneration;

    public CutLoadBuffer() { this(false); }

    /** Frozen results allocate only their exact backing, without five discarded starter arrays. */
    private CutLoadBuffer(boolean frozen) {
        targets = frozen ? NO_LONGS : new long[32];
        generations = frozen ? NO_INTS : new int[32];
        starts = frozen ? NO_LONGS : new long[32];
        ends = frozen ? NO_LONGS : new long[32];
        amounts = frozen ? NO_DOUBLES : new double[32];
        sourceIds = frozen ? NO_LONGS : new long[32];
        sourceGenerations = frozen ? NO_INTS : new int[32];
        if (frozen) {
            frozenAirLoads = NO_LOADS;
            materialBlocks = NO_LONGS;
            materialProfiles = NO_INTS;
            materialIncarnations = NO_LONGS;
        }
    }

    public int size() { return size; }
    public long target(int index) { return targets[index]; }
    public int generation(int index) { return generations[index]; }
    public long startTick(int index) { return starts[index]; }
    public long endTick(int index) { return ends[index]; }
    public double amount(int index) { return amounts[index]; }
    public boolean impulse(int index) { return starts[index] == ends[index]; }
    public double pendingEnergyJ() { return pendingEnergyJ; }

    public boolean frozen() { return frozenAirLoads != null; }
    public AirLoadTable.Load airLoad(int index, AirLoadTable live) {
        return frozen() ? frozenAirLoads[index]
                : live.get(SourceBinding.targetIndex(targets[index]), generations[index]);
    }
    public long materialBlock(int index) { return materialBlocks[index]; }
    public int materialProfile(int index) { return materialProfiles[index]; }
    public long materialIncarnation(int index) { return materialIncarnations[index]; }
    public long sourceId(int index) { return sourceIds[index]; }
    public int sourceGeneration(int index) { return sourceGenerations[index]; }
    public void sourceContext(long sourceId, int generation) { recordingSourceId = sourceId; recordingSourceGeneration = generation; }

    @FunctionalInterface public interface MaterialLookup {
        int resolve(long block, int previousSlot, int generation, int profile, long incarnation);
    }

    /** Compile private execution addresses after the regional input prefix has frozen. */
    public static CutLoadBuffer compile(SpatialHeatInput input, AirFieldLayout layout,
            ThermalCellArena arena, MaterialLookup materials) {
        int size = input.size();
        if (size == 0) return EMPTY;
        var result = new CutLoadBuffer(true);
        result.targets = new long[size]; result.generations = new int[size];
        result.starts = new long[size]; result.ends = new long[size]; result.amounts = new double[size];
        result.frozenAirLoads = new AirLoadTable.Load[size];
        result.materialBlocks = new long[size]; result.materialProfiles = new int[size];
        result.materialIncarnations = new long[size];
        result.sourceIds = new long[size]; result.sourceGenerations = new int[size];
        for (int index = 0; index < size; index++) {
            long position = input.position(index);
            if (input.isAir(index)) {
                if (input.needsRouting(index)) return null;
                var load = AirLoadTable.compileWorldFace(layout, position, BlockFace.fromOrdinal(input.face(index)));
                if (load == null) return null;
                result.frozenAirLoads[index] = load;
                result.targets[index] = SourceBinding.airStencil(0, 0).accumulatorTargetId();
            } else {
                int slot = materials.resolve(position, input.materialSlot(index), input.generation(index), input.materialProfile(index), input.incarnation(index));
                if (slot < 0) return null;
                result.targets[index] = slot; result.generations[index] = arena.lifecycleGeneration(slot);
                result.materialBlocks[index] = position; result.materialProfiles[index] = input.materialProfile(index);
                result.materialIncarnations[index] = input.incarnation(index);
            }
            result.starts[index] = input.fromTick(index); result.ends[index] = input.toTick(index);
            result.sourceIds[index] = input.sourceId(index); result.sourceGenerations[index] = input.sourceGeneration(index);
            result.amounts[index] = input.amount(index); result.pendingEnergyJ += input.energyJ(index);
        }
        result.size = size;
        return result;
    }

    /** Exact-sized immutable source spans. No later table rebind can change their weights. */
    public CutLoadBuffer freeze(AirLoadTable air, ThermalCellArena arena) {
        if (size == 0) return EMPTY;
        CutLoadBuffer copy = new CutLoadBuffer(true);
        copy.targets = Arrays.copyOf(targets, size); copy.generations = Arrays.copyOf(generations, size);
        copy.starts = Arrays.copyOf(starts, size); copy.ends = Arrays.copyOf(ends, size);
        copy.amounts = Arrays.copyOf(amounts, size); copy.size = size; copy.pendingEnergyJ = pendingEnergyJ;
        copy.frozenAirLoads = new AirLoadTable.Load[size];
        copy.materialBlocks = new long[size]; copy.materialProfiles = new int[size];
        copy.materialIncarnations = new long[size];
        copy.sourceIds = Arrays.copyOf(sourceIds, size); copy.sourceGenerations = Arrays.copyOf(sourceGenerations, size);
        for (int index = 0; index < size; index++) {
            if (SourceBinding.isAirTarget(targets[index])) copy.frozenAirLoads[index] = airLoad(index, air);
            else {
                int slot = SourceBinding.targetIndex(targets[index]);
                copy.materialBlocks[index] = BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
                copy.materialProfiles[index] = arena.materialProfileId(slot);
                copy.materialIncarnations[index] = arena.materialIncarnation(slot);
            }
        }
        return copy;
    }

    public void recordBacking(AirBackingMeter meter) {
        meter.array(targets);
        meter.array(generations);
        meter.array(starts);
        meter.array(ends);
        meter.array(amounts);
        meter.array(frozenAirLoads); meter.array(materialBlocks); meter.array(materialProfiles); meter.array(materialIncarnations);
        meter.array(sourceIds); meter.array(sourceGenerations);
        if (frozenAirLoads != null) for (var load : frozenAirLoads) if (load != null) load.recordBacking(meter);
    }

    public void clear() {
        if (frozen()) throw new IllegalStateException("A recorded source cut is immutable");
        size = 0;
        pendingEnergyJ = 0;
    }

    public void reserve(int capacity) {
        if (frozen()) throw new IllegalStateException("A recorded source cut is immutable");
        if (capacity <= targets.length) return;
        int grown = Math.max(capacity, targets.length + targets.length / 2);
        targets = Arrays.copyOf(targets, grown);
        generations = Arrays.copyOf(generations, grown);
        starts = Arrays.copyOf(starts, grown);
        ends = Arrays.copyOf(ends, grown);
        amounts = Arrays.copyOf(amounts, grown);
        sourceIds = Arrays.copyOf(sourceIds, grown); sourceGenerations = Arrays.copyOf(sourceGenerations, grown);
    }

    void span(long target, int generation, long fromTick, long toTick, double watts) {
        if (watts == 0 || fromTick == toTick) return;
        append(target, generation, fromTick, toTick, watts);
        pendingEnergyJ += watts * ((toTick - fromTick) / 20.0);
    }

    void impulse(long target, int generation, long tick, double joules) {
        if (joules == 0) return;
        append(target, generation, tick, tick, joules);
        pendingEnergyJ += joules;
    }

    private void append(long target, int generation, long fromTick, long toTick, double amount) {
        reserve(size + 1);
        targets[size] = target;
        sourceIds[size] = recordingSourceId; sourceGenerations[size] = recordingSourceGeneration;
        generations[size] = generation;
        starts[size] = fromTick;
        ends[size] = toTick;
        amounts[size++] = amount;
    }
}
