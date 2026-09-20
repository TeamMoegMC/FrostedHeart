/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;

/** Fixed backward-Euler system for one material segment and one buoyancy iteration. */
final class CoupledThermalOperator {
    final AirOperatorFragment[] volumes;
    final double[] massJPerK;
    final boolean[] fixedTemperature;
    final double[] fixedOffsetC;
    final boolean[] waiting;
    final double[] buoyancyFactors;
    final double[] diagonalJPerK;
    private final int[] contactOffsets;
    private final int[] contactIndices;
    private final double[] contactWeights;
    private final double[] conductanceWPerK;
    private final int[] firstMaterialEntries;
    private final int[] secondMaterialEntries;
    private final int[] boundaryPages;
    private final AirRouteValidity[] routes;
    private final double[] activeConductanceWPerK;
    private final double[] boundaryOffsetC;
    private double dtSeconds;
    private enum Mode { TRANSPORT, PROJECTION, MASS }
    private Mode mode = Mode.TRANSPORT;
    private final int airUnknownCount;
    private final int[] modePartners;
    private final int[] pairedContactEntries;
    private final double[] modeOffDiagonal;

    CoupledThermalOperator(int size, AirFieldLayout layout,
            AirOperatorFragment[] volumes, Contacts contacts) {
        int airUnknownCount = layout.coefficientCount();
        this.airUnknownCount = airUnknownCount;
        modePartners = new int[size];
        modeOffDiagonal = new double[size];
        Arrays.fill(modePartners, -1);
        for (int mode = 0; mode + 1 < layout.localShapeCount(); mode++) {
            var first = layout.localShape(mode);
            var second = layout.localShape(mode + 1);
            if (first.sourceBlock == second.sourceBlock && first.sourceFace == second.sourceFace && first.branch == second.branch) {
                int index = layout.coarseCount() + mode;
                modePartners[index] = index + 1;
                modePartners[index + 1] = index;
                mode++;
            }
        }
        this.volumes = volumes;
        massJPerK = new double[size];
        diagonalJPerK = new double[size];
        fixedTemperature = new boolean[size];
        fixedOffsetC = new double[size];
        waiting = new boolean[size];
        buoyancyFactors = new double[volumes.length];
        Arrays.fill(buoyancyFactors, 1);
        for (AirOperatorFragment volume : volumes) {
            for (int index = 0; index < volume.indices.length; index++) {
                massJPerK[volume.indices[index]] += volume.capacityJPerK[index];
            }
        }
        contactOffsets = contacts.offsets.toIntArray();
        contactIndices = contacts.indices.toIntArray();
        contactWeights = contacts.weights.toDoubleArray();
        conductanceWPerK = contacts.conductances.toDoubleArray();
        firstMaterialEntries = new int[conductanceWPerK.length];
        secondMaterialEntries = new int[conductanceWPerK.length];
        Arrays.fill(firstMaterialEntries, -1);
        Arrays.fill(secondMaterialEntries, -1);
        boundaryPages = contacts.boundaryPages.toIntArray();
        routes = contacts.routes.toArray(AirRouteValidity[]::new);
        activeConductanceWPerK = new double[conductanceWPerK.length];
        boundaryOffsetC = new double[conductanceWPerK.length];
        pairedContactEntries = new int[contactIndices.length];
        Arrays.fill(pairedContactEntries, -1);
        for (int contact = 0; contact < conductanceWPerK.length; contact++) {
            int end = contactOffsets[contact + 1];
            for (int entry = contactOffsets[contact]; entry < end; entry++) {
                if (contactIndices[entry] == contacts.firstMaterials.getInt(contact)) firstMaterialEntries[contact] = entry;
                if (contactIndices[entry] == contacts.secondMaterials.getInt(contact)) secondMaterialEntries[contact] = entry;
                int index = contactIndices[entry], partner = modePartners[index];
                if (partner < index) continue;
                for (int other = contactOffsets[contact]; other < end; other++) {
                    if (contactIndices[other] == partner) { pairedContactEntries[entry] = other; break; }
                }
            }
        }
    }

    int size() { return massJPerK.length; }

    /** Contact activity and environment stay fixed throughout one material/buoyancy trial. */
    void refreshContacts(ThermalSolver geometry, double referenceC) {
        for (int contact = 0; contact < conductanceWPerK.length; contact++) {
            boolean paused = materialWaiting(firstMaterialEntries[contact])
                    || materialWaiting(secondMaterialEntries[contact])
                    || routes[contact] != null && !routes[contact].activeForSolve();
            boolean boundary = boundaryPages[contact] >= 0;
            activeConductanceWPerK[contact] = paused ? 0 : conductanceWPerK[contact] * (boundary ? geometry.windScale() : 1);
            boundaryOffsetC[contact] = boundary ? geometry.naturalTemperatureC(boundaryPages[contact]) - referenceC : 0;
        }
    }

