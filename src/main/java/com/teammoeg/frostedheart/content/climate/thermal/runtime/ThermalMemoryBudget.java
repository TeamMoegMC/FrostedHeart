/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime;

/**
 * Admission target shared by a server and its accounting categories. New work
 * can wait at the target; bounded completion of accepted work must still finish.
 */
public final class ThermalMemoryBudget {
    /** A fallible preparation, never a reason to delete an accepted input or field. */
    public static final class AllocationRefused extends RuntimeException {
        public AllocationRefused(long bytes) { super("Thermal storage allocation of " + bytes + " bytes was refused"); }
    }
    private final ThermalMemoryBudget parent;
    private final Object lock;
    private final java.util.function.LongSupplier limit;

    private long usedBytes;
    private long peakUsedBytes;
    private long capacityRevision;
    private long admissionRefusals;

    /** Optional-work admission refusals, independent of physical commit receipts. */
    public long admissionRefusals() {
        synchronized (lock) { return (parent == null ? this : parent).admissionRefusals; }
    }
    private void refused() { (parent == null ? this : parent).admissionRefusals++; }
    private final java.util.IdentityHashMap<Runnable, Boolean> capacityWaiters = new java.util.IdentityHashMap<>();

    public long capacityRevision() {
        synchronized (lock) { return (parent == null ? this : parent).capacityRevision; }
    }

    /** One registration per waiting owner; real releases wake it without polling the pool. */
    public void whenCapacityChanges(long observed, Runnable continuation) {
        boolean changed;
        synchronized (lock) {
            var root = parent == null ? this : parent;
            changed = root.capacityRevision != observed;
            if (!changed) root.capacityWaiters.put(continuation, Boolean.TRUE);
        }
        if (changed) continuation.run();
    }
    public void cancelCapacityWait(Runnable continuation) {
        synchronized (lock) { (parent == null ? this : parent).capacityWaiters.remove(continuation); }
    }
    private java.util.List<Runnable> capacityChanged() {
        var root = parent == null ? this : parent;
        root.capacityRevision++;
        var notified = java.util.List.copyOf(root.capacityWaiters.keySet());
        root.capacityWaiters.clear();
        return notified;
    }

    /** Creates the server-global root budget. */
    public ThermalMemoryBudget(long limitBytes) {
        this(null, limitBytes);
    }

    public ThermalMemoryBudget(java.util.function.LongSupplier limit) {
        this.parent = null;
        this.lock = new Object();
        this.limit = java.util.Objects.requireNonNull(limit);
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
        this.limit = () -> limitBytes;
    }

    /** Creates a dimension budget whose reservations also charge this server budget. */
    public ThermalMemoryBudget createDimensionBudget(long dimensionLimitBytes) {
        if (parent != null) {
            throw new IllegalStateException("only the server budget can own dimensions");
        }
        return new ThermalMemoryBudget(this, dimensionLimitBytes);
    }

    /** A reporting category has no second arbitrary cap beneath the server target. */
    public ThermalMemoryBudget createCategory() { return createDimensionBudget(Long.MAX_VALUE); }

    /**
     * Only for a bounded save or finishing an already accepted state transition.
     * The full overlap remains charged, including any excess above the target.
     * Never use this to admit an arbitrary number of new preparation tasks.
     */
    public Reservation reserveCompletion(long bytes) {
        if (bytes <= 0) throw new IllegalArgumentException("bytes must be positive");
        synchronized (lock) {
            apply(bytes);
            if (parent != null) parent.apply(bytes);
            return new Reservation(this, bytes);
        }
    }

