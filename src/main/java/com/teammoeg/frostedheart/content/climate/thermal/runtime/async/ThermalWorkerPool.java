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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.async;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Shared bounded worker set for dimension engines and their claimable assembly helpers. */
public final class ThermalWorkerPool implements AutoCloseable {
    private static final int MAXIMUM_THREADS = 4;
    private static final int MAXIMUM_QUEUED_DIMENSIONS = 64;
    private static final Runnable WAKE_LIFECYCLE = () -> { };
    private static final Runnable STOP_WORKER = () -> { };
    private static ThermalWorkerPool shared;

    private enum State {
        OPEN,
        CLOSING,
        STOPPING,
        CLOSED
    }

    /** A worker-owned engine whose close request is coordinated by this pool. */
    public interface LifecycleOwner {
        void requestWorkerPoolClose();
    }

    /** Marks work charged to the shared-grid service allowance, including inline helpers once. */
    public interface RegionalTask extends Runnable {
        default boolean controlPending() { return false; }
    }

    private record Deferred(long readyAt, Runnable task) implements Comparable<Deferred> {
        @Override public int compareTo(Deferred other) { return Long.compare(readyAt, other.readyAt); }
    }
    @jdk.jfr.Name("frostedheart.ThermalServiceWait")
    @jdk.jfr.StackTrace(false)
    private static final class ServiceWait extends jdk.jfr.Event {
        public long delayNanos;
        public double debtNanos;
    }

    private final ArrayBlockingQueue<Runnable> dimensionTasks =
            new ArrayBlockingQueue<>(MAXIMUM_QUEUED_DIMENSIONS);
    private final ConcurrentLinkedQueue<Runnable> lifecycleTasks =
            new ConcurrentLinkedQueue<>();
    // One entry per yielded running owner; owners cannot submit a second round
    // before this one is taken. This is independent of the bounded helper queue.
    private final ConcurrentLinkedQueue<Runnable> continuations = new ConcurrentLinkedQueue<>();
    private final Object submissionGate = new Object();
    private final Set<LifecycleOwner> lifecycleOwners =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Thread[] workers;
    private final java.util.PriorityQueue<Deferred> deferred = new java.util.PriorityQueue<>();
    private long serviceRepaidAt = System.nanoTime();
    private double serviceDebt;
    private boolean lifecycleWakeQueued;
    private volatile State state = State.OPEN;

    private ThermalWorkerPool(int threadCount) {
        workers = new Thread[threadCount];
        ThreadFactory factory = CUtils.makeThreadFactory(
                "frosted-heart-thermal", true);
        for (int index = 0; index < workers.length; index++) {
            workers[index] = factory.newThread(this::workerLoop);
            workers[index].start();
        }
    }

    public static synchronized ThermalWorkerPool startShared() {
        if (shared == null) {
            int processors = Runtime.getRuntime().availableProcessors();
            int threads = Math.max(1, Math.min(MAXIMUM_THREADS, processors - 1));
            shared = new ThermalWorkerPool(threads);
        }
        return shared;
    }

    public static synchronized ThermalWorkerPool shared() {
        return startShared();
    }

    /** Maximum simultaneous assembly work on the existing pool, including its coordinator. */
    public int parallelism() { return workers.length; }

    public long regionalWorkAllowanceNanos() {
        return com.teammoeg.frostedheart.infrastructure.config.FHConfig.COMMON.THERMAL_RUNTIME.continuousAirWorkBudgetMsPerSecond.get() * 1_000_000L;
    }

    /** Promote the already queued owner once; never dispatch a duplicate coordinator. */
    public void wakeControlOwners() {
        synchronized (submissionGate) {
            var iterator = deferred.iterator();
            while (iterator.hasNext()) {
                var waiting = iterator.next();
                if (waiting.task instanceof RegionalTask regional && regional.controlPending()) {
                    iterator.remove(); continuations.add(waiting.task);
                }
            }
            if (!lifecycleWakeQueued && dimensionTasks.offer(WAKE_LIFECYCLE)) lifecycleWakeQueued = true;
        }
    }

