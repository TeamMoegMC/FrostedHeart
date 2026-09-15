/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import net.minecraft.core.SectionPos;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

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
public final class WorkerPageStore implements AutoCloseable {
    private final java.util.IdentityHashMap<ThermalPageHandle, com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo> halos = new java.util.IdentityHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap haloSignatures = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap haloReferences = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private ThermalSignatureTable signatureTable;
    void signatures(ThermalSignatureTable signatures) { signatureTable = signatures; }

    void replaceHalo(ThermalPageHandle owner, com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo halo) {
        removeHalo(owner);
        halos.put(owner, halo);
        for (int index = 0; index < halo.size(); index++) {
            long position = halo.positions()[index];
            haloSignatures.put(position, halo.signatures()[index]);
            haloReferences.addTo(position, 1);
        }
    }

    void removeHalo(ThermalPageHandle owner) {
        var previous = halos.remove(owner);
        if (previous == null) return;
        for (long position : previous.positions()) {
            if (haloReferences.addTo(position, -1) == 1) {
                haloReferences.remove(position);
                haloSignatures.remove(position);
            }
        }
    }

    int haloSignatureAt(int x, int y, int z) {
        long position = net.minecraft.core.BlockPos.asLong(x, y, z);
        return haloReferences.containsKey(position) ? haloSignatures.get(position) : ThermalSignatureTable.UNRESOLVED;
    }
    private AirRouteCompiler airRoutes;
    void airRoutes(AirRouteCompiler compiler) { airRoutes = compiler; }

    boolean hasThermalInterestAt(long position) {
        int x = net.minecraft.core.BlockPos.getX(position), y = net.minecraft.core.BlockPos.getY(position), z = net.minecraft.core.BlockPos.getZ(position);
        PageState page = find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        if (page == null) return false;
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        return ((page.sourceSeedMask | hotMaskScratch.hotMask(page.pageSlot)) & 1L << brick) != 0;
    }

