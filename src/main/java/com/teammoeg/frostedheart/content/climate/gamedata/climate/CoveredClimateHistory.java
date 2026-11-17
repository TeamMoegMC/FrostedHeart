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

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import java.util.Arrays;
import java.util.function.LongToDoubleFunction;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;

/**
 * 覆盖层下的气候：逐小时记录全局气候，按覆盖厚度 c 做一阶低通，时间常数 τ(c) = τ10·(c/10)² 小时。
 * 持续的气候原样传下去；比 τ 短的变化被衰减并延迟。
 * <p>
 * Climate under cover: the hourly global climate is recorded and passed through a first-order low pass whose time
 * constant is τ(c) = τ10·(c/10)² hours for c opaque blocks of cover. Lasting climate reaches covered places unchanged;
 * changes shorter than τ arrive damped and late.
 */
public final class CoveredClimateHistory {
    /** Maps an absolute climate hour and the global climate recorded for it to the local climate of one place. */
    @FunctionalInterface
    public interface LocalClimate {
        double at(long hour, double globalCelsius);
    }

    private static final String NEWEST_HOUR = "newestHour";
    private static final String HOURLY = "hourlyCelsius";
    private float[] hourly = new float[0];
    private long newestHour;
    private int size;
    private long cachedHour = Long.MIN_VALUE;
    private long cachedGeneration = Long.MIN_VALUE;
    private float[] openChunks = new float[0];
    private final Long2ObjectOpenHashMap<float[]> curtainChunks = new Long2ObjectOpenHashMap<>();

    /** Time constant in climate hours under {@code cover} blocks. */
    public static double timeConstantHours(int cover) {
        double ratio = cover / 10.0;
        return FHConfig.SERVER.CLIMATE.coverResponseDaysAtTenBlocks.get() * WorldClockSource.hoursPerDay * ratio * ratio;
    }

    /** Hours summed for a time constant: four time constants leave under 2% of the weight out. */
    static int windowHours(double timeConstantHours) {
        return timeConstantHours <= 0 ? 1 : (int) Math.min(1 << 20, Math.ceil(4 * timeConstantHours) + 1);
    }

    /** Records every hour after the newest recorded one, through {@code hour}. */
    public void recordThrough(long hour, int coverLimit, LongToDoubleFunction climateAtHour) {
        resize(windowHours(timeConstantHours(coverLimit)));
        if (size > 0 && hour <= newestHour) return;
        long first = size == 0 ? hour : Math.max(newestHour + 1, hour - hourly.length + 1);
        for (long recorded = first; recorded <= hour; recorded++) {
            hourly[Math.floorMod(recorded, hourly.length)] = (float) climateAtHour.applyAsDouble(recorded);
            newestHour = recorded;
            size = Math.min(hourly.length, size + 1);
        }
    }

    public void clear() {
        size = 0;
        cachedHour = Long.MIN_VALUE;
    }

    /**
     * The covered climate at {@code hour}, or NaN before anything is recorded. Values are cached per hour, cover and
     * white-curtain generation; {@code chunk} is {@link Long#MIN_VALUE} for places no curtain reaches.
     */
    public float covered(int cover, int coverLimit, long hour, long chunk, long curtainGeneration, LocalClimate local) {
        if (size == 0) return Float.NaN;
        if (hour != cachedHour || curtainGeneration != cachedGeneration || openChunks.length != coverLimit + 1) {
            cachedHour = hour;
            cachedGeneration = curtainGeneration;
            if (openChunks.length != coverLimit + 1) openChunks = new float[coverLimit + 1];
            Arrays.fill(openChunks, Float.NaN);
            curtainChunks.clear();
        }
        float[] cache = chunk == Long.MIN_VALUE ? openChunks : curtainChunks.computeIfAbsent(chunk, ignored -> {
            float[] values = new float[coverLimit + 1];
            Arrays.fill(values, Float.NaN);
            return values;
        });
        int index = Math.min(cover, coverLimit);
        float value = cache[index];
        if (Float.isNaN(value)) cache[index] = value = (float) lowPass(index, hour, local);
        return value;
    }

    /** Exponentially weighted mean of the recorded hours up to {@code hour}, normalized over what is recorded. */
    double lowPass(int cover, long hour, LocalClimate local) {
        long newest = Math.min(hour, newestHour);
        int available = size - (int) Math.min(size, newestHour - newest);
        if (available <= 0) return local.at(newestHour, valueAt(newestHour));
        double timeConstant = timeConstantHours(cover);
        if (timeConstant <= 0) return local.at(newest, valueAt(newest));
        double keep = Math.exp(-1.0 / timeConstant);
        int window = Math.min(available, windowHours(timeConstant));
        double weight = 1 - keep, sum = 0, total = 0;
        for (int age = 0; age < window; age++) {
            long recorded = newest - age;
            sum += weight * local.at(recorded, valueAt(recorded));
            total += weight;
            weight *= keep;
        }
        return sum / total;
    }

    private float valueAt(long hour) {
        return hourly[Math.floorMod(hour, hourly.length)];
    }

    private void resize(int capacity) {
        if (capacity == hourly.length) return;
        float[] next = new float[capacity];
        int kept = Math.min(size, capacity);
        for (int age = 0; age < kept; age++) {
            long recorded = newestHour - age;
            next[Math.floorMod(recorded, capacity)] = valueAt(recorded);
        }
        hourly = next;
        size = kept;
        cachedHour = Long.MIN_VALUE;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        int[] values = new int[size];
        for (int index = 0; index < size; index++)
            values[index] = Float.floatToIntBits(valueAt(newestHour - size + 1 + index));
        tag.putLong(NEWEST_HOUR, newestHour);
        tag.putIntArray(HOURLY, values);
        return tag;
    }

    public void load(CompoundTag tag) {
        int[] values = tag.getIntArray(HOURLY);
        hourly = new float[Math.max(1, values.length)];
        newestHour = tag.getLong(NEWEST_HOUR);
        size = values.length;
        for (int index = 0; index < values.length; index++)
            hourly[Math.floorMod(newestHour - values.length + 1 + index, hourly.length)] = Float.intBitsToFloat(values[index]);
        cachedHour = Long.MIN_VALUE;
    }
}
