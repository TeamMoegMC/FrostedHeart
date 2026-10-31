/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;

import java.util.Objects;
import java.util.Set;

/**
 * 在第一次权威写入前已完全验证并定容的不可变拓扑 delta。
 *
 * <p>它同时携带 Page/Brick replacement、solver fragment、
 * phase 和 publication 写入；commit 不再分配、解析 Minecraft 状态或重新
 * 计算拓扑。</p>
 */
public final class PreparedTopologyChange {
    static final byte[] NO_BYTES = new byte[0];
    static final short[] NO_SHORTS = new short[0];
    static final int[] NO_INTS = new int[0];
    static final long[] NO_LONGS = new long[0];
    static final WorkerBrickTopology[] NO_BRICKS = new WorkerBrickTopology[0];
    static final ThermalFragment[] NO_FRAGMENTS = new ThermalFragment[0];
    static final PageWrite[] NO_PAGE_WRITES = new PageWrite[0];
    static final OldSpan[] NO_OLD_SPANS = new OldSpan[0];

    final long baseStructuralVersion;
    final long nextStructuralVersion;
    final double externalMaterialEnergyJ;
    final int[] fragmentIndexes;
    final ThermalFragment[] fragments;
    final int[] removedPhaseSlots;
    final int[] addedPhaseSlots;
    public final PageWrite[] pageWrites;
    final OldSpan[] oldSpans;
    public final long[] sourceDirtySections;
    public final ThermalPageHandle.GeometryResyncToken[] committedResyncTokens;

    private final Set<AirRouteValidity> retiringRoutes;

    /** Candidate assembly excludes removed routes before live validity changes at commit. */
    public boolean retiresRoute(AirRouteValidity route) {
        return retiringRoutes.contains(route);
    }

    public int[] fragmentIndexes() { return fragmentIndexes; }
    public ThermalFragment[] fragments() { return fragments; }

    public void recordBacking(AirBackingMeter meter) {
        if (!meter.objectOnce(this, 112)) return;
        meter.array(fragmentIndexes); meter.array(fragments);
        meter.array(removedPhaseSlots);
        meter.array(addedPhaseSlots); meter.array(pageWrites); meter.array(oldSpans); meter.array(sourceDirtySections);
        meter.array(committedResyncTokens);
        meter.container(retiringRoutes, retiringRoutes.size(), 16);
        for (var fragment : fragments) fragment.recordBacking(meter);
        for (var write : pageWrites) {
            meter.objectOnce(write, 160); write.signatures.recordBacking(meter); write.publication.recordBacking(meter);
            write.page.publication.recordBacking(meter); meter.array(write.brickIndexes); meter.array(write.bricks);
            meter.array(write.skyColumns); meter.array(write.firstExposedLocalY);
            for (var brick : write.bricks) brick.recordBacking(meter);
        }
    }

    /** Payload installed in shared tables/Pages; the delta and rollback bookkeeping stay in scratch. */
    public void recordInstalledBacking(AirBackingMeter meter) {
        for (var fragment : fragments) fragment.recordBacking(meter);
        for (var write : pageWrites) if (!write.retirement) {
            meter.objectOnce(write.page, 128); meter.array(write.page.bricks); meter.array(write.page.firstExposedLocalY);
            write.signatures.recordBacking(meter); write.publication.recordBacking(meter);
            meter.array(write.firstExposedLocalY);
            for (var brick : write.bricks) brick.recordBacking(meter);
        }
    }
    public long installedVersion(long currentVersion) {
        return nextStructuralVersion == baseStructuralVersion ? currentVersion : currentVersion + 1;
    }

    public boolean onlyNaturalTemperatureUpdates() {
        if (nextStructuralVersion != baseStructuralVersion
                || externalMaterialEnergyJ != 0.0D || fragmentIndexes.length != 0
                || removedPhaseSlots.length != 0 || addedPhaseSlots.length != 0
                || oldSpans.length != 0 || sourceDirtySections.length != 0
                || committedResyncTokens.length != 0 || !retiringRoutes.isEmpty()) return false;
        for (PageWrite write : pageWrites) {
            if (!write.naturalTemperatureChanged || write.admission || write.retirement
                    || write.replacedPage != null || write.brickIndexes.length != 0
                    || write.skyColumns.length != 0) return false;
        }
        return true;
    }

