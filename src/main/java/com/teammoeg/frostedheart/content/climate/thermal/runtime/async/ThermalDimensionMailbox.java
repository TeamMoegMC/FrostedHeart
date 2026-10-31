/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.async;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;

import java.util.ArrayDeque;
import java.util.Objects;

/** One serial coordinator per dimension; input ownership and world-effect notifications advance independently. */
public final class ThermalDimensionMailbox implements AutoCloseable, ThermalWorkerPool.LifecycleOwner {
    private enum State { IDLE, QUEUED, RUNNING, AWAITING_ACK, CLOSE_REQUESTED, CLOSED }

    private final ThermalWorkerPool workers;
    private final ThermalDimensionEngine engine;
    private final ArrayDeque<ThermalCompletion> completions = new ArrayDeque<>();
    private State state = State.IDLE;
    private boolean terminalPending, engineClosed, closingEngineRound;
    private RoundWake waitingRound;
    private long inputGeneration, lastSubmittedSequence, progressRevision;

    public ThermalDimensionMailbox(ThermalWorkerPool workers, ThermalDimensionEngine engine) {
        this.workers = Objects.requireNonNull(workers, "workers");
        this.engine = Objects.requireNonNull(engine, "engine");
        workers.registerLifecycleOwner(this);
    }

    public synchronized boolean submit(ThermalInputEnvelope batch) {
        if (!canQueueInput()) return false;
        engine.offerInput(batch);
        inputGeneration = batch.dimensionGeneration();
        lastSubmittedSequence = batch.sequence();
        if (state == State.IDLE || state == State.AWAITING_ACK) {
            state = State.QUEUED;
            schedule();
        } else if (waitingRound != null) waitingRound.request(false);
        return true;
    }

    public synchronized boolean canQueueInput() {
        return !terminalPending && state != State.CLOSE_REQUESTED && state != State.CLOSED;
    }

    public synchronized void controlChanged() {
        if (state == State.CLOSE_REQUESTED || state == State.CLOSED
                || terminalPending && !engine.checkpointRecovery()) return;
        if (state == State.IDLE || state == State.AWAITING_ACK) {
            state = State.QUEUED;
            schedule();
        } else if (waitingRound != null) waitingRound.request(false);
        workers.wakeControlOwners();
    }

    public synchronized long progressRevision() { return progressRevision; }
    public synchronized boolean workPending() {
        return engine.hasWork() || state == State.RUNNING || state == State.QUEUED;
    }

