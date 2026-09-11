/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import com.teammoeg.chorda.math.Rect;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

/** Pure gameplay and visual field math for a moving white curtain. */
public final class WhiteCurtainFieldModel {
    public static final long HOURS_PER_CHUNK = 6L;
    public static final long SECONDS_PER_CHUNK =
            (long) (WorldClockSource.secondsPerDay / (24.0F / HOURS_PER_CHUNK));
    private static final long SECONDS_PER_CLIMATE_HOUR = 50L;
    private static final ClimateType[] CLIMATE_TYPES = ClimateType.values();

    private WhiteCurtainFieldModel() {
    }

    public static boolean isAffected(WhiteCurtainDescriptor curtain, ChunkPos chunk) {
        return curtain.affectedArea().inRange(chunk.x, chunk.z);
    }

    public static boolean isIntersected(WhiteCurtainDescriptor curtain, Rect area) {
        Rect own = curtain.affectedArea();
        return own.getX() <= area.getX2() && area.getX() <= own.getX2()
                && own.getY() <= area.getY2() && area.getY() <= own.getY2();
    }

    public static long deltaChunks(WhiteCurtainDescriptor curtain, ChunkPos chunk) {
        Rect area = curtain.affectedArea();
        return switch (curtain.moveDirection()) {
            case NORTH -> area.getY2() - chunk.z;
            case SOUTH -> chunk.z - area.getY();
            case WEST -> area.getX2() - chunk.x;
            case EAST -> chunk.x - area.getX();
            default -> 0L;
        };
    }

    public static int maxDeltaChunks(WhiteCurtainDescriptor curtain) {
        return curtain.moveDirection().getAxis() == Direction.Axis.Z
                ? curtain.affectedArea().getH()
                : curtain.affectedArea().getW();
    }

    public static long deltaSeconds(WhiteCurtainDescriptor curtain, ChunkPos chunk) {
        return deltaChunks(curtain, chunk) * SECONDS_PER_CHUNK;
    }

    public static long maxDeltaSeconds(WhiteCurtainDescriptor curtain) {
        return maxDeltaChunks(curtain) * SECONDS_PER_CHUNK;
    }

    public static boolean isInvalid(WhiteCurtainDescriptor curtain, long climateSeconds) {
        return curtain.climate().getCalmEndTime() + maxDeltaSeconds(curtain) < climateSeconds;
    }

    /**
     * Direction maps a chunk to an integer travel delay. The event is then sampled at a
     * whole climate hour exactly as the legacy per-curtain daily cache did.
     */
    public static ClimateResult sampleGameplay(WhiteCurtainDescriptor curtain, long climateSeconds, ChunkPos chunk) {
        return sampleEventHour(curtain.climate(), climateSeconds - deltaSeconds(curtain, chunk));
    }

    public static long nextGameplayTransition(WhiteCurtainDescriptor curtain, long climateSeconds, ChunkPos chunk) {
        long delay = deltaSeconds(curtain, chunk);
        long localSeconds = climateSeconds - delay;
        ClimateEvent event = curtain.climate();
        long relative = localSeconds - event.getStartTime();
        long emittedHours = (event.getCalmEndTime() - event.getStartTime()) / SECONDS_PER_CLIMATE_HOUR;
        long hour;
        long nextLocal;
        if (relative >= 0L) {
            hour = relative / SECONDS_PER_CLIMATE_HOUR;
            if (hour >= emittedHours) {
                return Long.MAX_VALUE;
            }
            nextLocal = event.getStartTime() + (hour + 1L) * SECONDS_PER_CLIMATE_HOUR;
        } else if (relative > -SECONDS_PER_CLIMATE_HOUR) {
            nextLocal = event.getStartTime() + SECONDS_PER_CLIMATE_HOUR;
        } else {
            // Java integer division makes [-49,-1] map to legacy hour zero.
            nextLocal = event.getStartTime() - SECONDS_PER_CLIMATE_HOUR + 1L;
        }
        long transition = nextLocal + delay;
        return transition > climateSeconds ? transition : climateSeconds + 1L;
    }

