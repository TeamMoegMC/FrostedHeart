/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** One regional slice with once-only execution, cancellation and completion. */
final class ThermalAssemblyTask implements ThermalWorkerPool.RegionalTask {
    private final AtomicBoolean started = new AtomicBoolean();
    final CompletableFuture<Void> finished = new CompletableFuture<>();
    private java.util.function.LongConsumer work;
    private com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.Context workContext =
            com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.capture();

    ThermalAssemblyTask(java.util.function.LongConsumer work) { this.work = work; }
    /** Abandon queued private assembly on teardown; an executing reader must settle. */
    boolean discardBeforeStart() {
        if (!started.compareAndSet(false, true)) return false;
        work = null;
        workContext = null;
        finished.complete(null);
        return true;
    }

    @Override
    public void run() {
        run(System.nanoTime() + 2_000_000L);
    }

    /** Inline work shares its coordinator's remaining slice; queued work starts its clock on execution. */
    void run(long deadline) {
        if (!started.compareAndSet(false, true)) return;
        Throwable failure = null;
        try (var scope = com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.attach(workContext)) {
            if (System.nanoTime() < deadline) work.accept(deadline);
        } catch (Throwable problem) { failure = problem; }
        finally { work = null; workContext = null; }
        // Queued inline-claimed tasks retain neither inputs nor completed matrix results.
        if (failure == null) finished.complete(null);
        else finished.completeExceptionally(failure);
    }
}
