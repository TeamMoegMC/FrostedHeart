/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;

import java.util.Arrays;

/**
 * Primitive structure-of-arrays storage for air cells and sparse material poles.
 *
 * <p>The arena is owned by one logical thermal writer. Page compilation uses
 * {@link ArenaSpan} allocations to install fixed 4x4x4 Brick fragments while
 * enthalpy and capacity remain authoritative here.</p>
 */
public final class ThermalCellArena {
    public static final int NO_SLOT = -1;

    private static final byte FREE = 0;
    private static final byte LIVE = 1;
    private static final byte REGULAR_CELL = 0;
    private static final byte MIXED_COMPONENT = 1;
    private static final byte MATERIAL_SURFACE = 2;
    private static final byte MATERIAL_DEEP = 3;
    private static final byte PHASE_RESERVOIR = 4;
    private static final byte PHASE_REQUEST_IDLE = 0;
    private static final byte PHASE_REQUEST_RETRY = 1;
    private static final byte PHASE_REQUEST_ENQUEUED = 2;

    private double[] enthalpyJ;
    private double[] capacityJPerK;
    private int[] pageSlots;
    private int[] lifecycleGenerations;
    private int[] supportRefs;
    private int[] minimumX;
    private int[] minimumY;
    private int[] minimumZ;
    private int[] mediumIds;
    private byte[] cellFlags;
    private byte[] cellKinds;
    private int[] mixedComponentIds;
    private ComponentBrickCompiler.CompiledBrick[] mixedBrickGeometries;
    private long[] phaseCandidateMasks;
    private double[] phaseTransitionTemperaturesC;
    private double[] phaseTransitionEnergyJPerUnit;
    private double[] phaseReservedEnergyJ;
    private long[] phaseRequestSequences;
    private byte[] phaseRequestCandidateBits;
    private byte[] phaseRequestStates;
    private byte[] allocationState;

    private int highWaterMark;
    private int liveCellCount;

    public ThermalCellArena(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        enthalpyJ = new double[initialCapacity];
        capacityJPerK = new double[initialCapacity];
        pageSlots = new int[initialCapacity];
        lifecycleGenerations = new int[initialCapacity];
        supportRefs = new int[initialCapacity];
        minimumX = new int[initialCapacity];
        minimumY = new int[initialCapacity];
        minimumZ = new int[initialCapacity];
        mediumIds = new int[initialCapacity];
        cellFlags = new byte[initialCapacity];
        cellKinds = new byte[initialCapacity];
        mixedComponentIds = new int[initialCapacity];
        mixedBrickGeometries = new ComponentBrickCompiler.CompiledBrick[initialCapacity];
        phaseCandidateMasks = new long[initialCapacity];
        phaseTransitionTemperaturesC = new double[initialCapacity];
        phaseTransitionEnergyJPerUnit = new double[initialCapacity];
        phaseReservedEnergyJ = new double[initialCapacity];
        phaseRequestSequences = new long[initialCapacity];
        phaseRequestCandidateBits = new byte[initialCapacity];
        phaseRequestStates = new byte[initialCapacity];
        allocationState = new byte[initialCapacity];
        Arrays.fill(pageSlots, NO_SLOT);
        Arrays.fill(supportRefs, NO_SLOT);
        Arrays.fill(mediumIds, -1);
        Arrays.fill(mixedComponentIds, NO_SLOT);
    }

    public int highWaterMark() {
        return highWaterMark;
    }

    public int liveCellCount() {
        return liveCellCount;
    }

    public boolean isLive(int slot) {
        return slot >= 0 && slot < highWaterMark && allocationState[slot] == LIVE;
    }

