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

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.util.Arrays;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Primitive structure-of-arrays storage for Air cells and whole-block material cells.
 *
 * <p>The arena is owned by one logical thermal writer. Page compilation uses
 * {@link ArenaSpan} allocations to install fixed 4x4x4 Brick fragments while
 * staged initialization and explicit material controls live here. Installed regional
 * material values belong to their accepted regional snapshot.</p>
 */
public final class ThermalCellArena implements MaterialStateAccess {
    private static final int NO_SLOT = -1;

    private static final byte FREE = 0;
    private static final byte LIVE = 1;
    private static final byte RESERVED = 2;
    private static final byte GEOMETRY_RETAINED = 3;
    private static final byte REGULAR_CELL = 0;
    private static final byte MIXED_COMPONENT = 1;
    private static final byte MATERIAL_CELL = 2;

    private double[] enthalpyJ;
    private double[] capacityJPerK;
    private double[] inverseCapacityKPerJ;
    private MaterialThermalLaw[] materialLaws;
    private int[] materialProfileIds;
    private long[] materialIncarnations;
    private long nextMaterialIncarnation;
    private byte[] materialBranches;
    private long[] pendingMaterialLayouts;
    private int[] pageSlots;
    private int[] lifecycleGenerations;
    private int[] supportRefs;
    private int[] minimumX;
    private int[] minimumY;
    private int[] minimumZ;
    private byte[] cellKinds;
    private BlockBrickLayout[] mixedBrickGeometries;
    private byte[] allocationState;
    private com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken[] activations, retirements;
    private long[] liveSlots;
    private long[] liveWordSummary;
    private final ThermalFreeSpanIndex freeSpans = new ThermalFreeSpanIndex();
    private final ThermalPhaseRequestStore phases;
    private Int2ObjectOpenHashMap<GeometryPin> geometryPins;

    private int highWaterMark;
    private int allocationHighWaterMark;
    private int liveCellCount;
    private double externalMaterialEnergyJ;
    private com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation storage;

    public void storageOwner(com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation owner) {
        storage = owner;
    }

    private static long columnBytes(int capacity) {
        long bytes = 9 * AirBackingMeter.arrayPayloadBytes(capacity, 8)
                + 7 * AirBackingMeter.arrayPayloadBytes(capacity, 4)
                + 4 * AirBackingMeter.arrayPayloadBytes(capacity, 1);
        int words = (capacity + 63) >>> 6;
        return bytes + 2 * AirBackingMeter.arrayPayloadBytes(words, 8)
                + AirBackingMeter.arrayPayloadBytes((words + 63) >>> 6, 8);
    }

    public double externalMaterialEnergyJ() {
        return externalMaterialEnergyJ;
    }

    public void recordExternalMaterialEnergy(double energyJ) {
        externalMaterialEnergyJ += energyJ;
    }

    public ThermalCellArena(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        enthalpyJ = new double[initialCapacity];
        capacityJPerK = new double[initialCapacity];
        inverseCapacityKPerJ = new double[initialCapacity];
        materialLaws = new MaterialThermalLaw[initialCapacity];
        materialProfileIds = new int[initialCapacity];
        materialIncarnations = new long[initialCapacity];
        materialBranches = new byte[initialCapacity];
        pendingMaterialLayouts = new long[(initialCapacity + 63) >>> 6];
        pageSlots = new int[initialCapacity];
        lifecycleGenerations = new int[initialCapacity];
        supportRefs = new int[initialCapacity];
        minimumX = new int[initialCapacity];
        minimumY = new int[initialCapacity];
        minimumZ = new int[initialCapacity];
        cellKinds = new byte[initialCapacity];
        mixedBrickGeometries = new BlockBrickLayout[initialCapacity];
        phases = new ThermalPhaseRequestStore(initialCapacity);
        allocationState = new byte[initialCapacity];
        activations = new com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken[initialCapacity];
        retirements = new com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken[initialCapacity];
        liveSlots = new long[(initialCapacity + 63) >>> 6];
        liveWordSummary = new long[(liveSlots.length + 63) >>> 6];
        Arrays.fill(pageSlots, NO_SLOT);
        Arrays.fill(supportRefs, NO_SLOT);
    }

    public int highWaterMark() {
        return highWaterMark;
    }

    public int liveCellCount() {
        return liveCellCount;
    }

    /** Address limit required by committed and currently staged spans. */
    public int requiredSlotCapacity() {
        return allocationHighWaterMark;
    }

