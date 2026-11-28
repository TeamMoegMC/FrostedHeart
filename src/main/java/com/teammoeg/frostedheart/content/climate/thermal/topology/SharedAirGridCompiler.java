/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.core.BlockPos;
import java.util.Arrays;

/** Unit-cell Q1 geometry with exact local corner identities and bounded Brick scratch. */
public final class SharedAirGridCompiler {
    public Preparation prepare(AirFieldLayout captured) { return new Preparation(captured); }

    // A cell corner sees eight cells in this 3x3x3 neighbourhood of four-cubed Bricks.
    private static final int[] INCIDENT_CELLS = new int[64 * 8 * 8];
    static {
        for (int cell = 0; cell < 64; cell++) for (int corner = 0; corner < 8; corner++)
            for (int octant = 0; octant < 8; octant++) {
                int x = (cell & 3) + (corner & 1) - (octant & 1);
                int z = (cell >>> 2 & 3) + (corner >>> 1 & 1) - (octant >>> 1 & 1);
                int y = (cell >>> 4) + (corner >>> 2 & 1) - (octant >>> 2 & 1);
                int bx = x < 0 ? 0 : x >= 4 ? 2 : 1;
                int bz = z < 0 ? 0 : z >= 4 ? 2 : 1;
                int by = y < 0 ? 0 : y >= 4 ? 2 : 1;
                int neighbour = bx + 3 * bz + 9 * by;
                INCIDENT_CELLS[cell * 64 + corner * 8 + octant] = neighbour * 64 + (x & 3) + ((z & 3) << 2) + ((y & 3) << 4);
            }
    }

    /** Preserve the original first-occurrence coefficient order without whole-region parents/identities. */
    public static final class Preparation {
        private final AirFieldLayout captured;
        private Int2IntOpenHashMap componentIndices;
        private Int2IntOpenHashMap localIndices = new Int2IntOpenHashMap(128);
        private int[][] neighbours = new int[27][];
        private final int[] incidentComponents = new int[8];
        private AirFieldLayout.Component[] components;
        private final IntArrayList globals = new IntArrayList(128);
        private final ShortArrayList positions = new ShortArrayList(128);
        private int phase, cursor, coefficients, presentNeighbours;
        private long remainingBlocks;
        private long cachedBrick;
        private boolean cached;
        private short[] corners;
        private AirFieldLayout result;

        Preparation(AirFieldLayout captured) {
            this.captured = captured;
            componentIndices = new Int2IntOpenHashMap(captured.componentCount());
            componentIndices.defaultReturnValue(-1); localIndices.defaultReturnValue(-1);
            components = new AirFieldLayout.Component[captured.componentCount()];
        }
        public AirFieldLayout layout() { return result; }
        public boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int end = Math.min(captured.componentCount(), cursor + 128);
                    for (; cursor < end; cursor++) componentIndices.put(captured.component(cursor).arenaSlot, cursor);
                    if (cursor == captured.componentCount()) { cursor = 0; phase = 1; }
                } else if (cursor == components.length) {
                    componentIndices = localIndices = null; neighbours = null;
                    result = captured.withUnitGrid(components, coefficients, coefficients, 1);
                    return true;
                } else {
                    compileCells(16);
                }
            } while (System.nanoTime() < deadline);
            return false;
        }

        private void compileCells(int count) {
            var component = captured.component(cursor);
            if (corners == null) {
                globals.clear(); positions.clear(); localIndices.clear();
                corners = new short[512]; Arrays.fill(corners, (short) -1);
                remainingBlocks = component.airBlocks;
                long brick = BlockPos.asLong(component.minX, component.minY, component.minZ);
                if (!cached || cachedBrick != brick) loadNeighbours(component, brick);
            }
            while (count-- > 0 && remainingBlocks != 0) {
                int block = Long.numberOfTrailingZeros(remainingBlocks); remainingBlocks &= remainingBlocks - 1;
                for (int corner = 0; corner < 8; corner++) {
                    long first = firstOccurrence(block, corner);
                    long current = (long) cursor << 9 | block << 3 | corner;
                    int fine;
                    if (first == current) fine = coefficients++;
                    else {
                        int owner = (int) (first >>> 9), cell = (int) (first >>> 3) & 63, vertex = (int) first & 7;
                        fine = owner == cursor ? globals.getInt(corners[cell * 8 + vertex])
                                : components[owner].coefficientIndex(components[owner].unitCorner(cell, vertex));
                    }
                    int local = localIndices.get(fine);
                    if (local < 0) {
                        local = globals.size(); globals.add(fine); localIndices.put(fine, local);
                        int x = block & 3, z = block >>> 2 & 3, y = block >>> 4;
                        positions.add((short) ((x + (corner & 1)) | (z + (corner >>> 1 & 1)) << 3 | (y + (corner >>> 2)) << 6));
                    }
                    corners[block * 8 + corner] = (short) local;
                }
            }
            if (remainingBlocks == 0) {
                components[cursor++] = component.withUnitGrid(globals.toIntArray(), corners, positions.toShortArray());
                corners = null;
            }
        }

        private long firstOccurrence(int block, int corner) {
            int start = block * 64 + corner * 8, occupied = 0;
            for (int octant = 0; octant < 8; octant++) {
                int address = INCIDENT_CELLS[start + octant], neighbour = address >>> 6;
                int index = (presentNeighbours & 1 << neighbour) == 0 ? -1 : neighbours[neighbour][address & 63];
                incidentComponents[octant] = index;
                if (index >= 0) occupied |= 1 << octant;
            }
            int members = AirCornerTopology.component(occupied, AirCornerTopology.group(occupied, corner));
            long first = (long) cursor << 9 | block << 3 | corner;
            while (members != 0) {
                int octant = Integer.numberOfTrailingZeros(members); members &= members - 1;
                int cell = INCIDENT_CELLS[start + octant] & 63;
                first = Math.min(first, (long) incidentComponents[octant] << 9 | cell << 3 | octant);
            }
            return first;
        }

        private void loadNeighbours(AirFieldLayout.Component origin, long key) {
            cached = true; cachedBrick = key; presentNeighbours = 0;
            for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
                int index = x + 1 + 3 * (z + 1) + 9 * (y + 1);
                var entries = captured.borrowedComponentsAtBrick(BlockPos.asLong(origin.minX + x * 4, origin.minY + y * 4, origin.minZ + z * 4));
                if (entries == null || entries.length == 0) continue;
                int[] cells = neighbours[index];
                if (cells == null) cells = neighbours[index] = new int[64];
                Arrays.fill(cells, -1);
                for (var component : entries) {
                    int ordinal = componentIndices.get(component.arenaSlot);
                    long mask = component.airBlocks;
                    while (mask != 0) {
                        int block = Long.numberOfTrailingZeros(mask); mask &= mask - 1;
                        cells[block] = ordinal;
                    }
                }
                presentNeighbours |= 1 << index;
            }
        }
    }
}