    private PreparedTopologyChange(Builder builder) {
        long baseStructuralVersion = builder.baseStructuralVersion;
        long nextStructuralVersion = builder.nextStructuralVersion;
        int[] fragmentIndexes = builder.fragmentIndexes;
        ThermalFragment[] fragments = builder.fragments;
        int[] removedPhaseSlots = builder.removedPhaseSlots;
        int[] addedPhaseSlots = builder.addedPhaseSlots;
        PageWrite[] pageWrites = builder.pageWrites;
        OldSpan[] oldSpans = builder.oldSpans;
        long[] sourceDirtySections = builder.sourceDirtySections;
        ThermalPageHandle.GeometryResyncToken[] committedResyncTokens =
                builder.committedResyncTokens;
        Objects.requireNonNull(fragmentIndexes, "fragmentIndexes");
        Objects.requireNonNull(fragments, "fragments");
        Objects.requireNonNull(removedPhaseSlots, "removedPhaseSlots");
        Objects.requireNonNull(addedPhaseSlots, "addedPhaseSlots");
        Objects.requireNonNull(pageWrites, "pageWrites");
        Objects.requireNonNull(oldSpans, "oldSpans");
        Objects.requireNonNull(sourceDirtySections, "sourceDirtySections");
        Objects.requireNonNull(committedResyncTokens, "committedResyncTokens");
        if (baseStructuralVersion < 0L
                || nextStructuralVersion < baseStructuralVersion
                || fragmentIndexes.length != fragments.length) {
            throw new IllegalArgumentException(
                    "prepared topology payload is inconsistent");
        }
        requireOrderedFragments(fragmentIndexes, fragments);
        for (PageWrite write : pageWrites) {
            Objects.requireNonNull(write, "pageWrites contains null");
        }
        for (OldSpan oldSpan : oldSpans) {
            Objects.requireNonNull(oldSpan, "oldSpans contains null");
        }
        this.baseStructuralVersion = baseStructuralVersion;
        this.externalMaterialEnergyJ = builder.externalMaterialEnergyJ;
        this.nextStructuralVersion = nextStructuralVersion;
        this.fragmentIndexes = fragmentIndexes;
        this.fragments = fragments;
        this.removedPhaseSlots = removedPhaseSlots;
        this.addedPhaseSlots = addedPhaseSlots;
        this.pageWrites = pageWrites;
        this.oldSpans = oldSpans;
        this.sourceDirtySections = sourceDirtySections;
        this.committedResyncTokens = committedResyncTokens;
        this.retiringRoutes = builder.retiringRoutes;
    }

    static final class Builder {
        private Set<AirRouteValidity> retiringRoutes = Set.of();

        Builder retiringRoutes(Set<AirRouteValidity> routes) {
            retiringRoutes = routes;
            return this;
        }
        private long baseStructuralVersion;
        private long nextStructuralVersion;
        private double externalMaterialEnergyJ;
        private int[] fragmentIndexes;
        private ThermalFragment[] fragments;
        private int[] removedPhaseSlots;
        private int[] addedPhaseSlots;
        private PageWrite[] pageWrites;
        private OldSpan[] oldSpans;
        private long[] sourceDirtySections;
        private ThermalPageHandle.GeometryResyncToken[] committedResyncTokens;

        Builder identity(long baseVersion, long nextVersion) {
            baseStructuralVersion = baseVersion;
            nextStructuralVersion = nextVersion;
            return this;
        }

        Builder externalMaterialEnergy(double energyJ) {
            externalMaterialEnergyJ = energyJ;
            return this;
        }

        Builder fragments(int[] indexes, ThermalFragment[] values) {
            fragmentIndexes = indexes;
            fragments = values;
            return this;
        }

        Builder phaseSlots(int[] removedSlots, int[] addedSlots) {
            removedPhaseSlots = removedSlots;
            addedPhaseSlots = addedSlots;
            return this;
        }

        Builder pages(PageWrite[] writes, OldSpan[] releasedSpans) {
            pageWrites = writes;
            oldSpans = releasedSpans;
            return this;
        }

        Builder sourceSections(long[] dirtySections) {
            sourceDirtySections = dirtySections;
            return this;
        }

        Builder resyncTokens(
                ThermalPageHandle.GeometryResyncToken[] tokens
        ) {
            committedResyncTokens = tokens;
            return this;
        }

        PreparedTopologyChange build() {
            return new PreparedTopologyChange(this);
        }
    }

    private static void requireOrderedFragments(
            int[] indexes,
            ThermalFragment[] values
    ) {
        int previous = -1;
        for (int index = 0; index < indexes.length; index++) {
            if (indexes[index] <= previous || values[index] == null) {
                throw new IllegalArgumentException(
                        "prepared fragment writes are not ordered and valid");
            }
            previous = indexes[index];
        }
    }

    public static final class PageWrite {
        public final WorkerPageStore.PageState page;
        final WorkerPageStore.PageState replacedPage;
        final boolean admission;
        public final boolean retirement;
        final PageSignatures signatures;
        final long resolvedBrickMask;
        final long residentBrickMask;
        final long sourceSeedMask;
        public final PagePublication publication;
        final PagePublication expectedPublication;
        final long stagedBrickMask;
        final int[] brickIndexes;
        final WorkerBrickTopology[] bricks;
        final boolean naturalTemperatureChanged;
        final double naturalTemperatureC;
        final short[] skyColumns;
        final byte[] firstExposedLocalY;

