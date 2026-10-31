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

import static com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.TRACE;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * worker 侧已提交 Page 身份与 64-Brick 目录的唯一权威。
 *
 * <p>主线程只持有 {@link com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle}
 * 和 publication；arena span 与 fragment 引用不得反向暴露。publication 只把
 * worker Page slot 作为 QueryPublication change-ID 的不透明索引交给主线程，
 * 该身份不进入网络或客户端模型。</p>
 */
public final class WorkerPageStore implements AutoCloseable, AirGeometryView {
    private java.util.function.IntFunction<AirFieldLayout.Component> regionalComponent;
    private java.util.function.IntFunction<double[]> regionalCoefficients;
    private Long2LongMap airSupportRequests;
    private final Long2ObjectOpenHashMap<Long2LongOpenHashMap> supportOwners = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<int[]> supportCounts = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap supportPreparations = new Long2IntOpenHashMap();
    private final Long2LongOpenHashMap regionalSupport = new Long2LongOpenHashMap();

    private java.util.function.IntFunction<com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot> regionalSnapshots;
    private QueryPublication regionalQueries;
    private final it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet maintenanceReady = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
    private final it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet maintenanceTargets = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
    private final Long2ObjectOpenHashMap<MaintenanceState> maintenance = new Long2ObjectOpenHashMap<>();
    private final java.util.IdentityHashMap<Object, QueryPublication.FrameLease> maintenanceFrames = new java.util.IdentityHashMap<>();
    private final Long2LongOpenHashMap routeSupport = new Long2LongOpenHashMap();
    private MaintenanceState maintenanceCursor;
    private PageState maintenancePage;
    private boolean maintenanceBlocked;
    private long maintenanceCapacityWait = -1;
    private java.util.function.IntConsumer phaseDirty = ignored -> { };
    public void phaseMaintenance(java.util.function.IntConsumer dirty) { phaseDirty = dirty; }

    private static final class MaintenanceState {
        final long[] outputs = new long[7];
        long revision, processed, runningRevision, hot, nextHot;
        PagePublication publication;
        int brick, face, phase;
        double maximumResidualC, residualTemperatureC;
        int residualX, residualY, residualZ;
        long residualTick;
        boolean residualMaterial, residualMinimum;
    }

    public void setRegionalSnapshots(java.util.function.IntFunction<com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot> snapshots,
            QueryPublication queries) {
        regionalSnapshots = snapshots; regionalQueries = queries;
        airSupportRequests = regionalSupport;
    }

    public final class SupportChange implements AutoCloseable {
        private final long owner;
        private final Long2LongOpenHashMap next;
        private final long[] addedSections, removedSections;
        private final long[] addedMasks, removedMasks;
        private int phase, cursor;
        private long remaining;
        private boolean completed, released;
        private SupportChange(long owner, Long2LongOpenHashMap next) {
            this.owner = owner; this.next = next;
            var old = supportOwners.get(owner);
            addedSections = next.keySet().toLongArray(); addedMasks = new long[addedSections.length];
            removedSections = old == null ? new long[0] : old.keySet().toLongArray(); removedMasks = new long[removedSections.length];
            for (int index = 0; index < addedSections.length; index++) {
                long section = addedSections[index]; addedMasks[index] = next.get(section);
                supportCounts.computeIfAbsent(section, ignored -> new int[64]);
                supportPreparations.addTo(section, 1);
            }
            for (int index = 0; index < removedSections.length; index++) {
                removedMasks[index] = old.get(removedSections[index]); supportPreparations.addTo(removedSections[index], 1);
            }
        }
        /** Add successors first, retaining the old completed support until the replacement is ready. */
        public boolean run(long deadline) {
            if (completed) return true;
            do {
                long[] sections = phase == 0 ? addedSections : removedSections;
                long[] masks = phase == 0 ? addedMasks : removedMasks;
                if (cursor == sections.length) {
                    if (phase++ == 1) {
                        if (next.isEmpty()) supportOwners.remove(owner); else supportOwners.put(owner, next);
                        completed = true;
                        return true;
                    }
                    cursor = 0; remaining = 0; continue;
                }
                long section = sections[cursor];
                if (remaining == 0) remaining = masks[cursor];
                int limit = 64;
                while (limit-- > 0 && remaining != 0) {
                    int brick = Long.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
                    var counts = supportCounts.get(section);
                    int count = counts[brick] += phase == 0 ? 1 : -1;
                    if (count < 0) throw new IllegalStateException("Air support owner count underflow");
                    long previous = regionalSupport.get(section), mask = count == 0 ? previous & ~(1L << brick) : previous | 1L << brick;
                    if (mask != previous) {
                        if (mask == 0) regionalSupport.remove(section); else regionalSupport.put(section, mask);
                        maintenanceTargets.add(section);
                    }
                }
                if (remaining == 0) cursor++;
            } while (System.nanoTime() < deadline);
            return false;
        }
        @Override public void close() {
            if (released) return; released = true;
            releaseSections(addedSections); releaseSections(removedSections);
        }
        private void releaseSections(long[] sections) {
            for (long section : sections) if (supportPreparations.addTo(section, -1) == 1) {
                supportPreparations.remove(section);
                if (regionalSupport.get(section) == 0) supportCounts.remove(section);
            }
        }
    }
    public SupportChange prepareAirSupport(long owner, Long2LongOpenHashMap requests) { return new SupportChange(owner, requests); }

    public void markMaintenanceDirty(long section) {
        var state = maintenance.computeIfAbsent(section, ignored -> new MaintenanceState());
        state.revision++;
        maintenanceReady.add(section);
    }

    private void markGeometryMaintenance(long section) {
        markMaintenanceDirty(section);
        maintenanceTargets.add(section);
        for (var face : BlockFace.values()) {
            long neighbor = neighborSection(section, face);
            if (pageAt(neighbor) != null) markMaintenanceDirty(neighbor);
            maintenanceTargets.add(neighbor);
        }
    }

    public boolean maintenanceReady(long capacityRevision) {
        return maintenanceCursor != null ? maintenanceCapacityWait < 0 || maintenanceCapacityWait != capacityRevision || !maintenanceTargets.isEmpty()
                : !maintenanceReady.isEmpty() || !maintenanceTargets.isEmpty();
    }

    public long maintenanceCapacityWait() { return maintenanceCapacityWait; }

    private com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot maintenanceSnapshot(int slot) {
        var snapshot = regionalSnapshots.apply(slot);
        if (snapshot == null) { maintenanceBlocked = true; return null; }
        var lease = maintenanceFrames.get(snapshot.geometryIdentity());
        if (lease == null) {
            lease = regionalQueries.borrowFrame(snapshot);
            if (lease == null) { maintenanceBlocked = true; return null; }
            maintenanceFrames.put(snapshot.geometryIdentity(), lease);
        }
        return lease.field;
    }

