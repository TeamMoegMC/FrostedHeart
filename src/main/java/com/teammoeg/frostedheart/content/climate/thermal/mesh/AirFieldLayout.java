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
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Immutable spatial basis for one compiled Air field. Components describe geometry;
 * their eight corners reference shared coefficients, not eight copies of temperature.
 * Coefficient vectors belong to the solver or a coherent query publication.
 */
public final class AirFieldLayout {
    private final Component[] components;
    private final Int2ObjectOpenHashMap<Component> componentsByArenaSlot;
    private final Long2ObjectOpenHashMap<Component[]> bricks;
    private final int coefficientCount;
    private final boolean compiledUnitGrid;
    private final int fineCoefficientCount;
    private final int aggregationSpan;
    private volatile long backingBytes = -1;

    /** Arrays and the completed index transfer ownership from the topology compiler. */
    public AirFieldLayout(Component[] components,
            Long2ObjectOpenHashMap<Component[]> bricks, int coefficientCount) {
        this(components, indexSlots(components), bricks, coefficientCount, false, coefficientCount, 1);
    }

    private AirFieldLayout(Component[] components, Int2ObjectOpenHashMap<Component> componentsByArenaSlot,
            Long2ObjectOpenHashMap<Component[]> bricks, int coefficientCount,
            boolean compiledUnitGrid, int fineCoefficientCount, int aggregationSpan) {
        this.components = components;
        this.componentsByArenaSlot = componentsByArenaSlot;
        this.bricks = bricks;
        this.coefficientCount = coefficientCount;
        this.compiledUnitGrid = compiledUnitGrid;
        this.fineCoefficientCount = fineCoefficientCount;
        this.aggregationSpan = aggregationSpan;
    }

    public int coefficientCount() { return coefficientCount; }
    public int componentCount() { return components.length; }
    public Component component(int index) { return components[index]; }
    public boolean isUnitGrid() { return components.length != 0 && components[0].isUnitGrid(); }
    /** -1 means the direct coarse compiler did not build or count a fine layout. */
    public int fineCoefficientCount() { return fineCoefficientCount; }
    /** 1 is the ordinary grid; 0 is the emergency aggregate; 4 is the direct coarse trial. */
    public int aggregationSpan() { return aggregationSpan; }
    public boolean isAggregated() {
        return coefficientCount != 0 && (aggregationSpan != 1 || coefficientCount < fineCoefficientCount);
    }

    /** Reuses captured eight-corner geometry; no fine nodes or addresses are allocated. */
    public AirFieldLayout asDirectCoarseGrid() {
        return new AirFieldLayout(components, componentsByArenaSlot, bricks, coefficientCount, false, -1, 4);
    }

    public AirFieldLayout withUnitGrid(Component[] gridComponents, int count, int fineCount, int span) {
        Long2ObjectOpenHashMap<ArrayList<Component>> grouped = new Long2ObjectOpenHashMap<>();
        for (Component component : gridComponents) {
            grouped.computeIfAbsent(BlockPos.asLong(component.minX, component.minY, component.minZ),
                    ignored -> new ArrayList<>()).add(component);
        }
        Long2ObjectOpenHashMap<Component[]> gridBricks = new Long2ObjectOpenHashMap<>();
        for (var entry : grouped.long2ObjectEntrySet()) gridBricks.put(entry.getLongKey(), entry.getValue().toArray(Component[]::new));
        return new AirFieldLayout(gridComponents, indexSlots(gridComponents), gridBricks,
                count, true, fineCount, span);
    }

