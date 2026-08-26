/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime;

/**
 * Two-level byte admission shared by a server budget and its dimensions.
 * Optional storage cannot consume the quota reserved for critical source,
 * queue, and recovery state.
 */
public final class ThermalMemoryBudget {
    public enum AllocationClass {
        CRITICAL,
        OPTIONAL
    }

    private final ThermalMemoryBudget parent;
    private final Object lock;
    private final long limitBytes;
    private final long criticalReserveBytes;

    private long criticalUsedBytes;
    private long optionalUsedBytes;

    /** Creates the server-global root budget. */
    public ThermalMemoryBudget(long limitBytes, long criticalReserveBytes) {
        this(null, limitBytes, criticalReserveBytes);
    }

    private ThermalMemoryBudget(
            ThermalMemoryBudget parent,
            long limitBytes,
            long criticalReserveBytes
    ) {
        if (limitBytes <= 0L) {
            throw new IllegalArgumentException("limitBytes must be positive");
        }
        if (criticalReserveBytes < 0L || criticalReserveBytes > limitBytes) {
            throw new IllegalArgumentException(
                    "criticalReserveBytes must be within the budget limit");
        }
        this.parent = parent;
        this.lock = parent == null ? new Object() : parent.lock;
        this.limitBytes = limitBytes;
        this.criticalReserveBytes = criticalReserveBytes;
    }

    /** Creates a dimension budget whose reservations also charge this server budget. */
    public ThermalMemoryBudget createDimensionBudget(
            long dimensionLimitBytes,
            long dimensionCriticalReserveBytes
    ) {
        if (parent != null) {
            throw new IllegalStateException("only the server budget can own dimensions");
        }
        return new ThermalMemoryBudget(
                this, dimensionLimitBytes, dimensionCriticalReserveBytes);
    }

    /** Returns null when either the dimension or server hard cap refuses admission. */
    public Reservation tryReserve(AllocationClass allocationClass, long bytes) {
        if (allocationClass == null) {
            throw new IllegalArgumentException("allocationClass is required");
        }
        if (bytes <= 0L) {
            throw new IllegalArgumentException("bytes must be positive");
        }
        synchronized (lock) {
            if (!canAdmitHere(allocationClass, bytes)
                    || parent != null && !parent.canAdmitHere(allocationClass, bytes)) {
                return null;
            }
            apply(allocationClass, bytes);
            if (parent != null) {
                parent.apply(allocationClass, bytes);
            }
            return new Reservation(this, allocationClass, bytes);
        }
    }

    private boolean canAdmitHere(AllocationClass allocationClass, long bytes) {
        return bytes <= remainingHere(allocationClass);
    }

    private long remainingHere(AllocationClass allocationClass) {
        long used = criticalUsedBytes + optionalUsedBytes;
        long totalRemaining = Math.max(0L, limitBytes - used);
        if (allocationClass == AllocationClass.CRITICAL) {
            return totalRemaining;
        }
        long optionalRemaining = Math.max(
                0L,
                limitBytes - criticalReserveBytes - optionalUsedBytes);
        return Math.min(totalRemaining, optionalRemaining);
    }

    private void apply(AllocationClass allocationClass, long bytes) {
        if (allocationClass == AllocationClass.CRITICAL) {
            criticalUsedBytes += bytes;
        } else {
            optionalUsedBytes += bytes;
        }
    }

    private void release(AllocationClass allocationClass, long bytes) {
        synchronized (lock) {
            apply(allocationClass, -bytes);
            if (parent != null) {
                parent.apply(allocationClass, -bytes);
            }
        }
    }

    /**
     * Ownership token for one admitted backing store. To replace an array,
     * reserve its complete replacement first, swap storage, then release the
     * old token so peak double ownership remains charged.
     */
    public static final class Reservation implements AutoCloseable {
        private final ThermalMemoryBudget owner;
        private final AllocationClass allocationClass;
        private final long bytes;
        private boolean released;

        private Reservation(
                ThermalMemoryBudget owner,
                AllocationClass allocationClass,
                long bytes
        ) {
            this.owner = owner;
            this.allocationClass = allocationClass;
            this.bytes = bytes;
        }

        @Override
        public void close() {
            synchronized (owner.lock) {
                if (released) {
                    return;
                }
                released = true;
            }
            owner.release(allocationClass, bytes);
        }
    }
}
