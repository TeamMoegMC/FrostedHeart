/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import java.util.Arrays;

/**
 * Inputs accepted once by the source ledger and consumed by one numerical cut.
 * A nonempty interval carries W; an equal-endpoint record is a signed J impulse.
 * Records retain their target identity even when a later source event removes its port.
 */
public final class CutLoadBuffer {
    private long[] targets = new long[32];
    private int[] generations = new int[32];
    private long[] starts = new long[32];
    private long[] ends = new long[32];
    private double[] amounts = new double[32];
    private int size;
    private double pendingEnergyJ;

    public int size() { return size; }
    public long target(int index) { return targets[index]; }
    public int generation(int index) { return generations[index]; }
    public long startTick(int index) { return starts[index]; }
    public long endTick(int index) { return ends[index]; }
    public double amount(int index) { return amounts[index]; }
    public boolean impulse(int index) { return starts[index] == ends[index]; }
    public double pendingEnergyJ() { return pendingEnergyJ; }

    public void clear() {
        size = 0;
        pendingEnergyJ = 0;
    }

    public void reserve(int capacity) {
        if (capacity <= targets.length) return;
        int grown = Math.max(capacity, targets.length + targets.length / 2);
        targets = Arrays.copyOf(targets, grown);
        generations = Arrays.copyOf(generations, grown);
        starts = Arrays.copyOf(starts, grown);
        ends = Arrays.copyOf(ends, grown);
        amounts = Arrays.copyOf(amounts, grown);
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
        generations[size] = generation;
        starts[size] = fromTick;
        ends[size] = toTick;
        amounts[size++] = amount;
    }
}
