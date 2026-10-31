/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;

/** One component's volume coefficients and buoyancy probe; fine cells share this description. */
public final class AirOperatorFragment {
    final AirFieldLayout.Component component;
    final double[] capacityJPerK;
    final double[] horizontalWPerK;
    final double[] verticalWPerK;
    final double[] lowerWeights;
    final double[] upperWeights;
    final double halfHeightDifference;
    private final UnitMatrices mixedMatrices;
    private final long mixedCells;

    /** Direct four-block CV: captured Air branches and shared coarse Q1 geometry, without unit vertices. */
    static AirOperatorFragment coarseGridVolume(AirFieldLayout.Component component, double capacity,
            double conductance, AirMixingRegion mixing) {
        long mixed = 0, remaining = component.airBlocks;
        while (remaining != 0) {
            int block = Long.numberOfTrailingZeros(remaining);
            remaining &= remaining - 1;
            if (mixing.volumeScale(component.minX + (block & 3) + .5,
                    component.minY + (block >>> 4) + .5, component.minZ + (block >>> 2 & 3) + .5) > 1)
                mixed |= 1L << block;
        }
        return new AirOperatorFragment(component, capacity, conductance, mixed);
    }

    private AirOperatorFragment(AirFieldLayout.Component component, double capacity, double conductance,
            long mixed) {
        if (component.isUnitGrid() || component.basisCount() != 8)
            throw new IllegalArgumentException("Direct coarse CV requires the captured eight-corner basis");
        this.component = component;
        mixedMatrices = null; mixedCells = 0;
        capacityJPerK = new double[8];
        horizontalWPerK = new double[36];
        verticalWPerK = new double[36];
        lowerWeights = new double[8];
        upperWeights = new double[8];
        int lowCount = 0, highCount = 0;
        double lowHeight = 0, highHeight = 0;
        long remaining = component.airBlocks;
        while (remaining != 0) {
            int block = Long.numberOfTrailingZeros(remaining);
            remaining &= remaining - 1;
            double x = component.minX + (block & 3) + .5;
            double y = component.minY + (block >>> 4) + .5;
            double z = component.minZ + (block >>> 2 & 3) + .5;
            boolean low = (block >>> 4) < 2;
            if (low) { lowCount++; lowHeight += y; }
            else { highCount++; highHeight += y; }
            for (int corner = 0; corner < 8; corner++) {
                double weight = component.weightAtAirCell(corner, block, x, y, z);
                capacityJPerK[corner] += capacity * weight;
                (low ? lowerWeights : upperWeights)[corner] += weight;
            }
        }
        // A full four-block cube has G = k * area / length = k on each axial CV edge.
        // Partial captured Air uses its occupied/mixed volume fraction: an explicit overload approximation.
        double edgeConductance = conductance * (Long.bitCount(component.airBlocks)
                + (AirMixingRegion.MULTIPLIER - 1) * Long.bitCount(mixed)) / 64.0;
        for (int corner = 0; corner < 8; corner++) {
            int diagonal = triangleEntry(corner, corner);
            if (lowCount != 0) lowerWeights[corner] /= lowCount;
            if (highCount != 0) upperWeights[corner] /= highCount;
            for (int axisBit = 1; axisBit <= 4; axisBit <<= 1) {
                if ((corner & axisBit) != 0) continue;
                int other = corner | axisBit;
                double[] stiffness = axisBit == 4 ? verticalWPerK : horizontalWPerK;
                stiffness[triangleEntry(corner, other)] = -edgeConductance;
                stiffness[diagonal] += edgeConductance;
                stiffness[triangleEntry(other, other)] += edgeConductance;
            }
        }
        halfHeightDifference = lowCount != 0 && highCount != 0
                ? highHeight / highCount - lowHeight / lowCount : 0;
    }

    private static int triangleEntry(int row, int column) {
        return row * 8 - row * (row - 1) / 2 + column - row;
    }

    private AirOperatorFragment(AirFieldLayout.Component component, long mixedCells, double[] capacity,
            UnitMatrices ordinary, UnitMatrices mixed) {
        this.component = component; this.mixedCells = mixedCells; mixedMatrices = mixed;
        capacityJPerK = capacity;
        horizontalWPerK = ordinary.horizontal; verticalWPerK = ordinary.vertical;
        lowerWeights = new double[component.basisCount()]; upperWeights = new double[component.basisCount()];
        int lowerCount = 0, upperCount = 0; double lowerHeight = 0, upperHeight = 0;
        long mask = component.airBlocks;
        while (mask != 0) {
            int block = Long.numberOfTrailingZeros(mask); mask &= mask - 1;
            boolean low = (block >>> 4) < 2;
            if (low) { lowerCount++; lowerHeight += (block >>> 4) + .5; }
            else { upperCount++; upperHeight += (block >>> 4) + .5; }
            for (int corner = 0; corner < 8; corner++) (low ? lowerWeights : upperWeights)[component.unitCorner(block, corner)] += .125;
        }
        for (int i = 0; i < lowerWeights.length; i++) {
            if (lowerCount > 0) lowerWeights[i] /= lowerCount;
            if (upperCount > 0) upperWeights[i] /= upperCount;
        }
        halfHeightDifference = lowerCount == 0 || upperCount == 0 ? 0 : upperHeight / upperCount - lowerHeight / lowerCount;
    }

