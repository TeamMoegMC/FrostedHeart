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
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialStateAccess;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.source.CutLoadBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirFieldCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.topology.SharedAirGridCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirGeometryView;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirShapeStore;
import com.teammoeg.frostedheart.content.climate.thermal.topology.PreparedTopologyChange;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.Arrays;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;

/**
 * One worker's continuous Air and material trial. The engine alone commits the result.
 * Spatial operators are compiled when geometry changes; stable steps reuse all vectors.
 */
public final class ContinuousAirSolver {
    public static final double TRANSPORT_TOLERANCE_C = 1.0e-3D;
    public static final double DEFAULT_CORRECTION_TOLERANCE_C = 1e-5;
    private static final double LOW = 0.5 - 0.5 / Math.sqrt(3);
    private static final double HIGH = 1 - LOW;
    private static final int[] DX = {-1, 1, 0, 0, 0, 0};
    private static final int[] DY = {0, 0, -1, 1, 0, 0};
    private static final int[] DZ = {0, 0, 0, 0, -1, 1};
    private final ThermalCellArena arena;
    private final MaterialStateAccess materialStates;
    private final MaterialSample materialSample = new MaterialSample();
    private final WorkerPageStore pages;
    private final ThermalSolver geometry;
    private final ThermalTopologyParameters parameters;
    private final MaterialBoundaryRegistry materials;
    private final AirLoadTable airLoads;
    private final AirFieldCompiler compiler = new AirFieldCompiler();
    private AirShapeStore shapes;
    private PendingRebuild pendingRebuild;
    private RegionalQuerySnapshot[] preparationMaterials;
    private boolean preparationHasMinimum;
    private final ThermalSignatureTable signatureTable;

    /** Immutable parent values, captured by the coordinator before construction helpers start. */
    void preparationMaterials(RegionalQuerySnapshot[] parents) {
        preparationMaterials = parents; preparationHasMinimum = false;
        for (var parent : parents) if (parent != null && parent.minimumQuality()) preparationHasMinimum = true;
    }
    private final double correctionToleranceC;
    private final RegionalAirRuntime.Scope regionScope;
    private AirFieldLayout layout;
    private CoupledThermalOperator operator;
    private LocalMaterialBoundary localMaterials;
    private int[] materialSlots = new int[0];
    private Int2IntOpenHashMap materialUnknownBySlot = new Int2IntOpenHashMap();
    private double[] stateC = new double[0], candidateC = new double[0], rhsJ = new double[0];
    private double[] powerW = new double[0];
    private double[] materialH = new double[0], candidateH = new double[0];
    private byte[] branches = new byte[0], candidateBranches = new byte[0];
    private int[] loadEvents = new int[0], loadMergeScratch = new int[0];
    private RegionalQuerySnapshot acceptedSnapshot;
    private MinimumThermalModel minimumModel;
    private MinimumThermalModel.Preparation minimumPreparation;
    private static final boolean[] NO_MATERIAL_WAITING = new boolean[0];
    private boolean[] minimumMaterialWaiting = NO_MATERIAL_WAITING;
    private double airPowerW;
    private double deliveredJ, unacceptedJ;
    private long acceptedSteps;
    private String lastTransportContactMode;
    private String lastRebuildTiming = "none";
    private boolean reusedGridStructure;
    private int gridStep = (1);
    private long lastContactTermScratchBytes;

    void gridStep(int step) { gridStep = step; }
    int gridStep() { return gridStep; }
    private final Long2ObjectOpenHashMap<RestoredSection> restorations = new Long2ObjectOpenHashMap<>();

    public void acceptAdmissions(ThermalInputBatch batch) {
        for (var retirement : batch.retirements()) restorations.remove(retirement.page().sectionKey());
        for (var admission : batch.admissions()) {
            if (admission.dormantAir() == null) continue;
            if ((regionScope.containsSection(admission.page().sectionKey())))
                restore(admission.page().sectionKey(), admission.dormantAir());
        }
        for (var residency : batch.residencyUpdates()) {
            if (residency.dormantAir() == null) continue;
            if ((regionScope.containsSection(residency.page().sectionKey())))
                restore(residency.page().sectionKey(), residency.dormantAir());
        }
    }

    public boolean hasPendingSourceResponse() { return (shapes.hasPendingSourceResponse()); }

    ContinuousAirSolver(ThermalCellArena arena, WorkerPageStore pages, ThermalSolver geometry,
            ThermalTopologyParameters parameters, MaterialBoundaryRegistry materials, AirLoadTable airLoads,
            ThermalSignatureTable signatures, double correctionToleranceC, RegionalAirRuntime.Scope scope,
            MaterialStateAccess materialStates) {
        this.arena = arena;
        this.materialStates = materialStates;
        this.pages = pages;
        this.geometry = geometry;
        this.parameters = parameters;
        this.materials = materials;
        this.airLoads = airLoads;
        this.correctionToleranceC = correctionToleranceC;
        this.signatureTable = signatures;
        this.regionScope = scope;
        shapes = new AirShapeStore(signatures);
    }

