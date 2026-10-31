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
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.Arrays;

/** Compiles shared four-block basis identities from captured Air, without world reads. */
public final class AirFieldCompiler {
    private static final int[] CORNER_AXIS_BITS = {1, 4, 2};

    public Preparation prepare(ThermalCellArena arena, AirGeometryView pages) {
        return new Preparation(arena, pages);
    }

    /** Same component/corner order as synchronous compilation; only the loop cursor moves. */
    public static final class Preparation {
        private final ThermalCellArena arena;
        private final AirGeometryView pages;
        private final IntArrayList slots = new IntArrayList();
        private final Long2ObjectOpenHashMap<IntArrayList> componentIndicesByBrick = new Long2ObjectOpenHashMap<>();
        private int[] parents, coefficientByRoot;
        private byte[] dependencyOctants;
        private int[][] indices;
        private AirFieldLayout.Component[] components;
        private final ThermalPageHandle[] dependencyScratch = new ThermalPageHandle[8];
        private final long[] revisionScratch = new long[8];
        private final Long2ObjectOpenHashMap<AirFieldLayout.Component[]> bricks = new Long2ObjectOpenHashMap<>();
        private java.util.Iterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<IntArrayList>> brickCursor;
        private int phase, cursor, coefficientCount;
        private AirFieldLayout result;
        private AirGeometryView.CellCursor cells;
        private int initializedRoots;
        private int initializedCoefficients;

        private Preparation(ThermalCellArena arena, AirGeometryView pages) {
            this.arena = arena; this.pages = pages;
        }

        public AirFieldLayout layout() { return result; }

        public boolean run(long deadline) {
            while (result == null) {
                switch (phase) {
                    case 0 -> {
                        if (cells == null) cells = pages.cellCursor(arena);
                        int remaining = 512;
                        while (remaining-- > 0 && cells.hasNext()) {
                            int slot = cells.nextSlot();
                            if (slot < 0 || !arena.isAirCell(slot)) continue;
                            long brick = BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
                            componentIndicesByBrick.computeIfAbsent(brick, ignored -> new IntArrayList()).add(slots.size());
                            slots.add(slot);
                        }
                        if (!cells.hasNext()) {
                            if (parents == null) parents = new int[slots.size() * 8];
                            int end = Math.min(parents.length, initializedRoots + 4096);
                            for (; initializedRoots < end; initializedRoots++) parents[initializedRoots] = initializedRoots;
                            if (initializedRoots == parents.length) phase++;
                        }
                    }
                    case 1 -> {
                        int end = Math.min(slots.size(), cursor + 16);
                        while (cursor < end) joinNeighbors(cursor++);
                        if (cursor == slots.size()) {
                            if (coefficientByRoot == null) {
                                coefficientByRoot = new int[parents.length];
                                dependencyOctants = new byte[parents.length]; indices = new int[slots.size()][];
                            }
                            int limit = Math.min(coefficientByRoot.length, initializedCoefficients + 4096);
                            Arrays.fill(coefficientByRoot, initializedCoefficients, limit, -1); initializedCoefficients = limit;
                            if (initializedCoefficients == coefficientByRoot.length) { cursor = 0; phase++; }
                        }
                    }
                    case 2 -> {
                        int end = Math.min(slots.size(), cursor + 16);
                        while (cursor < end) assignCorners(cursor++);
                        if (cursor == slots.size()) {
                            components = new AirFieldLayout.Component[slots.size()];
                            cursor = 0; phase++;
                        }
                    }
                    case 3 -> {
                        int end = Math.min(slots.size(), cursor + 16);
                        while (cursor < end) captureComponent(cursor++);
                        if (cursor == slots.size()) {
                            brickCursor = componentIndicesByBrick.long2ObjectEntrySet().iterator();
                            phase++;
                        }
                    }
                    case 4 -> {
                        for (int count = 0; count < 16 && brickCursor.hasNext(); count++) {
                            var entry = brickCursor.next();
                            IntArrayList componentIndices = entry.getValue();
                            var members = new AirFieldLayout.Component[componentIndices.size()];
                            for (int index = 0; index < members.length; index++) members[index] = components[componentIndices.getInt(index)];
                            bricks.put(entry.getLongKey(), members);
                        }
                        if (!brickCursor.hasNext()) result = new AirFieldLayout(components, bricks, coefficientCount);
                    }
                    default -> throw new IllegalStateException("Unknown Air basis preparation stage");
                }
                if (System.nanoTime() >= deadline) break;
            }
            return result != null;
        }

