/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.ThermalRetentionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Immutable cell temperatures for one Section. No runtime addresses or old field layouts. */
public final class AirFieldCheckpoint {
    private record Brick(long blocks, double[] temperaturesC, long sampleTick, double minimumC, double maximumC) {
        static Brick capture(long blocks, double[] values, long tick) {
            double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
            for (double value : values) { low = Math.min(low, value); high = Math.max(high, value); }
            return new Brick(blocks, values, tick, low, high);
        }
        double at(int cell) {
            return (blocks & 1L << cell) == 0 ? Double.NaN
                    : temperaturesC[Long.bitCount(blocks & ((1L << cell) - 1))];
        }
    }
    private final int minX, minY, minZ;
    private final Brick[] bricks;
    private final long brickMask;
    private AirFieldCheckpoint(int x, int y, int z, Brick[] bricks) {
        minX = x; minY = y; minZ = z; this.bricks = bricks;
        long mask = 0;
        for (int brick = 0; brick < 64; brick++) if (bricks[brick] != null) mask |= 1L << brick;
        brickMask = mask;
    }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public long brickMask() { return brickMask; }
    public void recordBacking(AirBackingMeter meter) {
        meter.bytes(64); meter.array(bricks);
        for (var brick : bricks) if (brick != null && meter.objectOnce(brick, 64)) meter.array(brick.temperaturesC);
    }
    public static final class Capture {
        private final double naturalC;
        private final Brick[] bricks = new Brick[64];
        private int x, y, z;
        private long tick = -1;
        private boolean present;
        public Capture(double naturalC) { this.naturalC = naturalC; }
        public Capture atTick(long tick) { this.tick = tick; return this; }
        /** Caller transfers this newly captured, compact cell array. */
        public void addCellBrick(int x, int y, int z, int brick, long blocks, double[] temperaturesC) {
            if (blocks == 0) return;
            var value = Brick.capture(blocks, temperaturesC, tick);
            if (Double.isFinite(naturalC) && !ThermalRetentionPolicy.retains(Math.max(Math.abs(value.minimumC - naturalC), Math.abs(value.maximumC - naturalC)))) return;
            this.x = x; this.y = y; this.z = z; bricks[brick] = value; present = true;
        }
        public AirFieldCheckpoint finish() { return present ? new AirFieldCheckpoint(x, y, z, bricks) : null; }
    }
    private int brickAt(double x, double y, double z) {
        int lx = (int) Math.floor(x) - minX, ly = (int) Math.floor(y) - minY, lz = (int) Math.floor(z) - minZ;
        if (lx < 0 || lx >= 16 || ly < 0 || ly >= 16 || lz < 0 || lz >= 16) return -1;
        return lx >>> 2 | (lz >>> 2) << 2 | (ly >>> 2) << 4;
    }
    public double sampleC(double x, double y, double z) {
        int brick = brickAt(x, y, z);
        if (brick < 0 || bricks[brick] == null) return Double.NaN;
        int block = ((int) Math.floor(x) & 3) | (((int) Math.floor(z) & 3) << 2) | (((int) Math.floor(y) & 3) << 4);
        return bricks[brick].at(block);
    }
    public double blockCenterC(int brick, int block) { return bricks[brick] == null ? Double.NaN : bricks[brick].at(block); }
    public long sampleTickAt(double x, double y, double z, long fallback) {
        int brick = brickAt(x, y, z);
        if (brick < 0 || bricks[brick] == null || !Double.isFinite(sampleC(x, y, z))) return fallback;
        return bricks[brick].sampleTick < 0 ? fallback : bricks[brick].sampleTick;
    }
    public double sampleC(double x, double y, double z, double natural, long tick, double rate, long fallbackTick) {
        double saved = sampleC(x, y, z);
        return Double.isFinite(saved) ? DormantThermalCooling.temperature(saved, natural,
                DormantThermalCooling.factor(sampleTickAt(x, y, z, fallbackTick), tick, rate)) : Double.NaN;
    }
    public double blockCenterC(int brick, int block, double natural, long tick, double rate, long fallbackTick) {
        var value = bricks[brick];
        if (value == null) return Double.NaN;
        double saved = value.at(block);
        return Double.isFinite(saved) ? DormantThermalCooling.temperature(saved, natural,
                DormantThermalCooling.factor(value.sampleTick < 0 ? fallbackTick : value.sampleTick, tick, rate)) : Double.NaN;
    }
    public double meanC(int brick) {
        var value = bricks[brick];
        if (value == null) return Double.NaN;
        double sum = 0; for (double temperature : value.temperaturesC) sum += temperature;
        return sum / value.temperaturesC.length;
    }
    public double meanC(int brick, double natural, long tick, double rate, long fallbackTick) {
        var value = bricks[brick];
        return value == null ? Double.NaN : DormantThermalCooling.temperature(meanC(brick), natural,
                DormantThermalCooling.factor(value.sampleTick < 0 ? fallbackTick : value.sampleTick, tick, rate));
    }
    /** Filtering keeps each value's original time; it does not restart dormant cooling. */
    public AirFieldCheckpoint retain(long eligibleBricks, double naturalC, long tick, double rate, long fallbackTick) {
        Brick[] retained = null;
        for (int index = 0; index < 64; index++) {
            var value = bricks[index];
            if (value == null || (eligibleBricks & 1L << index) == 0) continue;
            double factor = DormantThermalCooling.factor(value.sampleTick < 0 ? fallbackTick : value.sampleTick, tick, rate);
            if (ThermalRetentionPolicy.retains(Math.max(Math.abs(value.minimumC - naturalC), Math.abs(value.maximumC - naturalC)) * factor)) continue;
            if (retained == null) retained = bricks.clone();
            retained[index] = null;
        }
        if (retained == null) return this;
        var result = new AirFieldCheckpoint(minX, minY, minZ, retained);
        return result.brickMask == 0 ? null : result;
    }
    /** New captures replace only their own Bricks and preserve individual accepted times. */
    public static AirFieldCheckpoint mergeDated(AirFieldCheckpoint first, long firstTick,
            AirFieldCheckpoint second, long secondTick, long secondBricks) {
        if (first == null && second == null) return null;
        var origin = second == null ? first : second;
        Brick[] merged = new Brick[64];
        for (int brick = 0; brick < 64; brick++) {
            boolean replacement = (secondBricks & 1L << brick) != 0;
            var source = replacement ? second : first;
            if (source == null || source.bricks[brick] == null) continue;
            var value = source.bricks[brick];
            long tick = replacement ? secondTick : firstTick;
            merged[brick] = value.sampleTick >= 0 ? value : new Brick(value.blocks, value.temperaturesC, tick, value.minimumC, value.maximumC);
        }
        var result = new AirFieldCheckpoint(origin.minX, origin.minY, origin.minZ, merged);
        return result.brickMask == 0 ? null : result;
    }
    public CompoundTag encode() {
        var tag = new CompoundTag();
        tag.putLong("origin", BlockPos.asLong(minX, minY, minZ));
        var entries = new ListTag();
        for (int index = 0; index < 64; index++) {
            var brick = bricks[index]; if (brick == null) continue;
            var entry = new CompoundTag();
            entry.putByte("brick", (byte) index); entry.putLong("blocks", brick.blocks); entry.putLong("sample_tick", brick.sampleTick);
            long[] values = new long[brick.temperaturesC.length];
            for (int cell = 0; cell < values.length; cell++) values[cell] = Double.doubleToLongBits(brick.temperaturesC[cell]);
            entry.putLongArray("temperatures", values); entries.add(entry);
        }
        tag.put("bricks", entries); return tag;
    }
    public static AirFieldCheckpoint decode(CompoundTag tag) {
        long origin = tag.getLong("origin");
        Brick[] bricks = new Brick[64];
        var entries = tag.getList("bricks", Tag.TAG_COMPOUND);
        for (int entry = 0; entry < entries.size(); entry++) {
            var data = entries.getCompound(entry); int index = Byte.toUnsignedInt(data.getByte("brick"));
            long blocks = data.getLong("blocks"); long[] bits = data.getLongArray("temperatures");
            if (index >= 64 || bricks[index] != null || blocks == 0 || bits.length != Long.bitCount(blocks)) return null;
            double[] values = new double[bits.length];
            for (int cell = 0; cell < values.length; cell++) {
                values[cell] = Double.longBitsToDouble(bits[cell]); if (!Double.isFinite(values[cell])) return null;
            }
            bricks[index] = Brick.capture(blocks, values, data.getLong("sample_tick"));
        }
        var result = new AirFieldCheckpoint(BlockPos.getX(origin), BlockPos.getY(origin), BlockPos.getZ(origin), bricks);
        return result.brickMask == 0 ? null : result;
    }
}