    /** Allocates all air, material, and Brick-local phase state for one Page. */
    public PageAllocation allocatePageCells(
            int pageSlot,
            int lifecycleGeneration,
            CellSpec[] regularCells,
            MixedBrickSpec[] mixedBricks,
            MaterialPoleSpec[] materialPoles,
            PhaseReservoirSpec[] phaseReservoirs,
            double initialTemperatureC,
            double referenceTemperatureC
    ) {
        requireLifecycleGeneration(lifecycleGeneration);
        requireFinite("initialTemperatureC", initialTemperatureC);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        CellSpec[] regular = validateLayout(pageSlot, regularCells);
        MixedBrickSpec[] mixed = validateMixedLayout(pageSlot, regular, mixedBricks);
        MaterialPoleSpec[] materials = validateMaterialLayout(
                pageSlot, regular, mixed, materialPoles);
        PhaseReservoirSpec[] phases = validatePhaseLayout(pageSlot, phaseReservoirs);
        double temperatureOffset = initialTemperatureC - referenceTemperatureC;
        requireFinite("initial temperature offset", temperatureOffset);

        int totalCells = regular.length;
        for (MixedBrickSpec brick : mixed) {
            totalCells = Math.addExact(totalCells, brick.geometry().componentCount());
        }
        totalCells = Math.addExact(totalCells, materials.length);
        totalCells = Math.addExact(totalCells, phases.length);
        if (totalCells == 0) {
            return new PageAllocation(
                    ArenaSpan.EMPTY, new int[0], new int[0], new int[0]);
        }

        int firstSlot = findFreeSpan(totalCells);
        int required = Math.addExact(firstSlot, totalCells);
        ensureCapacity(required);
        int write = firstSlot;
        for (CellSpec cell : regular) {
            writeRegularCell(
                    write++, pageSlot, lifecycleGeneration, cell,
                    finiteProduct("initial enthalpy", cell.capacityJPerK(), temperatureOffset));
        }
        int[] mixedSupportRefs = new int[mixed.length];
        for (int brickIndex = 0; brickIndex < mixed.length; brickIndex++) {
            MixedBrickSpec brick = mixed[brickIndex];
            int supportRef = write;
            mixedSupportRefs[brickIndex] = supportRef;
            mixedBrickGeometries[supportRef] = brick.geometry();
            for (int component = 0; component < brick.geometry().componentCount(); component++) {
                double capacity = finiteProduct(
                        "mixed component capacity",
                        brick.effectiveVolumetricCapacityJPerBlockK(),
                        brick.geometry().componentVolume(component));
                writeMixedComponent(
                        write++, pageSlot, lifecycleGeneration, supportRef,
                        brick, component, capacity,
                        finiteProduct("initial enthalpy", capacity, temperatureOffset));
            }
        }
        int[] materialPoleSlots = new int[materials.length];
        for (int materialIndex = 0; materialIndex < materials.length; materialIndex++) {
            MaterialPoleSpec material = materials[materialIndex];
            materialPoleSlots[materialIndex] = write;
            double materialOffset = material.initialTemperatureC() - referenceTemperatureC;
            requireFinite("initial material temperature offset", materialOffset);
            writeMaterialPole(
                    write++, pageSlot, lifecycleGeneration, material,
                    finiteProduct(
                            "initial material enthalpy",
                            material.capacityJPerK(),
                            materialOffset));
        }
        int[] phaseReservoirSlots = new int[phases.length];
        for (int phaseIndex = 0; phaseIndex < phases.length; phaseIndex++) {
            phaseReservoirSlots[phaseIndex] = write;
            writePhaseReservoir(
                    write++, pageSlot, lifecycleGeneration, phases[phaseIndex]);
        }
        highWaterMark = Math.max(highWaterMark, required);
        liveCellCount = Math.addExact(liveCellCount, totalCells);
        return new PageAllocation(
                new ArenaSpan(firstSlot, totalCells),
                mixedSupportRefs,
                materialPoleSlots,
                phaseReservoirSlots);
    }

    public double enthalpyJ(int slot) {
        requireLiveSlot(slot);
        return enthalpyJ[slot];
    }

    public double capacityJPerK(int slot) {
        requireLiveSlot(slot);
        return capacityJPerK[slot];
    }

    public double temperatureC(int slot, double referenceTemperatureC) {
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireLiveSlot(slot);
        if (cellKinds[slot] == PHASE_RESERVOIR) {
            return phaseTransitionTemperaturesC[slot];
        }
        double temperature = referenceTemperatureC + enthalpyJ[slot] / capacityJPerK[slot];
        requireFinite("cell temperature", temperature);
        return temperature;
    }

    public void setEnthalpyJ(int slot, double value) {
        requireFinite("enthalpyJ", value);
        requireLiveSlot(slot);
        enthalpyJ[slot] = value;
    }

    public void addEnthalpyJ(int slot, double deltaJ) {
        requireFinite("deltaJ", deltaJ);
        requireLiveSlot(slot);
        double result = enthalpyJ[slot] + deltaJ;
        requireFinite("updated enthalpy", result);
        enthalpyJ[slot] = result;
    }

    public int pageSlot(int slot) {
        requireLiveSlot(slot);
        return pageSlots[slot];
    }

    public int lifecycleGeneration(int slot) {
        requireLiveSlot(slot);
        return lifecycleGenerations[slot];
    }

    /** Adds energy only when the source binding still names this exact cell incarnation. */
    public void addNodeEnthalpyJ(long nodeId, int lifecycleGeneration, double deltaJ) {
        int slot = requireNodeTarget(nodeId, lifecycleGeneration);
        addEnthalpyJ(slot, deltaJ);
    }

    /** Preflights one source delivery so a batch can validate before writing. */
    public void requireNodeEnthalpyWrite(
            long nodeId,
            int lifecycleGeneration,
            double deltaJ
    ) {
        int slot = requireNodeTarget(nodeId, lifecycleGeneration);
        requireFinite("deltaJ", deltaJ);
        requireFinite("updated enthalpy", enthalpyJ[slot] + deltaJ);
    }

    /** Validates a source binding without mutating the destination. */
    public int requireNodeTarget(long nodeId, int lifecycleGeneration) {
        if (nodeId < 0L || nodeId > Integer.MAX_VALUE) {
            throw new IllegalStateException("thermal node ID is not an arena slot: " + nodeId);
        }
        int slot = (int) nodeId;
        requireLiveSlot(slot);
        if (lifecycleGenerations[slot] != lifecycleGeneration) {
            throw new IllegalStateException(
                    "stale thermal node generation for slot " + slot);
        }
        return slot;
    }

    /** Phase C regular supports use their arena slot as the wide support ref. */
    public int supportRef(int slot) {
        requireLiveSlot(slot);
        return supportRefs[slot];
    }

    public int minimumX(int slot) {
        requireLiveSlot(slot);
        return minimumX[slot];
    }

    public int minimumY(int slot) {
        requireLiveSlot(slot);
        return minimumY[slot];
    }

