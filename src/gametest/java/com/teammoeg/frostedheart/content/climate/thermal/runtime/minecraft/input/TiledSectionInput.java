/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Historical captured-change fixture; production uses the original Page owner.
 * Bounded block changes for isolated core tests. It contains no
 * Level, Chunk, runtime, frame or callback reference. Repeated edits coalesce
 * per cell while retaining removal/replacement and phase ownership semantics.
 */
public final class TiledSectionInput {
    public final long section;
    private final long[] dirty = new long[64], replaced = new long[64], phase = new long[64];
    private byte[] brickSlots = new byte[64], brickIds;
    private int[] stateIds;
    private ThermalMemoryBudget.Reservation credit;
    private boolean valid = true, fullCapture, fullReplacement, worldRefresh;

    public static long reservedBytes(long bricks) { return 2304L + Long.bitCount(bricks) * 64L * Integer.BYTES; }

    /** New/old overlap has already been reserved; previous is copied, never retained. */
    public TiledSectionInput(long section, long bricks, int[] capturedStateIds, TiledSectionInput previous,
            ThermalMemoryBudget.Reservation credit) {
        this.section = section; this.credit = credit;
        int count = Long.bitCount(bricks); stateIds = new int[count * 64]; brickIds = new byte[count];
        Arrays.fill(brickSlots, (byte) -1);
        int ordinal = 0; long remaining = bricks;
        while (remaining != 0) {
            int brick = Long.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
            brickSlots[brick] = (byte) ordinal; brickIds[ordinal++] = (byte) brick;
        }
        if (previous == null) {
            for (int cell = 0; cell < stateIds.length; cell++) stateIds[cell] = capturedStateIds[pageCell(cell)];
        } else synchronized (previous) {
            for (int cell = 0; cell < stateIds.length; cell++) {
                int local = pageCell(cell), old = previous.index(local);
                stateIds[cell] = old < 0 ? capturedStateIds[local] : previous.stateIds[old];
            }
            System.arraycopy(previous.dirty, 0, dirty, 0, 64); System.arraycopy(previous.replaced, 0, replaced, 0, 64);
            System.arraycopy(previous.phase, 0, phase, 0, 64);
            fullCapture = previous.fullCapture; fullReplacement = previous.fullReplacement; worldRefresh = previous.worldRefresh;
        }
    }

    public synchronized void blockStateChanged(int x, int y, int z, BlockState previous, BlockState next) {
        if (!valid || previous == next) return;
        int cell = x | z << 4 | y << 8, word = cell >>> 6; long bit = 1L << (cell & 63);
        int index = index(cell);
        if (index < 0) return;
        stateIds[index] = Block.BLOCK_STATE_REGISTRY.getId(next);
        byte cause = MinecraftPhaseController.materialChangeCause(previous, next,
                SectionPos.sectionToBlockCoord(SectionPos.x(section)) + x,
                SectionPos.sectionToBlockCoord(SectionPos.y(section)) + y,
                SectionPos.sectionToBlockCoord(SectionPos.z(section)) + z);
        dirty[word] |= bit;
        if (cause == MaterialChanges.REPLACE) replaced[word] |= bit;
        else if (cause == MaterialChanges.THERMAL_TRANSITION || cause == MaterialChanges.GAMEPLAY_TRANSITION) phase[word] |= bit;
    }

    public synchronized void containerReplaced() {
        if (valid) fullCapture = fullReplacement = worldRefresh = true;
    }

    /** Re-entering the same saved world is a recapture, not fresh matter. */
    public synchronized void recapture() { if (valid) fullCapture = worldRefresh = true; }

    /** Only values survive unload, including raw container replacement before its next epoch. */
    public synchronized void retain(LevelChunkSection world) {
        if (!valid || !worldRefresh || world == null) return;
        for (int cell = 0; cell < stateIds.length; cell++) {
            int local = pageCell(cell);
            stateIds[cell] = Block.BLOCK_STATE_REGISTRY.getId(world.getBlockState(local & 15, local >>> 8, local >>> 4 & 15));
        }
        worldRefresh = false;
    }

