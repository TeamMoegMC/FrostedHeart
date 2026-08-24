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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Applies retained source segments exactly once and treats cumulative energy as a checksum. */
public final class SourceResyncReplayer {
    private static final double CHECKSUM_RELATIVE_TOLERANCE = 1.0e-10D;

    private final Map<Long, Cursor> cursors = new HashMap<>();

    public ReplayResult replay(SourceResyncSnapshot snapshot, ReplayTarget target) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(target, "target");
        Cursor cursor = cursors.get(snapshot.sourceId());
        if (cursor != null) {
            if (cursor.eventWatermark == snapshot.eventWatermark()
                    && close(cursor.cumulativeEnergyJ,
                            snapshot.cumulativeEmittedEnergyJ())) {
                return new ReplayResult(
                        ReplayStatus.ALREADY_APPLIED,
                        0,
                        0,
                        0.0D,
                        0.0D
                );
            }
            if (snapshot.eventWatermark() < cursor.eventWatermark) {
                return new ReplayResult(
                        ReplayStatus.STALE_SNAPSHOT,
                        0,
                        0,
                        0.0D,
                        0.0D
                );
            }
            if (snapshot.baseAckWatermark() != cursor.eventWatermark
                    || !close(snapshot.baseAckCumulativeEnergyJ(),
                            cursor.cumulativeEnergyJ)) {
                return new ReplayResult(
                        ReplayStatus.BASE_MISMATCH,
                        0,
                        0,
                        0.0D,
                        0.0D
                );
            }
        }

        double residual = snapshot.checksumResidualJ();
        if (!withinChecksumTolerance(residual, snapshot.cumulativeEmittedEnergyJ())) {
            return new ReplayResult(
                    ReplayStatus.CHECKSUM_MISMATCH,
                    0,
                    0,
                    0.0D,
                    residual
            );
        }

        SourceResyncSnapshot.BindingEnergySegment[] segments = snapshot.retainedSegments();
        SourceResyncSnapshot.SourceResyncLoss[] retainedLosses = snapshot.losses();
        boolean[] accepted = new boolean[segments.length];
        for (int index = 0; index < segments.length; index++) {
            SourceBinding binding = segments[index].binding();
            accepted[index] = binding.kind() != SourceBinding.Kind.UNBOUND
                    && target.accepts(binding);
        }

        int appliedCount = 0;
        int lossCount = 0;
        double appliedEnergy = 0.0D;
        double lossEnergy = 0.0D;
        for (int index = 0; index < segments.length; index++) {
            SourceResyncSnapshot.BindingEnergySegment segment = segments[index];
            if (accepted[index]) {
                target.applyEnergy(snapshot.sourceId(), segment);
                appliedCount++;
                appliedEnergy = finiteSum(appliedEnergy, segment.signedEnergyJ());
                continue;
            }
            SourceResyncSnapshot.SourceResyncLoss.Reason reason =
                    segment.binding().isThermalNode()
                            ? SourceResyncSnapshot.SourceResyncLoss.Reason
                                    .STALE_LIFECYCLE_GENERATION
                            : SourceResyncSnapshot.SourceResyncLoss.Reason.UNRESOLVED_BINDING;
            SourceResyncSnapshot.SourceResyncLoss loss =
                    new SourceResyncSnapshot.SourceResyncLoss(
                            reason,
                            segment.startTick(),
                            segment.endTick(),
                            segment.eventWatermark(),
                            segment.eventWatermark(),
                            1,
                            segment.signedEnergyJ(),
                            segment.binding()
                    );
            target.recordLoss(snapshot.sourceId(), loss);
            lossCount++;
            lossEnergy = finiteSum(lossEnergy, segment.signedEnergyJ());
        }
        for (SourceResyncSnapshot.SourceResyncLoss loss : retainedLosses) {
            target.recordLoss(snapshot.sourceId(), loss);
            lossCount++;
            lossEnergy = finiteSum(lossEnergy, loss.signedEnergyJ());
        }
        target.finish(snapshot);
        cursors.put(
                snapshot.sourceId(),
                new Cursor(
                        snapshot.eventWatermark(),
                        snapshot.snapshotTick(),
                        snapshot.cumulativeEmittedEnergyJ()
                )
        );
        return new ReplayResult(
                ReplayStatus.APPLIED,
                appliedCount,
                lossCount,
                canonicalZero(appliedEnergy),
                canonicalZero(lossEnergy)
        );
    }

    public CursorView cursor(long sourceId) {
        Cursor cursor = cursors.get(sourceId);
        if (cursor == null) {
            return null;
        }
        return new CursorView(
                cursor.eventWatermark,
                cursor.snapshotTick,
                cursor.cumulativeEnergyJ
        );
    }

    private static boolean withinChecksumTolerance(double residual, double cumulative) {
        return Math.abs(residual) <= CHECKSUM_RELATIVE_TOLERANCE
                * Math.max(1.0D, Math.abs(cumulative));
    }

    private static boolean close(double left, double right) {
        return withinChecksumTolerance(left - right, Math.max(Math.abs(left), Math.abs(right)));
    }

    private static double finiteSum(double left, double right) {
        double result = left + right;
        if (!Double.isFinite(result)) {
            throw new ArithmeticException("replayed source energy exceeded the finite domain");
        }
        return result;
    }

    private static double canonicalZero(double value) {
        return value == 0.0D ? 0.0D : value;
    }

    /** Caller-owned destination; {@link #accepts} must not mutate state. */
    public interface ReplayTarget {
        boolean accepts(SourceBinding binding);

        void applyEnergy(
                long sourceId,
                SourceResyncSnapshot.BindingEnergySegment segment
        );

        void recordLoss(long sourceId, SourceResyncSnapshot.SourceResyncLoss loss);

        default void finish(SourceResyncSnapshot snapshot) {
        }
    }

    public enum ReplayStatus {
        APPLIED,
        ALREADY_APPLIED,
        STALE_SNAPSHOT,
        BASE_MISMATCH,
        CHECKSUM_MISMATCH
    }

    public record ReplayResult(
            ReplayStatus status,
            int appliedSegmentCount,
            int lossCount,
            double appliedEnergyJ,
            double lossEnergyJ
    ) {
        public ReplayResult {
            Objects.requireNonNull(status, "status");
        }
    }

    public record CursorView(
            long eventWatermark,
            long snapshotTick,
            double cumulativeEnergyJ
    ) {
    }

    private record Cursor(long eventWatermark, long snapshotTick, double cumulativeEnergyJ) {
    }
}