    private boolean materialWaiting(int entry) {
        return entry >= 0 && waiting[contactIndices[entry]];
    }

    void prepare(double dtSeconds) {
        mode = Mode.TRANSPORT;
        Arrays.fill(modeOffDiagonal, 0);
        this.dtSeconds = dtSeconds;
        for (int index = 0; index < size(); index++) {
            diagonalJPerK[index] = fixedTemperature[index] ? 1 : massJPerK[index];
        }
        for (int volume = 0; volume < volumes.length; volume++) {
            volumes[volume].addDiagonal(diagonalJPerK, dtSeconds, buoyancyFactors[volume]);
            volumes[volume].addModeCouplings(modePartners, modeOffDiagonal, dtSeconds, buoyancyFactors[volume]);
        }
        for (int contact = 0; contact < conductanceWPerK.length; contact++) {
            double conductance = activeConductanceWPerK[contact];
            for (int entry = contactOffsets[contact]; entry < contactOffsets[contact + 1]; entry++) {
                int index = contactIndices[entry];
                if (!fixedTemperature[index]) diagonalJPerK[index] += dtSeconds * conductance * contactWeights[entry] * contactWeights[entry];
                int partnerEntry = pairedContactEntries[entry];
                if (partnerEntry >= 0) modeOffDiagonal[index] += dtSeconds * conductance * contactWeights[entry] * contactWeights[partnerEntry];
            }
        }
    }

    /** Output J for input coefficients C. Only this entry clears the output vector. */
    void apply(double[] inputC, double[] outputJ) {
        if (mode == Mode.PROJECTION) {
            Arrays.fill(outputJ, 0, size(), 0);
            for (AirOperatorFragment volume : volumes) volume.addProjectionMass(inputC, outputJ);
            for (int index = airUnknownCount; index < size(); index++) outputJ[index] = massJPerK[index] * inputC[index];
            return;
        }
        if (mode == Mode.MASS) {
            applyMass(inputC, outputJ);
            for (int index = airUnknownCount; index < size(); index++) outputJ[index] = diagonalJPerK[index] * inputC[index];
            return;
        }
        for (int index = 0; index < size(); index++) {
            outputJ[index] = fixedTemperature[index] ? inputC[index] : massJPerK[index] * inputC[index];
        }
        for (int volume = 0; volume < volumes.length; volume++) {
            volumes[volume].addCrossMass(inputC, outputJ);
            volumes[volume].addConductance(inputC, outputJ, dtSeconds, buoyancyFactors[volume]);
        }
        for (int contact = 0; contact < activeConductanceWPerK.length; contact++) {
            double conductance = activeConductanceWPerK[contact];
            if (conductance == 0) continue;
            double differenceC = 0;
            int first = contactOffsets[contact], end = contactOffsets[contact + 1];
            // Material pairs dominate room contacts. Apply the same rank-one term without
            // two variable-length loops; keep arbitrary Air traces on the packed path below.
            if (end - first == 2) {
                int a = contactIndices[first], b = contactIndices[first + 1];
                double wa = contactWeights[first], wb = contactWeights[first + 1];
                if (!fixedTemperature[a]) differenceC += wa * inputC[a];
                if (!fixedTemperature[b]) differenceC += wb * inputC[b];
                double energyJ = dtSeconds * conductance * differenceC;
                if (!fixedTemperature[a]) outputJ[a] += wa * energyJ;
                if (!fixedTemperature[b]) outputJ[b] += wb * energyJ;
                continue;
            }
            for (int entry = first; entry < end; entry++) {
                int index = contactIndices[entry];
                if (!fixedTemperature[index]) differenceC += contactWeights[entry] * inputC[index];
            }
            double energyJ = dtSeconds * conductance * differenceC;
            for (int entry = first; entry < end; entry++) {
                int index = contactIndices[entry];
                if (!fixedTemperature[index]) outputJ[index] += contactWeights[entry] * energyJ;
            }
        }
    }

    void prepareProjection() {
        mode = Mode.PROJECTION;
        Arrays.fill(modeOffDiagonal, 0);
        Arrays.fill(diagonalJPerK, 0);
        for (AirOperatorFragment volume : volumes) {
            volume.addModeCouplings(modePartners, modeOffDiagonal, 0, 1);
            int entry = 0;
            for (int row = 0; row < volume.indices.length; row++) {
                diagonalJPerK[volume.indices[row]] += volume.projectionMassJPerK[entry];
                entry += volume.indices.length - row;
            }
        }
        for (int index = airUnknownCount; index < size(); index++) diagonalJPerK[index] = massJPerK[index];
    }

    void applyMass(double[] inputC, double[] outputJ) {
        for (int index = 0; index < size(); index++) outputJ[index] = massJPerK[index] * inputC[index];
        for (AirOperatorFragment volume : volumes) volume.addCrossMass(inputC, outputJ);
    }