    /** Optional work waits when its category or server admission target is full. */
    public Reservation tryReserve(long bytes) {
        if (bytes <= 0L) {
            throw new IllegalArgumentException("bytes must be positive");
        }
        synchronized (lock) {
            if (!canAdmitHere(bytes)
                    || parent != null && !parent.canAdmitHere(bytes)) {
                refused();
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
        return bytes <= Math.max(0L, limitBytes() - usedBytes);
    }

    public long usedBytes() {
        synchronized (lock) { return usedBytes; }
    }

    /** Includes short-lived admitted overlap even when periodic sampling misses it. */
    public long peakUsedBytes() {
        synchronized (lock) { return peakUsedBytes; }
    }

    /** Current server charge, including all dimension children and preparation reservations. */
    public long rootUsedBytes() {
        synchronized (lock) { return parent == null ? usedBytes : parent.usedBytes; }
    }

    public long rootPeakUsedBytes() {
        synchronized (lock) { return parent == null ? peakUsedBytes : parent.peakUsedBytes; }
    }

    public long limitBytes() { return limit.getAsLong(); }

    private void apply(long bytes) {
        usedBytes += bytes;
        peakUsedBytes = Math.max(peakUsedBytes, usedBytes);
    }

    private void release(long bytes) {
        if (bytes == 0) return;
        java.util.List<Runnable> notified;
        synchronized (lock) {
            apply(-bytes);
            if (parent != null) {
                parent.apply(-bytes);
            }
            notified = capacityChanged();
        }
        notified.forEach(Runnable::run);
    }

    /**
     * Ownership token for one admitted backing store. To replace an array,
     * reserve its complete replacement first, swap storage, then release the
     * old token so peak double ownership remains charged.
     */
    public static final class Reservation implements AutoCloseable {
        private final ThermalMemoryBudget owner;
        private long bytes;
        private boolean released;

        private Reservation(
                ThermalMemoryBudget owner,
                long bytes
        ) {
            this.owner = owner;
            this.bytes = bytes;
        }

        public long bytes() {
            synchronized (owner.lock) { return released ? 0L : bytes; }
        }

        /** Move one already charged immutable allocation family into shared ownership. */
        public BackingLease shareBacking(long backingBytes) {
            synchronized (owner.lock) {
                if (released || backingBytes <= 0 || backingBytes > bytes)
                    throw new IllegalArgumentException("Backing family is not owned by this reservation");
                bytes -= backingBytes;
                return new BackingLease(new Reservation(owner, backingBytes));
            }
        }

        public Reservation split(long retainedBytes) {
            synchronized (owner.lock) {
                if (released || retainedBytes <= 0 || retainedBytes > bytes)
                    throw new IllegalArgumentException("Split exceeds the admitted owner");
                bytes -= retainedBytes;
                return new Reservation(owner, retainedBytes);
            }
        }

        /** A bounded immutable view needed to finish the current accepted task. */
        public Reservation reserveCompletionSibling(long requiredBytes) {
            synchronized (owner.lock) {
                if (released) throw new IllegalStateException("reservation was released");
                return owner.reserveCompletion(requiredBytes);
            }
        }

        /** Adjusts one owner's retained backing while preserving both budget limits. */
        public boolean tryResize(long nextBytes) {
            if (nextBytes <= 0L) throw new IllegalArgumentException("nextBytes must be positive");
            java.util.List<Runnable> notified = java.util.List.of();
            synchronized (owner.lock) {
                if (released) throw new IllegalStateException("reservation was released");
                long additional = nextBytes - bytes;
                if (additional > 0 && (!owner.canAdmitHere(additional)
                        || owner.parent != null && !owner.parent.canAdmitHere(additional))) { owner.refused(); return false; }
                owner.apply(additional);
                if (owner.parent != null) owner.parent.apply(additional);
                bytes = nextBytes;
                if (additional < 0) notified = owner.capacityChanged();
            }
            notified.forEach(Runnable::run);
            return true;
        }

        /** Retain the finite result/overlap of work that already owns its inputs. */
        public void completeResize(long nextBytes) {
            if (nextBytes <= 0) throw new IllegalArgumentException("nextBytes must be positive");
            java.util.List<Runnable> notified = java.util.List.of();
            synchronized (owner.lock) {
                if (released) throw new IllegalStateException("reservation was released");
                long additional = nextBytes - bytes;
                owner.apply(additional);
                if (owner.parent != null) owner.parent.apply(additional);
                bytes = nextBytes;
                if (additional < 0) notified = owner.capacityChanged();
            }
            notified.forEach(Runnable::run);
        }

        /** Multiple construction helpers can grow their shared owner's charge atomically. */
        public boolean tryGrow(long additionalBytes) {
            if (additionalBytes < 0) throw new IllegalArgumentException("allocation size is negative");
            synchronized (owner.lock) {
                if (released) throw new IllegalStateException("reservation was released");
                long next = Math.addExact(bytes, additionalBytes);
                if (!owner.canAdmitHere(additionalBytes)
                        || owner.parent != null && !owner.parent.canAdmitHere(additionalBytes)) { owner.refused(); return false; }
                owner.apply(additionalBytes);
                if (owner.parent != null) owner.parent.apply(additionalBytes);
                bytes = next;
                return true;
            }
        }

        /**
         * Backing of an admitted operation: charge the complete old/new array
         * overlap before allocation. Task admission, not each column resize,
         * decides whether another preparation may start.
         */
        public void beforeAllocation(long additionalBytes) {
            if (additionalBytes < 0) throw new IllegalArgumentException("allocation size is negative");
            synchronized (owner.lock) {
                if (released) throw new IllegalStateException("reservation was released");
                owner.apply(additionalBytes);
                if (owner.parent != null) owner.parent.apply(additionalBytes);
                bytes = Math.addExact(bytes, additionalBytes);
            }
        }

        /** Only after the writer has removed the last reference owned by this reservation. */
        public void afterReplacement(long retiredBytes) {
            if (retiredBytes < 0) throw new IllegalArgumentException("retired size is negative");
            if (retiredBytes != 0) tryResize(Math.max(1, bytes() - retiredBytes));
        }

        /** Transfer retained bytes between live owners without charging the same backing twice. */
        public void transferTo(Reservation destination, long transferredBytes) {
            var sourceRoot = owner.parent == null ? owner : owner.parent;
            var destinationRoot = destination.owner.parent == null ? destination.owner : destination.owner.parent;
            if (destinationRoot != sourceRoot || transferredBytes < 0)
                throw new IllegalArgumentException("Budget transfer requires the same server root");
            synchronized (owner.lock) {
                if (released || destination.released || transferredBytes > bytes)
                    throw new IllegalStateException("Budget transfer no longer owns its admitted bytes");
                if (destination.owner != owner) {
                    if (destination.owner.parent != null && !destination.owner.canAdmitHere(transferredBytes))
                        throw new AllocationRefused(transferredBytes);
                    if (owner.parent != null) owner.apply(-transferredBytes);
                    if (destination.owner.parent != null) destination.owner.apply(transferredBytes);
                }
                bytes -= transferredBytes;
                destination.bytes = Math.addExact(destination.bytes, transferredBytes);
            }
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

    /** Only a known immutable allocation family is shared; no object graph is traversed. */
    public static final class BackingLease implements AutoCloseable {
        private final Reservation reservation;
        private int references = 1;

        private BackingLease(Reservation reservation) { this.reservation = reservation; }

        public synchronized BackingLease retain() {
            if (references == 0) throw new IllegalStateException("Backing family has retired");
            references = Math.incrementExact(references);
            return this;
        }

        public synchronized Reservation split(long bytes) {
            if (references == 0) throw new IllegalStateException("Backing family has retired");
            return reservation.split(bytes);
        }

        @Override public void close() {
            boolean release;
            synchronized (this) {
                if (references == 0) throw new IllegalStateException("Backing family released twice");
                release = --references == 0;
            }
            if (release) reservation.close();
        }
    }
}
