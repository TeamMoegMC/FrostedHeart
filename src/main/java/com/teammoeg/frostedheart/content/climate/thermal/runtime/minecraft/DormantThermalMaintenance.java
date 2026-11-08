/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.ThermalRetentionPolicy;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;

/** Main-thread cleanup of loaded checkpoints. Unloaded chunks never have a maintenance task. */
final class DormantThermalMaintenance {
    private static final Object2ObjectLinkedOpenHashMap<LevelChunk, Entry> entries = new Object2ObjectLinkedOpenHashMap<>();
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final long BUDGET_NANOS = 200_000;
    private static final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    private static final MaterialSample sample = new MaterialSample();
    private static Work work;
    private static long examined, removedMaterials, removedAirSections, maximumNanos;

    private DormantThermalMaintenance() { }

    private static final class Entry {
        int section;
        long nextTick;
        boolean retained;
    }

    static void track(LevelChunk chunk) {
        if (!(chunk.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        if (level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) != chunk) return;
        loaded(chunk);
    }

    static void loaded(LevelChunk chunk) {
        if (MinecraftThermalInput.dormantState(chunk) == null) { unload(chunk); return; }
        if (!entries.containsKey(chunk)) entries.put(chunk, new Entry());
    }

    static void unload(LevelChunk chunk) {
        entries.remove(chunk);
        if (work != null && work.chunk == chunk) work = null;
    }

    static void unload(ServerLevel level) {
        var iterator = entries.keySet().iterator();
        while (iterator.hasNext()) if (iterator.next().getLevel() == level) iterator.remove();
        if (work != null && work.level == level) work = null;
    }

    static void clear() { entries.clear(); work = null; }

    static void tick() {
        if (entries.isEmpty() && work == null) return;
        long started = System.nanoTime(), deadline = started + BUDGET_NANOS;
        try {
            int candidates = Math.min(64, entries.size()), records = 512;
            do {
                if (work == null) {
                    if (entries.isEmpty() || candidates-- == 0) break;
                    LevelChunk chunk = entries.firstKey();
                    Entry entry = entries.getAndMoveToLast(chunk);
                    ServerLevel level = (ServerLevel) chunk.getLevel();
                    if (level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) != chunk) { unload(chunk); continue; }
                    var state = MinecraftThermalInput.dormantState(chunk);
                    if (state == null) { unload(chunk); continue; }
                    if (state.isEmpty()) {
                        if (!MinecraftThermalInput.dormantMaintenanceReady(level, chunk)) continue;
                        MinecraftThermalInput.setDormantState(chunk, null);
                        chunk.setUnsaved(true);
                        continue;
                    }
                    long tick = level.getGameTime();
                    if (tick < entry.nextTick) continue;
                    int sectionY = chunk.getSectionYFromSectionIndex(entry.section++);
                    long eligible = MinecraftThermalInput.dormantMaintenanceMask(level, chunk, sectionY);
                    var air = state.air(sectionY);
                    var materials = state.materials(sectionY);
                    if (eligible == 0) entry.retained = true; // Live ownership can end without a new stored object.
                    if (((air == null ? 0 : air.brickMask()) | (materials == null ? 0 : materials.brickMask())) != 0
                            && eligible != 0) {
                        entry.retained = true;
                        work = new Work(chunk, state, sectionY, eligible, tick, air, materials);
                    }
                    if (entry.section == chunk.getSections().length) {
                        entry.section = 0; entry.nextTick = tick + CHECK_INTERVAL_TICKS;
                        if (!entry.retained) entries.remove(chunk);
                        entry.retained = false;
                    }
                }
                if (work != null) {
                    Work current = work;
                    if (!current.current()) { work = null; continue; }
                    int end = Math.min(current.materials == null ? 0 : current.materials.size(), current.cursor + Math.min(32, records));
                    for (; current.cursor < end; current.cursor++) {
                        records--; examined++;
                        int index = current.cursor, local = current.materials.position(index);
                        int brick = (local & 15) >>> 2 | (local >>> 4 & 15) >>> 2 << 2 | (local >>> 8) >>> 2 << 4;
                        if ((current.eligible & 1L << brick) == 0) continue;
                        position.set(current.chunk.getPos().getMinBlockX() + (local & 15),
                                SectionPos.sectionToBlockCoord(current.sectionY) + (local >>> 8),
                                current.chunk.getPos().getMinBlockZ() + (local >>> 4 & 15));
                        var state = current.chunk.getBlockState(position);
                        var law = MinecraftThermalProfiles.materialLaw(state);
                        if (law == null || Block.getId(state) != current.materials.stateId(index)) { current.retirement.remove(index); continue; }
                        long saved = current.materials.savedTick(index);
                        if (current.projectedTick != saved) {
                            current.projectedTick = saved;
                            current.fraction = DormantThermalCooling.fraction(saved, current.tick, current.rate);
                        }
                        current.materials.read(index, law, current.tick, current.natural, current.rate, current.fraction, sample);
                        var edge = law.transition(sample.branch());
                        if (edge != null && edge.complete(sample.enthalpyJ()) && MinecraftPhaseController.applyDormantTransition(
                                current.level, position, state, edge, sample.enthalpyJ(), current.tick)) {
                            work = null; break; // World mutation changed the checkpoint; discard this old removal proposal.
                        }
                        if (!ThermalRetentionPolicy.retains(law, sample.enthalpyJ(), sample.branch(), current.natural))
                            current.retirement.remove(index);
                    }
                    if (work == current && (current.materials == null || current.cursor == current.materials.size())) {
                        current.commit(); work = null;
                    }
                    if (records == 0) break;
                }
            } while (System.nanoTime() < deadline);
        } finally { maximumNanos = Math.max(maximumNanos, System.nanoTime() - started); }
    }

