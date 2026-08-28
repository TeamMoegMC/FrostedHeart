/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ThermalRuntimeTestFixtures {
    private ThermalRuntimeTestFixtures() {
    }

    static EngineFixture engine() {
        ThermalSignatureRegistry.Builder builder =
                ThermalSignatureRegistry.builder();
        int airId = builder.intern(ThermalTestFixtures.fullAirSignature());
        int solidId = builder.intern(ThermalTestFixtures.solidSignature());
        return engine(
                builder.build(),
                new MaterialBoundaryRegistry(
                        java.util.List.of(), java.util.List.of()),
                airId,
                solidId);
    }

    static EngineFixture engine(
            ThermalSignatureRegistry signatures,
            MaterialBoundaryRegistry materials,
            int airId,
            int solidId
    ) {
        ThermalCellArena arena = new ThermalCellArena(256);
        QueryPublication publication = QueryPublication.tryCreate(
                new ThermalMemoryBudget(8L * 1024L * 1024L, 0L)
                        .createDimensionBudget(8L * 1024L * 1024L, 0L),
                256);
        assertNotNull(publication);
        ThermalDimensionEngine engine = new ThermalDimensionEngine(
                1L,
                0L,
                arena,
                signatures,
                materials,
                new ThermalTopologyParameters(
                        0, 64, 1_200.0D,
                        0.0D, 0.0D, 1.0D, 0.25D, false,
                        new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                        8, 4),
                new FarFieldSettings(false, 0.0D, 1.0D, 16.0D),
                new ThermalDimensionLimits(
                        16, 4_096, 2_048,
                        4_096, 4_096, 4_096,
                        2, 1.0e-6D),
                publication);
        ThermalPageHandle page = new ThermalPageHandle(0L, 1L);
        return new EngineFixture(
                engine, arena, publication, page, signatures, airId, solidId);
    }

    static ThermalInputBatch batch(
            long sequence,
            long targetTick,
            ThermalInputBatch.PageAdmission[] admissions,
            ThermalInputBatch.PageRetirement[] retirements,
            ResolvedGeometryBatch geometry
    ) {
        return new ThermalInputBatch(
                1L,
                sequence,
                targetTick,
                admissions,
                retirements,
                geometry,
                ThermalSourceBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS,
                Double.NaN);
    }

    static ThermalInputBatch.PageAdmission admission(
            ThermalPageHandle page,
            PageSignatures signatures
    ) {
        byte[] sky = new byte[256];
        Arrays.fill(sky, (byte) 16);
        return new ThermalInputBatch.PageAdmission(
                page, page.liveGeometryRevision(), signatures, 0.0D, sky);
    }

    static ResolvedGeometryBatch geometryCenter(
            ThermalPageHandle page,
            long revision,
            long tick,
            int block,
            int signatureId
    ) {
        ResolvedGeometryBatch.Builder builder =
                new ResolvedGeometryBatch.Builder();
        builder.addResolvedCenter(
                page, revision, tick, block, signatureId);
        return builder.buildAndReset();
    }

    record EngineFixture(
            ThermalDimensionEngine engine,
            ThermalCellArena arena,
            QueryPublication publication,
            ThermalPageHandle page,
            ThermalSignatureRegistry signatures,
            int airId,
            int solidId
    ) {
    }
}