    public synchronized int stateId(int cell) {
        int index = valid ? index(cell) : -1;
        return index < 0 || worldRefresh ? -1 : stateIds[index];
    }

    public synchronized boolean dirty() {
        if (fullCapture) return true;
        for (long word : dirty) if (word != 0) return true;
        return false;
    }

    public synchronized boolean changed(int cell) { return fullCapture || (dirty[cell >>> 6] & 1L << (cell & 63)) != 0; }
    public synchronized boolean replaced(int cell) { return fullReplacement || (replaced[cell >>> 6] & 1L << (cell & 63)) != 0; }

    /** Called at a main-thread epoch boundary; no world reference is retained. */
    public synchronized int capture(LevelChunkSection world, MinecraftThermalProfiles.Snapshot profiles, TiledThermalField field) {
        if (!valid || !field.idle()) return 0;
        retain(world);
        if (worldRefresh) return 0;
        int count = 0;
        for (int word = 0; word < 64; word++) {
            long remaining = fullCapture ? -1L : dirty[word];
            while (remaining != 0) {
                int bit = Long.numberOfTrailingZeros(remaining), cell = word * 64 + bit; remaining &= remaining - 1;
                int index = index(cell);
                if (index < 0) continue;
                var state = Block.BLOCK_STATE_REGISTRY.byId(stateIds[index]);
                if (field.changeCell(position(cell), profiles.states().signatureId(state), change(cell))) count++;
            }
        }
        clear();
        return count;
    }

    /** A save can project edits even while the sole writer holds an older cut. */
    public synchronized void project(LevelChunkSection world, MinecraftThermalProfiles.Snapshot profiles,
            TiledThermalField field, TiledThermalField.SectionSnapshot values) {
        if (!valid) return;
        retain(world);
        if (worldRefresh) return;
        for (int word = 0; word < 64; word++) {
            long remaining = fullCapture ? -1L : dirty[word];
            while (remaining != 0) {
                int bit = Long.numberOfTrailingZeros(remaining), cell = word * 64 + bit; remaining &= remaining - 1;
                int index = index(cell);
                if (index < 0) continue;
                field.projectSnapshotCell(values, cell,
                        profiles.states().signatureId(Block.BLOCK_STATE_REGISTRY.byId(stateIds[index])), change(cell));
            }
        }
    }

    private TiledThermalField.Change change(int cell) {
        int word = cell >>> 6; long bit = 1L << (cell & 63);
        return fullReplacement || (replaced[word] & bit) != 0 ? TiledThermalField.Change.REPLACEMENT
                : (phase[word] & bit) != 0 ? TiledThermalField.Change.PHASE : TiledThermalField.Change.PHYSICAL;
    }

    private int index(int cell) {
        int brick = (cell & 15) >>> 2 | (cell >>> 4 & 15) >>> 2 << 2 | (cell >>> 8) >>> 2 << 4;
        int ordinal = brickSlots[brick];
        return ordinal < 0 ? -1 : ordinal * 64 + (cell & 3) + (cell >>> 4 & 3) * 4 + (cell >>> 8 & 3) * 16;
    }
    private int pageCell(int index) { return BlockBrickLayout.pageBlock(Byte.toUnsignedInt(brickIds[index >>> 6]), index & 63); }
    private long position(int cell) {
        return BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(section)) + (cell & 15),
                SectionPos.sectionToBlockCoord(SectionPos.y(section)) + (cell >>> 8),
                SectionPos.sectionToBlockCoord(SectionPos.z(section)) + (cell >>> 4 & 15));
    }
    private void clear() { Arrays.fill(dirty, 0); Arrays.fill(replaced, 0); Arrays.fill(phase, 0); fullCapture = fullReplacement = worldRefresh = false; }
    public synchronized void invalidate() {
        valid = false; clear(); stateIds = null; brickSlots = brickIds = null;
        if (credit != null) { credit.close(); credit = null; }
    }
}