    private static final class Work {
        final LevelChunk chunk;
        final ServerLevel level;
        final DormantChunkThermalState state;
        final DormantChunkThermalState.SectionEntry air;
        final MaterialSectionState materials;
        final MaterialSectionState.Retirement retirement;
        final int sectionY, profileEpoch;
        final long tick, eligible, materialRevision;
        final double natural, rate;
        int cursor;
        long projectedTick = Long.MIN_VALUE;
        double fraction;

        Work(LevelChunk chunk, DormantChunkThermalState state, int sectionY, long eligible, long tick,
                DormantChunkThermalState.SectionEntry air, MaterialSectionState materials) {
            this.chunk = chunk; level = (ServerLevel) chunk.getLevel(); this.state = state; this.sectionY = sectionY;
            this.eligible = eligible; this.tick = tick; this.air = air; this.materials = materials;
            natural = natural(); materialRevision = state.materialRevision(sectionY);
            rate = DormantThermalCooling.rate(MinecraftThermalInput.dormantHalfLifeSeconds());
            profileEpoch = MinecraftThermalProfiles.profileEpoch();
            retirement = materials == null ? null : materials.new Retirement(eligible, tick, natural, rate);
        }

        double natural() {
            return state.naturalTemperature(level, chunk.getPos().x, sectionY, chunk.getPos().z, level.getGameTime(), position);
        }

        boolean current() {
            return level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) == chunk
                    && MinecraftThermalInput.dormantState(chunk) == state && state.air(sectionY) == air
                    && state.materialRevision(sectionY) == materialRevision && state.materials(sectionY) == materials
                    && MinecraftThermalProfiles.profileEpoch() == profileEpoch
                    && MinecraftThermalInput.dormantMaintenanceMask(level, chunk, sectionY) == eligible
                    && natural() == natural;
        }

        void commit() {
            if (!current()) return;
            var nextAir = air == null ? null : air.retain(eligible, natural, tick, rate);
            var nextMaterials = retirement == null ? null : retirement.finish();
            if (!state.commitRetirement(sectionY, air, materials, nextAir, nextMaterials)) return;
            if (air != null && nextAir == null) removedAirSections++;
            removedMaterials += (materials == null ? 0 : materials.size()) - (nextMaterials == null ? 0 : nextMaterials.size());
            chunk.setUnsaved(true);
            if (state.isEmpty()) MinecraftThermalInput.setDormantState(chunk, null);
        }
    }

    record Statistics(int chunks, long examinedRecords, long removedMaterials, long removedAirSections, long maximumNanos) { }
    static Statistics statistics() { return new Statistics(entries.size(), examined, removedMaterials, removedAirSections, maximumNanos); }
}
