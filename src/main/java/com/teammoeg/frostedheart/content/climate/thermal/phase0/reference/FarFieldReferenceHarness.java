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

import com.teammoeg.frostedheart.content.climate.thermal.mesh.FarFieldProfileRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Independent finite-volume reference and fit/holdout gate for V1 static FarField impedance.
 */
public final class FarFieldReferenceHarness {
    private static final double AIR_CELL_CAPACITY_J_PER_K = 76_800.0D;
    private static final double GOLDEN_RATIO_COMPLEMENT = 0.3819660112501051D;
    private static final int FIT_ITERATIONS = 96;

    private FarFieldReferenceHarness() {
    }

    public enum Split {
        FIT,
        HOLDOUT
    }

    public record FitRange(
            double minimumConductanceWPerK,
            double maximumConductanceWPerK
    ) {
        public FitRange {
            requirePositiveFinite("minimumConductanceWPerK", minimumConductanceWPerK);
            requirePositiveFinite("maximumConductanceWPerK", maximumConductanceWPerK);
            if (maximumConductanceWPerK < minimumConductanceWPerK) {
                throw new IllegalArgumentException("FarField fit range must be ordered");
            }
        }
    }

    /** Approval limits must come from the workload contract, not from this harness. */
    public record Tolerances(
            double maximumTemperatureErrorC,
            double maximumThresholdCrossingErrorSeconds,
            double maximumAbsoluteBoundaryEnergyErrorJ,
            double maximumPhasePowerErrorW
    ) {
        public Tolerances {
            requireNonNegativeFinite("maximumTemperatureErrorC", maximumTemperatureErrorC);
            requireNonNegativeFinite(
                    "maximumThresholdCrossingErrorSeconds",
                    maximumThresholdCrossingErrorSeconds);
            requireNonNegativeFinite(
                    "maximumAbsoluteBoundaryEnergyErrorJ",
                    maximumAbsoluteBoundaryEnergyErrorJ);
            requireNonNegativeFinite("maximumPhasePowerErrorW", maximumPhasePowerErrorW);
        }
    }

    /** One aggregate radial/axial domain case; conductances include the Natural boundary. */
    public static final class Fixture {
        private final String id;
        private final FarFieldProfileRegistry.Key key;
        private final Split split;
        private final double localCapacityJPerK;
        private final double[] outerCapacitiesJPerK;
        private final double[] interfaceConductancesWPerK;
        private final double initialTemperatureC;
        private final double naturalTemperatureC;
        private final double sourcePowerW;
        private final double durationSeconds;
        private final double sampleIntervalSeconds;
        private final double gameplayThresholdC;
        private final double phaseBoundaryTemperatureC;
        private final double phaseCouplingWPerK;

