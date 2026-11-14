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
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;

/**
 * Immutable execution loads for one retained regional numerical cut.
 * A nonempty interval carries W; an equal-endpoint record is a signed J impulse.
 * Source intake writes the original Store directly; this type owns no source transport.
 */
public final class CutLoadBuffer {
    public static final CutLoadBuffer EMPTY = new CutLoadBuffer(0);
    private final long[] targets, starts, ends;
    private final double[] amounts;
    private final AirLoadTable.Load[] airLoads;
    private double pendingEnergyJ;
    private CutLoadBuffer(int size) {
        targets = new long[size]; starts = new long[size]; ends = new long[size];
        amounts = new double[size]; airLoads = new AirLoadTable.Load[size];
    }

    public int size() { return targets.length; }
    public long target(int index) { return targets[index]; }
    public long startTick(int index) { return starts[index]; }
    public long endTick(int index) { return ends[index]; }
    public double amount(int index) { return amounts[index]; }
    public boolean impulse(int index) { return starts[index] == ends[index]; }
    public double pendingEnergyJ() { return pendingEnergyJ; }

    public AirLoadTable.Load airLoad(int index) { return airLoads[index]; }

    @FunctionalInterface public interface MaterialLookup {
        int resolve(long block, int previousSlot, int generation, int profile, long incarnation);
    }

    /** Compile private execution addresses after the regional input prefix has frozen. */
    public static CutLoadBuffer compile(SpatialHeatInput input, AirFieldLayout layout,
            MaterialLookup materials) {
        int size = input.size();
        if (size == 0) return EMPTY;
        var result = new CutLoadBuffer(size);
        for (int index = 0; index < size; index++) {
            long position = input.position(index);
            if (input.isAir(index)) {
                if (input.needsRouting(index)) return null;
                var load = AirLoadTable.compileWorldFace(layout, position, BlockFace.fromOrdinal(input.face(index)));
                if (load == null) return null;
                result.airLoads[index] = load;
                result.targets[index] = SourceBinding.airStencil(0, 0).accumulatorTargetId();
            } else {
                int slot = materials.resolve(position, input.materialSlot(index), input.generation(index), input.materialProfile(index), input.incarnation(index));
                if (slot < 0) return null;
                result.targets[index] = slot;
            }
            result.starts[index] = input.fromTick(index); result.ends[index] = input.toTick(index);
            result.amounts[index] = input.amount(index); result.pendingEnergyJ += input.energyJ(index);
        }
        return result;
    }

    public void recordBacking(AirBackingMeter meter) {
        meter.array(targets);
        meter.array(starts);
        meter.array(ends);
        meter.array(amounts);
        meter.array(airLoads);
        for (var load : airLoads) if (load != null) load.recordBacking(meter);
    }
}