    private long serviceReadyAt(long now) {
        repayServiceDebt(now);
        long allowance = regionalWorkAllowanceNanos();
        // A per-second service allowance permits one period's burst. Requiring
        // repayment after every small slice serializes otherwise ready work.
        if (serviceDebt <= allowance) return now;
        long delay = Math.max(1L, (long) Math.ceil((serviceDebt - allowance)
                * 1_000_000_000.0 / allowance));
        return now + delay;
    }

    /** Called under submissionGate; idle time repays work without accumulating burst credit. */
    private void repayServiceDebt(long now) {
        long elapsed = now - serviceRepaidAt;
        if (elapsed <= 0) return;
        serviceDebt = Math.max(0, serviceDebt
                - elapsed * (regionalWorkAllowanceNanos() / 1_000_000_000.0));
        serviceRepaidAt = now;
    }

    private void chargeService(long nanos) {
        if (nanos <= 0) return;
        synchronized (submissionGate) {
            repayServiceDebt(System.nanoTime());
            serviceDebt += nanos;
        }
    }

    /** Reuse registered dimension ownership when dividing the regional active-time allowance. */
    public int dimensionCount() {
        synchronized (submissionGate) { return Math.max(1, lifecycleOwners.size()); }
    }

    public static synchronized void closeShared() {
        ThermalWorkerPool current = shared;
        if (current == null) {
            return;
        }
        current.close();
        shared = null;
    }

