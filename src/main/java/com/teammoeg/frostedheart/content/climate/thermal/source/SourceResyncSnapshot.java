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

import java.util.Objects;

/**
 * Immutable recovery envelope for one source.
 *
 * <p>{@link #cumulativeEmittedEnergyJ()} is checksum-only. Recovery applies
 * retained binding segments and explicit losses, never the cumulative delta.</p>
 */
public record SourceResyncSnapshot(
        long sourceId,
        int sourceRevision,
        long eventWatermark,
        long snapshotTick,
        long baseAckWatermark,
        long baseAckTick,
        double baseAckCumulativeEnergyJ,
        double cumulativeEmittedEnergyJ,
        double currentPowerW,
        boolean enabled,
        ThermalSourceMode mode,
        EmissionPort[] currentPorts,
        BindingEnergySegment[] retainedSegments,
        SourceResyncLoss[] losses
) {
    public SourceResyncSnapshot {
        if (sourceRevision < 0) {
            throw new IllegalArgumentException("sourceRevision must be non-negative");
        }
        if (eventWatermark < baseAckWatermark) {
            throw new IllegalArgumentException("eventWatermark precedes its ACK base");
        }
        if (snapshotTick < baseAckTick) {
            throw new IllegalArgumentException("snapshotTick precedes its ACK base");
        }
        requireFinite("baseAckCumulativeEnergyJ", baseAckCumulativeEnergyJ);
        requireFinite("cumulativeEmittedEnergyJ", cumulativeEmittedEnergyJ);
        requireFinite("currentPowerW", currentPowerW);
        Objects.requireNonNull(mode, "mode");
        currentPorts = requireCloned(currentPorts, "currentPorts");
        retainedSegments = requireCloned(retainedSegments, "retainedSegments");
        losses = requireCloned(losses, "losses");
        long previousWatermark = baseAckWatermark;
        long previousEndTick = baseAckTick;
        for (BindingEnergySegment segment : retainedSegments) {
            if (segment.eventWatermark() < previousWatermark) {
                throw new IllegalArgumentException("retainedSegments are not watermark ordered");
            }
            if (segment.startTick() < previousEndTick
                    && segment.eventWatermark() != previousWatermark) {
                throw new IllegalArgumentException("retainedSegments are not time ordered");
            }
            previousWatermark = segment.eventWatermark();
            previousEndTick = Math.max(previousEndTick, segment.endTick());
        }
    }

    @Override
    public EmissionPort[] currentPorts() {
        return currentPorts.clone();
    }

    @Override
    public BindingEnergySegment[] retainedSegments() {
        return retainedSegments.clone();
    }

    @Override
    public SourceResyncLoss[] losses() {
        return losses.clone();
    }

    public double reconstructedCumulativeEnergyJ() {
        CompensatedSum sum = new CompensatedSum(baseAckCumulativeEnergyJ);
        for (BindingEnergySegment segment : retainedSegments) {
            sum.add(segment.signedEnergyJ());
        }
        for (SourceResyncLoss loss : losses) {
            sum.add(loss.signedEnergyJ());
        }
        return sum.value();
    }

    public double checksumResidualJ() {
        return cumulativeEmittedEnergyJ - reconstructedCumulativeEnergyJ();
    }

    public record BindingEnergySegment(
            long eventWatermark,
            long startTick,
            long endTick,
            int portId,
            int portRevision,
            SourceChannel channel,
            SourceBinding binding,
            double signedEnergyJ
    ) {
        public BindingEnergySegment {
            if (eventWatermark < 0L) {
                throw new IllegalArgumentException("eventWatermark must be non-negative");
            }
            if (endTick < startTick) {
                throw new IllegalArgumentException("segment endTick precedes startTick");
            }
            if (portId < 0 || portRevision < 0) {
                throw new IllegalArgumentException("port identity must be non-negative");
            }
            Objects.requireNonNull(channel, "channel");
            Objects.requireNonNull(binding, "binding");
            requireFinite("signedEnergyJ", signedEnergyJ);
        }
    }

    public record SourceResyncLoss(
            Reason reason,
            long startTick,
            long endTick,
            long firstWatermark,
            long lastWatermark,
            int segmentCount,
            double signedEnergyJ,
            SourceBinding rejectedBinding
    ) {
        public SourceResyncLoss {
            Objects.requireNonNull(reason, "reason");
            if (endTick < startTick) {
                throw new IllegalArgumentException("loss endTick precedes startTick");
            }
            if (lastWatermark < firstWatermark) {
                throw new IllegalArgumentException("loss watermark range is reversed");
            }
            if (segmentCount <= 0) {
                throw new IllegalArgumentException("segmentCount must be positive");
            }
            requireFinite("signedEnergyJ", signedEnergyJ);
        }

        public enum Reason {
            HISTORY_EXHAUSTED,
            STALE_LIFECYCLE_GENERATION,
            UNRESOLVED_BINDING
        }
    }

    private static <T> T[] requireCloned(T[] values, String name) {
        Objects.requireNonNull(values, name);
        T[] clone = values.clone();
        for (T value : clone) {
            Objects.requireNonNull(value, name + " contains null");
        }
        return clone;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static final class CompensatedSum {
        private double value;
        private double compensation;

        private CompensatedSum(double initialValue) {
            value = initialValue;
        }

        private void add(double increment) {
            double adjusted = increment - compensation;
            double next = value + adjusted;
            if (!Double.isFinite(next)) {
                throw new ArithmeticException("source checksum exceeded the finite domain");
            }
            compensation = (next - value) - adjusted;
            value = next;
        }

        private double value() {
            return value == 0.0D ? 0.0D : value;
        }
    }
}
