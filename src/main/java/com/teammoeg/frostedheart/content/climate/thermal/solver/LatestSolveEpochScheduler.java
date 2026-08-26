/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Objects;
import java.util.Optional;

/**
 * Latest-target, single-in-flight epoch state for one dimension generation.
 * Intermediate cadence targets are coalesced instead of becoming a backlog.
 */
public final class LatestSolveEpochScheduler {
    private final long dimensionGeneration;
    private final ThermalTimePolicy timePolicy;

    private long lastCompletedTargetTick;
    private InputWatermarks lastCompletedWatermarks;
    private long nextEpochId = 1L;
    private SealedInputFrame latestFrame;
    private boolean latestFrameUrgent;
    private SolveEpoch inFlight;

    public LatestSolveEpochScheduler(
            long dimensionGeneration,
            long initialCompletedTick,
            InputWatermarks initialAppliedWatermarks,
            ThermalTimePolicy timePolicy
    ) {
        if (dimensionGeneration < 0L) {
            throw new IllegalArgumentException("dimensionGeneration must be non-negative");
        }
        if (initialCompletedTick < 0L) {
            throw new IllegalArgumentException("initialCompletedTick must be non-negative");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.lastCompletedTargetTick = initialCompletedTick;
        this.lastCompletedWatermarks = Objects.requireNonNull(
                initialAppliedWatermarks,
                "initialAppliedWatermarks"
        );
        this.timePolicy = Objects.requireNonNull(timePolicy, "timePolicy");
    }

    public enum SealResult {
        ACCEPTED,
        DUPLICATE,
        STALE_IGNORED,
        GENERATION_MISMATCH,
        WATERMARK_REGRESSION
    }

    public enum CompletionResult {
        COMPLETED,
        NOT_IN_FLIGHT,
        GENERATION_MISMATCH,
        INPUTS_PENDING
    }

    public SealResult sealLatest(SealedInputFrame frame) {
        return sealLatest(frame, false);
    }

    /** Seals a frame that may bypass the normal cadence after an input event. */
    public SealResult sealLatest(SealedInputFrame frame, boolean urgent) {
        Objects.requireNonNull(frame, "frame");
        if (frame.dimensionGeneration() != dimensionGeneration) {
            return SealResult.GENERATION_MISMATCH;
        }
        if (frame.effectiveTick() < lastCompletedTargetTick) {
            return SealResult.STALE_IGNORED;
        }
        if (!frame.watermarks().covers(lastCompletedWatermarks)) {
            return SealResult.WATERMARK_REGRESSION;
        }
        if (latestFrame != null) {
            if (frame.effectiveTick() < latestFrame.effectiveTick()) {
                return SealResult.STALE_IGNORED;
            }
            if (!frame.watermarks().covers(latestFrame.watermarks())) {
                return SealResult.WATERMARK_REGRESSION;
            }
        }
        if (latestFrame != null && latestFrame.equals(frame)) {
            latestFrameUrgent |= urgent;
            return SealResult.DUPLICATE;
        }
        if (latestFrame == null
                && frame.effectiveTick() == lastCompletedTargetTick
                && frame.watermarks().equals(lastCompletedWatermarks)) {
            return SealResult.DUPLICATE;
        }
        latestFrame = frame;
        latestFrameUrgent |= urgent;
        return SealResult.ACCEPTED;
    }

    /** Starts at most one epoch and always targets the latest sealed frame. */
    public Optional<SolveEpoch> tryStartLatest() {
        if (!canStartLatest()) {
            return Optional.empty();
        }
        inFlight = SolveEpoch.fromFrame(lastCompletedTargetTick, nextEpochId++, latestFrame);
        return Optional.of(inFlight);
    }

    public boolean canStartLatest() {
        if (inFlight != null || latestFrame == null) {
            return false;
        }
        long deltaTicks = latestFrame.effectiveTick() - lastCompletedTargetTick;
        boolean watermarkAdvance = !lastCompletedWatermarks.covers(latestFrame.watermarks());
        if (deltaTicks == 0L && !watermarkAdvance) {
            return false;
        }
        return latestFrameUrgent
                || deltaTicks == 0L
                || deltaTicks >= timePolicy.uniformStepTicks();
    }

    /** Completion remains pending until every sealed input cut is applied. */
    public CompletionResult complete(
            SolveEpoch epoch,
            long appliedDimensionGeneration,
            InputWatermarks appliedWatermarks
    ) {
        if (inFlight == null || !inFlight.equals(epoch)) {
            return CompletionResult.NOT_IN_FLIGHT;
        }
        if (appliedDimensionGeneration != dimensionGeneration) {
            return CompletionResult.GENERATION_MISMATCH;
        }
        if (appliedWatermarks == null
                || !appliedWatermarks.covers(epoch.sealedWatermarks())) {
            return CompletionResult.INPUTS_PENDING;
        }
        lastCompletedTargetTick = epoch.targetTick();
        lastCompletedWatermarks = appliedWatermarks;
        inFlight = null;
        if (latestFrame != null
                && latestFrame.effectiveTick() == epoch.targetTick()
                && latestFrame.watermarks().equals(epoch.sealedWatermarks())) {
            latestFrameUrgent = false;
        }
        return CompletionResult.COMPLETED;
    }

    public Optional<SolveEpoch> inFlight() {
        return Optional.ofNullable(inFlight);
    }

    /** Retains one failed epoch while restarting at its failed transport interval. */
    public boolean retryInFlightFrom(long retryFromTick) {
        if (inFlight == null) {
            return false;
        }
        if (retryFromTick < inFlight.previousTick()
                || retryFromTick > inFlight.targetTick()) {
            throw new IllegalArgumentException(
                    "retry tick is outside the in-flight epoch");
        }
        inFlight = new SolveEpoch(
                retryFromTick,
                inFlight.targetTick(),
                inFlight.epochId(),
                inFlight.dimensionGeneration(),
                inFlight.sealedWatermarks());
        latestFrameUrgent = true;
        return true;
    }

    /** A latest-only scheduler has either zero or one coalesced pending target. */
    public int pendingTargetCount() {
        if (latestFrame == null) {
            return 0;
        }
        long baselineTick = inFlight == null
                ? lastCompletedTargetTick
                : inFlight.targetTick();
        InputWatermarks baselineWatermarks = inFlight == null
                ? lastCompletedWatermarks
                : inFlight.sealedWatermarks();
        boolean newerTick = latestFrame.effectiveTick() > baselineTick;
        boolean newerWatermark = !baselineWatermarks.covers(latestFrame.watermarks());
        return newerTick || newerWatermark ? 1 : 0;
    }

    public long lastCompletedTargetTick() {
        return lastCompletedTargetTick;
    }

    public InputWatermarks lastCompletedWatermarks() {
        return lastCompletedWatermarks;
    }
}
