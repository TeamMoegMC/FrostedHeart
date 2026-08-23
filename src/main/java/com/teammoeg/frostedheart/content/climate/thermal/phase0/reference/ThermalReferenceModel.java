/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.phase0.reference;

import java.util.List;
import java.util.Objects;

/** Closed-form numerical references used to validate later solver implementations. */
public final class ThermalReferenceModel {
    private ThermalReferenceModel() {
    }

    public record PowerSegment(double durationSeconds, double powerWatts) {
        public PowerSegment {
            ThermalUnits.requireNonNegative("durationSeconds", durationSeconds);
            ThermalUnits.requireFinite("powerWatts", powerWatts);
        }

        public double energyJ() {
            return ThermalUnits.requireFiniteResult(
                    "segment energy",
                    durationSeconds * powerWatts
            );
        }
    }

    public record SourceIntegration(double enthalpyJ, double integratedEnergyJ) {
    }

    /** Positive {@code transferredToAJ} means energy moved from B to A. */
    public record PairExchange(double enthalpyAJ, double enthalpyBJ, double transferredToAJ) {
        public double totalEnthalpyJ() {
            return enthalpyAJ + enthalpyBJ;
        }
    }

    /** Positive {@code energyFromBoundaryJ} means the fixed boundary heated the cell. */
    public record BoundaryExchange(double enthalpyJ, double energyFromBoundaryJ) {
    }

    public static SourceIntegration integrateConstantPower(
            double initialEnthalpyJ,
            double powerWatts,
            double durationSeconds
    ) {
        return integratePiecewisePower(
                initialEnthalpyJ,
                List.of(new PowerSegment(durationSeconds, powerWatts))
        );
    }

    /** Integrates an ordered, piecewise-constant source timeline without cadence sampling. */
    public static SourceIntegration integratePiecewisePower(
            double initialEnthalpyJ,
            List<PowerSegment> segments
    ) {
        ThermalUnits.requireFinite("initialEnthalpyJ", initialEnthalpyJ);
        Objects.requireNonNull(segments, "segments");

        double integral = 0.0;
        double compensation = 0.0;
        for (PowerSegment segment : segments) {
            Objects.requireNonNull(segment, "segments contains null");
            double adjusted = segment.energyJ() - compensation;
            double next = ThermalUnits.requireFiniteResult(
                    "integrated source energy",
                    integral + adjusted
            );
            compensation = (next - integral) - adjusted;
            integral = next;
        }
        return new SourceIntegration(
                ThermalUnits.requireFiniteResult("source enthalpy", initialEnthalpyJ + integral),
                integral
        );
    }

    /**
     * Exact solution for one isolated finite-capacity pair over {@code dtSeconds}.
     * Both enthalpies must use the same reference temperature.
     */
    public static PairExchange exchangePair(
            double enthalpyAJ,
            double capacityAJPerK,
            double enthalpyBJ,
            double capacityBJPerK,
            double conductanceWPerK,
            double dtSeconds
    ) {
        ThermalUnits.requireFinite("enthalpyAJ", enthalpyAJ);
        ThermalUnits.requireFinite("enthalpyBJ", enthalpyBJ);
        ThermalUnits.requirePositive("capacityAJPerK", capacityAJPerK);
        ThermalUnits.requirePositive("capacityBJPerK", capacityBJPerK);
        ThermalUnits.requireNonNegative("conductanceWPerK", conductanceWPerK);
        ThermalUnits.requireNonNegative("dtSeconds", dtSeconds);

        double temperatureOffsetA = enthalpyAJ / capacityAJPerK;
        double temperatureOffsetB = enthalpyBJ / capacityBJPerK;
        double reducedCapacity = reducedCapacity(capacityAJPerK, capacityBJPerK);
        double approach = approachFraction(
                conductanceWPerK * dtSeconds
                        * (1.0 / capacityAJPerK + 1.0 / capacityBJPerK)
        );
        double transferredToA = ThermalUnits.requireFiniteResult(
                "pair transfer",
                (temperatureOffsetB - temperatureOffsetA) * reducedCapacity * approach
        );
        return new PairExchange(
                ThermalUnits.requireFiniteResult("pair enthalpy A", enthalpyAJ + transferredToA),
                ThermalUnits.requireFiniteResult("pair enthalpy B", enthalpyBJ - transferredToA),
                transferredToA
        );
    }

    /** Exact finite-capacity exchange against an infinite fixed-temperature boundary. */
    public static BoundaryExchange exchangeFixedBoundary(
            double enthalpyJ,
            double capacityJPerK,
            double referenceTemperatureC,
            double boundaryTemperatureC,
            double conductanceWPerK,
            double dtSeconds
    ) {
        ThermalUnits.requireFinite("enthalpyJ", enthalpyJ);
        ThermalUnits.requirePositive("capacityJPerK", capacityJPerK);
        ThermalUnits.requireFinite("referenceTemperatureC", referenceTemperatureC);
        ThermalUnits.requireFinite("boundaryTemperatureC", boundaryTemperatureC);
        ThermalUnits.requireNonNegative("conductanceWPerK", conductanceWPerK);
        ThermalUnits.requireNonNegative("dtSeconds", dtSeconds);

        double temperatureC = ThermalUnits.temperatureFromEnthalpy(
                enthalpyJ,
                referenceTemperatureC,
                capacityJPerK
        );
        double approach = approachFraction(conductanceWPerK * dtSeconds / capacityJPerK);
        double energyFromBoundaryJ = ThermalUnits.requireFiniteResult(
                "fixed-boundary transfer",
                capacityJPerK * (boundaryTemperatureC - temperatureC) * approach
        );
        return new BoundaryExchange(
                ThermalUnits.requireFiniteResult(
                        "fixed-boundary enthalpy",
                        enthalpyJ + energyFromBoundaryJ
                ),
                energyFromBoundaryJ
        );
    }

    private static double approachFraction(double exponent) {
        if (Double.isNaN(exponent) || exponent < 0.0) {
            throw new ArithmeticException("exchange exponent must be non-negative");
        }
        if (exponent == Double.POSITIVE_INFINITY) {
            return 1.0;
        }
        return -Math.expm1(-exponent);
    }

    private static double reducedCapacity(double capacityA, double capacityB) {
        if (capacityA <= capacityB) {
            return capacityA / (1.0 + capacityA / capacityB);
        }
        return capacityB / (1.0 + capacityB / capacityA);
    }
}
