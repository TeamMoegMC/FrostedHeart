/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirCornerTopology;

/**
 * Historical comparison only: one 4-cube's contribution to Q1 Air corners. Adjacent cubes share a physical
 * corner through the field; this object stores only this cube's share of its
 * capacity and enthalpy. Coincident, disconnected corners stay separate.
 * Geometry is immutable; a frame owns the mutable enthalpy array exclusively.
 * No world, neighbouring state, frame, or construction callback is retained.
 */
final class BrickAirState {
    private static final int VERTICES = 125;
    private final long airCells;
    private final double cellCornerCapacity;
    private final byte[] occupancy;
    private final short[] starts;
    private final byte[] vertices, members;
    private final double[] enthalpyJ;

    /**
     * A caller-owned, short-lived view of one physical corner. At most eight
     * incident cell shares participate, including disconnected local branches
     * that connect through a neighbouring cube. Clear after every operation.
     */
    static final class Corner {
        private final BrickAirState[] parts = new BrickAirState[8];
        private final int[] nodes = new int[8];
        private int size;
        private double capacity, enthalpy;

        void add(BrickAirState part, int node) {
            for (int index = 0; index < size; index++) if (parts[index] == part && nodes[index] == node) return;
            parts[size] = part; nodes[size++] = node;
            capacity += part.capacityJPerK(node); enthalpy += part.enthalpyJ(node);
        }

        int partCount() { return size; }
        double capacityJPerK() { return capacity; }
        double enthalpyJ() { return enthalpy; }
        double temperatureC() { return enthalpy / capacity; }

        /** Restore one common temperature, including after a dormant neighbour rejoins. */
        void enthalpyJ(double value) {
            double temperature = value / capacity, remaining = value;
            for (int index = 0; index < size; index++) {
                double share = index == size - 1 ? remaining : temperature * parts[index].capacityJPerK(nodes[index]);
                parts[index].enthalpyJ(nodes[index], share); remaining -= share;
            }
            enthalpy = value;
        }

        void addEnergyJ(double joules) { enthalpyJ(enthalpy + joules); }

        void clear() {
            java.util.Arrays.fill(parts, 0, size, null);
            size = 0; capacity = enthalpy = 0;
        }
    }

    BrickAirState(long airCells, double cellCapacityJPerK, double temperatureC) {
        this.airCells = airCells;
        cellCornerCapacity = cellCapacityJPerK / 8;
        occupancy = new byte[VERTICES]; starts = new short[VERTICES];
        int count = 0;
        for (int y = 0; y <= 4; y++) for (int z = 0; z <= 4; z++) for (int x = 0; x <= 4; x++) {
            int vertex = vertex(x, y, z), mask = 0;
            for (int octant = 0; octant < 8; octant++) {
                int cx = x - (octant & 1), cy = y - (octant >>> 2), cz = z - (octant >>> 1 & 1);
                if (cx >= 0 && cx < 4 && cy >= 0 && cy < 4 && cz >= 0 && cz < 4
                        && (airCells & 1L << (cx | cz << 2 | cy << 4)) != 0) mask |= 1 << octant;
            }
            occupancy[vertex] = (byte) mask;
            starts[vertex] = (short) count;
            count += AirCornerTopology.count(mask);
        }
        vertices = new byte[count]; members = new byte[count]; enthalpyJ = new double[count];
        for (int vertex = 0; vertex < VERTICES; vertex++) {
            int mask = Byte.toUnsignedInt(occupancy[vertex]), start = Short.toUnsignedInt(starts[vertex]);
            for (int group = 0; group < AirCornerTopology.count(mask); group++) {
                int node = start + group;
                vertices[node] = (byte) vertex;
                members[node] = (byte) AirCornerTopology.component(mask, group);
                enthalpyJ[node] = capacityJPerK(node) * temperatureC;
            }
        }
    }

    /** Only immutable geometry arrays are shared; the new frame owns its H. */
    BrickAirState(BrickAirState geometry) {
        airCells = geometry.airCells; cellCornerCapacity = geometry.cellCornerCapacity;
        occupancy = geometry.occupancy; starts = geometry.starts;
        vertices = geometry.vertices; members = geometry.members;
        enthalpyJ = new double[vertices.length];
    }

