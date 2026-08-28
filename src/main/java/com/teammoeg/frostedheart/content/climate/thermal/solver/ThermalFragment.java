/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Objects;

/** Exact persistent primitive operations owned by one topology Brick. */
public record ThermalFragment(
        long spatialRank,
        AirPairs airPairs,
        MaterialContributions materialContributions,
        FixedBoundaries fixedBoundaries,
        PhaseContacts phaseContacts,
        FarBoundaries farBoundaries
) {
    public static final ThermalFragment EMPTY = new ThermalFragment(
            0L, AirPairs.EMPTY, MaterialContributions.EMPTY,
            FixedBoundaries.EMPTY, PhaseContacts.EMPTY, FarBoundaries.EMPTY);

    public ThermalFragment {
        Objects.requireNonNull(airPairs, "airPairs");
        Objects.requireNonNull(materialContributions, "materialContributions");
        Objects.requireNonNull(fixedBoundaries, "fixedBoundaries");
        Objects.requireNonNull(phaseContacts, "phaseContacts");
        Objects.requireNonNull(farBoundaries, "farBoundaries");
    }

    public boolean isEmpty() {
        return airPairs.size() == 0
                && materialContributions.size() == 0
                && fixedBoundaries.size() == 0
                && phaseContacts.size() == 0
                && farBoundaries.size() == 0;
    }

    /** Fixed or buoyant Air pairs; arrays transfer ownership to this value. */
    public record AirPairs(
            int[] first,
            int[] second,
            int[] firstGeneration,
            int[] secondGeneration,
            double[] conductance,
            double[] coefficient,
            double[] firstCenterY,
            double[] secondCenterY,
            byte[] buoyant
    ) {
        public static final AirPairs EMPTY = new AirPairs(
                new int[0], new int[0], new int[0], new int[0],
                new double[0], new double[0], new double[0], new double[0],
                new byte[0]);

        public AirPairs {
            int size = length(first);
            if (length(second) != size || length(firstGeneration) != size
                    || length(secondGeneration) != size
                    || length(conductance) != size
                    || length(coefficient) != size
                    || length(firstCenterY) != size
                    || length(secondCenterY) != size
                    || length(buoyant) != size) {
                throw new IllegalArgumentException("Air pair arrays differ");
            }
        }

        public int size() { return first.length; }
        public int first(int index) { return first[index]; }
        public int second(int index) { return second[index]; }
        public int firstGeneration(int index) { return firstGeneration[index]; }
        public int secondGeneration(int index) { return secondGeneration[index]; }
        public double conductance(int index) { return conductance[index]; }
        public double coefficient(int index) { return coefficient[index]; }
        public double firstCenterY(int index) { return firstCenterY[index]; }
        public double secondCenterY(int index) { return secondCenterY[index]; }
        public boolean buoyant(int index) { return buoyant[index] != 0; }
    }

    /** Raw material contributions used only for local edge aggregation. */
    public record MaterialContributions(
            int[] first,
            int[] second,
            int[] firstGeneration,
            int[] secondGeneration,
            double[] conductance
    ) {
        public static final MaterialContributions EMPTY =
                new MaterialContributions(
                        new int[0], new int[0], new int[0], new int[0],
                        new double[0]);

        public MaterialContributions {
            int size = length(first);
            if (length(second) != size || length(firstGeneration) != size
                    || length(secondGeneration) != size
                    || length(conductance) != size) {
                throw new IllegalArgumentException(
                        "material contribution arrays differ");
            }
        }

        public int size() { return first.length; }
        public int first(int index) { return first[index]; }
        public int second(int index) { return second[index]; }
        public int firstGeneration(int index) { return firstGeneration[index]; }
        public int secondGeneration(int index) { return secondGeneration[index]; }
        public double conductance(int index) { return conductance[index]; }
        public long edgeKey(int index) {
            int left = first[index];
            int right = second[index];
            if (left > right) {
                int swap = left;
                left = right;
                right = swap;
            }
            return (long) left << 32 | right & 0xffff_ffffL;
        }
    }

    public record FixedBoundaries(
            int[] cell,
            int[] generation,
            double[] temperatureC,
            double[] conductance,
            double[] coefficient
    ) {
        public static final FixedBoundaries EMPTY = new FixedBoundaries(
                new int[0], new int[0], new double[0],
                new double[0], new double[0]);

        public FixedBoundaries {
            int size = length(cell);
            if (length(generation) != size || length(temperatureC) != size
                    || length(conductance) != size
                    || length(coefficient) != size) {
                throw new IllegalArgumentException("fixed boundary arrays differ");
            }
        }

        public int size() { return cell.length; }
        public int cell(int index) { return cell[index]; }
        public int generation(int index) { return generation[index]; }
        public double temperatureC(int index) { return temperatureC[index]; }
        public double conductance(int index) { return conductance[index]; }
        public double coefficient(int index) { return coefficient[index]; }
    }

    public record PhaseContacts(
            int[] air,
            int[] airGeneration,
            int[] reservoir,
            int[] reservoirGeneration,
            double[] conductance
    ) {
        public static final PhaseContacts EMPTY = new PhaseContacts(
                new int[0], new int[0], new int[0], new int[0], new double[0]);

        public PhaseContacts {
            int size = length(air);
            if (length(airGeneration) != size || length(reservoir) != size
                    || length(reservoirGeneration) != size
                    || length(conductance) != size) {
                throw new IllegalArgumentException("phase contact arrays differ");
            }
        }

        public int size() { return air.length; }
        public int air(int index) { return air[index]; }
        public int airGeneration(int index) { return airGeneration[index]; }
        public int reservoir(int index) { return reservoir[index]; }
        public int reservoirGeneration(int index) {
            return reservoirGeneration[index];
        }
        public double conductance(int index) { return conductance[index]; }
    }

    /** Local exposed Air boundaries with lazy wind-dependent coefficients. */
    public record FarBoundaries(
            int[] cell,
            int[] generation,
            int[] pageSlot,
            double[] baseConductance,
            double[] coefficient,
            long[] coefficientWindGeneration
    ) {
        public static final FarBoundaries EMPTY = new FarBoundaries(
                new int[0], new int[0], new int[0], new double[0],
                new double[0], new long[0]);

        public FarBoundaries {
            int size = length(cell);
            if (length(generation) != size || length(pageSlot) != size
                    || length(baseConductance) != size
                    || length(coefficient) != size
                    || length(coefficientWindGeneration) != size) {
                throw new IllegalArgumentException("FarField arrays differ");
            }
        }

        public int size() { return cell.length; }
        public int cell(int index) { return cell[index]; }
        public int generation(int index) { return generation[index]; }
        public int pageSlot(int index) { return pageSlot[index]; }
        public double baseConductance(int index) {
            return baseConductance[index];
        }
        public double coefficient(int index) { return coefficient[index]; }
        public long coefficientWindGeneration(int index) {
            return coefficientWindGeneration[index];
        }
        public void cacheCoefficient(int index, double value, long generation) {
            coefficient[index] = value;
            coefficientWindGeneration[index] = generation;
        }
    }

    private static int length(int[] values) {
        return Objects.requireNonNull(values, "array").length;
    }

    private static int length(long[] values) {
        return Objects.requireNonNull(values, "array").length;
    }

    private static int length(double[] values) {
        return Objects.requireNonNull(values, "array").length;
    }

    private static int length(byte[] values) {
        return Objects.requireNonNull(values, "array").length;
    }
}