        private void joinNeighbors(int componentIndex) {
            int slot = slots.getInt(componentIndex);
            int x = arena.minimum(slot, 0), y = arena.minimum(slot, 1), z = arena.minimum(slot, 2);
            for (int axis = 0; axis < 3; axis++) {
                IntArrayList neighbors = componentIndicesByBrick.get(BlockPos.asLong(
                        x + (axis == 0 ? 4 : 0), y + (axis == 1 ? 4 : 0), z + (axis == 2 ? 4 : 0)));
                if (neighbors == null) continue;
                for (int neighborIndex : neighbors) {
                    if (!touches(arena.airBlockMask(slot), arena.airBlockMask(slots.getInt(neighborIndex)), axis)) continue;
                    int axisBit = CORNER_AXIS_BITS[axis];
                    for (int corner = 0; corner < 8; corner++) if ((corner & axisBit) != 0)
                        join(parents, componentIndex * 8 + corner, neighborIndex * 8 + (corner ^ axisBit));
                }
            }
        }

        private void assignCorners(int componentIndex) {
            indices[componentIndex] = new int[8];
            int slot = slots.getInt(componentIndex);
            WorkerPageStore.PageState page = pages.pageAtSlot(arena.pageSlot(slot));
            for (int corner = 0; corner < 8; corner++) {
                int root = root(parents, componentIndex * 8 + corner);
                if (coefficientByRoot[root] < 0) coefficientByRoot[root] = coefficientCount++;
                int coefficientIndex = coefficientByRoot[root];
                indices[componentIndex][corner] = coefficientIndex;
                int anchorX = (arena.minimum(slot, 0) + (corner & 1) * 4) >> 4;
                int anchorY = (arena.minimum(slot, 1) + (corner >>> 2) * 4) >> 4;
                int anchorZ = (arena.minimum(slot, 2) + (corner >>> 1 & 1) * 4) >> 4;
                long section = page.handle.sectionKey();
                int octant = (SectionPos.x(section) < anchorX ? 1 : 0)
                        | (SectionPos.y(section) < anchorY ? 4 : 0) | (SectionPos.z(section) < anchorZ ? 2 : 0);
                dependencyOctants[coefficientIndex] |= (byte) (1 << octant);
            }
        }

        private void captureComponent(int componentIndex) {
            int slot = slots.getInt(componentIndex);
            int dependencyCount = 0;
            for (int corner = 0; corner < 8; corner++) {
                int anchorX = (arena.minimum(slot, 0) + (corner & 1) * 4) >> 4;
                int anchorY = (arena.minimum(slot, 1) + (corner >>> 2) * 4) >> 4;
                int anchorZ = (arena.minimum(slot, 2) + (corner >>> 1 & 1) * 4) >> 4;
                int remaining = Byte.toUnsignedInt(dependencyOctants[indices[componentIndex][corner]]);
                while (remaining != 0) {
                    int octant = Integer.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
                    var page = pages.pageAt(SectionPos.asLong(anchorX - (octant & 1), anchorY - (octant >>> 2), anchorZ - (octant >>> 1 & 1)));
                    int index = 0;
                    while (index < dependencyCount && dependencyScratch[index] != page.handle) index++;
                    if (index == dependencyCount) {
                        dependencyScratch[dependencyCount] = page.handle;
                        revisionScratch[dependencyCount++] = pages.geometryRevision(page);
                    }
                }
            }
            components[componentIndex] = new AirFieldLayout.Component(slot, arena.lifecycleGeneration(slot),
                    arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2),
                    arena.airBlockMask(slot), indices[componentIndex],
                    Arrays.copyOf(dependencyScratch, dependencyCount), Arrays.copyOf(revisionScratch, dependencyCount));
        }
    }

    public static boolean touches(long first, long second, int axis) {
        for (int patch = 0; patch < 16; patch++) {
            int firstBlock = com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout.faceBlock(axis, 3, patch);
            int secondBlock = com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout.faceBlock(axis, 0, patch);
            if ((first & 1L << firstBlock) != 0 && (second & 1L << secondBlock) != 0) return true;
        }
        return false;
    }

    private static int root(int[] parents, int index) {
        while (parents[index] != index) {
            parents[index] = parents[parents[index]];
            index = parents[index];
        }
        return index;
    }

    private static void join(int[] parents, int first, int second) {
        int firstRoot = root(parents, first), secondRoot = root(parents, second);
        parents[Math.max(firstRoot, secondRoot)] = Math.min(firstRoot, secondRoot);
    }
}
