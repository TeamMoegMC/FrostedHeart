/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.MutableVisualWeatherSample;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainDescriptor;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainFieldModel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Lifecycle-owned client reconstruction of sparse white-curtain snapshots.
 *
 * <pre>
 * snapshot -> prepared kernels -> tick candidate prefilter -> previous/current primitive grids
 * render consumers sample the grids and never walk descriptors
 * </pre>
 */
public final class ClientWeatherState {
    private static final WhiteCurtainFieldModel.VisualKernel[] NO_KERNELS = new WhiteCurtainFieldModel.VisualKernel[0];
    private static final int[] NO_INDICES = new int[0];
    private static final int MAX_GRID_SIDE = WeatherQualityProfile.FANCY.gridSide();
    private static final int MAX_GRID_CELLS = MAX_GRID_SIDE * MAX_GRID_SIDE;
    private static final double MAX_CLOCK_CORRECTION_PER_TICK = 0.10;
    private static final double IMMEDIATE_CLOCK_CORRECTION_SECONDS = 5.0;
    private static final long MAX_PACKET_TRANSIT_DAY_TICKS = 20L;
    private static final double WALL_PREFILTER_BLOCKS = 512.0;
    public static final ClientWeatherState INSTANCE = new ClientWeatherState();

    private ResourceKey<Level> dimension;
    private List<WhiteCurtainDescriptor> descriptors = List.of();
    private WhiteCurtainFieldModel.VisualKernel[] kernels = NO_KERNELS;
    private int[] nearCandidateIndices = NO_INDICES;
    private int[] wallCandidateIndices = NO_INDICES;
    private double[] wallCandidateDistances = new double[0];
    private int nearCandidateCount;
    private int wallCandidateCount;
    private long snapshotGeneration;

    private boolean clockInitialized;
    private long lastLevelDayTime;
    private double frameClockAdvance;
    private double correctionRemaining;
    private double tickClimateSeconds;

    private Grid previousGrid = new Grid();
    private Grid currentGrid = new Grid();
    private boolean hasGrid;
    private WeatherQualityProfile activeProfile = WeatherQualityProfile.FAST;
    private float cameraExposure = 1.0F;
    private final MutableVisualWeatherSample scratch = new MutableVisualWeatherSample();
    private final MutableVisualWeatherSample tickCameraSample = new MutableVisualWeatherSample();
    private boolean tickOwnsPrecipitation;
    private PendingSnapshot pendingSnapshot;

    private long descriptorChecks;
    private long fieldEvaluations;
    private long gridRebuilds;

    private ClientWeatherState() {
    }

    public boolean receiveSnapshot(ResourceKey<Level> snapshotDimension, long climateSeconds,
                                   long serverClockDayTime, List<WhiteCurtainDescriptor> snapshot,
                                   @Nullable ResourceKey<Level> loadedDimension, long levelDayTime) {
        List<WhiteCurtainDescriptor> immutable = List.copyOf(snapshot);
        if (loadedDimension == null) {
            pendingSnapshot = new PendingSnapshot(
                    snapshotDimension, climateSeconds, serverClockDayTime, immutable);
            return false;
        }
        if (!snapshotDimension.equals(loadedDimension)) {
            return false;
        }
        replaceSnapshot(snapshotDimension, climateSeconds, serverClockDayTime, immutable, levelDayTime);
        return true;
    }

    public void replaceSnapshot(ResourceKey<Level> snapshotDimension, long climateSeconds,
                                long serverClockDayTime, List<WhiteCurtainDescriptor> snapshot,
                                long levelDayTime) {
        List<WhiteCurtainDescriptor> immutable = List.copyOf(snapshot);
        WhiteCurtainFieldModel.VisualKernel[] prepared = new WhiteCurtainFieldModel.VisualKernel[immutable.size()];
        for (int i = 0; i < prepared.length; i++) {
            prepared[i] = WhiteCurtainFieldModel.prepareVisual(immutable.get(i));
        }
        dimension = snapshotDimension;
        descriptors = immutable;
        kernels = prepared;
        nearCandidateIndices = new int[prepared.length];
        wallCandidateIndices = new int[prepared.length];
        wallCandidateDistances = new double[prepared.length];
        nearCandidateCount = 0;
        wallCandidateCount = 0;
        snapshotGeneration++;
        hasGrid = false;
        pendingSnapshot = null;
        setClockImmediate(authoritativeSecondsAt(
                climateSeconds, serverClockDayTime, levelDayTime), levelDayTime);
    }

