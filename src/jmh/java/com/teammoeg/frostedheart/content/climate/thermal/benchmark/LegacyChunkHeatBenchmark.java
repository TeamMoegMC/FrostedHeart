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

import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import net.minecraft.core.BlockPos;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LegacyChunkHeatBenchmark {
    @State(Scope.Benchmark)
    public static class EmptyState {
        private ChunkHeatData data;
        private BlockPos position;

        @Setup
        public void setup() {
            data = LegacyChunkHeatFixtures.data(0);
            position = LegacyChunkHeatFixtures.missPosition();
        }
    }

    @State(Scope.Benchmark)
    public static class PopulatedState {
        @Param({"1", "10", "100"})
        public int adjusterCount;

        private ChunkHeatData data;
        private BlockPos hitPosition;
        private BlockPos missPosition;

        @Setup
        public void setup() {
            data = LegacyChunkHeatFixtures.data(adjusterCount);
            hitPosition = LegacyChunkHeatFixtures.hitPosition();
            missPosition = LegacyChunkHeatFixtures.missPosition();
        }
    }

    @Benchmark
    public float queryEmpty(EmptyState state) {
        return consume(ChunkHeatData.queryAdjust(state.data, state.position));
    }

    @Benchmark
    public float queryHit(PopulatedState state) {
        return consume(ChunkHeatData.queryAdjust(state.data, state.hitPosition));
    }

    @Benchmark
    public float queryMiss(PopulatedState state) {
        return consume(ChunkHeatData.queryAdjust(state.data, state.missPosition));
    }

    private static float consume(ChunkHeatData.HeatQueryResult result) {
        return result.additionTemperature() + (result.hasActiveAdjust() ? 1.0F : 0.0F);
    }
}
