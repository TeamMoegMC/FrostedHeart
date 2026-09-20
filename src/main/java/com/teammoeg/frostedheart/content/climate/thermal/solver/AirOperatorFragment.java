/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;

/** Preintegrated Air volume terms. Geometry is visited only when compiling this fragment. */
public final class AirOperatorFragment {
    private static final double GAUSS_LOW = 0.5 - 0.5 / Math.sqrt(3);
    private static final double GAUSS_HIGH = 1 - GAUSS_LOW;
    final AirFieldLayout.Component component;
    final int[] indices;
    final double[] capacityJPerK;
    final double[] projectionMassJPerK;
    final double[] horizontalWPerK;
    final double[] verticalWPerK;
    final double[] lowerWeights;
    final double[] upperWeights;
    final double halfHeightDifference;
    private final long mixedBlocks;

    public AirOperatorFragment(AirFieldLayout.Component component, double airCapacityJPerBlockK,
            double mixingWPerBlockK, AirMixingRegion mixing, AirOperatorFragment previous) {
        this.component = component;
        int width = component.basisCount();
        indices = new int[width];
        for (int basis = 0; basis < width; basis++) indices[basis] = component.coefficientIndex(basis);
        long mixed = 0;
        long membership = component.airBlocks;
        while (membership != 0) {
            int block = Long.numberOfTrailingZeros(membership);
            membership &= membership - 1;
            if (mixing.volumeScale(component.minX + (block & 3) + 0.5, component.minY + (block >>> 4) + 0.5,
                    component.minZ + (block >>> 2 & 3) + 0.5) > 1) mixed |= 1L << block;
        }
        mixedBlocks = mixed;
        if (previous != null && mixedBlocks == previous.mixedBlocks && component.sameLocalBasis(previous.component)) {
            capacityJPerK = previous.capacityJPerK;
            projectionMassJPerK = previous.projectionMassJPerK;
            horizontalWPerK = previous.horizontalWPerK;
            verticalWPerK = previous.verticalWPerK;
            lowerWeights = previous.lowerWeights;
            upperWeights = previous.upperWeights;
            halfHeightDifference = previous.halfHeightDifference;
            return;
        }
        capacityJPerK = new double[width];
        horizontalWPerK = new double[width * (width + 1) / 2];
        projectionMassJPerK = new double[horizontalWPerK.length];
        verticalWPerK = new double[horizontalWPerK.length];
        lowerWeights = new double[width];
        upperWeights = new double[width];
        double lowerVolume = 0, upperVolume = 0, lowerHeight = 0, upperHeight = 0;
        double[] gradientsX = new double[width], gradientsY = new double[width], gradientsZ = new double[width];
        double[] values = new double[width];
        long blocks = component.airBlocks;
        while (blocks != 0) {
            int block = Long.numberOfTrailingZeros(blocks);
            blocks &= blocks - 1;
            int x = component.minX + (block & 3);
            int y = component.minY + (block >>> 4);
            int z = component.minZ + (block >>> 2 & 3);
            boolean lower = (block >>> 4) < 2;
            if (lower) { lowerVolume++; lowerHeight += y + 0.5; }
            else { upperVolume++; upperHeight += y + 0.5; }
            // Integral of a trilinear basis over a unit cube equals its center value.
            for (int basis = 0; basis < width; basis++) {
                double weight = component.weight(basis, x + 0.5, y + 0.5, z + 0.5);
                if (basis < 8) capacityJPerK[basis] += airCapacityJPerBlockK * weight;
                (lower ? lowerWeights : upperWeights)[basis] += weight;
            }
            double conductance = mixingWPerBlockK * ((mixedBlocks & 1L << block) == 0 ? 1 : AirMixingRegion.MULTIPLIER);
            for (int point = 0; point < 8; point++) {
                double px = x + ((point & 1) == 0 ? GAUSS_LOW : GAUSS_HIGH);
                double py = y + ((point & 4) == 0 ? GAUSS_LOW : GAUSS_HIGH);
                double pz = z + ((point & 2) == 0 ? GAUSS_LOW : GAUSS_HIGH);
                for (int basis = 0; basis < width; basis++) {
                    values[basis] = component.weight(basis, px, py, pz);
                    gradientsX[basis] = component.derivative(basis, 0, px, py, pz);
                    gradientsY[basis] = component.derivative(basis, 1, px, py, pz);
                    gradientsZ[basis] = component.derivative(basis, 2, px, py, pz);
                }
                int entry = 0;
                for (int row = 0; row < width; row++) {
                    for (int column = row; column < width; column++, entry++) {
                        projectionMassJPerK[entry] += airCapacityJPerBlockK * 0.125 * values[row] * values[column];
                        horizontalWPerK[entry] += conductance * 0.125 *
                                (gradientsX[row] * gradientsX[column] + gradientsZ[row] * gradientsZ[column]);
                        verticalWPerK[entry] += conductance * 0.125 * gradientsY[row] * gradientsY[column];
                    }
                }
            }
        }
        for (int basis = 0; basis < width; basis++) {
            if (lowerVolume > 0) lowerWeights[basis] /= lowerVolume;
            if (upperVolume > 0) upperWeights[basis] /= upperVolume;
        }
        int diagonalEntry = 0;
        for (int basis = 0; basis < width; basis++) {
            if (basis >= 8) capacityJPerK[basis] = projectionMassJPerK[diagonalEntry];
            diagonalEntry += width - basis;
        }
        halfHeightDifference = lowerVolume > 0 && upperVolume > 0
                ? upperHeight / upperVolume - lowerHeight / lowerVolume : 0;
    }