    public int minimumZ(int slot) {
        requireLiveSlot(slot);
        return minimumZ[slot];
    }

    public boolean isMixedComponent(int slot) {
        requireLiveSlot(slot);
        return cellKinds[slot] == MIXED_COMPONENT;
    }

    public boolean isMaterialPole(int slot) {
        requireLiveSlot(slot);
        return isMaterialKind(cellKinds[slot]);
    }

    public int materialProfileId(int slot) {
        if (!isMaterialPole(slot)) {
            throw new IllegalArgumentException("slot is not a material pole: " + slot);
        }
        return mediumIds[slot];
    }

    public boolean isPhaseReservoir(int slot) {
        requireLiveSlot(slot);
        return cellKinds[slot] == PHASE_RESERVOIR;
    }

    public int phaseProfileId(int slot) {
        requirePhaseReservoir(slot);
        return mediumIds[slot];
    }

    public long phaseCandidateMask(int slot) {
        requirePhaseReservoir(slot);
        return phaseCandidateMasks[slot];
    }

    public double phaseTransitionTemperatureC(int slot) {
        requirePhaseReservoir(slot);
        return phaseTransitionTemperaturesC[slot];
    }

    public double phaseTransitionEnergyJPerUnit(int slot) {
        requirePhaseReservoir(slot);
        return phaseTransitionEnergyJPerUnit[slot];
    }

    public double phaseAvailableEnergyJ(int slot) {
        requirePhaseReservoir(slot);
        return enthalpyJ[slot] - phaseReservedEnergyJ[slot];
    }

    public double phaseMaximumEnergyJ(int slot) {
        requirePhaseReservoir(slot);
        return Long.bitCount(phaseCandidateMasks[slot])
                * phaseTransitionEnergyJPerUnit[slot];
    }

    public boolean phaseRequestOutstanding(int slot) {
        requirePhaseReservoir(slot);
        return phaseRequestStates[slot] != PHASE_REQUEST_IDLE;
    }

    public boolean phaseRequestNeedsOffer(int slot) {
        requirePhaseReservoir(slot);
        return phaseRequestStates[slot] == PHASE_REQUEST_RETRY;
    }

    public long phaseRequestSequence(int slot) {
        requirePhaseReservoir(slot);
        return phaseRequestSequences[slot];
    }

    public int phaseRequestCandidateBit(int slot) {
        requirePhaseReservoir(slot);
        return phaseRequestStates[slot] == PHASE_REQUEST_IDLE
                ? -1 : Byte.toUnsignedInt(phaseRequestCandidateBits[slot]);
    }

    public void beginPhaseRequest(
            int slot,
            long requestSequence,
            int candidateBit
    ) {
        requirePhaseReservoir(slot);
        if (phaseRequestStates[slot] != PHASE_REQUEST_IDLE
                || requestSequence <= phaseRequestSequences[slot]
                || candidateBit < 0 || candidateBit >= Long.SIZE
                || (phaseCandidateMasks[slot] & (1L << candidateBit)) == 0L
                || phaseAvailableEnergyJ(slot) + 1.0e-12D
                < phaseTransitionEnergyJPerUnit[slot]) {
            throw new IllegalStateException("phase request cannot be reserved");
        }
        phaseReservedEnergyJ[slot] = phaseTransitionEnergyJPerUnit[slot];
        phaseRequestSequences[slot] = requestSequence;
        phaseRequestCandidateBits[slot] = (byte) candidateBit;
        phaseRequestStates[slot] = PHASE_REQUEST_RETRY;
    }

    public void markPhaseRequestEnqueued(int slot, long requestSequence) {
        requireCurrentPhaseRequest(slot, requestSequence);
        phaseRequestStates[slot] = PHASE_REQUEST_ENQUEUED;
    }

    public void retryPhaseRequest(int slot, long requestSequence) {
        requireCurrentPhaseRequest(slot, requestSequence);
        phaseRequestStates[slot] = PHASE_REQUEST_RETRY;
    }

    public double completePhaseRequest(
            int slot,
            long requestSequence,
            boolean mutationApplied
    ) {
        requireCurrentPhaseRequest(slot, requestSequence);
        double reserved = phaseReservedEnergyJ[slot];
        if (mutationApplied) {
            double next = enthalpyJ[slot] - reserved;
            if (!Double.isFinite(next) || next < -1.0e-9D) {
                throw new IllegalStateException("phase completion exceeds stored energy");
            }
            enthalpyJ[slot] = Math.max(0.0D, next);
        }
        phaseReservedEnergyJ[slot] = 0.0D;
        phaseRequestCandidateBits[slot] = 0;
        phaseRequestStates[slot] = PHASE_REQUEST_IDLE;
        return mutationApplied ? reserved : 0.0D;
    }

    public void copyPhaseRequestState(int oldSlot, int newSlot) {
        requirePhaseReservoir(oldSlot);
        requirePhaseReservoir(newSlot);
        if (phaseProfileId(oldSlot) != phaseProfileId(newSlot)
                || minimumX[oldSlot] != minimumX[newSlot]
                || minimumY[oldSlot] != minimumY[newSlot]
                || minimumZ[oldSlot] != minimumZ[newSlot]) {
            throw new IllegalArgumentException("phase reservoir ownership key changed");
        }
        phaseReservedEnergyJ[newSlot] = phaseReservedEnergyJ[oldSlot];
        phaseRequestSequences[newSlot] = phaseRequestSequences[oldSlot];
        phaseRequestCandidateBits[newSlot] = phaseRequestCandidateBits[oldSlot];
        phaseRequestStates[newSlot] = phaseRequestStates[oldSlot];
    }

