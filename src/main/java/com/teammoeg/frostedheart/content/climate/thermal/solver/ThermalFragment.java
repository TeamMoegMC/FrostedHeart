/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Objects;

/** Exact persistent primitive operations owned by one topology Brick. */
public record ThermalFragment(
        long spatialRank,
        AirPairs airPairs,
        MaterialContributions materialContributions,
        PhaseContacts phaseContacts,
        FarBoundaries farBoundaries
) {
    public static final ThermalFragment EMPTY = new ThermalFragment(
            0L, AirPairs.EMPTY, MaterialContributions.EMPTY,
            PhaseContacts.EMPTY, FarBoundaries.EMPTY);

    public ThermalFragment {
        Objects.requireNonNull(airPairs, "airPairs");
        Objects.requireNonNull(materialContributions, "materialContributions");
        Objects.requireNonNull(phaseContacts, "phaseContacts");
        Objects.requireNonNull(farBoundaries, "farBoundaries");
    }

    public boolean isEmpty() {
        return airPairs.size() == 0
                && materialContributions.size() == 0
                && phaseContacts.size() == 0
                && farBoundaries.size() == 0;
    }

    /** Buoyant Air pairs; arrays transfer ownership to this value. */
    public static final class AirPairs {
        public static final AirPairs EMPTY = new AirPairs(
                new int[0], new int[0], new double[0],
                new double[0], new double[0]);

        private final int[] first;
        private final int[] second;
        private final double[] conductance;
        private final double[] firstCenterY;
        private final double[] secondCenterY;

        public AirPairs(
                int[] first,
                int[] second,
                double[] conductance,
                double[] firstCenterY,
                double[] secondCenterY
        ) {
            int size = length(first);
            if (length(second) != size || length(conductance) != size
                    || length(firstCenterY) != size
                    || length(secondCenterY) != size) {
                throw new IllegalArgumentException("Air pair arrays differ");
            }
            this.first = first;
            this.second = second;
            this.conductance = conductance;
            this.firstCenterY = firstCenterY;
            this.secondCenterY = secondCenterY;
        }

        public int size() { return first.length; }
        public int first(int index) { return first[index]; }
        public int second(int index) { return second[index]; }
        public double conductance(int index) { return conductance[index]; }
        public double firstCenterY(int index) { return firstCenterY[index]; }
        public double secondCenterY(int index) { return secondCenterY[index]; }
    }

    /** Raw material contributions used only for local edge aggregation. */
    public static final class MaterialContributions {
        public static final MaterialContributions EMPTY =
                new MaterialContributions(
                        new int[0], new int[0], new double[0]);

        private final int[] first;
        private final int[] second;
        private final double[] conductance;

        public MaterialContributions(
                int[] first,
                int[] second,
                double[] conductance
        ) {
            int size = length(first);
            if (length(second) != size || length(conductance) != size) {
                throw new IllegalArgumentException(
                        "material contribution arrays differ");
            }
            this.first = first;
            this.second = second;
            this.conductance = conductance;
        }

        public int size() { return first.length; }
        public int first(int index) { return first[index]; }
        public int second(int index) { return second[index]; }
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

    public static final class PhaseContacts {
        public static final PhaseContacts EMPTY = new PhaseContacts(
                new int[0], new int[0], new double[0]);

        private final int[] air;
        private final int[] reservoir;
        private final double[] conductance;

        public PhaseContacts(
                int[] air,
                int[] reservoir,
                double[] conductance
        ) {
            int size = length(air);
            if (length(reservoir) != size
                    || length(conductance) != size) {
                throw new IllegalArgumentException("phase contact arrays differ");
            }
            this.air = air;
            this.reservoir = reservoir;
            this.conductance = conductance;
        }

        public int size() { return air.length; }
        public int air(int index) { return air[index]; }
        public int reservoir(int index) { return reservoir[index]; }
        public double conductance(int index) { return conductance[index]; }
    }

    /** Local exposed Air boundaries with lazy wind-dependent coefficients. */
    public static final class FarBoundaries {
        public static final FarBoundaries EMPTY = new FarBoundaries(
                new int[0], -1, new double[0], new double[0]);

        private final int[] cell;
        private final int pageSlot;
        private final double[] baseConductance;
        private final double[] coefficient;
        private long coefficientWindGeneration = -1L;

        public FarBoundaries(
                int[] cell,
                int pageSlot,
                double[] baseConductance,
                double[] coefficient
        ) {
            int size = length(cell);
            if (length(baseConductance) != size
                    || length(coefficient) != size
                    || size != 0 && pageSlot < 0) {
                throw new IllegalArgumentException("FarField arrays differ");
            }
            this.cell = cell;
            this.pageSlot = pageSlot;
            this.baseConductance = baseConductance;
            this.coefficient = coefficient;
        }

        public int size() { return cell.length; }
        public int cell(int index) { return cell[index]; }
        public int pageSlot() { return pageSlot; }
        public double baseConductance(int index) {
            return baseConductance[index];
        }
        public double coefficient(int index) { return coefficient[index]; }
        public long coefficientWindGeneration() {
            return coefficientWindGeneration;
        }
        public void cacheCoefficient(int index, double value) {
            coefficient[index] = value;
        }
        public void finishCoefficientRefresh(long generation) {
            coefficientWindGeneration = generation;
        }
    }

    private static int length(int[] values) {
        return Objects.requireNonNull(values, "array").length;
    }

    private static int length(double[] values) {
        return Objects.requireNonNull(values, "array").length;
    }

}