    /** Uses allocated capacity, including recycled addresses and pinned geometry. */
    public void recordBacking(AirBackingMeter meter) {
        meter.array(enthalpyJ); meter.array(capacityJPerK); meter.array(inverseCapacityKPerJ);
        meter.array(materialLaws); meter.array(materialProfileIds); meter.array(materialIncarnations); meter.array(materialBranches);
        meter.array(pendingMaterialLayouts); meter.array(pageSlots); meter.array(lifecycleGenerations);
        meter.array(supportRefs); meter.array(minimumX); meter.array(minimumY); meter.array(minimumZ);
        meter.array(cellKinds); meter.array(mixedBrickGeometries); meter.array(allocationState);
        meter.array(activations); meter.array(retirements);
        meter.array(liveSlots); meter.array(liveWordSummary); phases.recordBacking(meter); freeSpans.recordBacking(meter);
        for (var geometry : mixedBrickGeometries) if (geometry != null) geometry.recordBacking(meter);
        if (geometryPins != null) {
            meter.container(geometryPins, geometryPins.size(), 16);
            for (var pin : geometryPins.values()) {
                meter.objectOnce(pin, 48); meter.objectOnce(pin.span, 24); meter.bytes(32L * pin.readers);
            }
        }
    }

    public boolean isLive(int slot) {
        if (slot < 0 || slot >= highWaterMark) return false;
        var birth = activations[slot]; var end = retirements[slot];
        return (allocationState[slot] == LIVE || birth != null && birth.committed()) && (end == null || !end.committed());
    }

    public boolean isStagedCell(int slot) {
        return slot >= 0 && slot < allocationHighWaterMark && allocationState[slot] == RESERVED;
    }

    /** Returns the next live slot at or after {@code fromInclusive}, or -1. */
    public int nextLiveSlot(int fromInclusive) {
        int next = nextPotentialLiveSlot(fromInclusive);
        while (next >= 0 && !isLive(next)) next = nextPotentialLiveSlot(next + 1);
        return next;
    }
    private int nextPotentialLiveSlot(int fromInclusive) {
        int start = Math.max(0, fromInclusive);
        if (start >= highWaterMark) {
            return NO_SLOT;
        }
        int wordIndex = start >>> 6;
        long word = liveSlots[wordIndex] & (-1L << (start & 63));
        if (word != 0L) {
            return (wordIndex << 6) + Long.numberOfTrailingZeros(word);
        }
        for (wordIndex = nextOccupiedWord(wordIndex + 1);
                wordIndex >= 0;
                wordIndex = nextOccupiedWord(wordIndex + 1)) {
            word = liveSlots[wordIndex];
            if (word != 0L) {
                int slot = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return slot < highWaterMark ? slot : NO_SLOT;
            }
        }
        return NO_SLOT;
    }

    /** Reserves and fills one worker-private Brick without publishing live cells. */
    public ArenaSpan stageBrickCells(
            int pageSlot,
            int lifecycleGeneration,
            ThermalBrickCellLayout layout,
            double initialAirTemperatureC,
            double referenceTemperatureC,
            int maximumSlots) {
        if (pageSlot < 0
                || layout == null
                || maximumSlots <= 0
                || maximumSlots < allocationHighWaterMark) {
            throw new IllegalArgumentException("Brick allocation identity is invalid");
        }
        requireLifecycleGeneration(lifecycleGeneration);
        requireFinite("initialAirTemperatureC", initialAirTemperatureC);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        layout.requireReady();
        int airCells =
                switch (layout.airKind) {
                    case NONE -> 0;
                    case REGULAR -> 1;
                    case MIXED -> layout.mixedGeometry.airNodeCount();
                };
        int totalCells = Math.addExact(airCells, layout.materialCount);
        if (totalCells == 0) {
            return ArenaSpan.EMPTY;
        }

        int firstSlot = findFreeSpan(totalCells);
        int required = Math.addExact(firstSlot, totalCells);
        if (required > maximumSlots) {
            return null;
        }
        allocationHighWaterMark = Math.max(allocationHighWaterMark, required);
        try {
            ensureCapacity(required, maximumSlots);
            int write = firstSlot;
            double airOffset = initialAirTemperatureC - referenceTemperatureC;
            if (layout.airKind == ThermalBrickCellLayout.AirKind.REGULAR) {
                double capacity =
                        finiteProduct("regular air capacity", layout.airCapacityJPerBlockK, 64.0D);
                writeRegularCell(
                        write++,
                        pageSlot,
                        lifecycleGeneration,
                        layout.minX,
                        layout.minY,
                        layout.minZ,
                        capacity,
                        finiteProduct("initial enthalpy", capacity, airOffset));
            } else if (layout.airKind == ThermalBrickCellLayout.AirKind.MIXED) {
                int support = write;
                mixedBrickGeometries[support] = layout.mixedGeometry;
                for (int component = 0;
                        component < layout.mixedGeometry.airNodeCount();
                        component++) {
                    double capacity = layout.airNodeCapacityJPerK[component];
                    writeMixedComponent(
                            write++,
                            pageSlot,
                            lifecycleGeneration,
                            support,
                            layout.minX,
                            layout.minY,
                            layout.minZ,
                            capacity,
                            finiteProduct("initial enthalpy", capacity, airOffset));
                }
            }

            for (int index = 0; index < layout.materialCount; index++) {
                double offset = layout.materialInitialTemperatureC[index] - referenceTemperatureC;
                writeMaterialCell(
                        write++,
                        pageSlot,
                        lifecycleGeneration,
                        layout.materialBlockX[index],
                        layout.materialBlockY[index],
                        layout.materialBlockZ[index],
                        layout.materialCapacityJPerK[index],
                        finiteProduct(
                                "initial material enthalpy",
                                layout.materialCapacityJPerK[index],
                                offset));
            }

            if (write != required) {
                throw new IllegalStateException(
                        "staged Brick cell count changed during construction");
            }
            return new ArenaSpan(firstSlot, totalCells);
        } catch (RuntimeException | Error failure) {
            clearRange(firstSlot, required);
            addFreeSpan(firstSlot, totalCells);
            throw failure;
        }
    }

