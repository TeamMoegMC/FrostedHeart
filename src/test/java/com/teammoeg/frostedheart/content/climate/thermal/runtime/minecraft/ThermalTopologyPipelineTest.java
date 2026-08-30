/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalTopologyPipelineTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void materialSurfaceIsCompiledIntoOneLocalExecutionOwner() {
        ResolvedThermalSignature air =
                ThermalTestFixtures.fullAirSignature();
        ResolvedThermalSignature material = new ResolvedThermalSignature(
                new ConservativeAirGeometry.Resolution(
                        ConservativeAirGeometry.Status.RESOLVED, List.of()),
                1, 1);
        ThermalSignatureRegistry.Builder signatures =
                ThermalSignatureRegistry.builder();
        int airId = signatures.intern(air);
        int materialId = signatures.intern(material);
        ThermalRuntimeTestFixtures.EngineFixture fixture =
                ThermalRuntimeTestFixtures.engine(
                        signatures.build(),
                        new MaterialBoundaryRegistry(
                                List.of(MaterialBoundaryRegistry.Profile
                                        .capacitiveSurfaceAtNaturalTemperature(
                                                1, 5.0D, 100.0D)),
                                List.of(new MaterialBoundaryRegistry.ContactPattern(
                                        1, -1L))),
                        airId,
                        materialId);
        try {
            PageSignatures.Builder pageBuilder = new PageSignatures.Builder();
            for (int block = 0;
                 block < PageSignatures.ENTRY_COUNT;
                 block++) {
                pageBuilder.set(block, airId);
            }
            pageBuilder.set(0, materialId);
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    1L, 20L,
                    new ThermalInputBatch.PageAdmission[]{
                            ThermalRuntimeTestFixtures.admission(
                                    fixture.page(), pageBuilder.build())},
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            int materialSlot = -1;
            for (int slot = fixture.arena().nextLiveSlot(0);
                 slot >= 0;
                 slot = fixture.arena().nextLiveSlot(slot + 1)) {
                if (fixture.arena().isMaterialPole(slot)) {
                    materialSlot = slot;
                    break;
                }
            }
            assertNotEquals(-1, materialSlot);
            fixture.arena().setEnthalpyJ(materialSlot, 1_000.0D);
            fixture.engine().process(ThermalRuntimeTestFixtures.batch(
                    2L, 40L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS,
                    ResolvedGeometryBatch.EMPTY));

            assertTrue(fixture.arena().enthalpyJ(materialSlot) < 1_000.0D);
            double total = 0.0D;
            for (int slot = fixture.arena().nextLiveSlot(0);
                 slot >= 0;
                 slot = fixture.arena().nextLiveSlot(slot + 1)) {
                total += fixture.arena().enthalpyJ(slot);
            }
            assertEquals(1_000.0D, total, EPSILON);
        } finally {
            fixture.engine().close();
        }
    }

    @Test
    void pageSignatureCutsKeepUnchangedBrickPayloadsShared() {
        PageSignatures original =
                ThermalTestFixtures.filledPageSignatures(0);
        int[] changed = new int[PageSignatures.ENTRIES_PER_BRICK];
        Arrays.fill(changed, 1);
        PageSignatures next = original.withBricks(
                new int[]{0}, new int[][]{changed});

        assertNotEquals(original.brickPayload(0), next.brickPayload(0));
        assertEquals(original.brickPayload(1), next.brickPayload(1));
    }
}
