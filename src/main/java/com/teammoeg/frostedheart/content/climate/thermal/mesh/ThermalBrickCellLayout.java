/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;



import java.util.Arrays;
import java.util.Objects;

/** Reusable primitive input layout for one staged 4-cubed Brick. */
public final class ThermalBrickCellLayout {
    enum AirKind {
        NONE,
        REGULAR,
        MIXED
    }

    AirKind airKind = AirKind.NONE;
    int minX;
    int minY;
    int minZ;
    double airCapacityJPerBlockK;
    BlockBrickLayout mixedGeometry;
    final double[] transportCapacityJPerK = new double[64];

    int[] materialBlockX = new int[8];
    int[] materialBlockY = new int[8];
    int[] materialBlockZ = new int[8];
    double[] materialCapacityJPerK = new double[8];
    double[] materialInitialTemperatureC = new double[8];
    int materialCount;

    public void reset(int minX, int minY, int minZ) {
        if (Math.floorMod(minX, 4) != 0
                || Math.floorMod(minY, 4) != 0
                || Math.floorMod(minZ, 4) != 0) {
            throw new IllegalArgumentException("Brick minimum is not aligned");
        }
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        airKind = AirKind.NONE;
        mixedGeometry = null;
        materialCount = 0;
    }

    public void setRegularAir(double capacityJPerBlockK) {
        setAir(AirKind.REGULAR, null, capacityJPerBlockK);
    }

    public void setMixedAir(
            BlockBrickLayout geometry,
            double capacityJPerBlockK
    ) {
        setAir(
                AirKind.MIXED,
                Objects.requireNonNull(geometry, "geometry"),
                capacityJPerBlockK);
    }

    private void setAir(
            AirKind kind,
            BlockBrickLayout geometry,
            double capacityJPerBlockK
    ) {
        if (!Double.isFinite(capacityJPerBlockK)
                || capacityJPerBlockK <= 0.0D) {
            throw new IllegalArgumentException("Brick Air layout is invalid");
        }
        airKind = kind;
        mixedGeometry = geometry;
        airCapacityJPerBlockK = capacityJPerBlockK;
    }

    public void setTransportCapacity(int node, double capacity) {
        transportCapacityJPerK[node] = capacity;
    }

    public void addMaterialPole(
            int blockX,
            int blockY,
            int blockZ,
            double capacityJPerK,
            double initialTemperatureC
    ) {
        if (!Double.isFinite(capacityJPerK)
                || capacityJPerK <= 0.0D
                || !Double.isFinite(initialTemperatureC)) {
            throw new IllegalArgumentException("material pole layout is invalid");
        }
        ensureMaterialCapacity(materialCount + 1);
        materialBlockX[materialCount] = blockX;
        materialBlockY[materialCount] = blockY;
        materialBlockZ[materialCount] = blockZ;
        materialCapacityJPerK[materialCount] = capacityJPerK;
        materialInitialTemperatureC[materialCount] = initialTemperatureC;
        materialCount++;
    }



    void requireReady() {
        if (airKind == AirKind.MIXED && mixedGeometry == null) {
            throw new IllegalStateException("mixed Brick geometry is missing");
        }
    }

    private void ensureMaterialCapacity(int required) {
        if (required <= materialBlockX.length) {
            return;
        }
        int capacity = grow(materialBlockX.length, required);
        materialBlockX = Arrays.copyOf(materialBlockX, capacity);
        materialBlockY = Arrays.copyOf(materialBlockY, capacity);
        materialBlockZ = Arrays.copyOf(materialBlockZ, capacity);
        materialCapacityJPerK = Arrays.copyOf(materialCapacityJPerK, capacity);
        materialInitialTemperatureC = Arrays.copyOf(
                materialInitialTemperatureC, capacity);
    }



    private static int grow(int current, int required) {
        int capacity = Math.max(1, current);
        while (capacity < required) {
            capacity = Math.addExact(
                    capacity, Math.max(4, capacity >>> 1));
        }
        return capacity;
    }
}
