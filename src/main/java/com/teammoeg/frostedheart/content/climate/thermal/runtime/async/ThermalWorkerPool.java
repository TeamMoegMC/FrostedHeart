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

/** Shared bounded worker set for independent dimension-owned thermal engines. */
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

    private final ArrayBlockingQueue<Runnable> dimensionTasks =
            new ArrayBlockingQueue<>(MAXIMUM_QUEUED_DIMENSIONS);
    private final ConcurrentLinkedQueue<Runnable> lifecycleTasks =
            new ConcurrentLinkedQueue<>();
    private final Object submissionGate = new Object();
    private final Set<LifecycleOwner> lifecycleOwners =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Thread[] workers;
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
        while (true) {
            drainLifecycleTasks();
            Runnable task;
            try {
                task = dimensionTasks.take();
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
            try {
                task.run();
            } catch (Throwable failure) {
                FHMain.LOGGER.error("Uncaught thermal worker task failure", failure);
            }
        }
    }

    private void drainLifecycleTasks() {
        Runnable task;
        while ((task = lifecycleTasks.poll()) != null) {
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

        synchronized (submissionGate) {
            state = State.STOPPING;
        }

        boolean interrupted = false;
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
