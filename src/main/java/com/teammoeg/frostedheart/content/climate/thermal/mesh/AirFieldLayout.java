/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Immutable spatial basis for one compiled Air field. Components describe geometry;
 * their eight corners reference shared coefficients, not eight copies of temperature.
 * Coefficient vectors belong to the solver or a coherent query publication.
 */
public final class AirFieldLayout {
    private final Component[] components;
    private final Component[] componentsByArenaSlot;
    private final Long2ObjectOpenHashMap<Component[]> bricks;
    private final int coefficientCount;
    private final int coarseCount;
    private final LocalAirShape[] localShapes;

    /** Arrays and the completed index transfer ownership from the topology compiler. */
    public AirFieldLayout(Component[] components, Component[] componentsByArenaSlot,
            Long2ObjectOpenHashMap<Component[]> bricks, int coefficientCount) {
        this(components, componentsByArenaSlot, bricks, coefficientCount, coefficientCount, new LocalAirShape[0]);
    }

    private AirFieldLayout(Component[] components, Component[] componentsByArenaSlot,
            Long2ObjectOpenHashMap<Component[]> bricks, int coefficientCount, int coarseCount, LocalAirShape[] localShapes) {
        this.components = components;
        this.componentsByArenaSlot = componentsByArenaSlot;
        this.bricks = bricks;
        this.coefficientCount = coefficientCount;
        this.coarseCount = coarseCount;
        this.localShapes = localShapes;
    }

    public int coefficientCount() { return coefficientCount; }
    public int coarseCount() { return coarseCount; }
    public int localShapeCount() { return localShapes.length; }
    public LocalAirShape localShape(int index) { return localShapes[index]; }
    public int componentCount() { return components.length; }
    public Component component(int index) { return components[index]; }