    private void releaseMaintenanceFrames() {
        for (var lease : maintenanceFrames.values()) lease.close();
        maintenanceFrames.clear();
    }

    /** One Brick/face at a time, preserving the Page's prior completed outputs until all are ready. */
    public void advanceMaintenance(ThermalCellArena arena, double referenceC, long deadline, long capacityRevision,
            java.util.function.Consumer<ThermalCompletion.BrickResidency> output) {
        if (regionalSnapshots == null) return;
        if (maintenanceCursor == null && !maintenanceReady.isEmpty()) {
            long section = maintenanceReady.removeFirstLong();
            var state = maintenance.get(section);
            var page = pageAt(section);
            if (page == null) {
                if (state != null) {
                    java.util.Arrays.fill(state.outputs, 0);
                    maintenance.remove(section);
                    for (var face : BlockFace.values()) maintenanceTargets.add(neighborSection(section, face));
                }
                maintenanceTargets.add(section);
            } else {
                maintenanceCursor = state; maintenancePage = page; state.publication = page.publication;
                state.runningRevision = state.revision; state.nextHot = 0; state.brick = state.face = state.phase = 0;
                state.maximumResidualC = 0;
                desiredScratch.clear();
            }
        }
        if (maintenanceCursor != null && (maintenanceCapacityWait < 0 || maintenanceCapacityWait != capacityRevision)) {
            var state = maintenanceCursor; var page = maintenancePage;
            maintenanceCapacityWait = -1;
            if (pageAt(page.handle.sectionKey()) != page || page.publication != state.publication) {
                releaseMaintenanceFrames(); markMaintenanceDirty(page.handle.sectionKey());
                maintenanceCursor = null; maintenancePage = null;
                return;
            }
            do {
                maintenanceBlocked = false;
                if (state.phase == 0) {
                    int brick = state.brick;
                    long bit = 1L << brick;
                    if ((page.residentBrickMask & bit) != 0) {
                        var topology = page.brick(brick);
                        double threshold = (state.hot & bit) == 0 ? 1.0 : .5;
                        boolean hot = false;
                        for (int slot = topology.span.firstSlot(); slot < topology.span.endSlotExclusive(); slot++) {
                            var field = maintenanceSnapshot(slot);
                            if (field == null) break;
                            if (arena.hasMaterialTransition(slot)) phaseDirty.accept(slot);
                            hot = hot || field.activeAtSlot(slot, page.naturalTemperatureC, threshold);
                            if (TRACE) {
                                double low = field.minimumAtSlotC(slot), high = field.maximumAtSlotC(slot);
                                double temperature = Math.abs(low - page.naturalTemperatureC) > Math.abs(high - page.naturalTemperatureC) ? low : high;
                                double residual = Math.abs(temperature - page.naturalTemperatureC);
                                if (residual > state.maximumResidualC) {
                                    state.maximumResidualC = residual; state.residualTemperatureC = temperature;
                                    state.residualX = arena.minimum(slot, 0); state.residualY = arena.minimum(slot, 1); state.residualZ = arena.minimum(slot, 2);
                                    state.residualTick = field.sampleTick(); state.residualMaterial = arena.isMaterialCell(slot);
                                    state.residualMinimum = field.minimumQuality();
                                }
                            }
                        }
                        if (hot) state.nextHot |= bit;
                    }
                    if (!maintenanceBlocked && ++state.brick == 64) { state.phase = 1; state.brick = state.face = 0; }
                } else if (state.phase == 1) {
                    long active = page.sourceSeedMask | state.nextHot;
                    if (state.brick == 0 && state.face == 0) orDesired(page.handle.sectionKey(), active);
                    long bit = 1L << state.brick;
                    if ((active & bit) != 0) {
                        if (state.face == 6) collectMaterialFrontier(page, bit);
                        else {
                            BlockFace face = BlockFace.fromOrdinal(state.face);
                            long target = neighborSection(page.handle.sectionKey(), face);
                            int offset = switch (face) {
                                case NEGATIVE_X -> -1; case POSITIVE_X -> 1;
                                case NEGATIVE_Y -> -16; case POSITIVE_Y -> 16;
                                case NEGATIVE_Z -> -4; case POSITIVE_Z -> 4;
                            };
                            long boundary = switch (face) {
                                case NEGATIVE_X -> X_MIN; case POSITIVE_X -> X_MAX;
                                case NEGATIVE_Y -> Y_MIN; case POSITIVE_Y -> Y_MAX;
                                case NEGATIVE_Z -> Z_MIN; case POSITIVE_Z -> Z_MAX;
                            };
                            requestFace(page, state.brick, face, (boundary & bit) == 0 ? page.handle.sectionKey() : target,
                                    (boundary & bit) == 0 ? state.brick + offset : boundaryTargetBrick(state.brick, face), arena, referenceC, 1.0, .5);
                        }
                    }
                    if (!maintenanceBlocked && ++state.face == 7) {
                        state.face = 0;
                        if (++state.brick == 64) state.phase = 2;
                    }
                } else {
                    long section = page.handle.sectionKey();
                    if (TRACE && (state.nextHot & ~state.hot) != 0) {
                        com.teammoeg.frostedheart.FHMain.LOGGER.info("THERMAL_RESIDENCY_HOT,sectionX={},sectionY={},sectionZ={},residentBricks={},newHotBricks={},hotBricks={},sourceBricks={},naturalC={},maximumResidualC={},temperatureC={},x={},y={},z={},sampleTick={},material={},minimum={}",
                                SectionPos.x(section), SectionPos.y(section), SectionPos.z(section), Long.bitCount(page.residentBrickMask),
                                Long.bitCount(state.nextHot & ~state.hot), Long.bitCount(state.nextHot), Long.bitCount(page.sourceSeedMask),
                                page.naturalTemperatureC, state.maximumResidualC, state.residualTemperatureC, state.residualX, state.residualY, state.residualZ,
                                state.residualTick, state.residualMaterial, state.residualMinimum);
                    }
                    state.outputs[0] = desiredScratch.get(section);
                    maintenanceTargets.add(section);
                    for (var face : BlockFace.values()) {
                        long neighbor = neighborSection(section, face);
                        state.outputs[face.ordinal() + 1] = desiredScratch.get(neighbor);
                        maintenanceTargets.add(neighbor);
                    }
                    state.hot = state.nextHot; state.processed = state.runningRevision;
                    releaseMaintenanceFrames(); maintenanceCursor = null; maintenancePage = null;
                    if (state.revision != state.processed) maintenanceReady.add(section);
                    break;
                }
                if (maintenanceBlocked) { maintenanceCapacityWait = capacityRevision; break; }
            } while (System.nanoTime() < deadline);
        }
        int remaining = 7;
        while (remaining-- > 0 && !maintenanceTargets.isEmpty() && System.nanoTime() < deadline) {
            long section = maintenanceTargets.removeFirstLong();
            var own = maintenance.get(section);
            long desired = own == null ? 0 : own.outputs[0];
            var capturedPage = pageAt(section);
            if (capturedPage != null && (own == null || own.processed == 0)) desired |= capturedPage.residentBrickMask;
            for (var face : BlockFace.values()) {
                var neighbor = maintenance.get(neighborSection(section, face));
                if (neighbor != null) desired |= neighbor.outputs[(face.ordinal() ^ 1) + 1];
            }
            if (airSupportRequests != null) desired |= airSupportRequests.get(section);
            desired |= routeSupport.get(section);
            long previous = desiredBySection.get(section);
            if (previous == desired) continue;
            if (desired == 0) desiredBySection.remove(section); else desiredBySection.put(section, desired);
            var page = pageAt(section);
            output.accept(new ThermalCompletion.BrickResidency(section, page == null ? -1 : page.handle.lifecycleGeneration(), desired));
            if (page != null) markMaintenanceDirty(section);
            for (var face : BlockFace.values()) {
                long neighbor = neighborSection(section, face);
                if (pageAt(neighbor) != null) markMaintenanceDirty(neighbor);
            }
        }
    }


