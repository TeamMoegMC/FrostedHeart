/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/** Exclusive active-time accounting for thermal execution and its helpers. */
public final class ThermalWorkBudget implements AutoCloseable {
    @jdk.jfr.Name("frostedheart.RegionalThermalWork")
    @jdk.jfr.Label("Regional thermal active work")
    @jdk.jfr.StackTrace(false)
    private static final class WorkObservation extends jdk.jfr.Event {
        public long targetTick, regionId, epoch, wallNanos, activeNanos;
        public int gridStep;
        public String stage, outcome, phases;
    }
    public enum Phase { SOURCE, GEOMETRY, PARTITION, AIR, CONTACTS, OPERATOR, PROJECTION, TRANSPORT,
        MATERIAL, MATERIAL_INTERNAL, MATERIAL_CONTACT, MATERIAL_STATE, ACCEPT, PUBLICATION }
    private static final ThreadLocal<ThermalWorkBudget> ACTIVE = new ThreadLocal<>();
    public static boolean automatic() {
        return (com.teammoeg.frostedheart.infrastructure.config.FHConfig.COMMON.THERMAL_RUNTIME.continuousAirOverloadProtection.get());
    }

    /** Immutable capture: a queued helper must not inherit a later phase change. */
    public record Context(Account account, Phase phase) {}

    public static final class Account {
        private final long startedWall = System.nanoTime();
        private final AtomicLong active = new AtomicLong();
        private final AtomicLongArray phases = new AtomicLongArray(Phase.values().length);

        private Account() { }
        /** Sum of execution intervals across threads; time between intervals is excluded. */
        public long activeNanos() { return active.get(); }
        /** Lifetime includes queueing and waiting; only diagnostic output uses it. */
        public long elapsedNanos() { return System.nanoTime() - startedWall; }
        public long activeNanos(Phase phase) { return phases.get(phase.ordinal()); }
        public String summary() {
            StringBuilder out = new StringBuilder("activeNs=").append(activeNanos());
            long exclusive = 0;
            for (Phase phase : Phase.values()) {
                long value = activeNanos(phase); exclusive += value;
                out.append(',').append(phase.name().toLowerCase(java.util.Locale.ROOT)).append("ActiveNs=").append(value);
            }
            return out.append(",exclusiveActiveNs=").append(exclusive).toString();
        }
        private void charge(Phase phase, long nanos) {
            if (nanos <= 0) return;
            phases.addAndGet(phase.ordinal(), nanos);
            active.addAndGet(nanos);
        }
    }

    private final ThermalWorkBudget previous;
    private Context context;
    private long startedWall;

    private ThermalWorkBudget(Account account, Phase phase) {
        previous = ACTIVE.get();
        long now = System.nanoTime();
        if (previous != null) previous.flush(now);
        context = new Context(account, phase);
        startedWall = now;
        ACTIVE.set(this);
    }

    public static Account newAccount() {
        return new Account();
    }

    /** Detailed per-region measurements use the recorder, never synchronous log I/O. */
    public static void recordWork(long tick, long regionId, long epoch, String stage,
            int gridStep, String outcome, long wallNanos, Account account) {
        if (account == null) return;
        var event = new WorkObservation();
        if (!event.isEnabled()) return;
        event.targetTick = tick; event.regionId = regionId; event.epoch = epoch;
        event.stage = stage; event.gridStep = gridStep; event.outcome = outcome;
        event.wallNanos = wallNanos; event.activeNanos = account.activeNanos();
        event.phases = account.summary();
        event.commit();
    }
    public static ThermalWorkBudget enter(Account account, Phase phase) {
        return account == null ? null : new ThermalWorkBudget(account, phase);
    }
    public static Context capture() { var active = ACTIVE.get(); return active == null ? null : active.context; }
    public static ThermalWorkBudget attach(Context context) {
        return context == null ? null : enter(context.account, context.phase);
    }
    public static void phase(Phase phase) {
        var active = ACTIVE.get();
        if (active == null || active.context.phase == phase) return;
        active.flush(System.nanoTime());
        active.context = new Context(active.context.account, phase);
    }

    public static void checkpoint() {
        var active = ACTIVE.get();
        if (active == null) return;
        active.flush(System.nanoTime());
    }
    private void flush(long now) {
        context.account.charge(context.phase, Math.max(0, now - startedWall));
        startedWall = now;
    }

    @Override public void close() {
        if (ACTIVE.get() != this) return;
        long now = System.nanoTime();
        flush(now);
        if (previous == null) ACTIVE.remove();
        else { previous.startedWall = now; ACTIVE.set(previous); }
    }
}
