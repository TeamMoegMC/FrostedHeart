/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.benchmark;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;

import java.util.List;

/** Synthetic hot-query fixtures using the production Page/runtime/publication owners. */
final class ThermalShadowQueryFixtures {
    static final long DIMENSION_GENERATION = 9L;
    static final long GEOMETRY_REVISION = 0L;
    static final long TOPOLOGY_GENERATION = 1L;
    static final long SOLVE_EPOCH = 1L;
    static final long SAMPLE_TICK = 100L;
    static final long CURRENT_TICK = 105L;
    static final int MAXIMUM_PUBLICATION_AGE_TICKS = 40;
    private static final int SECTION_Y = 4;

    private ThermalShadowQueryFixtures() {
    }

    static Fixture create(int receiverCount, String layoutName) {
        if (receiverCount <= 0) {
            throw new IllegalArgumentException("receiverCount must be positive");
        }
        Layout layout = Layout.parse(layoutName);
        int pageCount = layout == Layout.SHARED_PAGE ? 1 : receiverCount;
        ThermalCellArena arena = new ThermalCellArena(pageCount);
        Long2ObjectOpenHashMap<ThermalPage> pages = new Long2ObjectOpenHashMap<>();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int sectionX = layout == Layout.SHARED_PAGE ? 0 : pageIndex;
            ArenaSpan span = arena.allocatePageCells(
                    pageIndex,
                    1,
                    new ThermalCellArena.CellSpec[]{
                            ThermalCellArena.CellSpec.regularAir(
                                    SectionPos.sectionToBlockCoord(sectionX),
                                    SectionPos.sectionToBlockCoord(SECTION_Y),
                                    0,
                                    16,
                                    0,
                                    0,
                                    1.0D)
                    },
                    0.0D,
                    0.0D);
            long sectionKey = SectionPos.asLong(sectionX, SECTION_Y, 0);
            ThermalPage page = ThermalPage.allAir(
                    sectionKey, 1L, span.firstSlot(), 0);
            if (!page.tryPublishGeometry(
                    GEOMETRY_REVISION, TOPOLOGY_GENERATION, SOLVE_EPOCH)) {
                throw new IllegalStateException("synthetic Page publication failed");
            }
            pages.put(sectionKey, page);
        }

        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                DIMENSION_GENERATION,
                SAMPLE_TICK,
                16,
                new ThermalSourceRegistry(0, 3, 16, accumulators),
                arena);
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
        ThermalMemoryBudget serverBudget = new ThermalMemoryBudget(64L * 1024L * 1024L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                serverBudget.createDimensionBudget(64L * 1024L * 1024L, 0L),
                Math.max(1, arena.highWaterMark()));
        if (publication == null || !publication.publish(
                arena,
                0.0D,
                DIMENSION_GENERATION,
                GEOMETRY_REVISION,
                TOPOLOGY_GENERATION,
                SOLVE_EPOCH,
                SAMPLE_TICK,
                0,
                arena.highWaterMark())) {
            throw new IllegalStateException("synthetic query publication failed");
        }
        DimensionThermalRuntime runtime = new DimensionThermalRuntime(
                100L,
                DIMENSION_GENERATION,
                SAMPLE_TICK,
                InputWatermarks.ZERO,
                GEOMETRY_REVISION,
                TOPOLOGY_GENERATION,
                true,
                new ThermalTimePolicy(5L, 20L, 2),
                arena,
                sources,
                sweep,
                publication,
                0.0D,
                new DimensionThermalRuntime.Limits(
                        pageCount, 0, 0, 3, 1.0e-9D));

        double[] receiverX = new double[receiverCount];
        double[] receiverY = new double[receiverCount];
        double[] receiverZ = new double[receiverCount];
        for (int receiver = 0; receiver < receiverCount; receiver++) {
            receiverX[receiver] = layout == Layout.SHARED_PAGE
                    ? (receiver & 15) + 0.5D
                    : SectionPos.sectionToBlockCoord(receiver) + 0.5D;
            receiverY[receiver] = SectionPos.sectionToBlockCoord(SECTION_Y) + 1.5D;
            receiverZ[receiver] = layout == Layout.SHARED_PAGE
                    ? ((receiver >>> 4) & 15) + 0.5D
                    : 0.5D;
        }
        return new Fixture(
                layout, receiverCount, pages, runtime,
                receiverX, receiverY, receiverZ);
    }

    enum Layout {
        SHARED_PAGE("shared_page"),
        DISTRIBUTED_PAGES("distributed_pages");

        private final String id;

        Layout(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        static Layout parse(String id) {
            for (Layout value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("unknown query layout: " + id);
        }
    }

    static final class Fixture implements AutoCloseable {
        private final Layout layout;
        private final int receiverCount;
        private final Long2ObjectOpenHashMap<ThermalPage> pages;
        private final DimensionThermalRuntime runtime;
        private final double[] receiverX;
        private final double[] receiverY;
        private final double[] receiverZ;
        private final ThermalPage.MutableCoverageQuery coverage =
                new ThermalPage.MutableCoverageQuery();
        private final QueryPublication.MutableSample publication =
                new QueryPublication.MutableSample();

        private Fixture(
                Layout layout,
                int receiverCount,
                Long2ObjectOpenHashMap<ThermalPage> pages,
                DimensionThermalRuntime runtime,
                double[] receiverX,
                double[] receiverY,
                double[] receiverZ
        ) {
            this.layout = layout;
            this.receiverCount = receiverCount;
            this.pages = pages;
            this.runtime = runtime;
            this.receiverX = receiverX;
            this.receiverY = receiverY;
            this.receiverZ = receiverZ;
        }

        double queryBatch() {
            double checksum = 0.0D;
            for (int receiver = 0; receiver < receiverCount; receiver++) {
                int blockX = floor(receiverX[receiver]);
                int blockY = floor(receiverY[receiver]);
                int blockZ = floor(receiverZ[receiver]);
                long sectionKey = SectionPos.asLong(
                        SectionPos.blockToSectionCoord(blockX),
                        SectionPos.blockToSectionCoord(blockY),
                        SectionPos.blockToSectionCoord(blockZ));
                ThermalPage page = pages.get(sectionKey);
                if (page == null || !page.tryQueryPublishedCoverage(
                        SectionPos.sectionRelative(blockX),
                        SectionPos.sectionRelative(blockY),
                        SectionPos.sectionRelative(blockZ),
                        coverage)) {
                    throw new IllegalStateException("synthetic query missed current Page geometry");
                }
                if (!runtime.tryReadPublishedCell(coverage.coverageRef(), publication)) {
                    throw new IllegalStateException("synthetic query missed current publication");
                }
                if (!page.publishedGeometryIsCurrent()
                        || page.publishedGeometryRevision() != coverage.geometryRevision()
                        || page.publishedTopologyGeneration() != coverage.topologyGeneration()) {
                    throw new IllegalStateException("synthetic query observed a torn Page envelope");
                }
                long age = CURRENT_TICK - publication.sampleTick();
                if (age < 0L || age > MAXIMUM_PUBLICATION_AGE_TICKS) {
                    throw new IllegalStateException("synthetic query publication age is invalid");
                }
                checksum += publication.temperatureC()
                        + publication.mediumId()
                        + publication.flags()
                        + age;
            }
            return checksum;
        }

        String layoutId() {
            return layout.id();
        }

        int receiverCount() {
            return receiverCount;
        }

        int pageCount() {
            return pages.size();
        }

        DimensionThermalRuntime.Diagnostics diagnostics() {
            return runtime.diagnostics();
        }

        Object retainedGraphRoot() {
            return this;
        }

        @Override
        public void close() {
            runtime.close();
        }
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}
