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
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftMaterialBoundaryTest {
    private static final long DIMENSION_GENERATION = 19L;
    private static final int AIR = 0;
    private static final int STATELESS = 1;
    private static final int CAPACITIVE = 2;
    private static final int NATURAL_ROCK = 3;
    private static final int PHASE = 4;
    private static final int FULL_CONTACT = 1;

    private static final ResolvedThermalSignature AIR_SIGNATURE =
            new ResolvedThermalSignature(
                    0,
                    0,
                    List.of(new LocalAirRegionPattern(
                            0,
                            -1L,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK)),
                    0, 0, 0, 0, 0);

    @Test
    void statelessBridgeCrossesExactlyOneConfirmedBarrierBlock() {
        int[] oneBlockWall = allAir();
        fillXPlane(oneBlockWall, 8, STATELESS);
        int[] twoBlockWall = allAir();
        fillXPlane(twoBlockWall, 7, STATELESS);
        fillXPlane(twoBlockWall, 8, STATELESS);

        try (Fixture thin = Fixture.create(oneBlockWall);
             Fixture thick = Fixture.create(twoBlockWall)) {
            thin.applyTopology(5L, 1L);
            thick.applyTopology(5L, 1L);
            setSeparatedAirTemperatures(thin.arena, 8.0D, 9.0D);
            setSeparatedAirTemperatures(thick.arena, 7.0D, 9.0D);

            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    thin.runtime.runOne().status());
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    thick.runtime.runOne().status());

            assertTrue(airEnthalpyRightOf(thin.arena, 9.0D) > 0.0D,
                    "a confirmed one-block wall must transfer through its stateless G");
            assertEquals(0.0D, airEnthalpyRightOf(thick.arena, 9.0D), 1.0e-12D,
                    "two solid blocks must not collapse into one cross-wall G");
            assertEquals(0, materialPoleCount(thin.arena),
                    "a stateless wall must not allocate solid heat state");
            assertEquals(0, materialPoleCount(thick.arena));
        }
    }

    @Test
    void capacitiveSurfaceStoresReleasesMigratesAndUnloadsWithItsPage() {
        int[] snapshot = allAir();
        fillXPlane(snapshot, 8, CAPACITIVE);
        try (Fixture fixture = Fixture.create(snapshot)) {
            fixture.applyTopology(5L, 1L);
            forEachLiveAir(fixture.arena, slot -> fixture.arena.setEnthalpyJ(
                    slot, fixture.arena.capacityJPerK(slot) * 100.0D));

            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
            double stored = materialEnthalpy(fixture.arena);
            assertTrue(stored > 0.0D, "the wall surface must retain residual heat");

            ThermalPage.MutationObservation mutation = fixture.page.recordGeometryMutation(
                    0, 0, 0, 10L, fixture.geometryDeltas);
            assertTrue(fixture.resolvedInputs.offerResolvedCenter(
                    fixture.page.sectionKey(),
                    fixture.page.lifecycleGeneration(),
                    mutation.geometryRevision(),
                    10L,
                    0,
                    ThermalSignatureResolution.resolved(AIR)));
            fixture.applyTopology(10L, 1L);

            assertEquals(stored, materialEnthalpy(fixture.arena), 1.0e-9D,
                    "stable surface/deep keys must preserve authoritative H on rebuild");
            assertTrue(
                    fixture.applier.migrationSnapshot().residualWithin(1.0e-8D),
                    fixture.applier.migrationSnapshot().toString());

            forEachLiveAir(fixture.arena, slot -> fixture.arena.setEnthalpyJ(slot, 0.0D));
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
            assertTrue(materialEnthalpy(fixture.arena) < stored,
                    "a warm surface must release heat back to colder air");
            assertTrue(airEnthalpy(fixture.arena) > 0.0D);

            fixture.applier.retirePage(fixture.page, 2L);
            fixture.applyTopology(15L, 2L);
            assertEquals(0, fixture.arena.liveCellCount(),
                    "Page retirement must release its material poles with the air span");
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
        }
    }

    @Test
    void naturalBoundaryExistsOnlyWhereDeepRockIsExposedToAir() {
        int[] exposedFloor = allAir();
        fillYPlane(exposedFloor, 0, NATURAL_ROCK);
        int[] buriedRock = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
        Arrays.fill(buriedRock, NATURAL_ROCK);

        try (Fixture exposed = Fixture.create(exposedFloor);
             Fixture buried = Fixture.create(buriedRock)) {
            exposed.applyTopology(5L, 1L);
            buried.applyTopology(5L, 1L);

            assertTrue(materialPoleCount(exposed.arena) > 0);
            assertTrue(deepPoleCount(exposed.arena) > 0);
            assertTrue(exposed.runtime.sweepBoundaryOperationCount() > 0,
                    "exposed rock must compile a deep natural boundary");
            assertEquals(0, materialPoleCount(buried.arena),
                    "unexposed mountain volume must not receive material nodes");
            assertEquals(0, buried.runtime.sweepBoundaryOperationCount(),
                    "geothermal heat must not enter a page without exposed deep rock");

            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    exposed.runtime.runOne().status());
            assertTrue(airEnthalpy(exposed.arena) > 0.0D,
                    "natural rock must warm air through surface/deep material exchange");
            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    buried.runtime.runOne().status());
        }
    }

    @Test
    void phaseReservoirAggregatesOneBrickAndMigratesReservedEnergy() {
        int[] snapshot = allAir();
        setBlock(snapshot, 1, 1, 1, PHASE);
        setBlock(snapshot, 2, 1, 1, PHASE);
        try (Fixture fixture = Fixture.create(snapshot)) {
            fixture.applyTopology(5L, 1L);
            assertEquals(1, phaseReservoirCount(fixture.arena));
            assertTrue(fixture.runtime.sweepPhaseOperationCount() > 0);
            forEachLiveAir(fixture.arena, slot -> fixture.arena.setEnthalpyJ(
                    slot, fixture.arena.capacityJPerK(slot) * 100.0D));

            assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                    fixture.runtime.runOne().status());
            int oldPhaseSlot = firstPhaseReservoir(fixture.arena);
            assertEquals(100.0D, fixture.arena.enthalpyJ(oldPhaseSlot), 1.0e-9D);
            assertEquals(50.0D,
                    fixture.arena.phaseReservedEnergyJ(oldPhaseSlot), 1.0e-9D);

            PhaseTransitionRuntime.MutableRequest request =
                    new PhaseTransitionRuntime.MutableRequest();
            assertTrue(fixture.applier.pollPhaseRequest(request));
            assertEquals(1, request.blockX());
            assertEquals(1, request.blockY());
            assertEquals(1, request.blockZ());
            fixture.applier.submitPhaseAck(
                    request, PhaseTransitionRuntime.AckOutcome.APPLIED);

            ThermalPage.MutationObservation mutation = fixture.page.recordGeometryMutation(
                    1, 1, 1, 10L, fixture.geometryDeltas);
            assertTrue(fixture.resolvedInputs.offerResolvedCenter(
                    fixture.page.sectionKey(),
                    fixture.page.lifecycleGeneration(),
                    mutation.geometryRevision(),
                    10L,
                    (1 << 8) | (1 << 4) | 1,
                    ThermalSignatureResolution.resolved(AIR)));
            fixture.applyTopology(10L, 1L);

            int migratedPhaseSlot = firstPhaseReservoir(fixture.arena);
            assertEquals(1, phaseReservoirCount(fixture.arena));
            assertEquals(50.0D,
                    fixture.arena.enthalpyJ(migratedPhaseSlot), 1.0e-8D);
            assertEquals(50.0D,
                    fixture.applier.committedPhaseEnergyJ(), 1.0e-9D);
            assertEquals(1L << ((1 << 4) | (1 << 2) | 2),
                    fixture.arena.phaseCandidateMask(migratedPhaseSlot));
            assertFalse(fixture.arena.phaseRequestOutstanding(migratedPhaseSlot));
        }
    }

    @Test
    void transitionMutationPolicySeparatesGameruleFromMachineEnergy() {
        assertFalse(MinecraftThermalInput.allowsAutomaticPhaseMutation(
                MaterialBoundaryRegistry.TransitionMutationPolicy
                        .RESPECT_RANDOM_TICK_SPEED,
                0));
        assertTrue(MinecraftThermalInput.allowsAutomaticPhaseMutation(
                MaterialBoundaryRegistry.TransitionMutationPolicy
                        .RESPECT_RANDOM_TICK_SPEED,
                3));
        assertTrue(MinecraftThermalInput.allowsAutomaticPhaseMutation(
                MaterialBoundaryRegistry.TransitionMutationPolicy
                        .IGNORE_RANDOM_TICK_SPEED,
                0));
        assertFalse(MinecraftThermalInput.allowsAutomaticPhaseMutation(
                MaterialBoundaryRegistry.TransitionMutationPolicy.SCRIPT_CONTROLLED,
                3));
    }

    @Test
    void phaseInterfaceValidationRequiresAlignedMicrocellContact() {
        long material = microcellBit(1, 0, 1);
        long sameBlockAir = microcellBit(1, 1, 1);
        assertTrue(MinecraftThermalInput.hasExposedPhaseContact(
                material, sameBlockAir, new long[6]),
                "partial phase material may contact air inside its own block");

        long boundaryMaterial = microcellBit(0, 1, 1);
        long[] neighborAir = new long[6];
        neighborAir[0] = microcellBit(3, 1, 2);
        assertFalse(MinecraftThermalInput.hasExposedPhaseContact(
                boundaryMaterial, 0L, neighborAir),
                "an unrelated aperture on the same face is not an interface");

        neighborAir[0] |= microcellBit(3, 1, 1);
        assertTrue(MinecraftThermalInput.hasExposedPhaseContact(
                boundaryMaterial, 0L, neighborAir));
    }

    private static long microcellBit(int x, int y, int z) {
        return 1L << ((y << 4) | (z << 2) | x);
    }

    private static ResolvedThermalSignature materialSignature(int profileId) {
        return new ResolvedThermalSignature(
                0, profileId, List.of(), FULL_CONTACT, 0, 0, 0, 0);
    }

    private static MaterialBoundaryRegistry materialRegistry() {
        return new MaterialBoundaryRegistry(
                List.of(
                        MaterialBoundaryRegistry.Profile.stateless(STATELESS, 8.0D),
                        MaterialBoundaryRegistry.Profile.capacitiveSurface(
                                CAPACITIVE, 8.0D, 20.0D, 0.0D),
                        MaterialBoundaryRegistry.Profile.naturalRock(
                                NATURAL_ROCK,
                                8.0D,
                                20.0D,
                                4.0D,
                                40.0D,
                                2.0D,
                                100.0D,
                                0.5D),
                        MaterialBoundaryRegistry.Profile.phaseReservoir(
                                PHASE,
                                100.0D,
                                0.0D,
                                50.0D,
                                MaterialBoundaryRegistry.TransitionMutationPolicy
                                        .IGNORE_RANDOM_TICK_SPEED,
                                MaterialBoundaryRegistry.TransitionAction
                                        .REMOVE_ONE_SNOW_LAYER)),
                List.of(MaterialBoundaryRegistry.ContactPattern.fullBlock(FULL_CONTACT)));
    }

    private static int[] allAir() {
        int[] snapshot = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
        Arrays.fill(snapshot, AIR);
        return snapshot;
    }

    private static void fillXPlane(int[] snapshot, int x, int signatureId) {
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                snapshot[(y << 8) | (z << 4) | x] = signatureId;
            }
        }
    }

    private static void fillYPlane(int[] snapshot, int y, int signatureId) {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                snapshot[(y << 8) | (z << 4) | x] = signatureId;
            }
        }
    }

    private static void setBlock(
            int[] snapshot,
            int x,
            int y,
            int z,
            int signatureId
    ) {
        snapshot[(y << 8) | (z << 4) | x] = signatureId;
    }

    private static void setSeparatedAirTemperatures(
            ThermalCellArena arena,
            double leftMaximumX,
            double rightMinimumX
    ) {
        forEachLiveAir(arena, slot -> {
            double temperature = arena.centerX(slot) < leftMaximumX ? 100.0D
                    : arena.centerX(slot) > rightMinimumX ? 0.0D : 0.0D;
            arena.setEnthalpyJ(slot, arena.capacityJPerK(slot) * temperature);
        });
    }

    private static double airEnthalpyRightOf(ThermalCellArena arena, double minimumX) {
        double total = 0.0D;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && !arena.isMaterialPole(slot)
                    && !arena.isPhaseReservoir(slot)
                    && arena.centerX(slot) > minimumX) {
                total += arena.enthalpyJ(slot);
            }
        }
        return total;
    }

    private static double airEnthalpy(ThermalCellArena arena) {
        double total = 0.0D;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && !arena.isMaterialPole(slot)) {
                if (arena.isPhaseReservoir(slot)) {
                    continue;
                }
                total += arena.enthalpyJ(slot);
            }
        }
        return total;
    }

    private static double materialEnthalpy(ThermalCellArena arena) {
        double total = 0.0D;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && arena.isMaterialPole(slot)) {
                total += arena.enthalpyJ(slot);
            }
        }
        return total;
    }

    private static int materialPoleCount(ThermalCellArena arena) {
        int count = 0;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && arena.isMaterialPole(slot)) {
                count++;
            }
        }
        return count;
    }

    private static int deepPoleCount(ThermalCellArena arena) {
        int count = 0;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && arena.isMaterialPole(slot)
                    && arena.materialPoleDepth(slot)
                    == ThermalCellArena.MaterialPoleDepth.DEEP) {
                count++;
            }
        }
        return count;
    }

    private static int phaseReservoirCount(ThermalCellArena arena) {
        int count = 0;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && arena.isPhaseReservoir(slot)) {
                count++;
            }
        }
        return count;
    }

    private static int firstPhaseReservoir(ThermalCellArena arena) {
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && arena.isPhaseReservoir(slot)) {
                return slot;
            }
        }
        throw new AssertionError("phase reservoir was not allocated");
    }

    private static void forEachLiveAir(ThermalCellArena arena, SlotConsumer consumer) {
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot) && !arena.isMaterialPole(slot)) {
                if (arena.isPhaseReservoir(slot)) {
                    continue;
                }
                consumer.accept(slot);
            }
        }
    }

    @FunctionalInterface
    private interface SlotConsumer {
        void accept(int slot);
    }

    private static final class Fixture implements AutoCloseable {
        private final ThermalCellArena arena;
        private final DimensionThermalRuntime runtime;
        private final GeometryDeltaRing geometryDeltas;
        private final ResolvedGeometryInputRing resolvedInputs;
        private final MinecraftThermalTopologyApplier applier;
        private final ThermalPage page;

        private Fixture(
                ThermalCellArena arena,
                DimensionThermalRuntime runtime,
                GeometryDeltaRing geometryDeltas,
                ResolvedGeometryInputRing resolvedInputs,
                MinecraftThermalTopologyApplier applier,
                ThermalPage page
        ) {
            this.arena = arena;
            this.runtime = runtime;
            this.geometryDeltas = geometryDeltas;
            this.resolvedInputs = resolvedInputs;
            this.applier = applier;
            this.page = page;
        }

        private static Fixture create(int[] snapshot) {
            ThermalCellArena arena = new ThermalCellArena(1);
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
            ThermalMemoryBudget budget = new ThermalMemoryBudget(20_000_000L, 0L);
            QueryPublication publication = QueryPublication.tryCreate(
                    budget.createDimensionBudget(20_000_000L, 0L),
                    20_000);
            if (publication == null) {
                throw new IllegalStateException("material test publication admission failed");
            }
            DimensionThermalRuntime runtime = new DimensionThermalRuntime(
                    200L,
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
                    new DimensionThermalRuntime.Limits(
                            20_000, 100_000, 20_000, 3, 1.0e-9D));
            ThermalSignatureRegistry.Builder signatures = ThermalSignatureRegistry.builder();
            assertEquals(AIR, signatures.intern(AIR_SIGNATURE));
            assertEquals(STATELESS, signatures.intern(materialSignature(STATELESS)));
            assertEquals(CAPACITIVE, signatures.intern(materialSignature(CAPACITIVE)));
            assertEquals(NATURAL_ROCK, signatures.intern(materialSignature(NATURAL_ROCK)));
            assertEquals(PHASE, signatures.intern(materialSignature(PHASE)));
            GeometryDeltaRing deltas = new GeometryDeltaRing(64);
            ResolvedGeometryInputRing resolved = new ResolvedGeometryInputRing(64);
            MinecraftThermalTopologyApplier applier = new MinecraftThermalTopologyApplier(
                    runtime,
                    signatures.build(),
                    deltas,
                    resolved,
                    new MinecraftThermalTopologyApplier.Parameters(
                            0,
                            0,
                            ConservativeAirGeometry.MICROCELL_COUNT,
                            1.0D,
                            0.0D,
                            0.0D,
                            1.0D,
                            0.25D,
                            false,
                            new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D)),
                    materialRegistry());
            ThermalPage page = applier.registerCapturedPage(
                    SectionPos.asLong(0, 0, 0), 1L, 1L, snapshot);
            return new Fixture(arena, runtime, deltas, resolved, applier, page);
        }

        private void applyTopology(long targetTick, long chunkWatermark) {
            page.sealGeometryDeltas(geometryDeltas);
            SealedInputFrame frame = new SealedInputFrame(
                    targetTick,
                    DIMENSION_GENERATION,
                    new InputWatermarks(
                            resolvedInputs.latestOfferedWatermark(),
                            runtime.latestOfferedSourceWatermark(),
                            chunkWatermark,
                            1L,
                            applier.latestPhaseAckWatermark()));
            LatestSolveEpochScheduler.SealResult sealed = runtime.sealFrame(frame);
            assertTrue(sealed == LatestSolveEpochScheduler.SealResult.ACCEPTED
                    || sealed == LatestSolveEpochScheduler.SealResult.DUPLICATE);
            MinecraftThermalTopologyApplier.ApplyReport report = applier.apply(frame);
            assertTrue(report.readyForSolve(), "material topology frame must install");
        }

        @Override
        public void close() {
            runtime.close();
        }
    }
}