    public long capturedBrickMask(long sectionKey) {
        PageState page = find(sectionKey);
        if (page == null) return 0;
        long mask = 0;
        for (int brick = 0; brick < 64; brick++) if (page.brick(brick).cellsResolved) mask |= 1L << brick;
        return mask & page.residentBrickMask;
    }

    int capturedSignatureAt(int x, int y, int z) {
        PageState page = find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        return page != null && (page.residentBrickMask & 1L << brick) != 0
                ? page.signatures.get((x & 15) | (z & 15) << 4 | (y & 15) << 8) : haloSignatureAt(x, y, z);
    }

    /** Borrowed worker state for residency bounds; it is not a second temperature authority. */
    /** Frontier reads the accepted owner directly; no dimension-sized temperature copy. */
    public void setRegionalAirField(java.util.function.IntFunction<AirFieldLayout.Component> component,
            java.util.function.IntFunction<double[]> coefficients) {
        regionalComponent = component; regionalCoefficients = coefficients;
    }

    /** Excludes old spans that are still live only until source rebinding releases them. */
    public boolean ownsCommittedCell(ThermalCellArena arena, int slot) {
        PageState page = findPageSlot(arena.pageSlot(slot));
        if (page == null) return false;
        int x = arena.minimum(slot, 0), y = arena.minimum(slot, 1), z = arena.minimum(slot, 2);
        int brickIndex = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        var span = page.brick(brickIndex).span;
        return slot >= span.firstSlot() && slot < span.endSlotExclusive();
    }

    @Override
    public boolean ownsCell(ThermalCellArena arena, int slot) {
        return arena.isLive(slot) && ownsCommittedCell(arena, slot);
    }

    @Override
    public PageState pageAt(long sectionKey) { return find(sectionKey); }

    @Override
    public PageState pageAtSlot(int pageSlot) { return findPageSlot(pageSlot); }

    @Override
    public long geometryRevision(PageState page) { return page.geometryRevision; }

    @Override
    public int signatureAtWorld(int x, int y, int z) { return capturedSignatureAt(x, y, z); }

    private final java.util.IdentityHashMap<ThermalPageHandle, GeometryHalo> halos =
            new java.util.IdentityHashMap<>();
    private final Long2IntOpenHashMap haloSignatures = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap haloReferences = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap haloSectionPositions = new Long2IntOpenHashMap();
    private final Long2LongOpenHashMap haloSectionVersions = new Long2LongOpenHashMap();
    private long haloVersion;
    private ThermalSignatureTable signatureTable;

    void signatures(ThermalSignatureTable signatures) {
        signatureTable = signatures;
    }

    GeometryHalo halo(ThermalPageHandle owner) { return halos.get(owner); }
    int haloReferences(long position) { return haloReferences.get(position); }
    int haloSignature(long position) {
        return haloReferences.containsKey(position) ? haloSignatures.get(position) : ThermalSignatureTable.UNRESOLVED;
    }
    long haloVersion(long section) { return haloSectionVersions.get(section); }

