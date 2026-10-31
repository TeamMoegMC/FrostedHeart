/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import it.unimi.dsi.fastutil.longs.Long2DoubleLinkedOpenHashMap;

/** Bounded physical-time and environment controls. All unconsumed J belongs to SpatialHeatStore. */
final class RegionPending {
    record Environment(double windScale, long[] sections, double[] naturalC) {
        static final Environment UNCHANGED = new Environment(Double.NaN, new long[0], new double[0]);
    }
    static final class Input {
        final long sequence, fromTick, toTick;
        final Environment environment;
        final boolean aggregated;
        Input(long sequence, long fromTick, long toTick, Environment environment, boolean aggregated) {
            this.sequence = sequence; this.fromTick = fromTick; this.toTick = toTick;
            this.environment = environment; this.aggregated = aggregated;
        }
    }
    private Input first, second, frozen;
    private final Long2DoubleLinkedOpenHashMap olderEnvironment = new Long2DoubleLinkedOpenHashMap();
    private long oldFrom = -1, oldTo = -1, oldSequence, receivedThrough = -1, receivedSequence = -1;
    private double oldWind = Double.NaN;
    private int oldEnvironmentCapacity;
    private java.util.function.LongConsumer retentionChanged = ignored -> { };
    private long accountedBytes;

    void retentionListener(java.util.function.LongConsumer changed) { retentionChanged = changed; noteRetention(); }
    long retainedBytes() {
        return 32L * oldEnvironmentCapacity + 16L * olderEnvironment.size()
                + inputBytes(first) + inputBytes(second) + inputBytes(frozen);
    }
    private static long inputBytes(Input input) {
        return input == null ? 0 : 128L + 16L * input.environment.sections().length;
    }
    void retireRetention() {
        retentionChanged.accept(-accountedBytes); accountedBytes = 0; retentionChanged = ignored -> { };
    }
    private void noteRetention() {
        long next = retainedBytes(); retentionChanged.accept(next - accountedBytes); accountedBytes = next;
    }
    long throughTick(long acceptedTick) { return Math.max(acceptedTick, receivedThrough); }
    boolean hasPending() { return frozen != null || oldFrom >= 0 || first != null; }
    boolean hasAggregatedInput() { return oldFrom >= 0 || frozen != null && frozen.aggregated
            || first != null && first.aggregated || second != null && second.aggregated; }
    long firstPendingTick() {
        long tick = oldFrom < 0 ? Long.MAX_VALUE : oldFrom + (oldFrom == oldTo ? 0 : 1);
        if (frozen != null) tick = Math.min(tick, frozen.fromTick + (frozen.fromTick == frozen.toTick ? 0 : 1));
        if (first != null) tick = Math.min(tick, first.fromTick + (first.fromTick == first.toTick ? 0 : 1));
        if (second != null) tick = Math.min(tick, second.fromTick + (second.fromTick == second.toTick ? 0 : 1));
        return tick;
    }
    boolean hasControlThrough(long tick, long sequence) {
        return controlDue(frozen, tick, sequence) || controlDue(first, tick, sequence) || controlDue(second, tick, sequence)
                || oldFrom == tick && oldTo == tick && oldSequence <= sequence;
    }
    private static boolean controlDue(Input input, long tick, long sequence) {
        return input != null && input.fromTick == tick && input.toTick == tick && input.sequence <= sequence;
    }
    void receive(long sequence, long fromTick, long toTick, Environment environment) {
        if (sequence <= receivedSequence) return;
        if (receivedThrough >= 0 && fromTick != receivedThrough)
            throw new IllegalStateException("Regional input has a missing time interval");
        if (second != null) { summarize(first); first = second; second = null; }
        var next = new Input(sequence, fromTick, toTick, environment, false);
        if (first == null) first = next; else second = next;
        receivedThrough = toTick; receivedSequence = sequence; noteRetention();
    }
    Input freeze(long through, long sequence) {
        if (frozen != null) { var result = limitFrozen(through, sequence); noteRetention(); return result; }
        if (oldFrom >= 0 && (first == null || oldFrom <= first.fromTick)) {
            frozen = olderInput(); olderEnvironment.clear(); oldFrom = oldTo = -1; oldWind = Double.NaN;
        } else { frozen = first; first = second; second = null; }
        var result = limitFrozen(through, sequence); noteRetention(); return result;
    }
    private Input olderInput() {
        return new Input(oldSequence, oldFrom, oldTo, new Environment(oldWind,
                olderEnvironment.keySet().toLongArray(), olderEnvironment.values().toDoubleArray()), true);
    }
    private Input limitFrozen(long through, long sequence) {
        if (frozen != null && frozen.fromTick == through && frozen.toTick == through && frozen.sequence > sequence) return null;
        if (frozen == null || frozen.toTick <= through) return frozen;
        if (frozen.fromTick >= through) return null;
        var full = frozen;
        summarize(new Input(full.sequence, through, full.toTick, full.environment, full.aggregated));
        return frozen = new Input(full.sequence, full.fromTick, through, Environment.UNCHANGED, full.aggregated);
    }
    Input frozen() { return frozen; }
    void accepted(Input input) {
        if (input != frozen) throw new IllegalStateException("A thermal commit does not own its time prefix");
        frozen = null; noteRetention();
    }
    void receiveTransferred(Input input) {
        summarize(input);
        receivedThrough = Math.max(receivedThrough, input.toTick);
        receivedSequence = Math.max(receivedSequence, input.sequence); noteRetention();
    }
    java.util.List<Input> snapshotInputs() {
        var result = new java.util.ArrayList<Input>(4);
        if (frozen != null) result.add(frozen);
        if (oldFrom >= 0) result.add(olderInput());
        if (first != null) result.add(first);
        if (second != null) result.add(second);
        return result;
    }
    private void summarize(Input input) {
        boolean latest = input.toTick >= oldTo;
        oldFrom = oldFrom < 0 ? input.fromTick : Math.min(oldFrom, input.fromTick);
        oldTo = Math.max(oldTo, input.toTick); oldSequence = Math.max(oldSequence, input.sequence);
        if (!Double.isNaN(input.environment.windScale()) && (latest || Double.isNaN(oldWind))) oldWind = input.environment.windScale();
        for (int index = 0; index < input.environment.sections().length; index++)
            if (latest || !olderEnvironment.containsKey(input.environment.sections()[index]))
                olderEnvironment.put(input.environment.sections()[index], input.environment.naturalC()[index]);
        oldEnvironmentCapacity = Math.max(oldEnvironmentCapacity, olderEnvironment.size());
    }
    void clear() {
        first = second = frozen = null; olderEnvironment.clear();
        oldFrom = oldTo = receivedThrough = receivedSequence = -1; oldWind = Double.NaN; noteRetention();
    }
}
