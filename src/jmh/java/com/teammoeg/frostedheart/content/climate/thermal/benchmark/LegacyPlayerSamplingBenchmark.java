/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.benchmark;

import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulatorBenchmarkFixture;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class LegacyPlayerSamplingBenchmark {
    private static final long FIXED_SEED = 0x4f4c445f54454d50L;

    @State(Scope.Thread)
    public static class SnapshotState {
        @Param({"all_air", "room"})
        public String fixture;

        private SurroundingTemperatureSimulatorBenchmarkFixture.SourceFixture source;

        @Setup(Level.Trial)
        public void setup() {
            source = SurroundingTemperatureSimulatorBenchmarkFixture.source(fixture);
        }
    }

    @State(Scope.Thread)
    public static class WorkerState {
        @Param({"all_air", "room"})
        public String fixture;

        @Param({"hot", "cold"})
        public String cacheMode;

        private SurroundingTemperatureSimulatorBenchmarkFixture.CapturedFixture captured;
        private SurroundingTemperatureSimulator simulator;

        @Setup(Level.Trial)
        public void setup() {
            captured = SurroundingTemperatureSimulatorBenchmarkFixture.source(fixture).capture();
            if ("hot".equals(cacheMode)) {
                SurroundingTemperatureSimulatorBenchmarkFixture.simulate(
                        captured.simulator(FIXED_SEED));
            }
        }

        @Setup(Level.Invocation)
        public void resetSimulator() {
            if ("cold".equals(cacheMode)) {
                SurroundingTemperatureSimulatorBenchmarkFixture.clearBlockInfoCache();
            }
            simulator = captured.simulator(FIXED_SEED);
        }

        @TearDown(Level.Trial)
        public void cleanup() {
            SurroundingTemperatureSimulatorBenchmarkFixture.cleanupThreadLocal();
        }
    }

    @Benchmark
    public SurroundingTemperatureSimulatorBenchmarkFixture.CapturedFixture snapshotCopy(
            SnapshotState state
    ) {
        return state.source.capture();
    }

    @Benchmark
    public float simulateWorker(WorkerState state) {
        return SurroundingTemperatureSimulatorBenchmarkFixture.simulate(state.simulator);
    }
}
