/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;

import net.minecraft.core.SectionPos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalDimensionEngineTest {
    @Test
    void topologyPublicationMarksTheInfraredPageChangeId() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            assertTrue(fixture.publication().noteInfraredRequest(0L, 80));
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()))},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            PagePublication page = fixture.page().currentPublication();
            assertNotNull(page);
            QueryPublication.InfraredReadCursor cursor =
                    new QueryPublication.InfraredReadCursor();
            assertTrue(fixture.publication().beginInfraredRead(cursor));
            assertTrue(cursor.pageChangeId(page.workerPageSlot()) > 1L);
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void admissionPublishesAFlatPageWithoutMissingNeighborFarField() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            ThermalCompletion completion = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            1L,
                            20L,
                            new ThermalInputBatch.PageAdmission[]{
                                    ThermalRuntimeTestFixtures.admission(
                                            fixture.page(),
                                            ThermalTestFixtures.filledPageSignatures(
                                                    fixture.airId()))},
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));

            assertEquals(ThermalCompletion.Status.COMPLETED,
                    completion.status());
            PagePublication publication = fixture.page().currentPublication();
            assertNotNull(publication);
            assertEquals(64, fixture.arena().liveCellCount());
            assertEquals(1L, publication.topologyGeneration());
            assertEquals(0, publication.brickAt(0, 0, 0).coverageSlot());
            assertEquals(0, completion.residencyUpdates().length);
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void sparseAdmissionCompilesOnlyTheRequestedBrick() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()),
                                    1L, 0L)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            PagePublication publication = fixture.page().currentPublication();
            assertNotNull(publication);
            assertEquals(1, fixture.arena().liveCellCount());
            assertTrue(publication.brickAt(0, 0, 0).coverageSlot() >= 0);
            assertEquals(-1, publication.brickAt(4, 0, 0).coverageSlot());
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void hotBoundaryPublishesChangedAbsoluteNeighborResidency() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            long ownerBit = 1L << 3;
            ThermalCompletion admitted = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            1L, 20L,
                            new ThermalInputBatch.PageAdmission[]{
                                    ThermalRuntimeTestFixtures.admission(
                                            fixture.page(),
                                            ThermalTestFixtures.filledPageSignatures(
                                                    fixture.airId()),
                                            ownerBit, ownerBit)},
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));
            assertEquals(ownerBit,
                    residencyMask(admitted, fixture.page().sectionKey()));

            int slot = fixture.page().currentPublication()
                    .brickAt(12, 0, 0).coverageSlot();
            fixture.arena().setEnthalpyJ(
                    slot, fixture.arena().capacityJPerK(slot));
            ThermalCompletion expanded = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            2L, 40L,
                            ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));
            long neighborSection = SectionPos.asLong(1, 0, 0);
            assertEquals(1L, residencyMask(expanded, neighborSection));

            fixture.arena().setEnthalpyJ(slot, 0.0D);
            ThermalCompletion cooled = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            3L, 60L,
                            ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));
            assertEquals(0L, residencyMask(cooled, neighborSection));
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void admittedNeighborReceivesHeatAcrossTheSectionBoundary() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        ThermalPageHandle neighbor = new ThermalPageHandle(
                SectionPos.asLong(1, 0, 0), 2L);
        try {
            long ownerBit = 1L << 3;
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()),
                                    ownerBit, ownerBit)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            int ownerSlot = fixture.page().currentPublication()
                    .brickAt(12, 0, 0).coverageSlot();
            fixture.arena().setEnthalpyJ(
                    ownerSlot, fixture.arena().capacityJPerK(ownerSlot));
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    2L, 40L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    3L, 60L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    neighbor,
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()),
                                    1L, 0L)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            int neighborSlot = neighbor.currentPublication()
                    .brickAt(0, 0, 0).coverageSlot();
            assertEquals(2, fixture.arena().liveCellCount());
            assertTrue(fixture.arena().temperatureC(neighborSlot, 0.0D) > 0.0D);
            assertTrue(fixture.arena().temperatureC(ownerSlot, 0.0D) < 1.0D);
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void frontierThresholdsRefineAndReleaseWithoutChatter() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        long neighborSection = SectionPos.asLong(1, 0, 0);
        try {
            long ownerBit = 1L << 3;
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()),
                                    ownerBit, ownerBit)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            int slot = fixture.page().currentPublication()
                    .brickAt(12, 0, 0).coverageSlot();
            double capacity = fixture.arena().capacityJPerK(slot);

            fixture.arena().setEnthalpyJ(slot, capacity * 0.124D);
            ThermalCompletion belowRefine = fixture.engine().process(
                    activeBatch(2L, 40L, fixture.page()));
            assertFalse(hasResidencyUpdate(belowRefine, neighborSection));

            fixture.arena().setEnthalpyJ(slot, capacity * 0.125D);
            ThermalCompletion refined = fixture.engine().process(
                    activeBatch(3L, 60L, fixture.page()));
            assertEquals(1L, residencyMask(refined, neighborSection));

            fixture.arena().setEnthalpyJ(slot, capacity * 0.063D);
            ThermalCompletion retained = fixture.engine().process(
                    activeBatch(4L, 80L, fixture.page()));
            assertFalse(hasResidencyUpdate(retained, neighborSection));

            fixture.arena().setEnthalpyJ(slot, capacity * 0.062D);
            ThermalCompletion released = fixture.engine().process(
                    activeBatch(5L, 100L, fixture.page()));
            assertEquals(0L, residencyMask(released, neighborSection));
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void directSkyFarFieldDoesNotRequestTheSectionAbove() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        long aboveSection = SectionPos.asLong(0, 1, 0);
        try {
            long topBrick = 1L << 48;
            byte[] directSky = new byte[256];
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()),
                                    topBrick, topBrick, directSky)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            int slot = fixture.page().currentPublication()
                    .brickAt(0, 12, 0).coverageSlot();
            fixture.arena().setEnthalpyJ(
                    slot, fixture.arena().capacityJPerK(slot));

            ThermalCompletion completion = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            2L, 40L,
                            ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));

            assertFalse(hasResidencyUpdate(completion, aboveSection));
        } finally {
            fixture.engine().close();
        }
    }

    private static boolean hasResidencyUpdate(
            ThermalCompletion completion,
            long sectionKey
    ) {
        for (ThermalCompletion.BrickResidency update
                : completion.residencyUpdates()) {
            if (update.sectionKey() == sectionKey) {
                return true;
            }
        }
        return false;
    }

    private static ThermalInputBatch activeBatch(
            long sequence,
            long targetTick,
            ThermalPageHandle page
    ) {
        return new ThermalInputBatch(
                1L,
                sequence,
                targetTick,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES,
                ResolvedGeometryBatch.EMPTY,
                ThermalSourceBatch.EMPTY,
                new ThermalInputBatch.PageEnvironmentUpdate[]{
                        new ThermalInputBatch.PageEnvironmentUpdate(
                                page, true, 0.0D,
                                new short[0], new byte[0])},
                ThermalInputBatch.NO_PHASE_ACKS,
                Double.NaN);
    }

    private static long residencyMask(
            ThermalCompletion completion,
            long sectionKey
    ) {
        for (ThermalCompletion.BrickResidency update
                : completion.residencyUpdates()) {
            if (update.sectionKey() == sectionKey) {
                return update.desiredBrickMask();
            }
        }
        throw new AssertionError("missing residency update for " + sectionKey);
    }

    @Test
    void aLocalSignatureChangeRebuildsOneBrickAndInvalidatesOnlyItsAirPoint() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()))},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            long revision = fixture.page().beginGeometryMutation();

            ThermalCompletion completion = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            2L,
                            40L,
                            ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS,
                            ThermalRuntimeTestFixtures.geometryCenter(
                                    fixture.page(), revision, 0,
                                    fixture.solidId())));

            assertEquals(ThermalCompletion.Status.COMPLETED,
                    completion.status());
            PagePublication publication = fixture.page().currentPublication();
            assertNotNull(publication);
            assertEquals(revision, publication.geometryRevision());
            assertEquals(PagePublication.NO_AIR_POINT,
                    publication.resolveAirPoint(
                            0, 0, 0, 0, fixture.signatures()));
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void retirementRemovesThePagePublicationAndReleasesItsCells() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()))},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    2L, 40L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    new ThermalInputBatch.PageRetirement[]{
                            new ThermalInputBatch.PageRetirement(
                                    fixture.page())},
                    ResolvedGeometryBatch.EMPTY));

            assertNull(fixture.page().currentPublication());
            assertEquals(0, fixture.arena().liveCellCount());
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void sameSectionLifecycleReplacementIsAtomicWithAnActiveNeighbor() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        ThermalPageHandle neighbor = new ThermalPageHandle(
                SectionPos.asLong(1, 0, 0), 1L);
        ThermalPageHandle replacement = new ThermalPageHandle(
                fixture.page().sectionKey(), 2L);
        try {
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId())),
                            ThermalRuntimeTestFixtures.admission(
                                    neighbor,
                                    ThermalTestFixtures.filledPageSignatures(
                                            fixture.airId()))},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            int workerPageSlot = fixture.page().currentPublication()
                    .workerPageSlot();

            ThermalCompletion completion = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            2L, 40L,
                            new ThermalInputBatch.PageAdmission[]{
                                    ThermalRuntimeTestFixtures.admission(
                                            replacement,
                                            ThermalTestFixtures
                                                    .filledPageSignatures(
                                                            fixture.airId()))},
                            new ThermalInputBatch.PageRetirement[]{
                                    new ThermalInputBatch.PageRetirement(
                                            fixture.page())},
                            ResolvedGeometryBatch.EMPTY));

            assertEquals(ThermalCompletion.Status.COMPLETED,
                    completion.status());
            assertNull(fixture.page().currentPublication());
            assertNotNull(replacement.currentPublication());
            assertEquals(workerPageSlot,
                    replacement.currentPublication().workerPageSlot());
            assertNotNull(neighbor.currentPublication());
            assertEquals(128, fixture.arena().liveCellCount());
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void engineRejectsUnalignedOrOutOfOrderBatchesBeforeMutation() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> fixture.engine().process(
                            ThermalRuntimeTestFixtures.batch(
                                    1L, 10L,
                                    ThermalInputBatch.NO_ADMISSIONS,
                                    ThermalInputBatch.NO_RETIREMENTS,
                                    ResolvedGeometryBatch.EMPTY)));
            assertEquals(0, fixture.arena().liveCellCount());
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void quietBatchesReachTheWorkerSleepGateWithoutASecondRuntime() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        try {
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 0L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    2L, 20L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            ThermalCompletion sleeping = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            3L, 40L,
                            ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));

            assertEquals(ThermalCompletion.Status.COMPLETED,
                    sleeping.status());
            assertTrue(!fixture.publication().tryRead(
                    0, 1, 0L,
                    new com.teammoeg.frostedheart.content.climate.thermal.query
                            .QueryPublication.MutableSample()));
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void topologyMigrationPreservesSourceEnergySettledInTheSameCut() {
        ResolvedThermalSignature air =
                ThermalTestFixtures.fullAirSignature();
        ResolvedThermalSignature replacementAir =
                new ResolvedThermalSignature(
                        new ConservativeAirGeometry.Resolution(
                                ConservativeAirGeometry.Status.RESOLVED,
                                List.of(new ConservativeAirGeometry.AirComponent(
                                        0, -1L,
                                        0xffff, 0xfffe, 0xffff,
                                        0xffff, 0xffff, 0xffff))),
                        air.materialProfileId(),
                        air.materialContactPatternId());
        ThermalSignatureTable.Builder signatures =
                ThermalSignatureTable.builder();
        int airId = signatures.intern(air);
        int replacementAirId = signatures.intern(replacementAir);
        int solidId = signatures.intern(
                ThermalTestFixtures.solidSignature());
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine(
                        signatures.build(),
                        new MaterialBoundaryRegistry(List.of(), List.of()),
                        airId,
                        solidId);
        try {
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(),
                                    ThermalTestFixtures.filledPageSignatures(
                                            airId))},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            long sourceId = 42L;
            MinecraftPhysicalSourceProfile profile =
                    MinecraftPhysicalSourceProfile.GENERATOR;
            ThermalSourceBatch.Builder registration =
                    new ThermalSourceBatch.Builder(20L);
            registration.addRegister(
                    sourceId,
                    1,
                    ThermalSourceMode.POWER_SOURCE,
                    0.0D,
                    true,
                    20L,
                    0,
                    0,
                    0,
                    profile.profileId(),
                    WorkerPhysicalSourceBindings.initialPorts(
                            sourceId, profile));
            fixture.engine().process(batchWithSources(
                    2L, 20L, ResolvedGeometryBatch.EMPTY,
                    registration.buildAndReset()));

            long revision = fixture.page().requireFullGeometryResync(
                    ThermalPageHandle.GeometryResyncReason
                            .EXPLICIT_INVALIDATION);
            ResolvedGeometryBatch.Builder geometry =
                    new ResolvedGeometryBatch.Builder();
            geometry.addFullResync(
                    fixture.page(),
                    revision,
                    ThermalPageHandle.GeometryResyncReason
                            .EXPLICIT_INVALIDATION,
                    ThermalTestFixtures.filledPageSignatures(
                            replacementAirId));
            ThermalSourceBatch.Builder power =
                    new ThermalSourceBatch.Builder(20L);
            power.addPowerChange(sourceId, 100.0D, 20L);
            fixture.engine().process(batchWithSources(
                    3L, 40L, geometry.buildAndReset(),
                    power.buildAndReset()));

            double totalEnthalpyJ = 0.0D;
            for (int slot = fixture.arena().nextLiveSlot(0);
                 slot >= 0;
                 slot = fixture.arena().nextLiveSlot(slot + 1)) {
                totalEnthalpyJ += fixture.arena().enthalpyJ(slot);
            }
            assertTrue(totalEnthalpyJ > 79.0D);
            assertTrue(totalEnthalpyJ < 80.0D);
        } finally {
            fixture.engine().close();
        }
    }

    private static ThermalInputBatch batchWithSources(
            long sequence,
            long targetTick,
            ResolvedGeometryBatch geometry,
            ThermalSourceBatch sources
    ) {
        return new ThermalInputBatch(
                1L,
                sequence,
                targetTick,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES,
                geometry,
                sources,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS,
                Double.NaN);
    }

}
