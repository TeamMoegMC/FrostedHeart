/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
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
 * 合并；{@link #seal(long)} 后所有权转移到不可变 {@link ThermalInputBatch}，
 * accumulator 随即复用自己的 builder 容量。</p>
 */
public final class DimensionInputAccumulator {
    private final long dimensionGeneration;
    private final List<ThermalInputBatch.PageAdmission> admissions =
            new ArrayList<>();
    private final List<ThermalInputBatch.PageRetirement> retirements =
            new ArrayList<>();
    private final IdentityHashMap<ThermalPageHandle, EnvironmentBuilder> environments =
            new IdentityHashMap<>();
    private final ArrayDeque<EnvironmentBuilder> recycledEnvironments =
            new ArrayDeque<>();
    private final List<ThermalInputBatch.PhaseAck> phaseAcks = new ArrayList<>();

    private final ResolvedGeometryBatch.Builder geometry;
    private final ThermalSourceBatch.Builder sourceEvents;
    private long nextSequence;
    private long lastTargetTick;
    private double farFieldConductanceScale = Double.NaN;

    public DimensionInputAccumulator(
            long dimensionGeneration,
            long initialTargetTick
    ) {
        if (dimensionGeneration < 0L || initialTargetTick < 0L) {
            throw new IllegalArgumentException("input accumulator baseline is invalid");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.geometry = new ResolvedGeometryBatch.Builder();
        this.sourceEvents = new ThermalSourceBatch.Builder(initialTargetTick);
        this.lastTargetTick = initialTargetTick;
    }

    void admit(
            ThermalPageHandle page,
            long geometryRevision,
            PageSignatures signatures,
            double naturalTemperatureC,
            byte[] firstExposedLocalY,
            ThermalInputBatch.DormantAirCut dormantAir
    ) {
        admissions.add(new ThermalInputBatch.PageAdmission(
                page,
                geometryRevision,
                signatures,
                naturalTemperatureC,
                firstExposedLocalY,
                dormantAir));
    }

    void retire(ThermalPageHandle page) {
        boolean pendingAdmission = false;
        for (int index = 0; index < admissions.size(); index++) {
            if (admissions.get(index).page() == page) {
                admissions.remove(index);
                pendingAdmission = true;
                break;
            }
        }
        if (!pendingAdmission) {
            retirements.add(new ThermalInputBatch.PageRetirement(page));
        }
        EnvironmentBuilder removed = environments.remove(page);
        if (removed != null) {
            recycle(removed);
        }
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

    void requeueEnvironment(ThermalInputBatch.PageEnvironmentUpdate update) {
        if (update.naturalTemperatureChanged()) {
            updateNaturalTemperature(
                    update.page(), update.naturalTemperatureC());
        }
        for (int index = 0; index < update.skyColumns().length; index++) {
            updateSkyColumn(
                    update.page(),
                    Short.toUnsignedInt(update.skyColumns()[index]),
                    Byte.toUnsignedInt(update.firstExposedLocalY()[index]));
        }
    }

    public void registerSource(
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
            EmissionPort[] ports
    ) {
        sourceEvents.addRegister(
                sourceId,
                lifecycleGeneration,
                mode,
                powerW,
                enabled,
                effectiveTick,
                anchorX,
                anchorY,
                anchorZ,
                profileId,
                ports);
    }

    public void changeSourcePower(long sourceId, double powerW, long effectiveTick) {
        sourceEvents.addPowerChange(sourceId, powerW, effectiveTick);
    }

    public void changeSourceEnabled(long sourceId, boolean enabled, long effectiveTick) {
        sourceEvents.addEnabledChange(sourceId, enabled, effectiveTick);
    }

    public void emitSourceImpulse(
            long sourceId,
            int portId,
            double signedEnergyJ,
            long effectiveTick
    ) {
        sourceEvents.addImpulse(sourceId, portId, signedEnergyJ, effectiveTick);
    }

    public void unloadSource(
            long sourceId,
            int lifecycleGeneration,
            long effectiveTick
    ) {
        sourceEvents.addUnload(sourceId, lifecycleGeneration, effectiveTick);
    }

    void acknowledgePhase(
            PhaseTransitionRuntime.Request request,
            PhaseTransitionRuntime.AckOutcome outcome
    ) {
        phaseAcks.add(new ThermalInputBatch.PhaseAck(request, outcome));
    }

    void updateFarFieldConductanceScale(double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            throw new IllegalArgumentException("FarField scale must be finite and positive");
        }
        farFieldConductanceScale = scale;
    }

    public ThermalInputBatch seal(long targetTick) {
        if (targetTick < lastTargetTick) {
            throw new IllegalArgumentException("thermal batch ticks must be monotonic");
        }
        ResolvedGeometryBatch geometryBatch = geometry.buildAndReset();
        ThermalSourceBatch sourceBatch = sourceEvents.buildAndReset();
        nextSequence = Math.incrementExact(nextSequence);
        ThermalInputBatch batch = new ThermalInputBatch(
                dimensionGeneration,
                nextSequence,
                targetTick,
                admissions.isEmpty()
                        ? ThermalInputBatch.NO_ADMISSIONS
                        : admissions.toArray(ThermalInputBatch.PageAdmission[]::new),
                retirements.isEmpty()
                        ? ThermalInputBatch.NO_RETIREMENTS
                        : retirements.toArray(ThermalInputBatch.PageRetirement[]::new),
                geometryBatch,
                sourceBatch,
                environmentBatch(),
                phaseAcks.isEmpty()
                        ? ThermalInputBatch.NO_PHASE_ACKS
                        : phaseAcks.toArray(ThermalInputBatch.PhaseAck[]::new),
                farFieldConductanceScale);
        admissions.clear();
        retirements.clear();
        phaseAcks.clear();
        farFieldConductanceScale = Double.NaN;
        lastTargetTick = targetTick;
        return batch;
    }

    private ThermalInputBatch.PageEnvironmentUpdate[] environmentBatch() {
        if (environments.isEmpty()) {
            return ThermalInputBatch.NO_ENVIRONMENT_UPDATES;
        }
        ThermalInputBatch.PageEnvironmentUpdate[] result =
                new ThermalInputBatch.PageEnvironmentUpdate[environments.size()];
        int index = 0;
        for (Map.Entry<ThermalPageHandle, EnvironmentBuilder> entry :
                environments.entrySet()) {
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
            if (column < 0 || column >= 256
                    || firstExposedLocalY < 0 || firstExposedLocalY > 16) {
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
