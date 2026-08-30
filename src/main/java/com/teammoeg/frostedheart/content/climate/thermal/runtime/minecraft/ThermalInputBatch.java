/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;

import java.util.Objects;

/** Immutable ownership transfer from Minecraft capture to a dimension worker. */
final class ThermalInputBatch {
    static final long CUT_INTERVAL_TICKS = 20L;
    static final PageAdmission[] NO_ADMISSIONS = new PageAdmission[0];
    static final PageRetirement[] NO_RETIREMENTS = new PageRetirement[0];
    static final PageEnvironmentUpdate[] NO_ENVIRONMENT_UPDATES =
            new PageEnvironmentUpdate[0];
    static final PhaseAck[] NO_PHASE_ACKS = new PhaseAck[0];

    private final long dimensionGeneration;
    private final long sequence;
    private final long targetTick;
    private final PageAdmission[] admissions;
    private final PageRetirement[] retirements;
    private final ResolvedGeometryBatch geometry;
    private final ThermalSourceBatch sourceEvents;
    private final PageEnvironmentUpdate[] environmentUpdates;
    private final PhaseAck[] phaseAcks;
    private final double farFieldConductanceScale;

    ThermalInputBatch(
            long dimensionGeneration,
            long sequence,
            long targetTick,
            PageAdmission[] admissions,
            PageRetirement[] retirements,
            ResolvedGeometryBatch geometry,
            ThermalSourceBatch sourceEvents,
            PageEnvironmentUpdate[] environmentUpdates,
            PhaseAck[] phaseAcks,
            double farFieldConductanceScale
    ) {
        if (dimensionGeneration < 0L || sequence <= 0L || targetTick < 0L) {
            throw new IllegalArgumentException("batch identity is invalid");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.sequence = sequence;
        this.targetTick = targetTick;
        this.admissions = Objects.requireNonNull(admissions, "admissions");
        this.retirements = Objects.requireNonNull(retirements, "retirements");
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        this.sourceEvents = Objects.requireNonNull(sourceEvents, "sourceEvents");
        this.environmentUpdates = Objects.requireNonNull(
                environmentUpdates, "environmentUpdates");
        this.phaseAcks = Objects.requireNonNull(phaseAcks, "phaseAcks");
        if (!Double.isNaN(farFieldConductanceScale)
                && (!Double.isFinite(farFieldConductanceScale)
                || farFieldConductanceScale <= 0.0D)) {
            throw new IllegalArgumentException(
                    "FarField conductance scale must be positive or absent");
        }
        this.farFieldConductanceScale = farFieldConductanceScale;
    }

    long dimensionGeneration() {
        return dimensionGeneration;
    }

    long sequence() {
        return sequence;
    }

    long targetTick() {
        return targetTick;
    }

    PageAdmission[] admissions() {
        return admissions;
    }

    PageRetirement[] retirements() {
        return retirements;
    }

    ResolvedGeometryBatch geometry() {
        return geometry;
    }

    ThermalSourceBatch sourceEvents() {
        return sourceEvents;
    }

    PageEnvironmentUpdate[] environmentUpdates() {
        return environmentUpdates;
    }

    PhaseAck[] phaseAcks() {
        return phaseAcks;
    }

    boolean hasFarFieldConductanceScale() {
        return !Double.isNaN(farFieldConductanceScale);
    }

    double farFieldConductanceScale() {
        return farFieldConductanceScale;
    }

    record PageAdmission(
            ThermalPageHandle page,
            long geometryRevision,
            PageSignatures signatures,
            double naturalTemperatureC,
            byte[] firstExposedLocalY
    ) {
        PageAdmission {
            Objects.requireNonNull(page, "page");
            Objects.requireNonNull(signatures, "signatures");
            Objects.requireNonNull(firstExposedLocalY, "firstExposedLocalY");
            if (geometryRevision < 0L
                    || firstExposedLocalY.length != 16 * 16
                    || !Double.isFinite(naturalTemperatureC)) {
                throw new IllegalArgumentException("Page admission payload is invalid");
            }
        }
    }

    record PageRetirement(
            ThermalPageHandle page
    ) {
        PageRetirement {
            Objects.requireNonNull(page, "page");
        }
    }

    record PageEnvironmentUpdate(
            ThermalPageHandle page,
            boolean naturalTemperatureChanged,
            double naturalTemperatureC,
            short[] skyColumns,
            byte[] firstExposedLocalY
    ) {
        PageEnvironmentUpdate {
            Objects.requireNonNull(page, "page");
            Objects.requireNonNull(skyColumns, "skyColumns");
            Objects.requireNonNull(firstExposedLocalY, "firstExposedLocalY");
            if (skyColumns.length != firstExposedLocalY.length
                    || !Double.isFinite(naturalTemperatureC)) {
                throw new IllegalArgumentException("Page environment payload is invalid");
            }
            for (int index = 0; index < skyColumns.length; index++) {
                int column = Short.toUnsignedInt(skyColumns[index]);
                int exposedY = Byte.toUnsignedInt(firstExposedLocalY[index]);
                if (column >= 256 || exposedY > 16) {
                    throw new IllegalArgumentException(
                            "Page sky-column update is out of bounds");
                }
            }
        }
    }

    record PhaseAck(
            PhaseTransitionRuntime.Request request,
            PhaseTransitionRuntime.AckOutcome outcome
    ) {
        PhaseAck {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

}