    /** Local capacity projection; the field subsequently joins changed boundary corners. */
    void projectFrom(BrickAirState before, double ambientC) {
        for (int node = 0; node < nodeCount(); node++) {
            int octants = members(node); double temperatureSum = 0;
            while (octants != 0) {
                int octant = Integer.numberOfTrailingZeros(octants); octants &= octants - 1;
                int old = before == null ? -1 : before.nodeAtVertex(vertexX(node), vertexY(node), vertexZ(node), octant);
                temperatureSum += old < 0 ? ambientC : before.temperatureC(old);
            }
            enthalpyJ[node] = temperatureSum * cellCornerCapacity;
        }
    }

    static long maximumBytes() { return 6144; } // 125 vertex positions, at most four branches per position.

    private static int vertex(int x, int y, int z) { return x + 5 * z + 25 * y; }

    long airCells() { return airCells; }
    int nodeCount() { return enthalpyJ.length; }
    int vertex(int node) { return Byte.toUnsignedInt(vertices[node]); }
    int vertexX(int node) { return vertex(node) % 5; }
    int vertexY(int node) { return vertex(node) / 25; }
    int vertexZ(int node) { return vertex(node) / 5 % 5; }
    int members(int node) { return Byte.toUnsignedInt(members[node]); }
    int occupancy(int x, int y, int z) { return Byte.toUnsignedInt(occupancy[vertex(x, y, z)]); }

    /** The cell's corner bits also identify its incident octant at that vertex. */
    int node(int cell, int corner) {
        if ((airCells & 1L << cell) == 0) return -1;
        int vertex = vertex((cell & 3) + (corner & 1), (cell >>> 4) + (corner >>> 2), (cell >>> 2 & 3) + (corner >>> 1 & 1));
        int group = AirCornerTopology.group(Byte.toUnsignedInt(occupancy[vertex]), corner);
        return Short.toUnsignedInt(starts[vertex]) + group;
    }

    int nodeAtVertex(int x, int y, int z, int incidentOctant) {
        int vertex = vertex(x, y, z);
        int group = AirCornerTopology.group(Byte.toUnsignedInt(occupancy[vertex]), incidentOctant);
        return group < 0 ? -1 : Short.toUnsignedInt(starts[vertex]) + group;
    }

    double capacityJPerK(int node) { return cellCornerCapacity * Integer.bitCount(members(node)); }
    double enthalpyJ(int node) { return enthalpyJ[node]; }
    double temperatureC(int node) { return enthalpyJ[node] / capacityJPerK(node); }
    void enthalpyJ(int node, double value) { enthalpyJ[node] = value; }
    void addEnergyJ(int node, double energy) { enthalpyJ[node] += energy; }

    /** Reuse the other frame's allocation when geometry is unchanged. */
    void copyFrom(BrickAirState accepted) {
        if (airCells != accepted.airCells || cellCornerCapacity != accepted.cellCornerCapacity)
            throw new IllegalArgumentException("Air frame geometry differs");
        System.arraycopy(accepted.enthalpyJ, 0, enthalpyJ, 0, enthalpyJ.length);
    }

    /** A cell owns one eighth of its capacity at each of its eight corners. */
    double cellEnthalpyJ(int cell) {
        if ((airCells & 1L << cell) == 0) return 0;
        double value = 0;
        for (int corner = 0; corner < 8; corner++) value += temperatureC(node(cell, corner)) * cellCornerCapacity;
        return value;
    }

    double sampleC(int cell, double x, double y, double z) {
        if ((airCells & 1L << cell) == 0) return Double.NaN;
        double value = 0;
        for (int corner = 0; corner < 8; corner++) {
            double weight = ((corner & 1) == 0 ? 1 - x : x) * ((corner & 4) == 0 ? 1 - y : y)
                    * ((corner & 2) == 0 ? 1 - z : z);
            value += weight * temperatureC(node(cell, corner));
        }
        return value;
    }

    double totalEnthalpyJ() {
        double value = 0;
        for (double energy : enthalpyJ) value += energy;
        return value;
    }

    long retainedBytes() {
        return 64 + AirBackingMeter.shallowArrayBytes(occupancy) + AirBackingMeter.shallowArrayBytes(starts)
                + AirBackingMeter.shallowArrayBytes(vertices) + AirBackingMeter.shallowArrayBytes(members)
                + AirBackingMeter.shallowArrayBytes(enthalpyJ);
    }
}
