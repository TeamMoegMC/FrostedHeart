/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

/** Position-owned structural input. A busy page retains two exact descriptions and one older summary. */
public final class TopologyInbox {
    public record Input(ThermalPageHandle page, ThermalInputBatch batch, LongSet mixing,
            boolean aggregated) { }
    private static final class PageQueue {
        Input frozen;
        final ArrayDeque<Input> recent = new ArrayDeque<>();
        Input older;
        Input first() { return frozen != null ? frozen : older != null ? older : recent.peekFirst(); }
        void enqueue(Input input) {
            // Source membership can arrive after a Page description with a later physical boundary.
            // Keep due controls ahead of that description; transport order is not physical order.
            var ordered = new ArrayList<Input>(recent.size() + 2);
            if (older != null) { ordered.add(older); older = null; }
            ordered.addAll(recent);
            boolean combined = false;
            for (int index = 0; index < ordered.size(); index++) {
                var existing = ordered.get(index);
                if (existing.aggregated || input.aggregated || existing.page != input.page
                        || existing.batch.dimensionGeneration() != input.batch.dimensionGeneration()
                        || existing.batch.targetTick() != input.batch.targetTick()
                        || existing.batch.sequence() != input.batch.sequence()) continue;
                // Same physical boundary, same transport cut: keep exact material
                // event order, but prepare this Page only once. Frozen inputs are
                // deliberately absent from ordered and retain their ownership.
                var merged = new PageBuilder(existing, false);
                merged.merge(input); ordered.set(index, merged.build()); combined = true; break;
            }
            if (!combined) ordered.add(input);
            ordered.sort(java.util.Comparator.comparingLong((Input value) -> value.batch.targetTick())
                    .thenComparingLong(value -> value.batch.sequence()));
            recent.clear();
            while (ordered.size() > 2) {
                var next = ordered.remove(0);
                if (older == null) older = next;
                else { var merged = new PageBuilder(older); merged.merge(next); older = merged.build(); }
            }
            recent.addAll(ordered);
        }
    }
    private final Long2ObjectLinkedOpenHashMap<PageQueue> pages = new Long2ObjectLinkedOpenHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private PageQueue queue(long section) {
        var queue = pages.get(section);
        if (queue == null) {
            queue = new PageQueue(); pages.put(section, queue);
            byChunk.computeIfAbsent(chunk(section), ignored -> new LongOpenHashSet()).add(section);
        }
        return queue;
    }
    private static long chunk(long section) { return net.minecraft.world.level.ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section)); }
    public long firstTickInChunk(long chunk) {
        var sections = byChunk.get(chunk);
        long first = Long.MAX_VALUE;
        if (sections != null) for (long section : sections) first = Math.min(first, firstTick(section));
        return first;
    }

    /** Coalesce only an unfrozen Page descriptor; source windows keep their own ordered owners. */
    public static ThermalPageInput merge(ThermalPageInput first, ThermalPageInput next) {
        var handle = next.handle() == null ? first.handle() : next.handle();
        var a = new Input(handle, first.payload(), new LongOpenHashSet(), true);
        var b = new Input(handle, next.payload(), new LongOpenHashSet(), true);
        var builder = new PageBuilder(a);
        builder.merge(b);
        return new ThermalPageInput(first.section(), builder.build().batch());
    }

    /** Transport already grouped this Page. Keep its exact arrays and material event order. */
    public void receive(ThermalPageInput fragment, LongFunction<ThermalPageHandle> handles) {
        var handle = fragment.handle();
        if (handle == null) handle = handles.apply(fragment.section());
        if (handle != null) queue(fragment.section()).enqueue(
                new Input(handle, fragment.payload(), LongSets.EMPTY_SET, false));
    }

    public boolean receiveMixing(long generation, long sequence, long tick, LongSet newlyChangedMixing,
            LongFunction<ThermalPageHandle> handles, double windScale) {
        var grouped = new Long2ObjectLinkedOpenHashMap<PageBuilder>();
        java.util.function.Function<ThermalPageHandle, PageBuilder> page = handle -> grouped.computeIfAbsent(handle.sectionKey(),
                ignored -> new PageBuilder(generation, sequence, tick, handle, windScale));
        for (long position : newlyChangedMixing) {
            long section = SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4);
            var handle = handles.apply(section);
            if (handle != null) page.apply(handle).mixing.add(position);
        }
        for (var entry : grouped.long2ObjectEntrySet()) {
            var built = entry.getValue().build();
            queue(entry.getLongKey()).enqueue(built);
        }
        return !grouped.isEmpty();
    }

    public boolean isEmpty() { return pages.isEmpty(); }
    public long firstTick(long section) {
        PageQueue queue = pages.get(section);
        Input first = queue == null ? null : queue.first();
        return first == null ? Long.MAX_VALUE : first.batch.targetTick();
    }
    public long[] pendingSections() { return pages.keySet().toLongArray(); }
    public int pendingSectionCount() { return pages.size(); }
    /** Coordinator-owned fair visitation; physical input ordering remains inside each PageQueue. */
    public long nextPendingSection() {
        long section = pages.firstLongKey(); pages.getAndMoveToLast(section); return section;
    }

    public Input peek(long section) {
        var page = pages.get(section);
        return page == null ? null : page.first();
    }

    public List<Input> freeze(LongSet sections, long tick, long sequence) {
        var selected = new ArrayList<Input>();
        for (long section : sections) {
            PageQueue queue = pages.get(section);
            if (queue == null || queue.frozen != null) continue;
            Input first = queue.first();
            if (first == null || first.batch.targetTick() != tick || first.batch.sequence() != sequence) continue;
            if (queue.older != null) { queue.frozen = first; queue.older = null; }
            else queue.frozen = queue.recent.removeFirst();
            selected.add(queue.frozen);
        }
        return selected;
    }

    public void complete(List<Input> completed) {
        for (Input input : completed) {
            long section = input.page.sectionKey();
            PageQueue queue = pages.get(section);
            if (queue == null || queue.frozen != input) continue;
            queue.frozen = null;
            if (queue.older == null && queue.recent.isEmpty()) {
                pages.remove(section);
                var indexed = byChunk.get(chunk(section));
                indexed.remove(section); if (indexed.isEmpty()) byChunk.remove(chunk(section));
            }
        }
    }

    /** Compiler adapter for one local closure; source events have a different owner. */
    public static ThermalInputBatch combine(List<Input> inputs, long sequence, long tick) {
        var admissions = new ArrayList<ThermalInputBatch.PageAdmission>();
        var retirements = new ArrayList<ThermalInputBatch.PageRetirement>();
        var residency = new ArrayList<ThermalInputBatch.PageResidencyUpdate>();
        var environments = new ArrayList<ThermalInputBatch.PageEnvironmentUpdate>();
        var acks = new ArrayList<ThermalInputBatch.PhaseAck>();
        var intents = new ArrayList<ThermalInputBatch.PhaseIntent>();
        var geometry = new ResolvedGeometryBatch.Builder();
        long generation = inputs.get(0).batch.dimensionGeneration();
        for (var input : inputs) {
            var batch = input.batch;
            java.util.Collections.addAll(admissions, batch.admissions());
            java.util.Collections.addAll(retirements, batch.retirements());
            java.util.Collections.addAll(residency, batch.residencyUpdates());
            java.util.Collections.addAll(environments, batch.environmentUpdates());
            java.util.Collections.addAll(acks, batch.phaseAcks());
            java.util.Collections.addAll(intents, batch.phaseIntents());
            appendGeometry(geometry, batch.geometry());
        }
        return new ThermalInputBatch(generation, sequence, tick,
                admissions.toArray(ThermalInputBatch.PageAdmission[]::new), retirements.toArray(ThermalInputBatch.PageRetirement[]::new),
                residency.toArray(ThermalInputBatch.PageResidencyUpdate[]::new), geometry.buildAndReset(),
                environments.toArray(ThermalInputBatch.PageEnvironmentUpdate[]::new), acks.toArray(ThermalInputBatch.PhaseAck[]::new),
                inputs.get(0).batch.farFieldConductanceScale(), intents.toArray(ThermalInputBatch.PhaseIntent[]::new));
    }

    private static void appendGeometry(ResolvedGeometryBatch.Builder into, ResolvedGeometryBatch input) {
        for (int index = 0; index < input.size(); index++) {
            if (input.kind(index) == ResolvedGeometryBatch.Kind.RESOLVED_CENTER)
                into.addResolvedCenter(input.page(index), input.geometryRevision(index), input.blockIndex(index), input.signatureId(index));
            else into.addFullResync(input.page(index), input.geometryRevision(index), input.geometryResyncReason(index), input.fullPageSignatures(index));
        }
        into.prependMaterialChanges(input.materialChanges());
        for (var halo : input.halos()) into.addHalo(halo.page(), halo.geometryRevision(), halo.halo());
    }

    public void clear() { pages.clear(); byChunk.clear(); }

    private record Material(int previous, int next, byte cause) { }
    private record MaterialAt(int block, Material material) { }
    private static final class PageBuilder {
        final long generation, firstTick;
        long sequence, revision;
        ThermalPageHandle handle;
        boolean aggregated, naturalChanged;
        double natural;
        double windScale;
        ThermalInputBatch.PageAdmission admission;
        ThermalInputBatch.PageRetirement retirement;
        ThermalInputBatch.PageResidencyUpdate residency;
        final Int2ObjectLinkedOpenHashMap<ThermalInputBatch.PhaseIntent> intents = new Int2ObjectLinkedOpenHashMap<>();
        final java.util.LinkedHashMap<ThermalInputBatch.PhaseAckIdentity, ThermalInputBatch.PhaseAck> acks = new java.util.LinkedHashMap<>();
        final Int2ObjectLinkedOpenHashMap<Material> materials = new Int2ObjectLinkedOpenHashMap<>();
        final ArrayList<MaterialAt> exactMaterials = new ArrayList<>();
        final Int2ObjectLinkedOpenHashMap<Integer> centers = new Int2ObjectLinkedOpenHashMap<>();
        final Int2ObjectLinkedOpenHashMap<Byte> sky = new Int2ObjectLinkedOpenHashMap<>();
        final LongOpenHashSet mixing = new LongOpenHashSet();
        com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures signatures;
        ThermalPageHandle.GeometryResyncReason reason;
        ResolvedGeometryBatch.HaloUpdate halo;

        PageBuilder(long generation, long sequence, long tick, ThermalPageHandle handle, double windScale) {
            this.generation = generation; this.sequence = sequence; firstTick = tick; this.handle = handle;
            this.windScale = windScale;
        }
        PageBuilder(Input input) {
            this(input, true);
        }
        PageBuilder(Input input, boolean aggregate) {
            this(input.batch.dimensionGeneration(), input.batch.sequence(), input.batch.targetTick(), input.page,
                    input.batch.farFieldConductanceScale());
            aggregated = aggregate || input.aggregated; merge(input);
        }
        void merge(Input input) {
            var batch = input.batch; sequence = Math.max(sequence, batch.sequence());
            windScale = input.batch.farFieldConductanceScale();
            for (var value : batch.retirements()) {
                if (retirement == null) retirement = value;
                if (admission != null && admission.page() == value.page()) admission = null;
                if (residency != null && residency.page() == value.page()) residency = null;
            }
            for (var value : batch.admissions()) { admission = value; handle = value.page(); }
            for (var value : batch.residencyUpdates()) residency = value;
            for (var value : batch.environmentUpdates()) environment(value);
            for (var value : batch.phaseIntents()) intents.put(value.blockIndex(), value);
            for (var ack : batch.phaseAcks()) acks.put(ack.identity(), ack);
            for (int index = 0; index < batch.geometry().size(); index++) geometry(batch.geometry(), index);
            for (int index = 0; index < batch.geometry().materialChanges().size(); index++) material(batch.geometry().materialChanges(), index);
            for (var value : batch.geometry().halos()) halo = value;
            mixing.addAll(input.mixing);
        }
        void geometry(ResolvedGeometryBatch input, int index) {
            revision = Math.max(revision, input.geometryRevision(index));
            if (input.kind(index) == ResolvedGeometryBatch.Kind.FULL_RESYNC_REQUIRED) {
                signatures = input.fullPageSignatures(index); reason = input.geometryResyncReason(index); centers.clear();
            } else centers.put(input.blockIndex(index), Integer.valueOf(input.signatureId(index)));
        }
        void material(ResolvedGeometryBatch.MaterialChanges input, int index) {
            if (!aggregated && exactMaterials.size() < 64) {
                exactMaterials.add(new MaterialAt(input.blockIndex(index), new Material(
                        input.previousSignature(index), input.nextSignature(index), input.cause(index))));
                return;
            }
            if (!aggregated) {
                aggregated = true;
                for (var entry : exactMaterials) mergeMaterial(entry.block, entry.material);
                exactMaterials.clear();
            }
            mergeMaterial(input.blockIndex(index), new Material(input.previousSignature(index), input.nextSignature(index), input.cause(index)));
        }
        void mergeMaterial(int block, Material next) {
            var old = materials.get(block);
            byte cause = old != null && old.cause == ResolvedGeometryBatch.MaterialChanges.REPLACE ? old.cause : next.cause;
            materials.put(block, new Material(old == null ? next.previous : old.previous, next.next, cause));
        }
        void environment(ThermalInputBatch.PageEnvironmentUpdate input) {
            if (input.naturalTemperatureChanged()) { naturalChanged = true; natural = input.naturalTemperatureC(); }
            for (int index = 0; index < input.skyColumns().length; index++) sky.put(Short.toUnsignedInt(input.skyColumns()[index]), Byte.valueOf(input.firstExposedLocalY()[index]));
        }
        Input build() {
            var geometry = new ResolvedGeometryBatch.Builder();
            if (signatures != null) geometry.addFullResync(handle, revision, reason, signatures);
            for (var entry : centers.int2ObjectEntrySet()) geometry.addResolvedCenter(handle, revision, entry.getIntKey(), entry.getValue());
            for (var entry : materials.int2ObjectEntrySet()) {
                var material = entry.getValue();
                geometry.addMaterialChange(handle, entry.getIntKey(), material.previous, material.next, material.cause);
            }
            for (var entry : exactMaterials) {
                var material = entry.material;
                geometry.addMaterialChange(handle, entry.block, material.previous, material.next, material.cause);
            }
            if (halo != null) geometry.addHalo(handle, halo.geometryRevision(), halo.halo());
            short[] columns = new short[sky.size()]; byte[] exposed = new byte[sky.size()]; int next = 0;
            for (var entry : sky.int2ObjectEntrySet()) { columns[next] = (short) entry.getIntKey(); exposed[next++] = entry.getValue(); }
            var environment = naturalChanged || columns.length != 0
                    ? new ThermalInputBatch.PageEnvironmentUpdate[] {new ThermalInputBatch.PageEnvironmentUpdate(handle, naturalChanged, natural, columns, exposed)}
                    : ThermalInputBatch.NO_ENVIRONMENT_UPDATES;
            var batch = new ThermalInputBatch(generation, sequence, firstTick,
                    admission == null ? ThermalInputBatch.NO_ADMISSIONS : new ThermalInputBatch.PageAdmission[] {admission},
                    retirement == null ? ThermalInputBatch.NO_RETIREMENTS : new ThermalInputBatch.PageRetirement[] {retirement},
                    residency == null ? ThermalInputBatch.NO_RESIDENCY_UPDATES : new ThermalInputBatch.PageResidencyUpdate[] {residency},
                    geometry.buildAndReset(), environment,
                    acks.values().toArray(ThermalInputBatch.PhaseAck[]::new), windScale,
                    intents.values().toArray(ThermalInputBatch.PhaseIntent[]::new));
            return new Input(handle, batch, new LongOpenHashSet(mixing), aggregated);
        }
    }
}
