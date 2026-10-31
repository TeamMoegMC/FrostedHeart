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
package com.teammoeg.frostedheart.content.climate.thermal.source.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.Port;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.PortKind;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirGeometryView;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

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
public final class WorkerPhysicalSourceBindings implements ThermalSourceLedger.BindingUpdates {
    private final WorkerPageStore pages;
    private final ThermalSignatureTable signatures;
    private final MinecraftPhysicalSourceProfile campfireProfile;
    private final AirLoadTable airLoads;
    private final Long2ObjectOpenHashMap<SourceDescriptor> sources = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> sourcesBySection =
            new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet dirtySources = new LongOpenHashSet();
    private final LongArrayList dirtyOrder = new LongArrayList();
    private final Long2LongOpenHashMap mixingChanges = new Long2LongOpenHashMap();
    private final LongSet pendingMixing = LongSets.unmodifiable(mixingChanges.keySet());
    private long mixingRevision;
    private int mixingBrickHighWater;
    private final WorkerPageStore.MutableAirTarget airTarget =
            new WorkerPageStore.MutableAirTarget();

    public WorkerPhysicalSourceBindings(WorkerPageStore pages, ThermalSignatureTable signatures,
            MinecraftPhysicalSourceProfile campfireProfile, AirLoadTable airLoads) {
        this.pages = pages;
        this.signatures = signatures;
        this.campfireProfile = Objects.requireNonNull(campfireProfile, "campfireProfile");
        this.airLoads = airLoads;
    }

    /** Recorded input has already copied world identities before retired ids are reusable. */
    public void reclaimInputIdentities() {
        airLoads.reclaimWorldBindings(32);
    }

    public int activeAirPortCount() {
        int count = 0;
        for (SourceDescriptor source : sources.values()) {
            if (!source.emitting) continue;
            for (int index = 0; index < source.profile.portCount(); index++) {
                Port port = source.profile.port(index);
                if (port.kind() == PortKind.AIR_FACE && port.powerShare() > 0) count++;
            }
        }
        return count;
    }

    @FunctionalInterface
    public interface AirPortConsumer {
        void accept(int x, int y, int z, BlockFace face);
    }

    /**
     * One private build's emitter membership and mixing invalidations. Positions retain
     * the live traversal order; neither mutable descriptors nor binding addresses escape.
     * Geometry remains owned by the topology plan for the duration of this build.
     */
    public FrozenSources freeze() {
        return freezeSources(sources.values());
    }

    public FrozenSources freeze(LongSet sections, long tick) {
        var selected = new LongOpenHashSet();
        var entries = new java.util.ArrayList<SourceDescriptor>();
        for (long section : sections) {
            var indexed = sourcesBySection.get(section);
            if (indexed == null) continue;
            for (long id : indexed) if (selected.add(id)) {
                var source = sources.get(id);
                if (source != null && source.birthTick <= tick) entries.add(source);
            }
        }
        return freezeSources(entries);
    }