    /**
     * Reuse the compiler's deterministic numbering only for identical ordered Air cells.
     * Combined layouts have different row ordering and cannot donate this structure.
     * The new components retain this capture's dependencies and geometry revisions.
     */
    public AirFieldLayout withRetainedUnitGrid(AirFieldLayout previous) {
        if (previous == null || !previous.compiledUnitGrid || !previous.isUnitGrid()
                || components.length != previous.components.length) return null;
        for (int index = 0; index < components.length; index++) {
            Component current = components[index], old = previous.components[index];
            if (current.arenaSlot != old.arenaSlot || current.arenaGeneration != old.arenaGeneration
                    || current.minX != old.minX || current.minY != old.minY || current.minZ != old.minZ
                    || current.airBlocks != old.airBlocks) return null;
        }
        Component[] retained = new Component[components.length];
        for (int index = 0; index < retained.length; index++) {
            Component old = previous.components[index];
            retained[index] = components[index].withUnitGrid(old.coefficientIndices, old.unitCorners, old.unitPositions);
        }
        return withUnitGrid(retained, previous.coefficientCount, previous.fineCoefficientCount, previous.aggregationSpan);
    }

    public void recordBacking(AirBackingMeter meter) {
        if (meter.summaries()) {
            long bytes = backingBytes;
            if (bytes < 0) {
                try (var local = new AirBackingMeter()) { recordBacking(local); backingBytes = bytes = local.bytes(); }
            }
            meter.objectOnce(this, bytes);
            return;
        }
        // Solver, candidate and query snapshots can all retain this immutable layout.
        if (!meter.visit(this)) return;
        meter.array(components);
        meter.container(componentsByArenaSlot, componentsByArenaSlot.size(), 16);
        meter.container(bricks, bricks.size(), 16);
        for (Component[] members : bricks.values()) meter.array(members);
        for (Component component : components) component.recordBacking(meter);
    }

    public Component componentForSlot(int slot) {
        return componentsByArenaSlot.get(slot);
    }

    private static Int2ObjectOpenHashMap<Component> indexSlots(Component[] components) {
        var index = new Int2ObjectOpenHashMap<Component>(components.length);
        for (Component component : components) index.put(component.arenaSlot, component);
        return index;
    }

    /** A publication bridge only: regional matrices and dynamic state keep their local addresses. */
    public record Combined(AirFieldLayout layout, int[][] localToCombined) {}

    public static Combined combine(List<AirFieldLayout> regions) {
        int coefficientCount = 0, componentCount = 0;
        for (AirFieldLayout region : regions) {
            coefficientCount += region.coefficientCount;
            componentCount += region.components.length;
        }
        int[][] maps = new int[regions.size()][];
        Component[] combined = new Component[componentCount];
        var byBrick = new Long2ObjectOpenHashMap<ArrayList<Component>>();
        int coefficientOffset = 0, nextComponent = 0;
        for (int regionIndex = 0; regionIndex < regions.size(); regionIndex++) {
            AirFieldLayout region = regions.get(regionIndex);
            int[] map = maps[regionIndex] = new int[region.coefficientCount];
            for (int row = 0; row < region.coefficientCount; row++) map[row] = coefficientOffset + row;
            coefficientOffset += region.coefficientCount;
            for (Component component : region.components) {
                int[] addresses = new int[component.coefficientIndices.length];
                for (int basis = 0; basis < addresses.length; basis++) addresses[basis] = map[component.coefficientIndices[basis]];
                Component copy = new Component(component.arenaSlot, component.arenaGeneration,
                        component.minX, component.minY, component.minZ, component.airBlocks, addresses,
                        component.dependencies, component.geometryRevisions);
                copy.unitCorners = component.unitCorners;
                copy.unitPositions = component.unitPositions;
                combined[nextComponent++] = copy;
                byBrick.computeIfAbsent(BlockPos.asLong(copy.minX, copy.minY, copy.minZ),
                        ignored -> new ArrayList<>()).add(copy);
            }
        }
        Arrays.sort(combined, Comparator.comparingInt(component -> component.arenaSlot));
        var bricks = new Long2ObjectOpenHashMap<Component[]>();
        for (var entry : byBrick.long2ObjectEntrySet()) bricks.put(entry.getLongKey(), entry.getValue().toArray(Component[]::new));
        return new Combined(new AirFieldLayout(combined, indexSlots(combined), bricks,
                coefficientCount, false, coefficientCount, 1), maps);
    }

