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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable view over already-captured block and fluid states. This type has
 * no World, chunk, callback, or Supplier reference, so a resolver lookup can
 * never trigger a chunk load. Use a fresh {@link Access} for each resolution
 * to retain and normalize sentinel accesses.
 */
public final class ResolverBlockView<B, F> {
    private final DependencyOffsetMask dependencyMask;
    private final Map<DependencyOffsetMask.Offset, SnapshotCell<B, F>> cells;

    private ResolverBlockView(
            DependencyOffsetMask dependencyMask,
            Map<DependencyOffsetMask.Offset, SnapshotCell<B, F>> cells
    ) {
        this.dependencyMask = dependencyMask;
        this.cells = cells;
    }

    /**
     * Copies only declared cells from a loaded-only union snapshot. Missing
     * declared entries become explicit {@link LookupStatus#MISSING} sentinels.
     */
    public static <B, F> ResolverBlockView<B, F> snapshot(
            DependencyOffsetMask dependencyMask,
            Map<DependencyOffsetMask.Offset, SnapshotCell<B, F>> unionSnapshot
    ) {
        Objects.requireNonNull(dependencyMask, "dependencyMask");
        Objects.requireNonNull(unionSnapshot, "unionSnapshot");
        Map<DependencyOffsetMask.Offset, SnapshotCell<B, F>> copied = new LinkedHashMap<>();
        for (DependencyOffsetMask.Offset offset : dependencyMask.offsets()) {
            SnapshotCell<B, F> cell = unionSnapshot.get(offset);
            copied.put(offset, cell == null ? SnapshotCell.missing() : cell);
        }
        return new ResolverBlockView<>(dependencyMask, Map.copyOf(copied));
    }

    public DependencyOffsetMask dependencyMask() {
        return dependencyMask;
    }

    public int presentCellCount() {
        int count = 0;
        for (SnapshotCell<B, F> cell : cells.values()) {
            if (cell.status() == LookupStatus.PRESENT) {
                count++;
            }
        }
        return count;
    }

    public boolean isComplete() {
        return presentCellCount() == dependencyMask.offsetCount();
    }

    /** Creates a stateful access audit over this immutable snapshot. */
    public Access<B, F> openAccess() {
        return new Access<>(this);
    }

    private Lookup<B, F> lookup(int x, int y, int z) {
        if (!DependencyOffsetMask.Offset.isInRange(x, y, z)) {
            return Lookup.outsideDeclaredMask();
        }
        DependencyOffsetMask.Offset offset = new DependencyOffsetMask.Offset(x, y, z);
        if (!dependencyMask.contains(offset)) {
            return Lookup.outsideDeclaredMask();
        }
        return cells.get(offset).toLookup();
    }

    /** Immutable block/fluid pair captured by the main thread. */
    public record StateAndFluid<B, F>(B blockState, F fluidState) {
        public StateAndFluid {
            Objects.requireNonNull(blockState, "blockState");
            Objects.requireNonNull(fluidState, "fluidState");
        }
    }

    /** One input cell in a loaded-only union snapshot. */
    public static final class SnapshotCell<B, F> {
        private final LookupStatus status;
        private final StateAndFluid<B, F> value;

        private SnapshotCell(LookupStatus status, StateAndFluid<B, F> value) {
            this.status = status;
            this.value = value;
        }

        public static <B, F> SnapshotCell<B, F> present(B blockState, F fluidState) {
            return new SnapshotCell<>(
                    LookupStatus.PRESENT,
                    new StateAndFluid<>(blockState, fluidState)
            );
        }

        public static <B, F> SnapshotCell<B, F> unloaded() {
            return new SnapshotCell<>(LookupStatus.UNLOADED, null);
        }

        public static <B, F> SnapshotCell<B, F> missing() {
            return new SnapshotCell<>(LookupStatus.MISSING, null);
        }

        public LookupStatus status() {
            return status;
        }

        private Lookup<B, F> toLookup() {
            return new Lookup<>(status, Optional.ofNullable(value));
        }
    }

    public enum LookupStatus {
        PRESENT,
        UNLOADED,
        MISSING,
        OUTSIDE_DECLARED_MASK
    }

