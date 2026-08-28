/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

import java.util.Objects;

/**
 * Allocation-free synchronous view over main-thread-captured dependency
 * states. One Scratch owns 27 reusable lookup slots.
 */
public final class ResolverBlockView<B, F> {
    private static final int CELL_COUNT =
            DependencyOffsetMask.MAXIMUM_OFFSET_COUNT;
    private DependencyOffsetMask dependencyMask =
            DependencyOffsetMask.SELF_ONLY;
    private final Lookup<B, F>[] cells;
    private final Access<B, F> access = new Access<>(this);

    @SuppressWarnings("unchecked")
    private ResolverBlockView() {
        cells = new Lookup[CELL_COUNT];
        for (int index = 0; index < cells.length; index++) {
            cells[index] = new Lookup<>();
        }
    }

    public DependencyOffsetMask dependencyMask() {
        return dependencyMask;
    }

    public Access<B, F> openAccess() {
        access.reset();
        return access;
    }

    private Lookup<B, F> lookup(int x, int y, int z) {
        if (!DependencyOffsetMask.Offset.isInRange(x, y, z)
                || !dependencyMask.contains(x, y, z)) {
            return Lookup.outside();
        }
        return cells[cellIndex(x, y, z)];
    }

    private static int cellIndex(int x, int y, int z) {
        return ((y + 1) * 9) + ((z + 1) * 3) + x + 1;
    }

    /** Main-thread scratch borrowed for one synchronous resolver call. */
    public static final class Scratch<B, F> {
        private final ResolverBlockView<B, F> view =
                new ResolverBlockView<>();

        public ResolverBlockView<B, F> begin(DependencyOffsetMask mask) {
            view.dependencyMask = Objects.requireNonNull(mask, "mask");
            return view;
        }

        public void putPresent(int x, int y, int z, B blockState, F fluidState) {
            requireOffset(x, y, z);
            view.cells[cellIndex(x, y, z)].setPresent(
                    blockState, fluidState);
        }

        public void putUnavailable(
                int x,
                int y,
                int z,
                LookupStatus status
        ) {
            requireOffset(x, y, z);
            if (status == LookupStatus.PRESENT
                    || status == LookupStatus.OUTSIDE_DECLARED_MASK) {
                throw new IllegalArgumentException(
                        "unavailable lookup status is invalid");
            }
            view.cells[cellIndex(x, y, z)].setUnavailable(status);
        }

        private static void requireOffset(int x, int y, int z) {
            if (!DependencyOffsetMask.Offset.isInRange(x, y, z)) {
                throw new IllegalArgumentException(
                        "capture offset is outside [-1, 1]^3");
            }
        }
    }

    public enum LookupStatus {
        PRESENT,
        UNLOADED,
        MISSING,
        OUTSIDE_DECLARED_MASK
    }

    /** Reused scalar lookup; valid only until its Scratch is written again. */
    public static final class Lookup<B, F> {
        private static final Lookup<?, ?> OUTSIDE = new Lookup<>(
                LookupStatus.OUTSIDE_DECLARED_MASK);
        private LookupStatus status;
        private B blockState;
        private F fluidState;

        private Lookup() {
            this(LookupStatus.MISSING);
        }

        private Lookup(LookupStatus status) {
            this.status = status;
        }

        private void setPresent(B blockState, F fluidState) {
            this.status = LookupStatus.PRESENT;
            this.blockState = Objects.requireNonNull(
                    blockState, "blockState");
            this.fluidState = Objects.requireNonNull(
                    fluidState, "fluidState");
        }

        private void setUnavailable(LookupStatus status) {
            this.status = status;
            blockState = null;
            fluidState = null;
        }

        public LookupStatus status() {
            return status;
        }

        public B blockState() {
            requirePresent();
            return blockState;
        }

        public F fluidState() {
            requirePresent();
            return fluidState;
        }

        public ThermalResolution.Reason reason() {
            return switch (status) {
                case PRESENT -> ThermalResolution.Reason.NONE;
                case UNLOADED ->
                        ThermalResolution.Reason.DEPENDENCY_UNLOADED;
                case MISSING ->
                        ThermalResolution.Reason.SNAPSHOT_DATA_MISSING;
                case OUTSIDE_DECLARED_MASK ->
                        ThermalResolution.Reason
                                .DEPENDENCY_OUTSIDE_DECLARED_MASK;
            };
        }

        private void requirePresent() {
            if (status != LookupStatus.PRESENT) {
                throw new IllegalStateException(
                        "unavailable dependency has no state");
            }
        }

        @SuppressWarnings("unchecked")
        private static <B, F> Lookup<B, F> outside() {
            return (Lookup<B, F>) OUTSIDE;
        }
    }

    /** Access audit that normalizes unavailable dependency reads. */
    public static final class Access<B, F> {
        private final ResolverBlockView<B, F> snapshot;
        private ThermalResolution.Reason firstFailure;

        private Access(ResolverBlockView<B, F> snapshot) {
            this.snapshot = snapshot;
        }

        private void reset() {
            firstFailure = null;
        }

        public DependencyOffsetMask dependencyMask() {
            return snapshot.dependencyMask();
        }

        public Lookup<B, F> lookup(DependencyOffsetMask.Offset offset) {
            Objects.requireNonNull(offset, "offset");
            return lookup(offset.x(), offset.y(), offset.z());
        }

        public Lookup<B, F> lookup(int x, int y, int z) {
            Lookup<B, F> lookup = snapshot.lookup(x, y, z);
            remember(lookup.reason());
            return lookup;
        }

        public ThermalResolution<Void> blockEntity(int x, int y, int z) {
            Lookup<B, F> dependency = lookup(x, y, z);
            if (dependency.status() != LookupStatus.PRESENT) {
                return ThermalResolution.failure(firstFailure);
            }
            remember(ThermalResolution.Reason.BLOCK_ENTITY_DEPENDENT);
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.BLOCK_ENTITY_DEPENDENT);
        }

        public ThermalResolution<Void> entityContext() {
            remember(ThermalResolution.Reason.ENTITY_CONTEXT_DEPENDENT);
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.ENTITY_CONTEXT_DEPENDENT);
        }

        public <T> ThermalResolution<T> normalize(
                ThermalResolution<T> resolverResult
        ) {
            Objects.requireNonNull(resolverResult, "resolverResult");
            return firstFailure == null
                    ? resolverResult
                    : ThermalResolution.failure(firstFailure);
        }

        private void remember(ThermalResolution.Reason reason) {
            if (reason == ThermalResolution.Reason.NONE) {
                return;
            }
            if (firstFailure == null
                    || firstFailure.expectedStatus()
                            == ThermalResolution.Status.UNRESOLVED
                    && reason.expectedStatus()
                            == ThermalResolution.Status
                                    .CONSERVATIVE_UNSUPPORTED) {
                firstFailure = reason;
            }
        }
    }
}