    public boolean hasResidentBrick(long position) {
        int x = net.minecraft.core.BlockPos.getX(position), y = net.minecraft.core.BlockPos.getY(position), z = net.minecraft.core.BlockPos.getZ(position);
        PageState page = find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        return page != null && (page.residentBrickMask & 1L << brick) != 0;
    }
    static final int PORT_BLOCKED = -1;
    public static final int PORT_TOPOLOGY_UNAVAILABLE = -2;
    private final Long2ObjectOpenHashMap<PageState> activeBySection;
    private final Int2ObjectOpenHashMap<PageState> activeBySlot;
    private final IntArrayList freePageSlots;
    private final QueryPublication.HotMaskScratch hotMaskScratch;
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
        hotMaskScratch = new QueryPublication.HotMaskScratch(maximumPages);
    }

    PageState find(long sectionKey) {
        return activeBySection.get(sectionKey);
    }

    PageState find(ThermalPageHandle handle) {
        PageState state = activeBySection.get(handle.sectionKey());
        return state != null && state.handle == handle ? state : null;
    }

    PageState findPageSlot(int pageSlot) {
        return activeBySlot.get(pageSlot);
    }

    public int materialSlot(ThermalPageHandle handle, int position, int expectedStateId) {
        PageState page = find(handle);
        if (page == null || signatureTable.materialStateId(page.signatures.get(position)) != expectedStateId) return -1;
        int x = position & 15, y = position >>> 8 & 15, z = position >>> 4 & 15;
        var brick = page.brick((x >>> 2) | (z >>> 2) << 2 | (y >>> 2) << 4);
        return brick.cellsResolved ? brick.slotAt((x & 3) | (z & 3) << 2 | (y & 3) << 4) : -1;
    }

    public void awaitChangedMaterials(ThermalInputBatch batch, ThermalCellArena arena) {
        var changes = batch.geometry().materialChanges();
        for (int index = 0; index < changes.size(); index++) {
            int previousState = signatureTable.materialStateId(changes.previousSignature(index));
            int slot = materialSlot(changes.page(index), changes.blockIndex(index), previousState);
            if (slot >= 0) arena.awaitMaterialLayout(slot);
        }
        for (int index = 0; index < batch.geometry().size(); index++) {
            if (batch.geometry().geometryResyncReason(index) != ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED) continue;
            PageState page = find(batch.geometry().page(index));
            if (page == null) continue;
            for (WorkerBrickTopology brick : page.bricks) {
                for (int slot = brick.span.firstSlot(); slot < brick.span.endSlotExclusive(); slot++) arena.awaitMaterialLayout(slot);
            }
        }
    }

    int activePageCount() {
        return activeBySection.size();
    }

    int pageSlotCapacity() {
        return pageSlotHighWater;
    }

    public QueryPublication.HotMaskScratch hotMaskScratch() {
        return hotMaskScratch;
    }

    public void configureHotMaskThresholds(
            double refineHighC,
            double releaseLowC
    ) {
        hotMaskScratch.configure(refineHighC, releaseLowC);
    }

    public ThermalCompletion.BrickResidency[] collectResidencyChanges(
            ThermalCellArena arena,
            double referenceTemperatureC,
            double refineHighC,
            double releaseLowC
    ) {
        requireOpen();
        desiredScratch.clear();
        for (PageState page : activeBySection.values()) {
            long hotBrickMask = hotMaskScratch.hotMask(page.pageSlot)
                    & page.residentBrickMask;
            long active = page.sourceSeedMask | hotBrickMask;
            if (active == 0L) {
                continue;
            }
            orDesired(page.handle.sectionKey(), page.residentBrickMask);
            collectInternalFaces(page, active, BlockFace.NEGATIVE_X,
                    X_MIN, page.residentBrickMask << 1, -1,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
            collectInternalFaces(page, active, BlockFace.POSITIVE_X,
                    X_MAX, page.residentBrickMask >>> 1, 1,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
            collectInternalFaces(page, active, BlockFace.NEGATIVE_Z,
                    Z_MIN, page.residentBrickMask << 4, -4,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
            collectInternalFaces(page, active, BlockFace.POSITIVE_Z,
                    Z_MAX, page.residentBrickMask >>> 4, 4,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
            collectMaterialFrontier(page, active);
            collectInternalFaces(page, active, BlockFace.NEGATIVE_Y,
                    Y_MIN, page.residentBrickMask << 16, -16,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
            collectInternalFaces(page, active, BlockFace.POSITIVE_Y,
                    Y_MAX, page.residentBrickMask >>> 16, 16,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
        }
        if (airRoutes != null) airRoutes.collectRequiredBricks(this, desiredScratch);
        ThermalCompletion.BrickResidency[] result = finishResidencyChanges();
        return result;
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
                int x = net.minecraft.core.BlockPos.getX(position), y = net.minecraft.core.BlockPos.getY(position), z = net.minecraft.core.BlockPos.getZ(position);
                int contacts = signatureTable.fullContactFaces(page.signatures.get(BlockBrickLayout.pageBlock(brick, block)));
                for (int face = 0; face < 6; face++) {
                    if ((contacts & 1 << face) == 0) continue;
                    int nx = x + TopologyView.DX[face], ny = y + TopologyView.DY[face], nz = z + TopologyView.DZ[face];
                    long section = SectionPos.asLong(nx >> 4, ny >> 4, nz >> 4);
                    int neighborBrick = (nx & 15) >>> 2 | ((nz & 15) >>> 2) << 2 | ((ny & 15) >>> 2) << 4;
                    PageState neighbor = find(section);
                    // A cold resident neighbor still needs the owner's incoming request.
                    int signature = neighbor != null && (neighbor.residentBrickMask & 1L << neighborBrick) != 0
                            ? neighbor.signatures.get((nx & 15) | (nz & 15) << 4 | (ny & 15) << 8)
                            : haloSignatureAt(nx, ny, nz);
                    if (signatureTable.materialProfileId(signature) != 0
                            && (signatureTable.fullContactFaces(signature) & 1 << (face ^ 1)) != 0) {
                        orDesired(section, 1L << neighborBrick);
                    }
                }
            }
        }
    }

    private void collectInternalFaces(
            PageState page,
            long active,
            BlockFace face,
            long boundaryMask,
            long alignedResident,
            int internalOffset,
            ThermalCellArena arena,
            double referenceTemperatureC,
            double refineHighC,
            double releaseLowC
    ) {
        long internal = active & ~boundaryMask & ~alignedResident;
        while (internal != 0L) {
            int ownerBrick = Long.numberOfTrailingZeros(internal);
            int targetBrick = ownerBrick + internalOffset;
            requestFace(
                    page, ownerBrick, face,
                    page.handle.sectionKey(), targetBrick,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
            internal &= internal - 1L;
        }
        long boundary = active & boundaryMask;
        long targetSection = neighborSection(page.handle.sectionKey(), face);
        while (boundary != 0L) {
            int ownerBrick = Long.numberOfTrailingZeros(boundary);
            int targetBrick = boundaryTargetBrick(ownerBrick, face);
            requestFace(
                    page, ownerBrick, face, targetSection, targetBrick,
                    arena, referenceTemperatureC, refineHighC, releaseLowC);
            boundary &= boundary - 1L;
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
            double releaseLowC
    ) {
        long targetBit = 1L << targetBrick;
        double threshold = (desiredBySection.get(targetSection) & targetBit) != 0L
                ? releaseLowC : refineHighC;
        if (faceResidualC(
                page, ownerBrick, face, arena, referenceTemperatureC)
                >= threshold) {
            orDesired(targetSection, targetBit);
        }
    }

    private static double faceResidualC(
            PageState page,
            int brick,
            BlockFace face,
            ThermalCellArena arena,
            double referenceTemperatureC
    ) {
        WorkerBrickTopology topology = page.brick(brick);
        if (!topology.resolved || topology.coverageSlot < 0) {
            return 0.0D;
        }
        if (topology.blockLayout == null) {
            if (face == BlockFace.POSITIVE_Y
                    && allTopColumnsDirectSky(page, brick)) {
                return 0.0D;
            }
            return Math.abs(
                    arena.temperatureC(topology.coverageSlot, referenceTemperatureC)
                            - page.naturalTemperatureC);
        }
        double residual = 0.0D;
        long seen=0;
        int axis=face.ordinal()/2, side=(face.ordinal()&1)==0?0:3;
        for (int i=0;i<16;i++) {
            if (face==BlockFace.POSITIVE_Y && topPortDirectSky(page,brick,i)) continue;
            int node=topology.blockLayout.transportAt(BlockBrickLayout.faceBlock(axis,side,i));
            if(node<0 || (seen & 1L<<node)!=0) continue;
            seen|=1L<<node;
            int slot=topology.coverageSlot+node;
            residual=Math.max(residual,Math.abs(arena.temperatureC(slot,referenceTemperatureC)-page.naturalTemperatureC));
        }
        return residual;
    }

    private static boolean allTopColumnsDirectSky(PageState page, int brick) {
        if ((brick >>> 4 & 3) != 3) {
            return false;
        }
        int firstX = (brick & 3) << 2;
        int firstZ = (brick >>> 2 & 3) << 2;
        for (int z = firstZ; z < firstZ + 4; z++) {
            for (int x = firstX; x < firstX + 4; x++) {
                if (Byte.toUnsignedInt(page.firstExposedLocalY[x | z << 4])
                        > 15) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean topPortDirectSky(
            PageState page,
            int brick,
            int blockInBrick
    ) {
        if ((brick >>> 4 & 3) != 3) {
            return false;
        }
        int x = ((brick & 3) << 2) + (blockInBrick & 3);
        int z = ((brick >>> 2 & 3) << 2)
                + (blockInBrick >>> 2 & 3);
        return Byte.toUnsignedInt(page.firstExposedLocalY[x | z << 4]) <= 15;
    }

    private void orDesired(long sectionKey, long mask) {
        desiredScratch.put(sectionKey, desiredScratch.get(sectionKey) | mask);
    }

    private ThermalCompletion.BrickResidency[] finishResidencyChanges() {
        int count = 0;
        for (Long2LongMap.Entry entry : desiredScratch.long2LongEntrySet()) {
            if (desiredBySection.get(entry.getLongKey()) != entry.getLongValue()) {
                count++;
            }
        }
        for (Long2LongMap.Entry entry : desiredBySection.long2LongEntrySet()) {
            if (entry.getLongValue() != 0L
                    && !desiredScratch.containsKey(entry.getLongKey())) {
                count++;
            }
        }
        for (int index = 0; index < residencyIdentityChanges.size(); index++) {
            long sectionKey = residencyIdentityChanges.getLong(index);
            if (desiredBySection.get(sectionKey)
                    == desiredScratch.get(sectionKey)) {
                count++;
            }
        }
        if (count == 0) {
            Long2LongOpenHashMap previous = desiredBySection;
            desiredBySection = desiredScratch;
            desiredScratch = previous;
            return ThermalCompletion.NO_RESIDENCY_UPDATES;
        }
        long[] keys = new long[count];
        int write = 0;
        for (Long2LongMap.Entry entry : desiredScratch.long2LongEntrySet()) {
            if (desiredBySection.get(entry.getLongKey()) != entry.getLongValue()) {
                keys[write++] = entry.getLongKey();
            }
        }
        for (Long2LongMap.Entry entry : desiredBySection.long2LongEntrySet()) {
            if (entry.getLongValue() != 0L
                    && !desiredScratch.containsKey(entry.getLongKey())) {
                keys[write++] = entry.getLongKey();
            }
        }
        for (int index = 0; index < residencyIdentityChanges.size(); index++) {
            long sectionKey = residencyIdentityChanges.getLong(index);
            if (desiredBySection.get(sectionKey)
                    == desiredScratch.get(sectionKey)) {
                keys[write++] = sectionKey;
            }
        }
        Arrays.sort(keys);
        ThermalCompletion.BrickResidency[] updates =
                new ThermalCompletion.BrickResidency[count];
        for (int index = 0; index < count; index++) {
            long sectionKey = keys[index];
            PageState page = activeBySection.get(sectionKey);
            updates[index] = new ThermalCompletion.BrickResidency(
                    sectionKey,
                    page == null ? -1L : page.handle.lifecycleGeneration(),
                    desiredScratch.get(sectionKey));
        }
        Long2LongOpenHashMap previous = desiredBySection;
        desiredBySection = desiredScratch;
        desiredScratch = previous;
        residencyIdentityChanges.clear();
        return updates;
    }

    private static long neighborSection(
            long sectionKey,
            BlockFace face
    ) {
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

    private static int boundaryTargetBrick(
            int ownerBrick,
            BlockFace face
    ) {
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
            ArrayList<TopologyPlan.PageDraft> drafts,
            int draftCount,
            ArrayList<ThermalPageHandle.GeometryResyncToken> resyncTokens
    ) {
        if (draftCount == 0) {
            return PreparedTopologyChange.NO_PAGE_WRITES;
        }
        PreparedTopologyChange.PageWrite[] writes =
                new PreparedTopologyChange.PageWrite[draftCount];
        int admissionCount = 0;
        for (int index = 0; index < draftCount; index++) {
            TopologyPlan.PageDraft draft = drafts.get(index);
            if (draft.retirement) {
                writes[index] = PreparedTopologyChange.PageWrite.retirement(
                        draft);
                continue;
            }
            if (draft.admission) {
                admissionCount++;
            }
            int replacementCount = Long.bitCount(draft.replacementMask);
            int[] brickIndexes = replacementCount == 0
                    ? PreparedTopologyChange.NO_INTS
                    : new int[replacementCount];
            WorkerBrickTopology[] bricks = replacementCount == 0
                    ? PreparedTopologyChange.NO_BRICKS
                    : new WorkerBrickTopology[replacementCount];
            boolean structural = draft.fragmentChangedMask != 0L
                    || draft.cellReplacementMask != 0L
                    || draft.admission;
            long publicationChanges = draft.replacementMask
                    | draft.signatureChangedMask;
            boolean publicationChanged = structural
                    || publicationChanges != 0L
                    || draft.geometryRevision != draft.page.geometryRevision;
            PagePublication previousPublication = draft.admission
                    ? PagePublication.EMPTY
                    : draft.page.publication;
            PagePublication.Brick[] publicationBricks = null;
            long resolvedMask = draft.admission
                    ? 0L : draft.page.resolvedBrickMask;
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
                    publicationBricks[brick] = publicationBrick(
                            topology,
                            draft.nextSignatures.brickPayload(brick));
                }
                publicationChanges &= publicationChanges - 1L;
            }
            long topologyGeneration = structural
                    ? Math.incrementExact(draft.page.topologyGeneration)
                    : draft.page.topologyGeneration;
            PagePublication publication = previousPublication;
            if (publicationChanged) {
                publication = publicationBricks == null
                        ? previousPublication.withIdentities(
                                 draft.geometryRevision,
                                 topologyGeneration)
                        : PagePublication.owned(
                                draft.page.pageSlot,
                                draft.geometryRevision,
                                topologyGeneration,
                                publicationBricks);
            }
            writes[index] = PreparedTopologyChange.PageWrite.active(
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
            residencyIdentityChanges.ensureCapacity(Math.addExact(
                    residencyIdentityChanges.size(), admissionCount));
        }
        return writes;
    }

    private static boolean queryMetadataChanged(
            WorkerBrickTopology previous,
            WorkerBrickTopology next
    ) {
        return previous.resolved != next.resolved
                || previous.coverageSlot != next.coverageSlot
                || previous.coverageGeneration != next.coverageGeneration
                || previous.blockLayout != next.blockLayout
                || previous.transportNodeCount != next.transportNodeCount;
    }

    private static PagePublication.Brick publicationBrick(
            WorkerBrickTopology topology,
            Object signaturePayload
    ) {
        return new PagePublication.Brick(
                topology.cellsResolved && topology.span.count() > 0 ? topology.span.firstSlot() : -1,
                topology.cellsResolved ? topology.coverageGeneration : 0,
                signaturePayload,
                topology.cellsResolved ? topology.blockLayout : null,
                topology.cellsResolved ? topology.transportNodeCount : 0,
                topology.cellsResolved);
    }

    public int resolveAirFaceSlot(
            int blockX,
            int blockY,
            int blockZ,
            BlockFace face,
            ThermalSignatureTable signatures
    ) {
        return resolveAirFaceTarget(blockX, blockY, blockZ, face, signatures, null);
    }

    public static final class MutableAirTarget {
        private int generation;
        public int generation() { return generation; }
    }

    public int resolveAirFaceTarget(int blockX, int blockY, int blockZ, BlockFace face,
            ThermalSignatureTable signatures, MutableAirTarget out) {
        long sectionKey = net.minecraft.core.SectionPos.asLong(
                net.minecraft.core.SectionPos.blockToSectionCoord(blockX),
                net.minecraft.core.SectionPos.blockToSectionCoord(blockY),
                net.minecraft.core.SectionPos.blockToSectionCoord(blockZ));
        PageState page = activeBySection.get(sectionKey);
        if (page == null) {
            return PORT_TOPOLOGY_UNAVAILABLE;
        }
        int localX = net.minecraft.core.SectionPos.sectionRelative(blockX);
        int localY = net.minecraft.core.SectionPos.sectionRelative(blockY);
        int localZ = net.minecraft.core.SectionPos.sectionRelative(blockZ);
        int pageBlock = localX | localZ << 4 | localY << 8;
        int brickIndex = (localX >>> 2)
                | (localZ >>> 2) << 2
                | (localY >>> 2) << 4;
        WorkerBrickTopology brick = page.brick(brickIndex);
        if (!brick.cellsResolved) {
            return PORT_TOPOLOGY_UNAVAILABLE;
        }
        int signatureId = page.signatures.get(pageBlock);
        int blockInBrick = localX & 3
                | (localZ & 3) << 2
                | (localY & 3) << 4;
        int slot = brick.transportSlot(blockInBrick);
        if (slot >= 0) {
            if (out != null) out.generation = page.lifecycleGeneration;
            return slot;
        }
        if (signatures.ventilation(signatureId) == 0) return PORT_BLOCKED;
        if (brick.blockLayout != null && brick.blockLayout.hasAirRoute(blockInBrick)) {
            long target = brick.blockLayout.routedAirBlock(blockInBrick);
            int x = net.minecraft.core.BlockPos.getX(target), y = net.minecraft.core.BlockPos.getY(target), z = net.minecraft.core.BlockPos.getZ(target);
            PageState targetPage = find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
            if (targetPage == null) return PORT_TOPOLOGY_UNAVAILABLE;
            int targetBrick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
            var topology = targetPage.brick(targetBrick);
            int targetBlock = (x & 3) | (z & 3) << 2 | (y & 3) << 4;
            slot = topology.cellsResolved ? topology.transportSlot(targetBlock) : -1;
            if (slot >= 0) {
                if (out != null) out.generation = targetPage.lifecycleGeneration;
                return slot;
            }
        }
        long position = net.minecraft.core.BlockPos.asLong(blockX, blockY, blockZ);
        return airRoutes != null && airRoutes.closed(position) ? PORT_BLOCKED : PORT_TOPOLOGY_UNAVAILABLE;
    }

    public int lifecycleGenerationAt(int blockX, int blockY, int blockZ) {
        PageState page = activeBySection.get(
                net.minecraft.core.SectionPos.asLong(
                        net.minecraft.core.SectionPos.blockToSectionCoord(blockX),
                        net.minecraft.core.SectionPos.blockToSectionCoord(blockY),
                        net.minecraft.core.SectionPos.blockToSectionCoord(blockZ)));
        if (page == null) {
            throw new IllegalStateException(
                    "resolved source target Page is no longer active");
        }
        return page.lifecycleGeneration;
    }



    PageState stageAdmission(ThermalInputBatch.PageAdmission admission) {
        requireOpen();
        ThermalPageHandle handle = admission.page();
        PageState current = activeBySection.get(handle.sectionKey());
        if (current != null) {
            if (current.handle != handle) {
                throw new IllegalStateException(
                        "another Page lifecycle already owns this section");
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
                admission.dormantAir(), admission.dormantMaterials());
    }

    PageState stageReplacement(
            PageState current,
            ThermalInputBatch.PageAdmission admission
    ) {
        requireOpen();
        ThermalPageHandle handle = admission.page();
        if (current == null
                || current.handle.sectionKey() != handle.sectionKey()
                || current.handle == handle
                || activeBySection.get(handle.sectionKey()) != current
                || activeBySlot.get(current.pageSlot) != current) {
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
                admission.dormantAir(), admission.dormantMaterials());
    }

    void releaseStagedAdmission(PageState state) {
        if (activeBySection.get(state.handle.sectionKey()) != state) {
            releasePageSlot(state.pageSlot);
        }
    }

    boolean canCommit(
            PageState state,
            PageState replacedPage,
            boolean admission
    ) {
        PageState sectionOwner = activeBySection.get(
                state.handle.sectionKey());
        PageState slotOwner = activeBySlot.get(state.pageSlot);
        if (replacedPage != null) {
            return admission
                    && sectionOwner == replacedPage
                    && slotOwner == replacedPage
                    && replacedPage.pageSlot == state.pageSlot;
        }
        return admission
                ? sectionOwner == null && slotOwner == null
                : sectionOwner == state && slotOwner == state;
    }

    void commitAdmission(PageState state) {
        PageState previous = activeBySection.get(state.handle.sectionKey());
        if (previous != null && previous.handle != state.handle) removeHalo(previous.handle);
        activeBySection.put(state.handle.sectionKey(), state);
        activeBySlot.put(state.pageSlot, state);
        residencyIdentityChanges.add(state.handle.sectionKey());
        hotMaskScratch.installPage(
                state.pageSlot, state.naturalTemperatureC);
        state.dormantAir = null;
        state.dormantMaterials = null;
    }

    void commitRetirement(PageState state) {
        if (activeBySection.get(state.handle.sectionKey()) != state
                || activeBySlot.get(state.pageSlot) != state) {
            return;
        }
        activeBySection.remove(state.handle.sectionKey());
        activeBySlot.remove(state.pageSlot);
        hotMaskScratch.removePage(state.pageSlot);
        releasePageSlot(state.pageSlot);
    }

    void installPageState(
            PageState state,
            PageSignatures signatures,
            long geometryRevision,
            long topologyGeneration,
            long resolvedBrickMask,
            long residentBrickMask,
            long sourceSeedMask,
            PagePublication publication
    ) {
        state.signatures = signatures;
        state.geometryRevision = geometryRevision;
        state.topologyGeneration = topologyGeneration;
        state.resolvedBrickMask = resolvedBrickMask;
        state.residentBrickMask = residentBrickMask;
        state.sourceSeedMask = sourceSeedMask;
        state.publication = publication;
    }

    void installBrick(
            PageState state,
            int brickIndex,
            WorkerBrickTopology topology
    ) {
        state.bricks[brickIndex] = topology;
    }

    void installNaturalTemperature(PageState state, double temperatureC) {
        state.naturalTemperatureC = temperatureC;
        hotMaskScratch.updateNaturalTemperature(state.pageSlot, temperatureC);
    }

    void installSkyColumn(PageState state, int column, byte exposedLocalY) {
        state.firstExposedLocalY[column] = exposedLocalY;
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
        if (closed) {
            return;
        }
        closed = true;
        for (PageState page : activeBySection.values()) {
            page.handle.publish(PagePublication.EMPTY);
        }
        activeBySection.clear();
        halos.clear();
        haloSignatures.clear();
        haloReferences.clear();
        activeBySlot.clear();
        freePageSlots.clear();
        desiredBySection.clear();
        desiredScratch.clear();
        residencyIdentityChanges.clear();
    }

    public static final class PageState {
        public final ThermalPageHandle handle;
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
                ThermalInputBatch.DormantMaterialCut dormantMaterials
        ) {
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
                    Math.multiplyExact(
                            pageSlot, ThermalPageHandle.BASE_BRICK_COUNT),
                    brickIndex);
        }

        WorkerBrickTopology brick(int brickIndex) {
            return bricks[brickIndex];
        }

    }
}
