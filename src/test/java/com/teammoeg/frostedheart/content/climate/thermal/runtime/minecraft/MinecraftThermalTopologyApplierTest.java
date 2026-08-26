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
import com.teammoeg.frostedheart.content.climate.thermal.mesh.FarFieldProfileRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
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
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweepFragments;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftThermalTopologyApplierTest {
    private static final long DIMENSION_GENERATION = 9L;
    private static final double AIR_CAPACITY = 1.0D;
    private static final ResolvedThermalSignature SOLID =
            new ResolvedThermalSignature(0, 0, List.of(), 0, 0, 0, 0, 0);
    private static final ResolvedThermalSignature AIR = new ResolvedThermalSignature(
            0,
            0,
            List.of(new LocalAirRegionPattern(
                    0, -1L,
                    0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff)),
            0, 0, 0, 0, 0);

    @Test
    void mixedRebuildInstallsSweepAcknowledgesAndReleasesOnlyAfterRetirement() {
        try (Fixture fixture = fixture(16)) {
            mutate(fixture, 0, 0, 0, 5L);
            SealedInputFrame frame = fixture.seal(5L, 1L);

            MinecraftThermalTopologyApplier.ApplyStatus applied =
                    fixture.applier.apply(frame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    applied);
            assertEquals(64, fixture.arena.liveCellCount());
            int mixedSupport = coverageAt(fixture.page, 0);
            assertTrue(fixture.arena.isMixedSupport(mixedSupport));
            assertEquals(GeometrySummary.Kind.MIXED,
                    fixture.page.geometrySummary(0).kind());
            fixture.runtime.runOne();
            assertEquals(5L, fixture.runtime.lastCompletedTargetTick());

            fixture.applier.retirePage(fixture.page, 2L);
            SealedInputFrame retiredFrame = fixture.seal(10L, 2L);
            MinecraftThermalTopologyApplier.ApplyStatus retired =
                    fixture.applier.apply(retiredFrame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    retired);
            assertEquals(0, fixture.arena.liveCellCount());
            fixture.runtime.runOne();
            assertEquals(10L, fixture.runtime.lastCompletedTargetTick());
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

            MinecraftThermalTopologyApplier.ApplyStatus applied =
                    fixture.applier.apply(frame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    applied);
            assertEquals(ThermalPage.NO_COVERAGE, coverageAt(fixture.page, 0));
            assertEquals(GeometrySummary.Kind.NO_AIR,
                    fixture.page.geometrySummary(0).kind());
            assertFalse((fixture.page.mixedBrickMask() & 1L) != 0L);
            assertEquals(63, fixture.arena.liveCellCount());
        }
    }

    @Test
    void laterMutationReplacesOnlyItsFourCubedBrickFragment() {
        try (Fixture fixture = fixture(32)) {
            mutate(fixture, 0, 0, 0, 5L);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(5L, 1L)));

            int changedSupport = coverageAt(fixture.page, 0);
            int stableSupport = coverageAt(fixture.page, 1);
            fixture.arena.setEnthalpyJ(stableSupport, 17.0D);

            mutate(fixture, 1, 0, 0, 10L);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(10L, 1L)));

            assertFalse(fixture.arena.isLive(changedSupport));
            assertTrue(fixture.arena.isLive(stableSupport));
            assertEquals(stableSupport, coverageAt(fixture.page, 1));
            assertEquals(17.0D, fixture.arena.enthalpyJ(stableSupport), 0.0D);
            assertTrue(coverageAt(fixture.page, 0) != changedSupport);
        }
    }

    @Test
    void breakAndReplaceWithinOneBatchCancelsTheFinalUnchangedTopology() {
        try (Fixture fixture = fixture(32)) {
            mutateTo(fixture, 0, 0, 0, 5L, 0);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(5L, 1L)));
            int installedSupport = coverageAt(fixture.page, 0);
            int installedCells = fixture.arena.liveCellCount();

            mutateTo(fixture, 0, 0, 0, 10L, 1);
            mutateTo(fixture, 0, 0, 0, 10L, 0);
            MinecraftThermalTopologyApplier.ApplyStatus applied =
                    fixture.applier.apply(fixture.seal(10L, 1L));

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    applied);
            assertEquals(installedSupport, coverageAt(fixture.page, 0));
            assertEquals(installedCells, fixture.arena.liveCellCount());
            assertTrue(fixture.page.publishedGeometryIsCurrent());
        }
    }

    @Test
    void localMutationDoesNotRevisitAnUnrelatedAirComponent() {
        try (Fixture fixture = fixture(64)) {
            int[] distantAir = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
            Arrays.fill(distantAir, 1);
            ThermalPage distant = fixture.applier.registerCapturedPage(
                    SectionPos.asLong(2, 0, 0), 2L, 1L, distantAir);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(5L, 1L)));
            int stableSupport = coverageAt(distant, 0);
            fixture.arena.setEnthalpyJ(stableSupport, 19.0D);

            mutate(fixture, 0, 0, 0, 10L);
            MinecraftThermalTopologyApplier.ApplyStatus applied =
                    fixture.applier.apply(fixture.seal(10L, 1L));

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    applied);
            assertEquals(stableSupport, coverageAt(distant, 0));
            assertEquals(19.0D, fixture.arena.enthalpyJ(stableSupport), 0.0D);
        }
    }

    @Test
    void openingAndClosingBridgeMergesAndSplitsOnlyTouchedComponents() {
        try (Fixture fixture = fixture(64)) {
            int[] dividedAir = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
            Arrays.fill(dividedAir, 1);
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    dividedAir[(y << 8) | (z << 4) | 8] = 0;
                }
            }
            ThermalPage divided = fixture.applier.registerCapturedPage(
                    SectionPos.asLong(2, 0, 0), 2L, 1L, dividedAir);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(5L, 1L)));
            long closedGeneration = fixture.runtime.topologyGeneration();

            mutatePageTo(fixture, divided, 8, 8, 8, 10L, 1);
            divided.sealGeometryDeltas(fixture.geometryDeltas);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(10L, 1L)));
            assertTrue(fixture.runtime.topologyGeneration() > closedGeneration);
            long openGeneration = fixture.runtime.topologyGeneration();

            mutatePageTo(fixture, divided, 8, 8, 8, 15L, 0);
            divided.sealGeometryDeltas(fixture.geometryDeltas);
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(15L, 1L)));
            assertTrue(fixture.runtime.topologyGeneration() > openGeneration);
        }
    }

    @Test
    void affectedSourceFrameCompletesDegradedThenRebindsBeforeReplacement() {
        try (Fixture sourceFixture = fixture(8)) {
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    sourceFixture.applier.apply(sourceFixture.seal(0L, 1L)));
            sourceFixture.runtime.runOne();
            int sourceSlot = coverageAt(sourceFixture.page, 0);
            assertTrue(sourceFixture.arena.isLive(sourceSlot));

            sourceFixture.sources.offerRegister(
                    1L,
                    1,
                    ThermalSourceMode.POWER_SOURCE,
                    10.0D,
                    true,
                    0L,
                    new EmissionPort[]{EmissionPort.of(
                            0,
                            1.0D,
                            SourceBinding.thermalNode(sourceSlot, 1))});
            mutate(sourceFixture, 0, 0, 0, 5L);
            SealedInputFrame frame = sourceFixture.seal(5L, 1L);
            MinecraftThermalTopologyApplier.ApplyStatus deferred =
                    sourceFixture.applier.apply(frame);

            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    deferred);
            assertFalse(sourceFixture.runtime.topologyResolved());
            assertTrue(sourceFixture.arena.isLive(sourceSlot));
            assertFalse(sourceFixture.page.publishedGeometryIsCurrent());
            sourceFixture.runtime.runOne();
            assertEquals(2.5D, sourceFixture.arena.enthalpyJ(sourceSlot), 1.0e-12D);

            sourceFixture.sources.offerRebind(
                    1L, 0, SourceBinding.declaredLoss(100L), 5L);
            SealedInputFrame reboundFrame = sourceFixture.seal(5L, 1L);
            MinecraftThermalTopologyApplier.ApplyStatus rebound =
                    sourceFixture.applier.apply(reboundFrame);

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    rebound);
            sourceFixture.runtime.runOne();
            assertFalse(sourceFixture.arena.isLive(sourceSlot));
            assertEquals(2.5D * 63.0D / 64.0D,
                    totalEnthalpy(sourceFixture.arena), 1.0e-12D);
            assertEquals(2.5D, sourceFixture.sources.routedEnergyJ(
                    1L, SourceBinding.Kind.THERMAL_NODE), 1.0e-12D);
        }
    }

    @Test
    void sourceBoundOutsideRebuiltPageDoesNotBlockTopology() {
        try (Fixture fixture = fixture(8)) {
            fixture.sources.offerRegister(
                    1L,
                    1,
                    ThermalSourceMode.POWER_SOURCE,
                    10.0D,
                    true,
                    5L,
                    new EmissionPort[]{EmissionPort.of(
                            0,
                            1.0D,
                            SourceBinding.thermalNode(10_000L, 1))});
            mutate(fixture, 0, 0, 0, 5L);
            SealedInputFrame frame = fixture.seal(5L, 1L);

            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(frame));
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
                    resyncFixture.applier.apply(frame));
            assertFalse(resyncFixture.page.fullGeometryResyncRequired());
            assertEquals(frame.watermarks().geometry(),
                    resyncFixture.runtime.appliedWatermarks().geometry());
            assertEquals(0, resyncFixture.arena.liveCellCount());
        }
    }

    @Test
    void capturedAdmissionDefersAllocationAndResyncRebuildsOnlyChangedBrick() {
        try (Fixture fixture = fixture(32)) {
            int[] airPage = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
            Arrays.fill(airPage, 1);
            int beforeAdmission = fixture.arena.liveCellCount();
            ThermalPage captured = fixture.applier.registerCapturedPage(
                    SectionPos.asLong(1, 0, 0), 2L, 1L, airPage);

            assertEquals(beforeAdmission, fixture.arena.liveCellCount());
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(5L, 1L)));

            int replacedSupport = coverageAt(captured, 0);
            int stableSupport = coverageAt(captured, 1);
            fixture.arena.setEnthalpyJ(stableSupport, 17.0D);
            int[] oneSolid = airPage.clone();
            oneSolid[0] = 0;
            long changedRevision = captured.requireFullGeometryResync(
                    ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION);
            assertTrue(fixture.resolvedInputs.offerFullResync(
                    captured.sectionKey(),
                    captured.lifecycleGeneration(),
                    changedRevision,
                    10L,
                    ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION,
                    oneSolid));

            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(10L, 1L)));
            assertFalse(fixture.arena.isLive(replacedSupport));
            assertEquals(stableSupport, coverageAt(captured, 1));
            assertEquals(17.0D, fixture.arena.enthalpyJ(stableSupport), 0.0D);
            assertFalse(captured.fullGeometryResyncRequired());

            int currentChangedSupport = coverageAt(captured, 0);
            long unchangedRevision = captured.requireFullGeometryResync(
                    ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION);
            assertTrue(fixture.resolvedInputs.offerFullResync(
                    captured.sectionKey(),
                    captured.lifecycleGeneration(),
                    unchangedRevision,
                    15L,
                    ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION,
                    oneSolid));

            MinecraftThermalTopologyApplier.ApplyStatus unchanged =
                    fixture.applier.apply(fixture.seal(15L, 1L));
            assertFalse(captured.fullGeometryResyncRequired());
            assertTrue(captured.publishedGeometryIsCurrent());
            assertEquals(
                    MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    unchanged);
            assertEquals(currentChangedSupport, coverageAt(captured, 0));
            assertEquals(stableSupport, coverageAt(captured, 1));
            assertTrue(fixture.arena.isLive(currentChangedSupport));
            assertTrue(fixture.arena.isLive(stableSupport));
        }
    }

    @Test
    void approvedOpenFrontierCompilesAreaScaledFarFieldBoundary() {
        try (Fixture fixture = fixture(16, farFieldSettings())) {
            fixture.applier.updateSkyExposure(fixture.page, fullSkyExposure());
            MinecraftThermalTopologyApplier.ApplyStatus applied =
                    fixture.applier.apply(fixture.seal(5L, 1L));

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED, applied);
            assertTrue(fixture.runtime.topologyResolved());
            fixture.arena.setEnthalpyJ(
                    0,
                    20.0D * fixture.arena.capacityJPerK(0));
            double before = fixture.arena.enthalpyJ(0);
            fixture.runtime.runOne();
            assertTrue(fixture.arena.enthalpyJ(0) < before);

            fixture.arena.setEnthalpyJ(0, 0.0D);
            assertTrue(fixture.applier.updateNaturalTemperature(
                    fixture.page, 10.0D, 0.25D));
            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(10L, 1L)));
            assertTrue(fixture.runtime.topologyResolved());
            fixture.runtime.runOne();
            assertTrue(fixture.arena.enthalpyJ(0) > 0.0D);

            fixture.arena.setEnthalpyJ(0, 0.0D);
            assertTrue(fixture.applier.updateNaturalTemperature(
                    fixture.page, -10.0D, 0.25D));
            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    fixture.applier.apply(fixture.seal(15L, 1L)));
            assertTrue(fixture.runtime.topologyResolved());
            fixture.runtime.runOne();
            assertTrue(fixture.arena.enthalpyJ(0) < 0.0D);

            MinecraftThermalTopologyApplier.ApplyStatus unchanged =
                    fixture.applier.apply(fixture.seal(20L, 1L));
            assertTrue(unchanged == MinecraftThermalTopologyApplier.ApplyStatus.APPLIED
                    || unchanged == MinecraftThermalTopologyApplier.ApplyStatus.DUPLICATE);
            fixture.runtime.runOne();
        }
    }

    @Test
    void undergroundFrontierGetsWeakBoundaryButRemainsDegraded() {
        try (Fixture fixture = fixture(16, farFieldSettings())) {
            MinecraftThermalTopologyApplier.ApplyStatus applied =
                    fixture.applier.apply(fixture.seal(5L, 1L));

            assertEquals(MinecraftThermalTopologyApplier.ApplyStatus.APPLIED, applied);
            assertFalse(fixture.runtime.topologyResolved());
            assertEquals(0b11_1111, fixture.applier.continuationFaceMask(fixture.page));
            fixture.arena.setEnthalpyJ(
                    0, 20.0D * fixture.arena.capacityJPerK(0));
            double before = fixture.arena.enthalpyJ(0);
            fixture.runtime.runOne();
            assertTrue(fixture.arena.enthalpyJ(0) < before);
        }
    }

    private static void mutate(
            Fixture fixture,
            int localX,
            int localY,
            int localZ,
            long tick
    ) {
        mutateTo(fixture, localX, localY, localZ, tick, 0);
    }

    private static void mutateTo(
            Fixture fixture,
            int localX,
            int localY,
            int localZ,
            long tick,
            int signatureId
    ) {
        mutatePageTo(
                fixture, fixture.page,
                localX, localY, localZ, tick, signatureId);
    }

    private static void mutatePageTo(
            Fixture fixture,
            ThermalPage page,
            int localX,
            int localY,
            int localZ,
            long tick,
            int signatureId
    ) {
        ThermalPage.MutationObservation mutation = page.recordGeometryMutation(
                localX, localY, localZ, tick, fixture.geometryDeltas);
        assertTrue(fixture.resolvedInputs.offerResolvedCenter(
                page.sectionKey(),
                page.lifecycleGeneration(),
                mutation.geometryRevision(),
                tick,
                (localY << 8) | (localZ << 4) | localX,
                ThermalSignatureResolution.resolved(signatureId)));
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
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(8);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                DIMENSION_GENERATION,
                0L,
                16,
                new ThermalSourceRegistry(8, 2, accumulators),
                arena);
        ThermalSweep sweep = ThermalSweepFragments.builder(
                arena, null,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D), 0).build();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                budget.createDimensionBudget(1_000_000L, 0L),
                256);
        if (publication == null) {
            throw new IllegalStateException("test publication admission failed");
        }
        DimensionThermalRuntime runtime = new DimensionThermalRuntime(
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
        signatureBuilder.intern(AIR);
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
        int[] initialSignatures = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
        Arrays.fill(initialSignatures, 1);
        ThermalPage page = applier.registerCapturedPage(
                sectionKey, 1L, 1L, initialSignatures);
        return new Fixture(
                arena, page, runtime, sources, geometryDeltas, resolvedInputs, applier);
    }

    private static MinecraftThermalTopologyApplier.FarFieldSettings farFieldSettings() {
        FarFieldProfileRegistry.Profile profile = new FarFieldProfileRegistry.Profile(
                FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
                10.0D,
                new FarFieldProfileRegistry.ApplicabilityDomain(100.0D, 100.0D));
        return new MinecraftThermalTopologyApplier.FarFieldSettings(
                new FarFieldProfileRegistry(List.of(profile)),
                true,
                FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
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

    private static int coverageAt(ThermalPage page, int baseIndex) {
        return page.coverageSnapshot()[baseIndex];
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
