/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;

import java.util.Objects;

/**
 * 从 Minecraft 主线程转移给一个维度 worker 的不可变输入消息。
 *
 * <p>数组在构造后不得修改；batch 只描述一个确定的 target tick，不引用
 * Level、Chunk 或 BlockEntity 等可变 Minecraft 对象。</p>
 */
public final class ThermalInputBatch {
    public static final long CUT_INTERVAL_TICKS = 20L;
    public static final PageAdmission[] NO_ADMISSIONS = new PageAdmission[0];
    public static final PageRetirement[] NO_RETIREMENTS = new PageRetirement[0];
    public static final PageResidencyUpdate[] NO_RESIDENCY_UPDATES =
            new PageResidencyUpdate[0];
    public static final PageEnvironmentUpdate[] NO_ENVIRONMENT_UPDATES =
            new PageEnvironmentUpdate[0];
    public static final PhaseAck[] NO_PHASE_ACKS = new PhaseAck[0];

    private final long dimensionGeneration;
    private final long sequence;
    private final long targetTick;
    private final PageAdmission[] admissions;
    private final PageRetirement[] retirements;
    private final PageResidencyUpdate[] residencyUpdates;
    private final ResolvedGeometryBatch geometry;
    private final ThermalSourceBatch sourceEvents;
    private final PageEnvironmentUpdate[] environmentUpdates;
    private final PhaseAck[] phaseAcks;
    private final double farFieldConductanceScale;

    public ThermalInputBatch(
            long dimensionGeneration,
            long sequence,
            long targetTick,
            PageAdmission[] admissions,
            PageRetirement[] retirements,
            PageResidencyUpdate[] residencyUpdates,
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
        this.residencyUpdates = Objects.requireNonNull(
                residencyUpdates, "residencyUpdates");
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

    public long dimensionGeneration() {
        return dimensionGeneration;
    }

    public long sequence() {
        return sequence;
    }

    public long targetTick() {
        return targetTick;
    }

    public PageAdmission[] admissions() {
        return admissions;
    }

    public PageRetirement[] retirements() {
        return retirements;
    }

    public PageResidencyUpdate[] residencyUpdates() {
        return residencyUpdates;
    }

    public ResolvedGeometryBatch geometry() {
        return geometry;
    }

    public ThermalSourceBatch sourceEvents() {
        return sourceEvents;
    }

    public PageEnvironmentUpdate[] environmentUpdates() {
        return environmentUpdates;
    }

    public PhaseAck[] phaseAcks() {
        return phaseAcks;
    }

    public boolean hasFarFieldConductanceScale() {
        return !Double.isNaN(farFieldConductanceScale);
    }

    public double farFieldConductanceScale() {
        return farFieldConductanceScale;
    }

    public record PageAdmission(
            ThermalPageHandle page,
            long geometryRevision,
            long residentBrickMask,
            long sourceSeedMask,
            PageSignatures signatures,
            double naturalTemperatureC,
            byte[] firstExposedLocalY,
            DormantAirCut dormantAir
    ) {
        public PageAdmission {
            Objects.requireNonNull(page, "page");
            Objects.requireNonNull(signatures, "signatures");
            Objects.requireNonNull(firstExposedLocalY, "firstExposedLocalY");
            if (geometryRevision < 0L || residentBrickMask == 0L
                    || (sourceSeedMask & ~residentBrickMask) != 0L
                    || firstExposedLocalY.length != 16 * 16
                    || !Double.isFinite(naturalTemperatureC)) {
                throw new IllegalArgumentException("Page admission payload is invalid");
            }
        }
    }

    public record PageResidencyUpdate(
            ThermalPageHandle page,
            long geometryRevision,
            long residentBrickMask,
            long sourceSeedMask,
            PageSignatures signatures
    ) {
        public PageResidencyUpdate {
            Objects.requireNonNull(page, "page");
            Objects.requireNonNull(signatures, "signatures");
            if (geometryRevision < 0L || residentBrickMask == 0L
                    || (sourceSeedMask & ~residentBrickMask) != 0L) {
                throw new IllegalArgumentException(
                        "Page residency payload is invalid");
            }
        }
    }

    public record DormantAirCut(
            DormantChunkThermalState.SectionEntry entry,
            double naturalTemperatureC,
            double decayFactor
    ) {
        public DormantAirCut {
            Objects.requireNonNull(entry, "entry");
            if (!Double.isFinite(naturalTemperatureC)
                    || !Double.isFinite(decayFactor)
                    || decayFactor < 0.0D || decayFactor > 1.0D) {
                throw new IllegalArgumentException("dormant Air cut is invalid");
            }
        }

        public double meanTemperatureC(int brick) {
            return entry.meanTemperatureC(
                    brick, naturalTemperatureC, decayFactor);
        }

        public double componentTemperatureC(
                int brick,
                int component,
                int currentComponentCount
        ) {
            return entry.componentTemperatureC(
                    brick, component, currentComponentCount,
                    naturalTemperatureC, decayFactor);
        }

        public boolean hasBrick(int brick) {
            return entry.hasBrick(brick);
        }
    }

    public record PageRetirement(
            ThermalPageHandle page
    ) {
        public PageRetirement {
            Objects.requireNonNull(page, "page");
        }
    }

    public record PageEnvironmentUpdate(
            ThermalPageHandle page,
            boolean naturalTemperatureChanged,
            double naturalTemperatureC,
            short[] skyColumns,
            byte[] firstExposedLocalY
    ) {
        public PageEnvironmentUpdate {
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

    public record PhaseAck(
            PhaseTransitionRuntime.Request request,
            PhaseTransitionRuntime.AckOutcome outcome
    ) {
        public PhaseAck {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

}
