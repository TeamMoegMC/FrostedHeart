/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.core.BlockPos;
import java.util.Arrays;

/** Unit-cell Q1 geometry. Overload aggregation contracts only actual axial Air edges. */
public final class SharedAirGridCompiler {
    public Preparation prepare(AirFieldLayout captured) { return new Preparation(captured); }

    /** Same cell, union and coefficient order; each inner traversal has a retained cursor. */
    public static final class Preparation {
        private final AirFieldLayout captured;
        private Long2IntOpenHashMap cells;
        private int[] xs, ys, zs, parents, identities;
        private AirFieldLayout.Component[] components;
        private final IntArrayList globals = new IntArrayList(128);
        private final ShortArrayList positions = new ShortArrayList(128);
        private int phase, cursor, componentIndex, cellCount, filled, coefficients;
        private long remainingBlocks;
        private AirFieldLayout result;
        private static final int[] BITS = {1, 4, 2};

        Preparation(AirFieldLayout captured) { this.captured = captured; }
        public AirFieldLayout layout() { return result; }
        public boolean run(long deadline) {
            do {
                switch (phase) {
                    case 0 -> {
                        int end = Math.min(captured.componentCount(), cursor + 128);
                        for (; cursor < end; cursor++) cellCount = Math.addExact(cellCount, Long.bitCount(captured.component(cursor).airBlocks));
                        if (cursor == captured.componentCount()) {
                            xs = new int[cellCount]; ys = new int[cellCount]; zs = new int[cellCount];
                            parents = new int[Math.multiplyExact(cellCount, 8)]; identities = new int[parents.length];
                            cells = new Long2IntOpenHashMap(cellCount); cells.defaultReturnValue(-1);
                            components = new AirFieldLayout.Component[captured.componentCount()];
                            cursor = 0; phase++;
                        }
                    }
                    case 1 -> {
                        int limit = 512;
                        while (limit-- > 0 && componentIndex < captured.componentCount()) {
                            var component = captured.component(componentIndex);
                            if (remainingBlocks == 0) remainingBlocks = component.airBlocks;
                            int block = Long.numberOfTrailingZeros(remainingBlocks); remainingBlocks &= remainingBlocks - 1;
                            int x = component.minX + (block & 3), y = component.minY + (block >>> 4), z = component.minZ + (block >>> 2 & 3);
                            cells.put(BlockPos.asLong(x, y, z), filled); xs[filled] = x; ys[filled] = y; zs[filled++] = z;
                            if (remainingBlocks == 0) componentIndex++;
                        }
                        if (componentIndex == captured.componentCount()) { phase++; cursor = 0; }
                    }
                    case 2 -> {
                        int end = Math.min(parents.length, cursor + 4096);
                        for (; cursor < end; cursor++) { parents[cursor] = cursor; identities[cursor] = -1; }
                        if (cursor == parents.length) { phase++; cursor = 0; }
                    }
                    case 3 -> {
                        int end = Math.min(cellCount, cursor + 128);
                        for (; cursor < end; cursor++) for (int axis = 0; axis < 3; axis++) {
                            int other = cells.get(BlockPos.asLong(xs[cursor] + (axis == 0 ? 1 : 0),
                                    ys[cursor] + (axis == 1 ? 1 : 0), zs[cursor] + (axis == 2 ? 1 : 0)));
                            if (other < 0) continue;
                            int bit = BITS[axis];
                            for (int corner = 0; corner < 8; corner++) if ((corner & bit) != 0)
                                join(parents, cursor * 8 + corner, other * 8 + (corner ^ bit));
                        }
                        if (cursor == cellCount) { phase++; cursor = 0; }
                    }
                    case 4 -> {
                        int end = Math.min(parents.length, cursor + 4096);
                        for (; cursor < end; cursor++) {
                            int representative = root(parents, cursor);
                            if (identities[representative] < 0) identities[representative] = coefficients++;
                            identities[cursor] = identities[representative];
                        }
                        if (cursor == parents.length) { phase++; cursor = 0; }
                    }
                    case 5 -> {
                        int end = Math.min(coefficients, cursor + 4096);
                        Arrays.fill(parents, cursor, end, -1); cursor = end;
                        if (cursor == coefficients) { phase++; cursor = 0; }
                    }
                    case 6 -> {
                        if (cursor == components.length) { phase++; continue; }
                        var component = captured.component(cursor); globals.clear(); positions.clear();
                        short[] corners = new short[512]; Arrays.fill(corners, (short) -1);
                        long mask = component.airBlocks;
                        while (mask != 0) {
                            int block = Long.numberOfTrailingZeros(mask); mask &= mask - 1;
                            int x = block & 3, y = block >>> 4, z = block >>> 2 & 3;
                            int cell = cells.get(BlockPos.asLong(component.minX + x, component.minY + y, component.minZ + z));
                            for (int corner = 0; corner < 8; corner++) {
                                int fine = identities[cell * 8 + corner], local = parents[fine];
                                if (local < 0) {
                                    local = globals.size(); globals.add(fine); parents[fine] = local;
                                    positions.add((short) ((x + (corner & 1)) | (z + (corner >>> 1 & 1)) << 3 | (y + (corner >>> 2)) << 6));
                                }
                                corners[block * 8 + corner] = (short) local;
                            }
                        }
                        components[cursor++] = component.withUnitGrid(globals.toIntArray(), corners, positions.toShortArray());
                        for (int fine : globals) parents[fine] = -1;
                    }
                    default -> { result = captured.withUnitGrid(components, coefficients, coefficients, 1); return true; }
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
    }

    private static int root(int[] parents, int index) {
        while (parents[index] != index) { parents[index] = parents[parents[index]]; index = parents[index]; }
        return index;
    }
    private static void join(int[] parents, int first, int second) {
        int a = root(parents, first), b = root(parents, second);
        parents[Math.max(a, b)] = Math.min(a, b);
    }
}