    /** Returns false when shutdown or the bounded dimension queue is full. */
    public boolean tryExecute(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("thermal task is required");
        }
        synchronized (submissionGate) {
            if (state != State.OPEN) {
                return false;
            }
            return dimensionTasks.offer(task);
        }
    }

    /** A yielded owner keeps its existing slot instead of competing for a new input slot. */
    public void executeContinuation(Runnable task) {
        synchronized (submissionGate) {
            if (state == State.OPEN) continuations.add(task);
            else lifecycleTasks.add(task); // close-requested owner only tears down
            if (!lifecycleWakeQueued && dimensionTasks.offer(WAKE_LIFECYCLE)) lifecycleWakeQueued = true;
        }
    }

    /** Registers one mailbox before it can submit work to this pool. */
    public void registerLifecycleOwner(LifecycleOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException("thermal lifecycle owner is required");
        }
        synchronized (submissionGate) {
            if (state != State.OPEN) {
                throw new IllegalStateException("thermal worker pool is closing");
            }
            if (!lifecycleOwners.add(owner)) {
                throw new IllegalStateException("thermal lifecycle owner is already registered");
            }
        }
    }

    /** Releases a mailbox after its processor has closed on a worker. */
    public void unregisterLifecycleOwner(LifecycleOwner owner) {
        synchronized (submissionGate) {
            lifecycleOwners.remove(owner);
            submissionGate.notifyAll();
        }
    }

    /**
     * Schedules worker-owned teardown without consuming bounded dimension
     * capacity. A wake marker is only needed when every worker is idle.
     */
    public void executeLifecycle(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("thermal lifecycle task is required");
        }
        synchronized (submissionGate) {
            if (state != State.OPEN && state != State.CLOSING) {
                throw new IllegalStateException(
                        "thermal lifecycle submission after pool shutdown began");
            }
            lifecycleTasks.add(task);
            if (!lifecycleWakeQueued && dimensionTasks.offer(WAKE_LIFECYCLE)) {
                lifecycleWakeQueued = true;
            }
        }
    }

    private void workerLoop() {
        boolean preferContinuation = false;
        while (true) {
            drainLifecycleTasks();
            Runnable task;
            try {
                synchronized (submissionGate) {
                    if (!deferred.isEmpty() && (state != State.OPEN || deferred.peek().readyAt <= System.nanoTime()))
                        continuations.add(deferred.remove().task);
                }
                task = preferContinuation ? continuations.poll() : dimensionTasks.poll();
                if (task == null) task = preferContinuation ? dimensionTasks.poll() : continuations.poll();
                if (task == null) {
                    long delay;
                    synchronized (submissionGate) { delay = deferred.isEmpty() ? Long.MAX_VALUE : Math.max(1, deferred.peek().readyAt - System.nanoTime()); }
                    task = delay == Long.MAX_VALUE ? dimensionTasks.take()
                            : dimensionTasks.poll(delay, java.util.concurrent.TimeUnit.NANOSECONDS);
                    if (task == null) continue;
                }
                preferContinuation = !preferContinuation;
            } catch (InterruptedException ignored) {
                continue;
            }
            if (task == STOP_WORKER) {
                drainLifecycleTasks();
                return;
            }
            if (task == WAKE_LIFECYCLE) {
                synchronized (submissionGate) {
                    lifecycleWakeQueued = false;
                }
                continue;
            }
            boolean regional = task instanceof RegionalTask;
            if (regional && state == State.OPEN && !((RegionalTask) task).controlPending()) {
                synchronized (submissionGate) {
                    long now = System.nanoTime(), readyAt = serviceReadyAt(now);
                    if (readyAt > now) {
                        if (jdk.jfr.FlightRecorder.isInitialized()) {
                            var event = new ServiceWait(); event.delayNanos = readyAt - now;
                            event.debtNanos = serviceDebt; event.commit();
                        }
                        deferred.add(new Deferred(readyAt, task)); continue;
                    }
                }
            }
            long wallBefore = System.nanoTime();
            try {
                task.run();
            } catch (Throwable failure) {
                FHMain.LOGGER.error("Uncaught thermal worker task failure", failure);
            } finally {
                // Queued waiting is outside this interval. Inline helpers are part
                // of their executing task and are charged only once here.
                if (regional) chargeService(System.nanoTime() - wallBefore);
            }
        }
    }

    private void drainLifecycleTasks() {
        Runnable task;
        int remaining = 8;
        while (remaining-- > 0 && (task = lifecycleTasks.poll()) != null) {
            try {
                task.run();
            } catch (Throwable failure) {
                FHMain.LOGGER.error("Thermal worker lifecycle task failed", failure);
            }
        }
    }

    @Override
    public void close() {
        LifecycleOwner[] owners;
        synchronized (submissionGate) {
            if (state == State.CLOSED) {
                return;
            }
            if (state != State.OPEN) {
                awaitClosed();
                return;
            }
            for (Thread worker : workers) {
                if (worker == Thread.currentThread()) {
                    throw new IllegalStateException(
                            "thermal worker pool cannot close itself");
                }
            }
            state = State.CLOSING;
            owners = lifecycleOwners.toArray(LifecycleOwner[]::new);
        }

        for (LifecycleOwner owner : owners) {
            try {
                owner.requestWorkerPoolClose();
            } catch (Throwable failure) {
                FHMain.LOGGER.error(
                        "Thermal lifecycle owner rejected pool shutdown", failure);
            }
        }

        boolean interrupted = false;
        synchronized (submissionGate) {
            while (!lifecycleOwners.isEmpty()) {
                try { submissionGate.wait(); }
                catch (InterruptedException ignored) { interrupted = true; }
            }
            while (!deferred.isEmpty()) lifecycleTasks.add(deferred.remove().task);
            state = State.STOPPING;
            Runnable continuation;
            while ((continuation = continuations.poll()) != null) lifecycleTasks.add(continuation);
        }

        int submittedStops = 0;
        while (submittedStops < workers.length) {
            try {
                dimensionTasks.put(STOP_WORKER);
                submittedStops++;
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        for (Thread worker : workers) {
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        synchronized (submissionGate) {
            if (!lifecycleOwners.isEmpty()) {
                FHMain.LOGGER.error(
                        "Thermal worker pool stopped with {} unclosed dimension engines",
                        lifecycleOwners.size());
                lifecycleOwners.clear();
            }
            lifecycleTasks.clear();
            continuations.clear();
            state = State.CLOSED;
            submissionGate.notifyAll();
        }
    }

    private void awaitClosed() {
        boolean interrupted = false;
        synchronized (submissionGate) {
            while (state != State.CLOSED) {
                try {
                    submissionGate.wait();
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