    /** A coarse fragment is one volume; a fine fragment retains its original ascending cell order. */
    long cells() { return component.isUnitGrid() ? component.airBlocks : 1L; }
    int index(int cell, int corner) {
        return component.coefficientIndex(component.isUnitGrid() ? component.unitCorner(cell, corner) : corner);
    }
    double[] horizontal(int cell) { return (mixedCells & 1L << cell) == 0 ? horizontalWPerK : mixedMatrices.horizontal; }
    double[] vertical(int cell) { return (mixedCells & 1L << cell) == 0 ? verticalWPerK : mixedMatrices.vertical; }

    static final class UnitGridPreparation {
        final AirFieldLayout layout;
        final double[] capacity = new double[8];
        final UnitMatrices ordinary, mixed;
        final com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources bindings;
        final AirMixingRegion mixing = new AirMixingRegion();
        final AirOperatorFragment[] result;
        int componentCursor;
        long mask, enhancedCells;

        UnitGridPreparation(AirFieldLayout layout, double capacity, double mixingWPerK,
                com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources bindings) {
            this.layout = layout; this.bindings = bindings;
            java.util.Arrays.fill(this.capacity, capacity / 8);
            ordinary = new UnitMatrices(mixingWPerK);
            mixed = new UnitMatrices(mixingWPerK * AirMixingRegion.MULTIPLIER);
            result = new AirOperatorFragment[layout.componentCount()];
        }
        boolean run(long deadline) {
            do {
                if (componentCursor == layout.componentCount()) return true;
                var component = layout.component(componentCursor);
                if (mask == 0) {
                    mixing.reset(component.minX, component.minY, component.minZ); bindings.collectMixingSources(mixing);
                    mask = component.airBlocks; enhancedCells = 0;
                }
                int remaining = 16;
                while (remaining-- > 0 && mask != 0) {
                    int block = Long.numberOfTrailingZeros(mask); mask &= mask - 1;
                    boolean enhanced = mixing.volumeScale(component.minX + (block & 3) + .5,
                            component.minY + (block >>> 4) + .5, component.minZ + (block >>> 2 & 3) + .5) > 1;
                    if (enhanced) enhancedCells |= 1L << block;
                }
                if (mask == 0) result[componentCursor++] = new AirOperatorFragment(component, enhancedCells, capacity, ordinary, mixed);
            } while (System.nanoTime() < deadline);
            return componentCursor == layout.componentCount();
        }
    }

    private static final class UnitMatrices {
        final double[] horizontal = new double[36], vertical = new double[36];
        UnitMatrices(double conductance) {
            int entry = 0;
            for (int row = 0; row < 8; row++) for (int column = row; column < 8; column++, entry++) {
                if (row == column) {
                    horizontal[entry] = conductance / 2;
                    vertical[entry] = conductance / 4;
                } else {
                    int direction = row ^ column;
                    if (direction == 1 || direction == 2) horizontal[entry] = -conductance / 4;
                    else if (direction == 4) vertical[entry] = -conductance / 4;
                }
            }
        }
    }

    void recordBacking(AirBackingMeter meter) {
        meter.array(capacityJPerK);
        meter.array(horizontalWPerK);
        meter.array(verticalWPerK);
        meter.array(lowerWeights);
        meter.array(upperWeights);
        if (mixedMatrices != null) { meter.array(mixedMatrices.horizontal); meter.array(mixedMatrices.vertical); }
    }

    double buoyancyFactor(double[] coefficientsC, BuoyancyConductance.Parameters parameters) {
        if (halfHeightDifference == 0) return 1;
        double lowerMinusUpperC = 0;
        for (int basis = 0; basis < lowerWeights.length; basis++) {
            lowerMinusUpperC += (lowerWeights[basis] - upperWeights[basis]) * coefficientsC[component.coefficientIndex(basis)];
        }
        double factor = 1 + lowerMinusUpperC * 4 / halfHeightDifference / parameters.temperatureScaleK();
        return Math.max(parameters.minimumFactor(), Math.min(parameters.maximumFactor(), factor));
    }

}
