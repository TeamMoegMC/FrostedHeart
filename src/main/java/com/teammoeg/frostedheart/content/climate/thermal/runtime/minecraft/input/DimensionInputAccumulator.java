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
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主线程尚未提交的下一份维度输入 cut。
 *
 * <p>同一 20-tick 窗口内的 Page、几何、source、环境和 phase ACK 在这里
 * 合并；{@link #seal(long)} 后所有权转移到不可变 {@link ThermalInputEnvelope}，
 * accumulator 随即复用自己的 builder 容量。</p>
 */
public final class DimensionInputAccumulator implements AutoCloseable {
    private final long dimensionGeneration;
    private final List<ThermalInputBatch.PageAdmission> admissions = new ArrayList<>();
    private final List<ThermalInputBatch.PageRetirement> retirements = new ArrayList<>();
    private final List<ThermalInputBatch.PageResidencyUpdate> residencyUpdates = new ArrayList<>();
    private final IdentityHashMap<ThermalPageHandle, EnvironmentBuilder> environments =
            new IdentityHashMap<>();
    private final ArrayDeque<EnvironmentBuilder> recycledEnvironments = new ArrayDeque<>();
    private final java.util.LinkedHashMap<ThermalInputBatch.PhaseAckIdentity, ThermalInputBatch.PhaseAck> phaseAcks = new java.util.LinkedHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<ThermalInputBatch.PhaseIntent>
            phaseIntents = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();

    public void requestPhase(
            long position, ThermalPageHandle page, int blockIndex, int stateId, byte branch) {
        var existing = phaseIntents.get(position);
        if (existing != null
                && existing.page() == page
                && existing.sourceStateId() == stateId
                && existing.branch() == branch) return;
        phaseIntents.put(
                position, new ThermalInputBatch.PhaseIntent(page, blockIndex, stateId, branch));
    }

    private final ResolvedGeometryBatch.Builder geometry;
    private final SpatialSourceTimeline spatialSources;
    private final ThermalMemoryBudget sourceBudget;

    public long latestSourceTick() { return spatialSources.latestTick(); }
    public boolean sourcesCapturedThrough(Long chunk, long tick) {
        return spatialSources.capturedThrough(chunk, tick);
    }
    public SpatialSourceTimeline.Capture captureSources(long tick) {
        return spatialSources.capture(tick);
    }
    public ThermalMemoryBudget.Reservation reservePageInputCredit() {
        return sourceBudget.reserveCompletion(512);
    }
    private long nextSequence;
    private long lastTargetTick;
    private double farFieldConductanceScale = Double.NaN;

    public boolean hasPendingRetirementInChunk(long chunkKey) {
        for (var retirement : retirements) {
            long section = retirement.page().sectionKey();
            if (net.minecraft.world.level.ChunkPos.asLong(net.minecraft.core.SectionPos.x(section),
                    net.minecraft.core.SectionPos.z(section)) == chunkKey) return true;
        }
        return false;
    }

    public DimensionInputAccumulator(long dimensionGeneration, long initialTargetTick,
            ThermalMemoryBudget sourceBudget, int maximumSources) {
        if (dimensionGeneration < 0L || initialTargetTick < 0L) {
            throw new IllegalArgumentException("input accumulator baseline is invalid");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.geometry = new ResolvedGeometryBatch.Builder();
        this.sourceBudget = sourceBudget;
        spatialSources = new SpatialSourceTimeline(sourceBudget, maximumSources);
        this.lastTargetTick = initialTargetTick;
    }

    void admit(
            ThermalPageHandle page,
            long geometryRevision,
            long residentBrickMask,
            long sourceSeedMask,
            PageSignatures signatures,
            double naturalTemperatureC,
            byte[] firstExposedLocalY,
            ThermalInputBatch.DormantAirCut dormantAir,
            ThermalInputBatch.DormantMaterialCut dormantMaterials) {
        admissions.add(
                new ThermalInputBatch.PageAdmission(
                        page,
                        geometryRevision,
                        residentBrickMask,
                        sourceSeedMask,
                        signatures,
                        naturalTemperatureC,
                        firstExposedLocalY,
                        dormantAir,
                        dormantMaterials));
    }

    void updateResidency(
            ThermalPageHandle page,
            long geometryRevision,
            long residentBrickMask,
            long sourceSeedMask,
            PageSignatures signatures,
            ThermalInputBatch.DormantAirCut dormantAir,
            ThermalInputBatch.DormantMaterialCut dormantMaterials) {
        for (int index = 0; index < residencyUpdates.size(); index++) {
            if (residencyUpdates.get(index).page() == page) {
                residencyUpdates.set(
                        index,
                        new ThermalInputBatch.PageResidencyUpdate(
                                page,
                                geometryRevision,
                                residentBrickMask,
                                sourceSeedMask,
                                signatures,
                                dormantAir != null ? dormantAir : residencyUpdates.get(index).dormantAir(),
                                dormantMaterials));
                return;
            }
        }
        residencyUpdates.add(
                new ThermalInputBatch.PageResidencyUpdate(
                        page,
                        geometryRevision,
                        residentBrickMask,
                        sourceSeedMask,
                        signatures,
                        dormantAir,
                        dormantMaterials));
    }

    boolean cancelAdmission(ThermalPageHandle page) {
        for (int index = 0; index < admissions.size(); index++) {
            if (admissions.get(index).page() == page) {
                admissions.remove(index);
                discardPageUpdates(page);
                return true;
            }
        }
        return false;
    }

    void retire(ThermalPageHandle page) {
        if (cancelAdmission(page)) {
            return;
        }
        retirements.add(new ThermalInputBatch.PageRetirement(page));
        discardPageUpdates(page);
    }

    private void discardPageUpdates(ThermalPageHandle page) {
        EnvironmentBuilder removed = environments.remove(page);
        if (removed != null) {
            recycle(removed);
        }
        residencyUpdates.removeIf(update -> update.page() == page);
    }

    ResolvedGeometryBatch.Builder geometry() {
        return geometry;
    }

    void updateNaturalTemperature(ThermalPageHandle page, double naturalTemperatureC) {
        environment(page).setNaturalTemperature(naturalTemperatureC);
    }

    void updateSkyColumn(ThermalPageHandle page, int column, int firstExposedLocalY) {
        environment(page).putSkyColumn(column, firstExposedLocalY);
    }

    public boolean registerSource(
            long sourceId,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean enabled,
            long effectiveTick,
            int anchorX,
            int anchorY,
            int anchorZ,
            int profileId,
            EmissionPort[] ports) {
        return spatialSources.register(
                new SpatialSourceTimeline.Descriptor(
                        sourceId, lifecycleGeneration, mode, anchorX, anchorY, anchorZ, profileId, ports), powerW, enabled, effectiveTick);
    }

    public void changeSourcePower(long sourceId, double powerW, long effectiveTick) {
        spatialSources.power(sourceId, powerW, effectiveTick);
    }

    public void changeSourceEnabled(long sourceId, boolean enabled, long effectiveTick) {
        spatialSources.enabled(sourceId, enabled, effectiveTick);
    }

    public void emitSourceImpulse(
            long sourceId, int portId, double signedEnergyJ, long effectiveTick) {
        spatialSources.impulse(sourceId, portId, signedEnergyJ, effectiveTick);
    }

    public void unloadSource(long sourceId, int lifecycleGeneration, long effectiveTick) {
        spatialSources.unload(sourceId, lifecycleGeneration, effectiveTick);
    }

    void acknowledgePhase(
            PhaseTransitionRuntime.Request request, PhaseTransitionRuntime.AckOutcome outcome) {
        var ack = new ThermalInputBatch.PhaseAck(request, outcome);
        phaseAcks.put(ack.identity(), ack);
    }

    void updateFarFieldConductanceScale(double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            throw new IllegalArgumentException("FarField scale must be finite and positive");
        }
        farFieldConductanceScale = scale;
    }

    public ThermalInputEnvelope seal(long targetTick) {
        targetTick = Math.max(targetTick, latestSourceTick());
        // Transfer immutable payloads before clearing mutable input queues. A retained
        // submission owns these arrays even while the worker queue is full.
        if (targetTick < lastTargetTick) {
            throw new IllegalArgumentException("thermal batch ticks must be monotonic");
        }
        ResolvedGeometryBatch geometryBatch = geometry.buildAndReset();
        nextSequence = Math.incrementExact(nextSequence);
        ThermalInputBatch batch =
                new ThermalInputBatch(
                        dimensionGeneration,
                        nextSequence,
                        targetTick,
                        admissions.isEmpty()
                                ? ThermalInputBatch.NO_ADMISSIONS
                                : admissions.toArray(ThermalInputBatch.PageAdmission[]::new),
                        retirements.isEmpty()
                                ? ThermalInputBatch.NO_RETIREMENTS
                                : retirements.toArray(ThermalInputBatch.PageRetirement[]::new),
                        residencyUpdates.isEmpty()
                                ? ThermalInputBatch.NO_RESIDENCY_UPDATES
                                : residencyUpdates.toArray(
                                        ThermalInputBatch.PageResidencyUpdate[]::new),
                        geometryBatch,
                        environmentBatch(),
                        phaseAcks.isEmpty()
                                ? ThermalInputBatch.NO_PHASE_ACKS
                                : phaseAcks.values().toArray(ThermalInputBatch.PhaseAck[]::new),
                        farFieldConductanceScale,
                        phaseIntents.values().toArray(ThermalInputBatch.PhaseIntent[]::new));
        admissions.clear();
        retirements.clear();
        residencyUpdates.clear();
        phaseAcks.clear();
        phaseIntents.clear();
        farFieldConductanceScale = Double.NaN;
        lastTargetTick = targetTick;
        return new ThermalInputEnvelope(batch, spatialSources.seal(targetTick));
    }

    @Override public void close() { spatialSources.close(); }

    private ThermalInputBatch.PageEnvironmentUpdate[] environmentBatch() {
        if (environments.isEmpty()) {
            return ThermalInputBatch.NO_ENVIRONMENT_UPDATES;
        }
        ThermalInputBatch.PageEnvironmentUpdate[] result =
                new ThermalInputBatch.PageEnvironmentUpdate[environments.size()];
        int index = 0;
        for (Map.Entry<ThermalPageHandle, EnvironmentBuilder> entry : environments.entrySet()) {
            EnvironmentBuilder builder = entry.getValue();
            result[index++] = builder.build(entry.getKey());
            recycle(builder);
        }
        environments.clear();
        return result;
    }

    private EnvironmentBuilder environment(ThermalPageHandle page) {
        EnvironmentBuilder builder = environments.get(page);
        if (builder != null) {
            return builder;
        }
        builder = recycledEnvironments.pollFirst();
        if (builder == null) {
            builder = new EnvironmentBuilder();
        }
        environments.put(page, builder);
        return builder;
    }

    private void recycle(EnvironmentBuilder builder) {
        builder.reset();
        recycledEnvironments.addLast(builder);
    }

    private static final class EnvironmentBuilder {
        private final byte[] sky = new byte[256];
        private final long[] dirtySky = new long[4];
        private double naturalTemperatureC;
        private boolean naturalTemperatureChanged;

        private void setNaturalTemperature(double value) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("natural temperature must be finite");
            }
            naturalTemperatureC = value;
            naturalTemperatureChanged = true;
        }

        private void putSkyColumn(int column, int firstExposedLocalY) {
            if (column < 0 || column >= 256 || firstExposedLocalY < 0 || firstExposedLocalY > 16) {
                throw new IllegalArgumentException("sky column is out of bounds");
            }
            sky[column] = (byte) firstExposedLocalY;
            dirtySky[column >>> 6] |= 1L << column;
        }

        private ThermalInputBatch.PageEnvironmentUpdate build(ThermalPageHandle page) {
            int count = 0;
            for (long word : dirtySky) {
                count += Long.bitCount(word);
            }
            short[] columns = new short[count];
            byte[] values = new byte[count];
            int write = 0;
            for (int wordIndex = 0; wordIndex < dirtySky.length; wordIndex++) {
                long remaining = dirtySky[wordIndex];
                while (remaining != 0L) {
                    int bit = Long.numberOfTrailingZeros(remaining);
                    int column = (wordIndex << 6) + bit;
                    columns[write] = (short) column;
                    values[write] = sky[column];
                    write++;
                    remaining &= remaining - 1L;
                }
            }
            return new ThermalInputBatch.PageEnvironmentUpdate(
                    page,
                    naturalTemperatureChanged,
                    naturalTemperatureChanged ? naturalTemperatureC : 0.0D,
                    columns,
                    values);
        }

        private void reset() {
            java.util.Arrays.fill(dirtySky, 0L);
            naturalTemperatureC = 0.0D;
            naturalTemperatureChanged = false;
        }
    }
}