    public boolean isMixedSupport(int supportRef) {
        return isLive(supportRef)
                && cellKinds[supportRef] == MIXED_COMPONENT
                && supportRefs[supportRef] == supportRef
                && mixedBrickGeometries[supportRef] != null;
    }

    public int mixedComponentSlot(int supportRef, int componentId) {
        ComponentBrickCompiler.CompiledBrick geometry = mixedGeometry(supportRef);
        if (componentId < 0 || componentId >= geometry.componentCount()) {
            throw new IllegalArgumentException("mixed component ID is out of bounds");
        }
        int slot = Math.addExact(supportRef, componentId);
        requireMixedComponent(slot);
        if (supportRefs[slot] != supportRef || mixedComponentIds[slot] != componentId) {
            throw new IllegalStateException("mixed component span is not dense");
        }
        return slot;
    }

    public double centerX(int slot) {
        requireLiveSlot(slot);
        if (isMaterialKind(cellKinds[slot])) {
            return minimumX[slot] + 0.5D;
        }
        if (cellKinds[slot] == REGULAR_CELL) {
            return minimumX[slot] + 2.0D;
        }
        ComponentBrickCompiler.CompiledBrick geometry = mixedGeometry(supportRefs[slot]);
        return minimumX[slot] + geometry.componentCentroidX(mixedComponentIds[slot]);
    }

    public double centerY(int slot) {
        requireLiveSlot(slot);
        if (isMaterialKind(cellKinds[slot])) {
            return minimumY[slot] + 0.5D;
        }
        if (cellKinds[slot] == REGULAR_CELL) {
            return minimumY[slot] + 2.0D;
        }
        ComponentBrickCompiler.CompiledBrick geometry = mixedGeometry(supportRefs[slot]);
        return minimumY[slot] + geometry.componentCentroidY(mixedComponentIds[slot]);
    }

    public double centerZ(int slot) {
        requireLiveSlot(slot);
        if (isMaterialKind(cellKinds[slot])) {
            return minimumZ[slot] + 0.5D;
        }
        if (cellKinds[slot] == REGULAR_CELL) {
            return minimumZ[slot] + 2.0D;
        }
        ComponentBrickCompiler.CompiledBrick geometry = mixedGeometry(supportRefs[slot]);
        return minimumZ[slot] + geometry.componentCentroidZ(mixedComponentIds[slot]);
    }

    ComponentBrickCompiler.CompiledBrick mixedGeometry(int supportRef) {
        if (!isMixedSupport(supportRef)) {
            throw new IllegalArgumentException("slot is not a mixed-Brick support: " + supportRef);
        }
        return mixedBrickGeometries[supportRef];
    }

    public int mediumId(int slot) {
        requireLiveSlot(slot);
        return mediumIds[slot];
    }

    public int flags(int slot) {
        requireLiveSlot(slot);
        return Byte.toUnsignedInt(cellFlags[slot]);
    }