    public boolean ownsStagedCells(ArenaSpan span) {
        return ownsSpan(span, RESERVED, NO_SLOT, 0, false);
    }

    /** Allocation-free authoritative state transition after global preflight. */
    public void commitStagedCells(ArenaSpan span) {
        if (span.count() == 0) {
            return;
        }
        int nextLiveCellCount = Math.addExact(liveCellCount, span.count());
        int nextHighWaterMark = Math.max(highWaterMark, span.endSlotExclusive());
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            allocationState[slot] = LIVE; activations[slot] = null;
            liveSlots[slot >>> 6] |= 1L << slot;
        }
        setLiveWordSummary(span.firstSlot(), span.endSlotExclusive());
        highWaterMark = nextHighWaterMark;
        liveCellCount = nextLiveCellCount;
    }

    /** A Brick has at most its admitted local cells; visibility changes with the shared token. */
    public void stageVisibility(ArenaSpan span, com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token, boolean activation) {
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            if (activation) { activations[slot] = token; liveSlots[slot >>> 6] |= 1L << slot; }
            else retirements[slot] = token;
        }
        if (activation) {
            highWaterMark = Math.max(highWaterMark, span.endSlotExclusive());
            setLiveWordSummary(span.firstSlot(), span.endSlotExclusive());
        }
    }
    public void finishVisibility(ArenaSpan span, com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token, boolean activation) {
        if (activation && token.committed()) { commitStagedCells(span); return; }
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            if (activation && activations[slot] == token) {
                activations[slot] = null; liveSlots[slot >>> 6] &= ~(1L << slot);
            } else if (!activation && retirements[slot] == token) retirements[slot] = null;
        }
        if (activation) { clearEmptyLiveWordSummary(span.firstSlot(), span.endSlotExclusive()); recomputeHighWaterMark(); }
    }

    public void discardStagedCells(ArenaSpan span) {
        if (span.count() == 0) {
            return;
        }
        if (!ownsStagedCells(span)) {
            throw new IllegalStateException("Brick staging span is not reserved");
        }
        if (deferGeometryRelease(span, false)) return;
        clearRange(span.firstSlot(), span.endSlotExclusive());
        addFreeSpan(span.firstSlot(), span.count());
    }

    public void stageEnthalpyJ(int slot, double value) {
        requireFinite("enthalpyJ", value);
        requireReservedSlot(slot);
        enthalpyJ[slot] = value;
    }

    public double enthalpyJ(int slot) {
        requireAllocatedSlot(slot);
        return enthalpyJ[slot];
    }

    public double capacityJPerK(int slot) {
        requireAllocatedSlot(slot);
        return capacityJPerK[slot];
    }

    public double temperatureC(int slot, double referenceTemperatureC) {
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireLiveSlot(slot);
        return temperatureForAllocatedSlot(slot, referenceTemperatureC);
    }

    /** Candidate projection can read staged cells without publishing them as live. */
    public double allocatedTemperatureC(int slot, double referenceTemperatureC) {
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireAllocatedSlot(slot);
        return temperatureForAllocatedSlot(slot, referenceTemperatureC);
    }

    private double temperatureForAllocatedSlot(int slot, double referenceTemperatureC) {
        if (materialLaws[slot] != null) {
            return materialLaws[slot].temperatureC(enthalpyJ[slot], materialBranches[slot]);
        }
        return referenceTemperatureC + enthalpyJ[slot] * inverseCapacityKPerJ[slot];
    }

    /** Installs body parameters while its replacement span is still private. */
    public void stageMaterialLaw(
            int slot, int profileId, MaterialThermalLaw law, double initialTemperatureC) {
        requireReservedSlot(slot);
        materialLaws[slot] = law;
        materialProfileIds[slot] = profileId;
        materialIncarnations[slot] = Math.incrementExact(nextMaterialIncarnation);
        nextMaterialIncarnation = materialIncarnations[slot];
        materialBranches[slot] = MaterialThermalLaw.SENSIBLE;
        capacityJPerK[slot] = law.capacityJPerK();
        inverseCapacityKPerJ[slot] = 1 / law.capacityJPerK();
        enthalpyJ[slot] = law.enthalpyAtTemperature(initialTemperatureC);
        if (law.heating() != null && initialTemperatureC >= law.heating().transitionTemperatureC())
            materialBranches[slot] = MaterialThermalLaw.HEATING;
        else if (law.cooling() != null
                && initialTemperatureC <= law.cooling().transitionTemperatureC())
            materialBranches[slot] = MaterialThermalLaw.COOLING;
    }

    public MaterialThermalLaw materialLaw(int slot) {
        requireAllocatedSlot(slot);
        return materialLaws[slot];
    }

    public int materialProfileId(int slot) {
        requireAllocatedSlot(slot);
        return materialProfileIds[slot];
    }

    /** Object identity survives address migration, but never removal followed by replacement. */
    public long materialIncarnation(int slot) {
        requireAllocatedSlot(slot);
        return materialIncarnations[slot];
    }

    public void inheritMaterialIncarnation(int oldSlot, int newSlot) {
        requireAllocatedSlot(oldSlot);
        requireReservedSlot(newSlot);
        materialIncarnations[newSlot] = materialIncarnations[oldSlot];
    }

    @Override
    public void readMaterial(int slot, MaterialSample target) {
        requireAllocatedSlot(slot);
        target.setLive(enthalpyJ[slot], materialLaws[slot], materialBranches[slot], -1,
                hasMaterialTransition(slot) ? phaseRequestSequence(slot) : 0);
    }

    @Override
    public void applyMaterialControl(int slot, double energyJ, byte branch) {
        commitMaterialState(slot, energyJ, branch);
    }

    public void stageMaterialState(int slot, double energyJ, byte branch) {
        stageEnthalpyJ(slot, energyJ);
        materialBranches[slot] = branch;
    }

    /** Installs an accepted worker trial without selecting a branch or emitting a phase request. */
    public void commitMaterialState(int slot, double energyJ, byte branch) {
        requireLiveSlot(slot);
        requireFinite("material enthalpy", energyJ);
        enthalpyJ[slot] = energyJ;
        materialBranches[slot] = branch;
    }

    public boolean materialTransitionWaiting(int slot) {
        return materialLayoutPending(slot)
                || materialLaws[slot] != null && phases.requestOutstanding(slot);
    }

    public boolean materialLayoutPending(int slot) {
        return (pendingMaterialLayouts[slot >>> 6] & 1L << slot) != 0;
    }

    public void awaitMaterialLayout(int slot) {
        if (isLive(slot) && materialLaws[slot] != null)
            pendingMaterialLayouts[slot >>> 6] |= 1L << slot;
    }

    public boolean materialTransitionAcknowledged(int slot) {
        return materialLaws[slot] != null && phases.acknowledged(slot);
    }

    public boolean hasMaterialTransition(int slot) {
        MaterialThermalLaw law = materialLaws[slot];
        return law != null && (law.heating() != null || law.cooling() != null);
    }

    public int lifecycleGeneration(int slot) {
        requireAllocatedSlot(slot);
        return lifecycleGenerations[slot];
    }

    /** Worker publication ownership for one allocated cell. */
    public int pageSlot(int slot) {
        requireAllocatedSlot(slot);
        return pageSlots[slot];
    }

    /** Whether one allocated slot is an Air cell published to gameplay queries. */
    public boolean isAirCell(int slot) {
        requireAllocatedSlot(slot);
        return cellKinds[slot] == REGULAR_CELL || cellKinds[slot] == MIXED_COMPONENT;
    }

    /** Captured actual Air membership, indexed x | z << 2 | y << 4 within its Brick. */
    public long airBlockMask(int slot) {
        requireAllocatedSlot(slot);
        if (cellKinds[slot] == REGULAR_CELL) return -1L;
        if (cellKinds[slot] != MIXED_COMPONENT) return 0L;
        int support = supportRefs[slot];
        return mixedGeometry(support).nodeBlockMask(slot - support);
    }

    public int minimum(int slot, int axis) {
        requireAllocatedSlot(slot);
        return switch (axis) {
            case 0 -> minimumX[slot];
            case 1 -> minimumY[slot];
            case 2 -> minimumZ[slot];
            default -> throw new IllegalArgumentException("axis must be 0, 1, or 2");
        };
    }

    /** Material membership, not geometric exposure; valid for staged and live cells. */
    public boolean isMaterialCell(int slot) {
        requireAllocatedSlot(slot);
        return cellKinds[slot] == MATERIAL_CELL;
    }

    public boolean phaseRequestOutstanding(int slot) {
        requirePhaseMaterial(slot);
        return phases.requestOutstanding(slot);
    }

    public boolean phaseRequestNeedsOffer(int slot) {
        requirePhaseMaterial(slot);
        return phases.requestNeedsOffer(slot);
    }

    public long phaseRequestSequence(int slot) {
        requirePhaseMaterial(slot);
        return phases.requestSequence(slot);
    }

    public void beginPhaseRequest(int slot, long requestSequence) {
        requirePhaseMaterial(slot);
        phases.beginMaterialRequest(slot, requestSequence);
    }

    public void markPhaseRequestEnqueued(int slot, long requestSequence) {
        requirePhaseMaterial(slot);
        phases.markRequestEnqueued(slot, requestSequence);
    }

    public void retryPhaseRequest(int slot, long requestSequence) {
        requirePhaseMaterial(slot);
        phases.retryRequest(slot, requestSequence);
    }

    public void completePhaseRequest(int slot, long requestSequence, boolean mutationApplied) {
        requirePhaseMaterial(slot);
        phases.completeMaterialRequest(slot, requestSequence, mutationApplied);
    }

    public void copyPhaseRequestState(int oldSlot, int newSlot) {
        requirePhaseMaterial(oldSlot);
        requirePhaseMaterial(newSlot);
        if (materialProfileIds[oldSlot] != materialProfileIds[newSlot]
                || minimumX[oldSlot] != minimumX[newSlot]
                || minimumY[oldSlot] != minimumY[newSlot]
                || minimumZ[oldSlot] != minimumZ[newSlot]) {
            throw new IllegalArgumentException("phase material ownership key changed");
        }
        phases.copyRequest(oldSlot, newSlot);
    }

    private boolean isMixedSupport(int supportRef) {
        return isAllocated(supportRef)
                && cellKinds[supportRef] == MIXED_COMPONENT
                && supportRefs[supportRef] == supportRef
                && mixedBrickGeometries[supportRef] != null;
    }

    public double center(int slot, int axis) {
        requireAllocatedSlot(slot);
        int origin =
                switch (axis) {
                    case 0 -> minimumX[slot];
                    case 1 -> minimumY[slot];
                    case 2 -> minimumZ[slot];
                    default -> throw new IllegalArgumentException("axis must be 0, 1, or 2");
                };
        if (isMaterialKind(cellKinds[slot])) {
            return origin + 0.5D;
        }
        if (cellKinds[slot] == REGULAR_CELL) {
            return origin + 2.0D;
        }
        int support = supportRefs[slot];
        BlockBrickLayout geometry = mixedGeometry(support);
        // Transport nodes occupy consecutive slots starting at their Brick support.
        return origin + geometry.center(slot - support, axis);
    }

    BlockBrickLayout mixedGeometry(int supportRef) {
        if (!isMixedSupport(supportRef)) {
            throw new IllegalArgumentException("slot is not a mixed-Brick support: " + supportRef);
        }
        return mixedBrickGeometries[supportRef];
    }

    /** Releases one retired Page span after its replacement sweep is installed. */
    public void releasePageCells(
            int expectedPageSlot, int expectedLifecycleGeneration, ArenaSpan span) {
        if (span == null) {
            throw new IllegalArgumentException("span is required");
        }
        if (span.count() == 0) {
            return;
        }
        requireLifecycleGeneration(expectedLifecycleGeneration);
        if (!ownsLiveCells(expectedPageSlot, expectedLifecycleGeneration, span)) {
            throw new IllegalArgumentException("Page release does not own the complete arena span");
        }
        releaseSpan(span);
    }

    public boolean ownsLiveCells(
            int expectedPageSlot, int expectedLifecycleGeneration, ArenaSpan span) {
        return ownsSpan(span, LIVE, expectedPageSlot, expectedLifecycleGeneration, true);
    }

    /**
     * Coordinator-owned lease of a complete Brick span, including its mixed-Air support.
     * This pins existing arena addresses; it does not copy cell columns. The arena's
     * storage owner keeps charging their full allocated capacity until shutdown,
     * including retired pinned spans, and prepays any replacement arrays on growth.
     */
    public GeometryReadLease retainGeometry(int pageSlot, int generation, ArenaSpan span) {
        if (span.count() == 0) throw new IllegalArgumentException("geometry span is empty");
        GeometryPin pin = geometryPins == null ? null : geometryPins.get(span.firstSlot());
        if (pin == null) {
            if (!ownsSpan(span, LIVE, pageSlot, generation, true)
                    && !ownsSpan(span, RESERVED, pageSlot, generation, true))
                throw new IllegalArgumentException("geometry reader does not own the Brick span");
            if (geometryPins == null) geometryPins = new Int2ObjectOpenHashMap<>();
            pin = new GeometryPin(pageSlot, generation, span);
            geometryPins.put(span.firstSlot(), pin);
        } else if (pin.pageSlot != pageSlot || pin.generation != generation || !pin.span.equals(span)) {
            throw new IllegalArgumentException("geometry readers name different Brick spans");
        }
        pin.readers++;
        return new GeometryReadLease(pin);
    }

    /** Retired read-only geometry is never a live source/phase target. */
    public boolean isRetainedGeometryCell(int slot) {
        return slot >= 0 && slot < allocationHighWaterMark && allocationState[slot] == GEOMETRY_RETAINED;
    }

    private boolean deferGeometryRelease(ArenaSpan span, boolean live) {
        GeometryPin pin = geometryPins == null ? null : geometryPins.get(span.firstSlot());
        if (pin == null) return false;
        if (!pin.span.equals(span)) throw new IllegalArgumentException("geometry release changes the Brick span");
        pin.retired = true;
        Arrays.fill(allocationState, span.firstSlot(), span.endSlotExclusive(), GEOMETRY_RETAINED);
        if (live) {
            for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++)
                liveSlots[slot >>> 6] &= ~(1L << slot);
            clearEmptyLiveWordSummary(span.firstSlot(), span.endSlotExclusive());
            liveCellCount -= span.count();
            recomputeHighWaterMark();
        }
        return true;
    }

    private static final class GeometryPin {
        final int pageSlot, generation;
        final ArenaSpan span;
        int readers;
        boolean retired;
        GeometryPin(int pageSlot, int generation, ArenaSpan span) {
            this.pageSlot = pageSlot; this.generation = generation; this.span = span;
        }
    }

    /** Release on the same logical writer after every helper using this input has finished. */
    public final class GeometryReadLease implements AutoCloseable {
        private GeometryPin pin;
        private GeometryReadLease(GeometryPin pin) { this.pin = pin; }
        public boolean containsCell(int slot) {
            return pin != null && slot >= pin.span.firstSlot() && slot < pin.span.endSlotExclusive();
        }
        @Override public void close() {
            GeometryPin held = pin;
            if (held == null) return;
            pin = null;
            if (--held.readers != 0) return;
            geometryPins.remove(held.span.firstSlot());
            if (geometryPins.isEmpty()) geometryPins = null;
            if (held.retired) {
                clearRange(held.span.firstSlot(), held.span.endSlotExclusive());
                addFreeSpan(held.span.firstSlot(), held.span.count());
            }
        }
    }

    private int findFreeSpan(int count) {
        long fit = freeSpans.takeBestFit(count);
        if (fit == ThermalFreeSpanIndex.NO_SPAN) {
            return allocationHighWaterMark;
        }
        int spanLength = (int) (fit >>> 32);
        int firstSlot = (int) fit;
        if (spanLength > count) {
            addFreeSpan(firstSlot + count, spanLength - count);
        }
        return firstSlot;
    }

    private void addFreeSpan(int firstSlot, int count) {
        if (count <= 0 || firstSlot < 0 || firstSlot + count > allocationHighWaterMark) {
            throw new IllegalArgumentException("free arena span is invalid");
        }
        allocationHighWaterMark = freeSpans.addAndMerge(firstSlot, count, allocationHighWaterMark);
    }

    private void ensureCapacity(int requiredCapacity, int maximumCapacity) {
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
        grown = Math.min(grown, maximumCapacity);
        if (grown < requiredCapacity) {
            throw new IllegalStateException("thermal cell arena exceeded int address space");
        }
        if (storage != null) storage.beforeAllocation(columnBytes(grown));
        enthalpyJ = Arrays.copyOf(enthalpyJ, grown);
        capacityJPerK = Arrays.copyOf(capacityJPerK, grown);
        inverseCapacityKPerJ = Arrays.copyOf(inverseCapacityKPerJ, grown);
        materialLaws = Arrays.copyOf(materialLaws, grown);
        materialProfileIds = Arrays.copyOf(materialProfileIds, grown);
        materialIncarnations = Arrays.copyOf(materialIncarnations, grown);
        materialBranches = Arrays.copyOf(materialBranches, grown);
        pendingMaterialLayouts = Arrays.copyOf(pendingMaterialLayouts, (grown + 63) >>> 6);
        pageSlots = Arrays.copyOf(pageSlots, grown);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, grown);
        supportRefs = Arrays.copyOf(supportRefs, grown);
        minimumX = Arrays.copyOf(minimumX, grown);
        minimumY = Arrays.copyOf(minimumY, grown);
        minimumZ = Arrays.copyOf(minimumZ, grown);
        cellKinds = Arrays.copyOf(cellKinds, grown);
        mixedBrickGeometries = Arrays.copyOf(mixedBrickGeometries, grown);
        phases.ensureCapacity(grown);
        allocationState = Arrays.copyOf(allocationState, grown);
        activations = Arrays.copyOf(activations, grown); retirements = Arrays.copyOf(retirements, grown);
        liveSlots = Arrays.copyOf(liveSlots, (grown + 63) >>> 6);
        liveWordSummary = Arrays.copyOf(liveWordSummary, (liveSlots.length + 63) >>> 6);
        Arrays.fill(pageSlots, oldCapacity, grown, NO_SLOT);
        Arrays.fill(supportRefs, oldCapacity, grown, NO_SLOT);
        if (storage != null) storage.afterReplacement(columnBytes(oldCapacity));
    }

    private void releaseSpan(ArenaSpan span) {
        if (deferGeometryRelease(span, true)) return;
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            liveSlots[slot >>> 6] &= ~(1L << slot);
        }
        clearRange(span.firstSlot(), span.endSlotExclusive());
        clearEmptyLiveWordSummary(span.firstSlot(), span.endSlotExclusive());
        liveCellCount -= span.count();
        recomputeHighWaterMark();
        addFreeSpan(span.firstSlot(), span.count());
    }

    private boolean isAllocated(int slot) {
        return slot >= 0 && slot < allocationHighWaterMark && allocationState[slot] != FREE;
    }

    private boolean ownsSpan(
            ArenaSpan span,
            byte expectedState,
            int expectedPageSlot,
            int expectedLifecycleGeneration,
            boolean validateOwner) {
        if (span == null
                || span.firstSlot() < 0
                || span.endSlotExclusive() > allocationHighWaterMark) {
            return false;
        }
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            if (allocationState[slot] != expectedState
                    || validateOwner
                            && (pageSlots[slot] != expectedPageSlot
                                    || lifecycleGenerations[slot] != expectedLifecycleGeneration)) {
                return false;
            }
        }
        return true;
    }

    private void setLiveWordSummary(int firstSlot, int endSlotExclusive) {
        int firstWord = firstSlot >>> 6;
        int lastWord = (endSlotExclusive - 1) >>> 6;
        for (int word = firstWord; word <= lastWord; word++) {
            liveWordSummary[word >>> 6] |= 1L << (word & 63);
        }
    }

    private void clearEmptyLiveWordSummary(int firstSlot, int endSlotExclusive) {
        int firstWord = firstSlot >>> 6;
        int lastWord =
                Math.min(liveSlots.length - 1, Math.max(firstWord, (endSlotExclusive - 1) >>> 6));
        for (int word = firstWord; word <= lastWord; word++) {
            if (liveSlots[word] == 0L) {
                liveWordSummary[word >>> 6] &= ~(1L << (word & 63));
            }
        }
    }

    private void recomputeHighWaterMark() {
        int word = previousOccupiedWord(liveSlots.length - 1);
        while (word >= 0) {
            long live = liveSlots[word];
            if (live != 0L) {
                highWaterMark = (word << 6) + Long.SIZE - Long.numberOfLeadingZeros(live);
                return;
            }
            word = previousOccupiedWord(word - 1);
        }
        highWaterMark = 0;
    }

    private int nextOccupiedWord(int fromInclusive) {
        if (fromInclusive < 0 || fromInclusive >= liveSlots.length) {
            return NO_SLOT;
        }
        int summary = fromInclusive >>> 6;
        long word = liveWordSummary[summary] & (-1L << (fromInclusive & 63));
        while (true) {
            if (word != 0L) {
                int result = (summary << 6) + Long.numberOfTrailingZeros(word);
                return result < liveSlots.length ? result : NO_SLOT;
            }
            if (++summary >= liveWordSummary.length) {
                return NO_SLOT;
            }
            word = liveWordSummary[summary];
        }
    }

    private int previousOccupiedWord(int fromInclusive) {
        if (fromInclusive < 0 || liveWordSummary.length == 0) {
            return NO_SLOT;
        }
        int summary = Math.min(fromInclusive >>> 6, liveWordSummary.length - 1);
        int bit = Math.min(fromInclusive & 63, 63);
        long word = liveWordSummary[summary] & (-1L >>> (63 - bit));
        while (true) {
            if (word != 0L) {
                return summary << 6 | 63 - Long.numberOfLeadingZeros(word);
            }
            if (--summary < 0) {
                return NO_SLOT;
            }
            word = liveWordSummary[summary];
        }
    }

    private void clearRange(int firstSlot, int endSlotExclusive) {
        int end = Math.min(endSlotExclusive, allocationState.length);
        for (int slot = firstSlot; slot < end; slot++) {
            allocationState[slot] = FREE; activations[slot] = retirements[slot] = null;
            enthalpyJ[slot] = 0.0D;
            capacityJPerK[slot] = 0.0D;
            inverseCapacityKPerJ[slot] = 0.0D;
            materialLaws[slot] = null;
            materialProfileIds[slot] = 0;
            materialIncarnations[slot] = 0;
            materialBranches[slot] = MaterialThermalLaw.SENSIBLE;
            pendingMaterialLayouts[slot >>> 6] &= ~(1L << slot);
            pageSlots[slot] = NO_SLOT;
            lifecycleGenerations[slot] = 0;
            supportRefs[slot] = NO_SLOT;
            minimumX[slot] = 0;
            minimumY[slot] = 0;
            minimumZ[slot] = 0;
            cellKinds[slot] = REGULAR_CELL;
            mixedBrickGeometries[slot] = null;
            phases.clear(slot);
        }
    }

    private void requireLiveSlot(int slot) {
        if (!isLive(slot)) {
            throw new IllegalArgumentException("cell slot is not live: " + slot);
        }
    }

    private void requireAllocatedSlot(int slot) {
        if (!isAllocated(slot)) {
            throw new IllegalArgumentException("cell slot is not allocated: " + slot);
        }
    }

    private void requireReservedSlot(int slot) {
        if (slot < 0 || slot >= allocationHighWaterMark || allocationState[slot] != RESERVED) {
            throw new IllegalArgumentException("cell slot is not staged: " + slot);
        }
    }

    private void requirePhaseMaterial(int slot) {
        requireAllocatedSlot(slot);
        if (!hasMaterialTransition(slot)) {
            throw new IllegalArgumentException("slot is not a phase material: " + slot);
        }
    }

    private void writeRegularCell(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int minX,
            int minY,
            int minZ,
            double capacity,
            double initialEnthalpyJ) {
        allocationState[slot] = RESERVED;
        enthalpyJ[slot] = initialEnthalpyJ;
        writeCapacity(slot, capacity);
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = minX;
        minimumY[slot] = minY;
        minimumZ[slot] = minZ;
        cellKinds[slot] = REGULAR_CELL;
        mixedBrickGeometries[slot] = null;
    }

    private void writeMixedComponent(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int supportRef,
            int minX,
            int minY,
            int minZ,
            double capacity,
            double initialEnthalpyJ) {
        allocationState[slot] = RESERVED;
        enthalpyJ[slot] = initialEnthalpyJ;
        writeCapacity(slot, capacity);
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = supportRef;
        minimumX[slot] = minX;
        minimumY[slot] = minY;
        minimumZ[slot] = minZ;
        cellKinds[slot] = MIXED_COMPONENT;
    }

    private void writeMaterialCell(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int blockX,
            int blockY,
            int blockZ,
            double capacity,
            double initialEnthalpyJ) {
        allocationState[slot] = RESERVED;
        enthalpyJ[slot] = initialEnthalpyJ;
        writeCapacity(slot, capacity);
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = blockX;
        minimumY[slot] = blockY;
        minimumZ[slot] = blockZ;
        cellKinds[slot] = MATERIAL_CELL;
        mixedBrickGeometries[slot] = null;
    }

    private void writeCapacity(int slot, double capacity) {
        if (!Double.isFinite(capacity) || capacity <= 0.0D) {
            throw new IllegalArgumentException("cell capacity must be finite and positive");
        }
        double inverse = 1.0D / capacity;
        if (!Double.isFinite(inverse) || inverse <= 0.0D) {
            throw new IllegalArgumentException("inverse cell capacity is invalid");
        }
        capacityJPerK[slot] = capacity;
        inverseCapacityKPerJ[slot] = inverse;
    }

    private static double finiteProduct(String name, double left, double right) {
        double result = left * right;
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

    private static boolean isMaterialKind(byte kind) {
        return kind == MATERIAL_CELL;
    }
}
