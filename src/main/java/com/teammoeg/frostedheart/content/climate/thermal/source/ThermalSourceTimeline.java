/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SolveEpoch;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded source-event timeline and the single logical owner of source state.
 *
 * <p>The main-thread producer offers immutable commands in game-tick order.
 * The dimension owner is the only code that mutates the registry, node power
 * accumulators, and destination enthalpy. This class is also the concrete
 * source input used by the thermal step executor. Event ordering and interval
 * integration are concrete here; the only destination boundary is the final
 * node-enthalpy write.</p>
 */
public final class ThermalSourceTimeline {
    /** A source watermark starts at one, so zero can report a full queue. */
    public static final long OFFER_REJECTED = 0L;

    private final long dimensionGeneration;
    private final int capacity;
    private final long[] eventWatermarks;
    private final SourceCommand[] commands;
    private final AtomicLong readSequence = new AtomicLong();
    private final AtomicLong writeSequence = new AtomicLong();
    private final ThermalSourceRegistry registry;
    private final NodePowerAccumulatorArena accumulators;
    private final ThermalCellArena destination;

    private long nextOfferedWatermark;
    private long lastOfferedTick;
    private long appliedWatermark;
    private long cursorTick;
    private long preAppliedEpochId = -1L;
    private double preAppliedEnergyJ;