    void prepareMass() {
        mode = Mode.MASS;
        Arrays.fill(modeOffDiagonal, 0);
        for (AirOperatorFragment volume : volumes) volume.addModeCouplings(modePartners, modeOffDiagonal, 0, 1);
        System.arraycopy(massJPerK, 0, diagonalJPerK, 0, size());
        for (int index = airUnknownCount; index < size(); index++) if (diagonalJPerK[index] == 0) diagonalJPerK[index] = 1;
    }

    /** Disjoint 2-mode Cholesky blocks; coarse and material unknowns use scalar Jacobi. */
    void precondition(double[] residualJ, double[] correctionC) {
        for (int index = 0; index < size(); index++) correctionC[index] = residualJ[index] / diagonalJPerK[index];
        for (int first = 0; first < airUnknownCount; first++) {
            int second = modePartners[first];
            if (second < first) continue;
            double l00 = Math.sqrt(diagonalJPerK[first]);
            double l10 = modeOffDiagonal[first] / l00;
            double remaining = diagonalJPerK[second] - l10 * l10;
            if (!(remaining > 0)) continue;
            double l11 = Math.sqrt(remaining);
            double y0 = residualJ[first] / l00;
            double y1 = (residualJ[second] - l10 * y0) / l11;
            correctionC[second] = y1 / l11;
            correctionC[first] = (y0 - l10 * correctionC[second]) / l00;
        }
    }

    void addKnownTemperaturesToRhs(double[] rhsJ) {
        for (int contact = 0; contact < activeConductanceWPerK.length; contact++) {
            double knownC = -boundaryOffsetC[contact];
            int first = contactOffsets[contact], end = contactOffsets[contact + 1];
            for (int entry = first; entry < end; entry++) {
                int index = contactIndices[entry];
                if (fixedTemperature[index]) knownC += contactWeights[entry] * fixedOffsetC[index];
            }
            double energyJ = dtSeconds * activeConductanceWPerK[contact] * knownC;
            for (int entry = first; entry < end; entry++) {
                int index = contactIndices[entry];
                if (!fixedTemperature[index]) rhsJ[index] -= contactWeights[entry] * energyJ;
            }
        }
        // These rows impose a temperature, not a fictitious material heat capacity.
        for (int index = 0; index < size(); index++) if (fixedTemperature[index]) rhsJ[index] = fixedOffsetC[index];
    }

    /** Only material rates are needed for H/branch trials; Air is advanced by the coupled solve. */
    void materialContactRates(double[] stateC, double[] ratesW) {
        Arrays.fill(ratesW, airUnknownCount, size(), 0);
        for (int contact = 0; contact < activeConductanceWPerK.length; contact++) {
            int firstBody = firstMaterialEntries[contact], secondBody = secondMaterialEntries[contact];
            if (firstBody < 0 && secondBody < 0 || activeConductanceWPerK[contact] == 0) continue;
            int first = contactOffsets[contact], end = contactOffsets[contact + 1];
            double differenceC = -boundaryOffsetC[contact];
            for (int entry = first; entry < end; entry++) differenceC += contactWeights[entry] * stateC[contactIndices[entry]];
            double powerW = activeConductanceWPerK[contact] * differenceC;
            if (firstBody >= 0) ratesW[contactIndices[firstBody]] -= contactWeights[firstBody] * powerW;
            if (secondBody >= 0 && secondBody != firstBody) ratesW[contactIndices[secondBody]] -= contactWeights[secondBody] * powerW;
        }
    }

    /** Compile-time builder; stable numerical steps read only the resulting primitive arrays. */
    static final class Contacts {
        final IntArrayList offsets = new IntArrayList(new int[]{0});
        final IntArrayList indices = new IntArrayList();
        final DoubleArrayList weights = new DoubleArrayList();
        final DoubleArrayList conductances = new DoubleArrayList();
        final IntArrayList firstMaterials = new IntArrayList();
        final IntArrayList secondMaterials = new IntArrayList();
        final IntArrayList boundaryPages = new IntArrayList();
        final ArrayList<AirRouteValidity> routes = new ArrayList<>();

        void term(int index, double weight) {
            if (weight == 0) return;
            int start = offsets.getInt(offsets.size() - 1);
            for (int entry = start; entry < indices.size(); entry++) {
                if (indices.getInt(entry) == index) {
                    weights.set(entry, weights.getDouble(entry) + weight);
                    return;
                }
            }
            indices.add(index);
            weights.add(weight);
        }

        void finish(double conductance, int firstBody, int secondBody, int boundaryPage, AirRouteValidity route) {
            offsets.add(indices.size());
            conductances.add(conductance);
            firstMaterials.add(firstBody);
            secondMaterials.add(secondBody);
            boundaryPages.add(boundaryPage);
            routes.add(route);
        }
    }
}
