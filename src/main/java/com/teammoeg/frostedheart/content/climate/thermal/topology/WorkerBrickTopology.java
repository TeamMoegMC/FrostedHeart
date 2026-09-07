/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;

/**
 * 一个 {@code 4x4x4} Brick 的不可变已提交或待提交拓扑。
 *
 * <p>对象只保存该 Brick 的 arena span、Air/material 联系、phase 候选和
 * publication 载荷；温度与焓仍由 arena/solver 持有。</p>
 */
final class WorkerBrickTopology {
    static final WorkerBrickTopology EMPTY = new WorkerBrickTopology(
            ArenaSpan.EMPTY, -1, 0, null,
            PagePublication.PhaseCandidates.EMPTY,
            MaterialPoles.EMPTY, PhaseReservoirs.EMPTY, MaterialContacts.EMPTY,
            false, false);

    final ArenaSpan span;
    final int coverageSlot;
    final int coverageGeneration;
    final ComponentBrickCompiler.CompiledBrick mixedGeometry;
    final PagePublication.PhaseCandidates phaseCandidates;
    final MaterialPoles materialPoles;
    final PhaseReservoirs phaseReservoirs;
    final MaterialContacts materialContacts;
    final boolean cellsResolved;
    final boolean resolved;

    WorkerBrickTopology(
            ArenaSpan span,
            int coverageSlot,
            int coverageGeneration,
            ComponentBrickCompiler.CompiledBrick mixedGeometry,
            PagePublication.PhaseCandidates phaseCandidates,
            MaterialPoles materialPoles,
            PhaseReservoirs phaseReservoirs,
            MaterialContacts materialContacts,
            boolean cellsResolved,
            boolean resolved
    ) {
        this.span = span;
        this.coverageSlot = coverageSlot;
        this.coverageGeneration = coverageGeneration;
        this.mixedGeometry = mixedGeometry;
        this.phaseCandidates = phaseCandidates;
        this.materialPoles = materialPoles;
        this.phaseReservoirs = phaseReservoirs;
        this.materialContacts = materialContacts;
        this.cellsResolved = cellsResolved;
        this.resolved = resolved;
    }

    WorkerBrickTopology withFragmentResult(boolean nextResolved) {
        return new WorkerBrickTopology(
                span, coverageSlot, coverageGeneration, mixedGeometry,
                phaseCandidates, materialPoles, phaseReservoirs,
                materialContacts, cellsResolved,
                nextResolved);
    }

    record MaterialPoles(
            int[] blockX,
            int[] blockY,
            int[] blockZ,
            int[] profileId,
            int[] slot
    ) {
        static final MaterialPoles EMPTY = new MaterialPoles(
                new int[0], new int[0], new int[0], new int[0], new int[0]);

        MaterialPoles {
            int size = blockX.length;
            if (blockY.length != size || blockZ.length != size
                    || profileId.length != size || slot.length != size) {
                throw new IllegalArgumentException(
                        "material pole directory is invalid");
            }
        }

        int size() { return slot.length; }
    }

    record PhaseReservoirs(
            int[] brickMinX,
            int[] brickMinY,
            int[] brickMinZ,
            int[] profileId,
            int[] slot
    ) {
        static final PhaseReservoirs EMPTY = new PhaseReservoirs(
                new int[0], new int[0], new int[0], new int[0], new int[0]);

        PhaseReservoirs {
            int size = brickMinX.length;
            if (brickMinY.length != size || brickMinZ.length != size
                    || profileId.length != size || slot.length != size) {
                throw new IllegalArgumentException(
                        "phase reservoir directory is invalid");
            }
        }

        int size() { return slot.length; }
    }

    /** Primitive contacts retained only for exact neighbor recompilation. */
    static final class MaterialContacts {
        private static final int[] NO_INTS = new int[0];
        private static final long[] NO_LONGS = new long[0];
        static final MaterialContacts EMPTY = new MaterialContacts();

        private final int[] surfaceBlockX;
        private final int[] surfaceBlockY;
        private final int[] surfaceBlockZ;
        private final int[] surfaceProfileId;
        private final int[] surfacePoleOrdinal;
        private final int[] surfaceContactStart;
        private final int[] surfaceContactCount;
        private final long[] surfaceContactAirReference;
        private final int[] surfaceContactPatches;
        private final int[] phaseProfileId;
        private final long[] phaseCandidateMask;
        private final int[] phaseReservoirOrdinal;
        private final int[] phaseContactStart;
        private final int[] phaseContactCount;
        private final long[] phaseContactAirReference;
        private final int[] phaseContactPatches;

        private MaterialContacts() {
            surfaceBlockX = NO_INTS;
            surfaceBlockY = NO_INTS;
            surfaceBlockZ = NO_INTS;
            surfaceProfileId = NO_INTS;
            surfacePoleOrdinal = NO_INTS;
            surfaceContactStart = NO_INTS;
            surfaceContactCount = NO_INTS;
            surfaceContactAirReference = NO_LONGS;
            surfaceContactPatches = NO_INTS;
            phaseProfileId = NO_INTS;
            phaseCandidateMask = NO_LONGS;
            phaseReservoirOrdinal = NO_INTS;
            phaseContactStart = NO_INTS;
            phaseContactCount = NO_INTS;
            phaseContactAirReference = NO_LONGS;
            phaseContactPatches = NO_INTS;
        }

