/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialPowerTimeline;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

/** GameTest value-file fixture using v6; production keeps the original RegionalCheckpoint/Chunk chain. */
public final class TiledThermalCheckpoint implements AutoCloseable {
    /** Main-owned edits can outlive the frozen geometry of an in-progress epoch. */
    public interface CaptureOverlay {
        void project(long section, TiledThermalField.SectionSnapshot values);
        int materialStateId(SpatialEnergyBanks.Address address, int capturedStateId);
        boolean airMayRemain(SpatialEnergyBanks.Address address, boolean capturedAir);
    }
    // One 4096-cell snapshot plus the reusable material capture arrays and their
    // worst-case 4096-entry palette. Output arrays are charged separately.
    private static final long SCRATCH_BYTES = 512 * 1024;
    private final MinecraftThermalProfiles.Snapshot profiles;
    private final ThermalMemoryBudget budget;
    private final ThermalMemoryBudget.Reservation scratchCredit;
    private TiledThermalField.SectionSnapshot scratch;
    private MaterialSectionState.CaptureScratch materialScratch;
    private final MaterialSample material = new MaterialSample();

    /** A save owner retains this until both its value capture and I/O use have ended. */
    public static final class Payload implements AutoCloseable {
        private DormantChunkThermalState state;
        private ThermalMemoryBudget.Reservation credit;
        public final long chunk, saveTick;
        public final double removedMatterInputJ;
        private Payload(long chunk, long saveTick, DormantChunkThermalState state, ThermalMemoryBudget.Reservation credit, double removedMatterInputJ) {
            this.chunk = chunk; this.saveTick = saveTick; this.state = state; this.credit = credit;
            this.removedMatterInputJ = removedMatterInputJ;
        }
        /** Borrowed values; the caller must finish using them before close. */
        public DormantChunkThermalState state() { return state; }
        public long reservedBytes() { return credit == null ? 0 : credit.bytes(); }
        public void encode(CompoundTag chunkTag) {
            if (state == null) throw new IllegalStateException("Checkpoint values have been released");
            state.encode(chunkTag);
        }
        @Override public void close() {
            state = null;
            if (credit != null) { credit.close(); credit = null; }
        }
    }

    public TiledThermalCheckpoint(ThermalMemoryBudget budget, MinecraftThermalProfiles.Snapshot profiles) {
        this.budget = budget; this.profiles = profiles;
        scratchCredit = budget.tryReserve(SCRATCH_BYTES);
        if (scratchCredit == null) throw new ThermalMemoryBudget.AllocationRefused(SCRATCH_BYTES);
        try { scratch = new TiledThermalField.SectionSnapshot(); materialScratch = new MaterialSectionState.CaptureScratch(); }
        catch (RuntimeException | Error failure) { scratchCredit.close(); throw failure; }
    }

    /**
     * No numerical wait and no trial state. Main must integrate current producer
     * power to saveTick before calling. Accepted and the unconfirmed banks form
     * the same prefix because only main can commit the core and its input bank.
     * A save-slot owner supplies its pre-admitted output credit, or obtains one
     * through tryCapture when free capacity is available.
     */
    public Payload capture(long chunk, int minimumSectionY, int sectionCount, long saveTick,
            TiledThermalField field, SpatialEnergyBanks banks, ThermalMemoryBudget.Reservation outputCredit) {
        return capture(chunk, minimumSectionY, sectionCount, saveTick, field, banks, null, outputCredit);
    }

    public Payload capture(long chunk, int minimumSectionY, int sectionCount, long saveTick,
            TiledThermalField field, SpatialEnergyBanks banks, SpatialPowerTimeline power, ThermalMemoryBudget.Reservation outputCredit) {
        return capture(chunk, minimumSectionY, sectionCount, saveTick, field, banks, power, outputCredit, null);
    }

