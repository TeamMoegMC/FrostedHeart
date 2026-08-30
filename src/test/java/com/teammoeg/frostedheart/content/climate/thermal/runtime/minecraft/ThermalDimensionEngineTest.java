/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalDimensionEngineTest {
    @Test
    void admissionCompilesFarFieldAndPublishesAFlatPageCut() {
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
            assertEquals(1, completion.continuations().length);
            assertEquals(0x3f,
                    Byte.toUnsignedInt(completion.continuations()[0].faceMask()));
        } finally {
            fixture.engine().close();
        }
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
        ThermalSignatureRegistry.Builder signatures =
                ThermalSignatureRegistry.builder();
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
            assertTrue(totalEnthalpyJ > 69.0D);
            assertTrue(totalEnthalpyJ < 70.0D);
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
                geometry,
                sources,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS,
                Double.NaN);
    }

}
