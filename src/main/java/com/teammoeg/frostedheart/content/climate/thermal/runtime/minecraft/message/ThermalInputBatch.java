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
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;

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
    public static final PhaseIntent[] NO_PHASE_INTENTS = new PhaseIntent[0];
    public record PhaseIntent(ThermalPageHandle page, int blockIndex, int sourceStateId, byte branch) {}

    private final long dimensionGeneration;
    private final long sequence;
    private final long targetTick;
    private final PageAdmission[] admissions;
    private final PageRetirement[] retirements;
    private final PageResidencyUpdate[] residencyUpdates;
    private final ResolvedGeometryBatch geometry;
    private final PageEnvironmentUpdate[] environmentUpdates;
    private final PhaseAck[] phaseAcks;
    private final double farFieldConductanceScale;
    private final PhaseIntent[] phaseIntents;
    public ThermalInputBatch(
            long dimensionGeneration,
            long sequence,
            long targetTick,
            PageAdmission[] admissions,
            PageRetirement[] retirements,
            PageResidencyUpdate[] residencyUpdates,
            ResolvedGeometryBatch geometry,
            PageEnvironmentUpdate[] environmentUpdates,
            PhaseAck[] phaseAcks,
            double farFieldConductanceScale
    ) {
        this(dimensionGeneration, sequence, targetTick, admissions, retirements, residencyUpdates,
                geometry, environmentUpdates, phaseAcks, farFieldConductanceScale, NO_PHASE_INTENTS);
    }

    public ThermalInputBatch(long dimensionGeneration, long sequence, long targetTick,
            PageAdmission[] admissions, PageRetirement[] retirements, PageResidencyUpdate[] residencyUpdates,
            ResolvedGeometryBatch geometry, PageEnvironmentUpdate[] environmentUpdates,
            PhaseAck[] phaseAcks, double farFieldConductanceScale, PhaseIntent[] phaseIntents) {
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
        this.phaseIntents = phaseIntents;
    }
    public PhaseIntent[] phaseIntents() { return phaseIntents; }

    /** Conservative retained payload charge for the bounded input queue, including restored state. */
    public long queuedPayloadBytes() {
        long bytes = 1024L + 64L * geometry.size() + 48L * geometry.materialChanges().size()
                + 96L * retirements.length + 18500L * (admissions.length + residencyUpdates.length)
                + 128L * (phaseAcks.length + phaseIntents.length);
        for (int index = 0; index < geometry.size(); index++)
            if (geometry.fullPageSignatures(index) != null) bytes += 18000L;
        for (var halo : geometry.halos()) bytes += 128L + 24L * halo.halo().size();
        for (var environment : environmentUpdates) bytes += 128L + 3L * environment.skyColumns().length;
        AirBackingMeter restored = null;
        for (var admission : admissions)
            restored = recordRestoredBacking(restored, admission.dormantAir(), admission.dormantMaterials());
        for (var update : residencyUpdates)
            restored = recordRestoredBacking(restored, update.dormantAir(), update.dormantMaterials());
        return bytes + (restored == null ? 0L : restored.bytes());
    }

    private static AirBackingMeter recordRestoredBacking(AirBackingMeter meter, DormantAirCut air, DormantMaterialCut material) {
        if (air == null && material == null) return meter;
        if (meter == null) meter = new AirBackingMeter();
        if (air != null) air.entry().recordBacking(meter);
        if (material != null && material.state() != null) material.state().recordBacking(meter);
        return meter;
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
            DormantAirCut dormantAir,
            DormantMaterialCut dormantMaterials
    ) {
        public PageAdmission(ThermalPageHandle page, long geometryRevision, long residentBrickMask,
                long sourceSeedMask, PageSignatures signatures, double naturalTemperatureC,
                byte[] firstExposedLocalY, DormantAirCut dormantAir) {
            this(page, geometryRevision, residentBrickMask, sourceSeedMask, signatures,
                    naturalTemperatureC, firstExposedLocalY, dormantAir, null);
        }
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
            PageSignatures signatures,
            DormantAirCut dormantAir,
            DormantMaterialCut dormantMaterials
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

    public record DormantMaterialCut(MaterialSectionState state, long tick, double naturalC, double coolingRate) {}

    public record DormantAirCut(
            DormantChunkThermalState.SectionEntry entry,
            double naturalTemperatureC,
            double decayFactor,
            long targetTick,
            double coolingRate
    ) {
        public DormantAirCut(DormantChunkThermalState.SectionEntry entry, double naturalTemperatureC, double decayFactor) {
            this(entry, naturalTemperatureC, decayFactor, -1, 0);
        }
        public DormantAirCut {
            Objects.requireNonNull(entry, "entry");
            if (!Double.isFinite(naturalTemperatureC)
                    || !Double.isFinite(decayFactor)
                    || decayFactor < 0.0D || decayFactor > 1.0D) {
                throw new IllegalArgumentException("dormant Air cut is invalid");
            }
        }

        public double meanTemperatureC(int brick) {
            if (targetTick >= 0 && entry.spatialAir() != null) return entry.spatialAir().meanC(
                    brick, naturalTemperatureC, targetTick, coolingRate, entry.savedGameTick());
            return entry.meanTemperatureC(
                    brick, naturalTemperatureC, decayFactor);
        }

        public double temperatureC(double x, double y, double z) {
            if (entry.spatialAir() != null) {
                if (targetTick >= 0) return entry.spatialAir().sampleC(x, y, z,
                        naturalTemperatureC, targetTick, coolingRate, entry.savedGameTick());
                double saved = entry.spatialAir().sampleC(x, y, z);
                return Double.isFinite(saved)
                        ? naturalTemperatureC + (saved - naturalTemperatureC) * decayFactor : Double.NaN;
            }
            int brick = (((int) Math.floor(x) & 15) >>> 2)
                    | (((int) Math.floor(z) & 15) >>> 2) << 2 | (((int) Math.floor(y) & 15) >>> 2) << 4;
            return hasBrick(brick) ? meanTemperatureC(brick) : Double.NaN;
        }

        public void fillBlockTemperatures(int brick,double[] target) {
            if (targetTick >= 0 && entry.spatialAir() != null) {
                for (int block = 0; block < 64; block++) {
                    double value = entry.spatialAir().blockCenterC(brick, block,
                            naturalTemperatureC, targetTick, coolingRate, entry.savedGameTick());
                    target[block] = Double.isFinite(value) ? value : naturalTemperatureC;
                }
                return;
            }
            entry.fillBlockTemperatures(brick,naturalTemperatureC,decayFactor,target);
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

    public record PhaseAckIdentity(long incarnation, long requestSequence) { }
    public record PhaseAck(
            PhaseTransitionRuntime.Request request,
            PhaseTransitionRuntime.AckOutcome outcome
    ) {
        public PhaseAck {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(outcome, "outcome");
        }
        public PhaseAckIdentity identity() { return new PhaseAckIdentity(request.incarnation(), request.requestSequence()); }
    }

}