    /** Releases one retired Page span after its replacement sweep is installed. */
    public void releasePageCells(
            int expectedPageSlot,
            int expectedLifecycleGeneration,
            ArenaSpan span
    ) {
        if (span == null) {
            throw new IllegalArgumentException("span is required");
        }
        if (span.count() == 0) {
            return;
        }
        requireLifecycleGeneration(expectedLifecycleGeneration);
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            requireLiveSlot(slot);
            if (pageSlots[slot] != expectedPageSlot
                    || lifecycleGenerations[slot] != expectedLifecycleGeneration) {
                throw new IllegalArgumentException(
                        "Page release does not own the complete arena span");
            }
        }
        releaseSpan(span);
    }

    private CellSpec[] validateLayout(int pageSlot, CellSpec[] cells) {
        if (pageSlot < 0) {
            throw new IllegalArgumentException("pageSlot must be non-negative");
        }
        if (cells == null) {
            throw new IllegalArgumentException("cells are required");
        }
        CellSpec[] copy = cells.clone();
        if (copy.length == 0) {
            return copy;
        }
        int sectionX = Math.floorDiv(requireCellSpec(copy[0]).minX(), 16);
        int sectionY = Math.floorDiv(copy[0].minY(), 16);
        int sectionZ = Math.floorDiv(copy[0].minZ(), 16);
        for (int index = 0; index < copy.length; index++) {
            CellSpec cell = requireCellSpec(copy[index]);
            if (Math.floorDiv(cell.minX(), 16) != sectionX
                    || Math.floorDiv(cell.minY(), 16) != sectionY
                    || Math.floorDiv(cell.minZ(), 16) != sectionZ
                    || cell.maxXExclusive() > (sectionX + 1) * 16
                    || cell.maxYExclusive() > (sectionY + 1) * 16
                    || cell.maxZExclusive() > (sectionZ + 1) * 16) {
                throw new IllegalArgumentException(
                        "all Page cells must fit inside one 16-block section");
            }
        }
        requireNonOverlapping(copy);
        return copy;
    }

    private static MixedBrickSpec[] validateMixedLayout(
            int pageSlot,
            CellSpec[] regular,
            MixedBrickSpec[] mixedBricks
    ) {
        if (pageSlot < 0) {
            throw new IllegalArgumentException("pageSlot must be non-negative");
        }
        if (mixedBricks == null) {
            throw new IllegalArgumentException("mixedBricks are required");
        }
        MixedBrickSpec[] mixed = mixedBricks.clone();
        for (int index = 0; index < mixed.length; index++) {
            if (mixed[index] == null) {
                throw new IllegalArgumentException("mixed Brick specification is required");
            }
        }
        if (regular.length == 0 && mixed.length == 0) {
            return mixed;
        }

        int anchorX = regular.length == 0 ? mixed[0].minX() : regular[0].minX();
        int anchorY = regular.length == 0 ? mixed[0].minY() : regular[0].minY();
        int anchorZ = regular.length == 0 ? mixed[0].minZ() : regular[0].minZ();
        int sectionX = Math.floorDiv(anchorX, 16);
        int sectionY = Math.floorDiv(anchorY, 16);
        int sectionZ = Math.floorDiv(anchorZ, 16);
        for (MixedBrickSpec brick : mixed) {
            if (Math.floorDiv(brick.minX(), 16) != sectionX
                    || Math.floorDiv(brick.minY(), 16) != sectionY
                    || Math.floorDiv(brick.minZ(), 16) != sectionZ
                    || brick.minX() + 4 > (sectionX + 1) * 16
                    || brick.minY() + 4 > (sectionY + 1) * 16
                    || brick.minZ() + 4 > (sectionZ + 1) * 16) {
                throw new IllegalArgumentException(
                        "all Page supports must fit inside one 16-block section");
            }
        }
        for (CellSpec cell : regular) {
            for (MixedBrickSpec brick : mixed) {
                if (overlap(cell.minX(), cell.maxXExclusive(), brick.minX(), brick.minX() + 4)
                        && overlap(cell.minY(), cell.maxYExclusive(), brick.minY(), brick.minY() + 4)
                        && overlap(cell.minZ(), cell.maxZExclusive(), brick.minZ(), brick.minZ() + 4)) {
                    throw new IllegalArgumentException(
                            "regular cells and mixed Brick supports cannot overlap");
                }
            }
        }
        for (int first = 0; first < mixed.length; first++) {
            for (int second = first + 1; second < mixed.length; second++) {
                MixedBrickSpec left = mixed[first];
                MixedBrickSpec right = mixed[second];
                if (overlap(left.minX(), left.minX() + 4, right.minX(), right.minX() + 4)
                        && overlap(left.minY(), left.minY() + 4, right.minY(), right.minY() + 4)
                        && overlap(left.minZ(), left.minZ() + 4, right.minZ(), right.minZ() + 4)) {
                    throw new IllegalArgumentException("mixed Brick supports cannot overlap");
                }
            }
        }
        return mixed;
    }

    private static MaterialPoleSpec[] validateMaterialLayout(
            int pageSlot,
            CellSpec[] regular,
            MixedBrickSpec[] mixed,
            MaterialPoleSpec[] materialPoles
    ) {
        if (pageSlot < 0) {
            throw new IllegalArgumentException("pageSlot must be non-negative");
        }
        if (materialPoles == null) {
            throw new IllegalArgumentException("materialPoles are required");
        }
        MaterialPoleSpec[] materials = materialPoles.clone();
        for (MaterialPoleSpec material : materials) {
            if (material == null) {
                throw new IllegalArgumentException("material pole specification is required");
            }
        }
        if (materials.length == 0) {
            return materials;
        }

        int anchorX = regular.length != 0 ? regular[0].minX()
                : mixed.length != 0 ? mixed[0].minX() : materials[0].blockX();
        int anchorY = regular.length != 0 ? regular[0].minY()
                : mixed.length != 0 ? mixed[0].minY() : materials[0].blockY();
        int anchorZ = regular.length != 0 ? regular[0].minZ()
                : mixed.length != 0 ? mixed[0].minZ() : materials[0].blockZ();
        int sectionX = Math.floorDiv(anchorX, 16);
        int sectionY = Math.floorDiv(anchorY, 16);
        int sectionZ = Math.floorDiv(anchorZ, 16);
        for (MaterialPoleSpec material : materials) {
            if (Math.floorDiv(material.blockX(), 16) != sectionX
                    || Math.floorDiv(material.blockY(), 16) != sectionY
                    || Math.floorDiv(material.blockZ(), 16) != sectionZ) {
                throw new IllegalArgumentException(
                        "all material poles must belong to the Page section");
            }
        }
        return materials;
    }

    private static PhaseReservoirSpec[] validatePhaseLayout(
            int pageSlot,
            PhaseReservoirSpec[] phaseReservoirs
    ) {
        if (pageSlot < 0 || phaseReservoirs == null) {
            throw new IllegalArgumentException("Page and phase reservoir layout are required");
        }
        PhaseReservoirSpec[] phases = phaseReservoirs.clone();
        for (PhaseReservoirSpec phase : phases) {
            if (phase == null) {
                throw new IllegalArgumentException("phase reservoir specification is required");
            }
        }
        return phases;
    }

    private static void requireNonOverlapping(CellSpec[] cells) {
        for (int first = 0; first < cells.length; first++) {
            CellSpec left = cells[first];
            for (int second = first + 1; second < cells.length; second++) {
                CellSpec right = cells[second];
                if (overlap(left.minX(), left.maxXExclusive(), right.minX(), right.maxXExclusive())
                        && overlap(left.minY(), left.maxYExclusive(), right.minY(), right.maxYExclusive())
                        && overlap(left.minZ(), left.maxZExclusive(), right.minZ(), right.maxZExclusive())) {
                    throw new IllegalArgumentException("regular Page cells cannot overlap");
                }
            }
        }
    }

    private static boolean overlap(int minimumA, int maximumA, int minimumB, int maximumB) {
        return Math.max(minimumA, minimumB) < Math.min(maximumA, maximumB);
    }

    private int findFreeSpan(int count) {
        int runStart = 0;
        int runLength = 0;
        for (int slot = 0; slot < highWaterMark; slot++) {
            if (allocationState[slot] == FREE) {
                if (runLength == 0) {
                    runStart = slot;
                }
                runLength++;
                if (runLength == count) {
                    return runStart;
                }
            } else {
                runLength = 0;
            }
        }
        return highWaterMark;
    }

    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity <= allocationState.length) {
            return;
        }
        int oldCapacity = allocationState.length;
        int grown = Math.max(8, oldCapacity);
        while (grown < requiredCapacity) {
            int next = grown + Math.max(8, grown >>> 1);
            if (next < 0 || next < grown) {
                grown = Integer.MAX_VALUE;
                break;
            }
            grown = next;
        }
        if (grown < requiredCapacity) {
            throw new IllegalStateException("thermal cell arena exceeded int address space");
        }
        enthalpyJ = Arrays.copyOf(enthalpyJ, grown);
        capacityJPerK = Arrays.copyOf(capacityJPerK, grown);
        pageSlots = Arrays.copyOf(pageSlots, grown);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, grown);
        supportRefs = Arrays.copyOf(supportRefs, grown);
        minimumX = Arrays.copyOf(minimumX, grown);
        minimumY = Arrays.copyOf(minimumY, grown);
        minimumZ = Arrays.copyOf(minimumZ, grown);
        mediumIds = Arrays.copyOf(mediumIds, grown);
        cellFlags = Arrays.copyOf(cellFlags, grown);
        cellKinds = Arrays.copyOf(cellKinds, grown);
        mixedComponentIds = Arrays.copyOf(mixedComponentIds, grown);
        mixedBrickGeometries = Arrays.copyOf(mixedBrickGeometries, grown);
        phaseCandidateMasks = Arrays.copyOf(phaseCandidateMasks, grown);
        phaseTransitionTemperaturesC = Arrays.copyOf(
                phaseTransitionTemperaturesC, grown);
        phaseTransitionEnergyJPerUnit = Arrays.copyOf(
                phaseTransitionEnergyJPerUnit, grown);
        phaseReservedEnergyJ = Arrays.copyOf(phaseReservedEnergyJ, grown);
        phaseRequestSequences = Arrays.copyOf(phaseRequestSequences, grown);
        phaseRequestCandidateBits = Arrays.copyOf(phaseRequestCandidateBits, grown);
        phaseRequestStates = Arrays.copyOf(phaseRequestStates, grown);
        allocationState = Arrays.copyOf(allocationState, grown);
        Arrays.fill(pageSlots, oldCapacity, grown, NO_SLOT);
        Arrays.fill(supportRefs, oldCapacity, grown, NO_SLOT);
        Arrays.fill(mediumIds, oldCapacity, grown, -1);
        Arrays.fill(mixedComponentIds, oldCapacity, grown, NO_SLOT);
    }

    private void releaseSpan(ArenaSpan span) {
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            requireLiveSlot(slot);
        }
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            allocationState[slot] = FREE;
            enthalpyJ[slot] = 0.0D;
            capacityJPerK[slot] = 0.0D;
            pageSlots[slot] = NO_SLOT;
            lifecycleGenerations[slot] = 0;
            supportRefs[slot] = NO_SLOT;
            minimumX[slot] = 0;
            minimumY[slot] = 0;
            minimumZ[slot] = 0;
            mediumIds[slot] = -1;
            cellFlags[slot] = 0;
            cellKinds[slot] = REGULAR_CELL;
            mixedComponentIds[slot] = NO_SLOT;
            mixedBrickGeometries[slot] = null;
            phaseCandidateMasks[slot] = 0L;
            phaseTransitionTemperaturesC[slot] = 0.0D;
            phaseTransitionEnergyJPerUnit[slot] = 0.0D;
            phaseReservedEnergyJ[slot] = 0.0D;
            phaseRequestSequences[slot] = 0L;
            phaseRequestCandidateBits[slot] = 0;
            phaseRequestStates[slot] = PHASE_REQUEST_IDLE;
        }
        liveCellCount -= span.count();
        while (highWaterMark > 0 && allocationState[highWaterMark - 1] == FREE) {
            highWaterMark--;
        }
    }

    private void requireLiveSlot(int slot) {
        if (!isLive(slot)) {
            throw new IllegalArgumentException("cell slot is not live: " + slot);
        }
    }

    private void requireMixedComponent(int slot) {
        requireLiveSlot(slot);
        if (cellKinds[slot] != MIXED_COMPONENT) {
            throw new IllegalArgumentException("slot is not a mixed component: " + slot);
        }
    }

    private void requirePhaseReservoir(int slot) {
        requireLiveSlot(slot);
        if (cellKinds[slot] != PHASE_RESERVOIR) {
            throw new IllegalArgumentException("slot is not a phase reservoir: " + slot);
        }
    }

    private void requireCurrentPhaseRequest(int slot, long requestSequence) {
        requirePhaseReservoir(slot);
        if (phaseRequestStates[slot] == PHASE_REQUEST_IDLE
                || phaseRequestSequences[slot] != requestSequence) {
            throw new IllegalStateException("phase request is not current");
        }
    }

    private void writeRegularCell(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            CellSpec cell,
            double initialEnthalpyJ
    ) {
        allocationState[slot] = LIVE;
        enthalpyJ[slot] = initialEnthalpyJ;
        capacityJPerK[slot] = cell.capacityJPerK();
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = cell.minX();
        minimumY[slot] = cell.minY();
        minimumZ[slot] = cell.minZ();
        mediumIds[slot] = cell.mediumId();
        cellFlags[slot] = (byte) cell.flags();
        cellKinds[slot] = REGULAR_CELL;
        mixedComponentIds[slot] = NO_SLOT;
        mixedBrickGeometries[slot] = null;
    }

    private void writeMixedComponent(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int supportRef,
            MixedBrickSpec brick,
            int componentId,
            double capacity,
            double initialEnthalpyJ
    ) {
        allocationState[slot] = LIVE;
        enthalpyJ[slot] = initialEnthalpyJ;
        capacityJPerK[slot] = capacity;
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = supportRef;
        minimumX[slot] = brick.minX();
        minimumY[slot] = brick.minY();
        minimumZ[slot] = brick.minZ();
        mediumIds[slot] = brick.mediumId();
        cellFlags[slot] = (byte) brick.flags();
        cellKinds[slot] = MIXED_COMPONENT;
        mixedComponentIds[slot] = componentId;
    }

    private void writeMaterialPole(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            MaterialPoleSpec material,
            double initialEnthalpyJ
    ) {
        allocationState[slot] = LIVE;
        enthalpyJ[slot] = initialEnthalpyJ;
        capacityJPerK[slot] = material.capacityJPerK();
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = material.blockX();
        minimumY[slot] = material.blockY();
        minimumZ[slot] = material.blockZ();
        mediumIds[slot] = material.materialProfileId();
        cellFlags[slot] = 0;
        cellKinds[slot] = material.depth() == MaterialPoleDepth.SURFACE
                ? MATERIAL_SURFACE : MATERIAL_DEEP;
        mixedComponentIds[slot] = NO_SLOT;
        mixedBrickGeometries[slot] = null;
    }

    private void writePhaseReservoir(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            PhaseReservoirSpec phase
    ) {
        allocationState[slot] = LIVE;
        enthalpyJ[slot] = 0.0D;
        capacityJPerK[slot] = phase.transitionEnergyJPerUnit();
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = phase.brickMinX();
        minimumY[slot] = phase.brickMinY();
        minimumZ[slot] = phase.brickMinZ();
        mediumIds[slot] = phase.materialProfileId();
        cellFlags[slot] = 0;
        cellKinds[slot] = PHASE_RESERVOIR;
        mixedComponentIds[slot] = NO_SLOT;
        mixedBrickGeometries[slot] = null;
        phaseCandidateMasks[slot] = phase.candidateMask();
        phaseTransitionTemperaturesC[slot] = phase.transitionTemperatureC();
        phaseTransitionEnergyJPerUnit[slot] = phase.transitionEnergyJPerUnit();
        phaseReservedEnergyJ[slot] = 0.0D;
        phaseRequestSequences[slot] = 0L;
        phaseRequestCandidateBits[slot] = 0;
        phaseRequestStates[slot] = PHASE_REQUEST_IDLE;
    }

    private static CellSpec requireCellSpec(CellSpec cell) {
        if (cell == null) {
            throw new IllegalArgumentException("cell specification is required");
        }
        return cell;
    }

    private static double finiteProduct(String name, double left, double right) {
        double result = left * right;
        requireFinite(name, result);
        return result;
    }

    private static double finiteSum(String name, double left, double right) {
        double result = left + right;
        requireFinite(name, result);
        return result;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireLifecycleGeneration(int lifecycleGeneration) {
        if (lifecycleGeneration < 0) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
    }

    public record CellSpec(
            int minX,
            int minY,
            int minZ,
            int mediumId,
            int flags,
            double capacityJPerK
    ) {
        public CellSpec {
            if (Math.floorMod(minX, 4) != 0
                    || Math.floorMod(minY, 4) != 0
                    || Math.floorMod(minZ, 4) != 0) {
                throw new IllegalArgumentException(
                        "regular cell minimum must be aligned to 4 blocks");
            }
            Math.addExact(minX, 4);
            Math.addExact(minY, 4);
            Math.addExact(minZ, 4);
            if (mediumId < 0) {
                throw new IllegalArgumentException("mediumId must be non-negative");
            }
            if (flags < 0 || flags > 0xff) {
                throw new IllegalArgumentException("flags must fit an unsigned byte");
            }
            if (!Double.isFinite(capacityJPerK) || capacityJPerK <= 0.0D) {
                throw new IllegalArgumentException(
                        "capacityJPerK must be finite and positive");
            }
        }

        public static CellSpec regularAir(
                int minX,
                int minY,
                int minZ,
                int mediumId,
                int flags,
                double effectiveVolumetricCapacityJPerBlockK
        ) {
            if (!Double.isFinite(effectiveVolumetricCapacityJPerBlockK)
                    || effectiveVolumetricCapacityJPerBlockK <= 0.0D) {
                throw new IllegalArgumentException(
                        "effective volumetric capacity must be finite and positive");
            }
            return new CellSpec(
                    minX,
                    minY,
                    minZ,
                    mediumId,
                    flags,
                    finiteProduct(
                            "regular air capacity",
                            effectiveVolumetricCapacityJPerBlockK,
                            64.0D
                    )
            );
        }

        public int maxXExclusive() {
            return Math.addExact(minX, 4);
        }

        public int maxYExclusive() {
            return Math.addExact(minY, 4);
        }

        public int maxZExclusive() {
            return Math.addExact(minZ, 4);
        }
    }

    public record PageAllocation(
            ArenaSpan cellSpan,
            int[] mixedSupportRefs,
            int[] materialPoleSlots,
            int[] phaseReservoirSlots
    ) {
        public PageAllocation {
            if (cellSpan == null || mixedSupportRefs == null
                    || materialPoleSlots == null || phaseReservoirSlots == null) {
                throw new IllegalArgumentException("Page allocation fields are required");
            }
            mixedSupportRefs = mixedSupportRefs.clone();
            materialPoleSlots = materialPoleSlots.clone();
            phaseReservoirSlots = phaseReservoirSlots.clone();
        }

        @Override
        public int[] mixedSupportRefs() {
            return mixedSupportRefs.clone();
        }

        @Override
        public int[] materialPoleSlots() {
            return materialPoleSlots.clone();
        }

        @Override
        public int[] phaseReservoirSlots() {
            return phaseReservoirSlots.clone();
        }
    }

    public enum MaterialPoleDepth {
        SURFACE,
        DEEP
    }

    public record MaterialPoleSpec(
            int blockX,
            int blockY,
            int blockZ,
            int materialProfileId,
            MaterialPoleDepth depth,
            double capacityJPerK,
            double initialTemperatureC
    ) {
        public MaterialPoleSpec {
            if (materialProfileId <= 0) {
                throw new IllegalArgumentException("materialProfileId must be positive");
            }
            if (depth == null) {
                throw new IllegalArgumentException("material pole depth is required");
            }
            if (!Double.isFinite(capacityJPerK) || capacityJPerK <= 0.0D) {
                throw new IllegalArgumentException(
                        "material pole capacity must be finite and positive");
            }
            requireFinite("initialTemperatureC", initialTemperatureC);
        }
    }

    public record PhaseReservoirSpec(
            int brickMinX,
            int brickMinY,
            int brickMinZ,
            int materialProfileId,
            long candidateMask,
            double transitionTemperatureC,
            double transitionEnergyJPerUnit
    ) {
        public PhaseReservoirSpec {
            if ((Math.floorMod(brickMinX, 4)
                    | Math.floorMod(brickMinY, 4)
                    | Math.floorMod(brickMinZ, 4)) != 0) {
                throw new IllegalArgumentException("phase reservoir must align to a 4-block Brick");
            }
            if (materialProfileId <= 0 || candidateMask == 0L) {
                throw new IllegalArgumentException(
                        "phase reservoir profile and candidate mask are required");
            }
            requireFinite("transitionTemperatureC", transitionTemperatureC);
            if (!Double.isFinite(transitionEnergyJPerUnit)
                    || transitionEnergyJPerUnit <= 0.0D) {
                throw new IllegalArgumentException(
                        "transitionEnergyJPerUnit must be finite and positive");
            }
        }
    }

    public record MixedBrickSpec(
            int minX,
            int minY,
            int minZ,
            ComponentBrickCompiler.CompiledBrick geometry,
            int mediumId,
            int flags,
            double effectiveVolumetricCapacityJPerBlockK
    ) {
        public MixedBrickSpec {
            if (Math.floorMod(minX, 4) != 0
                    || Math.floorMod(minY, 4) != 0
                    || Math.floorMod(minZ, 4) != 0) {
                throw new IllegalArgumentException("mixed Brick minimum must be 4-block aligned");
            }
            if (geometry == null || geometry.componentCount() == 0) {
                throw new IllegalArgumentException(
                        "mixed Brick geometry must contain at least one air component");
            }
            if (mediumId < 0) {
                throw new IllegalArgumentException("mediumId must be non-negative");
            }
            if (flags < 0 || flags > 0xff) {
                throw new IllegalArgumentException("flags must fit an unsigned byte");
            }
            if (!Double.isFinite(effectiveVolumetricCapacityJPerBlockK)
                    || effectiveVolumetricCapacityJPerBlockK <= 0.0D) {
                throw new IllegalArgumentException(
                        "effective volumetric capacity must be finite and positive");
            }
        }
    }

    private static boolean isMaterialKind(byte kind) {
        return kind == MATERIAL_SURFACE || kind == MATERIAL_DEEP;
    }
}