    private FrozenSources freezeSources(java.util.Collection<SourceDescriptor> selected) {
        int count = 0;
        for (var source : selected) if (source.emitting)
            for (int port = 0; port < source.profile.portCount(); port++)
                if (source.profile.port(port).kind() == PortKind.AIR_FACE && source.profile.port(port).powerShare() > 0) count++;
        long[] positions = new long[count];
        byte[] faces = new byte[count];
        var offsets = new Long2IntOpenHashMap(count);
        int next = 0;
        for (SourceDescriptor source : selected) {
            if (!source.emitting) continue;
            for (int index = 0; index < source.profile.portCount(); index++) {
                Port port = source.profile.port(index);
                if (port.kind() != PortKind.AIR_FACE || port.powerShare() == 0) continue;
                int x = source.anchorX + port.offsetX();
                int y = source.anchorY + port.offsetY();
                int z = source.anchorZ + port.offsetZ();
                positions[next] = BlockPos.asLong(x, y, z);
                faces[next++] = (byte) port.targetFace().ordinal();
                offsets.addTo(sectionKey(x, y, z), 1);
            }
        }
        var bySection = new Long2ObjectOpenHashMap<int[]>(offsets.size());
        for (var entry : offsets.long2IntEntrySet()) {
            bySection.put(entry.getLongKey(), new int[entry.getIntValue()]);
            entry.setValue(0);
        }
        for (int index = 0; index < count; index++) {
            long position = positions[index];
            long section = sectionKey(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position));
            bySection.get(section)[offsets.addTo(section, 1)] = index;
        }
        var frozen = new FrozenSources(positions, faces, bySection,
                new LongOpenHashSet(mixingChanges.keySet()), mixingRevision, signatures);
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.sourceFrozen(this, frozen);
        return frozen;
    }

    /** Upper bound for new snapshot backing plus its temporary section counters. */
    public long frozenPreparationBytes() {
        // Empty backing: snapshot/arrays 96, section map 288, dirty set 160,
        // temporary counters 224. The variable bound allows one section per port.
        return 768L + 256L * activeAirPortCount() + 64L * mixingChanges.size();
    }

    public static final class FrozenSources {
        private final long[] positions;
        private final byte[] faces;
        private final Long2ObjectOpenHashMap<int[]> bySection;
        private final LongSet mixingChanges;
        private final long mixingRevision;
        private final ThermalSignatureTable signatures;

        private FrozenSources(long[] positions, byte[] faces,
                Long2ObjectOpenHashMap<int[]> bySection, LongOpenHashSet mixingChanges, long mixingRevision,
                ThermalSignatureTable signatures) {
            this.positions = positions;
            this.faces = faces;
            this.bySection = bySection;
            this.mixingChanges = LongSets.unmodifiable(mixingChanges);
            this.mixingRevision = mixingRevision;
            this.signatures = signatures;
        }

        public int activeAirPortCount() { return positions.length; }
        public boolean resolveAirPort(int index, AirGeometryView geometry, WorkerPageStore.MutableAirTarget target) {
            long position = positions[index];
            return geometry.resolveAirFaceTarget(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position),
                    BlockFace.fromOrdinal(faces[index]), signatures, target) >= 0;
        }
        public LongSet mixingChanges() { return mixingChanges; }
        public long mixingRevision() { return mixingRevision; }

        /** Original emitter faces, before routing, for capturing this build's routing reads. */
        public void collectOriginalAirPorts(AirPortConsumer consumer) {
            for (int index = 0; index < positions.length; index++) {
                long position = positions[index];
                consumer.accept(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position),
                        BlockFace.fromOrdinal(faces[index]));
            }
        }

        public void collectAirPorts(AirGeometryView geometry, AirPortConsumer consumer) {
            var target = new WorkerPageStore.MutableAirTarget();
            for (int index = 0; index < positions.length; index++) {
                long position = positions[index];
                int slot = geometry.resolveAirFaceTarget(BlockPos.getX(position), BlockPos.getY(position),
                        BlockPos.getZ(position), BlockFace.fromOrdinal(faces[index]), signatures, target);
                if (slot >= 0) consumer.accept(target.blockX(), target.blockY(), target.blockZ(), target.face());
            }
        }

        public void collectMixingSources(AirMixingRegion region) {
            if (positions.length == 0) return;
            for (int x = region.minX() >> 4; x <= region.maxX() >> 4; x++)
                for (int y = region.minY() >> 4; y <= region.maxY() >> 4; y++)
                    for (int z = region.minZ() >> 4; z <= region.maxZ() >> 4; z++) {
                        int[] ports = bySection.get(SectionPos.asLong(x, y, z));
                        if (ports == null) continue;
                        for (int port : ports) {
                            long position = positions[port];
                            region.include(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position));
                        }
                    }
        }

        public void recordBacking(AirBackingMeter meter) {
            if (!meter.objectOnce(this, 64)) return;
            meter.array(positions); meter.array(faces);
            meter.container(bySection, bySection.size(), 16);
            for (int[] ports : bySection.values()) meter.array(ports);
            meter.container(mixingChanges, mixingChanges.size(), 8);
        }
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
    public LongSet mixingChanges() {
        return pendingMixing;
    }

    public long mixingRevision() { return mixingRevision; }
    public boolean mixingChangedAfter(long revision, java.util.function.LongPredicate section) {
        for (var entry : mixingChanges.long2LongEntrySet()) if (entry.getLongValue() > revision) {
            long block = entry.getLongKey();
            if (section.test(sectionKey(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block)))) return true;
        }
        return false;
    }

    /** Iterator removal retains map capacity, even after all pending revisions are consumed. */
    public void recordMixingBacking(AirBackingMeter meter) {
        meter.container(mixingChanges, mixingBrickHighWater, 16);
    }

    public void recordBacking(AirBackingMeter meter) {
        recordMixingBacking(meter);
        meter.container(sources, sources.size(), 16); meter.container(sourcesBySection, sourcesBySection.size(), 16);
        meter.container(dirtySources, dirtySources.size(), 8); meter.array(dirtyOrder.elements());
        for (var indexed : sourcesBySection.values()) meter.container(indexed, indexed.size(), 8);
        for (var source : sources.values()) {
            meter.objectOnce(source, 72); meter.array(source.bindings); meter.array(source.previousBindings);
            for (var binding : source.bindings) if (binding != null) meter.objectOnce(binding, 40);
        }
    }

    public LongOpenHashSet mixingChangesAfter(long revision) {
        var changed = new LongOpenHashSet();
        for (var entry : mixingChanges.long2LongEntrySet())
            if (entry.getLongValue() > revision) changed.add(entry.getLongKey());
        return changed;
    }

    public void mixingCommitted(long throughRevision, LongSet positions) {
        for (long position : positions)
            if (mixingChanges.containsKey(position) && mixingChanges.get(position) <= throughRevision) mixingChanges.remove(position);
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.sourceCommitted(this, throughRevision, mixingChanges);
    }

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
                            if (port.kind() != PortKind.AIR_FACE || port.powerShare() == 0)
                                continue;
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
        long revision = ++mixingRevision;
        forEachMixingBrick(source.profile, source.anchorX, source.anchorY, source.anchorZ,
                brick -> mixingChanges.put(brick, revision));
        mixingBrickHighWater = Math.max(mixingBrickHighWater, mixingChanges.size());
    }

    /** Announce exactly the same resident footprint before an unbound control can change membership. */
    public void collectMixingFootprint(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor source,
            LongSet positions) {
        forEachMixingBrick(profile(source.profile()), source.x(), source.y(), source.z(), positions::add);
    }

    private void forEachMixingBrick(MinecraftPhysicalSourceProfile profile, int anchorX, int anchorY, int anchorZ,
            java.util.function.LongConsumer visitor) {
        int radius = AirMixingRegion.RADIUS;
        for (int i = 0; i < profile.portCount(); i++) {
            Port port = profile.port(i);
            if (port.kind() != PortKind.AIR_FACE || port.powerShare() == 0) continue;
            int px = anchorX + port.offsetX();
            int py = anchorY + port.offsetY();
            int pz = anchorZ + port.offsetZ();
            // Fragments own positive faces, including the lower side of Brick boundaries.
            for (int x = (px - radius) & ~3; x <= px + radius; x += 4) {
                for (int y = (py - radius) & ~3; y <= py + radius; y += 4) {
                    for (int z = (pz - radius) & ~3; z <= pz + radius; z += 4) {
                        long brick = BlockPos.asLong(x, y, z);
                        if (pages.hasResidentBrick(brick)) visitor.accept(brick);
                    }
                }
            }
        }
    }

    private java.util.function.LongPredicate bindingGeometryCurrent = ignored -> true;
    public void bindingGeometryCurrent(java.util.function.LongPredicate predicate) { bindingGeometryCurrent = predicate; }

    /** An emitter and its actual outlet participate in the same structural handoff. */
    public RegionalContinuationBoundary includeBoundOutlets(RegionalContinuationBoundary boundary) {
        var sections = new LongOpenHashSet();
        for (var source : sources.values()) for (int index = 0; index < source.bindings.length; index++) {
            var binding = source.bindings[index];
            if (binding == null || binding.kind() != SourceBinding.Kind.AIR_STENCIL) continue;
            var port = source.profile.port(index);
            long origin = sectionKey(source.anchorX + port.offsetX(), source.anchorY + port.offsetY(), source.anchorZ + port.offsetZ());
            long block = airLoads.get((int) binding.targetId(), binding.lifecycleGeneration()).block();
            long target = sectionKey(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
            if (boundary.affects(origin) || boundary.affects(target)) { sections.add(origin); sections.add(target); }
        }
        return boundary.including(sections);
    }

    /** Both the original face and the current routed destination constrain recording. */
    public long bindingLimit(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor,
            java.util.function.LongUnaryOperator boundary, java.util.function.LongUnaryOperator ownerBoundary) {
        long through = boundary.applyAsLong(descriptor.section());
        var profile = profile(descriptor.profile());
        var current = sources.get(descriptor.sourceId());
        for (int index = 0; index < profile.portCount(); index++) {
            var port = profile.port(index);
            through = Math.min(through, boundary.applyAsLong(sectionKey(descriptor.x() + port.offsetX(),
                    descriptor.y() + port.offsetY(), descriptor.z() + port.offsetZ())));
            through = Math.min(through, ownerBoundary.applyAsLong(BlockPos.asLong(descriptor.x() + port.offsetX(),
                    descriptor.y() + port.offsetY(), descriptor.z() + port.offsetZ())));
            var binding = current == null ? null : current.bindings[index];
            if (binding != null && binding.kind() == SourceBinding.Kind.AIR_STENCIL) {
                long block = airLoads.get((int) binding.targetId(), binding.lifecycleGeneration()).block();
                through = Math.min(through, boundary.applyAsLong(sectionKey(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block))));
                through = Math.min(through, ownerBoundary.applyAsLong(block));
            }
        }
        return through;
    }

    public void rebindDirty(ThermalSourceLedger ledger, java.util.function.LongPredicate geometryCurrent) {
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
            if (source != null && !geometryCurrent.test(source.sourceId)) {
                order[retained++] = sourceId;
                continue;
            }
            if (source == null || rebind(source, ledger, true)) {
                dirtySources.remove(sourceId);
            } else {
                order[retained++] = sourceId;
            }
        }
        dirtyOrder.size(retained);
    }

    public boolean geometryCurrent(long sourceId, java.util.function.LongPredicate sectionCurrent) {
        var source = sources.get(sourceId);
        if (source == null) return true;
        for (int index = 0; index < source.profile.portCount(); index++) {
            var port = source.profile.port(index);
            if (!sectionCurrent.test(sectionKey(source.anchorX + port.offsetX(), source.anchorY + port.offsetY(), source.anchorZ + port.offsetZ())))
                return false;
        }
        return true;
    }

    @Override
    public void registered(SpatialSourceTimeline.Descriptor descriptor, long tick, ThermalSourceLedger ledger) {
        long sourceId = descriptor.sourceId();
        SourceDescriptor previous = sources.remove(sourceId);
        if (previous != null) {
            for (var binding : previous.bindings) airLoads.releaseWorldBinding(binding);
            if (previous.emitting) markMixingChanged(previous);
            unindex(previous);
        }
        var next = new SourceDescriptor(sourceId, descriptor.generation(), descriptor.x(), descriptor.y(),
                descriptor.z(), profile(descriptor.profile()));
        next.emitting = ledger.suppliesPower(sourceId); next.birthTick = tick;
        sources.put(sourceId, next); index(next);
        if (next.emitting) markMixingChanged(next);
        markDirty(sourceId);
        finishControl(descriptor, ledger, true);
    }

    @Override
    public void emissionChanged(SpatialSourceTimeline.Descriptor descriptor, ThermalSourceLedger ledger) {
        SourceDescriptor current = sources.get(descriptor.sourceId());
        if (current != null) {
            boolean emitting = ledger.suppliesPower(descriptor.sourceId());
            if (current.emitting != emitting) {
                current.emitting = emitting;
                markMixingChanged(current);
            }
        }
        finishControl(descriptor, ledger, false);
    }

    @Override
    public void unloaded(SpatialSourceTimeline.Descriptor descriptor) {
        long sourceId = descriptor.sourceId();
        SourceDescriptor previous = sources.get(sourceId);
        if (previous != null && previous.lifecycleGeneration == descriptor.generation()) {
            for (var binding : previous.bindings) airLoads.releaseWorldBinding(binding);
            if (previous.emitting) markMixingChanged(previous);
            sources.remove(sourceId); unindex(previous); dirtySources.remove(sourceId);
        }
        observeControl(descriptor, false);
    }

    private void finishControl(SpatialSourceTimeline.Descriptor descriptor, ThermalSourceLedger ledger, boolean registration) {
        long sourceId = descriptor.sourceId();
        SourceDescriptor source = sources.get(sourceId);
        if (source != null && dirtySources.contains(sourceId) && rebind(source, ledger)) {
            dirtySources.remove(sourceId);
        }
        observeControl(descriptor, registration);
    }

    private void observeControl(SpatialSourceTimeline.Descriptor descriptor, boolean registration) {
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.sourceEvent(this, descriptor, registration);
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

    /** A source can be accepted before its geometry exists. Null denotes the declared radiation sink. */
    public com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target originalPort(
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor, int portIndex) {
        var port = profile(descriptor.profile()).port(portIndex);
        if (port.kind() != PortKind.AIR_FACE) return null;
        return new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target(
                net.minecraft.core.BlockPos.asLong(descriptor.x() + port.offsetX(), descriptor.y() + port.offsetY(), descriptor.z() + port.offsetZ()),
                (byte) (port.targetFace().ordinal() + 6), -1, 0, 0, 0, descriptor.sourceId(), descriptor.generation());
    }

    /** Read the current world destination without recording or advancing a source. */
    public com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.PortDestination checkpointTarget(
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor, int portIndex) {
        var port = profile(descriptor.profile()).port(portIndex);
        if (port.kind() != PortKind.AIR_FACE)
            return com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.PortDestination.loss(true);
        var target = new WorkerPageStore.MutableAirTarget();
        int slot = pages.resolveAirFaceTarget(descriptor.x() + port.offsetX(), descriptor.y() + port.offsetY(),
                descriptor.z() + port.offsetZ(), port.targetFace(), signatures, target);
        if (slot < 0) return com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.PortDestination.loss(
                slot != WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE);
        return com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.PortDestination.thermal(
                new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target(
                        BlockPos.asLong(target.blockX(), target.blockY(), target.blockZ()), (byte) target.face().ordinal(),
                        -1, target.generation(), 0, 0, descriptor.sourceId(), descriptor.generation()));
    }

    private boolean rebind(SourceDescriptor source, ThermalSourceLedger ledger) {
        return rebind(source, ledger, false);
    }
    private boolean rebind(SourceDescriptor source, ThermalSourceLedger ledger, boolean committedBoundary) {
        if (!committedBoundary && !bindingGeometryCurrent.test(source.sourceId)) return false;
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
        // Missing geometry is accounted as degraded loss; a blocked/unused port is a declared
        // sink. Neither case creates a deferred energy queue or a second target search.
        System.arraycopy(source.bindings, 0, source.previousBindings, 0, source.bindings.length);
        Arrays.fill(source.bindings, null);
        for (int index = 0; index < source.profile.portCount(); index++) {
            Port port = source.profile.port(index);
            if (port.kind() == PortKind.RADIATION_LOSS) {
                source.bindings[index] = SourceBinding.declaredLoss(sinkId(source.sourceId, port));
            } else {
                int blockX = source.anchorX + port.offsetX();
                int blockY = source.anchorY + port.offsetY();
                int blockZ = source.anchorZ + port.offsetZ();
                int slot =
                        pages.resolveAirFaceTarget(
                                blockX, blockY, blockZ, port.targetFace(), signatures, airTarget);
                if (slot >= 0) {
                    source.bindings[index] =
                            airLoads.bindWorldIdentity(airTarget.blockX(), airTarget.blockY(), airTarget.blockZ(),
                                    airTarget.face(), airTarget.generation());
                } else if (slot == WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE) {
                    source.bindings[index] =
                            SourceBinding.degradedLoss(sinkId(source.sourceId, port));
                }
            }
        }
        for (int index = 0; index < source.profile.portCount(); index++) {
            if (source.bindings[index] != null) {
                continue;
            }
            Port port = source.profile.port(index);
            source.bindings[index] = SourceBinding.declaredLoss(sinkId(source.sourceId, port));
        }
        for (int index = 0; index < source.bindings.length; index++) {
            airLoads.retainWorldBinding(source.bindings[index]); airLoads.releaseWorldBinding(source.previousBindings[index]);
            source.previousBindings[index] = null;
        }
    }

    public static EmissionPort[] initialPorts(
            long sourceId, MinecraftPhysicalSourceProfile profile) {
        EmissionPort[] result = new EmissionPort[profile.portCount()];
        for (int index = 0; index < result.length; index++) {
            Port port = profile.port(index);
            SourceBinding binding =
                    switch (port.kind()) {
                        case AIR_FACE -> SourceBinding.degradedLoss(sinkId(sourceId, port));
                        case RADIATION_LOSS -> SourceBinding.declaredLoss(sinkId(sourceId, port));
                    };
            result[index] = EmissionPort.of(port.portId(), port.powerShare(), binding);
        }
        return result;
    }

    private void index(SourceDescriptor source) {
        for (int index = 0; index < source.profile.portCount(); index++) {
            Port port = source.profile.port(index);
            if (port.kind() != PortKind.AIR_FACE) {
                continue;
            }
            long sectionKey =
                    sectionKey(
                            source.anchorX + port.offsetX(),
                            source.anchorY + port.offsetY(),
                            source.anchorZ + port.offsetZ());
            sourcesBySection
                    .computeIfAbsent(sectionKey, ignored -> new LongOpenHashSet())
                    .add(source.sourceId);
        }
    }

    private void unindex(SourceDescriptor source) {
        for (int index = 0; index < source.profile.portCount(); index++) {
            Port port = source.profile.port(index);
            if (port.kind() != PortKind.AIR_FACE) {
                continue;
            }
            long sectionKey =
                    sectionKey(
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
        return Long.rotateLeft(sourceId, 17) ^ Integer.toUnsignedLong(port.portId() + 1);
    }

    private static final class SourceDescriptor {
        private final long sourceId;
        private final int lifecycleGeneration;
        private final int anchorX;
        private final int anchorY;
        private final int anchorZ;
        private final MinecraftPhysicalSourceProfile profile;
        private final SourceBinding[] bindings;
        private final SourceBinding[] previousBindings;
        private long birthTick;
        private boolean emitting;

        private SourceDescriptor(
                long sourceId,
                int lifecycleGeneration,
                int anchorX,
                int anchorY,
                int anchorZ,
                MinecraftPhysicalSourceProfile profile) {
            this.sourceId = sourceId;
            this.lifecycleGeneration = lifecycleGeneration;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.profile = profile;
            bindings = new SourceBinding[profile.portCount()];
            previousBindings = new SourceBinding[profile.portCount()];
        }
    }
}