    private void haloChanged(long position, int positionDelta) {
        long section = SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4);
        int remaining = haloSectionPositions.addTo(section, positionDelta) + positionDelta;
        long revision = ++haloVersion;
        if (remaining == 0) { haloSectionPositions.remove(section); haloSectionVersions.remove(section); }
        else haloSectionVersions.put(section, revision);
    }

    void replaceHalo(ThermalPageHandle owner, GeometryHalo halo) {
        removeHalo(owner);
        halos.put(owner, halo);
        for (int index = 0; index < halo.size(); index++) {
            long position = halo.positions()[index];
            haloSignatures.put(position, halo.signatures()[index]);
            haloChanged(position, haloReferences.addTo(position, 1) == 0 ? 1 : 0);
        }
    }

    void removeHalo(ThermalPageHandle owner) {
        var previous = halos.remove(owner);
        if (previous == null) return;
        for (long position : previous.positions()) {
            if (haloReferences.addTo(position, -1) == 1) {
                haloReferences.remove(position);
                haloSignatures.remove(position);
                haloChanged(position, -1);
            }
        }
    }

    int haloSignatureAt(int x, int y, int z) {
        long position = BlockPos.asLong(x, y, z);
        return haloReferences.containsKey(position)
                ? haloSignatures.get(position)
                : ThermalSignatureTable.UNRESOLVED;
    }

    private AirRouteCompiler airRoutes;

    void airRoutes(AirRouteCompiler compiler) {
        airRoutes = compiler;
        if (regionalSnapshots != null) {
            for (long section : routeSupport.keySet()) maintenanceTargets.add(section);
            routeSupport.clear();
            if (compiler != null) compiler.collectRequiredBricks(this, routeSupport);
            for (long section : routeSupport.keySet()) maintenanceTargets.add(section);
        }
    }

    boolean hasThermalInterestAt(long position) {
        int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
        PageState page = find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        if (page == null) return false;
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        var state = maintenance.get(page.handle.sectionKey());
        return ((page.sourceSeedMask | (state == null ? 0 : state.hot)) & 1L << brick) != 0;
    }

    public boolean hasResidentBrick(long position) {
        int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
        PageState page = find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        return page != null && (page.residentBrickMask & 1L << brick) != 0;
    }

    static final int PORT_BLOCKED = -1;
    public static final int PORT_TOPOLOGY_UNAVAILABLE = -2;
    private final Long2ObjectOpenHashMap<PageState> activeBySection;
    private final Int2ObjectOpenHashMap<PageState> activeBySlot;
    private final IntArrayList freePageSlots;
    private final LongArrayList residencyIdentityChanges = new LongArrayList();
    private int pageSlotHighWater;
    private Long2LongOpenHashMap desiredBySection = new Long2LongOpenHashMap();
    private Long2LongOpenHashMap desiredScratch = new Long2LongOpenHashMap();
    private boolean closed;

    private static final long X_MIN = 0x1111_1111_1111_1111L;
    private static final long X_MAX = 0x8888_8888_8888_8888L;
    private static final long Z_MIN = 0x000f_000f_000f_000fL;
    private static final long Z_MAX = 0xf000_f000_f000_f000L;
    private static final long Y_MIN = 0x0000_0000_0000_ffffL;
    private static final long Y_MAX = 0xffff_0000_0000_0000L;

    public WorkerPageStore(int maximumPages) {
        if (maximumPages <= 0) {
            throw new IllegalArgumentException("maximumPages must be positive");
        }
        activeBySection = new Long2ObjectOpenHashMap<>(maximumPages);
        activeBySlot = new Int2ObjectOpenHashMap<>(maximumPages);
        freePageSlots = new IntArrayList(maximumPages);
    }

    private static PageState visiblePage(PageState stored) {
        if (stored == null || stored.installToken == null) return stored;
        return stored.installToken.committed() ? stored.retiring ? null : stored : stored.previousState;
    }

    /** Prepare one immutable Page successor while all accessors still resolve its predecessor. */
    PageState stagePage(PreparedTopologyChange.PageWrite write,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
        var old = pageAt(write.page.handle.sectionKey());
        var source = write.page;
        var next = new PageState(source.handle, source.pageSlot, source.lifecycleGeneration,
                write.publication.geometryRevision(), write.residentBrickMask, write.sourceSeedMask, write.signatures,
                write.naturalTemperatureChanged ? write.naturalTemperatureC : source.naturalTemperatureC,
                source.firstExposedLocalY.clone(), source.dormantAir, source.dormantMaterials);
        System.arraycopy(source.bricks, 0, next.bricks, 0, next.bricks.length);
        for (int index = 0; index < write.brickIndexes.length; index++) next.bricks[write.brickIndexes[index]] = write.bricks[index];
        for (int index = 0; index < write.skyColumns.length; index++)
            next.firstExposedLocalY[Short.toUnsignedInt(write.skyColumns[index])] = write.firstExposedLocalY[index];
        next.geometryRevision = write.publication.geometryRevision(); next.topologyGeneration = write.publication.topologyGeneration();
        next.resolvedBrickMask = write.resolvedBrickMask; next.publication = write.publication;
        next.previousState = old; next.installToken = token; next.retiring = write.retirement;
        activeBySection.put(source.handle.sectionKey(), next); activeBySlot.put(source.pageSlot, next);
        source.handle.stagePublication(token, write.retirement ? PagePublication.EMPTY : write.publication);
        if (write.replacedPage != null) write.replacedPage.handle.stagePublication(token, PagePublication.EMPTY);
        return next;
    }

    void finishPage(PageState staged, PreparedTopologyChange.PageWrite write,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
        var previous = staged.previousState;
        if (!token.committed()) {
            if (previous == null) { activeBySection.remove(staged.handle.sectionKey()); activeBySlot.remove(staged.pageSlot); }
            else { activeBySection.put(previous.handle.sectionKey(), previous); activeBySlot.put(previous.pageSlot, previous); }
        } else if (write.retirement) {
            activeBySection.remove(staged.handle.sectionKey()); activeBySlot.remove(staged.pageSlot);
            releasePageSlot(staged.pageSlot);
        } else {
            if (previous != null && previous.handle != staged.handle) removeHalo(previous.handle);
            if (write.admission) {
                residencyIdentityChanges.add(staged.handle.sectionKey());
            }
            staged.dormantAir = null; staged.dormantMaterials = null;
        }
        if (token.committed() && regionalSnapshots != null) markGeometryMaintenance(staged.handle.sectionKey());
        staged.handle.finishPublication(token);
        if (write.replacedPage != null) write.replacedPage.handle.finishPublication(token);
        staged.previousState = null; staged.installToken = null;
    }

    PageState find(long sectionKey) {
        return visiblePage(activeBySection.get(sectionKey));
    }

    PageState find(ThermalPageHandle handle) {
        PageState state = visiblePage(activeBySection.get(handle.sectionKey()));
        return state != null && state.handle == handle ? state : null;
    }

    PageState findPageSlot(int pageSlot) {
        return visiblePage(activeBySlot.get(pageSlot));
    }

    public int materialSlot(ThermalPageHandle handle, int position, int expectedStateId) {
        PageState page = find(handle);
        if (page == null
                || signatureTable.materialStateId(page.signatures.get(position)) != expectedStateId)
            return -1;
        return cellSlot(page, position);
    }

    /** Resolve a recorded material's world identity after an arena/layout replacement. */
    public int materialSlot(long blockPosition, int expectedProfileId) {
        int x = net.minecraft.core.BlockPos.getX(blockPosition);
        int y = net.minecraft.core.BlockPos.getY(blockPosition);
        int z = net.minecraft.core.BlockPos.getZ(blockPosition);
        PageState page = pageAt(net.minecraft.core.SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        int position = (x & 15) | (z & 15) << 4 | (y & 15) << 8;
        if (page == null || signatureTable.materialProfileId(page.signatures.get(position)) != expectedProfileId)
            return -1;
        return cellSlot(page, position);
    }

    private static int cellSlot(PageState page, int position) {
        int x = position & 15, y = position >>> 8 & 15, z = position >>> 4 & 15;
        var brick = page.brick((x >>> 2) | (z >>> 2) << 2 | (y >>> 2) << 4);
        return brick.cellsResolved ? brick.slotAt((x & 3) | (z & 3) << 2 | (y & 3) << 4) : -1;
    }

    public void awaitChangedMaterials(ThermalInputBatch batch, ThermalCellArena arena,
            java.util.function.IntConsumer materialControlChanged) {
        var changes = batch.geometry().materialChanges();
        for (int index = 0; index < changes.size(); index++) {
            int previousState = signatureTable.materialStateId(changes.previousSignature(index));
            int slot = materialSlot(changes.page(index), changes.blockIndex(index), previousState);
            if (slot >= 0) {
                arena.awaitMaterialLayout(slot);
                if (materialControlChanged != null) materialControlChanged.accept(slot);
            }
        }
        for (int index = 0; index < batch.geometry().size(); index++) {
            if (batch.geometry().geometryResyncReason(index)
                    != ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED) continue;
            PageState page = find(batch.geometry().page(index));
            if (page == null) continue;
            for (WorkerBrickTopology brick : page.bricks) {
                for (int slot = brick.span.firstSlot();
                        slot < brick.span.endSlotExclusive();
                        slot++) {
                    arena.awaitMaterialLayout(slot);
                    if (materialControlChanged != null && arena.isLive(slot) && arena.materialLaw(slot) != null)
                        materialControlChanged.accept(slot);
                }
            }
        }
    }

    int activePageCount() {
        return activeBySection.size();
    }

    void forEachPage(java.util.function.Consumer<PageState> consumer) {
        for (PageState stored : activeBySection.values()) {
            PageState page = visiblePage(stored); if (page != null) consumer.accept(page);
        }
    }

    int haloSignatureCount(LongSet sections) {
        if (sections == null) return haloSignatures.size();
        int count = 0;
        for (long section : sections) count = Math.addExact(count, haloSectionPositions.get(section));
        return count;
    }

    /** Geometry footprint only; includes actual halo sections rather than a radius guess. */
    boolean hasCapturedSection(long section) {
        return activeBySection.containsKey(section) || haloSectionPositions.containsKey(section);
    }
    int capturedSectionCountBound() { return activeBySection.size() + haloSectionPositions.size(); }

    Long2IntOpenHashMap copyHaloSignatures(LongSet sections, int capacityBound) {
        var copy = new Long2IntOpenHashMap(capacityBound);
        copy.defaultReturnValue(ThermalSignatureTable.UNRESOLVED);
        var iterator = haloSignatures.long2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            long position = entry.getLongKey();
            if (sections == null || sections.contains(SectionPos.asLong(
                    BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4)))
                copy.put(position, entry.getIntValue());
        }
        return copy;
    }

    int pageSlotCapacity() {
        return pageSlotHighWater;
    }

    public void recordBacking(com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter meter) {
        meter.container(activeBySection, pageSlotHighWater, 16); meter.container(activeBySlot, pageSlotHighWater, 12);
        meter.array(freePageSlots.elements()); meter.array(residencyIdentityChanges.elements());
        meter.container(desiredBySection, desiredBySection.size(), 16); meter.container(desiredScratch, desiredScratch.size(), 16);
        meter.container(halos, pageSlotHighWater, 16); meter.container(haloSignatures, haloSignatures.size(), 12);
        meter.container(haloReferences, haloReferences.size(), 12);
        meter.container(haloSectionPositions, haloSectionPositions.size(), 12);
        meter.container(haloSectionVersions, haloSectionVersions.size(), 16);
        for (var halo : halos.values()) { meter.array(halo.positions()); meter.array(halo.signatures()); }
        for (var page : activeBySection.values()) {
            if (!meter.objectOnce(page, 384)) continue;
            meter.array(page.bricks); meter.array(page.firstExposedLocalY); page.signatures.recordBacking(meter);
            page.publication.recordBacking(meter);
            for (var brick : page.bricks) brick.recordBacking(meter);
            if (page.dormantAir != null) page.dormantAir.entry().recordBacking(meter);
            if (page.dormantMaterials != null && page.dormantMaterials.state() != null)
                page.dormantMaterials.state().recordBacking(meter);
        }
        meter.container(maintenance, maintenance.size(), 16);
        meter.container(maintenanceReady, maintenanceReady.size(), 16);
        meter.container(maintenanceTargets, maintenanceTargets.size(), 16);
        meter.container(maintenanceFrames, maintenanceFrames.size(), 16);
        meter.container(routeSupport, routeSupport.size(), 16);
        meter.container(supportOwners, supportOwners.size(), 16); meter.container(supportCounts, supportCounts.size(), 16);
        meter.container(supportPreparations, supportPreparations.size(), 12);
        meter.container(regionalSupport, regionalSupport.size(), 16);
        for (var counts : supportCounts.values()) meter.array(counts);
        for (var requests : supportOwners.values()) meter.container(requests, requests.size(), 16);
        for (var state : maintenance.values()) { meter.bytes(128); meter.array(state.outputs); }
        if (airRoutes != null) airRoutes.recordBacking(meter);
    }

    private void collectMaterialFrontier(PageState page, long activeBricks) {
        if (signatureTable == null) return;
        while (activeBricks != 0) {
            int brick = Long.numberOfTrailingZeros(activeBricks);
            activeBricks &= activeBricks - 1;
            var layout = page.brick(brick).blockLayout;
            if (layout == null) continue;
            long surfaces = layout.airContactBlocks();
            long origin = AirRouteCompiler.brickPosition(page, brick);
            while (surfaces != 0) {
                int block = Long.numberOfTrailingZeros(surfaces);
                surfaces &= surfaces - 1;
                long position = AirRouteCompiler.blockPosition(origin, block);
                int x = BlockPos.getX(position),
                        y = BlockPos.getY(position),
                        z = BlockPos.getZ(position);
                int contacts =
                        signatureTable.fullContactFaces(
                                page.signatures.get(BlockBrickLayout.pageBlock(brick, block)));
                for (int face = 0; face < 6; face++) {
                    if ((contacts & 1 << face) == 0) continue;
                    int nx = x + TopologyView.DX[face],
                            ny = y + TopologyView.DY[face],
                            nz = z + TopologyView.DZ[face];
                    long section = SectionPos.asLong(nx >> 4, ny >> 4, nz >> 4);
                    int neighborBrick =
                            (nx & 15) >>> 2 | ((nz & 15) >>> 2) << 2 | ((ny & 15) >>> 2) << 4;
                    PageState neighbor = find(section);
                    // A cold resident neighbor still needs the owner's incoming request.
                    int signature =
                            neighbor != null
                                            && (neighbor.residentBrickMask & 1L << neighborBrick)
                                                    != 0
                                    ? neighbor.signatures.get(
                                            (nx & 15) | (nz & 15) << 4 | (ny & 15) << 8)
                                    : haloSignatureAt(nx, ny, nz);
                    if (signatureTable.materialProfileId(signature) != 0
                            && (signatureTable.fullContactFaces(signature) & 1 << (face ^ 1))
                                    != 0) {
                        orDesired(section, 1L << neighborBrick);
                    }
                }
            }
        }
    }

    private void requestFace(
            PageState page,
            int ownerBrick,
            BlockFace face,
            long targetSection,
            int targetBrick,
            ThermalCellArena arena,
            double referenceTemperatureC,
            double refineHighC,
            double releaseLowC) {
        long targetBit = 1L << targetBrick;
        double threshold =
                (desiredBySection.get(targetSection) & targetBit) != 0L ? releaseLowC : refineHighC;
        if (faceResidualC(page, ownerBrick, face, arena, referenceTemperatureC) >= threshold) {
            orDesired(targetSection, targetBit);
        }
    }

    private double faceResidualC(
            PageState page,
            int brick,
            BlockFace face,
            ThermalCellArena arena,
            double referenceTemperatureC) {
        WorkerBrickTopology topology = page.brick(brick);
        if (!topology.resolved || topology.coverageSlot < 0) {
            return 0.0D;
        }
        if (regionalComponent != null) return continuousFaceResidual(page, topology, brick, face, referenceTemperatureC);
        if (topology.blockLayout == null) {
            if (face == BlockFace.POSITIVE_Y && allTopColumnsDirectSky(page, brick)) {
                return 0.0D;
            }
            return Math.abs(
                    arena.temperatureC(topology.coverageSlot, referenceTemperatureC)
                            - page.naturalTemperatureC);
        }
        double residual = 0.0D;
        long seen = 0;
        int axis = face.ordinal() / 2, side = (face.ordinal() & 1) == 0 ? 0 : 3;
        for (int i = 0; i < 16; i++) {
            if (face == BlockFace.POSITIVE_Y && topPortDirectSky(page, brick, i)) continue;
            int node = topology.blockLayout.airNodeAt(BlockBrickLayout.faceBlock(axis, side, i));
            if (node < 0 || (seen & 1L << node) != 0) continue;
            seen |= 1L << node;
            int slot = topology.coverageSlot + node;
            residual =
                    Math.max(
                            residual,
                            Math.abs(
                                    arena.temperatureC(slot, referenceTemperatureC)
                                            - page.naturalTemperatureC));
        }
        return residual;
    }

    private double continuousFaceResidual(PageState page, WorkerBrickTopology topology, int brick,
            BlockFace face, double referenceC) {
        int axis = face.ordinal() / 2;
        int side = (face.ordinal() & 1) == 0 ? 0 : 3;
        double residualC = 0;
        int componentSlot = -1, sampledVertices = 0;
        AirFieldLayout.Component component = null;
        com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot sampledField = null;
        double[] coefficients = null;
        for (int patch = 0; patch < 16; patch++) {
            if (face == BlockFace.POSITIVE_Y && topPortDirectSky(page, brick, patch)) continue;
            int block = BlockBrickLayout.faceBlock(axis, side, patch);
            int slot = topology.airSlotAt(block);
            if (slot < 0) continue;
            if (slot != componentSlot) {
                if (maintenanceCursor != null) {
                    var field = maintenanceSnapshot(slot);
                    if (field == null) return 0;
                    sampledField = field;
                    component = field.componentForSlot(slot); coefficients = field.borrowedAirCoefficients();
                } else {
                    component = regionalComponent.apply(slot);
                    coefficients = regionalCoefficients.apply(slot);
                    sampledField = regionalSnapshots == null ? null : regionalSnapshots.apply(slot);
                }
                componentSlot = slot;
                sampledVertices = 0;
            }
            if (component == null) continue;
            // Face tangent order differs on X faces: use actual local coordinates.
            int vertexBase = (axis == 0 ? block >>> 4 : block & 3)
                    + 5 * (axis == 2 ? block >>> 4 : block >>> 2 & 3);
            for (int corner = 0; corner < 4; corner++) {
                // Fine vertices at the same coordinate may belong to different Air branches.
                // Their direct lookups are cheap; coarse fields can share the coordinate sample.
                if (!component.isUnitGrid() || sampledField != null && sampledField.minimumQuality()) {
                    int vertexBit = 1 << (vertexBase + (corner & 1) + 5 * (corner >>> 1));
                    if ((sampledVertices & vertexBit) != 0) continue;
                    sampledVertices |= vertexBit;
                }
                int x = axis == 0 ? face.ordinal() & 1 : corner & 1;
                int y = axis == 1 ? face.ordinal() & 1 : axis == 0 ? corner & 1 : corner >>> 1;
                int z = axis == 2 ? face.ordinal() & 1 : corner >>> 1;
                int vertex = x | z << 1 | y << 2;
                double residual = sampledField == null ? referenceC + component.temperatureOffsetAtVertex(block, vertex, coefficients) - page.naturalTemperatureC
                        : sampledField.vertexPerturbationC(component, block, vertex, page.naturalTemperatureC);
                residualC = Math.max(residualC, Math.abs(residual));
            }
        }
        return residualC;
    }

    private static boolean allTopColumnsDirectSky(PageState page, int brick) {
        if ((brick >>> 4 & 3) != 3) {
            return false;
        }
        int firstX = (brick & 3) << 2;
        int firstZ = (brick >>> 2 & 3) << 2;
        for (int z = firstZ; z < firstZ + 4; z++) {
            for (int x = firstX; x < firstX + 4; x++) {
                if (Byte.toUnsignedInt(page.firstExposedLocalY[x | z << 4]) > 15) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean topPortDirectSky(PageState page, int brick, int blockInBrick) {
        if ((brick >>> 4 & 3) != 3) {
            return false;
        }
        int x = ((brick & 3) << 2) + (blockInBrick & 3);
        int z = ((brick >>> 2 & 3) << 2) + (blockInBrick >>> 2 & 3);
        return Byte.toUnsignedInt(page.firstExposedLocalY[x | z << 4]) <= 15;
    }

    private void orDesired(long sectionKey, long mask) {
        desiredScratch.put(sectionKey, desiredScratch.get(sectionKey) | mask);
    }

    private static long neighborSection(long sectionKey, BlockFace face) {
        int x = net.minecraft.core.SectionPos.x(sectionKey);
        int y = net.minecraft.core.SectionPos.y(sectionKey);
        int z = net.minecraft.core.SectionPos.z(sectionKey);
        return switch (face) {
            case NEGATIVE_X -> net.minecraft.core.SectionPos.asLong(x - 1, y, z);
            case POSITIVE_X -> net.minecraft.core.SectionPos.asLong(x + 1, y, z);
            case NEGATIVE_Y -> net.minecraft.core.SectionPos.asLong(x, y - 1, z);
            case POSITIVE_Y -> net.minecraft.core.SectionPos.asLong(x, y + 1, z);
            case NEGATIVE_Z -> net.minecraft.core.SectionPos.asLong(x, y, z - 1);
            case POSITIVE_Z -> net.minecraft.core.SectionPos.asLong(x, y, z + 1);
        };
    }

    private static int boundaryTargetBrick(int ownerBrick, BlockFace face) {
        return switch (face) {
            case NEGATIVE_X -> ownerBrick + 3;
            case POSITIVE_X -> ownerBrick - 3;
            case NEGATIVE_Y -> ownerBrick + 48;
            case POSITIVE_Y -> ownerBrick - 48;
            case NEGATIVE_Z -> ownerBrick + 12;
            case POSITIVE_Z -> ownerBrick - 12;
        };
    }

    PreparedTopologyChange.PageWrite[] prepareWrites(
            ArrayList<TopologyUpdatePlanner.PageDraft> drafts,
            int draftCount,
            ArrayList<ThermalPageHandle.GeometryResyncToken> resyncTokens) {
        if (draftCount == 0) {
            return PreparedTopologyChange.NO_PAGE_WRITES;
        }
        PreparedTopologyChange.PageWrite[] writes =
                new PreparedTopologyChange.PageWrite[draftCount];
        int admissionCount = 0;
        for (int index = 0; index < draftCount; index++) {
            TopologyUpdatePlanner.PageDraft draft = drafts.get(index);
            if (draft.retirement) {
                writes[index] = PreparedTopologyChange.PageWrite.retirement(draft);
                continue;
            }
            if (draft.admission) {
                admissionCount++;
            }
            int replacementCount = Long.bitCount(draft.replacementMask);
            int[] brickIndexes =
                    replacementCount == 0
                            ? PreparedTopologyChange.NO_INTS
                            : new int[replacementCount];
            WorkerBrickTopology[] bricks =
                    replacementCount == 0
                            ? PreparedTopologyChange.NO_BRICKS
                            : new WorkerBrickTopology[replacementCount];
            boolean structural =
                    draft.fragmentChangedMask != 0L
                            || draft.cellReplacementMask != 0L
                            || draft.admission;
            long publicationChanges = draft.replacementMask | draft.signatureChangedMask;
            boolean publicationChanged =
                    structural
                            || publicationChanges != 0L
                            || draft.geometryRevision != draft.page.geometryRevision;
            PagePublication previousPublication =
                    draft.admission ? PagePublication.EMPTY : draft.page.publication;
            PagePublication.Brick[] publicationBricks = null;
            long resolvedMask = draft.admission ? 0L : draft.page.resolvedBrickMask;
            int write = 0;
            long remaining = draft.replacementMask;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                WorkerBrickTopology topology = draft.replacements[brick];
                brickIndexes[write] = brick;
                bricks[write++] = topology;
                if (topology.resolved) {
                    resolvedMask |= 1L << brick;
                } else {
                    resolvedMask &= ~(1L << brick);
                }
                remaining &= remaining - 1L;
            }
            while (publicationChanges != 0L) {
                int brick = Long.numberOfTrailingZeros(publicationChanges);
                WorkerBrickTopology previous = draft.page.brick(brick);
                WorkerBrickTopology topology = draft.replacements[brick];
                if (topology == null) {
                    topology = previous;
                }
                if (draft.admission
                        || (draft.signatureChangedMask & 1L << brick) != 0L
                        || queryMetadataChanged(previous, topology)) {
                    if (publicationBricks == null) {
                        publicationBricks = previousPublication.copyBricks();
                    }
                    publicationBricks[brick] =
                            (draft.nextResidentBrickMask & 1L << brick) == 0L
                                    ? PagePublication.Brick.EMPTY
                                    : publicationBrick(topology, draft.nextSignatures.brickPayload(brick));
                }
                publicationChanges &= publicationChanges - 1L;
            }
            long topologyGeneration =
                    structural
                            ? Math.incrementExact(draft.page.topologyGeneration)
                            : draft.page.topologyGeneration;
            PagePublication publication = previousPublication;
            if (publicationChanged) {
                publication =
                        publicationBricks == null
                                ? previousPublication.withIdentities(
                                        draft.geometryRevision, topologyGeneration)
                                : PagePublication.owned(
                                        draft.page.pageSlot,
                                        draft.geometryRevision,
                                        topologyGeneration,
                                        publicationBricks);
            }
            writes[index] =
                    PreparedTopologyChange.PageWrite.active(
                            draft,
                            resolvedMask,
                            publication,
                            brickIndexes,
                            bricks,
                            draft.skyCount == 0
                                    ? PreparedTopologyChange.NO_SHORTS
                                    : Arrays.copyOf(draft.skyColumns, draft.skyCount),
                            draft.skyCount == 0
                                    ? PreparedTopologyChange.NO_BYTES
                                    : Arrays.copyOf(draft.skyValues, draft.skyCount));
            if (draft.resyncToken != null) {
                resyncTokens.add(draft.resyncToken);
            }
        }
        if (admissionCount != 0) {
            residencyIdentityChanges.ensureCapacity(
                    Math.addExact(residencyIdentityChanges.size(), admissionCount));
        }
        return writes;
    }

    private static boolean queryMetadataChanged(
            WorkerBrickTopology previous, WorkerBrickTopology next) {
        return previous.resolved != next.resolved
                || previous.coverageSlot != next.coverageSlot
                || previous.coverageGeneration != next.coverageGeneration
                || previous.blockLayout != next.blockLayout
                || previous.airNodeCount != next.airNodeCount;
    }

    private static PagePublication.Brick publicationBrick(
            WorkerBrickTopology topology, Object signaturePayload) {
        return new PagePublication.Brick(
                topology.cellsResolved && topology.span.count() > 0
                        ? topology.span.firstSlot()
                        : -1,
                topology.cellsResolved ? topology.coverageGeneration : 0,
                signaturePayload,
                topology.cellsResolved ? topology.blockLayout : null,
                topology.cellsResolved ? topology.airNodeCount : 0,
                topology.cellsResolved);
    }

    public static final class MutableAirTarget {
        private int generation;
        private int blockX, blockY, blockZ;
        private BlockFace face;

        public int generation() {
            return generation;
        }
        public int blockX() { return blockX; }
        public int blockY() { return blockY; }
        public int blockZ() { return blockZ; }
        public BlockFace face() { return face; }

        void set(int generation, int x, int y, int z, BlockFace face) {
            this.generation = generation;
            blockX = x;
            blockY = y;
            blockZ = z;
            this.face = face;
        }
    }

    /** Resolves a slot and writes its generation to the caller's reusable result on success. */
    public int resolveAirFaceTarget(
            int blockX,
            int blockY,
            int blockZ,
            BlockFace face,
            ThermalSignatureTable signatures,
            MutableAirTarget out) {
        long sectionKey =
                net.minecraft.core.SectionPos.asLong(
                        net.minecraft.core.SectionPos.blockToSectionCoord(blockX),
                        net.minecraft.core.SectionPos.blockToSectionCoord(blockY),
                        net.minecraft.core.SectionPos.blockToSectionCoord(blockZ));
        PageState page = visiblePage(activeBySection.get(sectionKey));
        if (page == null) {
            return PORT_TOPOLOGY_UNAVAILABLE;
        }
        int localX = net.minecraft.core.SectionPos.sectionRelative(blockX);
        int localY = net.minecraft.core.SectionPos.sectionRelative(blockY);
        int localZ = net.minecraft.core.SectionPos.sectionRelative(blockZ);
        int pageBlock = localX | localZ << 4 | localY << 8;
        int brickIndex = (localX >>> 2) | (localZ >>> 2) << 2 | (localY >>> 2) << 4;
        WorkerBrickTopology brick = page.brick(brickIndex);
        if (!brick.cellsResolved) {
            return PORT_TOPOLOGY_UNAVAILABLE;
        }
        int signatureId = page.signatures.get(pageBlock);
        int blockInBrick = localX & 3 | (localZ & 3) << 2 | (localY & 3) << 4;
        int slot = brick.airSlotAt(blockInBrick);
        if (slot >= 0) {
            out.set(page.lifecycleGeneration, blockX, blockY, blockZ, face);
            return slot;
        }
        if (signatures.ventilation(signatureId) == 0) return PORT_BLOCKED;
        if (brick.blockLayout != null && brick.blockLayout.hasCommittedAirRoute(blockInBrick)) {
            long target = brick.blockLayout.routedAirBlock(blockInBrick);
            int x = BlockPos.getX(target), y = BlockPos.getY(target), z = BlockPos.getZ(target);
            PageState targetPage = find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
            if (targetPage == null) return PORT_TOPOLOGY_UNAVAILABLE;
            int targetBrick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
            var topology = targetPage.brick(targetBrick);
            int targetBlock = (x & 3) | (z & 3) << 2 | (y & 3) << 4;
            slot = topology.cellsResolved ? topology.airSlotAt(targetBlock) : -1;
            if (slot >= 0) {
                var outlet = airRoutes.outletTrace(BlockPos.asLong(blockX, blockY, blockZ));
                if (outlet == null) return PORT_TOPOLOGY_UNAVAILABLE;
                long outletBlock = outlet.block(0);
                out.set(targetPage.lifecycleGeneration, BlockPos.getX(outletBlock), BlockPos.getY(outletBlock),
                        BlockPos.getZ(outletBlock), BlockFace.fromOrdinal(outlet.face(0)));
                return slot;
            }
        }
        long position = BlockPos.asLong(blockX, blockY, blockZ);
        return airRoutes != null && airRoutes.closed(position)
                ? PORT_BLOCKED
                : PORT_TOPOLOGY_UNAVAILABLE;
    }

    PageState stageAdmission(ThermalInputBatch.PageAdmission admission) {
        requireOpen();
        ThermalPageHandle handle = admission.page();
        PageState current = visiblePage(activeBySection.get(handle.sectionKey()));
        if (current != null) {
            if (current.handle != handle) {
                throw new IllegalStateException("another Page lifecycle already owns this section");
            }
            return current;
        }
        int pageSlot = acquirePageSlot();
        return new PageState(
                handle,
                pageSlot,
                Math.toIntExact(handle.lifecycleGeneration()),
                admission.geometryRevision(),
                admission.residentBrickMask(),
                admission.sourceSeedMask(),
                admission.signatures(),
                admission.naturalTemperatureC(),
                admission.firstExposedLocalY(),
                admission.dormantAir(),
                admission.dormantMaterials());
    }

    PageState stageReplacement(PageState current, ThermalInputBatch.PageAdmission admission) {
        requireOpen();
        ThermalPageHandle handle = admission.page();
        if (current == null
                || current.handle.sectionKey() != handle.sectionKey()
                || current.handle == handle
                || visiblePage(activeBySection.get(handle.sectionKey())) != current
                || visiblePage(activeBySlot.get(current.pageSlot)) != current) {
            throw new IllegalStateException(
                    "Page replacement no longer owns the current lifecycle");
        }
        return new PageState(
                handle,
                current.pageSlot,
                Math.toIntExact(handle.lifecycleGeneration()),
                admission.geometryRevision(),
                admission.residentBrickMask(),
                admission.sourceSeedMask(),
                admission.signatures(),
                admission.naturalTemperatureC(),
                admission.firstExposedLocalY(),
                admission.dormantAir(),
                admission.dormantMaterials());
    }

    void releaseStagedAdmission(PageState state) {
        if (visiblePage(activeBySection.get(state.handle.sectionKey())) != state) {
            releasePageSlot(state.pageSlot);
        }
    }

    private int acquirePageSlot() {
        return freePageSlots.isEmpty()
                ? pageSlotHighWater++
                : freePageSlots.removeInt(freePageSlots.size() - 1);
    }

    private void releasePageSlot(int pageSlot) {
        freePageSlots.add(pageSlot);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("worker Page store is closed");
        }
    }

    @Override
    public void close() {
        releaseMaintenanceFrames(); maintenance.clear(); maintenanceReady.clear(); maintenanceTargets.clear();
        routeSupport.clear(); maintenanceCursor = null; maintenancePage = null;
        supportOwners.clear(); supportCounts.clear(); supportPreparations.clear(); regionalSupport.clear();
        if (closed) {
            return;
        }
        closed = true;
        for (PageState stored : activeBySection.values()) {
            PageState page = visiblePage(stored); if (page == null) continue;
            page.handle.publish(PagePublication.EMPTY);
        }
        activeBySection.clear();
        halos.clear();
        haloSignatures.clear();
        haloReferences.clear();
        haloSectionPositions.clear(); haloSectionVersions.clear();
        activeBySlot.clear();
        freePageSlots.clear();
        desiredBySection.clear();
        desiredScratch.clear();
        residencyIdentityChanges.clear();
    }

    public static final class PageState {
        public int workerPageSlot() { return pageSlot; }
        public final ThermalPageHandle handle;
        private com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken installToken;
        private PageState previousState;
        private boolean retiring;
        final int pageSlot;
        final int lifecycleGeneration;
        final WorkerBrickTopology[] bricks =
                new WorkerBrickTopology[ThermalPageHandle.BASE_BRICK_COUNT];
        final byte[] firstExposedLocalY;
        PageSignatures signatures;
        PagePublication publication = PagePublication.EMPTY;
        long geometryRevision;
        long topologyGeneration;
        long resolvedBrickMask;
        long residentBrickMask;
        long sourceSeedMask;
        double naturalTemperatureC;
        ThermalInputBatch.DormantAirCut dormantAir;
        ThermalInputBatch.DormantMaterialCut dormantMaterials;

        private PageState(
                ThermalPageHandle handle,
                int pageSlot,
                int lifecycleGeneration,
                long geometryRevision,
                long residentBrickMask,
                long sourceSeedMask,
                PageSignatures signatures,
                double naturalTemperatureC,
                byte[] firstExposedLocalY,
                ThermalInputBatch.DormantAirCut dormantAir,
                ThermalInputBatch.DormantMaterialCut dormantMaterials) {
            this.handle = Objects.requireNonNull(handle, "handle");
            this.pageSlot = pageSlot;
            this.lifecycleGeneration = lifecycleGeneration;
            this.geometryRevision = geometryRevision;
            this.residentBrickMask = residentBrickMask;
            this.sourceSeedMask = sourceSeedMask;
            this.signatures = Objects.requireNonNull(signatures, "signatures");
            this.naturalTemperatureC = naturalTemperatureC;
            this.firstExposedLocalY = firstExposedLocalY;
            this.dormantAir = dormantAir;
            this.dormantMaterials = dormantMaterials;
            Arrays.fill(bricks, WorkerBrickTopology.EMPTY);
        }

        int fragmentIndex(int brickIndex) {
            return Math.addExact(
                    Math.multiplyExact(pageSlot, ThermalPageHandle.BASE_BRICK_COUNT), brickIndex);
        }

        WorkerBrickTopology brick(int brickIndex) {
            return bricks[brickIndex];
        }

        /** Identity/environment shell for numerical geometry; cell lookup belongs to its view. */
        static PageState geometryMetadata(PageState source, PageSignatures signatures, long revision, long resident) {
            return new PageState(source.handle, source.pageSlot, source.lifecycleGeneration, revision,
                    resident, source.sourceSeedMask, signatures, source.naturalTemperatureC,
                    source.firstExposedLocalY.clone(), null, null);
        }
    }
}