        private PageWrite(
                WorkerPageStore.PageState page,
                WorkerPageStore.PageState replacedPage,
                boolean admission,
                boolean retirement,
                PageSignatures signatures,
                long resolvedBrickMask,
                long residentBrickMask,
                long sourceSeedMask,
                PagePublication publication,
                long stagedBrickMask,
                int[] brickIndexes,
                WorkerBrickTopology[] bricks,
                boolean naturalTemperatureChanged,
                double naturalTemperatureC,
                short[] skyColumns,
                byte[] firstExposedLocalY
        ) {
            if (page == null || signatures == null || publication == null
                    || brickIndexes == null || bricks == null
                    || skyColumns == null || firstExposedLocalY == null
                    || brickIndexes.length != bricks.length
                    || skyColumns.length != firstExposedLocalY.length
                    || admission && retirement
                    || (sourceSeedMask & ~residentBrickMask) != 0L
                    || retirement && (stagedBrickMask != 0L
                    || brickIndexes.length != 0)
                    || naturalTemperatureChanged
                    && !Double.isFinite(naturalTemperatureC)) {
                throw new IllegalArgumentException("prepared Page write is invalid");
            }
            if (replacedPage != null
                    && (!admission || retirement
                    || replacedPage.handle.sectionKey()
                            != page.handle.sectionKey()
                    || replacedPage.pageSlot != page.pageSlot
                    || replacedPage.handle == page.handle)) {
                throw new IllegalArgumentException(
                        "prepared Page replacement is invalid");
            }
            long indexedBricks = 0L;
            int previousBrick = -1;
            for (int index = 0; index < brickIndexes.length; index++) {
                int brick = brickIndexes[index];
                if (brick <= previousBrick
                        || brick >= ThermalPageHandle.BASE_BRICK_COUNT
                        || bricks[index] == null) {
                    throw new IllegalArgumentException(
                            "prepared Brick writes are not ordered and unique");
                }
                indexedBricks |= 1L << brick;
                previousBrick = brick;
            }
            if ((stagedBrickMask & ~indexedBricks) != 0L) {
                throw new IllegalArgumentException(
                        "staged Brick mask has no matching Page write");
            }
            int previousColumn = -1;
            for (int index = 0; index < skyColumns.length; index++) {
                int column = Short.toUnsignedInt(skyColumns[index]);
                int exposedY = Byte.toUnsignedInt(firstExposedLocalY[index]);
                if (column <= previousColumn || column >= 256
                        || exposedY > 16) {
                    throw new IllegalArgumentException(
                            "prepared sky columns are not ordered and valid");
                }
                previousColumn = column;
            }
            this.page = page;
            this.replacedPage = replacedPage;
            this.admission = admission;
            this.retirement = retirement;
            this.signatures = signatures;
            this.resolvedBrickMask = resolvedBrickMask;
            this.residentBrickMask = residentBrickMask;
            this.sourceSeedMask = sourceSeedMask;
            this.publication = publication;
            expectedPublication = replacedPage == null ? page.publication : replacedPage.publication;
            this.stagedBrickMask = stagedBrickMask;
            this.brickIndexes = brickIndexes;
            this.bricks = bricks;
            this.naturalTemperatureChanged = naturalTemperatureChanged;
            this.naturalTemperatureC = naturalTemperatureC;
            this.skyColumns = skyColumns;
            this.firstExposedLocalY = firstExposedLocalY;
        }

        static PageWrite active(
                TopologyUpdatePlanner.PageDraft draft,
                long resolvedBrickMask,
                PagePublication publication,
                int[] brickIndexes,
                WorkerBrickTopology[] bricks,
                short[] skyColumns,
                byte[] firstExposedLocalY
        ) {
            return new PageWrite(
                    draft.page,
                    draft.replacedPage,
                    draft.admission,
                    false,
                    draft.nextSignatures,
                    resolvedBrickMask,
                    draft.nextResidentBrickMask,
                    draft.nextSourceSeedMask,
                    publication,
                    draft.cellReplacementMask,
                    brickIndexes,
                    bricks,
                    draft.naturalTemperatureChanged,
                    draft.naturalTemperatureC,
                    skyColumns,
                    firstExposedLocalY);
        }

        static PageWrite retirement(TopologyUpdatePlanner.PageDraft draft) {
            return new PageWrite(
                    draft.page,
                    null,
                    false,
                    true,
                    draft.page.signatures,
                    0L,
                    0L,
                    0L,
                    PagePublication.EMPTY,
                    0L,
                    NO_INTS,
                    NO_BRICKS,
                    false,
                    draft.page.naturalTemperatureC,
                    NO_SHORTS,
                    NO_BYTES);
        }
    }

    record OldSpan(
            int pageSlot,
            int lifecycleGeneration,
            ArenaSpan span
    ) {
        OldSpan {
            if (pageSlot < 0 || lifecycleGeneration < 0 || span == null) {
                throw new IllegalArgumentException(
                        "old arena span identity is invalid");
            }
        }
    }
}