    /**
     * Takes exclusive mutation ownership of a fresh source registry.
     */
    public ThermalSourceTimeline(
            long dimensionGeneration,
            long initialTick,
            int eventCapacity,
            ThermalSourceRegistry registry,
            ThermalCellArena destination
    ) {
        if (dimensionGeneration < 0L) {
            throw new IllegalArgumentException("dimensionGeneration must be non-negative");
        }
        requireTick("initialTick", initialTick);
        if (eventCapacity <= 0) {
            throw new IllegalArgumentException("eventCapacity must be positive");
        }
        this.registry = Objects.requireNonNull(registry, "registry");
        if (registry.sourceCount() != 0) {
            throw new IllegalArgumentException("source timeline requires a fresh registry");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.capacity = eventCapacity;
        this.eventWatermarks = new long[eventCapacity];
        this.commands = new SourceCommand[eventCapacity];
        this.accumulators = registry.accumulators();
        this.destination = Objects.requireNonNull(destination, "destination");
        this.lastOfferedTick = initialTick;
        this.cursorTick = initialTick;
    }

    public long latestOfferedWatermark() {
        return nextOfferedWatermark;
    }

    public long appliedWatermark() {
        return appliedWatermark;
    }

    /** Conservative sleep gate for continuous power and pending impulses. */
    public boolean hasActivePowerOrPendingEnergy() {
        return accumulators.hasActivePowerOrPendingEnergy();
    }

    public boolean targets(ThermalCellArena arena) {
        return destination == arena;
    }

    /** Includes live bindings and queued registrations/rebinds in the sealed producer cut. */
    public boolean mayReferenceThermalNodeRange(
            long firstNodeId,
            long endNodeIdExclusive
    ) {
        if (registry.referencesThermalNodeRange(firstNodeId, endNodeIdExclusive)) {
            return true;
        }
        long read = readSequence.get();
        long write = writeSequence.get();
        for (long sequence = read; sequence < write; sequence++) {
            SourceCommand command = commands[(int) (sequence % capacity)];
            if (command != null
                    && command.referencesThermalNodeRange(
                            firstNodeId, endNodeIdExclusive)) {
                return true;
            }
        }
        return false;
    }

    public double routedEnergyJ(long sourceId, SourceBinding.Kind kind) {
        return registry.routedEnergyJAt(sourceId, kind, cursorTick);
    }

    public long offerRegister(
            long sourceId,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean enabled,
            long effectiveTick,
            EmissionPort[] ports
    ) {
        return offer(new RegisterCommand(
                sourceId,
                lifecycleGeneration,
                mode,
                powerW,
                enabled,
                effectiveTick,
                ports
        ));
    }

    public long offerPowerChange(long sourceId, double powerW, long effectiveTick) {
        return offer(new PowerChangeCommand(sourceId, powerW, effectiveTick));
    }

    public long offerEnabledChange(long sourceId, boolean enabled, long effectiveTick) {
        return offer(new EnabledChangeCommand(sourceId, enabled, effectiveTick));
    }

    public long offerRebind(
            long sourceId,
            int portId,
            SourceBinding binding,
            long effectiveTick
    ) {
        return offer(new RebindCommand(sourceId, portId, binding, effectiveTick));
    }

    public long offerImpulse(
            long sourceId,
            int portId,
            double signedEnergyJ,
            long effectiveTick
    ) {
        return offer(new ImpulseCommand(sourceId, portId, signedEnergyJ, effectiveTick));
    }

    public long offerUnload(
            long sourceId,
            int expectedLifecycleGeneration,
            long effectiveTick
    ) {
        return offer(new UnloadCommand(
                sourceId, expectedLifecycleGeneration, effectiveTick));
    }

    /**
     * Returns zero when the bounded queue is full. The rejected command does
     * not consume a watermark and can be retried or recovered by its owner.
     */
    private long offer(SourceCommand command) {
        Objects.requireNonNull(command, "command");
        long effectiveTick = command.effectiveTick();
        requireTick("effectiveTick", effectiveTick);
        if (effectiveTick < lastOfferedTick) {
            throw new IllegalArgumentException(
                    "source commands must be offered in game-tick order");
        }
        long write = writeSequence.get();
        if (write - readSequence.get() >= capacity) {
            return OFFER_REJECTED;
        }
        nextOfferedWatermark = Math.incrementExact(nextOfferedWatermark);
        long watermark = nextOfferedWatermark;
        int slot = (int) (write % capacity);
        eventWatermarks[slot] = watermark;
        commands[slot] = command;
        lastOfferedTick = effectiveTick;
        writeSequence.lazySet(write + 1L);
        return watermark;
    }

    /** Verifies that the complete sealed source cut is present and contiguous. */
    public boolean isReady(SolveEpoch epoch) {
        Objects.requireNonNull(epoch, "epoch");
        if (isPreApplied(epoch)) {
            return true;
        }
        if (preAppliedEpochId >= 0L) {
            return false;
        }
        if (epoch.dimensionGeneration() != dimensionGeneration
                || epoch.previousTick() != cursorTick
                || epoch.sourceWatermark() < appliedWatermark) {
            return false;
        }
        long requiredCount = epoch.sourceWatermark() - appliedWatermark;
        long read = readSequence.get();
        long write = writeSequence.get();
        if (requiredCount > write - read) {
            return false;
        }
        for (long offset = 0L; offset < requiredCount; offset++) {
            int slot = (int) ((read + offset) % capacity);
            SourceCommand command = commands[slot];
            long expectedWatermark = appliedWatermark + offset + 1L;
            if (command == null
                    || eventWatermarks[slot] != expectedWatermark
                    || command.effectiveTick() < epoch.previousTick()
                    || command.effectiveTick() > epoch.targetTick()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Settles the old bindings before a topology replacement at the epoch cut.
     * The solver later consumes the recorded energy without applying it twice.
     */
    public double preApplyForTopology(SolveEpoch epoch) {
        Objects.requireNonNull(epoch, "epoch");
        if (isPreApplied(epoch)) {
            return preAppliedEnergyJ;
        }
        if (preAppliedEpochId >= 0L || !isReady(epoch)) {
            throw new IllegalStateException("source epoch is not ready for topology settlement");
        }
        double energy = apply(epoch, epoch.previousTick(), epoch.previousTick());
        if (epoch.targetTick() != epoch.previousTick()) {
            energy = finiteSum(
                    "topology source settlement",
                    energy,
                    apply(epoch, epoch.previousTick(), epoch.targetTick()));
        }
        preAppliedEpochId = epoch.epochId();
        preAppliedEnergyJ = energy;
        return energy;
    }

    public boolean isPreApplied(SolveEpoch epoch) {
        return epoch != null
                && preAppliedEpochId == epoch.epochId()
                && epoch.dimensionGeneration() == dimensionGeneration
                && cursorTick == epoch.targetTick()
                && appliedWatermark == epoch.sourceWatermark();
    }

    public double preAppliedEnergyJ(SolveEpoch epoch) {
        if (!isPreApplied(epoch)) {
            throw new IllegalStateException("source epoch was not pre-applied");
        }
        return preAppliedEnergyJ;
    }

    public void completePreApplied(SolveEpoch epoch) {
        if (preAppliedEpochId < 0L) {
            return;
        }
        if (!isPreApplied(epoch)) {
            throw new IllegalStateException("a different source epoch owns the topology cut");
        }
        preAppliedEpochId = -1L;
        preAppliedEnergyJ = 0.0D;
    }

    /**
     * Applies the sealed cut {@code (fromTick, toTick]}. A zero-length call
     * consumes newly sealed commands exactly at that tick.
     */
    public double apply(SolveEpoch epoch, long fromTick, long toTick) {
        Objects.requireNonNull(epoch, "epoch");
        if (epoch.dimensionGeneration() != dimensionGeneration) {
            throw new IllegalArgumentException("source timeline generation mismatch");
        }
        if (fromTick != cursorTick || toTick < fromTick || toTick > epoch.targetTick()) {
            throw new IllegalArgumentException("source interval is not contiguous with its owner");
        }

        double total = 0.0D;
        double compensation = 0.0D;
        while (appliedWatermark < epoch.sourceWatermark()) {
            long read = readSequence.get();
            if (read >= writeSequence.get()) {
                throw new IllegalStateException("sealed source command is no longer available");
            }
            int slot = (int) (read % capacity);
            SourceCommand command = commands[slot];
            long eventTick = command.effectiveTick();
            boolean belongsToCut = fromTick == toTick
                    ? eventTick == toTick
                    : eventTick > fromTick && eventTick <= toTick;
            if (!belongsToCut) {
                break;
            }

            double drained = accumulators.drainAllPendingEnergyTo(
                    eventTick, destination);
            double adjusted = drained - compensation;
            double next = total + adjusted;
            requireFinite("source interval energy", next);
            compensation = (next - total) - adjusted;
            total = next;

            command.apply(registry);
            appliedWatermark = eventWatermarks[slot];
            commands[slot] = null;
            readSequence.lazySet(read + 1L);
        }

        double drained = accumulators.drainAllPendingEnergyTo(toTick, destination);
        double adjusted = drained - compensation;
        double next = total + adjusted;
        requireFinite("source interval energy", next);
        cursorTick = toTick;
        return canonicalZero(next);
    }

    private interface SourceCommand {
        long effectiveTick();

        void apply(ThermalSourceRegistry registry);

        default boolean referencesThermalNodeRange(
                long firstNodeId,
                long endNodeIdExclusive
        ) {
            return false;
        }
    }

    private record RegisterCommand(
            long sourceId,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean enabled,
            long effectiveTick,
            EmissionPort[] ports
    ) implements SourceCommand {
        private RegisterCommand {
            Objects.requireNonNull(mode, "mode");
            requireFinite("powerW", powerW);
            ports = Objects.requireNonNull(ports, "ports").clone();
        }

        @Override
        public void apply(ThermalSourceRegistry registry) {
            registry.registerSource(
                    sourceId,
                    lifecycleGeneration,
                    mode,
                    powerW,
                    enabled,
                    effectiveTick,
                    ports
            );
        }

        @Override
        public boolean referencesThermalNodeRange(
                long firstNodeId,
                long endNodeIdExclusive
        ) {
            for (EmissionPort port : ports) {
                SourceBinding binding = port.binding();
                if (binding.isThermalNode()
                        && binding.targetId() >= firstNodeId
                        && binding.targetId() < endNodeIdExclusive) {
                    return true;
                }
            }
            return false;
        }
    }

    private record PowerChangeCommand(
            long sourceId,
            double powerW,
            long effectiveTick
    ) implements SourceCommand {
        private PowerChangeCommand {
            requireFinite("powerW", powerW);
        }

        @Override
        public void apply(ThermalSourceRegistry registry) {
            registry.setPower(sourceId, powerW, effectiveTick);
        }
    }

    private record EnabledChangeCommand(
            long sourceId,
            boolean enabled,
            long effectiveTick
    ) implements SourceCommand {
        @Override
        public void apply(ThermalSourceRegistry registry) {
            registry.setEnabled(sourceId, enabled, effectiveTick);
        }
    }

    private record RebindCommand(
            long sourceId,
            int portId,
            SourceBinding binding,
            long effectiveTick
    ) implements SourceCommand {
        private RebindCommand {
            Objects.requireNonNull(binding, "binding");
        }

        @Override
        public void apply(ThermalSourceRegistry registry) {
            registry.rebindPort(sourceId, portId, binding, effectiveTick);
        }

        @Override
        public boolean referencesThermalNodeRange(
                long firstNodeId,
                long endNodeIdExclusive
        ) {
            return binding.isThermalNode()
                    && binding.targetId() >= firstNodeId
                    && binding.targetId() < endNodeIdExclusive;
        }
    }

    private record ImpulseCommand(
            long sourceId,
            int portId,
            double signedEnergyJ,
            long effectiveTick
    ) implements SourceCommand {
        private ImpulseCommand {
            requireFinite("signedEnergyJ", signedEnergyJ);
        }

        @Override
        public void apply(ThermalSourceRegistry registry) {
            registry.applyImpulse(sourceId, portId, signedEnergyJ, effectiveTick);
        }
    }

    private record UnloadCommand(
            long sourceId,
            int expectedLifecycleGeneration,
            long effectiveTick
    ) implements SourceCommand {
        @Override
        public void apply(ThermalSourceRegistry registry) {
            registry.unloadSource(sourceId, expectedLifecycleGeneration, effectiveTick);
        }
    }

    private static void requireTick(String name, long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static double canonicalZero(double value) {
        return value == 0.0D ? 0.0D : value;
    }

    private static double finiteSum(String name, double left, double right) {
        double result = left + right;
        if (!Double.isFinite(result)) {
            throw new ArithmeticException(name + " exceeded the finite domain");
        }
        return canonicalZero(result);
    }
}
