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

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ComponentBrickCompilerBenchmark {
    @Param({"all_air", "solid_wall", "split_regions"})
    public String pattern;

    private List<ConservativeAirGeometry.Resolution> blockGeometry;
    private int generation;

    @Setup
    public void setup() {
        ConservativeAirGeometry.Resolution air =
                ConservativeAirGeometry.resolve(List.of(), 4);
        blockGeometry = new ArrayList<>(Collections.nCopies(
                ComponentBrickCompiler.BLOCK_COUNT, air));
        if ("solid_wall".equals(pattern)) {
            ConservativeAirGeometry.Resolution solid = ConservativeAirGeometry.resolve(
                    List.of(ConservativeAirGeometry.UnitBox.fullBlock()), 4);
            for (int y = 0; y < ComponentBrickCompiler.BLOCKS_PER_AXIS; y++) {
                for (int z = 0; z < ComponentBrickCompiler.BLOCKS_PER_AXIS; z++) {
                    blockGeometry.set(ComponentBrickCompiler.blockIndex(1, y, z), solid);
                }
            }
        } else if ("split_regions".equals(pattern)) {
            ConservativeAirGeometry.Resolution split = ConservativeAirGeometry.resolve(
                    List.of(new ConservativeAirGeometry.UnitBox(
                            0.49D, 0.0D, 0.0D, 0.51D, 1.0D, 1.0D)),
                    4);
            Collections.fill(blockGeometry, split);
        } else if (!"all_air".equals(pattern)) {
            throw new IllegalArgumentException("unknown pattern " + pattern);
        }
        generation = 1;
    }

    @Benchmark
    public ComponentBrickCompiler.Compilation compileBrick() {
        return ComponentBrickCompiler.compile(blockGeometry, 4, generation);
    }
}