    public Component componentForSlot(int slot) {
        return slot >= 0 && slot < componentsByArenaSlot.length ? componentsByArenaSlot[slot] : null;
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

    /** Relative temperature (C); NaN means the point has no captured actual Air. */
    public double temperatureOffsetC(double x, double y, double z, double[] coefficientsC) {
        Component component = componentAt(floor(x), floor(y), floor(z));
        return component == null ? Double.NaN : component.temperatureOffsetC(x, y, z, coefficientsC);
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    /** Builds one immutable enriched layout, sharing static shape data between its components. */
    public AirFieldLayout withLocalShapes(List<LocalAirShape> shapes) {
        if (shapes.isEmpty()) return this;
        Component[] enriched = new Component[components.length];
        Component[] bySlot = new Component[componentsByArenaSlot.length];
        Long2ObjectOpenHashMap<ArrayList<Component>> byBrick = new Long2ObjectOpenHashMap<>();
        Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.ints.IntArrayList> shapesByBrick = new Long2ObjectOpenHashMap<>();
        for (int shapeIndex = 0; shapeIndex < shapes.size(); shapeIndex++) {
            LocalAirShape shape = shapes.get(shapeIndex);
            for (int x = shape.minX & ~3; x < shape.minX + LocalAirShape.WIDTH; x += 4) {
                for (int y = shape.minY & ~3; y < shape.minY + LocalAirShape.WIDTH; y += 4) {
                    for (int z = shape.minZ & ~3; z < shape.minZ + LocalAirShape.WIDTH; z += 4) {
                        shapesByBrick.computeIfAbsent(BlockPos.asLong(x, y, z), ignored -> new it.unimi.dsi.fastutil.ints.IntArrayList()).add(shapeIndex);
                    }
                }
            }
        }
        for (int componentIndex = 0; componentIndex < components.length; componentIndex++) {
            Component component = components[componentIndex];
            ArrayList<LocalAirShape> present = new ArrayList<>();
            it.unimi.dsi.fastutil.ints.IntArrayList indices = new it.unimi.dsi.fastutil.ints.IntArrayList(component.coefficientIndices);
            LinkedHashMap<ThermalPageHandle, Long> dependencies = new LinkedHashMap<>();
            for (int index = 0; index < component.dependencies.length; index++) dependencies.put(component.dependencies[index], component.geometryRevisions[index]);
            var candidates = shapesByBrick.get(BlockPos.asLong(component.minX, component.minY, component.minZ));
            for (int candidate = 0; candidates != null && candidate < candidates.size(); candidate++) {
                int shapeIndex = candidates.getInt(candidate);
                LocalAirShape shape = shapes.get(shapeIndex);
                if (!intersects(component, shape)) continue;
                present.add(shape);
                indices.add(coarseCount + shapeIndex);
                for (int index = 0; index < shape.dependencyCount(); index++) dependencies.put(shape.dependency(index), shape.revision(index));
            }
            long[] revisions = new long[dependencies.size()];
            int revisionIndex = 0;
            for (long revision : dependencies.values()) revisions[revisionIndex++] = revision;
            Component next = new Component(component.arenaSlot, component.arenaGeneration, component.minX,
                    component.minY, component.minZ, component.airBlocks, indices.toIntArray(),
                    dependencies.keySet().toArray(ThermalPageHandle[]::new), revisions, present.toArray(LocalAirShape[]::new));
            enriched[componentIndex] = next;
            bySlot[next.arenaSlot] = next;
            byBrick.computeIfAbsent(BlockPos.asLong(next.minX, next.minY, next.minZ), ignored -> new ArrayList<>()).add(next);
        }
        Long2ObjectOpenHashMap<Component[]> index = new Long2ObjectOpenHashMap<>();
        for (var entry : byBrick.long2ObjectEntrySet()) index.put(entry.getLongKey(), entry.getValue().toArray(Component[]::new));
        return new AirFieldLayout(enriched, bySlot, index, coarseCount + shapes.size(), coarseCount, shapes.toArray(LocalAirShape[]::new));
    }

    private static boolean intersects(Component component, LocalAirShape shape) {
        if (component.minX + 4 <= shape.minX || component.minX >= shape.minX + LocalAirShape.WIDTH
                || component.minY + 4 <= shape.minY || component.minY >= shape.minY + LocalAirShape.WIDTH
                || component.minZ + 4 <= shape.minZ || component.minZ >= shape.minZ + LocalAirShape.WIDTH) return false;
        long blocks = component.airBlocks;
        while (blocks != 0) {
            int block = Long.numberOfTrailingZeros(blocks);
            blocks &= blocks - 1;
            if (shape.containsCell(component.minX + (block & 3), component.minY + (block >>> 4), component.minZ + (block >>> 2 & 3))) return true;
        }
        return false;
    }

    /** Geometry and shared corner addresses for one 4-cubed Brick Air component. */
    public static final class Component {
        public final int arenaSlot;
        public final int arenaGeneration;
        public final int minX, minY, minZ;
        public final long airBlocks;
        private final int[] coefficientIndices;
        private final ThermalPageHandle[] dependencies;
        private final long[] geometryRevisions;
        private final LocalAirShape[] shapes;

        public Component(int arenaSlot, int arenaGeneration, int minX, int minY, int minZ,
                long airBlocks, int[] coefficientIndices, ThermalPageHandle[] dependencies,
                long[] geometryRevisions) {
            this(arenaSlot, arenaGeneration, minX, minY, minZ, airBlocks, coefficientIndices,
                    dependencies, geometryRevisions, new LocalAirShape[0]);
        }

        private Component(int arenaSlot, int arenaGeneration, int minX, int minY, int minZ,
                long airBlocks, int[] coefficientIndices, ThermalPageHandle[] dependencies,
                long[] geometryRevisions, LocalAirShape[] shapes) {
            this.arenaSlot = arenaSlot;
            this.arenaGeneration = arenaGeneration;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.airBlocks = airBlocks;
            this.coefficientIndices = coefficientIndices;
            this.dependencies = dependencies;
            this.geometryRevisions = geometryRevisions;
            this.shapes = shapes;
        }

        public int coefficientIndex(int corner) { return coefficientIndices[corner]; }
        public int basisCount() { return coefficientIndices.length; }

        /** Numeric fragment reuse ignores coefficient addresses and publication generations. */
        public boolean sameLocalBasis(Component other) {
            if (other == null || minX != other.minX || minY != other.minY || minZ != other.minZ
                    || airBlocks != other.airBlocks || shapes.length != other.shapes.length) return false;
            for (int index = 0; index < shapes.length; index++) if (!shapes[index].sameValues(other.shapes[index])) return false;
            return true;
        }

        /** The point and face compilers use exactly these same trilinear weights. */
        public double weight(int corner, double x, double y, double z) {
            if (corner >= 8) {
                int cell = airCellAtPoint(x, y, z);
                return cell < 0 ? 0 : shapes[corner - 8].value(minX + (cell & 3), minY + (cell >>> 4), minZ + (cell >>> 2 & 3), x, y, z);
            }
            double relativeX = (x - minX) * 0.25;
            double relativeY = (y - minY) * 0.25;
            double relativeZ = (z - minZ) * 0.25;
            return ((corner & 1) == 0 ? 1 - relativeX : relativeX)
                    * ((corner & 2) == 0 ? 1 - relativeZ : relativeZ)
                    * ((corner & 4) == 0 ? 1 - relativeY : relativeY);
        }

        public double derivative(int corner, int axis, double x, double y, double z) {
            if (corner >= 8) {
                int cell = airCellAtPoint(x, y, z);
                return cell < 0 ? 0 : shapes[corner - 8].derivative(minX + (cell & 3), minY + (cell >>> 4), minZ + (cell >>> 2 & 3), x, y, z, axis);
            }
            double relativeX = (x - minX) * 0.25;
            double relativeY = (y - minY) * 0.25;
            double relativeZ = (z - minZ) * 0.25;
            double wx = (corner & 1) == 0 ? 1 - relativeX : relativeX;
            double wy = (corner & 4) == 0 ? 1 - relativeY : relativeY;
            double wz = (corner & 2) == 0 ? 1 - relativeZ : relativeZ;
            return switch (axis) {
                case 0 -> ((corner & 1) == 0 ? -0.25 : 0.25) * wy * wz;
                case 1 -> wx * ((corner & 4) == 0 ? -0.25 : 0.25) * wz;
                case 2 -> wx * wy * ((corner & 2) == 0 ? -0.25 : 0.25);
                default -> throw new IllegalArgumentException("axis must be 0, 1 or 2");
            };
        }

        public double temperatureOffsetC(double x, double y, double z, double[] coefficientsC) {
            double temperatureC = 0;
            for (int corner = 0; corner < coefficientIndices.length; corner++) {
                temperatureC += weight(corner, x, y, z) * coefficientsC[coefficientIndices[corner]];
            }
            return temperatureC;
        }

        public double minimumOffsetC(double[] coefficientsC) {
            double minimum = Double.POSITIVE_INFINITY;
            for (int index = 0; index < 8; index++) minimum = Math.min(minimum, coefficientsC[coefficientIndices[index]]);
            for (int index = 8; index < coefficientIndices.length; index++) minimum -= Math.abs(coefficientsC[coefficientIndices[index]]);
            return minimum;
        }

        public double maximumOffsetC(double[] coefficientsC) {
            double maximum = Double.NEGATIVE_INFINITY;
            for (int index = 0; index < 8; index++) maximum = Math.max(maximum, coefficientsC[coefficientIndices[index]]);
            for (int index = 8; index < coefficientIndices.length; index++) maximum += Math.abs(coefficientsC[coefficientIndices[index]]);
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
                PagePublication publication = dependencies[index].currentPublication();
                if (publication == null || publication.geometryRevision() != geometryRevisions[index]) return false;
            }
            return true;
        }
    }
}
