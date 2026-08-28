/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;

/** Immutable committed or staged authority for one 4-cubed Brick. */
final class WorkerBrickTopology {
    static final WorkerBrickTopology EMPTY = new WorkerBrickTopology(
            ArenaSpan.EMPTY, -1, 0, null,
            new GeometrySummary(
                    GeometrySummary.Kind.UNKNOWN,
                    GeometrySummary.NO_MEDIUM,
                    0),
            ThermalFragment.EMPTY, PagePublication.PhaseCandidates.EMPTY,
            MaterialPoles.EMPTY, PhaseReservoirs.EMPTY, MaterialContacts.EMPTY,
            (byte) 0, false, false);

    final ArenaSpan span;
    final int coverageSlot;
    final int coverageGeneration;
    final ComponentBrickCompiler.CompiledBrick mixedGeometry;
    final GeometrySummary summary;
    final ThermalFragment fragment;
    final PagePublication.PhaseCandidates phaseCandidates;
    final MaterialPoles materialPoles;
    final PhaseReservoirs phaseReservoirs;
    final MaterialContacts materialContacts;
    final byte continuationFaceMask;
    final boolean cellsResolved;
    final boolean resolved;

    WorkerBrickTopology(
            ArenaSpan span,
            int coverageSlot,
            int coverageGeneration,
            ComponentBrickCompiler.CompiledBrick mixedGeometry,
            GeometrySummary summary,
            ThermalFragment fragment,
            PagePublication.PhaseCandidates phaseCandidates,
            MaterialPoles materialPoles,
            PhaseReservoirs phaseReservoirs,
            MaterialContacts materialContacts,
            byte continuationFaceMask,
            boolean cellsResolved,
            boolean resolved
    ) {
        this.span = span;
        this.coverageSlot = coverageSlot;
        this.coverageGeneration = coverageGeneration;
        this.mixedGeometry = mixedGeometry;
        this.summary = summary;
        this.fragment = fragment;
        this.phaseCandidates = phaseCandidates;
        this.materialPoles = materialPoles;
        this.phaseReservoirs = phaseReservoirs;
        this.materialContacts = materialContacts;
        this.continuationFaceMask = continuationFaceMask;
        this.cellsResolved = cellsResolved;
        this.resolved = resolved;
    }

    WorkerBrickTopology withFragment(
            ThermalFragment nextFragment,
            boolean nextResolved,
            byte nextContinuationFaceMask
    ) {
        return new WorkerBrickTopology(
                span, coverageSlot, coverageGeneration, mixedGeometry, summary,
                nextFragment, phaseCandidates, materialPoles, phaseReservoirs,
                materialContacts, nextContinuationFaceMask, cellsResolved,
                nextResolved);
    }

    record MaterialPoles(
            int[] blockX,
            int[] blockY,
            int[] blockZ,
            int[] profileId,
            byte[] depth,
            int[] slot
    ) {
        static final MaterialPoles EMPTY = new MaterialPoles(
                new int[0], new int[0], new int[0], new int[0],
                new byte[0], new int[0]);

        MaterialPoles {
            int size = blockX.length;
            if (blockY.length != size || blockZ.length != size
                    || profileId.length != size || depth.length != size
                    || slot.length != size) {
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
    record MaterialContacts(
            int[] surfaceBlockX,
            int[] surfaceBlockY,
            int[] surfaceBlockZ,
            int[] surfaceProfileId,
            double[] surfaceArea,
            int[] surfacePoleOrdinal,
            int[] deepPoleOrdinal,
            int[] surfaceContactStart,
            int[] surfaceContactCount,
            long[] surfaceContactAirReference,
            int[] surfaceContactPatches,
            int[] phaseProfileId,
            long[] phaseCandidateMask,
            int[] phaseReservoirOrdinal,
            int[] phaseContactStart,
            int[] phaseContactCount,
            long[] phaseContactAirReference,
            int[] phaseContactPatches,
            long[] bridgeNegativeAirReference,
            long[] bridgePositiveAirReference,
            double[] bridgeConductanceWPerK
    ) {
        static final MaterialContacts EMPTY = new MaterialContacts(
                new int[0], new int[0], new int[0], new int[0],
                new double[0], new int[0], new int[0], new int[0], new int[0],
                new long[0], new int[0],
                new int[0], new long[0], new int[0], new int[0], new int[0],
                new long[0], new int[0],
                new long[0], new long[0], new double[0]);

        MaterialContacts {
            int surfaces = surfaceBlockX.length;
            int phases = phaseProfileId.length;
            int bridges = bridgeNegativeAirReference.length;
            if (surfaceBlockY.length != surfaces
                    || surfaceBlockZ.length != surfaces
                    || surfaceProfileId.length != surfaces
                    || surfaceArea.length != surfaces
                    || surfacePoleOrdinal.length != surfaces
                    || deepPoleOrdinal.length != surfaces
                    || surfaceContactStart.length != surfaces
                    || surfaceContactCount.length != surfaces
                    || surfaceContactAirReference.length
                            != surfaceContactPatches.length
                    || phaseCandidateMask.length != phases
                    || phaseReservoirOrdinal.length != phases
                    || phaseContactStart.length != phases
                    || phaseContactCount.length != phases
                    || phaseContactAirReference.length
                            != phaseContactPatches.length
                    || bridgePositiveAirReference.length != bridges
                    || bridgeConductanceWPerK.length != bridges) {
                throw new IllegalArgumentException(
                        "Brick material contact arrays are invalid");
            }
        }

        int surfaceCount() { return surfaceBlockX.length; }
        int phaseCount() { return phaseProfileId.length; }
        int bridgeCount() { return bridgeNegativeAirReference.length; }
    }
}
