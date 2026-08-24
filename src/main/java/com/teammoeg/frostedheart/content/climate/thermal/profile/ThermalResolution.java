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
import java.util.Optional;
import java.util.function.Function;

/** A value-or-reason result shared by resolver, registry, and census code. */
public record ThermalResolution<T>(Status status, Reason reason, Optional<T> value) {
    public ThermalResolution {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(value, "value");
        if (reason.expectedStatus() != status) {
            throw new IllegalArgumentException("resolution reason does not match status");
        }
        if (status == Status.RESOLVED && value.isEmpty()) {
            throw new IllegalArgumentException("resolved result must contain a value");
        }
        if (status != Status.RESOLVED && value.isPresent()) {
            throw new IllegalArgumentException("non-resolved result must not contain a value");
        }
    }

    public static <T> ThermalResolution<T> resolved(T value) {
        return new ThermalResolution<>(
                Status.RESOLVED,
                Reason.NONE,
                Optional.of(Objects.requireNonNull(value, "value"))
        );
    }

    public static <T> ThermalResolution<T> unresolved(Reason reason) {
        if (Objects.requireNonNull(reason, "reason").expectedStatus() != Status.UNRESOLVED) {
            throw new IllegalArgumentException("reason is not unresolved");
        }
        return new ThermalResolution<>(Status.UNRESOLVED, reason, Optional.empty());
    }

    public static <T> ThermalResolution<T> unsupported(Reason reason) {
        if (Objects.requireNonNull(reason, "reason").expectedStatus()
                != Status.CONSERVATIVE_UNSUPPORTED) {
            throw new IllegalArgumentException("reason is not conservative unsupported");
        }
        return new ThermalResolution<>(Status.CONSERVATIVE_UNSUPPORTED, reason, Optional.empty());
    }

    public static <T> ThermalResolution<T> failure(Reason reason) {
        return switch (Objects.requireNonNull(reason, "reason").expectedStatus()) {
            case UNRESOLVED -> unresolved(reason);
            case CONSERVATIVE_UNSUPPORTED -> unsupported(reason);
            case RESOLVED -> throw new IllegalArgumentException("NONE is not a failure reason");
        };
    }

    public boolean isResolved() {
        return status == Status.RESOLVED;
    }

    public <R> ThermalResolution<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (!isResolved()) {
            return failure(reason);
        }
        return resolved(mapper.apply(value.orElseThrow()));
    }

    public enum Status {
        RESOLVED,
        UNRESOLVED,
        CONSERVATIVE_UNSUPPORTED
    }

    /** Reasons intentionally distinguish retryable snapshot gaps from unsupported geometry. */
    public enum Reason {
        NONE(Status.RESOLVED),

        DEPENDENCY_OUTSIDE_DECLARED_MASK(Status.UNRESOLVED),
        DEPENDENCY_UNLOADED(Status.UNRESOLVED),
        SNAPSHOT_DATA_MISSING(Status.UNRESOLVED),
        UNRESOLVED_DYNAMIC(Status.UNRESOLVED),

        BLOCK_ENTITY_DEPENDENT(Status.CONSERVATIVE_UNSUPPORTED),
        ENTITY_CONTEXT_DEPENDENT(Status.CONSERVATIVE_UNSUPPORTED),
        DYNAMIC_SHAPE_UNSUPPORTED(Status.CONSERVATIVE_UNSUPPORTED),
        REGION_LIMIT_EXCEEDED(Status.CONSERVATIVE_UNSUPPORTED),
        NOT_REGISTERED(Status.CONSERVATIVE_UNSUPPORTED),
        INVALID_RESOLVER_OUTPUT(Status.CONSERVATIVE_UNSUPPORTED);

        private final Status expectedStatus;

        Reason(Status expectedStatus) {
            this.expectedStatus = expectedStatus;
        }

        public Status expectedStatus() {
            return expectedStatus;
        }
    }
}
