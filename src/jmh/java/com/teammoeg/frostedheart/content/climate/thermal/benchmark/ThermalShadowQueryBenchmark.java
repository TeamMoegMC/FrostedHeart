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

/** Measures one sequential main-thread-style batch; scores are per batch. */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ThermalShadowQueryBenchmark {
    @State(Scope.Thread)
    public static class QueryState {
        @Param({"1", "10", "50", "100"})
        public int receiverCount;

        @Param({"shared_page", "distributed_pages"})
        public String layout;

        private ThermalShadowQueryFixtures.Fixture fixture;

        @Setup(Level.Trial)
        public void setup() {
            fixture = ThermalShadowQueryFixtures.create(receiverCount, layout);
            fixture.queryBatch();
        }

        @TearDown(Level.Trial)
        public void cleanup() {
            fixture.close();
        }
    }

    @Benchmark
    public double queryPublishedAirBatch(QueryState state) {
        return state.fixture.queryBatch();
    }
}