    public void correctClock(long serverClimateSeconds, long serverClockDayTime, long levelDayTime) {
        double authoritativeSeconds = authoritativeSecondsAt(
                serverClimateSeconds, serverClockDayTime, levelDayTime);
        if (!clockInitialized) {
            setClockImmediate(authoritativeSeconds, levelDayTime);
            return;
        }
        advanceClock(levelDayTime);
        double error = authoritativeSeconds - tickClimateSeconds;
        if (Math.abs(error) >= IMMEDIATE_CLOCK_CORRECTION_SECONDS) {
            setClockImmediate(authoritativeSeconds, levelDayTime);
        } else {
            correctionRemaining = error;
        }
    }

    private void setClockImmediate(double climateSeconds, long levelDayTime) {
        clockInitialized = true;
        lastLevelDayTime = levelDayTime;
        frameClockAdvance = 0.0;
        correctionRemaining = 0.0;
        tickClimateSeconds = climateSeconds;
    }

    public void tick(ResourceKey<Level> loadedDimension, long levelDayTime, double cameraX, double cameraZ,
                     ClimateType globalClimate, int globalWind, WeatherQualityProfile profile) {
        tick(loadedDimension, levelDayTime, cameraX, cameraZ, globalClimate, globalWind, profile, true);
    }

    public void tick(ResourceKey<Level> loadedDimension, long levelDayTime, double cameraX, double cameraZ,
                     ClimateType globalClimate, int globalWind, WeatherQualityProfile profile,
                     boolean cameraExposedToSky) {
        tickClock(loadedDimension, levelDayTime);
        cameraExposure = approach(cameraExposure, cameraExposedToSky ? 1.0F : 0.0F, 0.15F);
        activeProfile = profile;
        if (kernels.length == 0) {
            nearCandidateCount = 0;
            wallCandidateCount = 0;
            disableSpatialTick();
            return;
        }
        prefilter(cameraX, cameraZ, profile);
        if (nearCandidateCount == 0 && wallCandidateCount == 0) {
            disableSpatialTick();
            return;
        }
        fillGrid(cameraX, cameraZ, globalClimate == null ? ClimateType.NONE : globalClimate,
                globalWind, profile);
        currentGrid.sample(cameraX, cameraZ, tickCameraSample);
        applyCameraExposure(tickCameraSample);
        tickOwnsPrecipitation = hasPrecipitationFootprint();
    }

    /** Advances the analytic clock in every rendering mode without evaluating the weather field. */
    public void tickClock(ResourceKey<Level> loadedDimension, long levelDayTime) {
        if (pendingSnapshot != null && pendingSnapshot.dimension.equals(loadedDimension)) {
            PendingSnapshot pending = pendingSnapshot;
            replaceSnapshot(pending.dimension, pending.climateSeconds, pending.serverClockDayTime,
                    pending.descriptors, levelDayTime);
        } else if (dimension != null && !dimension.equals(loadedDimension)) {
            resetForDimension(loadedDimension);
        } else if (dimension == null) {
            dimension = loadedDimension;
        }

        if (clockInitialized) {
            advanceClock(levelDayTime);
            double correction = Math.max(-MAX_CLOCK_CORRECTION_PER_TICK,
                    Math.min(MAX_CLOCK_CORRECTION_PER_TICK, correctionRemaining));
            if (correction != 0.0) {
                tickClimateSeconds += correction;
                correctionRemaining -= correction;
            }
        }
    }

    private void resetForDimension(ResourceKey<Level> loadedDimension) {
        dimension = loadedDimension;
        descriptors = List.of();
        kernels = NO_KERNELS;
        nearCandidateIndices = NO_INDICES;
        wallCandidateIndices = NO_INDICES;
        wallCandidateDistances = new double[0];
        nearCandidateCount = 0;
        wallCandidateCount = 0;
        hasGrid = false;
        clockInitialized = false;
        frameClockAdvance = 0.0;
        correctionRemaining = 0.0;
        cameraExposure = 1.0F;
        tickCameraSample.clear();
        tickOwnsPrecipitation = false;
        snapshotGeneration++;
    }

