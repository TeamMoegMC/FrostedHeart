/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.radiation;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiationServiceTest {
    @Test
    void inverseSquareSamplingReusesRevisionWitnessesWithoutTouchingSources() {
        TestSources sources = new TestSources();
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L);
        RadiationService service = RadiationService.tryCreate(
                parameters(8, 8, 24), sources, null, tracer, budget);
        assertNotNull(service);
        assertTrue(sources.upsertSource(
                7L, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));

        RadiationService.MutableSample first = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 1.62D, 0.5D, first);
        double expected = 0.0D;
        for (double offset : new double[]{0.1D, 0.9D, 1.62D}) {
            double dy = offset - 1.0D;
            expected += 200.0D / (4.0D * Math.PI * (16.0D + dy * dy)) / 3.0D;
        }
        assertEquals(expected, first.radiantFluxWPerM2(), 1.0e-12D);

        RadiationService.MutableSample repeated = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 1.62D, 0.5D, repeated);
        assertEquals(expected, repeated.radiantFluxWPerM2(), 1.0e-12D);
        assertEquals(3, tracer.traces);

        assertTrue(sources.upsertSource(
                7L, 0.5D, 1.0D, 0.5D, 400.0D, 1.0D));
        RadiationService.MutableSample updated = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 1.62D, 0.5D, updated);
        assertEquals(expected * 2.0D, updated.radiantFluxWPerM2(), 1.0e-12D);
        assertEquals(6, tracer.traces);

        service.close();
    }

    @Test
    void sectionRevisionChangeRetracesAndAppliesNewOcclusion() {
        TestSources sources = new TestSources();
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters(8, 8, 24), sources, null, tracer, budget)) {
            assertNotNull(service);
            sources.upsertSource(
                    1L, 0.5D, 1.0D, 0.5D, 100.0D, 1.0D);
            RadiationService.MutableSample sample = new RadiationService.MutableSample();
            service.samplePlayer(2L, 1, 3.5D, 0.0D, 1.62D, 0.5D, sample);
            assertTrue(sample.radiantFluxWPerM2() > 0.0D);

            tracer.blocked = true;
            tracer.revision++;
            service.samplePlayer(2L, 1, 3.5D, 0.0D, 1.62D, 0.5D, sample);
            assertEquals(0.0D, sample.radiantFluxWPerM2());
            assertEquals(6, tracer.traces);
        }
    }

    @Test
    void candidateAndRayCapsBoundTheWork() {
        TestSources sources = new TestSources();
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters(1, 8, 1), sources, null, tracer, budget)) {
            assertNotNull(service);
            sources.upsertSource(
                    1L, 0.5D, 1.0D, 0.5D, 100.0D, 1.0D);
            sources.upsertSource(
                    2L, 1.5D, 1.0D, 0.5D, 100.0D, 1.0D);
            RadiationService.MutableSample sample = new RadiationService.MutableSample();
            service.samplePlayer(3L, 1, 4.5D, 0.0D, 1.62D, 0.5D, sample);

            assertEquals(1, tracer.traces);
            assertTrue(sample.radiantFluxWPerM2() >= 0.0D);
        }
    }

    @Test
    void staticNearbySourceRetracesOneEyeRayWithoutReceiverState() {
        TestSources sources = new TestSources();
        TestTracer tracer = new TestTracer();
        RadiationService.NearbySourceIndex nearby = new RadiationService.NearbySourceIndex() {
            @Override
            public void visitNearby(
                    double receiverX,
                    double receiverY,
                    double receiverZ,
                    int maximumVisits,
                    RadiationService.SourceVisitor visitor
            ) {
                visitor.visit(
                        91L,
                        RadiationService.STATIC_BLOCK_REVISION,
                        0.5D, 1.0D, 0.5D,
                        200.0D, 1.0D);
            }
        };
        try (RadiationService service = RadiationService.tryCreate(
                parameters(8, 8, 24), sources, nearby, tracer,
                new ThermalMemoryBudget(1_000_000L))) {
            assertNotNull(service);
            RadiationService.MutableSample sample =
                    new RadiationService.MutableSample();
            service.samplePlayer(33L, 1, 4.5D, 0.0D, 1.62D, 0.5D, sample);
            double expected = 200.0D
                    / (4.0D * Math.PI * (16.0D + 0.62D * 0.62D));
            assertEquals(expected, sample.radiantFluxWPerM2(), 1.0e-12D);
            assertEquals(1, tracer.traces);

            service.samplePlayer(33L, 1, 4.5D, 0.0D, 1.62D, 0.5D, sample);
            assertEquals(2, tracer.traces);
        }
    }

    private static RadiationService.Parameters parameters(
            int maximumCandidates,
            int maximumCandidateVisits,
            int maximumRays
    ) {
        return new RadiationService.Parameters(
                8, 32, maximumCandidateVisits,
                maximumCandidates, maximumRays, 8, 128,
                16.0D, 0.0D, 0.5D,
                0.1D, 0.9D, 1.62D);
    }

    private static final class TestSources
            implements RadiationService.SourceIndex {
        private final Map<Long, Source> sources = new HashMap<>();
        private long revision;

        private boolean upsertSource(
                long key,
                double x,
                double y,
                double z,
                double power,
                double directionalBound
        ) {
            if (!Double.isFinite(x) || !Double.isFinite(y)
                    || !Double.isFinite(z) || !Double.isFinite(power)
                    || power < 0.0D || !Double.isFinite(directionalBound)
                    || directionalBound <= 0.0D) {
                return false;
            }
            sources.put(key, new Source(
                    key, ++revision, x, y, z, power, directionalBound));
            return true;
        }

        private boolean removeSource(long key) {
            return sources.remove(key) != null;
        }

        @Override
        public void visitSection(
                int sectionX,
                int sectionY,
                int sectionZ,
                RadiationService.SourceVisitor visitor
        ) {
            for (Source source : sources.values()) {
                if (Math.floor(source.x / 16.0D) != sectionX
                        || Math.floor(source.y / 16.0D) != sectionY
                        || Math.floor(source.z / 16.0D) != sectionZ) {
                    continue;
                }
                if (!visitor.visit(
                        source.key,
                        source.revision,
                        source.x,
                        source.y,
                        source.z,
                        source.power,
                        source.directionalBound)) {
                    return;
                }
            }
        }

        private record Source(
                long key,
                long revision,
                double x,
                double y,
                double z,
                double power,
                double directionalBound
        ) {
        }
    }

    private static final class TestTracer implements RadiationService.OcclusionTracer {
        private static final long SECTION = RadiationService.packSection(0, 0, 0);
        private long revision = 1L;
        private int traces;
        private boolean blocked;

        @Override
        public void trace(
                double sourceX,
                double sourceY,
                double sourceZ,
                double targetX,
                double targetY,
                double targetZ,
                int maximumSteps,
                boolean collectWitnesses,
                RadiationService.MutableTrace result
        ) {
            traces++;
            if (collectWitnesses) {
                result.addSection(SECTION, revision);
            }
            result.finish(blocked
                    ? RadiationService.TraceStatus.BLOCKED
                    : RadiationService.TraceStatus.VISIBLE);
        }

        @Override
        public long currentSectionRevision(long packedSectionKey) {
            return packedSectionKey == SECTION
                    ? revision : RadiationService.NO_SECTION_REVISION;
        }
    }
}
