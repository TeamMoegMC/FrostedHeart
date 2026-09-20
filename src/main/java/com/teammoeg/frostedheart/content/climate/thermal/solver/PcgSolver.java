/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Arrays;

/** Allocation-free block-preconditioned conjugate gradients for a fixed thermal system. */
final class PcgSolver {
    static final double CORRECTION_TOLERANCE_C = 1e-9;
    private double[] residualJ = new double[0];
    private double[] directionC = new double[0];
    private double[] appliedJ = new double[0];
    private double[] preconditionedC = new double[0];
    private int iterations;

    int iterations() { return iterations; }

    boolean solve(CoupledThermalOperator operator, double[] rhsJ, double[] solutionC) {
        int size = operator.size();
        reserve(size);
        operator.apply(solutionC, appliedJ);
        double residualProduct = 0;
        for (int index = 0; index < size; index++) {
            residualJ[index] = rhsJ[index] - appliedJ[index];
        }
        operator.precondition(residualJ, preconditionedC);
        for (int index = 0; index < size; index++) {
            directionC[index] = preconditionedC[index];
            residualProduct += residualJ[index] * preconditionedC[index];
        }
        iterations = 0;
        int maximumIterations = Math.max(64, Math.min(2048, size * 2));
        while (iterations < maximumIterations) {
            if (maximumCorrection(size) <= CORRECTION_TOLERANCE_C) {
                operator.apply(solutionC, appliedJ);
                for (int index = 0; index < size; index++) residualJ[index] = rhsJ[index] - appliedJ[index];
                operator.precondition(residualJ, preconditionedC);
                if (maximumCorrection(size) <= CORRECTION_TOLERANCE_C) return true;
                residualProduct = 0;
                for (int index = 0; index < size; index++) {
                    directionC[index] = preconditionedC[index];
                    residualProduct += residualJ[index] * preconditionedC[index];
                }
            }
            operator.apply(directionC, appliedJ);
            double curvature = 0;
            for (int index = 0; index < size; index++) curvature += directionC[index] * appliedJ[index];
            if (!(curvature > 0) || !Double.isFinite(curvature)) return false;
            double alpha = residualProduct / curvature;
            double nextProduct = 0;
            for (int index = 0; index < size; index++) {
                solutionC[index] += alpha * directionC[index];
                residualJ[index] -= alpha * appliedJ[index];
            }
            operator.precondition(residualJ, preconditionedC);
            for (int index = 0; index < size; index++) {
                nextProduct += residualJ[index] * preconditionedC[index];
            }
            double beta = nextProduct / residualProduct;
            for (int index = 0; index < size; index++) directionC[index] = preconditionedC[index] + beta * directionC[index];
            residualProduct = nextProduct;
            iterations++;
        }
        return false;
    }

    private double maximumCorrection(int size) {
        double maximum = 0;
        for (int index = 0; index < size; index++) maximum = Math.max(maximum, Math.abs(preconditionedC[index]));
        return maximum;
    }

    private void reserve(int size) {
        if (size <= residualJ.length) return;
        residualJ = Arrays.copyOf(residualJ, size);
        directionC = Arrays.copyOf(directionC, size);
        appliedJ = Arrays.copyOf(appliedJ, size);
        preconditionedC = Arrays.copyOf(preconditionedC, size);
    }
}
