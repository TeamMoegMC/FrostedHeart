/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class ThermalRuntimeTestFixtures {
    private ThermalRuntimeTestFixtures() {
    }

    static EngineFixture engine() {
        ThermalSignatureTable.Builder builder =
                ThermalSignatureTable.builder();
        int airId = builder.intern(ThermalTestFixtures.fullAirSignature());
        int solidId = builder.intern(ThermalTestFixtures.solidSignature());
        return engine(
                builder.build(),
                new MaterialBoundaryRegistry(
                        java.util.List.of(), java.util.List.of()),
                airId,
                solidId,
                new FarFieldSettings(1.0D, 1.0D, 16.0D));
    }

    static EngineFixture engine(
            ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials,
            int airId,
            int solidId
    ) {
        return engine(
                signatures,
                materials,
                airId,
                solidId,
                new FarFieldSettings(1.0D, 1.0D, 16.0D));
    }

    private static EngineFixture engine(
            ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials,
            int airId,
            int solidId,
            FarFieldSettings farField
    ) {
        ThermalCellArena arena = new ThermalCellArena(256);
        QueryPublication publication = QueryPublication.tryCreate(
                new ThermalMemoryBudget(8L * 1024L * 1024L)
                        .createDimensionBudget(8L * 1024L * 1024L),
                256,
                16);
        assertNotNull(publication);
        ThermalDimensionEngine engine = new ThermalDimensionEngine(
                1L,
                0L,
                arena,
                signatures,
                materials,
                new ThermalTopologyParameters(
                        64, 1_200.0D,
                        0.0D, 1.0D, 0.25D,
                        new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                        8, 4),
                farField,
                MinecraftPhysicalSourceProfile.CAMPFIRE,
                new ThermalDimensionLimits(
                        16, 128, 256,
                        4_096, 2_048,
                        4_096, 4_096, 4_096,
                        2, 1.0e-6D),
                publication);
        ThermalPageHandle page = new ThermalPageHandle(0L, 1L);
        return new EngineFixture(
                engine, arena, publication, page, signatures, airId, solidId);
    }

    public static ThermalInputBatch batch(
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
                ThermalInputBatch.NO_RESIDENCY_UPDATES,
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
        return admission(page, signatures, -1L, 0L);
    }

    static ThermalInputBatch.PageAdmission admission(
            ThermalPageHandle page,
            PageSignatures signatures,
            long residentBrickMask,
            long sourceSeedMask
    ) {
        byte[] sky = new byte[256];
        Arrays.fill(sky, (byte) 16);
        return admission(
                page, signatures, residentBrickMask, sourceSeedMask, sky);
    }

    static ThermalInputBatch.PageAdmission admission(
            ThermalPageHandle page,
            PageSignatures signatures,
            long residentBrickMask,
            long sourceSeedMask,
            byte[] sky
    ) {
        return new ThermalInputBatch.PageAdmission(
                page, page.liveGeometryRevision(),
                residentBrickMask, sourceSeedMask,
                signatures, 0.0D, sky.clone(), null);
    }

    static ResolvedGeometryBatch geometryCenter(
            ThermalPageHandle page,
            long revision,
            int block,
            int signatureId
    ) {
        ResolvedGeometryBatch.Builder builder =
                new ResolvedGeometryBatch.Builder();
        builder.addResolvedCenter(
                page, revision, block, signatureId);
        return builder.buildAndReset();
    }

    record EngineFixture(
            ThermalDimensionEngine engine,
            ThermalCellArena arena,
            QueryPublication publication,
            ThermalPageHandle page,
            ThermalSignatureTable signatures,
            int airId,
            int solidId
    ) {
    }
}
