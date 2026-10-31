/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.gametest.framework.GameTestHelper;

/** Drives the real regional coordinator; only the test waits for all its work. */
public final class RegionalEngineTestDriver {
    private RegionalEngineTestDriver() { }

    public static ThermalCompletion submit(GameTestHelper helper, ThermalDimensionEngine engine, ThermalInputBatch input) {
        engine.offerInput(new ThermalInputEnvelope(input, new SpatialSourceTimeline.Window[0]));
        var phases = new ArrayList<PhaseTransitionRuntime.Request>();
        var resyncs = new ArrayList<ThermalPageHandle.GeometryResyncToken>();
        var residency = new ArrayList<ThermalCompletion.BrickResidency>();
        boolean limited = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        do {
            if (engine.hasReadyWork()) engine.processWorkRound();
            ThermalCompletion completion;
            while ((completion = engine.pollWorkCompletion()) != null) {
                helper.assertTrue(completion.failure() == null, "regional effects failed: " + completion.failure());
                limited |= completion.status() == ThermalCompletion.Status.WORK_LIMITED;
                Collections.addAll(phases, completion.phaseRequests());
                Collections.addAll(resyncs, completion.committedResyncTokens());
                Collections.addAll(residency, completion.residencyUpdates());
            }
            if (!engine.hasWork()) break;
            helper.assertTrue(System.nanoTime() < deadline, "regional fixture did not finish: " + engine.regionalProgressDescription());
            if (!engine.hasReadyWork()) {
                var ready = new CountDownLatch(1);
                engine.whenRoundReady(ready::countDown);
                try { ready.await(100, TimeUnit.MILLISECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            }
        } while (true);
        return new ThermalCompletion(input.dimensionGeneration(), input.sequence(),
                limited ? ThermalCompletion.Status.WORK_LIMITED : ThermalCompletion.Status.COMPLETED, null,
                phases.toArray(ThermalCompletion.NO_PHASE_REQUESTS), resyncs.toArray(ThermalCompletion.NO_RESYNC_TOKENS),
                residency.toArray(ThermalCompletion.NO_RESIDENCY_UPDATES));
    }
}