    public AirFieldLayout layout() { return (layout); }
    RegionalQuerySnapshot.TemplatePreparation prepareQueryTemplate(RegionalQuerySnapshot.Handle handle) {
        return new RegionalQuerySnapshot.TemplatePreparation(handle, layout, materialSlots, arena, this::materialSnapshotStateId);
    }
    private int materialSnapshotStateId(int slot) {
        var view = (regionScope);
        int signature = view.signatureAtWorld(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
        return signatureTable.valid(signature) ? signatureTable.materialStateId(signature) : -1;
    }
    RegionalQuerySnapshot.Preparation prepareSnapshot(RegionalQuerySnapshot.Template template, long tick, long topology) {
        return new RegionalQuerySnapshot.Preparation(template, stateC, materialH, branches, materialWaitingState(),
                arena, parameters.referenceTemperatureC(), tick, topology, acceptedSnapshot);
    }
    MinimumThermalModel.Projection prepareMinimumProjection(RegionalQuerySnapshot field) {
        return minimumModel.project(field);
    }
    MinimumThermalModel.Preparation prepareMinimumTransport() {
        return new MinimumThermalModel.Preparation(layout, operator, localMaterials, materialSlots, arena,
                (regionScope), parameters.referenceTemperatureC(), true);
    }
    long minimumTransportBytes() {
        return MinimumThermalModel.preparationBytes(layout.coefficientCount(), materialSlots.length);
    }
    void acceptPublishedState(RegionalQuerySnapshot snapshot) {
        acceptedSnapshot = snapshot;
    }
    /** Only private numerical arrays; geometry, immutable matrices and query arrays remain charged. */
    long privateWorkspaceBytes() {
        var meter = new AirBackingMeter();
        meter.array(stateC); meter.array(candidateC); meter.array(rhsJ); meter.array(powerW);
        meter.array(materialH); meter.array(candidateH); meter.array(branches); meter.array(candidateBranches);
        meter.array(loadEvents); meter.array(loadMergeScratch);
        meter.array(minimumMaterialWaiting);
        if (localMaterials != null) { meter.array(localMaterials.waiting); meter.array(localMaterials.trialWaiting); meter.array(localMaterials.stepH); }
        if (operator != null) {
            meter.array(operator.buoyancyFactors);
        }
        return meter.bytes();
    }
    void releaseDetailedWorkspaces() {
        operator = null; localMaterials = null;
        minimumModel = null; minimumPreparation = null;
        stateC = candidateC = rhsJ = powerW = materialH = candidateH = new double[0];
        branches = candidateBranches = new byte[0]; loadEvents = loadMergeScratch = new int[0];
        minimumMaterialWaiting = NO_MATERIAL_WAITING;
    }
    void restoreAcceptedState() {
        if (acceptedSnapshot == null) return;
        acceptedSnapshot.copyStateTo(stateC, materialH, branches, materialWaitingState());
    }
    RegionalQuerySnapshot.Restoration prepareAcceptedRestoration() {
        return acceptedSnapshot == null ? null : acceptedSnapshot.prepareRestoration(stateC, materialH, branches,
                materialWaitingState());
    }
    double[] committedCoefficientsC() {
        return acceptedSnapshot == null ? stateC : acceptedSnapshot.borrowedAirCoefficients();
    }
    private boolean[] materialWaitingState() {
        return localMaterials != null ? localMaterials.waiting : minimumMaterialWaiting;
    }
    long geometryVersion() { return geometry.structuralVersion(); }
    public RegionalQuerySnapshot acceptedSnapshot() { return acceptedSnapshot; }
    record DiagnosticCost(long acceptedSteps, LocalMaterialBoundary local) { }
    DiagnosticCost diagnosticCost() { return new DiagnosticCost(acceptedSteps, localMaterials); }

    public double deliveredEnergyJ() { return (deliveredJ); }
    public double unacceptedEnergyJ() { return (unacceptedJ); }
    public String contactModeSummary() {
        return lastTransportContactMode + ",correctionToleranceC=" + correctionToleranceC
                + ",acceptedSteps=" + acceptedSteps
                + "," + (localMaterials == null ? "materialTrial=minimum" : localMaterials.describe());
    }
    public String lastRebuildTiming() { return (lastRebuildTiming); }

    public void recordBacking(AirBackingMeter meter) {
        recordBacking(meter, null);
    }

    public void recordBacking(AirBackingMeter meter, StringBuilder description) {
        long before = meter.bytes();
        meter.array(materialSlots);
        meter.container(materialUnknownBySlot, materialUnknownBySlot.size(), 8);
        meter.array(stateC);
        meter.array(minimumMaterialWaiting);
        meter.array(candidateC);
        meter.array(rhsJ);
        meter.array(powerW);
        meter.array(materialH);
        meter.array(candidateH);
        meter.array(branches);
        meter.array(candidateBranches);
        meter.array(loadEvents);
        meter.array(loadMergeScratch);
        if (candidateBuild != null && candidateBuild.warmStart != null) {
            meter.array(candidateBuild.warmStart.minimumMass);
        }
        appendBacking(description, "vectors", meter.bytes() - before);
        before = meter.bytes();
        if (layout != null) layout.recordBacking(meter);
        appendBacking(description, "layout", meter.bytes() - before);
        before = meter.bytes();
        if (operator != null) operator.recordBacking(meter);
        if (minimumModel != null) minimumModel.recordBacking(meter);
        if (minimumPreparation != null) minimumPreparation.recordBacking(meter);
        if (acceptedSnapshot != null && acceptedSnapshot.minimumQuality())
            acceptedSnapshot.minimumState().recordGeometryBacking(meter);
        appendBacking(description, "operator", meter.bytes() - before);
        before = meter.bytes();
        shapes.recordBacking(meter);
        appendBacking(description, "shapes", meter.bytes() - before);
        before = meter.bytes();
        airLoads.recordBacking(meter);
        meter.containerEntries(restorations.size(), 16);
        appendBacking(description, "loadsRestores", meter.bytes() - before);
        if (pendingRebuild != null) {
            before = meter.bytes();
            pendingRebuild.previous.recordBacking(meter);
            appendBacking(description, "previous", meter.bytes() - before);
            before = meter.bytes();
            pendingRebuild.nextShapes.recordBacking(meter);
            appendBacking(description, "candidateShapes", meter.bytes() - before);
        }
    }

    private static void appendBacking(StringBuilder description, String category, long bytes) {
        if (description == null) return;
        if (description.length() != 0) description.append(',');
        description.append(category).append('=').append(bytes);
    }

    /** Material state is known before helpers run; shared Air node counts are not. */
    long preparationStateBytes() {
        long edges = regionScope.materialContactCount;
        return minimumConstruction() || edges == 0 ? 0 : Math.addExact(64L, Math.multiplyExact(edges, 16L));
    }

    private void reserveMinimumPreparation(ThermalMemoryBudget.Reservation credit) {
        long bytes = MinimumThermalModel.preparationBytes(layout.coefficientCount(), materialSlots.length, minimumConstruction());
        if (minimumConstruction()) bytes = Math.addExact(bytes, 16L + (long) layout.coefficientCount() * Double.BYTES);
        if (minimumConstruction() && credit != null) credit.completeResize(Math.addExact(credit.bytes(), bytes));
        else reservePreparationGraph(credit, bytes);
    }

    /** State preparation and actual graph allocation share one atomically grown owner. */
    static void reservePreparationGraph(ThermalMemoryBudget.Reservation scratch, long graphBytes) {
        if (scratch == null || graphBytes == 0) return;
        long required = Math.addExact(scratch.bytes(), graphBytes);
        if (!scratch.tryGrow(graphBytes)) throw new PreparationLimitedException(required);
    }

    public static final class PreparationLimitedException extends RuntimeException {
        PreparationLimitedException(long requiredScratchBytes) {
            super("Air graph preparation scratch " + requiredScratchBytes + " bytes refused");
        }
    }

    public void abortPreparedRebuild() {
        try {
            if (candidateBuild != null) { candidateBuild.before.restore(this); candidateBuild = null; }
            if (pendingRebuild == null) return;
            pendingRebuild.previous.restore(this);
            pendingRebuild = null;
        } finally { preparationMaterials = null; }
    }

    public void installPreparedRebuild() {
        try {
            if (pendingRebuild == null) throw new IllegalStateException("Air rebuild was not prepared");
            shapes = pendingRebuild.nextShapes;
            var restoredIterator = restorations.long2ObjectEntrySet().iterator();
            while (restoredIterator.hasNext()) {
                var entry = restoredIterator.next();
                RestoredSection restored = entry.getValue();
                if (!shapes.canConsumeRestoration(entry.getLongKey())) continue;
                restored.consumedBricks |= pages.capturedBrickMask(entry.getLongKey());
                boolean remaining = false;
                for (int brick = 0; brick < 64; brick++) if (restored.cut.hasBrick(brick) && (restored.consumedBricks & 1L << brick) == 0) { remaining = true; break; }
                if (!remaining) restoredIterator.remove();
            }
            shapes.projected();
            pendingRebuild = null;
        } finally { preparationMaterials = null; }
    }

    private CandidateBuild candidateBuild;
    private boolean minimumConstruction() { return gridStep == 4; }
    private boolean holdCandidateProjection;
    void holdCandidateProjection() { holdCandidateProjection = true; }
    boolean candidateProjectionHeld() {
        return holdCandidateProjection && candidateBuild != null && candidateBuild.phase == CandidatePhase.ALLOCATE_STATE;
    }
    void resumeCandidateProjection(AirFieldLayout previous, double[] values, RegionalQuerySnapshot[] materials) {
        if (!candidateProjectionHeld()) throw new IllegalStateException("Fine recovery has no prepared static operator");
        candidateBuild.previousLayout = previous; candidateBuild.previousC = values;
        preparationMaterials(materials); holdCandidateProjection = false;
    }
    int preparationPhase() { return candidateBuild == null ? 0 : candidateBuild.phase.ordinal(); }

    private enum CandidatePhase {
        CAPTURE_LAYOUT, DISCOVER_SUPPORT, NUMBER_GRID, COMPILE_VOLUMES, INDEX_MATERIALS,
        COLLECT_CONTACTS, ASSEMBLE_OPERATOR, BIND_MATERIALS, ALLOCATE_STATE,
        PROJECT_ENERGY, PROJECT_TEMPERATURE, COMPLETE;

        private static final CandidatePhase[] ORDER = values();
        CandidatePhase next() { return ORDER[ordinal() + 1]; }
    }

    /** Each completed phase keeps its work; yielding never restarts support discovery or projection. */
    private final class CandidateBuild {
        final SolverState before = new SolverState(ContinuousAirSolver.this);
        final FrozenSources bindings;
        final PreparedTopologyChange change;
        final ThermalMemoryBudget.Reservation graphCredit;
        AirFieldLayout previousLayout = layout;
        double[] previousC = stateC;
        final CoupledThermalOperator previousOperator = operator;
        final long startedNanos = System.nanoTime();
        final long[] phaseNanos = new long[CandidatePhase.COMPLETE.ordinal()];
        CandidatePhase phase = CandidatePhase.CAPTURE_LAYOUT;
        long projectionRhsNanos;
        CoupledThermalOperator.ProjectionPreparation projection;
        AirFieldLayout coarse;
        AirFieldCompiler.Preparation coarsePreparation;
        SharedAirGridCompiler.Preparation gridPreparation;
        AirShapeStore nextShapes;
        AirShapeStore.GridPreparation supportPreparation;
        AirOperatorFragment[] volumes;
        AirOperatorFragment.UnitGridPreparation unitVolumePreparation;
        int volumeCursor, projectionRhsCursor, materialInitialization;
        boolean vectorsAllocated;
        MinimumWarmStart warmStart;
        ContactPreparation contactPreparation;
        CoupledThermalOperator.Preparation operatorPreparation;
        LocalMaterialBoundary.Preparation materialPreparation;
        CoupledThermalOperator.Contacts contacts;
        AirGeometryView.CellCursor materialCursor;
        IntArrayList bodyBuilder;
        int materialCopy;
        boolean materialSlotsAllocated;
        String regionSummary = "";
        CandidateBuild(FrozenSources bindings, PreparedTopologyChange change,
                ThermalMemoryBudget.Reservation graphCredit) {
            this.bindings = bindings; this.change = change;
            this.graphCredit = graphCredit;
        }
        boolean run(long deadline) {
            do {
                if (holdCandidateProjection && phase == CandidatePhase.ALLOCATE_STATE) return false;
                if (advance(deadline)) return true;
            } while (System.nanoTime() < deadline);
            return false;
        }
        private boolean advance(long deadline) {
            CandidatePhase runningPhase = phase;
            long started = System.nanoTime();
            try {
                switch (phase) {
                    case CAPTURE_LAYOUT -> {
                        if (coarsePreparation == null) coarsePreparation = compiler.prepare(arena, regionScope);
                        if (!coarsePreparation.run(deadline)) return false;
                        coarse = coarsePreparation.layout(); coarsePreparation = null;
                    }
                    case DISCOVER_SUPPORT -> {
                        if (nextShapes == null) nextShapes = shapes.copy();
                        if (minimumConstruction()) {
                            nextShapes.holdUniformSupport();
                        } else {
                            if (supportPreparation == null) supportPreparation = nextShapes.beginSharedGrid(coarse, previousLayout,
                                    previousC, regionScope, bindings, parameters.referenceTemperatureC(),
                                    preparationHasMinimum ? (component, natural) -> previousResidualC(component, natural) : null);
                            if (!supportPreparation.run(deadline)) return false;
                        }
                    }
                    case NUMBER_GRID -> {
                        AirFieldLayout retainedGrid = gridStep != 4
                                && (previousLayout == null || !previousLayout.isAggregated())
                                ? coarse.withRetainedUnitGrid(previousLayout) : null;
                        reusedGridStructure = retainedGrid != null;
                        if (gridStep == 4) layout = coarse.asDirectCoarseGrid();
                        else {
                            if (retainedGrid != null) layout = retainedGrid;
                            else {
                                if (gridPreparation == null) gridPreparation = new SharedAirGridCompiler().prepare(coarse);
                                if (!gridPreparation.run(deadline)) return false;
                                layout = gridPreparation.layout(); gridPreparation = null;
                            }
                        }
                    }
                    case COMPILE_VOLUMES -> {
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.checkpoint();
                        if (layout.isUnitGrid()) {
                            if (unitVolumePreparation == null) unitVolumePreparation = new AirOperatorFragment.UnitGridPreparation(layout,
                                    parameters.effectiveAirCapacityJPerBlockK(), parameters.effectiveMixingWPerBlockK(), bindings);
                            if (!unitVolumePreparation.run(deadline)) return false;
                            volumes = unitVolumePreparation.result;
                            unitVolumePreparation = null;
                        } else {
                            if (volumes == null) volumes = new AirOperatorFragment[layout.componentCount()];
                            int end = Math.min(volumes.length, volumeCursor + 16);
                            compileCoarseVolumeRange(layout, volumes, bindings, volumeCursor, end);
                            volumeCursor = end;
                            if (volumeCursor != volumes.length) return false;
                        }
                    }
                    case INDEX_MATERIALS -> {
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.phase(
                                com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.Phase.CONTACTS);
                        if (materialCursor == null) {
                            int capacity = (regionScope.cells.length);
                            bodyBuilder = new IntArrayList(capacity);
                            materialUnknownBySlot = new Int2IntOpenHashMap(capacity);
                            materialUnknownBySlot.defaultReturnValue(-1);
                            materialCursor = regionScope.cellCursor(arena);
                        }
                        int remaining = 512;
                        while (remaining-- > 0 && materialCursor.hasNext()) {
                            int slot = materialCursor.nextSlot();
                            if (slot < 0 || !arena.isMaterialCell(slot)) continue;
                            materialUnknownBySlot.put(slot, layout.coefficientCount() + bodyBuilder.size());
                            bodyBuilder.add(slot);
                        }
                        if (materialCursor.hasNext()) return false;
                        if (!materialSlotsAllocated) { materialSlots = new int[bodyBuilder.size()]; materialSlotsAllocated = true; }
                        int end = Math.min(materialSlots.length, materialCopy + 4096);
                        System.arraycopy(bodyBuilder.elements(), materialCopy, materialSlots, materialCopy, end - materialCopy);
                        materialCopy = end;
                        if (materialCopy != materialSlots.length) return false;
                        bodyBuilder = null;
                    }
                    case COLLECT_CONTACTS -> {
                        {
                            if (contactPreparation == null) contactPreparation = new ContactPreparation(change);
                            if (!contactPreparation.run(deadline)) return false;
                            contacts = contactPreparation.contacts; contactPreparation = null;
                        }
                        lastContactTermScratchBytes = (contacts.indices.elements().length + (long) contacts.surfaceNodes.elements().length) * 4L
                                + contacts.weights.elements().length * 8L;
                        regionSummary = ThermalDiagnostics.TRACE
                                ? ThermalRegionPartition.describe(layout, volumes, contacts, regionScope.fragments, materialSlots, materialUnknownBySlot, arena) : "";
                    }
                    case ASSEMBLE_OPERATOR -> {
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.phase(
                                com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.Phase.OPERATOR);
                        if (minimumConstruction()) {
                            operator = null; localMaterials = null;
                            if (minimumPreparation == null) {
                                reserveMinimumPreparation(graphCredit);
                                minimumPreparation = new MinimumThermalModel.Preparation(
                                        layout, volumes, contacts, regionScope.fragments, materialSlots, arena, regionScope, parameters.referenceTemperatureC());
                            }
                            if (!minimumPreparation.run(deadline)) return false;
                            minimumModel = minimumPreparation.result(); minimumPreparation = null;
                        } else {
                            if (operatorPreparation == null) operatorPreparation = new CoupledThermalOperator.Preparation(
                                    layout, volumes, contacts, previousOperator,
                                    graphCredit == null ? null : () -> reservePreparationGraph(graphCredit,
                                            Math.addExact(CoupledThermalOperator.pairwiseGraphPreparationBytes(
                                                    regionScope.fineVolumes, regionScope.fineEdges, regionScope.airComponents),
                                                    CoupledThermalOperator.pairwiseSortScratchBytes(1))));
                            if (!operatorPreparation.run(deadline)) return false;
                            operator = operatorPreparation.result(); operatorPreparation = null;
                        }
                    }
                    case BIND_MATERIALS -> {
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.phase(
                                com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.Phase.CONTACTS);
                        if (minimumConstruction()) { minimumMaterialWaiting = new boolean[materialSlots.length]; break; }
                        minimumMaterialWaiting = NO_MATERIAL_WAITING;
                        {
                            if (materialPreparation == null) materialPreparation = new LocalMaterialBoundary.Preparation(
                                    layout, materialSlots, materialUnknownBySlot, arena, contacts, operator, regionScope.fragments);
                            if (!materialPreparation.run(deadline)) return false;
                            localMaterials = materialPreparation.result; materialPreparation = null;
                        }
                        operator.materialBoundary = localMaterials;
                    }
                    case ALLOCATE_STATE -> {
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.phase(
                                com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.Phase.PROJECTION);
                        if (!vectorsAllocated) {
                            int airRows = layout.coefficientCount();
                            stateC = new double[airRows];
                            if (!minimumConstruction()) {
                                candidateC = reserveRebuildScratch(candidateC, airRows);
                                rhsJ = reserveRebuildScratch(rhsJ, airRows);
                                Arrays.fill(rhsJ, 0);
                                powerW = reserveRebuildScratch(powerW, airRows + materialSlots.length);
                            }
                            materialH = new double[materialSlots.length];
                            if (!minimumConstruction()) candidateH = reserveRebuildScratch(candidateH, materialSlots.length);
                            branches = new byte[materialSlots.length];
                            candidateBranches = minimumConstruction() ? new byte[0] : new byte[materialSlots.length];
                            warmStart = minimumConstruction() ? new MinimumWarmStart() : null;
                            vectorsAllocated = true;
                        }
                        if (warmStart != null && !warmStart.run(deadline)) return false;
                        int end = Math.min(materialSlots.length, materialInitialization + 128);
                        readMaterials(materialInitialization, end);
                        materialInitialization = end;
                        if (materialInitialization != materialSlots.length) return false;
                        warmStart = null;
                    }
                    case PROJECT_ENERGY -> {
                        long projectionRhsStartedNanos = System.nanoTime();
                        int end = Math.min(layout.componentCount(), projectionRhsCursor + 8);
                        addNodalProjectionRhs(layout, previousLayout, previousC, rhsJ, projectionRhsCursor, end);
                        projectionRhsCursor = end;
                        projectionRhsNanos += System.nanoTime() - projectionRhsStartedNanos;
                        if (projectionRhsCursor < layout.componentCount()) return false;
                    }
                    case PROJECT_TEMPERATURE -> {
                        if (!project(deadline)) return false;
                    }
                    case COMPLETE -> { return true; }
                }
                // MinimumWarmStart already filled the coarse state; only fine builds project nodal energy.
                phase = phase == CandidatePhase.ALLOCATE_STATE && minimumConstruction()
                        ? CandidatePhase.COMPLETE : phase.next();
            } finally {
                if (runningPhase != CandidatePhase.COMPLETE) phaseNanos[runningPhase.ordinal()] += System.nanoTime() - started;
            }
            if (phase != CandidatePhase.COMPLETE) return false;
            long active = 0; for (long elapsed : phaseNanos) active += elapsed;
            lastRebuildTiming = "coarseMs=" + elapsed(CandidatePhase.CAPTURE_LAYOUT) / 1_000_000L
                    + ",shapesMs=" + elapsed(CandidatePhase.DISCOVER_SUPPORT) / 1_000_000L
                    + ",volumesMs=" + (elapsed(CandidatePhase.NUMBER_GRID) + elapsed(CandidatePhase.COMPILE_VOLUMES)) / 1_000_000L
                    + ",operatorMs=" + (elapsed(CandidatePhase.INDEX_MATERIALS) + elapsed(CandidatePhase.COLLECT_CONTACTS)
                            + elapsed(CandidatePhase.ASSEMBLE_OPERATOR) + elapsed(CandidatePhase.BIND_MATERIALS)) / 1_000_000L
                    + ",projectionMs=" + (elapsed(CandidatePhase.ALLOCATE_STATE) + elapsed(CandidatePhase.PROJECT_ENERGY)
                            + elapsed(CandidatePhase.PROJECT_TEMPERATURE)) / 1_000_000L
                    + ",prepareActiveUs=" + active / 1000 + ",prepareElapsedUs=" + (System.nanoTime() - startedNanos) / 1000
                    + ",sharedGridStructureReused=" + reusedGridStructure + ",fineAirNodes=" + layout.fineCoefficientCount()
                    + ",airNodes=" + layout.coefficientCount() + ",aggregationSpan=" + layout.aggregationSpan()
                    + ",contactTerms=" + (contacts.indices.size() + contacts.surfaceTerms) + ",contactTermScratchBytes=" + lastContactTermScratchBytes
                    + ",projectionRhsUs=" + projectionRhsNanos / 1000
                    + ",materials=" + materialSlots.length + "," + nextShapes.gridPreparationSummary()
                    + (operator == null ? ",boundedMinimum=true" : "," + operator.assemblyReuseSummary()
                            + "," + operator.contactModeSummary());
            if (localMaterials != null) lastRebuildTiming += "," + localMaterials.preparationSummary();
            if (!regionSummary.isEmpty()) lastRebuildTiming += "," + regionSummary;
            return true;
        }
        private long elapsed(CandidatePhase phase) { return phaseNanos[phase.ordinal()]; }
        private boolean project(long deadline) {
            if (projection == null) projection = operator.prepareProjection(rhsJ, stateC, correctionToleranceC);
            return projection.run(deadline);
        }

    }

    boolean prepareCandidateSlice(FrozenSources bindings, PreparedTopologyChange change,
            ThermalMemoryBudget.Reservation graphCredit, long deadline) {
        if (candidateBuild == null) {
            candidateBuild = new CandidateBuild(bindings, change, graphCredit);
            minimumPreparation = null;
        }
        var build = candidateBuild;
        try {
            if (!build.run(deadline)) return false;
            if (!minimumConstruction()) {
                if (System.nanoTime() >= deadline) return false;
                if (minimumPreparation == null) {
                    reserveMinimumPreparation(graphCredit);
                    minimumPreparation = new MinimumThermalModel.Preparation(
                            layout, operator, localMaterials, materialSlots, arena, regionScope, parameters.referenceTemperatureC());
                }
                if (!minimumPreparation.run(deadline)) return false;
                minimumModel = minimumPreparation.result(); minimumPreparation = null;
            }
            pendingRebuild = new PendingRebuild(build.before, build.nextShapes);
            candidateBuild = null;
            return true;
        } catch (RuntimeException | Error failure) {
            build.before.restore(this); candidateBuild = null;
            throw failure;
        }
    }

    /** Transfer the old cell-side nodal heat; unchanged geometry retains its nodal field without smoothing. */
    private void addNodalProjectionRhs(AirFieldLayout candidate, AirFieldLayout previous,
            double[] previousC, double[] destination, int fromComponent, int toComponent) {
        double mass = parameters.effectiveAirCapacityJPerBlockK() / 8;
        for (int index = fromComponent; index < toComponent; index++) {
            if ((index & 127) == 0) com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.checkpoint();
            var component = candidate.component(index);
            long blocks = component.airBlocks;
            while (blocks != 0) {
                int block = Long.numberOfTrailingZeros(blocks); blocks &= blocks - 1;
                int cx = component.minX + (block & 3), cy = component.minY + (block >>> 4);
                int cz = component.minZ + (block >>> 2 & 3);
                var old = previous == null ? null : previous.componentAt(cx, cy, cz);
                var restored = restorations.get(SectionPos.asLong(cx >> 4, cy >> 4, cz >> 4));
                int brick = (component.minX & 15) >>> 2 | ((component.minZ & 15) >>> 2) << 2
                        | ((component.minY & 15) >>> 2) << 4;
                for (int corner = 0; corner < 8; corner++) {
                    double x = cx + (corner & 1), y = cy + (corner >>> 2), z = cz + (corner >>> 1 & 1);
                    double offset = Double.NaN;
                    if (preparationHasMinimum) {
                        offset = previousTemperatureOffset(previous, previousC, component, Math.nextAfter(x, cx + .5),
                                Math.nextAfter(y, cy + .5), Math.nextAfter(z, cz + .5));
                    } else if (old != null && component.samePageLifetime(old) && old.isUnitGrid()) {
                        int oldBlock = (cx - old.minX) | (cz - old.minZ) << 2 | (cy - old.minY) << 4;
                        offset = previousC[old.coefficientIndex(old.unitCorner(oldBlock, corner))];
                    } else if (old != null && component.samePageLifetime(old)) offset = old.temperatureOffsetC(x, y, z, previousC);
                    if (!Double.isFinite(offset) && restored != null && (restored.consumedBricks & 1L << brick) == 0)
                        offset = restored.cut.temperatureC(Math.nextAfter(x, cx + .5), Math.nextAfter(y, cy + .5),
                                Math.nextAfter(z, cz + .5)) - parameters.referenceTemperatureC();
                    if (!Double.isFinite(offset)) offset = arena.allocatedTemperatureC(component.arenaSlot,
                            parameters.referenceTemperatureC()) - parameters.referenceTemperatureC();
                    destination[component.coefficientIndex(component.unitCorner(block, corner))] += mass * offset;
                }
            }
        }
    }

    private double previousTemperatureOffset(AirFieldLayout previous, double[] values, AirFieldLayout.Component current,
            double x, double y, double z) {
        var old = previous == null ? null : previous.componentAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (old == null || !current.samePageLifetime(old)) return Double.NaN;
        if (preparationHasMinimum && preparationMaterials != null) {
            for (var parent : preparationMaterials) if (parent != null) {
                double temperature = parent.airTemperatureC(x, y, z);
                if (Double.isFinite(temperature)) return temperature - parameters.referenceTemperatureC();
            }
            return Double.NaN;
        }
        return previous == null ? Double.NaN : previous.temperatureOffsetC(x, y, z, values);
    }

    private double previousResidualC(AirFieldLayout.Component component, double natural) {
        double residual = 0;
        if (preparationMaterials != null) for (var parent : preparationMaterials) if (parent != null) {
            double low = parent.minimumAtSlotC(component.arenaSlot), high = parent.maximumAtSlotC(component.arenaSlot);
            if (Double.isFinite(low) && Double.isFinite(high))
                residual = Math.max(residual, Math.max(Math.abs(low - natural), Math.abs(high - natural)));
        }
        return residual;
    }

    private record PendingRebuild(SolverState previous, AirShapeStore nextShapes) {}

    /** Accepted temperature/enthalpy/branch arrays stay private to each candidate. */
    private static double[] reserveRebuildScratch(double[] previous, int required) {
        if (previous.length >= required) return previous;
        return new double[Math.addExact(required, Math.max(256, required / 8))];
    }

    private static final class SolverState {
        final AirFieldLayout layout;
        final CoupledThermalOperator operator;
        final LocalMaterialBoundary localMaterials;
        final int[] materialSlots;
        final Int2IntOpenHashMap materialUnknownBySlot;
        final double[] stateC, candidateC, rhsJ, powerW, materialH, candidateH;
        final byte[] branches, candidateBranches;
        final MinimumThermalModel minimumModel;
        final boolean[] minimumMaterialWaiting;

        SolverState(ContinuousAirSolver solver) {
            layout = solver.layout;
            operator = solver.operator;
            localMaterials = solver.localMaterials;
            materialSlots = solver.materialSlots;
            materialUnknownBySlot = solver.materialUnknownBySlot;
            stateC = solver.stateC;
            candidateC = solver.candidateC;
            rhsJ = solver.rhsJ;
            powerW = solver.powerW;
            materialH = solver.materialH;
            candidateH = solver.candidateH;
            branches = solver.branches;
            candidateBranches = solver.candidateBranches;
            minimumModel = solver.minimumModel;
            minimumMaterialWaiting = solver.minimumMaterialWaiting;
        }

        void restore(ContinuousAirSolver solver) {
            solver.layout = layout;
            solver.operator = operator;
            solver.localMaterials = localMaterials;
            solver.materialSlots = materialSlots;
            solver.materialUnknownBySlot = materialUnknownBySlot;
            solver.stateC = stateC;
            solver.candidateC = candidateC;
            solver.rhsJ = rhsJ;
            solver.powerW = powerW;
            solver.materialH = materialH;
            solver.candidateH = candidateH;
            solver.branches = branches;
            solver.candidateBranches = candidateBranches;
            solver.minimumModel = minimumModel; solver.minimumPreparation = null;
            solver.minimumMaterialWaiting = minimumMaterialWaiting;
        }

        void recordBacking(AirBackingMeter meter) {
            meter.array(materialSlots);
            meter.container(materialUnknownBySlot, materialUnknownBySlot.size(), 8);
            meter.array(stateC);
            meter.array(candidateC);
            meter.array(rhsJ);
            meter.array(powerW);
            meter.array(materialH);
            meter.array(candidateH);
            meter.array(branches);
            meter.array(candidateBranches);
            meter.array(minimumMaterialWaiting);
            if (minimumModel != null) minimumModel.recordBacking(meter);
            if (layout != null) layout.recordBacking(meter);
            if (operator != null) operator.recordBacking(meter);
        }
    }

    private void compileCoarseVolumeRange(AirFieldLayout candidateLayout, AirOperatorFragment[] volumes,
            FrozenSources bindings, int first, int end) {
        AirMixingRegion mixing = new AirMixingRegion();
        for (int index = first; index < end; index++) {
            var component = candidateLayout.component(index);
            mixing.reset(component.minX, component.minY, component.minZ);
            bindings.collectMixingSources(mixing);
            volumes[index] = AirOperatorFragment.coarseGridVolume(component, parameters.effectiveAirCapacityJPerBlockK(),
                    parameters.effectiveMixingWPerBlockK(), mixing);
        }
    }

    private static final class RestoredSection {
        final ThermalInputBatch.DormantAirCut cut;
        long consumedBricks;
        RestoredSection(ThermalInputBatch.DormantAirCut cut) { this.cut = cut; }
    }

    private final class MinimumWarmStart {
        final double[] minimumMass = new double[layout.coefficientCount()];
        int phase, cursor;
        boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int end = Math.min(layout.componentCount(), cursor + 16);
                    for (; cursor < end; cursor++) minimumComponent(layout.component(cursor));
                    if (cursor == layout.componentCount()) { phase++; cursor = 0; }
                } else if (phase == 1) {
                    int length = minimumMass.length;
                    int end = Math.min(length, cursor + 4096);
                    for (; cursor < end; cursor++) {
                        double weight = minimumMass[cursor];
                        if (weight > 0) stateC[cursor] /= weight;
                        stateC[cursor] += minimumModel.backgroundOffsetsC()[cursor];
                    }
                    if (cursor == length) return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
        private void minimumComponent(AirFieldLayout.Component component) {
            long blocks = component.airBlocks;
            var restored = restorations.get(SectionPos.asLong(component.minX >> 4, component.minY >> 4, component.minZ >> 4));
            int brick = (component.minX & 15) >>> 2 | ((component.minZ & 15) >>> 2) << 2 | ((component.minY & 15) >>> 2) << 4;
            while (blocks != 0) {
                int block = Long.numberOfTrailingZeros(blocks); blocks &= blocks - 1;
                int bx = component.minX + (block & 3), by = component.minY + (block >>> 4), bz = component.minZ + (block >>> 2 & 3);
                for (int point = 0; point < 8; point++) {
                    double x = bx + (point & 1), y = by + (point >>> 2), z = bz + (point >>> 1 & 1);
                    double value = Double.NaN;
                    if (preparationMaterials != null) for (var parent : preparationMaterials) if (parent != null) {
                        var old = parent.airLayout().componentAt(bx, by, bz);
                        if (old != null && component.samePageLifetime(old)) {
                            value = parent.componentPerturbationC(old.arenaSlot, x, y, z,
                                    regionScope.naturalTemperatureC(arena.pageSlot(component.arenaSlot)));
                            break;
                        }
                    }
                    if (!Double.isFinite(value) && restored != null && (restored.consumedBricks & 1L << brick) == 0)
                        value = restored.cut.temperatureC(x, y, z) - parameters.referenceTemperatureC()
                                - component.temperatureOffsetC(x, y, z, minimumModel.backgroundOffsetsC());
                    if (!Double.isFinite(value)) value = 0;
                    for (int corner = 0; corner < 8; corner++) {
                        int row = component.coefficientIndex(corner);
                        double weight = component.weight(corner, x, y, z);
                        stateC[row] += weight * value; minimumMass[row] += weight;
                    }
                }
            }
        }
    }

    /** Retains original body/fragment/edge order while yielding within dense contact lists. */
    private final class ContactPreparation {
        final PreparedTopologyChange change;
        final CoupledThermalOperator.Contacts contacts;
        final int surfacePoints;
        final int[] faceNodes = new int[4];
        final ThermalFragment[] fragments;
        int body, phase, fragmentIndex, edge;
        ThermalFragment fragment;
        ContactPreparation(PreparedTopologyChange change) {
            this.change = change;
            contacts = new CoupledThermalOperator.Contacts();
            surfacePoints = LocalMaterialBoundary.surfacePoints(layout);
            fragments = regionScope.fragments;
        }
        boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int end = Math.min(materialSlots.length, body + 8);
                    for (; body < end; body++) materialBody(materialSlots[body]);
                    if (body == materialSlots.length) phase = 1;
                } else if (phase == 1) {
                    if (fragmentIndex == fragments.length) return true;
                    fragment = fragments[fragmentIndex++];
                    edge = 0; phase = 2;
                } else if (phase == 2) {
                    int end = Math.min(fragment.routedContacts().size(), edge + 64);
                    for (; edge < end; edge++) routedEdge(edge);
                    if (edge == fragment.routedContacts().size()) { phase = 3; edge = 0; }
                } else {
                    int end = Math.min(fragment.farBoundaries().patchCount(), edge + 16);
                    for (; edge < end; edge++) boundary(edge);
                    if (edge == fragment.farBoundaries().patchCount()) { fragment = null; phase = 1; }
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
        private void materialBody(int slot) {
            int bodyIndex = materialUnknownBySlot.get(slot);
            int x = arena.minimum(slot, 0), y = arena.minimum(slot, 1), z = arena.minimum(slot, 2);
            double conductance = materials.profileOrNull(arena.materialProfileId(slot)).faceConductanceWPerK();
            for (int face = 0; face < 6; face++) {
                var air = layout.componentAt(x + DX[face], y + DY[face], z + DZ[face]);
                if (air == null) continue;
                int axis = face / 2;
                boolean compact = air.isUnitGrid() && surfacePoints == 4;
                for (int point = 0; point < surfacePoints; point++) {
                    double u = surfacePoints == 1 ? .5 : (point & 1) == 0 ? LOW : HIGH;
                    double v = surfacePoints == 1 ? .5 : (point & 2) == 0 ? LOW : HIGH;
                    double px = x + (axis == 0 ? face % 2 : u);
                    double py = y + (axis == 1 ? face % 2 : axis == 0 ? u : v);
                    double pz = z + (axis == 2 ? face % 2 : v);
                    if (compact) faceNodes[point] = unitAirRow(air, px, py, pz);
                    else {
                        airTerms(contacts, air, px, py, pz, 1);
                        contacts.finishMaterialSurface(conductance / surfacePoints, bodyIndex - layout.coefficientCount(), null);
                    }
                }
                if (compact) contacts.surface(bodyIndex - layout.coefficientCount(), conductance / surfacePoints, faceNodes);
            }
        }
        private void routedEdge(int edge) {
            var routed = fragment.routedContacts();
            // Committed numerical steps already assign these edges zero
            // exchange. A new layout must not resolve their obsolete outlets.
            if (routed.conductance(edge) == 0 || routed.routes()[edge].retired() || change.retiresRoute(routed.routes()[edge])) return;
            if ((!regionScope.contains(routed.first(edge)))
                    && !regionScope.contains(routed.second(edge))) return;
            {
                int first = materialIndex(routed.first(edge)), second = materialIndex(routed.second(edge));
                if (first >= 0 || second >= 0) {
                    int body = Math.max(first, second) - layout.coefficientCount();
                    nodeTerms(contacts, first >= 0 ? routed.second(edge) : routed.first(edge),
                            first >= 0 ? routed.secondTrace(edge) : routed.firstTrace(edge), 1, routed.conductance(edge), routed.routes()[edge]);
                    contacts.finishMaterialSurface(routed.conductance(edge), body, routed.routes()[edge]);
                    return;
                }
            }
            nodeTerms(contacts, routed.first(edge), routed.firstTrace(edge), 1, routed.conductance(edge), routed.routes()[edge]);
            nodeTerms(contacts, routed.second(edge), routed.secondTrace(edge), -1, routed.conductance(edge), routed.routes()[edge]);
            contacts.finish(routed.conductance(edge), -1, routed.routes()[edge]);
        }
        private void boundary(int boundary) {
            var boundaries = fragment.farBoundaries();
            var component = layout.componentForSlot(boundaries.patchCell(boundary));
            if (component == null) return;
            long block = boundaries.patchBlock(boundary);
            int x = BlockPos.getX(block), y = BlockPos.getY(block), z = BlockPos.getZ(block);
            for (int point = 0; point < 4; point++) {
                airTerms(contacts, component, x + ((point & 1) == 0 ? LOW : HIGH), y + 1,
                        z + ((point & 2) == 0 ? LOW : HIGH), 1);
                contacts.finish(boundaries.patchConductance(boundary) * 0.25, boundaries.pageSlot(), null);
            }
        }
    }

    private int materialIndex(int slot) {
        return materialUnknownBySlot.get(slot);
    }

    private static void airTerms(CoupledThermalOperator.Contacts contacts, AirFieldLayout.Component component,
            double x, double y, double z, double sign) {
        if (component.isUnitGrid()) {
            int row = unitAirRow(component, x, y, z);
            if (row >= 0) contacts.term(row, sign);
            return;
        }
        for (int basis = 0; basis < component.basisCount(); basis++)
            contacts.term(component.coefficientIndex(basis), sign * component.weightAtAirCell(basis, -1, x, y, z));
    }

    private static int unitAirRow(AirFieldLayout.Component component, double x, double y, double z) {
        int cell = component.airCellAtPointForGrid(x, y, z);
        if (cell < 0) return -1;
        int corner = (x - component.minX - (cell & 3) > .5 ? 1 : 0)
                | (z - component.minZ - (cell >>> 2 & 3) > .5 ? 2 : 0)
                | (y - component.minY - (cell >>> 4) > .5 ? 4 : 0);
        return component.coefficientIndex(component.unitCorner(cell, corner));
    }

    private void nodeTerms(CoupledThermalOperator.Contacts contacts, int slot, ThermalFragment.AirFaceTrace trace,
            double sign, double conductance, com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity route) {
        if (trace == null || trace.size() == 0) throw new IllegalStateException("Continuous route lacks its physical outlet faces");
        double faceWeight = sign / trace.size();
        for (int patch = 0; patch < trace.size(); patch++) {
            long block = trace.block(patch);
            int x = BlockPos.getX(block), y = BlockPos.getY(block), z = BlockPos.getZ(block);
            var component = layout.componentAt(x, y, z);
            if (component == null) throw new IllegalStateException("Continuous route outlet has no captured Air: slot="
                    + slot + ",slotInRegion=" + ((regionScope.contains(slot)))
                    + ",outlet=" + x + "/" + y + "/" + z + ",patch=" + patch + "/" + trace.size()
                    + ",conductance=" + conductance + ",airComponents=" + layout.componentCount()
                    + ",outletSignature=" + ((regionScope.signatureAtWorld(x, y, z)))
                    + ",capturedSignature=" + ((regionScope.capturedSignatureAtWorld(x, y, z)))
                    + ",routeCommitted=" + route.activeForSolve());
            int face = trace.face(patch);
            airTerms(contacts, component, x + 0.5 + DX[face] * 0.5,
                    y + 0.5 + DY[face] * 0.5, z + 0.5 + DZ[face] * 0.5, faceWeight);
        }
    }

    private void resetSolveMetrics() {
        deliveredJ = 0;
        unacceptedJ = 0;
        acceptedSteps = 0;
        lastTransportContactMode = null;
        if (localMaterials != null) localMaterials.resetMetrics();
    }

    /** One numerical prefix; only its owning RegionJob may resume or discard it. */
    final class CutCursor {
        private static final int BLOCK = 1024;
        private final long toTick;
        private final CutLoadBuffer loads;
        private long tick, nextTick;
        private int phase, cursor, eventCount, event, mergeWidth = BLOCK;
        private int mergeStart, left, middle, right, mergeEnd, output;
        private int[] sorted, mergeTarget;
        private PairwiseCursor pairwise;
        private boolean done;

        CutCursor(long fromTick, long toTick, CutLoadBuffer loads) {
            this.tick = fromTick; this.toTick = toTick; this.loads = loads;
            resetSolveMetrics();
            if (operator == null || layout.coefficientCount() == 0 && materialSlots.length == 0) {
                unacceptedJ = loads.pendingEnergyJ(); done = true; return;
            }
            int capacity = Math.multiplyExact(loads.size(), 2);
            if (loadEvents.length < capacity) loadEvents = new int[capacity];
            if (loadMergeScratch.length < capacity) loadMergeScratch = new int[capacity];
            sorted = loadEvents; mergeTarget = loadMergeScratch;
            airPowerW = 0;
        }

        boolean run(long deadlineNanos) {
            if (done) return true;
            do {
                switch (phase) {
                    case 0 -> {
                        int end = Math.min(powerW.length, cursor + BLOCK);
                        Arrays.fill(powerW, cursor, end, 0); cursor = end;
                        if (cursor == powerW.length) { phase++; cursor = 0; }
                    }
                    case 1 -> {
                        int end = Math.min(loads.size(), cursor + BLOCK);
                        for (; cursor < end; cursor++) {
                            if (!ownsLoad(loads, cursor)) continue;
                            loadEvents[eventCount++] = cursor * 2;
                            if (!loads.impulse(cursor)) loadEvents[eventCount++] = cursor * 2 + 1;
                        }
                        if (cursor == loads.size()) { phase++; cursor = 0; }
                    }
                    case 2 -> {
                        int end = Math.min(eventCount, cursor + BLOCK);
                        IntArrays.quickSort(loadEvents, cursor, end, this::compare);
                        cursor = end;
                        if (cursor == eventCount) { phase++; cursor = 0; }
                    }
                    case 3 -> {
                        if (mergeWidth >= eventCount) { phase++; continue; }
                        if (output == mergeEnd) {
                            if (mergeStart >= eventCount) {
                                int[] previous = sorted; sorted = mergeTarget; mergeTarget = previous;
                                mergeWidth *= 2; mergeStart = output = mergeEnd = 0; continue;
                            }
                            left = mergeStart; middle = Math.min(eventCount, mergeStart + mergeWidth);
                            right = middle; mergeEnd = Math.min(eventCount, middle + mergeWidth); output = mergeStart;
                        }
                        int end = Math.min(mergeEnd, output + BLOCK);
                        for (; output < end; output++)
                            mergeTarget[output] = right >= mergeEnd || left < middle && compare(sorted[left], sorted[right]) <= 0
                                    ? sorted[left++] : sorted[right++];
                        if (output == mergeEnd) mergeStart = mergeEnd;
                    }
                    case 4 -> {
                        if (pairwise != null) {
                            if (!pairwise.run(deadlineNanos)) return false;
                            pairwise = null; acceptedSteps++; tick = nextTick;
                        }
                        int stop = Math.min(eventCount, event + BLOCK);
                        while (event < stop && eventTick(loads, sorted[event]) == tick) {
                            int encoded = sorted[event++], record = encoded >>> 1;
                            if (loads.impulse(record)) applyImpulse(loads, record);
                            else changePower(loads, record, (encoded & 1) == 0 ? 1 : -1);
                        }
                        if (event < eventCount && eventTick(loads, sorted[event]) == tick) continue;
                        if (tick == toTick) { done = true; return true; }
                        nextTick = Math.min(toTick, tick + ThermalInputBatch.CUT_INTERVAL_TICKS);
                        if (event < eventCount) nextTick = Math.min(nextTick, eventTick(loads, sorted[event]));
                        if (nextTick <= tick) throw new IllegalStateException("Input prefix crosses its numerical start time");
                        pairwise = new PairwiseCursor((nextTick - tick) / 20.0);
                    }
                    default -> throw new IllegalStateException("Unknown numerical cursor phase");
                }
            } while (!done && System.nanoTime() < deadlineNanos);
            return done;
        }

        private int compare(int first, int second) {
            int time = Long.compare(eventTick(loads, first), eventTick(loads, second));
            return time != 0 ? time : Integer.compare(first, second);
        }
    }

    CutCursor beginCut(long fromTick, long toTick, CutLoadBuffer loads) {
        return new CutCursor(fromTick, toTick, loads);
    }

    /** Newly retained scratch moves from this input's allowance to installed solver backing. */
    boolean inputArraysFit(int records) { return loadEvents.length >= 2L * records && loadMergeScratch.length >= 2L * records; }

    void reserveInputArrays(int records, ThermalMemoryBudget.Reservation input, ThermalMemoryBudget.Reservation retained) {
        int count = Math.multiplyExact(records, 2);
        long added = 0, old = 0;
        if (loadEvents.length < count) { added += 16L + 4L * count; old += 16L + 4L * loadEvents.length; }
        if (loadMergeScratch.length < count) { added += 16L + 4L * count; old += 16L + 4L * loadMergeScratch.length; }
        if (added == 0) return;
        input.transferTo(retained, added);
        if (loadEvents.length < count) {
            loadEvents = new int[count];
        }
        if (loadMergeScratch.length < count) {
            loadMergeScratch = new int[count];
        }
        retained.afterReplacement(old);
    }

    /** Same symmetric sweep ordering, with a bounded number of elements per resume. */
    private final class PairwiseCursor {
        private static final int BLOCK = 512;
        private final double seconds, referenceC = parameters.referenceTemperatureC();
        private final int air = layout.coefficientCount();
        private int phase, cursor;
        PairwiseCursor(double seconds) { this.seconds = seconds; }

        boolean run(long deadlineNanos) {
            do {
                int count = switch (phase) {
                    case 0, 19 -> air;
                    case 5, 15 -> air + materialSlots.length;
                    case 1, 4, 18 -> materialSlots.length;
                    case 2, 3, 16, 17 -> localMaterials.internalPairCount();
                    case 6, 11, 14 -> operator.pairwiseContactCount();
                    case 7, 9 -> operator.volumes.length;
                    case 8, 10, 12, 13 -> operator.pairwiseEdgeCount();
                    default -> 0;
                };
                // Each component contains at most 64 fine volumes; retain the former volume-sized checkpoints.
                int end = Math.min(count, cursor + (phase == 7 || phase == 9 ? BLOCK / 64 : BLOCK));
                switch (phase) {
                    case 0, 1, 4, 5, 15, 18, 19 -> ThermalWorkBudget.phase(ThermalWorkBudget.Phase.MATERIAL_STATE);
                    case 2, 3, 16, 17 -> ThermalWorkBudget.phase(ThermalWorkBudget.Phase.MATERIAL_INTERNAL);
                    case 11, 14 -> ThermalWorkBudget.phase(ThermalWorkBudget.Phase.MATERIAL_CONTACT);
                    default -> ThermalWorkBudget.phase(ThermalWorkBudget.Phase.TRANSPORT);
                }
                switch (phase) {
                    case 0 -> System.arraycopy(stateC, cursor, candidateC, cursor, end - cursor);
                    case 1 -> localMaterials.copyStepState(materialH, branches, candidateBranches, cursor, end);
                    case 2, 3 -> localMaterials.advanceInternalRange(localMaterials.stepH, candidateBranches,
                            seconds * .25, phase == 3, cursor, end);
                    case 4 -> System.arraycopy(localMaterials.stepH, cursor, candidateH, cursor, end - cursor);
                    case 5, 15 -> {
                        if (cursor == 0) deliveredJ += seconds * .5 * airPowerW;
                        addPairwisePower(seconds * .5, cursor, end);
                    }
                    case 6 -> operator.refreshContacts(geometry, referenceC, cursor, end);
                    case 7 -> {
                        for (int volume = cursor; volume < end; volume++) {
                            var probe = operator.volumes[volume];
                            operator.buoyancyFactors[volume] = probe.buoyancyFactor(candidateC, parameters.buoyancyParameters());
                        }
                    }
                    case 8 -> operator.clearPairwiseEdges(cursor, end);
                    case 9 -> operator.accumulatePairwiseVolumes(cursor, end);
                    case 10 -> operator.preparePairwiseFactors(seconds * .5, cursor, end);
                    case 11, 14 -> operator.sweepPairwiseContacts(candidateC, candidateH, candidateBranches,
                            seconds * .5, referenceC, phase == 14, cursor, end);
                    case 12, 13 -> operator.sweepPairs(candidateC, phase == 13, cursor, end);
                    case 16, 17 -> localMaterials.advanceInternalRange(candidateH, candidateBranches,
                            seconds * .25, phase == 17, cursor, end);
                    case 18 -> {
                        for (int body = cursor; body < end; body++) {
                            materialH[body] = candidateH[body]; branches[body] = candidateBranches[body];
                            localMaterials.waiting[body] = localMaterials.trialWaiting[body];
                        }
                    }
                    case 19 -> System.arraycopy(candidateC, cursor, stateC, cursor, end - cursor);
                    default -> throw new IllegalStateException("Unknown pairwise stage");
                }
                cursor = end;
                if (cursor == count) { cursor = 0; phase++; }
                if (phase == 20) {
                    lastTransportContactMode = operator.contactModeSummary() + "," + localMaterials.describe();
                    return true;
                }
            } while (System.nanoTime() < deadlineNanos);
            return false;
        }
    }

    private void addPairwisePower(double seconds, int from, int to) {
        int air = layout.coefficientCount();
        for (int row = from; row < Math.min(to, air); row++) if (powerW[row] != 0)
            candidateC[row] += seconds * powerW[row] / operator.massJPerK[row];
        for (int row = Math.max(from, air); row < to; row++) {
            double energy = seconds * powerW[row];
            if (energy == 0) continue;
            double accepted = localMaterials.addPairwisePower(row - air, candidateH, candidateBranches, energy);
            deliveredJ += accepted; unacceptedJ += energy - accepted;
        }
    }

    private static long eventTick(CutLoadBuffer loads, int event) {
        return (event & 1) == 0 ? loads.startTick(event >>> 1) : loads.endTick(event >>> 1);
    }

    private boolean ownsLoad(CutLoadBuffer loads, int record) {
        long target = loads.target(record);
        return SourceBinding.isAirTarget(target)
                ? loads.airLoad(record, airLoads).owner() == layout
                : regionScope.contains(SourceBinding.targetIndex(target));
    }

    AirShapeStore shapeStore() { return shapes; }

    void restore(long section, ThermalInputBatch.DormantAirCut cut) {
        restorations.put(section, new RestoredSection(cut));
        shapes.restore(cut, regionScope::containsShape);
    }

    void seedFrom(ContinuousAirSolver previous, AirShapeStore previousShapes, boolean exclusivePrevious) {
        seed(previous.layout, previous.committedCoefficientsC(), previousShapes);
        // A split may inherit the old field twice, but only one child may borrow
        // mutable operator scratch. Other children build independent workspaces.
        if (!exclusivePrevious) return;
        borrowWorkspace(previous);
    }

    /** Project all parent fields; only the selected donor supplies mutable workspaces. */
    void seedMerged(AirFieldLayout previousLayout, double[] previousC, AirShapeStore previousShapes,
            ContinuousAirSolver workspaceDonor) {
        seed(previousLayout, previousC, previousShapes);
        if (workspaceDonor != null) borrowWorkspace(workspaceDonor);
    }

    private void borrowWorkspace(ContinuousAirSolver previous) {
        operator = previous.operator;
        // The same region settles its old cut before preparing a replacement.
        // Reuse scratch exactly as the original in-place rebuild did; accepted C/H
        // and branches remain separate, so rejection retains the installed field.
        candidateC = previous.candidateC;
        rhsJ = previous.rhsJ;
        powerW = previous.powerW;
        candidateH = previous.candidateH;
        loadEvents = previous.loadEvents;
    }

    void seed(AirFieldLayout previous, double[] previousC, AirShapeStore previousShapes) {
        layout = previous;
        stateC = previousC;
        shapes = previousShapes;
    }

    private void changePower(CutLoadBuffer loads, int record, double sign) {
        double watts = sign * loads.amount(record);
        long target = loads.target(record);
        if (SourceBinding.isAirTarget(target)) {
            loads.airLoad(record, airLoads).add(watts, powerW);
            airPowerW += watts;
        } else {
            int index = materialIndex(SourceBinding.targetIndex(target));
            if (index < 0) throw new IllegalStateException("Material load lost its committed target");
            powerW[index] += watts;
        }
    }

    private void applyImpulse(CutLoadBuffer loads, int record) {
        long target = loads.target(record);
        double joules = loads.amount(record);
        if (SourceBinding.isAirTarget(target)) {
            // The shared grid has no cross-mass modes. This is the exact
            // diagonal mass solve and visits only the source's local stencil.
            loads.airLoad(record, airLoads).addLumped(joules, operator.massJPerK, stateC);
            deliveredJ += joules;
        } else {
            int body = materialIndex(SourceBinding.targetIndex(target)) - layout.coefficientCount();
            MaterialThermalLaw law = localMaterials.law(body);
            double remaining = joules;
            for (int segment = 0; segment < 3 && remaining != 0 && !materialWaiting(body); segment++) {
                double direction = Math.signum(remaining);
                branches[body] = law.selectBranch(materialH[body], branches[body], direction);
                double accepted = direction * Math.min(Math.abs(remaining), law.energyLimitJ(materialH[body], branches[body], direction));
                materialH[body] += accepted;
                remaining -= accepted;
                var transition = law.transition(branches[body]);
                if (transition != null && transition.complete(materialH[body])) setMaterialWaiting(body, true);
                if (accepted == 0) break;
            }
            deliveredJ += joules - remaining;
            unacceptedJ += remaining;
        }
    }

    private void readMaterials(int fromBody, int toBody) {
        for (int body = fromBody; body < toBody; body++) {
            int slot = materialSlots[body];
            if (preparationMaterials == null) materialStates.readMaterial(slot, materialSample);
            else {
                boolean found = false;
                for (var parent : preparationMaterials)
                    if (parent != null && parent.readMaterial(slot, arena.lifecycleGeneration(slot), arena.materialIncarnation(slot), materialSample)) {
                        found = true; break;
                    }
                // New staged bodies were initialized by the same local topology task.
                if (!found) arena.readMaterial(slot, materialSample);
            }
            materialH[body] = materialSample.enthalpyJ();
            branches[body] = materialSample.branch();
            setMaterialWaiting(body, arena.materialTransitionWaiting(slot));
        }
    }

    /** Called by the coordinator before the region runs; trials read only this cut's inputs. */
    void freezeStepInputs(double windScale, java.util.function.IntToDoubleFunction naturalTemperature) {
        if (operator != null) operator.freezeEnvironment(windScale, naturalTemperature, parameters.referenceTemperatureC());
    }

    void releaseStepInputs() {
        if (operator != null) operator.releaseEnvironment();
    }

    private boolean materialWaiting(int body) {
        return materialWaitingState()[body];
    }

    private void setMaterialWaiting(int body, boolean waiting) {
        materialWaitingState()[body] = waiting;
    }

}