    /** Sentinel-bearing lookup result; unavailable states never use null values. */
    public record Lookup<B, F>(LookupStatus status, Optional<StateAndFluid<B, F>> value) {
        public Lookup {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(value, "value");
            if ((status == LookupStatus.PRESENT) != value.isPresent()) {
                throw new IllegalArgumentException("only PRESENT lookup may contain state data");
            }
        }

        private static <B, F> Lookup<B, F> outsideDeclaredMask() {
            return new Lookup<>(LookupStatus.OUTSIDE_DECLARED_MASK, Optional.empty());
        }

        public ThermalResolution<StateAndFluid<B, F>> asResolution() {
            return switch (status) {
                case PRESENT -> ThermalResolution.resolved(value.orElseThrow());
                case UNLOADED -> ThermalResolution.unresolved(
                        ThermalResolution.Reason.DEPENDENCY_UNLOADED);
                case MISSING -> ThermalResolution.unresolved(
                        ThermalResolution.Reason.SNAPSHOT_DATA_MISSING);
                case OUTSIDE_DECLARED_MASK -> ThermalResolution.unresolved(
                        ThermalResolution.Reason.DEPENDENCY_OUTSIDE_DECLARED_MASK);
            };
        }
    }

    /**
     * Per-resolution access audit. It remembers sentinel/forbidden reads so a
     * resolver cannot accidentally publish a resolved opening after ignoring
     * an unavailable dependency.
     */
    public static final class Access<B, F> {
        private final ResolverBlockView<B, F> snapshot;
        private final Set<DependencyOffsetMask.Offset> uniqueReads = new LinkedHashSet<>();
        private ThermalResolution.Reason firstFailure;

        private Access(ResolverBlockView<B, F> snapshot) {
            this.snapshot = snapshot;
        }

        public DependencyOffsetMask dependencyMask() {
            return snapshot.dependencyMask();
        }

        public Lookup<B, F> lookup(DependencyOffsetMask.Offset offset) {
            Objects.requireNonNull(offset, "offset");
            return lookup(offset.x(), offset.y(), offset.z());
        }

        public Lookup<B, F> lookup(int x, int y, int z) {
            if (snapshot.dependencyMask().contains(x, y, z)) {
                uniqueReads.add(new DependencyOffsetMask.Offset(x, y, z));
            }
            Lookup<B, F> lookup = snapshot.lookup(x, y, z);
            remember(lookup.asResolution().reason());
            return lookup;
        }

        /** Declared snapshot offsets read so far, in first-read order. */
        public Set<DependencyOffsetMask.Offset> uniqueReads() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(uniqueReads));
        }

        public int uniqueReadCount() {
            return uniqueReads.size();
        }

        /** Block entities are deliberately absent from a V1 resolver snapshot. */
        public ThermalResolution<Void> blockEntity(int x, int y, int z) {
            Lookup<B, F> dependency = lookup(x, y, z);
            if (dependency.status() != LookupStatus.PRESENT) {
                return ThermalResolution.failure(firstFailure);
            }
            remember(ThermalResolution.Reason.BLOCK_ENTITY_DEPENDENT);
            return ThermalResolution.unsupported(ThermalResolution.Reason.BLOCK_ENTITY_DEPENDENT);
        }

        /** Entity/collision context is deliberately absent from a V1 resolver snapshot. */
        public ThermalResolution<Void> entityContext() {
            remember(ThermalResolution.Reason.ENTITY_CONTEXT_DEPENDENT);
            return ThermalResolution.unsupported(ThermalResolution.Reason.ENTITY_CONTEXT_DEPENDENT);
        }

        public Optional<ThermalResolution.Reason> firstFailure() {
            return Optional.ofNullable(firstFailure);
        }

        /** Applies retained access failures before a resolver result is published. */
        public <T> ThermalResolution<T> normalize(ThermalResolution<T> resolverResult) {
            Objects.requireNonNull(resolverResult, "resolverResult");
            if (firstFailure == null) {
                return resolverResult;
            }
            return ThermalResolution.failure(firstFailure);
        }

        private void remember(ThermalResolution.Reason reason) {
            if (reason == ThermalResolution.Reason.NONE) {
                return;
            }
            if (firstFailure == null
                    || firstFailure.expectedStatus() == ThermalResolution.Status.UNRESOLVED
                    && reason.expectedStatus() == ThermalResolution.Status.CONSERVATIVE_UNSUPPORTED) {
                firstFailure = reason;
            }
        }
    }
}