        private MaterialContacts(Builder builder) {
            surfaceBlockX = builder.surfaceBlockX;
            surfaceBlockY = builder.surfaceBlockY;
            surfaceBlockZ = builder.surfaceBlockZ;
            surfaceProfileId = builder.surfaceProfileId;
            surfacePoleOrdinal = builder.surfacePoleOrdinal;
            surfaceContactStart = builder.surfaceContactStart;
            surfaceContactCount = builder.surfaceContactCount;
            surfaceContactAirReference = builder.surfaceContactAirReference;
            surfaceContactPatches = builder.surfaceContactPatches;
            phaseProfileId = builder.phaseProfileId;
            phaseCandidateMask = builder.phaseCandidateMask;
            phaseReservoirOrdinal = builder.phaseReservoirOrdinal;
            phaseContactStart = builder.phaseContactStart;
            phaseContactCount = builder.phaseContactCount;
            phaseContactAirReference = builder.phaseContactAirReference;
            phaseContactPatches = builder.phaseContactPatches;
            int surfaces = surfaceBlockX.length;
            int phases = phaseProfileId.length;
            if (surfaceBlockY.length != surfaces
                    || surfaceBlockZ.length != surfaces
                    || surfaceProfileId.length != surfaces
                    || surfacePoleOrdinal.length != surfaces
                    || surfaceContactStart.length != surfaces
                    || surfaceContactCount.length != surfaces
                    || surfaceContactAirReference.length
                            != surfaceContactPatches.length
                    || phaseCandidateMask.length != phases
                    || phaseReservoirOrdinal.length != phases
                    || phaseContactStart.length != phases
                    || phaseContactCount.length != phases
                    || phaseContactAirReference.length
                            != phaseContactPatches.length) {
                throw new IllegalArgumentException(
                        "Brick material contact arrays are invalid");
            }
        }

        int surfaceCount() { return surfaceBlockX.length; }
        int phaseCount() { return phaseProfileId.length; }

        int[] surfaceBlockX() { return surfaceBlockX; }
        int[] surfaceBlockY() { return surfaceBlockY; }
        int[] surfaceBlockZ() { return surfaceBlockZ; }
        int[] surfaceProfileId() { return surfaceProfileId; }
        int[] surfacePoleOrdinal() { return surfacePoleOrdinal; }
        int[] surfaceContactStart() { return surfaceContactStart; }
        int[] surfaceContactCount() { return surfaceContactCount; }
        long[] surfaceContactAirReference() { return surfaceContactAirReference; }
        int[] surfaceContactPatches() { return surfaceContactPatches; }
        int[] phaseProfileId() { return phaseProfileId; }
        long[] phaseCandidateMask() { return phaseCandidateMask; }
        int[] phaseReservoirOrdinal() { return phaseReservoirOrdinal; }
        int[] phaseContactStart() { return phaseContactStart; }
        int[] phaseContactCount() { return phaseContactCount; }
        long[] phaseContactAirReference() { return phaseContactAirReference; }
        int[] phaseContactPatches() { return phaseContactPatches; }

        static final class Builder {
            private int[] surfaceBlockX;
            private int[] surfaceBlockY;
            private int[] surfaceBlockZ;
            private int[] surfaceProfileId;
            private int[] surfacePoleOrdinal;
            private int[] surfaceContactStart;
            private int[] surfaceContactCount;
            private long[] surfaceContactAirReference;
            private int[] surfaceContactPatches;
            private int[] phaseProfileId;
            private long[] phaseCandidateMask;
            private int[] phaseReservoirOrdinal;
            private int[] phaseContactStart;
            private int[] phaseContactCount;
            private long[] phaseContactAirReference;
            private int[] phaseContactPatches;

            Builder surfaces(
                    int[] blockX,
                    int[] blockY,
                    int[] blockZ,
                    int[] profileId,
                    int[] poleOrdinal,
                    int[] contactStart,
                    int[] contactCount,
                    long[] contactAirReference,
                    int[] contactPatches
            ) {
                surfaceBlockX = blockX;
                surfaceBlockY = blockY;
                surfaceBlockZ = blockZ;
                surfaceProfileId = profileId;
                surfacePoleOrdinal = poleOrdinal;
                surfaceContactStart = contactStart;
                surfaceContactCount = contactCount;
                surfaceContactAirReference = contactAirReference;
                surfaceContactPatches = contactPatches;
                return this;
            }

            Builder phases(
                    int[] profileId,
                    long[] candidateMask,
                    int[] reservoirOrdinal,
                    int[] contactStart,
                    int[] contactCount,
                    long[] contactAirReference,
                    int[] contactPatches
            ) {
                phaseProfileId = profileId;
                phaseCandidateMask = candidateMask;
                phaseReservoirOrdinal = reservoirOrdinal;
                phaseContactStart = contactStart;
                phaseContactCount = contactCount;
                phaseContactAirReference = contactAirReference;
                phaseContactPatches = contactPatches;
                return this;
            }

            MaterialContacts build() {
                return new MaterialContacts(this);
            }
        }
    }
}
