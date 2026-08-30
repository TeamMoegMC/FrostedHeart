/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.MissingPortPolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.Port;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.PortKind;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.SectionPos;

import java.util.Arrays;

/** Worker descriptor-to-topology binding resolver with section-local invalidation. */
final class WorkerPhysicalSourceBindings
        implements ThermalSourceLedger.EventObserver {
    private final WorkerPageStore pages;
    private final ThermalSignatureCatalog signatures;
    private final Long2ObjectOpenHashMap<SourceDescriptor> sources =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> sourcesBySection =
            new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet dirtySources = new LongOpenHashSet();
    private final LongArrayList dirtyOrder = new LongArrayList();

    WorkerPhysicalSourceBindings(
            WorkerPageStore pages,
            ThermalSignatureCatalog signatures
    ) {
        this.pages = pages;
        this.signatures = signatures;
    }

    void markCommittedSections(long[] sectionKeys) {
        for (long sectionKey : sectionKeys) {
            LongOpenHashSet affected = sourcesBySection.get(sectionKey);
            if (affected != null) {
                for (long sourceId : affected) {
                    markDirty(sourceId);
                }
            }
        }
    }

    void rebindDirty(ThermalSourceLedger ledger) {
        int count = dirtyOrder.size();
        Arrays.sort(dirtyOrder.elements(), 0, count);
        long[] order = dirtyOrder.elements();
        int retained = 0;
        for (int index = 0; index < count; index++) {
            long sourceId = order[index];
            if (!dirtySources.contains(sourceId)
                    || retained != 0 && order[retained - 1] == sourceId) {
                continue;
            }
            SourceDescriptor source = sources.get(sourceId);
            if (source == null || rebind(source, ledger)) {
                dirtySources.remove(sourceId);
            } else {
                order[retained++] = sourceId;
            }
        }
        dirtyOrder.size(retained);
    }

    @Override
    public void afterEvent(
            ThermalSourceBatch batch,
            int eventIndex,
            ThermalSourceLedger ledger
    ) {
        ThermalSourceBatch.Kind kind = batch.kind(eventIndex);
        long sourceId = batch.sourceId(eventIndex);
        int lifecycleGeneration = batch.lifecycleGeneration(eventIndex);
        if (kind == ThermalSourceBatch.Kind.REGISTER) {
            SourceDescriptor previous = sources.remove(sourceId);
            if (previous != null) {
                unindex(previous);
            }
            SourceDescriptor next = new SourceDescriptor(
                    sourceId,
                    lifecycleGeneration,
                    batch.anchorX(eventIndex),
                    batch.anchorY(eventIndex),
                    batch.anchorZ(eventIndex),
                    MinecraftPhysicalSourceProfile.byId(
                            batch.profileId(eventIndex)));
            sources.put(sourceId, next);
            index(next);
            markDirty(sourceId);
        } else if (kind == ThermalSourceBatch.Kind.UNLOAD) {
            SourceDescriptor previous = sources.get(sourceId);
            if (previous != null
                    && previous.lifecycleGeneration == lifecycleGeneration) {
                sources.remove(sourceId);
                unindex(previous);
                dirtySources.remove(sourceId);
            }
            return;
        }
        SourceDescriptor source = sources.get(sourceId);
        if (source != null
                && source.lifecycleGeneration == lifecycleGeneration
                && dirtySources.contains(sourceId)
                && rebind(source, ledger)) {
            dirtySources.remove(sourceId);
        }
    }

    private void markDirty(long sourceId) {
        if (dirtySources.add(sourceId)) {
            dirtyOrder.add(sourceId);
        }
    }

    private boolean rebind(
            SourceDescriptor source,
            ThermalSourceLedger ledger
    ) {
        resolve(source);
        for (int index = 0; index < source.profile.portCount(); index++) {
            if (!ledger.rebindAtCursor(
                    source.sourceId,
                    source.lifecycleGeneration,
                    source.profile.port(index).portId(),
                    source.bindings[index])) {
                return false;
            }
        }
        return true;
    }

    private void resolve(SourceDescriptor source) {
        Arrays.fill(source.bindings, null);
        for (int index = 0; index < source.profile.portCount(); index++) {
            Port port = source.profile.port(index);
            if (port.kind() == PortKind.INTERNAL_HEAT) {
                source.bindings[index] = SourceBinding.internalReservoir(
                        sinkId(source.sourceId, port));
            } else if (port.kind() == PortKind.RADIATION_LOSS) {
                source.bindings[index] = SourceBinding.declaredLoss(
                        sinkId(source.sourceId, port));
            } else {
                int blockX = source.anchorX + port.offsetX();
                int blockY = source.anchorY + port.offsetY();
                int blockZ = source.anchorZ + port.offsetZ();
                int slot = pages.resolveAirFaceSlot(
                        blockX,
                        blockY,
                        blockZ,
                        port.targetFace(),
                        signatures);
                if (slot >= 0) {
                    source.bindings[index] = SourceBinding.thermalNode(
                            slot,
                            pages.lifecycleGenerationAt(
                                    blockX, blockY, blockZ));
                } else if (slot == WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE) {
                    source.bindings[index] = SourceBinding.degradedLoss(
                            sinkId(source.sourceId, port));
                }
            }
        }
        for (int index = 0; index < source.profile.portCount(); index++) {
            if (source.bindings[index] != null) {
                continue;
            }
            Port port = source.profile.port(index);
            MissingPortPolicy policy = source.profile.missingPortPolicy();
            source.bindings[index] =
                    policy == MissingPortPolicy.INTERNAL_HEAT
                            ? SourceBinding.internalReservoir(
                                    sinkId(source.sourceId, port))
                            : SourceBinding.declaredLoss(
                                    sinkId(source.sourceId, port));
        }
    }

    static EmissionPort[] initialPorts(
            long sourceId,
            MinecraftPhysicalSourceProfile profile
    ) {
        EmissionPort[] result = new EmissionPort[profile.portCount()];
        for (int index = 0; index < result.length; index++) {
            Port port = profile.port(index);
            SourceBinding binding = switch (port.kind()) {
                case AIR_FACE -> SourceBinding.degradedLoss(
                        sinkId(sourceId, port));
                case INTERNAL_HEAT -> SourceBinding.internalReservoir(
                        sinkId(sourceId, port));
                case RADIATION_LOSS -> SourceBinding.declaredLoss(
                        sinkId(sourceId, port));
            };
            result[index] = EmissionPort.of(
                    port.portId(), port.powerShare(), binding);
        }
        return result;
    }

    private void index(SourceDescriptor source) {
        for (int index = 0; index < source.profile.portCount(); index++) {
            Port port = source.profile.port(index);
            if (port.kind() != PortKind.AIR_FACE) {
                continue;
            }
            long sectionKey = sectionKey(
                    source.anchorX + port.offsetX(),
                    source.anchorY + port.offsetY(),
                    source.anchorZ + port.offsetZ());
            sourcesBySection.computeIfAbsent(
                    sectionKey, ignored -> new LongOpenHashSet())
                    .add(source.sourceId);
        }
    }

    private void unindex(SourceDescriptor source) {
        for (int index = 0; index < source.profile.portCount(); index++) {
            Port port = source.profile.port(index);
            if (port.kind() != PortKind.AIR_FACE) {
                continue;
            }
            long sectionKey = sectionKey(
                    source.anchorX + port.offsetX(),
                    source.anchorY + port.offsetY(),
                    source.anchorZ + port.offsetZ());
            LongOpenHashSet indexed = sourcesBySection.get(sectionKey);
            if (indexed != null) {
                indexed.remove(source.sourceId);
                if (indexed.isEmpty()) {
                    sourcesBySection.remove(sectionKey);
                }
            }
        }
    }

    private static long sectionKey(int x, int y, int z) {
        return SectionPos.asLong(
                SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(y),
                SectionPos.blockToSectionCoord(z));
    }

    private static long sinkId(long sourceId, Port port) {
        return Long.rotateLeft(sourceId, 17)
                ^ Integer.toUnsignedLong(port.portId() + 1);
    }

    private static final class SourceDescriptor {
        private final long sourceId;
        private final int lifecycleGeneration;
        private final int anchorX;
        private final int anchorY;
        private final int anchorZ;
        private final MinecraftPhysicalSourceProfile profile;
        private final SourceBinding[] bindings;

        private SourceDescriptor(
                long sourceId,
                int lifecycleGeneration,
                int anchorX,
                int anchorY,
                int anchorZ,
                MinecraftPhysicalSourceProfile profile
        ) {
            this.sourceId = sourceId;
            this.lifecycleGeneration = lifecycleGeneration;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.profile = profile;
            bindings = new SourceBinding[profile.portCount()];
        }
    }
}
