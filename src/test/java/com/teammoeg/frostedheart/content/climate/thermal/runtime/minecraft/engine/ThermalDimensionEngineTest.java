/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
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
    void admissionMarksEveryInfraredBrickInOnePublicationEpoch() {
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
            int epoch = cursor.infraredEpoch();
            assertTrue(epoch > 1);
            assertEquals(epoch,
                    cursor.pageChangeEpoch(page.workerPageSlot()));
            for (int brick = 0; brick < 64; brick++) {
                assertEquals(epoch, cursor.brickChangeEpoch(
                        page.workerPageSlot(), brick));
            }
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
            assertEquals(1, completion.residencyUpdates().length);
            assertEquals(fixture.page().sectionKey(),
                    completion.residencyUpdates()[0].sectionKey());
            assertEquals(fixture.page().lifecycleGeneration(),
                    completion.residencyUpdates()[0].lifecycleGeneration());
            assertEquals(0L,
                    completion.residencyUpdates()[0].desiredBrickMask());
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
    void sourceBrickAtReportedCoordinatesRequestsAllSixOpenNeighbors() {
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine();
        long section = SectionPos.asLong(0, 4, 3);
        ThermalPageHandle page = new ThermalPageHandle(section, 1L);
        long ownerBit = 1L << 2;
        PageSignatures signatures = ThermalTestFixtures.filledPageSignatures(
                fixture.airId());
        try {
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    page, signatures, ownerBit, ownerBit)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            int slot = page.currentPublication()
                    .brickAt(8, 0, 0).coverageSlot();
            fixture.arena().setEnthalpyJ(
                    slot, fixture.arena().capacityJPerK(slot));

            ThermalCompletion completion = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            2L, 40L,
                            ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));

            long sameSection = ownerBit
                    | 1L << 1
                    | 1L << 3
                    | 1L << 6
                    | 1L << 18;
            assertEquals(3, completion.residencyUpdates().length);
            assertEquals(sameSection, residencyMask(completion, section));
            assertEquals(1L << 14, residencyMask(
                    completion, SectionPos.asLong(0, 4, 2)));
            assertEquals(1L << 50, residencyMask(
                    completion, SectionPos.asLong(0, 3, 3)));
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void adjacentCampfiresShareOneMixedCellAndHeatEveryOpenNeighbor() {
        ThermalSignatureTable.Builder signatureBuilder =
                ThermalSignatureTable.builder();
        int airId = signatureBuilder.intern(
                ThermalTestFixtures.fullAirSignature());
        int solidId = signatureBuilder.intern(
                ThermalTestFixtures.solidSignature());
        ThermalSignatureTable signatures = signatureBuilder.build();
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine(
                        signatures,
                        new MaterialBoundaryRegistry(List.of(), List.of()),
                        airId,
                        solidId,
                        96.0D);
        long section = SectionPos.asLong(0, 4, 3);
        ThermalPageHandle page = new ThermalPageHandle(section, 1L);
        PageSignatures pageSignatures = adjacentCampfireSignatures(
                signatures, airId, solidId);
        long ownerBit = 1L << 2;
        MinecraftPhysicalSourceProfile profile =
                MinecraftPhysicalSourceProfile.CAMPFIRE;
        ThermalSourceBatch.Builder sourceEvents =
                new ThermalSourceBatch.Builder(0L);
        addCampfire(sourceEvents, 1L, 10, 66, 50, profile);
        addCampfire(sourceEvents, 2L, 10, 66, 49, profile);
        try {
            fixture.engine().process(new ThermalInputBatch(
                    1L, 1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    page, pageSignatures,
                                    ownerBit, ownerBit)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ThermalInputBatch.NO_RESIDENCY_UPDATES,
                    ResolvedGeometryBatch.EMPTY,
                    sourceEvents.buildAndReset(),
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS,
                    Double.NaN));

            ThermalCompletion heated = fixture.engine().process(
                    ThermalRuntimeTestFixtures.batch(
                            2L, 40L,
                            ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS,
                            ResolvedGeometryBatch.EMPTY));
            PagePublication publication = page.currentPublication();
            assertNotNull(publication);
            int sourceSlot = publication.brickAt(8, 0, 0).coverageSlot();
            assertEquals(1, fixture.arena().liveCellCount());
            assertEquals(12_800.0D,
                    fixture.arena().enthalpyJ(sourceSlot), 1.0e-9D);

            long sameSection = ownerBit
                    | 1L << 1
                    | 1L << 3
                    | 1L << 6
                    | 1L << 18;
            long negativeZSection = SectionPos.asLong(0, 4, 2);
            assertEquals(sameSection, residencyMask(heated, section));
            assertEquals(1L << 14,
                    residencyMask(heated, negativeZSection));
            assertFalse(hasResidencyUpdate(
                    heated, SectionPos.asLong(0, 3, 3)));

            ThermalPageHandle negativeZ = new ThermalPageHandle(
                    negativeZSection, 2L);
            fixture.engine().process(new ThermalInputBatch(
                    1L, 3L, 60L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    negativeZ,
                                    ThermalTestFixtures.filledPageSignatures(
                                            airId),
                                    1L << 14, 0L)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    new ThermalInputBatch.PageResidencyUpdate[]{
                            new ThermalInputBatch.PageResidencyUpdate(
                                    page,
                                    page.liveGeometryRevision(),
                                    sameSection,
                                    ownerBit,
                                    pageSignatures)},
                    ResolvedGeometryBatch.EMPTY,
                    ThermalSourceBatch.EMPTY,
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS,
                    Double.NaN));

            double negativeX = temperatureC(page, fixture, 4, 0, 0);
            double positiveX = temperatureC(page, fixture, 12, 0, 0);
            double positiveZ = temperatureC(page, fixture, 8, 0, 4);
            double positiveY = temperatureC(page, fixture, 8, 4, 0);
            double negativeZTemperature = temperatureC(
                    negativeZ, fixture, 8, 0, 12);
            assertTrue(negativeX > 0.0D);
            assertTrue(positiveX > 0.0D);
            assertTrue(negativeZTemperature > 0.0D);
            assertTrue(positiveZ > 0.0D);
            assertTrue(positiveY > 0.0D);
            double lateralMinimum = Math.min(
                    Math.min(negativeX, positiveX),
                    Math.min(negativeZTemperature, positiveZ));
            double lateralMaximum = Math.max(
                    Math.max(negativeX, positiveX),
                    Math.max(negativeZTemperature, positiveZ));
            assertTrue(lateralMaximum <= lateralMinimum * 1.05D);
            assertTrue(positiveY > lateralMaximum);

            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    4L, 80L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            assertTrue(temperatureC(page, fixture, 4, 0, 0) > negativeX);
            assertTrue(temperatureC(page, fixture, 12, 0, 0) > positiveX);
            assertTrue(temperatureC(page, fixture, 8, 0, 4) > positiveZ);
            assertTrue(temperatureC(negativeZ, fixture, 8, 0, 12)
                    > negativeZTemperature);
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void halfAirMixedNeighborReceivesHeatAfterResidencyExpansion() {
        ThermalSignatureTable.Builder signatureBuilder =
                ThermalSignatureTable.builder();
        int airId = signatureBuilder.intern(
                ThermalTestFixtures.fullAirSignature());
        int solidId = signatureBuilder.intern(
                ThermalTestFixtures.solidSignature());
        ThermalSignatureTable signatures = signatureBuilder.build();
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine(
                        signatures,
                        new MaterialBoundaryRegistry(List.of(), List.of()),
                        airId,
                        solidId,
                        96.0D);
        long section = SectionPos.asLong(0, 4, 3);
        ThermalPageHandle page = new ThermalPageHandle(section, 1L);
        PageSignatures pageSignatures =
                adjacentCampfireAndHalfAirNeighborSignatures(
                        signatures, airId, solidId);
        long sourceBit = 1L << 2;
        long residentMask = sourceBit | 1L << 3;
        ThermalSourceBatch.Builder sourceEvents =
                new ThermalSourceBatch.Builder(0L);
        MinecraftPhysicalSourceProfile profile =
                MinecraftPhysicalSourceProfile.CAMPFIRE;
        addCampfire(sourceEvents, 1L, 9, 66, 49, profile);
        addCampfire(sourceEvents, 2L, 10, 66, 49, profile);
        addCampfire(sourceEvents, 3L, 9, 66, 50, profile);
        addCampfire(sourceEvents, 4L, 10, 66, 50, profile);
        try {
            assertTrue(fixture.publication().noteInfraredRequest(0L, 80));
            fixture.engine().process(new ThermalInputBatch(
                    1L, 1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    page, pageSignatures,
                                    sourceBit, sourceBit)},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ThermalInputBatch.NO_RESIDENCY_UPDATES,
                    ResolvedGeometryBatch.EMPTY,
                    sourceEvents.buildAndReset(),
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS,
                    Double.NaN));
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    2L, 40L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));
            QueryPublication.InfraredReadCursor beforeExpansion =
                    new QueryPublication.InfraredReadCursor();
            assertTrue(fixture.publication().beginInfraredRead(beforeExpansion));
            int previousNeighborEpoch = beforeExpansion.brickChangeEpoch(
                    page.currentPublication().workerPageSlot(), 3);
            fixture.engine().process(new ThermalInputBatch(
                    1L, 3L, 60L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    new ThermalInputBatch.PageResidencyUpdate[]{
                            new ThermalInputBatch.PageResidencyUpdate(
                                    page,
                                    page.liveGeometryRevision(),
                                    residentMask,
                                    sourceBit,
                                    pageSignatures)},
                    ResolvedGeometryBatch.EMPTY,
                    ThermalSourceBatch.EMPTY,
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS,
                    Double.NaN));

            QueryPublication.InfraredReadCursor afterExpansion =
                    new QueryPublication.InfraredReadCursor();
            assertTrue(fixture.publication().beginInfraredRead(afterExpansion));
            assertTrue(afterExpansion.brickChangeEpoch(
                    page.currentPublication().workerPageSlot(), 3)
                    > previousNeighborEpoch);
            double migratedNeighbor = temperatureC(page, fixture, 12, 0, 0);
            assertTrue(migratedNeighbor > 0.0D);

            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    4L, 80L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            assertTrue(temperatureC(page, fixture, 12, 0, 0)
                    > migratedNeighbor);
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

    private static PageSignatures adjacentCampfireSignatures(
            ThermalSignatureTable signatures,
            int airId,
            int solidId
    ) {
        int[] brick = new int[PageSignatures.ENTRIES_PER_BRICK];
        java.util.Arrays.fill(brick, airId);
        for (int y = 0; y <= 1; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    brick[x | z << 2 | y << 4] = solidId;
                }
            }
        }
        brick[2 | 1 << 2 | 2 << 4] = solidId;
        brick[2 | 2 << 2 | 2 << 4] = solidId;
        return ThermalTestFixtures.filledPageSignatures(airId).withBricks(
                signatures,
                new int[]{2},
                new int[][]{brick});
    }

    private static PageSignatures adjacentCampfireAndHalfAirNeighborSignatures(
            ThermalSignatureTable signatures,
            int airId,
            int solidId
    ) {
        PageSignatures base = adjacentCampfireSignatures(
                signatures, airId, solidId);
        int[] source = new int[PageSignatures.ENTRIES_PER_BRICK];
        int[] neighbor = new int[PageSignatures.ENTRIES_PER_BRICK];
        java.util.Arrays.fill(source, airId);
        java.util.Arrays.fill(neighbor, airId);
        for (int y = 0; y <= 1; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    source[x | z << 2 | y << 4] = solidId;
                    neighbor[x | z << 2 | y << 4] = solidId;
                }
            }
        }
        source[1 | 1 << 2 | 2 << 4] = solidId;
        source[2 | 1 << 2 | 2 << 4] = solidId;
        source[1 | 2 << 2 | 2 << 4] = solidId;
        source[2 | 2 << 2 | 2 << 4] = solidId;
        return base.withBricks(
                signatures,
                new int[]{2, 3},
                new int[][]{source, neighbor});
    }

    private static void addCampfire(
            ThermalSourceBatch.Builder events,
            long sourceId,
            int x,
            int y,
            int z,
            MinecraftPhysicalSourceProfile profile
    ) {
        events.addRegister(
                sourceId,
                1,
                ThermalSourceMode.POWER_SOURCE,
                profile.powerForLevel(1.0D),
                true,
                0L,
                x, y, z,
                profile.profileId(),
                WorkerPhysicalSourceBindings.initialPorts(
                        sourceId, profile));
    }

    private static double temperatureC(
            ThermalPageHandle page,
            ThermalRuntimeTestFixtures.EngineFixture fixture,
            int x,
            int y,
            int z
    ) {
        int slot = page.currentPublication().brickAt(x, y, z).coverageSlot();
        return fixture.arena().temperatureC(slot, 0.0D);
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
