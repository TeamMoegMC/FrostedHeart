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

import com.teammoeg.frostedheart.content.climate.thermal.phase0.reference.ThermalReferenceModel;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ThermalReferenceCalibrationBenchmark {
    private double enthalpyA;
    private double capacityA;
    private double enthalpyB;
    private double capacityB;
    private double conductance;
    private double duration;
    private double referenceTemperature;
    private double boundaryTemperature;

    @Setup
    public void setup() {
        enthalpyA = 8_000.0D;
        capacityA = 1_200.0D;
        enthalpyB = -3_000.0D;
        capacityB = 900.0D;
        conductance = 75.0D;
        duration = 0.05D;
        referenceTemperature = 20.0D;
        boundaryTemperature = -10.0D;
    }

    @Benchmark
    public double exchangePair() {
        ThermalReferenceModel.PairExchange result = ThermalReferenceModel.exchangePair(
                enthalpyA, capacityA, enthalpyB, capacityB, conductance, duration);
        return result.enthalpyAJ() + result.enthalpyBJ() + result.transferredToAJ();
    }

    @Benchmark
    public double exchangeFixedBoundary() {
        ThermalReferenceModel.BoundaryExchange result = ThermalReferenceModel.exchangeFixedBoundary(
                enthalpyA, capacityA, referenceTemperature, boundaryTemperature,
                conductance, duration);
        return result.enthalpyJ() + result.energyFromBoundaryJ();
    }
}