    public Payload capture(long chunk, int minimumSectionY, int sectionCount, long saveTick,
            TiledThermalField field, SpatialEnergyBanks banks, SpatialPowerTimeline power, ThermalMemoryBudget.Reservation outputCredit,
            CaptureOverlay overlay) {
        if (power != null) power.integrateChunk(chunk, saveTick);
        long required = requiredBytes(chunk, minimumSectionY, sectionCount, field, banks, power);
        if (outputCredit.bytes() < required) throw new IllegalArgumentException("Checkpoint output was not fully admitted");
        var state = new DormantChunkThermalState(minimumSectionY, sectionCount);
        long referenceTick = saveTick;
        for (int offset = 0; offset < sectionCount; offset++) {
            int y = minimumSectionY + offset;
            long section = SectionPos.asLong(ChunkPos.getX(chunk), y, ChunkPos.getZ(chunk));
            if (!field.snapshot(section, scratch)) continue;
            if (overlay != null) overlay.project(section, scratch);
            referenceTick = Math.min(referenceTick, scratch.sampleTick);
            var air = field.captureAir(section, scratch.activeBricks, Double.NaN, scratch);
            if (air != null) state.replace(y, new DormantChunkThermalState.SectionEntry(scratch.sampleTick, scratch.ambientC, air));
            state.replaceMaterials(y, MaterialSectionState.captureCells(scratch.signatures, scratch.enthalpyJ, scratch.branches,
                    scratch.sampleTick, profiles.signatures(), profiles.materials(), materialScratch));
        }
        var pending = new SpatialHeatInput.Builder();
        double[] removed = {0};
        banks.pendingTimedInChunk(chunk, (address, joules, since) ->
                appendPending(pending, field, overlay, address, Math.min(since, saveTick), saveTick, joules, removed));
        if (power != null) power.pendingReplaysInChunk(chunk, (address, from, to, joules) ->
                appendPending(pending, field, overlay, address, from, to, joules, removed));
        var heat = PendingHeatCheckpoint.capture(pending.freeze(), ignored -> -1, referenceTick);
        state.setPending(heat, heat, 0);
        return new Payload(chunk, saveTick, state, outputCredit, removed[0]);
    }

    public Payload tryCapture(long chunk, int minimumSectionY, int sectionCount, long saveTick,
            TiledThermalField field, SpatialEnergyBanks banks) {
        return tryCapture(chunk, minimumSectionY, sectionCount, saveTick, field, banks, null);
    }

    public Payload tryCapture(long chunk, int minimumSectionY, int sectionCount, long saveTick,
            TiledThermalField field, SpatialEnergyBanks banks, SpatialPowerTimeline power) {
        if (scratch == null) return null;
        var credit = budget.tryReserve(requiredBytes(chunk, minimumSectionY, sectionCount, field, banks, power));
        if (credit == null) return null;
        try { return capture(chunk, minimumSectionY, sectionCount, saveTick, field, banks, power, credit); }
        catch (RuntimeException | Error failure) { credit.close(); throw failure; }
    }

    /** Bounds immutable captured arrays, independently of encoding and downstream I/O copies. */
    public static long requiredBytes(long chunk, int minimumSectionY, int sectionCount,
            TiledThermalField field, SpatialEnergyBanks banks) {
        return requiredBytes(chunk, minimumSectionY, sectionCount, field, banks, null);
    }

    public static long requiredBytes(long chunk, int minimumSectionY, int sectionCount,
            TiledThermalField field, SpatialEnergyBanks banks, SpatialPowerTimeline power) {
        long cells = 0;
        for (int y = minimumSectionY; y < minimumSectionY + sectionCount; y++)
            cells += field.sectionCellCount(SectionPos.asLong(ChunkPos.getX(chunk), y, ChunkPos.getZ(chunk)));
        // v6 Air is at most 32 corner-index + 8 value bytes per captured cell;
        // material arrays and palettes use fewer. Include object/array headers,
        // directory slack, and both the builder and frozen pending transport.
        return 4096 + cells * 64 + sectionCount * 16_384L
                + (banks.receiverCountInChunk(chunk) + (power == null ? 0 : power.replayCountInChunk(chunk))) * 6L * 384L;
    }

