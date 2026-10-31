/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;

/**
 * An accepted producer window owned by SpatialHeatStore. Its remaining port J may
 * wait for geometry, but its producer slot and arrays have already been returned.
 * Only the dimension coordinator advances this record.
 */
public final class SourceInputWindow {
    public static final int MAXIMUM_OWNED_WINDOWS = 2;
    public final SpatialSourceTimeline.Descriptor descriptor;
    public final long fromTick, toTick, boundaryTick, sequence;
    public final int ordinal, size;
    public final boolean registration, unload, controlsChanged, enabled;
    public final double finalPowerW;
    private final SpatialSourceTimeline.Span[] spans;
    private final SpatialHeatInput.Target[] origins;
    private final double[] remainingJ;
    private final double[] exportedJ;
    private final int routedMask;
    private final int routedPorts;
    private final int ports;
    private long through;
    private boolean first = true, complete;
    private double pendingJ;

    SourceInputWindow(SpatialSourceTimeline.Window producer, long sequence, int ordinal,
            SpatialHeatInput.Target[] origins) {
        descriptor = producer.descriptor;
        fromTick = through = producer.fromTick; toTick = producer.toTick;
        boundaryTick = producer.boundaryTick; this.sequence = sequence; this.ordinal = ordinal;
        registration = producer.registration; unload = producer.unload;
        controlsChanged = producer.controlsChanged;
        enabled = producer.enabled; finalPowerW = producer.finalPowerW; size = producer.size;
        ports = descriptor.ports().length; this.origins = origins;
        int routes = 0;
        for (int port = 0; port < ports; port++) if (origins[port] != null) routes |= 1 << port;
        routedMask = routes;
        routedPorts = Integer.bitCount(routes);
        spans = new SpatialSourceTimeline.Span[size];
        remainingJ = new double[Math.multiplyExact(size, routedPorts)];
        exportedJ = new double[remainingJ.length];
        for (int index = 0; index < size; index++) spans[index] = producer.span(index);
    }

    /** All arrays and destination capacity exist before producer counters are touched. */
    void accept(SpatialSourceTimeline.Window producer, ThermalSourceLedger ledger) {
        for (int index = 0; index < size; index++) {
            var span = spans[index];
            double assigned = 0;
            for (int port = 0; port < ports; port++) {
                if (span.port() >= 0 && descriptor.ports()[port].portId() != span.port()) continue;
                double share = span.port() >= 0 ? span.joules()
                        : port == ports - 1 ? span.joules() - assigned : span.joules() * descriptor.ports()[port].powerShare();
                assigned += share;
                double kept = producer.acceptPort(port, share);
                ledger.acceptSpatialEnergy(kept);
                if (origins[port] == null) ledger.confirmRecordedLoss(kept, false);
                else {
                    remainingJ[index * routedPorts + routeOffset(port)] = share;
                    exportedJ[index * routedPorts + routeOffset(port)] = share - kept;
                    pendingJ += kept;
                }
            }
        }
        producer.acceptedThrough(toTick);
    }

    public SpatialSourceTimeline.Span span(int index) { return spans[index]; }
    public boolean first() { return first; }
    public long through() { return through; }
    public boolean complete() { return complete; }
    public boolean canBind(long tick) { return !complete && (first ? tick >= through : tick > through); }
    public boolean hasEventThrough(long tick) {
        if (registration || controlsChanged && boundaryTick <= tick) return true;
        for (var span : spans) if (span.fromTick() == span.toTick() && span.toTick() <= tick) return true;
        return false;
    }
    public long nextControlTick() {
        if (first && registration) return fromTick;
        long next = controlsChanged && boundaryTick > through ? boundaryTick : toTick;
        for (var span : spans) if (span.port() < 0 && !Double.isNaN(span.declaredPowerW()) && span.fromTick() > through)
            next = Math.min(next, span.fromTick());
        return next;
    }
    /** The next control that binding has not applied; continuous heat alone creates no mixing fence. */
    public long pendingControlTick() {
        if (complete || !controlsChanged) return Long.MAX_VALUE;
        if (first && registration) return fromTick;
        if (first && boundaryTick <= through) return through;
        return nextControlTick();
    }
    public double pendingEnergyJ() { return pendingJ; }
    boolean hasHeat() {
        for (int index = 0; index < remainingJ.length; index++) if (remainingJ[index] != exportedJ[index]) return true;
        return false;
    }
    public int portCount() { return ports; }
    public SpatialHeatInput.Target origin(int port) { return origins[port]; }
    private int routeOffset(int port) { return Integer.bitCount(routedMask & ((1 << port) - 1)); }