    private static ClimateResult sampleEventHour(ClimateEvent event, long localSeconds) {
        if (localSeconds > event.getCalmEndTime()) {
            return ClimateResult.EMPTY;
        }
        long emittedHours = (event.getCalmEndTime() - event.getStartTime()) / SECONDS_PER_CLIMATE_HOUR;
        long hour = (localSeconds - event.getStartTime()) / SECONDS_PER_CLIMATE_HOUR;
        if (hour < 0L || hour >= emittedHours) {
            return ClimateResult.EMPTY;
        }
        return event.getHourClimate(event.getStartTime() + hour * SECONDS_PER_CLIMATE_HOUR);
    }

    public static VisualKernel prepareVisual(WhiteCurtainDescriptor curtain) {
        ClimateEvent event = curtain.climate();
        int hourCount = Math.max(0, Math.toIntExact(
                (event.getCalmEndTime() - event.getStartTime()) / SECONDS_PER_CLIMATE_HOUR));
        byte[] phases = new byte[hourCount];
        int firstSnowHour = -1;
        int lastSnowHourExclusive = -1;
        for (int hour = 0; hour < hourCount; hour++) {
            ClimateType phase = event.getHourClimate(
                    event.getStartTime() + hour * SECONDS_PER_CLIMATE_HOUR).climate();
            phases[hour] = (byte) phase.ordinal();
            if (phase.isSnowyOrBlizzard()) {
                if (firstSnowHour < 0) {
                    firstSnowHour = hour;
                }
                lastSnowHourExclusive = hour + 1;
            }
        }
        Rect area = curtain.affectedArea();
        long seed = mix64((((long) area.getX()) << 32) ^ (area.getY() & 0xffffffffL));
        seed = mix64(seed ^ (((long) area.getW()) << 32) ^ (area.getH() & 0xffffffffL));
        seed = mix64(seed ^ event.getStartTime() ^ ((long) curtain.moveDirection().ordinal() << 56));
        return new VisualKernel(
                area.getX() * 16.0,
                area.getY() * 16.0,
                (area.getX2() + 1) * 16.0,
                (area.getY2() + 1) * 16.0,
                area.getX(), area.getY(), area.getX2(), area.getY2(),
                curtain.moveDirection().getStepX(), curtain.moveDirection().getStepZ(),
                event.getStartTime(), maxDeltaChunks(curtain), firstSnowHour,
                lastSnowHourExclusive, phases, seed);
    }

    /**
     * Continuous block coordinates use the same delay as gameplay at every chunk center:
     * {@code continuousChunk = (block - 8) / 16}. Only the visual corridor/front edges are smoothed.
     */
    public static void sampleVisual(VisualKernel kernel, double climateSeconds, double blockX, double blockZ,
                                    WhiteCurtainVisualProfile profile, MutableVisualWeatherSample out) {
        out.clear();
        double edgeDistance = Math.min(
                Math.min(blockX - kernel.minBlockX, kernel.maxBlockXExclusive - blockX),
                Math.min(blockZ - kernel.minBlockZ, kernel.maxBlockZExclusive - blockZ));
        float corridor = smoothstep(0.0F, profile.corridorEdgeFadeBlocks(), (float) edgeDistance);
        if (corridor <= 0.0F) {
            return;
        }

        double continuousChunkX = (blockX - 8.0) / 16.0;
        double continuousChunkZ = (blockZ - 8.0) / 16.0;
        double deltaChunks;
        if (kernel.moveZ < 0) {
            deltaChunks = kernel.maxChunkZ - continuousChunkZ;
        } else if (kernel.moveZ > 0) {
            deltaChunks = continuousChunkZ - kernel.minChunkZ;
        } else if (kernel.moveX < 0) {
            deltaChunks = kernel.maxChunkX - continuousChunkX;
        } else {
            deltaChunks = continuousChunkX - kernel.minChunkX;
        }

        double localSeconds = climateSeconds - deltaChunks * SECONDS_PER_CHUNK;
        double phaseHours = (localSeconds - kernel.startSeconds) / SECONDS_PER_CLIMATE_HOUR;
        int hour = (int) Math.floor(phaseHours);
        ClimateType phase = kernel.phaseAt(hour);
        boolean snowy = phase.isSnowyOrBlizzard();

        float signedFrontDistance = (float) ((phaseHours - hour) *
                (16.0 * SECONDS_PER_CLIMATE_HOUR / SECONDS_PER_CHUNK));
        out.insideAffectedCorridor = true;
        out.signedDistanceToActiveFrontBlocks = snowy ? signedFrontDistance : -signedFrontDistance;

        float snowTarget = snowy ? 1.0F : 0.0F;
        float previousSnow = kernel.phaseAt(hour - 1).isSnowyOrBlizzard() ? 1.0F : 0.0F;
        float hourFraction = (float) (phaseHours - hour);
        float transitionFraction = Math.min(0.49F,
                profile.phaseTransitionSeconds() / SECONDS_PER_CLIMATE_HOUR);
        float snow = smoothPhaseTransition(previousSnow, snowTarget, hourFraction, transitionFraction);

        float whiteoutTarget = whiteoutTarget(phase);
        float previousWhiteout = whiteoutTarget(kernel.phaseAt(hour - 1));
        float whiteout = smoothPhaseTransition(
                previousWhiteout, whiteoutTarget, hourFraction, transitionFraction);
        out.snowIntensity = clamp01(snow * corridor);
        out.whiteoutIntensity = clamp01(whiteout * corridor);
        out.windIntensity = clamp01(Math.max(out.snowIntensity * 0.55F, out.whiteoutIntensity) * corridor);
        out.windX = kernel.moveX;
        out.windZ = kernel.moveZ;
        out.visibilityBlocks = lerp(out.whiteoutIntensity,
                512.0F, profile.minimumVisibilityBlocks());
    }