    double buoyancyFactor(double[] coefficientsC, BuoyancyConductance.Parameters parameters) {
        if (halfHeightDifference == 0) return 1;
        double lowerMinusUpperC = 0;
        for (int basis = 0; basis < indices.length; basis++) {
            lowerMinusUpperC += (lowerWeights[basis] - upperWeights[basis]) * coefficientsC[indices[basis]];
        }
        double factor = 1 + lowerMinusUpperC * 4 / halfHeightDifference / parameters.temperatureScaleK();
        return Math.max(parameters.minimumFactor(), Math.min(parameters.maximumFactor(), factor));
    }

    void addConductance(double[] input, double[] output, double dtSeconds, double buoyancyFactor) {
        int entry = 0;
        for (int row = 0; row < indices.length; row++) {
            int rowIndex = indices[row];
            for (int column = row; column < indices.length; column++, entry++) {
                int columnIndex = indices[column];
                double value = dtSeconds * (horizontalWPerK[entry] + buoyancyFactor * verticalWPerK[entry]);
                output[rowIndex] += value * input[columnIndex];
                if (row != column) output[columnIndex] += value * input[rowIndex];
            }
        }
    }

    void addDiagonal(double[] diagonal, double dtSeconds, double buoyancyFactor) {
        int entry = 0;
        for (int row = 0; row < indices.length; row++) {
            diagonal[indices[row]] += dtSeconds * (horizontalWPerK[entry] + buoyancyFactor * verticalWPerK[entry]);
            entry += indices.length - row;
        }
    }

    void addProjectionMass(double[] input, double[] output) {
        int entry = 0;
        for (int row = 0; row < indices.length; row++) {
            for (int column = row; column < indices.length; column++, entry++) {
                double capacity = projectionMassJPerK[entry];
                output[indices[row]] += capacity * input[indices[column]];
                if (row != column) output[indices[column]] += capacity * input[indices[row]];
            }
        }
    }

    /** Coarse/coarse mass is lumped; every cross term involving a local mode remains. */
    void addCrossMass(double[] input, double[] output) {
        int entry = 0;
        for (int row = 0; row < indices.length; row++) {
            for (int column = row; column < indices.length; column++, entry++) {
                if (row == column || row < 8 && column < 8) continue;
                double capacity = projectionMassJPerK[entry];
                output[indices[row]] += capacity * input[indices[column]];
                output[indices[column]] += capacity * input[indices[row]];
            }
        }
    }

    void addModeCouplings(int[] partners, double[] offDiagonal, double dtSeconds, double buoyancyFactor) {
        int width = indices.length;
        for (int row = 8; row < width; row++) {
            int partner = partners[indices[row]];
            if (partner < indices[row]) continue;
            int firstEntry = row * width - row * (row - 1) / 2;
            for (int column = row + 1; column < width; column++) {
                if (indices[column] != partner) continue;
                int entry = firstEntry + column - row;
                offDiagonal[indices[row]] += projectionMassJPerK[entry]
                        + dtSeconds * (horizontalWPerK[entry] + buoyancyFactor * verticalWPerK[entry]);
            }
        }
    }
}