    public void reset() {
        dimension = null;
        descriptors = List.of();
        kernels = NO_KERNELS;
        nearCandidateIndices = NO_INDICES;
        wallCandidateIndices = NO_INDICES;
        wallCandidateDistances = new double[0];
        nearCandidateCount = 0;
        wallCandidateCount = 0;
        hasGrid = false;
        clockInitialized = false;
        frameClockAdvance = 0.0;
        correctionRemaining = 0.0;
        cameraExposure = 1.0F;
        tickCameraSample.clear();
        tickOwnsPrecipitation = false;
        pendingSnapshot = null;
        snapshotGeneration++;
    }

    private void advanceClock(long levelDayTime) {
        long rawDelta = levelDayTime - lastLevelDayTime;
        if (rawDelta >= 0L && rawDelta <= 20L) {
            tickClimateSeconds += rawDelta / 20.0;
        }
        frameClockAdvance = rawDelta == 1L ? 1.0 / 20.0 : 0.0;
        lastLevelDayTime = levelDayTime;
    }

    private static double authoritativeSecondsAt(long serverClimateSeconds, long serverClockDayTime,
                                                 long levelDayTime) {
        long transitTicks = levelDayTime - serverClockDayTime;
        if (transitTicks < 0L || transitTicks > MAX_PACKET_TRANSIT_DAY_TICKS) {
            return serverClimateSeconds;
        }
        return serverClimateSeconds + transitTicks / 20.0;
    }

    public double frameClimateSeconds(float partialTick) {
        return tickClimateSeconds + partialTick * frameClockAdvance;
    }

    private void prefilter(double cameraX, double cameraZ, WeatherQualityProfile profile) {
        nearCandidateCount = 0;
        wallCandidateCount = 0;
        double nearRadius = profile.gridRadius() * profile.gridSpacingBlocks()
                + profile.fieldProfile().corridorEdgeFadeBlocks();
        for (int i = 0; i < kernels.length; i++) {
            WhiteCurtainFieldModel.VisualKernel kernel = kernels[i];
            descriptorChecks++;
            double dx = axisDistance(cameraX, kernel.minBlockX, kernel.maxBlockXExclusive);
            double dz = axisDistance(cameraZ, kernel.minBlockZ, kernel.maxBlockZExclusive);
            double distanceSquared = dx * dx + dz * dz;
            double wallDistanceSquared = activeFrontDistanceSquared(kernel, cameraX, cameraZ);
            if (wallDistanceSquared <= WALL_PREFILTER_BLOCKS * WALL_PREFILTER_BLOCKS) {
                insertWallCandidate(i, wallDistanceSquared);
            }
            if (distanceSquared <= nearRadius * nearRadius) {
                nearCandidateIndices[nearCandidateCount++] = i;
            }
        }
    }

    private void insertWallCandidate(int kernelIndex, double distanceSquared) {
        int insertAt = wallCandidateCount;
        while (insertAt > 0 && wallCandidateDistances[insertAt - 1] > distanceSquared) {
            wallCandidateDistances[insertAt] = wallCandidateDistances[insertAt - 1];
            wallCandidateIndices[insertAt] = wallCandidateIndices[insertAt - 1];
            insertAt--;
        }
        wallCandidateDistances[insertAt] = distanceSquared;
        wallCandidateIndices[insertAt] = kernelIndex;
        wallCandidateCount++;
    }

    private double activeFrontDistanceSquared(WhiteCurtainFieldModel.VisualKernel kernel,
                                              double cameraX, double cameraZ) {
        double leadingDelta = kernel.leadingSnowDeltaChunks(tickClimateSeconds);
        if (leadingDelta < -3.0 || leadingDelta > kernel.maxDeltaChunks + 3.0) {
            return Double.POSITIVE_INFINITY;
        }
        if (kernel.moveX != 0.0F) {
            double frontX = kernel.moveX > 0.0F
                    ? (kernel.minChunkX + leadingDelta) * 16.0 + 8.0
                    : (kernel.maxChunkX - leadingDelta) * 16.0 + 8.0;
            double dx = cameraX - frontX;
            double dz = axisDistance(cameraZ, kernel.minBlockZ, kernel.maxBlockZExclusive);
            return dx * dx + dz * dz;
        }
        double frontZ = kernel.moveZ > 0.0F
                ? (kernel.minChunkZ + leadingDelta) * 16.0 + 8.0
                : (kernel.maxChunkZ - leadingDelta) * 16.0 + 8.0;
        double dx = axisDistance(cameraX, kernel.minBlockX, kernel.maxBlockXExclusive);
        double dz = cameraZ - frontZ;
        return dx * dx + dz * dz;
    }

