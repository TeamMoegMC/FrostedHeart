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

    public AirFieldLayout compile(ThermalCellArena arena, WorkerPageStore pages) {
        IntArrayList slots = new IntArrayList();
        Long2ObjectOpenHashMap<IntArrayList> componentIndicesByBrick = new Long2ObjectOpenHashMap<>();
        for (int slot = arena.nextLiveSlot(0); slot >= 0; slot = arena.nextLiveSlot(slot + 1)) {
            if (!arena.isAirCell(slot) || !pages.ownsCommittedCell(arena, slot)) continue;
            long brick = BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
            componentIndicesByBrick.computeIfAbsent(brick, ignored -> new IntArrayList()).add(slots.size());
            slots.add(slot);
        }
        int[] parents = new int[slots.size() * 8];
        for (int index = 0; index < parents.length; index++) parents[index] = index;
        // Only the four functions nonzero on an open shared face can acquire the same identity.
        for (int componentIndex = 0; componentIndex < slots.size(); componentIndex++) {
            int slot = slots.getInt(componentIndex);
            int x = arena.minimum(slot, 0), y = arena.minimum(slot, 1), z = arena.minimum(slot, 2);
            for (int axis = 0; axis < 3; axis++) {
                IntArrayList neighbors = componentIndicesByBrick.get(BlockPos.asLong(
                        x + (axis == 0 ? 4 : 0), y + (axis == 1 ? 4 : 0), z + (axis == 2 ? 4 : 0)));
                if (neighbors == null) continue;
                for (int neighborIndex : neighbors) {
                    if (!touches(arena.airBlockMask(slot), arena.airBlockMask(slots.getInt(neighborIndex)), axis)) continue;
                    int axisBit = CORNER_AXIS_BITS[axis];
                    for (int corner = 0; corner < 8; corner++) {
                        if ((corner & axisBit) != 0) {
                            join(parents, componentIndex * 8 + corner, neighborIndex * 8 + (corner ^ axisBit));
                        }
                    }
                }
            }
        }
        int[] coefficientByRoot = new int[parents.length];
        Arrays.fill(coefficientByRoot, -1);
        int coefficientCount = 0;
        // A corner's support occupies at most the eight Pages around its anchor.
        // Record those octants in one byte instead of a map per coefficient.
        byte[] dependencyOctants = new byte[parents.length];
        int[][] indices = new int[slots.size()][8];
        for (int componentIndex = 0; componentIndex < slots.size(); componentIndex++) {
            int slot = slots.getInt(componentIndex);
            WorkerPageStore.PageState page = pages.findPageSlot(arena.pageSlot(slot));
            for (int corner = 0; corner < 8; corner++) {
                int root = root(parents, componentIndex * 8 + corner);
                if (coefficientByRoot[root] < 0) {
                    coefficientByRoot[root] = coefficientCount++;
                }
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
        AirFieldLayout.Component[] components = new AirFieldLayout.Component[slots.size()];
        AirFieldLayout.Component[] bySlot = new AirFieldLayout.Component[arena.highWaterMark()];
        ThermalPageHandle[] dependencyScratch = new ThermalPageHandle[8];
        long[] revisionScratch = new long[8];
        for (int componentIndex = 0; componentIndex < slots.size(); componentIndex++) {
            int slot = slots.getInt(componentIndex);
            int dependencyCount = 0;
            for (int corner = 0; corner < 8; corner++) {
                int anchorX = (arena.minimum(slot, 0) + (corner & 1) * 4) >> 4;
                int anchorY = (arena.minimum(slot, 1) + (corner >>> 2) * 4) >> 4;
                int anchorZ = (arena.minimum(slot, 2) + (corner >>> 1 & 1) * 4) >> 4;
                int remaining = Byte.toUnsignedInt(dependencyOctants[indices[componentIndex][corner]]);
                while (remaining != 0) {
                    int octant = Integer.numberOfTrailingZeros(remaining);
                    remaining &= remaining - 1;
                    var page = pages.find(SectionPos.asLong(anchorX - (octant & 1), anchorY - (octant >>> 2), anchorZ - (octant >>> 1 & 1)));
                    int index = 0;
                    while (index < dependencyCount && dependencyScratch[index] != page.handle) index++;
                    if (index == dependencyCount) {
                        dependencyScratch[dependencyCount] = page.handle;
                        revisionScratch[dependencyCount++] = page.geometryRevision;
                    }
                }
            }
            components[componentIndex] = new AirFieldLayout.Component(slot, arena.lifecycleGeneration(slot),
                    arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2),
                    arena.airBlockMask(slot), indices[componentIndex],
                    Arrays.copyOf(dependencyScratch, dependencyCount), Arrays.copyOf(revisionScratch, dependencyCount));
            bySlot[slot] = components[componentIndex];
        }
        Long2ObjectOpenHashMap<AirFieldLayout.Component[]> bricks = new Long2ObjectOpenHashMap<>();
        for (var entry : componentIndicesByBrick.long2ObjectEntrySet()) {
            IntArrayList componentIndices = entry.getValue();
            AirFieldLayout.Component[] members = new AirFieldLayout.Component[componentIndices.size()];
            for (int index = 0; index < members.length; index++) members[index] = components[componentIndices.getInt(index)];
            bricks.put(entry.getLongKey(), members);
        }
        return new AirFieldLayout(components, bySlot, bricks, coefficientCount);
    }

    private static boolean touches(long first, long second, int axis) {
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
