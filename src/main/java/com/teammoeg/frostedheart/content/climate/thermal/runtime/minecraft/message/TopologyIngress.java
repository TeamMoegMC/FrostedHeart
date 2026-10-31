/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

/** Main-to-coordinator transport for geometry and world controls. Contains no source windows. */
public final class TopologyIngress implements AutoCloseable {
    private static final class Slot {
        final ThermalMemoryBudget.Reservation metadata;
        ThermalPageInput pending;
        Slot(ThermalMemoryBudget.Reservation metadata) { this.metadata = metadata; }
    }
    private final ThermalMemoryBudget budget;
    private final Long2ObjectOpenHashMap<Slot> slots = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<Slot> ready = new Long2ObjectLinkedOpenHashMap<>();
    public TopologyIngress(ThermalMemoryBudget budget) { this.budget = budget; }

    /** Bounded control metadata must not wait for the geometry it is needed to update. */
    public synchronized void prepare(ThermalPageInput[] fragments) {
        for (var fragment : fragments) if (fragment.structural() && !slots.containsKey(fragment.section())) {
            var metadata = fragment.handle() == null ? null : fragment.handle().takeInputCredit();
            if (metadata == null) metadata = budget.reserveCompletion(512);
            slots.put(fragment.section(), new Slot(metadata));
        }
    }
    public synchronized void offer(ThermalPageInput fragment) {
        if (!fragment.structural()) return;
        var slot = slots.get(fragment.section());
        slot.pending = slot.pending == null ? fragment : TopologyInbox.merge(slot.pending, fragment);
        ready.put(fragment.section(), slot);
    }
    public synchronized ThermalPageInput poll() {
        if (ready.isEmpty()) return null;
        var slot = ready.removeFirst(); var input = slot.pending; slot.pending = null; return input;
    }
    public synchronized boolean hasWork() { return !ready.isEmpty(); }
    public synchronized long boundary(long section) {
        var slot = ready.get(section); return slot == null ? Long.MAX_VALUE : slot.pending.boundaryTick();
    }
    public synchronized long firstTickInChunk(long chunk) {
        long tick = Long.MAX_VALUE;
        for (var entry : ready.long2ObjectEntrySet()) {
            long section = entry.getLongKey();
            if (ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section)) == chunk)
                tick = Math.min(tick, entry.getValue().pending.boundaryTick());
        }
        return tick;
    }
    public synchronized void forEachPending(java.util.function.Consumer<ThermalPageInput> visitor) {
        for (var slot : ready.values()) visitor.accept(slot.pending);
    }
    public synchronized void retire(long section) {
        var slot = slots.get(section);
        if (slot != null && slot.pending == null) { slots.remove(section); slot.metadata.close(); }
    }
    @Override public synchronized void close() {
        for (var slot : slots.values()) slot.metadata.close(); slots.clear(); ready.clear();
    }
}
