/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Objects;

/** Worker-only Page identity, slot, scalar, and 64-Brick committed authority. */
final class WorkerPageStore implements AutoCloseable {
    static final int PORT_BLOCKED = -1;
    static final int PORT_TOPOLOGY_UNAVAILABLE = -2;
    private final Long2ObjectOpenHashMap<PageState> activeBySection;
    private final Int2ObjectOpenHashMap<PageState> activeBySlot;
    private final IntArrayList freePageSlots;
    private int pageSlotHighWater;
    private int unresolvedPageCount;
    private boolean closed;

    WorkerPageStore(int maximumPages) {
        if (maximumPages <= 0) {
            throw new IllegalArgumentException("maximumPages must be positive");
        }
        activeBySection = new Long2ObjectOpenHashMap<>(maximumPages);
        activeBySlot = new Int2ObjectOpenHashMap<>(maximumPages);
        freePageSlots = new IntArrayList(maximumPages);
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

    int activePageCount() {
        return activeBySection.size();
    }

    int pageSlotCapacity() {
        return pageSlotHighWater;
    }

    boolean topologyResolved() {
        return unresolvedPageCount == 0;
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
        for (int index = 0; index < draftCount; index++) {
            TopologyPlan.PageDraft draft = drafts.get(index);
            if (draft.retirement) {
                writes[index] = draft.retirementWrite();
                continue;
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
            PagePublication.Brick[] publicationBricks = publicationChanged
                    ? (draft.admission
                            ? PagePublication.EMPTY.copyBricks()
                            : draft.page.publication.copyBricks())
                    : null;
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
                    publicationBricks[brick] = publicationBrick(
                            topology,
                            draft.nextSignatures.brickPayload(brick));
                }
                publicationChanges &= publicationChanges - 1L;
            }
            long topologyGeneration = structural
                    ? Math.incrementExact(draft.page.topologyGeneration)
                    : draft.page.topologyGeneration;
            PagePublication publication = publicationChanged
                    ? PagePublication.owned(
                            draft.geometryRevision,
                            topologyGeneration,
                            publicationBricks)
                    : draft.page.publication;
            byte continuationFaceMask = 0;
            for (int brick = 0;
                 brick < ThermalPageHandle.BASE_BRICK_COUNT;
                 brick++) {
                WorkerBrickTopology topology = draft.replacements[brick];
                continuationFaceMask |= (topology == null
                        ? draft.page.brick(brick)
                        : topology).continuationFaceMask;
            }
            writes[index] = new PreparedTopologyChange.PageWrite(
                    draft.page,
                    draft.admission,
                    false,
                    draft.nextSignatures,
                    resolvedMask,
                    publication,
                    continuationFaceMask,
                    draft.cellReplacementMask,
                    brickIndexes,
                    bricks,
                    draft.naturalTemperatureChanged,
                    draft.naturalTemperatureC,
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
        return writes;
    }

    private static boolean queryMetadataChanged(
            WorkerBrickTopology previous,
            WorkerBrickTopology next
    ) {
        return previous.resolved != next.resolved
                || previous.coverageSlot != next.coverageSlot
                || previous.coverageGeneration != next.coverageGeneration
                || previous.mixedGeometry != next.mixedGeometry
                || previous.phaseCandidates != next.phaseCandidates;
    }

    private static PagePublication.Brick publicationBrick(
            WorkerBrickTopology topology,
            Object signaturePayload
    ) {
        return new PagePublication.Brick(
                topology.resolved ? topology.coverageSlot : -1,
                topology.resolved ? topology.coverageGeneration : 0,
                signaturePayload,
                topology.resolved ? topology.mixedGeometry : null,
                topology.resolved
                        ? topology.phaseCandidates
                        : PagePublication.PhaseCandidates.EMPTY);
    }

    int resolveAirFaceSlot(
            int blockX,
            int blockY,
            int blockZ,
            ConservativeAirGeometry.Face face,
            ThermalSignatureCatalog signatures
    ) {
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
        if (!brick.resolved) {
            return PORT_TOPOLOGY_UNAVAILABLE;
        }
        int signatureId = page.signatures.get(pageBlock);
        int blockInBrick = localX & 3
                | (localZ & 3) << 2
                | (localY & 3) << 4;
        int firstSlot = -1;
        int secondSlot = -1;
        for (int vertical = 0; vertical < 4; vertical++) {
            for (int horizontal = 0; horizontal < 4; horizontal++) {
                int microcell = faceMicrocell(face, horizontal, vertical);
                int region = signatures.componentOrdinal(
                        signatureId, microcell);
                if (region == 0xff || brick.coverageSlot < 0) {
                    continue;
                }
                int slot;
                if (brick.mixedGeometry == null) {
                    slot = brick.coverageSlot;
                } else {
                    int component = brick.mixedGeometry.compiledComponentAt(
                            blockInBrick, region);
                    if (component < 0) {
                        continue;
                    }
                    slot = brick.coverageSlot + component;
                }
                if (slot == firstSlot || slot == secondSlot) {
                    continue;
                }
                if (firstSlot < 0) {
                    firstSlot = slot;
                } else if (secondSlot < 0) {
                    secondSlot = slot;
                } else {
                    return PORT_TOPOLOGY_UNAVAILABLE;
                }
            }
        }
        return firstSlot < 0
                ? PORT_BLOCKED
                : secondSlot < 0 ? firstSlot : PORT_TOPOLOGY_UNAVAILABLE;
    }

    int lifecycleGenerationAt(int blockX, int blockY, int blockZ) {
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

    private static int faceMicrocell(
            ConservativeAirGeometry.Face face,
            int horizontal,
            int vertical
    ) {
        return switch (face) {
            case NEGATIVE_X -> vertical << 4 | horizontal << 2;
            case POSITIVE_X -> vertical << 4 | horizontal << 2 | 3;
            case NEGATIVE_Y -> horizontal;
            case POSITIVE_Y -> 3 << 4 | horizontal;
            case NEGATIVE_Z -> vertical << 4 | horizontal;
            case POSITIVE_Z -> vertical << 4 | 3 << 2 | horizontal;
        };
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
                admission.signatures(),
                admission.naturalTemperatureC(),
                admission.firstExposedLocalY());
    }

    void releaseStagedAdmission(PageState state) {
        if (activeBySection.get(state.handle.sectionKey()) != state) {
            releasePageSlot(state.pageSlot);
        }
    }

    boolean canCommit(PageState state, boolean admission) {
        PageState sectionOwner = activeBySection.get(
                state.handle.sectionKey());
        PageState slotOwner = activeBySlot.get(state.pageSlot);
        return admission
                ? sectionOwner == null && slotOwner == null
                : sectionOwner == state && slotOwner == state;
    }

    void commitAdmission(PageState state) {
        activeBySection.put(state.handle.sectionKey(), state);
        activeBySlot.put(state.pageSlot, state);
        if (!state.resolved()) {
            unresolvedPageCount++;
        }
    }

    void commitRetirement(PageState state) {
        if (activeBySection.get(state.handle.sectionKey()) != state
                || activeBySlot.get(state.pageSlot) != state) {
            return;
        }
        activeBySection.remove(state.handle.sectionKey());
        activeBySlot.remove(state.pageSlot);
        if (!state.resolved()) {
            unresolvedPageCount--;
        }
        releasePageSlot(state.pageSlot);
    }

    void installPageState(
            PageState state,
            PageSignatures signatures,
            long geometryRevision,
            long topologyGeneration,
            long resolvedBrickMask,
            PagePublication publication
    ) {
        boolean wasResolved = state.resolved();
        boolean nextResolved = resolvedBrickMask == -1L;
        if (wasResolved != nextResolved) {
            unresolvedPageCount += nextResolved ? -1 : 1;
        }
        state.signatures = signatures;
        state.geometryRevision = geometryRevision;
        state.topologyGeneration = topologyGeneration;
        state.resolvedBrickMask = resolvedBrickMask;
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
        activeBySlot.clear();
        freePageSlots.clear();
        unresolvedPageCount = 0;
    }

    static final class PageState {
        final ThermalPageHandle handle;
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
        double naturalTemperatureC;

        private PageState(
                ThermalPageHandle handle,
                int pageSlot,
                int lifecycleGeneration,
                long geometryRevision,
                PageSignatures signatures,
                double naturalTemperatureC,
                byte[] firstExposedLocalY
        ) {
            this.handle = Objects.requireNonNull(handle, "handle");
            this.pageSlot = pageSlot;
            this.lifecycleGeneration = lifecycleGeneration;
            this.geometryRevision = geometryRevision;
            this.signatures = Objects.requireNonNull(signatures, "signatures");
            this.naturalTemperatureC = naturalTemperatureC;
            this.firstExposedLocalY = firstExposedLocalY;
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

        boolean resolved() {
            return resolvedBrickMask == -1L;
        }
    }
}
