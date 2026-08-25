/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.FarFieldProfileRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolution;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.LatestSolveEpochScheduler;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceChannel;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftThermalTopologyApplierTest {
    private static final long DIMENSION_GENERATION = 9L;
    private static final double AIR_CAPACITY = 1.0D;
    private static final ResolvedThermalSignature SOLID =
            new ResolvedThermalSignature(0, 0, List.of(), 0, 0, 0, 0, 0);

    @Test
    void mixedRebuildInstallsSweepAcknowledgesAndReleasesOnlyAfterRetirement() {
        try (Fixture fixture = fixture(16)) {
            mutate(fixture, 0, 0, 0, 5L);
            SealedInputFrame frame = fixture.seal(5L, 1L);

            MinecraftThermalTopologyApplier.ApplyReport applied =
                    fixture.applier.apply(frame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    applied.status());
            assertEquals(1, applied.rebuiltPages());
            assertEquals(64, fixture.arena.liveCellCount());
            int mixedSupport = fixture.page.coverageRefAtBase(0);
            assertTrue(fixture.arena.isMixedSupport(mixedSupport));
            assertEquals(GeometrySummary.Kind.MIXED,
                    fixture.page.geometrySummary(0).kind());
            assertTrue(fixture.runtime.sweepPairOperationCount() > 0);
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());

            fixture.applier.retirePage(fixture.page, 2L);
            SealedInputFrame retiredFrame = fixture.seal(10L, 2L);
            MinecraftThermalTopologyApplier.ApplyReport retired =
                    fixture.applier.apply(retiredFrame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    retired.status());
            assertEquals(1, retired.retiredPages());
            assertEquals(0, fixture.arena.liveCellCount());
            assertEquals(0, fixture.runtime.sweepPairOperationCount());
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
        }
    }

    @Test
    void completelySolidBaseBrickPublishesNoCoverageOrFalseOpening() {
        try (Fixture fixture = fixture(128)) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        mutate(fixture, x, y, z, 5L);
                    }
                }
            }
            SealedInputFrame frame = fixture.seal(5L, 1L);

            MinecraftThermalTopologyApplier.ApplyReport applied =
                    fixture.applier.apply(frame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    applied.status());
            assertEquals(ThermalPage.NO_COVERAGE, fixture.page.coverageRefAtBase(0));
            assertEquals(GeometrySummary.Kind.NO_AIR,
                    fixture.page.geometrySummary(0).kind());
            assertFalse((fixture.page.mixedBrickMask() & 1L) != 0L);
            assertEquals(63, fixture.arena.liveCellCount());
        }
    }

    @Test
    void stablePageReleasesItsDesiredSignatureCutAfterCommit() {
        try (Fixture fixture = fixture(16)) {
            assertEquals(0, fixture.applier.stagedSignaturePageCount());
            fixture.applier.apply(fixture.seal(5L, 1L));
            assertEquals(0, fixture.applier.stagedSignaturePageCount());

            mutate(fixture, 0, 0, 0, 10L);
            fixture.applier.apply(fixture.seal(10L, 1L));

            assertEquals(0, fixture.applier.stagedSignaturePageCount());
        }
    }

    @Test
    void affectedSourceFrameCompletesDegradedThenRebindsBeforeReplacement() {
        try (Fixture sourceFixture = fixture(8)) {
            sourceFixture.sources.offerRegister(
                    1L,
                    0L,
                    0,
                    1,
                    ThermalSourceMode.POWER_SOURCE,
                    10.0D,
                    true,
                    0L,
                    new EmissionPort[]{EmissionPort.of(
                            0,
                            SourceChannel.CONVECTION,
                            1.0D,
                            SourceBinding.thermalNode(0L, 1))});
            mutate(sourceFixture, 0, 0, 0, 5L);
            SealedInputFrame frame = sourceFixture.seal(5L, 1L);
            MinecraftThermalTopologyApplier.ApplyReport deferred =
                    sourceFixture.applier.apply(frame);

            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.SOURCE_REBIND_REQUIRED,
                    deferred.status());
            assertTrue(deferred.readyForSolve());
            assertEquals(1, sourceFixture.arena.liveCellCount());
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    sourceFixture.runtime.runOne().status());
            assertEquals(2.5D, sourceFixture.arena.enthalpyJ(0), 1.0e-12D);

            sourceFixture.sources.offerRebind(
                    1L, 0, SourceBinding.declaredLoss(100L), 5L);
            SealedInputFrame reboundFrame = sourceFixture.seal(5L, 1L);
            MinecraftThermalTopologyApplier.ApplyReport rebound =
                    sourceFixture.applier.apply(reboundFrame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    rebound.status());
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    sourceFixture.runtime.runOne().status());
            assertEquals(2.5D,
                    totalEnthalpy(sourceFixture.arena)
                            + sourceFixture.applier.migrationSnapshot().geometryEgressJ(),
                    1.0e-12D);
            assertTrue(sourceFixture.applier.migrationSnapshot()
                    .residualWithin(1.0e-12D));
            assertEquals(2.5D, sourceFixture.sources.routedEnergyJ(
                    1L, SourceBinding.Kind.THERMAL_NODE), 1.0e-12D);
        }
    }

    @Test
    void sourceBoundOutsideRebuiltPageDoesNotBlockTopology() {
        try (Fixture fixture = fixture(8)) {
            fixture.sources.offerRegister(
                    1L,
                    0L,
                    0,
                    1,
                    ThermalSourceMode.POWER_SOURCE,
                    10.0D,
                    true,
                    5L,
                    new EmissionPort[]{EmissionPort.of(
                            0,
                            SourceChannel.CONVECTION,
                            1.0D,
                            SourceBinding.thermalNode(10_000L, 1))});
            mutate(fixture, 0, 0, 0, 5L);
            SealedInputFrame frame = fixture.seal(5L, 1L);

            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(frame).status());
            assertTrue(fixture.arena.liveCellCount() > 1);
        }
    }

    @Test
    void fullResyncSnapshotRebuildsAndClearsStickyRequirement() {
        try (Fixture resyncFixture = fixture(8)) {
            long revision = resyncFixture.page.requireFullGeometryResync(
                    ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION);
            int[] solidPage = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
            assertTrue(resyncFixture.resolvedInputs.offerFullResync(
                    resyncFixture.page.sectionKey(),
                    resyncFixture.page.lifecycleGeneration(),
                    revision,
                    5L,
                    ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION,
                    solidPage));
            SealedInputFrame frame = resyncFixture.seal(5L, 1L);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    resyncFixture.applier.apply(frame).status());
            assertFalse(resyncFixture.page.fullGeometryResyncRequired());
            assertEquals(frame.watermarks().geometry(),
                    resyncFixture.runtime.appliedWatermarks().geometry());
            assertEquals(0, resyncFixture.arena.liveCellCount());
        }
    }

    @Test
    void approvedOpenFrontierCompilesAreaScaledFarFieldBoundary() {
        try (Fixture fixture = fixture(16, farFieldSettings())) {
            fixture.applier.updateSkyExposure(fixture.page, fullSkyExposure());
            fixture.arena.setEnthalpyJ(
                    0,
                    20.0D * fixture.arena.capacityJPerK(0));
            double before = fixture.arena.enthalpyJ(0);

            MinecraftThermalTopologyApplier.ApplyReport applied =
                    fixture.applier.apply(fixture.seal(5L, 1L));

            assertTrue(applied.topologyResolved());
            assertEquals(1, fixture.runtime.sweepBoundaryOperationCount());
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
            assertTrue(fixture.arena.enthalpyJ(0) < before);

            fixture.arena.setEnthalpyJ(0, 0.0D);
            assertTrue(fixture.applier.updateNaturalTemperature(
                    fixture.page, 10.0D, 0.25D));
            assertTrue(fixture.applier.apply(fixture.seal(10L, 1L))
                    .topologyResolved());
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
            assertTrue(fixture.arena.enthalpyJ(0) > 0.0D);

            MinecraftThermalTopologyApplier.ApplyReport unchanged =
                    fixture.applier.apply(fixture.seal(15L, 1L));
            assertEquals(0, unchanged.pairOperations());
            assertEquals(1, fixture.runtime.sweepBoundaryOperationCount());
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
        }
    }

    @Test
    void undergroundFrontierGetsWeakBoundaryButRemainsDegraded() {
        try (Fixture fixture = fixture(16, farFieldSettings())) {
            MinecraftThermalTopologyApplier.ApplyReport applied =
                    fixture.applier.apply(fixture.seal(5L, 1L));

            assertFalse(applied.topologyResolved());
            assertEquals(1, fixture.runtime.sweepBoundaryOperationCount());
            assertEquals(0b11_1111, fixture.applier.continuationFaceMask(fixture.page));
        }
    }

    private static void mutate(
            Fixture fixture,
            int localX,
            int localY,
            int localZ,
            long tick
    ) {
        ThermalPage.MutationObservation mutation = fixture.page.recordGeometryMutation(
                localX, localY, localZ, tick, fixture.geometryDeltas);
        assertTrue(fixture.resolvedInputs.offerResolvedCenter(
                fixture.page.sectionKey(),
                fixture.page.lifecycleGeneration(),
                mutation.geometryRevision(),
                tick,
                (localY << 8) | (localZ << 4) | localX,
                ThermalSignatureResolution.resolved(0)));
    }

    private static Fixture fixture(int resolvedCapacity) {
        return fixture(
                resolvedCapacity,
                MinecraftThermalTopologyApplier.FarFieldSettings.disabled());
    }

    private static Fixture fixture(
            int resolvedCapacity,
            MinecraftThermalTopologyApplier.FarFieldSettings farFieldSettings
    ) {
        long sectionKey = SectionPos.asLong(0, 0, 0);
        ThermalCellArena arena = new ThermalCellArena(1);
        ArenaSpan initial = arena.allocatePageCells(
                7,
                1,
                new ThermalCellArena.CellSpec[]{ThermalCellArena.CellSpec.regularAir(
                        0, 0, 0, 16, 0, 0, AIR_CAPACITY)},
                0.0D,
                0.0D);
        ThermalPage page = ThermalPage.allAir(sectionKey, 1L, initial.firstSlot(), 0);
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(8);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                DIMENSION_GENERATION,
                0L,
                16,
                new ThermalSourceRegistry(8, 2, 16, accumulators),
                arena);
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                budget.createDimensionBudget(1_000_000L, 0L),
                256);
        if (publication == null) {
            throw new IllegalStateException("test publication admission failed");
        }
        DimensionThermalRuntime runtime = new DimensionThermalRuntime(
                100L,
                DIMENSION_GENERATION,
                0L,
                InputWatermarks.ZERO,
                0L,
                0L,
                false,
                new ThermalTimePolicy(5L, 20L, 2),
                arena,
                sources,
                sweep,
                publication,
                0.0D,
                new DimensionThermalRuntime.Limits(256, 1024, 1024, 3, 1.0e-9D));
        ThermalSignatureRegistry.Builder signatureBuilder = ThermalSignatureRegistry.builder();
        signatureBuilder.intern(SOLID);
        ThermalSignatureRegistry signatures = signatureBuilder.build();
        GeometryDeltaRing geometryDeltas = new GeometryDeltaRing(16);
        ResolvedGeometryInputRing resolvedInputs =
                new ResolvedGeometryInputRing(resolvedCapacity);
        MinecraftThermalTopologyApplier applier = new MinecraftThermalTopologyApplier(
                runtime,
                signatures,
                geometryDeltas,
                resolvedInputs,
                new MinecraftThermalTopologyApplier.Parameters(
                        0,
                        0,
                        ConservativeAirGeometry.MICROCELL_COUNT,
                        AIR_CAPACITY,
                        0.0D,
                        0.0D,
                        1.0D,
                        0.25D,
                        false,
                        new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D)),
                MaterialBoundaryRegistry.empty(),
                farFieldSettings);
        applier.registerAllAirPage(page, 1L);
        return new Fixture(
                arena, page, runtime, sources, geometryDeltas, resolvedInputs, applier);
    }

    private static MinecraftThermalTopologyApplier.FarFieldSettings farFieldSettings() {
        FarFieldProfileRegistry.Key key = new FarFieldProfileRegistry.Key(
                0,
                FarFieldProfileRegistry.OpeningClass.MULTI_FACE,
                2,
                FarFieldProfileRegistry.Orientation.HORIZONTAL,
                FarFieldProfileRegistry.WindBucket.CALM,
                FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
                FarFieldProfileRegistry.TopologyClass.OPEN_SPACE);
        FarFieldProfileRegistry.Profile profile = new FarFieldProfileRegistry.Profile(
                key,
                10.0D,
                new FarFieldProfileRegistry.ApplicabilityDomain(100.0D, 100.0D),
                new FarFieldProfileRegistry.ErrorEnvelope(
                        0.0D, 0.0D, false, 0.0D, 0.0D, 0.0D),
                FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE);
        return new MinecraftThermalTopologyApplier.FarFieldSettings(
                new FarFieldProfileRegistry(List.of(profile)),
                true,
                FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
                FarFieldProfileRegistry.WindBucket.CALM,
                100.0D,
                32.0D,
                16.0D);
    }

    private static byte[] fullSkyExposure() {
        return new byte[16 * 16];
    }

    private static double totalEnthalpy(ThermalCellArena arena) {
        double total = 0.0D;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot)) {
                total += arena.enthalpyJ(slot);
            }
        }
        return total;
    }

    private record Fixture(
            ThermalCellArena arena,
            ThermalPage page,
            DimensionThermalRuntime runtime,
            ThermalSourceTimeline sources,
            GeometryDeltaRing geometryDeltas,
            ResolvedGeometryInputRing resolvedInputs,
            MinecraftThermalTopologyApplier applier
    ) implements AutoCloseable {
        private SealedInputFrame seal(long targetTick, long chunkWatermark) {
            page.sealGeometryDeltas(geometryDeltas);
            SealedInputFrame frame = new SealedInputFrame(
                    targetTick,
                    DIMENSION_GENERATION,
                    new InputWatermarks(
                            resolvedInputs.latestOfferedWatermark(),
                            sources.latestOfferedWatermark(),
                            chunkWatermark,
                            1L,
                            0L));
            LatestSolveEpochScheduler.SealResult result = runtime.sealFrame(frame);
            assertTrue(result == LatestSolveEpochScheduler.SealResult.ACCEPTED
                    || result == LatestSolveEpochScheduler.SealResult.DUPLICATE);
            return frame;
        }

        @Override
        public void close() {
            runtime.close();
        }
    }
}