    private static double axisDistance(double coordinate, double min, double maxExclusive) {
        if (coordinate < min) {
            return min - coordinate;
        }
        if (coordinate >= maxExclusive) {
            return coordinate - maxExclusive;
        }
        return 0.0;
    }

    private void fillGrid(double cameraX, double cameraZ, ClimateType globalClimate, int globalWind,
                          WeatherQualityProfile profile) {
        Grid target = previousGrid;
        previousGrid = currentGrid;
        currentGrid = target;
        int spacing = profile.gridSpacingBlocks();
        int side = profile.gridSide();
        double originX = Math.floor(cameraX / spacing) * spacing - profile.gridRadius() * spacing;
        double originZ = Math.floor(cameraZ / spacing) * spacing - profile.gridRadius() * spacing;
        currentGrid.configure(originX, originZ, spacing, side);
        currentGrid.hasSpatialPrecipitation = false;

        float baseSnow = globalClimate.isSnowyOrBlizzard() ? 0.75F : 0.0F;
        float baseWhiteout = globalClimate.isBlizzard() ? 0.65F : 0.0F;
        float baseWind = Math.max(0.0F, Math.min(1.0F, globalWind / 100.0F));
        float baseVisibility = lerp(baseWhiteout, 512.0F, profile.fieldProfile().minimumVisibilityBlocks());
        int cell = 0;
        for (int z = 0; z < side; z++) {
            double blockZ = originZ + z * spacing;
            for (int x = 0; x < side; x++, cell++) {
                double blockX = originX + x * spacing;
                float snow = baseSnow;
                float whiteout = baseWhiteout;
                float wind = baseWind;
                float windX = 0.0F;
                float windZ = 0.0F;
                float visibility = baseVisibility;
                for (int candidate = 0; candidate < nearCandidateCount; candidate++) {
                    WhiteCurtainFieldModel.sampleVisual(
                            kernels[nearCandidateIndices[candidate]], tickClimateSeconds,
                            blockX, blockZ, profile.fieldProfile(), scratch);
                    fieldEvaluations++;
                    if (scratch.snowIntensity > 0.01F || scratch.whiteoutIntensity > 0.01F) {
                        currentGrid.hasSpatialPrecipitation = true;
                    }
                    snow = Math.max(snow, scratch.snowIntensity);
                    whiteout = Math.max(whiteout, scratch.whiteoutIntensity);
                    visibility = Math.min(visibility, scratch.visibilityBlocks);
                    if (scratch.windIntensity > wind) {
                        wind = scratch.windIntensity;
                        windX = scratch.windX;
                        windZ = scratch.windZ;
                    }
                }
                currentGrid.snow[cell] = snow;
                currentGrid.whiteout[cell] = whiteout;
                currentGrid.wind[cell] = wind;
                currentGrid.windX[cell] = windX;
                currentGrid.windZ[cell] = windZ;
                currentGrid.visibility[cell] = visibility;
            }
        }
        currentGrid.valid = true;
        gridRebuilds++;
        if (!hasGrid || !previousGrid.valid) {
            previousGrid.copyFrom(currentGrid);
            hasGrid = true;
        }
    }

    public void sampleGrid(double blockX, double blockZ, float partialTick, MutableVisualWeatherSample out) {
        out.clear();
        if (!hasGrid) {
            return;
        }
        currentGrid.sample(blockX, blockZ, scratch);
        float currentSnow = scratch.snowIntensity;
        float currentWhiteout = scratch.whiteoutIntensity;
        float currentWind = scratch.windIntensity;
        float currentWindX = scratch.windX;
        float currentWindZ = scratch.windZ;
        float currentVisibility = scratch.visibilityBlocks;
        previousGrid.sample(blockX, blockZ, out);
        float alpha = Math.max(0.0F, Math.min(1.0F, partialTick));
        out.insideAffectedCorridor = currentSnow > 0.0F || currentWhiteout > 0.0F;
        out.snowIntensity = lerp(alpha, out.snowIntensity, currentSnow);
        out.whiteoutIntensity = lerp(alpha, out.whiteoutIntensity, currentWhiteout);
        out.windIntensity = lerp(alpha, out.windIntensity, currentWind);
        out.windX = lerp(alpha, out.windX, currentWindX);
        out.windZ = lerp(alpha, out.windZ, currentWindZ);
        out.visibilityBlocks = lerp(alpha, out.visibilityBlocks, currentVisibility);
    }

