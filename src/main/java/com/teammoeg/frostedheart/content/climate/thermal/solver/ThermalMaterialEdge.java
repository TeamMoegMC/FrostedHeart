/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

/** Immutable aggregate and canonical raw-contribution order for one edge. */
public record ThermalMaterialEdge(
        int first,
        int second,
        int firstGeneration,
        int secondGeneration,
        int[] contributorFragments,
        int[] contributorOperations,
        long[] contributorRanks,
        double[] contributorConductance,
        double conductance,
        double coefficient,
        int ownerFragment,
        int ownerOperation
) {
    public ThermalMaterialEdge {
        int size = contributorFragments == null
                ? -1 : contributorFragments.length;
        if (first < 0 || second <= first
                || firstGeneration < 0 || secondGeneration < 0
                || size <= 0 || contributorOperations == null
                || contributorOperations.length != size
                || contributorRanks == null
                || contributorRanks.length != size
                || contributorConductance == null
                || contributorConductance.length != size
                || ownerFragment < 0 || ownerOperation < 0
                || !Double.isFinite(conductance) || conductance < 0.0D
                || !Double.isFinite(coefficient) || coefficient < 0.0D) {
            throw new IllegalArgumentException("material edge payload is invalid");
        }
    }

    public int contributionCount() { return contributorFragments.length; }
    public int contributorFragment(int index) {
        return contributorFragments[index];
    }
    public int contributorOperation(int index) {
        return contributorOperations[index];
    }
    public long contributorRank(int index) { return contributorRanks[index]; }
    public double contributorConductance(int index) {
        return contributorConductance[index];
    }
}
