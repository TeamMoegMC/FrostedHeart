/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;

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
    ComponentBrickCompiler.CompiledBrick mixedGeometry;

    int[] materialBlockX = new int[8];
    int[] materialBlockY = new int[8];
    int[] materialBlockZ = new int[8];
    double[] materialCapacityJPerK = new double[8];
    double[] materialInitialTemperatureC = new double[8];
    int materialCount;

    int[] phaseBrickMinX = new int[4];
    int[] phaseBrickMinY = new int[4];
    int[] phaseBrickMinZ = new int[4];
    int[] phaseProfileId = new int[4];
    long[] phaseCandidateMask = new long[4];
    double[] phaseTransitionTemperatureC = new double[4];
    double[] phaseTransitionEnergyJPerUnit = new double[4];
    int phaseCount;

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
        phaseCount = 0;
    }

    public void setRegularAir(double capacityJPerBlockK) {
        setAir(AirKind.REGULAR, null, capacityJPerBlockK);
    }

    public void setMixedAir(
            ComponentBrickCompiler.CompiledBrick geometry,
            double capacityJPerBlockK
    ) {
        setAir(
                AirKind.MIXED,
                Objects.requireNonNull(geometry, "geometry"),
                capacityJPerBlockK);
    }

    private void setAir(
            AirKind kind,
            ComponentBrickCompiler.CompiledBrick geometry,
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

    public void addPhaseReservoir(
            int brickMinX,
            int brickMinY,
            int brickMinZ,
            int profileId,
            long candidateMask,
            double transitionTemperatureC,
            double transitionEnergyJPerUnit
    ) {
        if (profileId <= 0 || candidateMask == 0L
                || !Double.isFinite(transitionTemperatureC)
                || !Double.isFinite(transitionEnergyJPerUnit)
                || transitionEnergyJPerUnit <= 0.0D) {
            throw new IllegalArgumentException("phase reservoir layout is invalid");
        }
        ensurePhaseCapacity(phaseCount + 1);
        phaseBrickMinX[phaseCount] = brickMinX;
        phaseBrickMinY[phaseCount] = brickMinY;
        phaseBrickMinZ[phaseCount] = brickMinZ;
        phaseProfileId[phaseCount] = profileId;
        phaseCandidateMask[phaseCount] = candidateMask;
        phaseTransitionTemperatureC[phaseCount] = transitionTemperatureC;
        phaseTransitionEnergyJPerUnit[phaseCount] = transitionEnergyJPerUnit;
        phaseCount++;
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

    private void ensurePhaseCapacity(int required) {
        if (required <= phaseBrickMinX.length) {
            return;
        }
        int capacity = grow(phaseBrickMinX.length, required);
        phaseBrickMinX = Arrays.copyOf(phaseBrickMinX, capacity);
        phaseBrickMinY = Arrays.copyOf(phaseBrickMinY, capacity);
        phaseBrickMinZ = Arrays.copyOf(phaseBrickMinZ, capacity);
        phaseProfileId = Arrays.copyOf(phaseProfileId, capacity);
        phaseCandidateMask = Arrays.copyOf(phaseCandidateMask, capacity);
        phaseTransitionTemperatureC = Arrays.copyOf(
                phaseTransitionTemperatureC, capacity);
        phaseTransitionEnergyJPerUnit = Arrays.copyOf(
                phaseTransitionEnergyJPerUnit, capacity);
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