    private void appendPending(SpatialHeatInput.Builder pending, TiledThermalField field, CaptureOverlay overlay, SpatialEnergyBanks.Address address,
            long from, long to, double joules, double[] removed) {
        int stateId = address.materialGeneration() < 0 ? -1 : field.currentMaterialStateId(address.position(), address.materialGeneration());
        if (overlay != null && address.materialGeneration() >= 0) stateId = overlay.materialStateId(address, stateId);
        boolean air = address.materialGeneration() >= 0 || (overlay == null ? field.airReceiverMayRemain(address.position())
                : overlay.airMayRemain(address, field.airReceiverMayRemain(address.position())));
        if (address.materialGeneration() >= 0 ? stateId < 0 : !air) {
            removed[0] += joules; return;
        }
        byte face = address.materialGeneration() < 0 ? (byte) address.face() : SpatialHeatInput.MATERIAL;
        int count = address.materialGeneration() < 0 && face < 0 ? 6 : 1;
        double remaining = joules;
        for (int part = 0; part < count; part++) {
            double amount = part == count - 1 ? remaining : joules / count; remaining -= amount;
            var target = new SpatialHeatInput.Target(address.position(), count == 1 ? face : (byte) part, -1, 0, 0, 0, 0, 0, stateId);
            pending.appendAggregate(target, from, to, amount);
        }
    }

    /** Restore only actual captured matter; ordinary replacements do not inherit old H. */
    public boolean restoreSection(long section, DormantChunkThermalState saved, TiledThermalField field,
            long tick, double halfLifeSeconds) {
        if (saved == null || !field.snapshot(section, scratch)) return false;
        var air = saved.air(SectionPos.y(section));
        var bodies = saved.materials(SectionPos.y(section));
        double rate = DormantThermalCooling.rate(halfLifeSeconds);
        int x0 = SectionPos.x(section) << 4, y0 = SectionPos.y(section) << 4, z0 = SectionPos.z(section) << 4;
        for (int cell = 0; cell < 4096; cell++) {
            int signature = scratch.signatures[cell];
            if (!profiles.signatures().valid(signature)) continue;
            if (profiles.signatures().isAir(signature)) {
                if (air == null) continue;
                int brick = (cell & 15) >>> 2 | (cell >>> 4 & 15) >>> 2 << 2 | (cell >>> 8) >>> 2 << 4;
                if (!air.hasBrick(brick)) continue;
                double temperature = air.spatialAir() == null
                        ? air.meanTemperatureC(brick, scratch.ambientC, DormantThermalCooling.factor(air.savedGameTick(), tick, rate))
                        : air.spatialAir().sampleC(x0 + (cell & 15) + .5, y0 + (cell >>> 8) + .5, z0 + (cell >>> 4 & 15) + .5,
                                scratch.ambientC, tick, rate, air.savedGameTick());
                if (Double.isFinite(temperature)) scratch.enthalpyJ[cell] = temperature * profiles.tuning().airHeatCapacityJPerBlockK();
            } else if (bodies != null) {
                int index = bodies.find(cell);
                if (index < 0) continue;
                var profile = profiles.materials().profileOrNull(profiles.signatures().materialProfileId(signature));
                if (profile == null) continue;
                if (!bodies.readMatchingState(index, profiles.signatures().materialStateId(signature), profile.thermalLaw(),
                        tick, scratch.ambientC, rate, material)) continue;
                scratch.enthalpyJ[cell] = material.enthalpyJ(); scratch.branches[cell] = material.branch();
            }
        }
        return field.restoreValues(section, scratch.enthalpyJ, scratch.branches, air == null ? null : air.spatialAir(),
                air == null ? tick : air.savedGameTick(), tick, halfLifeSeconds);
    }

    /** Returns the existing finite replay intervals; the source integrator schedules them once. */
    public PendingHeatCheckpoint.Restored pendingInputs(PendingHeatCheckpoint saved, long tick, double halfLifeSeconds) {
        return saved.restore(0, saved.size(), tick, halfLifeSeconds, 0);
    }

    @Override public void close() { scratch = null; materialScratch = null; scratchCredit.close(); }
}
