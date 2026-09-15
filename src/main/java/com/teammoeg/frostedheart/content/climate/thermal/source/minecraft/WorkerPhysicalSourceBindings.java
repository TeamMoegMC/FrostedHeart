/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.Port;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.PortKind;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.Arrays;
import java.util.Objects;

/**
 * worker 侧 source descriptor 到已提交 Page topology 的绑定解析器。
 *
 * <p>拓扑提交只标脏受影响 section 的 source；重绑随后按 source ID 的确定
 * 顺序更新 ledger，不扫描无关 Page。</p>
 */
public final class WorkerPhysicalSourceBindings
        implements ThermalSourceLedger.EventObserver {
    private final WorkerPageStore pages;
    private final ThermalSignatureTable signatures;
    private final MinecraftPhysicalSourceProfile campfireProfile;
    private final Long2ObjectOpenHashMap<SourceDescriptor> sources =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> sourcesBySection =
            new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet dirtySources = new LongOpenHashSet();
    private final LongArrayList dirtyOrder = new LongArrayList();
    private final LongOpenHashSet mixingChanges = new LongOpenHashSet();
    private final WorkerPageStore.MutableAirTarget airTarget = new WorkerPageStore.MutableAirTarget();

    public WorkerPhysicalSourceBindings(
            WorkerPageStore pages,
            ThermalSignatureTable signatures,
            MinecraftPhysicalSourceProfile campfireProfile
    ) {
        this.pages = pages;
        this.signatures = signatures;
        this.campfireProfile = Objects.requireNonNull(
                campfireProfile, "campfireProfile");
    }

    public void markCommittedSections(long[] sectionKeys) {
        for (long sectionKey : sectionKeys) {
            LongOpenHashSet affected = sourcesBySection.get(sectionKey);
            if (affected != null) {
                for (long sourceId : affected) {
                    markDirty(sourceId);
                }
            }
        }
    }

    /** Pending world Brick positions survive a work-limited topology attempt. */
    public LongSet mixingChanges() { return mixingChanges; }

    public void mixingCommitted() { mixingChanges.clear(); }

    public void collectMixingSources(AirMixingRegion region) {
        if (sourcesBySection.isEmpty()) return;
        for (int x = region.minX() >> 4; x <= (region.maxX() >> 4); x++) {
            for (int y = region.minY() >> 4; y <= (region.maxY() >> 4); y++) {
                for (int z = region.minZ() >> 4; z <= (region.maxZ() >> 4); z++) {
                    long section = SectionPos.asLong(x, y, z);
                    LongOpenHashSet indexed = sourcesBySection.get(section);
                    if (indexed == null) continue;
                    var iterator = indexed.iterator();
                    while (iterator.hasNext()) {
                        SourceDescriptor source = sources.get(iterator.nextLong());
                        if (!source.emitting) continue;
                        for (int i = 0; i < source.profile.portCount(); i++) {
                            Port port = source.profile.port(i);
                            if (port.kind() != PortKind.AIR_FACE || port.powerShare() == 0) continue;
                            int px = source.anchorX + port.offsetX();
                            int py = source.anchorY + port.offsetY();
                            int pz = source.anchorZ + port.offsetZ();
                            // A source may be indexed in several Sections; visit each port once.
                            if (sectionKey(px, py, pz) == section) region.include(px, py, pz);
                        }
                    }
                }
            }
        }
    }

    private void markMixingChanged(SourceDescriptor source) {
        int radius = AirMixingRegion.RADIUS;
        for (int i = 0; i < source.profile.portCount(); i++) {
            Port port = source.profile.port(i);
            if (port.kind() != PortKind.AIR_FACE || port.powerShare() == 0) continue;
            int px = source.anchorX + port.offsetX();
            int py = source.anchorY + port.offsetY();
            int pz = source.anchorZ + port.offsetZ();
            // Fragments own positive faces, including the lower side of Brick boundaries.
            for (int x = (px - radius) & ~3; x <= px + radius; x += 4) {
                for (int y = (py - radius) & ~3; y <= py + radius; y += 4) {
                    for (int z = (pz - radius) & ~3; z <= pz + radius; z += 4) {
                        long brick = BlockPos.asLong(x, y, z);
                        if (pages.hasResidentBrick(brick)) mixingChanges.add(brick);
                    }
                }
            }
        }
    }

    public void rebindDirty(ThermalSourceLedger ledger) {
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
                if (previous.emitting) markMixingChanged(previous);
                unindex(previous);
            }
            SourceDescriptor next = new SourceDescriptor(
                    sourceId,
                    lifecycleGeneration,
                    batch.anchorX(eventIndex),
                    batch.anchorY(eventIndex),
                    batch.anchorZ(eventIndex),
                    profile(batch.profileId(eventIndex)));
            next.emitting = ledger.suppliesPower(sourceId);
            sources.put(sourceId, next);
            index(next);
            if (next.emitting) markMixingChanged(next);
            markDirty(sourceId);
        } else if (kind == ThermalSourceBatch.Kind.UNLOAD) {
            SourceDescriptor previous = sources.get(sourceId);
            if (previous != null
                    && previous.lifecycleGeneration == lifecycleGeneration) {
                if (previous.emitting) markMixingChanged(previous);
                sources.remove(sourceId);
                unindex(previous);
                dirtySources.remove(sourceId);
            }
            return;
        } else if (kind == ThermalSourceBatch.Kind.POWER_CHANGE || kind == ThermalSourceBatch.Kind.ENABLED_CHANGE) {
            SourceDescriptor current = sources.get(sourceId);
            if (current != null) {
                boolean emitting = ledger.suppliesPower(sourceId);
                if (current.emitting != emitting) {
                    current.emitting = emitting;
                    markMixingChanged(current);
                }
            }
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

    private MinecraftPhysicalSourceProfile profile(int profileId) {
        return profileId == campfireProfile.profileId()
                ? campfireProfile
                : MinecraftPhysicalSourceProfile.byId(profileId);
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
            if (port.kind() == PortKind.RADIATION_LOSS) {
                source.bindings[index] = SourceBinding.declaredLoss(
                        sinkId(source.sourceId, port));
            } else {
                int blockX = source.anchorX + port.offsetX();
                int blockY = source.anchorY + port.offsetY();
                int blockZ = source.anchorZ + port.offsetZ();
                int slot = pages.resolveAirFaceTarget(
                        blockX,
                        blockY,
                        blockZ,
                        port.targetFace(),
                        signatures, airTarget);
                if (slot >= 0) {
                    source.bindings[index] = SourceBinding.thermalNode(
                            slot,
                            airTarget.generation());
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
            source.bindings[index] = SourceBinding.declaredLoss(
                    sinkId(source.sourceId, port));
        }
    }

    public static EmissionPort[] initialPorts(
            long sourceId,
            MinecraftPhysicalSourceProfile profile
    ) {
        EmissionPort[] result = new EmissionPort[profile.portCount()];
        for (int index = 0; index < result.length; index++) {
            Port port = profile.port(index);
            SourceBinding binding = switch (port.kind()) {
                case AIR_FACE -> SourceBinding.degradedLoss(
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
        private boolean emitting;

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
