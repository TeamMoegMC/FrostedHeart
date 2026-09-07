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
 * Byte admission shared by a server budget and its dimension-local children.
 */
public final class ThermalMemoryBudget {
    private final ThermalMemoryBudget parent;
    private final Object lock;
    private final long limitBytes;

    private long usedBytes;

    /** Creates the server-global root budget. */
    public ThermalMemoryBudget(long limitBytes) {
        this(null, limitBytes);
    }

    private ThermalMemoryBudget(
            ThermalMemoryBudget parent,
            long limitBytes
    ) {
        if (limitBytes <= 0L) {
            throw new IllegalArgumentException("limitBytes must be positive");
        }
        this.parent = parent;
        this.lock = parent == null ? new Object() : parent.lock;
        this.limitBytes = limitBytes;
    }

    /** Creates a dimension budget whose reservations also charge this server budget. */
    public ThermalMemoryBudget createDimensionBudget(long dimensionLimitBytes) {
        if (parent != null) {
            throw new IllegalStateException("only the server budget can own dimensions");
        }
        return new ThermalMemoryBudget(this, dimensionLimitBytes);
    }

    /** Returns null when either the dimension or server hard cap refuses admission. */
    public Reservation tryReserve(long bytes) {
        if (bytes <= 0L) {
            throw new IllegalArgumentException("bytes must be positive");
        }
        synchronized (lock) {
            if (!canAdmitHere(bytes)
                    || parent != null && !parent.canAdmitHere(bytes)) {
                return null;
            }
            apply(bytes);
            if (parent != null) {
                parent.apply(bytes);
            }
            return new Reservation(this, bytes);
        }
    }

    private boolean canAdmitHere(long bytes) {
        return bytes <= Math.max(0L, limitBytes - usedBytes);
    }

    private void apply(long bytes) {
        usedBytes += bytes;
    }

    private void release(long bytes) {
        synchronized (lock) {
            apply(-bytes);
            if (parent != null) {
                parent.apply(-bytes);
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
        private final long bytes;
        private boolean released;

        private Reservation(
                ThermalMemoryBudget owner,
                long bytes
        ) {
            this.owner = owner;
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
            owner.release(bytes);
        }
    }
}