    public Component componentAt(int x, int y, int z) {
        Component[] candidates = bricks.get(BlockPos.asLong(x & ~3, y & ~3, z & ~3));
        if (candidates == null) return null;
        int blockIndex = (x & 3) | (z & 3) << 2 | (y & 3) << 4;
        for (Component candidate : candidates) {
            if ((candidate.airBlocks & 1L << blockIndex) != 0) return candidate;
        }
        return null;
    }

    /** Actual captured membership, independent of coefficient addresses or region numbering. */
    public Component[] borrowedComponentsAtBrick(long brickOrigin) { return bricks.get(brickOrigin); }

    public long airBlocksAtBrick(long brickOrigin) {
        Component[] members = bricks.get(brickOrigin);
        long mask = 0;
        if (members != null) for (Component member : members) mask |= member.airBlocks;
        return mask;
    }

    /** Relative temperature (C); NaN means the point has no captured actual Air. */
    public double temperatureOffsetC(double x, double y, double z, double[] coefficientsC) {
        Component component = componentAt(floor(x), floor(y), floor(z));
        return component == null ? Double.NaN : component.temperatureOffsetC(x, y, z, coefficientsC);
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    /** Geometry and shared corner addresses for one 4-cubed Brick Air component. */
    public static final class Component {
        public final int arenaSlot;
        public final int arenaGeneration;
        private final long pageLifecycle;
        public final int minX, minY, minZ;
        public final long airBlocks;
        private final int[] coefficientIndices;
        private final ThermalPageHandle[] dependencies;
        private final long[] geometryRevisions;
        // Local to a 4x4x4 Brick: at most 512 corner identities; -1 is absent.
        // Packed local positions use at most 9 bits. Global coefficient addresses remain int.
        private short[] unitCorners, unitPositions;

        public Component(int arenaSlot, int arenaGeneration, int minX, int minY, int minZ,
                long airBlocks, int[] coefficientIndices, ThermalPageHandle[] dependencies,
                long[] geometryRevisions) {
            this.arenaSlot = arenaSlot;
            this.arenaGeneration = arenaGeneration;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.airBlocks = airBlocks;
            this.coefficientIndices = coefficientIndices;
            this.dependencies = dependencies;
            this.geometryRevisions = geometryRevisions;
            long lifecycle = -1;
            long section = net.minecraft.core.SectionPos.asLong(minX >> 4, minY >> 4, minZ >> 4);
            for (var dependency : dependencies) if (dependency.sectionKey() == section) {
                lifecycle = dependency.lifecycleGeneration(); break;
            }
            pageLifecycle = lifecycle;
        }

        /** Node slots may change during construction; only an actual Page reload ends inheritance. */
        public boolean samePageLifetime(Component previous) {
            return previous != null && (pageLifecycle < 0 || previous.pageLifecycle < 0
                    ? arenaGeneration == previous.arenaGeneration : pageLifecycle == previous.pageLifecycle);
        }
        public int coefficientIndex(int corner) { return coefficientIndices[corner]; }
        public int basisCount() { return coefficientIndices.length; }
        public boolean isUnitGrid() { return unitCorners != null; }
        public int pointBasisCount() { return isUnitGrid() ? 8 : basisCount(); }
        public int unitCorner(int block, int corner) { return unitCorners[block * 8 + corner]; }
        public int unitPosition(int basis) { return unitPositions[basis]; }
        /** v6 checkpoints retain their existing int-array representation. */
        public int[] unitCornersCopy() {
            int[] copy = new int[unitCorners.length];
            for (int index = 0; index < copy.length; index++) copy[index] = unitCorners[index];
            return copy;
        }
        public Component withUnitGrid(int[] indices, short[] corners, short[] positions) {
            Component result = new Component(arenaSlot, arenaGeneration, minX, minY, minZ,
                    airBlocks, indices, dependencies, geometryRevisions);
            result.unitCorners = corners; result.unitPositions = positions;
            return result;
        }

        public int airCellAtPointForGrid(double x, double y, double z) { return airCellAtPoint(x, y, z); }
        public double unitWeight(int cell, int corner, double x, double y, double z, int axis) {
            double dx = x - minX - (cell & 3), dy = y - minY - (cell >>> 4), dz = z - minZ - (cell >>> 2 & 3);
            return (axis == 0 ? ((corner & 1) == 0 ? -1 : 1) : ((corner & 1) == 0 ? 1 - dx : dx))
                    * (axis == 1 ? ((corner & 4) == 0 ? -1 : 1) : ((corner & 4) == 0 ? 1 - dy : dy))
                    * (axis == 2 ? ((corner & 2) == 0 ? -1 : 1) : ((corner & 2) == 0 ? 1 - dz : dz));
        }

        public void recordBacking(AirBackingMeter meter) {
            if (!meter.visit(this)) return;
            meter.array(coefficientIndices);
            meter.array(dependencies);
            meter.array(geometryRevisions);
            meter.array(unitCorners); meter.array(unitPositions);
        }

        public int dependencyCount() { return dependencies.length; }
        public ThermalPageHandle dependency(int index) { return dependencies[index]; }
        public long geometryRevision(int index) { return geometryRevisions[index]; }

        /** The point and face compilers use exactly these same trilinear weights. */
        public double weight(int corner, double x, double y, double z) {
            int cell = isUnitGrid() ? airCellAtPoint(x, y, z) : -1;
            return weightAtAirCell(corner, cell, x, y, z);
        }

        /** Reuses this component's already resolved Air cell for the same point. */
        public double weightAtAirCell(int corner, int cell, double x, double y, double z) {
            if (isUnitGrid()) {
                if (cell < 0) return 0;
                double total = 0;
                for (int vertex = 0; vertex < 8; vertex++) if (unitCorner(cell, vertex) == corner) total += unitWeight(cell, vertex, x, y, z, -1);
                return total;
            }
            double relativeX = (x - minX) * 0.25;
            double relativeY = (y - minY) * 0.25;
            double relativeZ = (z - minZ) * 0.25;
            return ((corner & 1) == 0 ? 1 - relativeX : relativeX)
                    * ((corner & 2) == 0 ? 1 - relativeZ : relativeZ)
                    * ((corner & 4) == 0 ? 1 - relativeY : relativeY);
        }

        public double temperatureOffsetC(double x, double y, double z, double[] coefficientsC) {
            double temperatureC = 0;
            if (isUnitGrid()) {
                int cell = airCellAtPoint(x, y, z);
                return temperatureOffsetAtAirCell(cell, x, y, z, coefficientsC);
            }
            for (int corner = 0; corner < coefficientIndices.length; corner++) {
                temperatureC += weightAtAirCell(corner, -1, x, y, z) * coefficientsC[coefficientIndices[corner]];
            }
            return temperatureC;
        }

        /** The caller knows the Air side of this vertex, including disconnected coincident branches. */
        public double temperatureOffsetAtVertex(int cell, int corner, double[] coefficientsC) {
            if (isUnitGrid()) return coefficientsC[coefficientIndices[unitCorner(cell, corner)]];
            return temperatureOffsetC(minX + (cell & 3) + (corner & 1),
                    minY + (cell >>> 4) + (corner >>> 2), minZ + (cell >>> 2 & 3) + (corner >>> 1 & 1), coefficientsC);
        }

        /** Caller already owns the interior cell; vertex/face queries keep the original selector. */
        public double temperatureOffsetAtAirCell(int cell, double x, double y, double z, double[] coefficientsC) {
            if (!isUnitGrid()) return temperatureOffsetC(x, y, z, coefficientsC);
            if (cell < 0) return Double.NaN;
            double temperatureC = 0;
            for (int corner = 0; corner < 8; corner++) temperatureC += unitWeight(cell, corner, x, y, z, -1)
                    * coefficientsC[coefficientIndices[unitCorner(cell, corner)]];
            return temperatureC;
        }

        public double minimumOffsetC(double[] coefficientsC) {
            double minimum = Double.POSITIVE_INFINITY;
            if (isUnitGrid()) {
                for (int coefficient : coefficientIndices) minimum = Math.min(minimum, coefficientsC[coefficient]);
                return minimum;
            }
            for (int index = 0; index < 8; index++) minimum = Math.min(minimum, coefficientsC[coefficientIndices[index]]);
            return minimum;
        }

        public double maximumOffsetC(double[] coefficientsC) {
            double maximum = Double.NEGATIVE_INFINITY;
            if (isUnitGrid()) {
                for (int coefficient : coefficientIndices) maximum = Math.max(maximum, coefficientsC[coefficient]);
                return maximum;
            }
            for (int index = 0; index < 8; index++) maximum = Math.max(maximum, coefficientsC[coefficientIndices[index]]);
            return maximum;
        }

        /** At a material face use the Air side's vertex branch, including upper Brick faces. */
        private int airCellAtPoint(double x, double y, double z) {
            int bx = floor(x), by = floor(y), bz = floor(z);
            for (int neighbor = 0; neighbor < 8; neighbor++) {
                if ((neighbor & 1) != 0 && x != bx || (neighbor & 2) != 0 && z != bz || (neighbor & 4) != 0 && y != by) continue;
                int lx = bx - minX - (neighbor & 1);
                int ly = by - minY - (neighbor >>> 2);
                int lz = bz - minZ - (neighbor >>> 1 & 1);
                if (lx < 0 || lx > 3 || ly < 0 || ly > 3 || lz < 0 || lz > 3) continue;
                int block = lx | lz << 2 | ly << 4;
                if ((airBlocks & 1L << block) != 0) return block;
            }
            return -1;
        }

        /** A shape support can cross Pages; checking only the query's Page is insufficient. */
        public boolean currentGeometry() {
            for (int index = 0; index < dependencies.length; index++) {
                if (!dependencies[index].hasCurrentAirGeometry(geometryRevisions[index])) return false;
            }
            return true;
        }

        /** Last complete worker capture remains readable while the world queues a replacement. */
        public boolean capturedGeometry(long gameTick, int maximumAgeTicks) {
            for (int index = 0; index < dependencies.length; index++) {
                ThermalPageHandle page = dependencies[index];
                PagePublication publication = page.lastPublication();
                PagePublication previous = page.previousPublication();
                long expected = geometryRevisions[index];
                if (page.pendingFullGeometryResync() != null) return false;
                if ((publication == null || publication.geometryRevision() != expected)
                        && (previous == null || previous.geometryRevision() != expected)) return false;
                if (page.liveGeometryRevision() != expected
                        && !page.capturedFallbackWithin(gameTick, maximumAgeTicks)) return false;
            }
            return true;
        }

        public String describeGeometryMismatch() {
            for (int index = 0; index < dependencies.length; index++) {
                ThermalPageHandle page = dependencies[index];
                if (page.hasCurrentAirGeometry(geometryRevisions[index])) continue;
                PagePublication current = page.currentAirPublication();
                PagePublication last = page.lastPublication();
                return "section=" + page.sectionKey() + ",expected=" + geometryRevisions[index]
                        + ",current=" + (current == null ? -1 : current.geometryRevision())
                        + ",last=" + (last == null ? -1 : last.geometryRevision())
                        + ",live=" + page.liveGeometryRevision()
                        + ",lastPhysical=" + page.lastPhysicalGeometryRevision()
                        + ",firstStaleTick=" + page.firstStaleGeometryTick()
                        + ",resync=" + page.pendingFullGeometryResync();
            }
            return "none";
        }
    }
}
