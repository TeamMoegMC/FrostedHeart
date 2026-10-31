/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import java.util.LinkedHashMap;
import java.util.function.LongToIntFunction;

/** Finite, signed, unconsumed energy in world coordinates. Contains no live solver addresses. */
public final class PendingHeatCheckpoint {
    public static final PendingHeatCheckpoint EMPTY = new PendingHeatCheckpoint(SpatialHeatInput.EMPTY);
    private final SpatialHeatInput input;
    private final long referenceTick;
    private PendingHeatCheckpoint(SpatialHeatInput input) { this(input, Long.MAX_VALUE); }
    private PendingHeatCheckpoint(SpatialHeatInput input, long referenceTick) { this.input = input; this.referenceTick = referenceTick; }
    public int size() { return input.size(); }
    public long retainedBytes() { return input.retainedBytes(); }
    public boolean empty() { return input.size() == 0; }
    public long position(int index) { return input.position(index); }

    public static PendingHeatCheckpoint capture(SpatialHeatInput source, LongToIntFunction materialState, long referenceTick) {
        if (source.size() == 0) return EMPTY;
        SpatialHeatInput.Builder changed = null;
        for (int index = 0; index < source.size(); index++) {
            if (source.isAir(index) || source.materialStateId(index) >= 0) continue;
            changed = new SpatialHeatInput.Builder(); changed.reserve(source.size());
            break;
        }
        if (changed == null) return new PendingHeatCheckpoint(source, referenceTick);
        for (int index = 0; index < source.size(); index++) {
            var old = source.target(index);
            int state = old.materialStateId() >= 0 ? old.materialStateId() : materialState.applyAsInt(old.incarnation());
            changed.appendAt(source, index, new SpatialHeatInput.Target(old.position(), old.face(), old.materialSlot(),
                    old.generation(), old.materialProfile(), old.incarnation(), old.sourceId(), old.sourceGeneration(), state,
                    old.minimumPageGeneration()));
        }
        return new PendingHeatCheckpoint(changed.freeze(), referenceTick);
    }
    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        if (referenceTick != Long.MAX_VALUE) tag.putLong("reference_tick", referenceTick);
        int size = input.size();
        long[] positions = new long[size], from = new long[size], to = new long[size], energy = new long[size];
        byte[] faces = new byte[size]; int[] states = new int[size];
        var paletteIndexes = new LinkedHashMap<Integer, Integer>();
        var palette = new ListTag();
        for (int index = 0; index < size; index++) {
            positions[index] = input.position(index); from[index] = input.fromTick(index); to[index] = input.toTick(index);
            energy[index] = Double.doubleToRawLongBits(input.energyJ(index)); faces[index] = input.face(index);
            int stateId = input.materialStateId(index); states[index] = -1;
            if (!input.isAir(index) && stateId >= 0) {
                Integer selected = paletteIndexes.get(stateId);
                if (selected == null) {
                    selected = paletteIndexes.size(); paletteIndexes.put(stateId, selected);
                    palette.add(NbtUtils.writeBlockState(Block.BLOCK_STATE_REGISTRY.byId(stateId)));
                }
                states[index] = selected;
            }
        }
        tag.putLongArray("positions", positions); tag.putLongArray("from", from); tag.putLongArray("to", to);
        tag.putLongArray("joules", energy); tag.putByteArray("faces", faces); tag.putIntArray("states", states);
        tag.put("palette", palette);
        return tag;
    }
    public static PendingHeatCheckpoint decode(CompoundTag tag) {
        long[] positions = tag.getLongArray("positions"), from = tag.getLongArray("from"), to = tag.getLongArray("to"), energy = tag.getLongArray("joules");
        byte[] faces = tag.getByteArray("faces"); int[] states = tag.getIntArray("states");
        int size = positions.length;
        if (from.length != size || to.length != size || energy.length != size || faces.length != size || states.length != size) return EMPTY;
        ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
        int[] stateIds = new int[palette.size()];
        for (int index = 0; index < stateIds.length; index++) stateIds[index] = Block.getId(
                NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(index)));
        var builder = new SpatialHeatInput.Builder(); builder.reserve(size);
        for (int index = 0; index < size; index++) {
            double joules = Double.longBitsToDouble(energy[index]);
            if (!Double.isFinite(joules) || from[index] < 0 || to[index] < from[index] || faces[index] < -1 || faces[index] >= 12) continue;
            int state = states[index] >= 0 && states[index] < stateIds.length ? stateIds[states[index]] : -1;
            builder.appendAggregate(new SpatialHeatInput.Target(positions[index], faces[index], -1, 0, 0, 0, 0, 0, state),
                    from[index], to[index], joules);
        }
        return new PendingHeatCheckpoint(builder.freeze(), tag.contains("reference_tick") ? tag.getLong("reference_tick") : Long.MAX_VALUE);
    }

    public record Restored(SpatialHeatInput heat, double originalJ, double coolingLossJ, double unacceptedJ) { }
    /** One restore fragment fits the existing intake slice; offline production is never invented. */
    public Restored restore(int start, int end, long tick, double halfLifeSeconds, long minimumPageGeneration) {
        var builder = new SpatialHeatInput.Builder(); builder.reserve(end - start);
        double original = 0, loss = 0, unaccepted = 0;
        double rate = DormantThermalCooling.rate(halfLifeSeconds);
        for (int index = start; index < end; index++) {
            double joules = input.energyJ(index); original += joules;
            long middle = input.fromTick(index) + (input.toTick(index) - input.fromTick(index)) / 2;
            double remaining = joules * DormantThermalCooling.factor(Math.min(middle, referenceTick), tick, rate); loss += joules - remaining;
            if (!input.isAir(index) && input.materialStateId(index) < 0) { unaccepted += remaining; continue; }
            var target = new SpatialHeatInput.Target(input.position(index), input.face(index), -1, 0, 0, 0,
                    0, 0, input.materialStateId(index), minimumPageGeneration);
            // A finite delivery interval avoids a temperature jump from dumping a
            // large archive into one zero-duration publication. It contains only
            // energy already produced before saving, not additional source power.
            long duration = Math.min(160, Math.max(20, input.toTick(index) - input.fromTick(index)));
            builder.appendAggregate(target, tick, tick + duration, remaining);
        }
        return new Restored(builder.freeze(), original, loss, unaccepted);
    }
    public void collectRemainingForSave(int start, long tick, double halfLifeSeconds,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore.RecordConsumer consumer) {
        double rate = DormantThermalCooling.rate(halfLifeSeconds);
        for (int index = start; index < size(); index++) {
            long middle = input.fromTick(index) + (input.toTick(index) - input.fromTick(index)) / 2;
            double joules = input.energyJ(index) * DormantThermalCooling.factor(Math.min(middle, referenceTick), tick, rate);
            long duration = Math.min(160, Math.max(20, input.toTick(index) - input.fromTick(index)));
            if (joules != 0) consumer.accept(input.target(index), tick, tick + duration, joules);
        }
    }
}