    public synchronized void awaitProgress(long observed) {
        boolean interrupted = false;
        while (progressRevision == observed && workPending() && (!terminalPending || engine.checkpointRecovery())) {
            try { wait(); } catch (InterruptedException exception) { interrupted = true; }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    public synchronized ThermalCompletion peekCompletion() { return completions.peekFirst(); }

    public void acknowledgeCompletion(long sequence) {
        boolean terminal;
        RoundWake releasedSlot = null;
        synchronized (this) {
            var completion = completions.peekFirst();
            if (completion == null || completion.effectSequence() != sequence)
                throw new IllegalStateException("thermal completion ACK does not own the mailbox");
            completions.removeFirst();
            terminal = completion.status() == ThermalCompletion.Status.ENGINE_FAILED;
            if (terminal) state = State.CLOSE_REQUESTED;
            else if (state == State.AWAITING_ACK && engine.hasWork()) {
                state = State.QUEUED;
                schedule();
            } else if (state == State.AWAITING_ACK && completions.isEmpty()) state = State.IDLE;
            else if (state == State.QUEUED && waitingRound != null) releasedSlot = waitingRound;
        }
        if (terminal) workers.executeLifecycle(this::closeEngine);
        else if (releasedSlot != null) releasedSlot.request(false);
    }

    public synchronized void awaitClosed() {
        boolean interrupted = false;
        while (state != State.CLOSED) {
            try { wait(); } catch (InterruptedException exception) { interrupted = true; }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private void schedule() {
        workers.executeContinuation(new ThermalWorkerPool.RegionalTask() {
            @Override public void run() { execute(); }
            @Override public boolean controlPending() { return engine.controlWorkPending(); }
        });
    }

    /** Register readiness before arming so an input/helper race schedules exactly one continuation. */
    private final class RoundWake {
        private boolean armed, requested, scheduled;
        void request(boolean arm) {
            boolean start;
            synchronized (ThermalDimensionMailbox.this) {
                if (state == State.CLOSED || state == State.CLOSE_REQUESTED) return;
                if (arm) armed = true; else requested = true;
                start = armed && requested && !scheduled;
                if (start) {
                    scheduled = true;
                    if (waitingRound == this) waitingRound = null;
                }
            }
            if (start) schedule();
        }
    }

    private void execute() {
        boolean closing;
        synchronized (this) {
            closing = state == State.CLOSE_REQUESTED;
            if (!closing) {
                if (state != State.QUEUED) throw new IllegalStateException("thermal coordinator lost its queued turn");
                state = State.RUNNING;
            }
        }
        if (closing) { closeEngine(); return; }

        ThermalCompletion effects = null;
        try {
            engine.processWorkRound();
            synchronized (this) {
                if (!terminalPending && completions.size() < ThermalCompletion.MAXIMUM_PENDING)
                    effects = engine.pollWorkCompletion();
            }
        } catch (Throwable problem) {
            engine.beginFailureRecovery();
            RuntimeException failure = problem instanceof RuntimeException runtime ? runtime
                    : new IllegalStateException("Regional runtime failed", problem);
            effects = new ThermalCompletion(inputGeneration, Math.max(1, lastSubmittedSequence),
                    ThermalCompletion.Status.ENGINE_FAILED, failure, ThermalCompletion.NO_PHASE_REQUESTS,
                    ThermalCompletion.NO_RESYNC_TOKENS, ThermalCompletion.NO_RESIDENCY_UPDATES);
        }

        boolean again = false;
        RoundWake wake = null;
        synchronized (this) {
            closing = state == State.CLOSE_REQUESTED;
            if (!closing) {
                if (effects != null) {
                    completions.addLast(effects);
                    terminalPending = effects.status() == ThermalCompletion.Status.ENGINE_FAILED;
                }
                if (terminalPending && !engine.checkpointRecovery()) state = State.AWAITING_ACK;
                else if (engine.hasReadyWork()) { again = true; state = State.QUEUED; }
                else if (engine.hasWork()) { wake = waitingRound = new RoundWake(); state = State.QUEUED; }
                else state = completions.isEmpty() ? State.IDLE : State.AWAITING_ACK;
                progressRevision++;
                notifyAll();
            }
        }
        if (closing) closeEngine();
        else if (again) schedule();
        else if (wake != null) {
            RoundWake ready = wake;
            engine.whenRoundReady(() -> ready.request(false));
            ready.request(true);
        }
    }

    @Override public void close() {
        boolean schedule = false;
        synchronized (this) {
            switch (state) {
                case IDLE -> { state = State.CLOSE_REQUESTED; schedule = true; }
                case AWAITING_ACK -> {
                    completions.clear();
                    state = State.CLOSE_REQUESTED;
                    schedule = !engineClosed;
                    if (!schedule) { state = State.CLOSED; workers.unregisterLifecycleOwner(this); }
                }
                case QUEUED -> {
                    state = State.CLOSE_REQUESTED;
                    schedule = waitingRound != null;
                    waitingRound = null;
                }
                case RUNNING -> state = State.CLOSE_REQUESTED;
                case CLOSE_REQUESTED, CLOSED -> { return; }
            }
        }
        if (schedule) workers.executeLifecycle(this::closeEngine);
    }

    @Override public void requestWorkerPoolClose() { close(); }

    private void closeEngine() {
        synchronized (this) {
            if (engineClosed || closingEngineRound) return;
            closingEngineRound = true;
        }
        final boolean complete;
        try { complete = engine.closeRound(); }
        catch (RuntimeException | Error failure) {
            synchronized (this) { closingEngineRound = false; }
            com.teammoeg.frostedheart.FHMain.LOGGER.error("Thermal close failed after retaining outstanding ownership", failure);
            throw failure;
        }
        if (!complete) {
            synchronized (this) { closingEngineRound = false; }
            engine.whenRoundReady(() -> workers.executeLifecycle(this::closeEngine));
            return;
        }
        synchronized (this) {
            engineClosed = true;
            closingEngineRound = false;
            completions.clear(); waitingRound = null;
            state = State.CLOSED;
            notifyAll();
        }
        workers.unregisterLifecycleOwner(this);
    }
}