    /** Fractional prefixes retain their remainder, including the final rounding tail. */
    public double take(int record, int port, long from, long to) {
        if (origins[port] == null) return 0; // Declared losses were settled at acceptance.
        var span = spans[record]; int offset = record * routedPorts + routeOffset(port);
        double old = remainingJ[offset];
        double amount = to == span.toTick() ? old : old * ((double) (to - from) / (span.toTick() - from));
        remainingJ[offset] = old - amount;
        double skipped = Math.copySign(Math.min(Math.abs(amount), Math.abs(exportedJ[offset])), amount);
        exportedJ[offset] -= skipped;
        double kept = amount - skipped; pendingJ -= kept;
        return kept;
    }
    public void boundThrough(long tick) { through = tick; first = false; complete = tick == toTick; }
    public double declaredPowerAt(long tick) {
        if (tick == toTick) return finalPowerW;
        for (var span : spans) if (span.port() < 0 && span.fromTick() <= tick && tick < span.toTick())
            return Double.isNaN(span.declaredPowerW()) ? finalPowerW : span.declaredPowerW();
        return finalPowerW;
    }
    public boolean enabledAt(long tick) {
        if (tick == toTick) return enabled;
        for (var span : spans) if (span.port() < 0 && span.fromTick() <= tick && tick < span.toTick())
            return Double.isNaN(span.declaredPowerW()) ? enabled : span.enabled();
        return false;
    }

    void collectChunk(long chunk, SpatialHeatStore.RecordConsumer consumer) {
        for (int port = 0; port < ports; port++) {
            var target = origins[port];
            if (target == null || net.minecraft.world.level.ChunkPos.asLong(
                    net.minecraft.core.BlockPos.getX(target.position()) >> 4,
                    net.minecraft.core.BlockPos.getZ(target.position()) >> 4) != chunk) continue;
            for (int index = 0; index < size; index++) {
                int offset = index * routedPorts + routeOffset(port);
                double joules = remainingJ[offset] - exportedJ[offset];
                if (joules == 0) continue;
                var span = spans[index];
                consumer.accept(target, Math.min(span.toTick(), Math.max(through, span.fromTick())), span.toTick(), joules);
            }
        }
    }
    double exportChunk(long chunk, java.util.function.ObjDoubleConsumer<SpatialHeatInput.Target> settle) {
        double exported = 0;
        for (int port = 0; port < ports; port++) {
            var target = origins[port];
            if (target == null || net.minecraft.world.level.ChunkPos.asLong(
                    net.minecraft.core.BlockPos.getX(target.position()) >> 4,
                    net.minecraft.core.BlockPos.getZ(target.position()) >> 4) != chunk) continue;
            double amount = 0;
            for (int index = 0; index < size; index++) {
                int offset = index * routedPorts + routeOffset(port);
                amount += remainingJ[offset] - exportedJ[offset]; remainingJ[offset] = exportedJ[offset] = 0;
            }
            pendingJ -= amount; exported += amount; settle.accept(target, amount);
        }
        return exported;
    }
    public long retainedBytes() { return storageBytes(size, ports, routedPorts); }
    public static long storageBytes(int records, int ports, int routedPorts) {
        return 384L + AirBackingMeter.arrayPayloadBytes(records, 8) + 80L * records
                + 2 * AirBackingMeter.arrayPayloadBytes(Math.multiplyExact(records, routedPorts), 8)
                + AirBackingMeter.arrayPayloadBytes(ports, 8) + 160L * ports;
    }
}
