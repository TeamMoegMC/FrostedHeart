/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

/** Immutable aggregate and canonical raw-contribution order for one edge. */
public final class ThermalMaterialEdge {
    private final int first;
    private final int second;
    private final int[] contributorFragments;
    private final int[] contributorOperations;
    private final long[] contributorRanks;
    private final double[] contributorConductance;
    private final double conductance;
    private final double coefficient;
    private final int ownerFragment;
    private final int ownerOperation;

    public ThermalMaterialEdge(
            int first,
            int second,
            int[] contributorFragments,
            int[] contributorOperations,
            long[] contributorRanks,
            double[] contributorConductance,
            double conductance,
            double coefficient,
            int ownerFragment,
            int ownerOperation
    ) {
        int size = contributorFragments == null
                ? -1 : contributorFragments.length;
        if (first < 0 || second <= first
                || size <= 0 || contributorOperations == null
                || contributorOperations.length != size
                || contributorRanks == null
                || contributorRanks.length != size
                || contributorConductance == null
                || contributorConductance.length != size
                || ownerFragment < 0 || ownerOperation < 0
                || !Double.isFinite(conductance) || conductance < 0.0D
                || !Double.isFinite(coefficient) || coefficient < 0.0D) {
            throw new IllegalArgumentException(
                    "material edge payload is invalid");
        }
        this.first = first;
        this.second = second;
        this.contributorFragments = contributorFragments;
        this.contributorOperations = contributorOperations;
        this.contributorRanks = contributorRanks;
        this.contributorConductance = contributorConductance;
        this.conductance = conductance;
        this.coefficient = coefficient;
        this.ownerFragment = ownerFragment;
        this.ownerOperation = ownerOperation;
    }

    public int first() { return first; }
    public int second() { return second; }
    public double conductance() { return conductance; }
    public double coefficient() { return coefficient; }
    public int ownerFragment() { return ownerFragment; }
    public int ownerOperation() { return ownerOperation; }
    public int contributionCount() { return contributorFragments.length; }
    public int contributorFragment(int index) {
        return contributorFragments[index];
    }
    public int contributorOperation(int index) {
        return contributorOperations[index];
    }
    public long contributorRank(int index) {
        return contributorRanks[index];
    }
    public double contributorConductance(int index) {
        return contributorConductance[index];
    }
}