    public void samplePrecipitation(double blockX, double blockZ, float partialTick,
                                    MutableVisualWeatherSample out) {
        out.clear();
        if (!hasGrid) {
            return;
        }
        currentGrid.samplePrecipitation(blockX, blockZ, scratch);
        float currentSnow = scratch.snowIntensity;
        float currentWhiteout = scratch.whiteoutIntensity;
        float currentWind = scratch.windIntensity;
        float currentWindX = scratch.windX;
        float currentWindZ = scratch.windZ;
        previousGrid.samplePrecipitation(blockX, blockZ, out);
        float alpha = Math.max(0.0F, Math.min(1.0F, partialTick));
        out.snowIntensity = lerp(alpha, out.snowIntensity, currentSnow);
        out.whiteoutIntensity = lerp(alpha, out.whiteoutIntensity, currentWhiteout);
        out.windIntensity = lerp(alpha, out.windIntensity, currentWind);
        out.windX = lerp(alpha, out.windX, currentWindX);
        out.windZ = lerp(alpha, out.windZ, currentWindZ);
    }

    public void disableSpatialTick() {
        tickCameraSample.clear();
        tickOwnsPrecipitation = false;
        hasGrid = false;
    }

    public boolean tickOwnsPrecipitation() {
        return tickOwnsPrecipitation;
    }

    public MutableVisualWeatherSample tickCameraSample() {
        return tickCameraSample;
    }

