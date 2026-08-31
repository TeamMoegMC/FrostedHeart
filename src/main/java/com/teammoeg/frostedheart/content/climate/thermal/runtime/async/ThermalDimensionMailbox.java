/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.async;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import java.util.Objects;

/**
 * 一个维度在主线程与共享 worker pool 之间的单槽 mailbox。
 *
 * <p>任意时刻至多有一个 in-flight batch；completion 由主线程显式 ACK，
 * close 也通过同一状态机完成。该类只负责传输，不解释热学载荷。</p>
 */
public final class ThermalDimensionMailbox implements AutoCloseable,
        ThermalWorkerPool.LifecycleOwner {
    private enum State {
        IDLE,
        QUEUED,
        RUNNING,
        AWAITING_ACK,
        CLOSE_REQUESTED,
        CLOSED
    }

    private final ThermalWorkerPool workers;
    private final ThermalDimensionProcessor processor;
    private State state = State.IDLE;
    private ThermalCompletion completion;
    private boolean processorClosed;

    public ThermalDimensionMailbox(
            ThermalWorkerPool workers,
            ThermalDimensionProcessor processor
    ) {
        this.workers = Objects.requireNonNull(workers, "workers");
        this.processor = Objects.requireNonNull(processor, "processor");
        workers.registerLifecycleOwner(this);
    }

    public synchronized boolean submit(ThermalInputBatch batch) {
        Objects.requireNonNull(batch, "batch");
        if (state != State.IDLE) {
            return false;
        }
        state = State.QUEUED;
        if (workers.tryExecute(() -> execute(batch))) {
            return true;
        }
        state = State.IDLE;
        return false;
    }

    public synchronized ThermalCompletion peekCompletion() {
        return state == State.AWAITING_ACK ? completion : null;
    }

    public synchronized void acknowledgeCompletion(long batchSequence) {
        if (state != State.AWAITING_ACK || completion == null
                || completion.batchSequence() != batchSequence) {
            throw new IllegalStateException(
                    "thermal completion ACK does not own the mailbox");
        }
        boolean terminal =
                completion.status() == ThermalCompletion.Status.ENGINE_FAILED;
        completion = null;
        state = terminal ? State.CLOSED : State.IDLE;
        if (terminal) {
            workers.unregisterLifecycleOwner(this);
        }
    }

    @Override
    public void close() {
        boolean schedule = false;
        synchronized (this) {
            switch (state) {
                case IDLE -> {
                    state = State.CLOSE_REQUESTED;
                    schedule = true;
                }
                case AWAITING_ACK -> {
                    completion = null;
                    state = State.CLOSE_REQUESTED;
                    schedule = !processorClosed;
                    if (!schedule) {
                        state = State.CLOSED;
                        workers.unregisterLifecycleOwner(this);
                    }
                }
                case QUEUED, RUNNING -> state = State.CLOSE_REQUESTED;
                case CLOSE_REQUESTED, CLOSED -> {
                    return;
                }
            }
        }
        if (schedule) {
            workers.executeLifecycle(this::closeProcessorTask);
        }
    }

    @Override
    public void requestWorkerPoolClose() {
        close();
    }

    private void execute(ThermalInputBatch batch) {
        boolean closeBeforeProcess;
        synchronized (this) {
            closeBeforeProcess = state == State.CLOSE_REQUESTED;
            if (!closeBeforeProcess && state != State.QUEUED) {
                throw new IllegalStateException(
                        "thermal task does not own the queued mailbox");
            }
            if (!closeBeforeProcess) {
                state = State.RUNNING;
            }
        }
        if (closeBeforeProcess) {
            closeProcessorTask();
            return;
        }

        ThermalCompletion result;
        boolean terminal = false;
        try {
            result = processor.process(batch);
        } catch (Throwable failure) {
            RuntimeException diagnostic = failure instanceof RuntimeException runtime
                    ? runtime
                    : new IllegalStateException(
                            "thermal dimension engine failed", failure);
            try {
                closeProcessorOnce();
            } catch (Throwable closeFailure) {
                diagnostic.addSuppressed(closeFailure);
            }
            result = failedCompletion(batch, diagnostic);
            terminal = true;
        }

        boolean closeRequested;
        synchronized (this) {
            closeRequested = state == State.CLOSE_REQUESTED;
            if (!closeRequested) {
                if (state != State.RUNNING) {
                    throw new IllegalStateException(
                            "thermal task lost running ownership");
                }
                completion = result;
                state = State.AWAITING_ACK;
            }
        }
        if (!closeRequested) {
            return;
        }
        if (!terminal) {
            closeProcessorTask();
        } else {
            synchronized (this) {
                state = State.CLOSED;
            }
            workers.unregisterLifecycleOwner(this);
        }
    }

    private static ThermalCompletion failedCompletion(
            ThermalInputBatch batch,
            RuntimeException failure
    ) {
        return new ThermalCompletion(
                batch.dimensionGeneration(),
                batch.sequence(),
                ThermalCompletion.Status.ENGINE_FAILED,
                failure,
                ThermalCompletion.NO_PHASE_REQUESTS,
                ThermalCompletion.NO_RESYNC_TOKENS,
                ThermalCompletion.NO_RESIDENCY_UPDATES);
    }

    private void closeProcessorTask() {
        try {
            closeProcessorOnce();
        } finally {
            synchronized (this) {
                completion = null;
                state = State.CLOSED;
            }
            workers.unregisterLifecycleOwner(this);
        }
    }

    private void closeProcessorOnce() {
        synchronized (this) {
            if (processorClosed) {
                return;
            }
            processorClosed = true;
        }
        processor.close();
    }
}