    private static float smoothPhaseTransition(float previous, float current,
                                               float hourFraction, float transitionFraction) {
        if (previous == current || hourFraction >= transitionFraction) {
            return current;
        }
        return lerp(smoothstep(0.0F, transitionFraction, hourFraction), previous, current);
    }

    private static float whiteoutTarget(ClimateType phase) {
        return phase.isBlizzard() ? 1.0F : phase == ClimateType.SNOW_BLIZZARD ? 0.35F : 0.0F;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0F : 1.0F;
        }
        float t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public static final class VisualKernel {
        public final double minBlockX;
        public final double minBlockZ;
        public final double maxBlockXExclusive;
        public final double maxBlockZExclusive;
        public final int minChunkX;
        public final int minChunkZ;
        public final int maxChunkX;
        public final int maxChunkZ;
        public final float moveX;
        public final float moveZ;
        public final long startSeconds;
        public final int maxDeltaChunks;
        public final int firstSnowHour;
        public final int lastSnowHourExclusive;
        public final long visualSeed;
        private final byte[] hourlyPhases;

        private VisualKernel(double minBlockX, double minBlockZ, double maxBlockXExclusive,
                             double maxBlockZExclusive, int minChunkX, int minChunkZ,
                             int maxChunkX, int maxChunkZ, float moveX, float moveZ,
                             long startSeconds, int maxDeltaChunks, int firstSnowHour,
                             int lastSnowHourExclusive, byte[] hourlyPhases, long visualSeed) {
            this.minBlockX = minBlockX;
            this.minBlockZ = minBlockZ;
            this.maxBlockXExclusive = maxBlockXExclusive;
            this.maxBlockZExclusive = maxBlockZExclusive;
            this.minChunkX = minChunkX;
            this.minChunkZ = minChunkZ;
            this.maxChunkX = maxChunkX;
            this.maxChunkZ = maxChunkZ;
            this.moveX = moveX;
            this.moveZ = moveZ;
            this.startSeconds = startSeconds;
            this.maxDeltaChunks = maxDeltaChunks;
            this.firstSnowHour = firstSnowHour;
            this.lastSnowHourExclusive = lastSnowHourExclusive;
            this.hourlyPhases = hourlyPhases;
            this.visualSeed = visualSeed;
        }

        public ClimateType phaseAt(int hour) {
            if (hour < 0 || hour >= hourlyPhases.length) {
                return ClimateType.NONE;
            }
            return CLIMATE_TYPES[hourlyPhases[hour] & 0xff];
        }

        public int phaseCount() {
            return hourlyPhases.length;
        }

        public double leadingSnowDeltaChunks(double climateSeconds) {
            if (firstSnowHour < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            double baseHour = (climateSeconds - startSeconds) / SECONDS_PER_CLIMATE_HOUR;
            return (baseHour - firstSnowHour) / HOURS_PER_CHUNK;
        }

        public double trailingSnowDeltaChunks(double climateSeconds) {
            if (lastSnowHourExclusive < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            double baseHour = (climateSeconds - startSeconds) / SECONDS_PER_CLIMATE_HOUR;
            return (baseHour - lastSnowHourExclusive) / HOURS_PER_CHUNK;
        }
    }
}