    void applyCameraExposure(MutableVisualWeatherSample sample) {
        float exposure = cameraExposure;
        sample.snowIntensity *= exposure;
        sample.whiteoutIntensity *= exposure;
        sample.windIntensity *= exposure;
        if (Float.isFinite(sample.visibilityBlocks)) {
            sample.visibilityBlocks = 512.0F + exposure * (sample.visibilityBlocks - 512.0F);
        }
        if (exposure <= 0.0F) {
            sample.insideAffectedCorridor = false;
        }
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public List<WhiteCurtainDescriptor> descriptors() {
        return descriptors;
    }

    public long snapshotGeneration() {
        return snapshotGeneration;
    }

    public int nearCandidateCount() {
        return nearCandidateCount;
    }

    public int wallCandidateCount() {
        return wallCandidateCount;
    }

    public WhiteCurtainFieldModel.VisualKernel wallKernel(int visibleIndex) {
        return kernels[wallCandidateIndices[visibleIndex]];
    }

    public double wallCandidateDistanceSquared(int visibleIndex) {
        return wallCandidateDistances[visibleIndex];
    }

    public WeatherQualityProfile activeProfile() {
        return activeProfile;
    }

    public boolean hasGrid() {
        return hasGrid;
    }

    /** Ownership follows descriptor-produced precipitation, never global weather or indoor camera fade. */
    public boolean hasPrecipitationFootprint() {
        return hasGrid && (currentGrid.hasSpatialPrecipitation || previousGrid.hasSpatialPrecipitation);
    }

    public float cameraExposure() {
        return cameraExposure;
    }

    public long descriptorChecks() {
        return descriptorChecks;
    }

    public long fieldEvaluations() {
        return fieldEvaluations;
    }

    public long gridRebuilds() {
        return gridRebuilds;
    }

    Object currentSnowBacking() {
        return currentGrid.snow;
    }

    Object previousSnowBacking() {
        return previousGrid.snow;
    }

    double correctionRemaining() {
        return correctionRemaining;
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    private static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }

    private static final class Grid {
        private final float[] snow = new float[MAX_GRID_CELLS];
        private final float[] whiteout = new float[MAX_GRID_CELLS];
        private final float[] wind = new float[MAX_GRID_CELLS];
        private final float[] windX = new float[MAX_GRID_CELLS];
        private final float[] windZ = new float[MAX_GRID_CELLS];
        private final float[] visibility = new float[MAX_GRID_CELLS];
        private double originX;
        private double originZ;
        private int spacing;
        private int side;
        private boolean valid;
        private boolean hasSpatialPrecipitation;

        private void configure(double originX, double originZ, int spacing, int side) {
            this.originX = originX;
            this.originZ = originZ;
            this.spacing = spacing;
            this.side = side;
        }

        private void copyFrom(Grid other) {
            configure(other.originX, other.originZ, other.spacing, other.side);
            int length = other.side * other.side;
            System.arraycopy(other.snow, 0, snow, 0, length);
            System.arraycopy(other.whiteout, 0, whiteout, 0, length);
            System.arraycopy(other.wind, 0, wind, 0, length);
            System.arraycopy(other.windX, 0, windX, 0, length);
            System.arraycopy(other.windZ, 0, windZ, 0, length);
            System.arraycopy(other.visibility, 0, visibility, 0, length);
            hasSpatialPrecipitation = other.hasSpatialPrecipitation;
            valid = other.valid;
        }

        private void sample(double blockX, double blockZ, MutableVisualWeatherSample out) {
            if (!valid || side <= 0) {
                out.clear();
                return;
            }
            double gridX = (blockX - originX) / spacing;
            double gridZ = (blockZ - originZ) / spacing;
            int x0 = clamp((int) Math.floor(gridX), 0, side - 1);
            int z0 = clamp((int) Math.floor(gridZ), 0, side - 1);
            int x1 = Math.min(x0 + 1, side - 1);
            int z1 = Math.min(z0 + 1, side - 1);
            float tx = (float) Math.max(0.0, Math.min(1.0, gridX - x0));
            float tz = (float) Math.max(0.0, Math.min(1.0, gridZ - z0));
            int i00 = z0 * side + x0;
            int i10 = z0 * side + x1;
            int i01 = z1 * side + x0;
            int i11 = z1 * side + x1;
            out.snowIntensity = bilerp(snow, i00, i10, i01, i11, tx, tz);
            out.whiteoutIntensity = bilerp(whiteout, i00, i10, i01, i11, tx, tz);
            out.windIntensity = bilerp(wind, i00, i10, i01, i11, tx, tz);
            out.windX = bilerp(windX, i00, i10, i01, i11, tx, tz);
            out.windZ = bilerp(windZ, i00, i10, i01, i11, tx, tz);
            out.visibilityBlocks = bilerp(visibility, i00, i10, i01, i11, tx, tz);
        }

        private void samplePrecipitation(double blockX, double blockZ, MutableVisualWeatherSample out) {
            if (!valid || side <= 0) {
                out.clear();
                return;
            }
            double gridX = (blockX - originX) / spacing;
            double gridZ = (blockZ - originZ) / spacing;
            int x0 = clamp((int) Math.floor(gridX), 0, side - 1);
            int z0 = clamp((int) Math.floor(gridZ), 0, side - 1);
            int x1 = Math.min(x0 + 1, side - 1);
            int z1 = Math.min(z0 + 1, side - 1);
            float tx = (float) Math.max(0.0, Math.min(1.0, gridX - x0));
            float tz = (float) Math.max(0.0, Math.min(1.0, gridZ - z0));
            int i00 = z0 * side + x0;
            int i10 = z0 * side + x1;
            int i01 = z1 * side + x0;
            int i11 = z1 * side + x1;
            out.snowIntensity = bilerp(snow, i00, i10, i01, i11, tx, tz);
            out.whiteoutIntensity = bilerp(whiteout, i00, i10, i01, i11, tx, tz);
            out.windIntensity = bilerp(wind, i00, i10, i01, i11, tx, tz);
            out.windX = bilerp(windX, i00, i10, i01, i11, tx, tz);
            out.windZ = bilerp(windZ, i00, i10, i01, i11, tx, tz);
        }

        private static float bilerp(float[] values, int i00, int i10, int i01, int i11,
                                    float tx, float tz) {
            return lerp(tz, lerp(tx, values[i00], values[i10]), lerp(tx, values[i01], values[i11]));
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private record PendingSnapshot(ResourceKey<Level> dimension, long climateSeconds,
                                   long serverClockDayTime,
                                   List<WhiteCurtainDescriptor> descriptors) {
    }
}