        public Fixture(
                String id,
                FarFieldProfileRegistry.Key key,
                Split split,
                double localCapacityJPerK,
                double[] outerCapacitiesJPerK,
                double[] interfaceConductancesWPerK,
                double initialTemperatureC,
                double naturalTemperatureC,
                double sourcePowerW,
                double durationSeconds,
                double sampleIntervalSeconds,
                double gameplayThresholdC,
                double phaseBoundaryTemperatureC,
                double phaseCouplingWPerK
        ) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("fixture id is required");
            }
            this.id = id;
            this.key = Objects.requireNonNull(key, "key");
            this.split = Objects.requireNonNull(split, "split");
            requirePositiveFinite("localCapacityJPerK", localCapacityJPerK);
            this.localCapacityJPerK = localCapacityJPerK;
            this.outerCapacitiesJPerK = requirePositiveArray(
                    "outerCapacitiesJPerK", outerCapacitiesJPerK);
            this.interfaceConductancesWPerK = requirePositiveArray(
                    "interfaceConductancesWPerK", interfaceConductancesWPerK);
            if (this.outerCapacitiesJPerK.length == 0
                    || this.interfaceConductancesWPerK.length
                    != this.outerCapacitiesJPerK.length + 1) {
                throw new IllegalArgumentException(
                        "a fixture needs one or more outer cells and one conductance per interface");
            }
            requireFinite("initialTemperatureC", initialTemperatureC);
            requireFinite("naturalTemperatureC", naturalTemperatureC);
            requireFinite("sourcePowerW", sourcePowerW);
            requirePositiveFinite("durationSeconds", durationSeconds);
            requirePositiveFinite("sampleIntervalSeconds", sampleIntervalSeconds);
            double samples = durationSeconds / sampleIntervalSeconds;
            if (Math.abs(samples - Math.rint(samples)) > 1.0e-9D) {
                throw new IllegalArgumentException(
                        "durationSeconds must be an integer number of sample intervals");
            }
            requireFinite("gameplayThresholdC", gameplayThresholdC);
            requireFinite("phaseBoundaryTemperatureC", phaseBoundaryTemperatureC);
            requireNonNegativeFinite("phaseCouplingWPerK", phaseCouplingWPerK);
            this.initialTemperatureC = initialTemperatureC;
            this.naturalTemperatureC = naturalTemperatureC;
            this.sourcePowerW = sourcePowerW;
            this.durationSeconds = durationSeconds;
            this.sampleIntervalSeconds = sampleIntervalSeconds;
            this.gameplayThresholdC = gameplayThresholdC;
            this.phaseBoundaryTemperatureC = phaseBoundaryTemperatureC;
            this.phaseCouplingWPerK = phaseCouplingWPerK;
        }

        public String id() {
            return id;
        }

        public FarFieldProfileRegistry.Key key() {
            return key;
        }

        public Split split() {
            return split;
        }

        public double localCapacityJPerK() {
            return localCapacityJPerK;
        }

        public double initialTemperatureC() {
            return initialTemperatureC;
        }

        public double naturalTemperatureC() {
            return naturalTemperatureC;
        }

        public double sourcePowerW() {
            return sourcePowerW;
        }

        public double durationSeconds() {
            return durationSeconds;
        }

        public double sampleIntervalSeconds() {
            return sampleIntervalSeconds;
        }

        public double gameplayThresholdC() {
            return gameplayThresholdC;
        }

        public double phaseBoundaryTemperatureC() {
            return phaseBoundaryTemperatureC;
        }

        public double phaseCouplingWPerK() {
            return phaseCouplingWPerK;
        }

        private double outerCapacity(int index) {
            return outerCapacitiesJPerK[index];
        }

        private int outerCellCount() {
            return outerCapacitiesJPerK.length;
        }

        private double interfaceConductance(int index) {
            return interfaceConductancesWPerK[index];
        }
    }

    public record Sample(
            double timeSeconds,
            double localTemperatureC,
            double boundaryEnergyFromNaturalJ,
            double phaseReceivedPowerW
    ) {
    }

    public record Trace(List<Sample> samples, double thresholdCrossingTimeSeconds) {
        public Trace {
            samples = List.copyOf(samples);
            if (samples.isEmpty()) {
                throw new IllegalArgumentException("a FarField trace requires samples");
            }
            if (thresholdCrossingTimeSeconds < -1.0D
                    || !Double.isFinite(thresholdCrossingTimeSeconds)) {
                throw new IllegalArgumentException(
                        "threshold crossing must be -1 or finite non-negative seconds");
            }
        }

        public boolean crossedThreshold() {
            return thresholdCrossingTimeSeconds >= 0.0D;
        }
    }

    public record CaseResult(
            String fixtureId,
            double maximumTemperatureErrorC,
            double thresholdCrossingErrorSeconds,
            boolean thresholdCrossingMismatch,
            double signedBoundaryEnergyErrorJ,
            double maximumPhasePowerErrorW
    ) {
    }

    public record HoldoutMetrics(
            int caseCount,
            double maximumTemperatureErrorC,
            double maximumThresholdCrossingErrorSeconds,
            boolean thresholdCrossingMismatchObserved,
            double minimumBoundaryEnergyErrorJ,
            double maximumBoundaryEnergyErrorJ,
            double maximumPhasePowerErrorW
    ) {
        public double maximumAbsoluteBoundaryEnergyErrorJ() {
            return Math.max(
                    Math.abs(minimumBoundaryEnergyErrorJ),
                    Math.abs(maximumBoundaryEnergyErrorJ));
        }
    }

    public record GateResult(
            FarFieldProfileRegistry.Profile profile,
            HoldoutMetrics holdoutMetrics,
            List<CaseResult> holdoutCases
    ) {
        public GateResult {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(holdoutMetrics, "holdoutMetrics");
            holdoutCases = List.copyOf(holdoutCases);
        }

        public boolean approved() {
            return profile.approval()
                    == FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE;
        }
    }

    public static GateResult calibrate(
            List<Fixture> fixtures,
            FitRange fitRange,
            Tolerances tolerances
    ) {
        Objects.requireNonNull(fixtures, "fixtures");
        Objects.requireNonNull(fitRange, "fitRange");
        Objects.requireNonNull(tolerances, "tolerances");
        if (fixtures.isEmpty()) {
            throw new IllegalArgumentException("FarField calibration fixtures are required");
        }

        FarFieldProfileRegistry.Key key = null;
        Set<String> ids = new HashSet<>();
        List<ReferenceCase> fitCases = new ArrayList<>();
        List<ReferenceCase> holdoutCases = new ArrayList<>();
        for (Fixture fixture : fixtures) {
            Objects.requireNonNull(fixture, "fixtures contains null");
            if (!ids.add(fixture.id())) {
                throw new IllegalArgumentException("duplicate FarField fixture id: " + fixture.id());
            }
            if (key == null) {
                key = fixture.key();
            } else if (!key.equals(fixture.key())) {
                throw new IllegalArgumentException(
                        "one calibration run must contain exactly one profile key");
            }
            ReferenceCase reference = new ReferenceCase(fixture, simulateExplicit(fixture));
            if (fixture.split() == Split.FIT) {
                fitCases.add(reference);
            } else {
                holdoutCases.add(reference);
            }
        }
        if (fitCases.isEmpty() || holdoutCases.isEmpty()) {
            throw new IllegalArgumentException(
                    "FarField calibration requires independent fit and holdout fixtures");
        }

        double conductance = fitConductance(fitCases, fitRange);
        List<CaseResult> caseResults = new ArrayList<>(holdoutCases.size());
        double maximumTemperatureError = 0.0D;
        double maximumCrossingError = 0.0D;
        boolean crossingMismatch = false;
        double minimumBoundaryEnergyError = Double.POSITIVE_INFINITY;
        double maximumBoundaryEnergyError = Double.NEGATIVE_INFINITY;
        double maximumPhasePowerError = 0.0D;
        for (ReferenceCase holdout : holdoutCases) {
            CaseResult result = compare(
                    holdout.fixture(),
                    holdout.trace(),
                    simulateStaticImpedance(holdout.fixture(), conductance));
            caseResults.add(result);
            maximumTemperatureError = Math.max(
                    maximumTemperatureError, result.maximumTemperatureErrorC());
            maximumCrossingError = Math.max(
                    maximumCrossingError, result.thresholdCrossingErrorSeconds());
            crossingMismatch |= result.thresholdCrossingMismatch();
            minimumBoundaryEnergyError = Math.min(
                    minimumBoundaryEnergyError, result.signedBoundaryEnergyErrorJ());
            maximumBoundaryEnergyError = Math.max(
                    maximumBoundaryEnergyError, result.signedBoundaryEnergyErrorJ());
            maximumPhasePowerError = Math.max(
                    maximumPhasePowerError, result.maximumPhasePowerErrorW());
        }

        HoldoutMetrics metrics = new HoldoutMetrics(
                caseResults.size(),
                maximumTemperatureError,
                maximumCrossingError,
                crossingMismatch,
                minimumBoundaryEnergyError,
                maximumBoundaryEnergyError,
                maximumPhasePowerError);
        boolean approved = !crossingMismatch
                && maximumTemperatureError <= tolerances.maximumTemperatureErrorC()
                && maximumCrossingError <= tolerances.maximumThresholdCrossingErrorSeconds()
                && metrics.maximumAbsoluteBoundaryEnergyErrorJ()
                <= tolerances.maximumAbsoluteBoundaryEnergyErrorJ()
                && maximumPhasePowerError <= tolerances.maximumPhasePowerErrorW();

        double maximumSourcePower = 0.0D;
        double maximumTemperatureDelta = 0.0D;
        for (ReferenceCase reference : concat(fitCases, holdoutCases)) {
            maximumSourcePower = Math.max(
                    maximumSourcePower, Math.abs(reference.fixture().sourcePowerW()));
            for (Sample sample : reference.trace().samples()) {
                maximumTemperatureDelta = Math.max(
                        maximumTemperatureDelta,
                        Math.abs(sample.localTemperatureC()
                                - reference.fixture().naturalTemperatureC()));
            }
        }

        FarFieldProfileRegistry.Profile profile = new FarFieldProfileRegistry.Profile(
                key,
                conductance,
                new FarFieldProfileRegistry.ApplicabilityDomain(
                        maximumSourcePower, maximumTemperatureDelta),
                new FarFieldProfileRegistry.ErrorEnvelope(
                        maximumTemperatureError,
                        maximumCrossingError,
                        crossingMismatch,
                        minimumBoundaryEnergyError,
                        maximumBoundaryEnergyError,
                        maximumPhasePowerError),
                approved
                        ? FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE
                        : FarFieldProfileRegistry.Approval.CANDIDATE);
        return new GateResult(profile, metrics, caseResults);
    }

    /** Synthetic coverage matrix for the required PR6 topology, wind, and power cases. */
    public static List<Fixture> standardFixtures() {
        List<Fixture> fixtures = new ArrayList<>();
        addPowerMatrix(fixtures, FarFieldProfileRegistry.TopologyClass.OPEN_SPACE,
                FarFieldProfileRegistry.WindBucket.CALM);
        addPowerMatrix(fixtures, FarFieldProfileRegistry.TopologyClass.OPEN_SPACE,
                FarFieldProfileRegistry.WindBucket.WINDY);
        addPowerMatrix(fixtures, FarFieldProfileRegistry.TopologyClass.HALF_OPEN_SPACE,
                FarFieldProfileRegistry.WindBucket.CALM);
        addPowerMatrix(fixtures, FarFieldProfileRegistry.TopologyClass.HALF_OPEN_SPACE,
                FarFieldProfileRegistry.WindBucket.WINDY);
        addPowerMatrix(fixtures, FarFieldProfileRegistry.TopologyClass.CAVERN,
                FarFieldProfileRegistry.WindBucket.CALM);
        addPowerMatrix(fixtures, FarFieldProfileRegistry.TopologyClass.TUNNEL_EXIT,
                FarFieldProfileRegistry.WindBucket.CALM);
        return List.copyOf(fixtures);
    }

    private static void addPowerMatrix(
            List<Fixture> fixtures,
            FarFieldProfileRegistry.TopologyClass topology,
            FarFieldProfileRegistry.WindBucket wind
    ) {
        fixtures.add(standardFixture(topology, wind, 1_000.0D, Split.FIT));
        fixtures.add(standardFixture(topology, wind, 10_000.0D, Split.HOLDOUT));
        fixtures.add(standardFixture(topology, wind, 100_000.0D, Split.FIT));
    }

    private static Fixture standardFixture(
            FarFieldProfileRegistry.TopologyClass topology,
            FarFieldProfileRegistry.WindBucket wind,
            double sourcePowerW,
            Split split
    ) {
        double windMultiplier = wind == FarFieldProfileRegistry.WindBucket.WINDY
                ? 1.8D : 1.0D;
        double[] capacities;
        double[] conductances;
        double duration;
        double sampleInterval;
        switch (topology) {
            case OPEN_SPACE -> {
                capacities = new double[]{19_200.0D, 38_400.0D};
                conductances = scaled(
                        new double[]{12_000.0D, 30_000.0D, 80_000.0D}, windMultiplier);
                duration = 300.0D;
                sampleInterval = 5.0D;
            }
            case HALF_OPEN_SPACE -> {
                capacities = new double[]{76_800.0D, 153_600.0D};
                conductances = scaled(
                        new double[]{3_500.0D, 2_500.0D, 6_000.0D}, windMultiplier);
                duration = 1_800.0D;
                sampleInterval = 15.0D;
            }
            case CAVERN -> {
                capacities = new double[]{768_000.0D, 1_536_000.0D, 1_536_000.0D};
                conductances = scaled(
                        new double[]{1_500.0D, 500.0D, 250.0D, 100.0D}, windMultiplier);
                duration = 14_400.0D;
                sampleInterval = 60.0D;
            }
            case TUNNEL_EXIT -> {
                capacities = new double[]{153_600.0D, 153_600.0D, 153_600.0D, 153_600.0D};
                conductances = scaled(
                        new double[]{2_500.0D, 1_200.0D, 600.0D, 300.0D, 120.0D},
                        windMultiplier);
                duration = 7_200.0D;
                sampleInterval = 30.0D;
            }
            default -> throw new IllegalStateException("unhandled topology: " + topology);
        }

        double naturalTemperature = -20.0D;
        double equivalentConductance = seriesConductance(conductances);
        double steadyDelta = sourcePowerW / equivalentConductance;
        FarFieldProfileRegistry.OpeningClass openingClass = switch (topology) {
            case OPEN_SPACE -> FarFieldProfileRegistry.OpeningClass.MULTI_FACE;
            case HALF_OPEN_SPACE -> FarFieldProfileRegistry.OpeningClass.HALF_OPEN;
            case CAVERN, TUNNEL_EXIT -> FarFieldProfileRegistry.OpeningClass.FULL_FACE;
        };
        FarFieldProfileRegistry.Key key = new FarFieldProfileRegistry.Key(
                0,
                openingClass,
                2,
                FarFieldProfileRegistry.Orientation.HORIZONTAL,
                wind,
                FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
                topology);
        return new Fixture(
                topology.name().toLowerCase() + "-" + wind.name().toLowerCase()
                        + "-" + Math.round(sourcePowerW),
                key,
                split,
                AIR_CELL_CAPACITY_J_PER_K,
                capacities,
                conductances,
                naturalTemperature,
                naturalTemperature,
                sourcePowerW,
                duration,
                sampleInterval,
                naturalTemperature + steadyDelta * 0.5D,
                naturalTemperature + steadyDelta * 0.35D,
                25.0D);
    }

    private static Trace simulateExplicit(Fixture fixture) {
        int cellCount = fixture.outerCellCount() + 1;
        double[] temperatures = new double[cellCount];
        java.util.Arrays.fill(temperatures, fixture.initialTemperatureC());
        double[] capacities = new double[cellCount];
        capacities[0] = fixture.localCapacityJPerK();
        for (int cell = 1; cell < cellCount; cell++) {
            capacities[cell] = fixture.outerCapacity(cell - 1);
        }

        double maximumRate = 0.0D;
        for (int cell = 0; cell < cellCount; cell++) {
            double conductanceSum = 0.0D;
            if (cell > 0) {
                conductanceSum += fixture.interfaceConductance(cell - 1);
            }
            if (cell < cellCount - 1) {
                conductanceSum += fixture.interfaceConductance(cell);
            } else {
                conductanceSum += fixture.interfaceConductance(cellCount - 1);
            }
            maximumRate = Math.max(maximumRate, conductanceSum / capacities[cell]);
        }
        double targetStep = Math.min(
                fixture.sampleIntervalSeconds() / 8.0D,
                0.1D / maximumRate);
        int substepsPerSample = Math.max(
                1, (int) Math.ceil(fixture.sampleIntervalSeconds() / targetStep));
        double stepSeconds = fixture.sampleIntervalSeconds() / substepsPerSample;

        RkScratch scratch = new RkScratch(cellCount);
        List<Sample> samples = new ArrayList<>();
        double elapsed = 0.0D;
        double boundaryEnergy = 0.0D;
        double crossingTime = alreadyCrossed(
                fixture.initialTemperatureC(), fixture.gameplayThresholdC(),
                fixture.sourcePowerW()) ? 0.0D : -1.0D;
        int sampleCount = (int) Math.rint(
                fixture.durationSeconds() / fixture.sampleIntervalSeconds());
        for (int sample = 0; sample < sampleCount; sample++) {
            for (int substep = 0; substep < substepsPerSample; substep++) {
                double before = temperatures[0];
                double energyIncrement = rk4Step(
                        temperatures, capacities, fixture, stepSeconds, scratch);
                if (crossingTime < 0.0D
                        && crosses(before, temperatures[0], fixture.gameplayThresholdC(),
                        fixture.sourcePowerW())) {
                    double fraction = crossingFraction(
                            before, temperatures[0], fixture.gameplayThresholdC());
                    crossingTime = elapsed + stepSeconds * fraction;
                }
                elapsed += stepSeconds;
                boundaryEnergy += energyIncrement;
            }
            samples.add(new Sample(
                    elapsed,
                    temperatures[0],
                    boundaryEnergy,
                    phasePower(fixture, temperatures[0])));
        }
        return new Trace(samples, crossingTime);
    }

    private static Trace simulateStaticImpedance(Fixture fixture, double conductanceWPerK) {
        List<Sample> samples = new ArrayList<>();
        double temperature = fixture.initialTemperatureC();
        double boundaryEnergy = 0.0D;
        double elapsed = 0.0D;
        double crossingTime = alreadyCrossed(
                temperature, fixture.gameplayThresholdC(), fixture.sourcePowerW())
                ? 0.0D : -1.0D;
        int sampleCount = (int) Math.rint(
                fixture.durationSeconds() / fixture.sampleIntervalSeconds());
        for (int sample = 0; sample < sampleCount; sample++) {
            double before = temperature;
            double equilibrium = fixture.naturalTemperatureC()
                    + fixture.sourcePowerW() / conductanceWPerK;
            double approach = -Math.expm1(
                    -conductanceWPerK * fixture.sampleIntervalSeconds()
                            / fixture.localCapacityJPerK());
            temperature = before + (equilibrium - before) * approach;
            if (crossingTime < 0.0D
                    && crosses(before, temperature, fixture.gameplayThresholdC(),
                    fixture.sourcePowerW())) {
                crossingTime = elapsed + exactCrossingOffset(
                        before,
                        equilibrium,
                        fixture.gameplayThresholdC(),
                        conductanceWPerK / fixture.localCapacityJPerK(),
                        fixture.sampleIntervalSeconds());
            }
            boundaryEnergy += fixture.localCapacityJPerK() * (temperature - before)
                    - fixture.sourcePowerW() * fixture.sampleIntervalSeconds();
            elapsed += fixture.sampleIntervalSeconds();
            samples.add(new Sample(
                    elapsed,
                    temperature,
                    boundaryEnergy,
                    phasePower(fixture, temperature)));
        }
        return new Trace(samples, crossingTime);
    }

    private static double fitConductance(
            List<ReferenceCase> fitCases,
            FitRange fitRange
    ) {
        if (fitRange.minimumConductanceWPerK() == fitRange.maximumConductanceWPerK()) {
            return fitRange.minimumConductanceWPerK();
        }
        double low = Math.log(fitRange.minimumConductanceWPerK());
        double high = Math.log(fitRange.maximumConductanceWPerK());
        double left = low + GOLDEN_RATIO_COMPLEMENT * (high - low);
        double right = high - GOLDEN_RATIO_COMPLEMENT * (high - low);
        double leftLoss = fitLoss(Math.exp(left), fitCases);
        double rightLoss = fitLoss(Math.exp(right), fitCases);
        for (int iteration = 0; iteration < FIT_ITERATIONS; iteration++) {
            if (leftLoss <= rightLoss) {
                high = right;
                right = left;
                rightLoss = leftLoss;
                left = low + GOLDEN_RATIO_COMPLEMENT * (high - low);
                leftLoss = fitLoss(Math.exp(left), fitCases);
            } else {
                low = left;
                left = right;
                leftLoss = rightLoss;
                right = high - GOLDEN_RATIO_COMPLEMENT * (high - low);
                rightLoss = fitLoss(Math.exp(right), fitCases);
            }
        }
        return Math.exp((low + high) * 0.5D);
    }

    private static double fitLoss(double conductance, List<ReferenceCase> fitCases) {
        double loss = 0.0D;
        for (ReferenceCase reference : fitCases) {
            Trace candidate = simulateStaticImpedance(reference.fixture(), conductance);
            double scale = Math.max(
                    1.0D,
                    maximumAbsoluteDelta(reference.trace(), reference.fixture().naturalTemperatureC()));
            for (int sample = 0; sample < candidate.samples().size(); sample++) {
                double error = candidate.samples().get(sample).localTemperatureC()
                        - reference.trace().samples().get(sample).localTemperatureC();
                double normalized = error / scale;
                loss += normalized * normalized;
            }
        }
        return loss;
    }

    private static CaseResult compare(
            Fixture fixture,
            Trace reference,
            Trace candidate
    ) {
        if (reference.samples().size() != candidate.samples().size()) {
            throw new IllegalStateException("FarField traces have different sample counts");
        }
        double maximumTemperatureError = 0.0D;
        double maximumPhasePowerError = 0.0D;
        for (int sample = 0; sample < reference.samples().size(); sample++) {
            Sample expected = reference.samples().get(sample);
            Sample actual = candidate.samples().get(sample);
            maximumTemperatureError = Math.max(
                    maximumTemperatureError,
                    Math.abs(actual.localTemperatureC() - expected.localTemperatureC()));
            maximumPhasePowerError = Math.max(
                    maximumPhasePowerError,
                    Math.abs(actual.phaseReceivedPowerW() - expected.phaseReceivedPowerW()));
        }
        boolean crossingMismatch = reference.crossedThreshold() != candidate.crossedThreshold();
        double crossingError = crossingMismatch
                ? fixture.durationSeconds()
                : Math.abs(candidate.thresholdCrossingTimeSeconds()
                - reference.thresholdCrossingTimeSeconds());
        Sample expectedFinal = reference.samples().get(reference.samples().size() - 1);
        Sample actualFinal = candidate.samples().get(candidate.samples().size() - 1);
        return new CaseResult(
                fixture.id(),
                maximumTemperatureError,
                crossingError,
                crossingMismatch,
                actualFinal.boundaryEnergyFromNaturalJ()
                        - expectedFinal.boundaryEnergyFromNaturalJ(),
                maximumPhasePowerError);
    }

    private static double rk4Step(
            double[] temperatures,
            double[] capacities,
            Fixture fixture,
            double dtSeconds,
            RkScratch scratch
    ) {
        derivatives(temperatures, capacities, fixture, scratch.k1);
        combine(temperatures, scratch.k1, dtSeconds * 0.5D, scratch.stage);
        derivatives(scratch.stage, capacities, fixture, scratch.k2);
        combine(temperatures, scratch.k2, dtSeconds * 0.5D, scratch.stage);
        derivatives(scratch.stage, capacities, fixture, scratch.k3);
        combine(temperatures, scratch.k3, dtSeconds, scratch.stage);
        derivatives(scratch.stage, capacities, fixture, scratch.k4);

        double flux1 = naturalBoundaryFlux(temperatures, fixture);
        combine(temperatures, scratch.k1, dtSeconds * 0.5D, scratch.stage);
        double flux2 = naturalBoundaryFlux(scratch.stage, fixture);
        combine(temperatures, scratch.k2, dtSeconds * 0.5D, scratch.stage);
        double flux3 = naturalBoundaryFlux(scratch.stage, fixture);
        combine(temperatures, scratch.k3, dtSeconds, scratch.stage);
        double flux4 = naturalBoundaryFlux(scratch.stage, fixture);

        for (int cell = 0; cell < temperatures.length; cell++) {
            temperatures[cell] += dtSeconds / 6.0D
                    * (scratch.k1[cell] + 2.0D * scratch.k2[cell]
                    + 2.0D * scratch.k3[cell] + scratch.k4[cell]);
            if (!Double.isFinite(temperatures[cell])) {
                throw new ArithmeticException("reference FarField temperature became non-finite");
            }
        }
        return dtSeconds / 6.0D * (flux1 + 2.0D * flux2 + 2.0D * flux3 + flux4);
    }

    private static void derivatives(
            double[] temperatures,
            double[] capacities,
            Fixture fixture,
            double[] output
    ) {
        java.util.Arrays.fill(output, 0.0D);
        output[0] = fixture.sourcePowerW() / capacities[0];
        for (int interfaceIndex = 0;
             interfaceIndex < temperatures.length - 1;
             interfaceIndex++) {
            double flowToLeft = fixture.interfaceConductance(interfaceIndex)
                    * (temperatures[interfaceIndex + 1] - temperatures[interfaceIndex]);
            output[interfaceIndex] += flowToLeft / capacities[interfaceIndex];
            output[interfaceIndex + 1] -= flowToLeft / capacities[interfaceIndex + 1];
        }
        int last = temperatures.length - 1;
        output[last] += naturalBoundaryFlux(temperatures, fixture) / capacities[last];
    }

    private static double naturalBoundaryFlux(double[] temperatures, Fixture fixture) {
        int last = temperatures.length - 1;
        return fixture.interfaceConductance(last)
                * (fixture.naturalTemperatureC() - temperatures[last]);
    }

    private static void combine(
            double[] base,
            double[] derivative,
            double scale,
            double[] output
    ) {
        for (int index = 0; index < base.length; index++) {
            output[index] = base[index] + derivative[index] * scale;
        }
    }

    private static boolean alreadyCrossed(
            double temperature,
            double threshold,
            double sourcePower
    ) {
        return sourcePower >= 0.0D ? temperature >= threshold : temperature <= threshold;
    }

    private static boolean crosses(
            double before,
            double after,
            double threshold,
            double sourcePower
    ) {
        return sourcePower >= 0.0D
                ? before < threshold && after >= threshold
                : before > threshold && after <= threshold;
    }

    private static double crossingFraction(double before, double after, double threshold) {
        if (after == before) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, (threshold - before) / (after - before)));
    }

    private static double exactCrossingOffset(
            double initial,
            double equilibrium,
            double threshold,
            double ratePerSecond,
            double intervalSeconds
    ) {
        double ratio = (threshold - equilibrium) / (initial - equilibrium);
        if (!(ratio > 0.0D && ratio <= 1.0D)) {
            return intervalSeconds * crossingFraction(initial, equilibrium, threshold);
        }
        return Math.max(0.0D, Math.min(intervalSeconds, -Math.log(ratio) / ratePerSecond));
    }

    private static double phasePower(Fixture fixture, double localTemperatureC) {
        return fixture.phaseCouplingWPerK()
                * Math.max(0.0D, localTemperatureC - fixture.phaseBoundaryTemperatureC());
    }

    private static double maximumAbsoluteDelta(Trace trace, double naturalTemperatureC) {
        double maximum = 0.0D;
        for (Sample sample : trace.samples()) {
            maximum = Math.max(
                    maximum, Math.abs(sample.localTemperatureC() - naturalTemperatureC));
        }
        return maximum;
    }

    private static List<ReferenceCase> concat(
            List<ReferenceCase> first,
            List<ReferenceCase> second
    ) {
        List<ReferenceCase> combined = new ArrayList<>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        return combined;
    }

    private static double[] scaled(double[] values, double multiplier) {
        double[] scaled = values.clone();
        for (int index = 0; index < scaled.length; index++) {
            scaled[index] *= multiplier;
        }
        return scaled;
    }

    private static double seriesConductance(double[] conductances) {
        double resistance = 0.0D;
        for (double conductance : conductances) {
            resistance += 1.0D / conductance;
        }
        return 1.0D / resistance;
    }

    private static double[] requirePositiveArray(String name, double[] values) {
        if (values == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        double[] copy = values.clone();
        for (double value : copy) {
            requirePositiveFinite(name + " entry", value);
        }
        return copy;
    }

    private static void requirePositiveFinite(String name, double value) {
        requireFinite(name, value);
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegativeFinite(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0D) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private record ReferenceCase(Fixture fixture, Trace trace) {
    }

    private static final class RkScratch {
        private final double[] k1;
        private final double[] k2;
        private final double[] k3;
        private final double[] k4;
        private final double[] stage;

        private RkScratch(int cellCount) {
            k1 = new double[cellCount];
            k2 = new double[cellCount];
            k3 = new double[cellCount];
            k4 = new double[cellCount];
            stage = new double[cellCount];
        }
    }
}
