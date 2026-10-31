/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime;

/** Stable graph-key sorting; omitted low address bits are already ordered within each pair. */
public final class LongRadixSortCursor {
    private static final int BLOCK = 4096;
    private final int firstBit, passes;
    private final int[] offsets = new int[256];
    private long[] sourceKeys, sourcePaired, targetKeys, targetPaired;
    private int pass, phase, cursor;
    public LongRadixSortCursor(long[] keys, long[] paired, int firstBit, int bitCount) {
        this.firstBit = firstBit; passes = (bitCount + 7) / 8;
        sourceKeys = keys; sourcePaired = paired;
        targetKeys = new long[keys.length]; targetPaired = paired == null ? null : new long[paired.length];
    }
    /** After run completes, the last exchange may have left either buffer as the result. */
    public long[] sortedKeys() { return sourceKeys; }
    public long[] sortedPaired() { return sourcePaired; }
    public boolean run(long deadline) {
        do {
            if (pass == passes) return true;
            int end = Math.min(sourceKeys.length, cursor + BLOCK), shift = firstBit + pass * 8;
            if (phase == 0) {
                for (; cursor < end; cursor++) offsets[bucket(sourceKeys[cursor], shift)]++;
                if (cursor == sourceKeys.length) {
                    int start = 0;
                    for (int index = 0; index < 256; index++) { int count = offsets[index]; offsets[index] = start; start += count; }
                    phase = 1; cursor = 0;
                }
            } else {
                for (; cursor < end; cursor++) {
                    int index = offsets[bucket(sourceKeys[cursor], shift)]++;
                    targetKeys[index] = sourceKeys[cursor];
                    if (sourcePaired != null) targetPaired[index] = sourcePaired[cursor];
                }
                if (cursor == sourceKeys.length) {
                    long[] swap = sourceKeys; sourceKeys = targetKeys; targetKeys = swap;
                    swap = sourcePaired; sourcePaired = targetPaired; targetPaired = swap;
                    java.util.Arrays.fill(offsets, 0); pass++; phase = cursor = 0;
                }
            }
        } while (System.nanoTime() < deadline);
        return pass == passes;
    }
    private static int bucket(long key, int shift) { return (int) ((key ^ Long.MIN_VALUE) >>> shift) & 255; }
}
