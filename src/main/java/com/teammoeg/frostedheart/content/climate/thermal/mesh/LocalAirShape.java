/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.util.Arrays;

/** Static, dimensionless unit-cell trilinear shape. It owns no dynamic temperature. */
public final class LocalAirShape {
    public static final int RADIUS = 6;
    public static final int WIDTH = 2 * RADIUS + 1;
    public final int minX, minY, minZ;
    public final long sourceBlock;
    public final byte sourceFace;
    public final int scale;
    public final int sizeX, sizeY, sizeZ;
    public final long branch;
    private final short[] cellCorners;
    private final float[] values;
    private final ThermalPageHandle[] dependencies;
    private final long[] revisions;

    public LocalAirShape(int minX, int minY, int minZ, long sourceBlock, byte sourceFace, int scale,
            short[] cellCorners, float[] values, ThermalPageHandle[] dependencies, long[] revisions) {
        this(minX, minY, minZ, WIDTH, WIDTH, WIDTH, sourceBlock, sourceFace, scale,
                cellCorners, values, dependencies, revisions);
    }

    public LocalAirShape(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ,
            long sourceBlock, byte sourceFace, int scale, short[] cellCorners, float[] values,
            ThermalPageHandle[] dependencies, long[] revisions) {
        this(minX, minY, minZ, sizeX, sizeY, sizeZ, sourceBlock, sourceFace, scale,
                cellCorners, values, dependencies, revisions, 0);
    }

    private LocalAirShape(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ,
            long sourceBlock, byte sourceFace, int scale, short[] cellCorners, float[] values,
            ThermalPageHandle[] dependencies, long[] revisions, long branch) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sourceBlock = sourceBlock;
        this.sourceFace = sourceFace;
        this.scale = scale;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.branch = branch;
        this.cellCorners = cellCorners;
        this.values = values;
        this.dependencies = dependencies;
        this.revisions = revisions;
    }

    public int dependencyCount() { return dependencies.length; }
    public ThermalPageHandle dependency(int index) { return dependencies[index]; }
    public long revision(int index) { return revisions[index]; }

    public boolean sameValues(LocalAirShape other) {
        return other != null && minX == other.minX && minY == other.minY && minZ == other.minZ
                && cellCorners == other.cellCorners && values == other.values;
    }

    public LocalAirShape withDependencies(ThermalPageHandle[] handles, long[] geometryRevisions) {
        return new LocalAirShape(minX, minY, minZ, sizeX, sizeY, sizeZ, sourceBlock, sourceFace, scale,
                cellCorners, values, handles, geometryRevisions, branch);
    }

    public LocalAirShape withBranch(long branch) {
        return new LocalAirShape(minX, minY, minZ, sizeX, sizeY, sizeZ, sourceBlock, sourceFace, scale,
                cellCorners, values, dependencies, revisions, branch);
    }

    public int cornerCount() { return cellCorners.length; }
    public short cornerIndex(int index) { return cellCorners[index]; }
    public int vertexCount() { return values.length; }
    public float vertexValue(int index) { return values[index]; }

    /** Copy only this section's static shape fragment, preserving its vertex branches. */
    public LocalAirShape sectionSlice(int sectionX, int sectionY, int sectionZ) {
        int x0 = Math.max(minX, sectionX), y0 = Math.max(minY, sectionY), z0 = Math.max(minZ, sectionZ);
        int nx = Math.min(minX + sizeX, sectionX + 16) - x0;
        int ny = Math.min(minY + sizeY, sectionY + 16) - y0;
        int nz = Math.min(minZ + sizeZ, sectionZ + 16) - z0;
        if (nx <= 0 || ny <= 0 || nz <= 0) return null;
        short[] corners = new short[nx * ny * nz * 8];
        Arrays.fill(corners, (short) -1);
        int[] remap = new int[values.length];
        Arrays.fill(remap, -1);
        int vertices = 0;
        for (int cell = 0; cell < nx * ny * nz; cell++) {
            int x = x0 + cell % nx, y = y0 + cell / (nx * nz), z = z0 + cell / nx % nz;
            int original = cellIndex(x, y, z);
            if (cellCorners[original * 8] == (short) -1) continue;
            for (int corner = 0; corner < 8; corner++) {
                int index = Short.toUnsignedInt(cellCorners[original * 8 + corner]);
                if (remap[index] < 0) remap[index] = vertices++;
                corners[cell * 8 + corner] = (short) remap[index];
            }
        }
        float[] slicedValues = new float[vertices];
        for (int index = 0; index < remap.length; index++) if (remap[index] >= 0) slicedValues[remap[index]] = values[index];
        return new LocalAirShape(x0, y0, z0, nx, ny, nz, sourceBlock, sourceFace, scale,
                corners, slicedValues, new ThermalPageHandle[0], new long[0], branch);
    }

    public boolean containsCell(int x, int y, int z) {
        int cell = cellIndex(x, y, z);
        return cell >= 0 && cellCorners[cell * 8] != (short) -1;
    }

    private int cellIndex(int x, int y, int z) {
        int lx = x - minX, ly = y - minY, lz = z - minZ;
        return lx < 0 || ly < 0 || lz < 0 || lx >= sizeX || ly >= sizeY || lz >= sizeZ
                ? -1 : lx + sizeX * (lz + sizeZ * ly);
    }

    public double value(int cellX, int cellY, int cellZ, double x, double y, double z) {
        return evaluate(cellX, cellY, cellZ, x, y, z, -1);
    }

    public double derivative(int cellX, int cellY, int cellZ, double x, double y, double z, int axis) {
        return evaluate(cellX, cellY, cellZ, x, y, z, axis);
    }

    private double evaluate(int cellX, int cellY, int cellZ, double x, double y, double z, int axis) {
        int cell = cellIndex(cellX, cellY, cellZ);
        if (cell < 0 || cellCorners[cell * 8] == (short) -1) return 0;
        double dx = x - cellX, dy = y - cellY, dz = z - cellZ;
        double result = 0;
        for (int corner = 0; corner < 8; corner++) {
            double wx = axis == 0 ? ((corner & 1) == 0 ? -1 : 1) : ((corner & 1) == 0 ? 1 - dx : dx);
            double wy = axis == 1 ? ((corner & 4) == 0 ? -1 : 1) : ((corner & 4) == 0 ? 1 - dy : dy);
            double wz = axis == 2 ? ((corner & 2) == 0 ? -1 : 1) : ((corner & 2) == 0 ? 1 - dz : dz);
            result += wx * wy * wz * values[Short.toUnsignedInt(cellCorners[cell * 8 + corner])];
        }
        return result;
    }
}
