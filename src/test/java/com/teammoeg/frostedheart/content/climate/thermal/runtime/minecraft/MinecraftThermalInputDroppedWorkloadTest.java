/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.sun.management.ThreadMXBean;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftThermalInputDroppedWorkloadTest {
    private static final int CLAIMS_PER_TICK = 4_096;
    private static final int UNIQUE_LOCATIONS = 256;
    private static final long STEADY_ALLOCATION_LIMIT_BYTES = 64L * 1024L;

    @Test
    void quarterBlockClaimsReuseSixtyFourSamplesAndRecycleOnNextTick() {
        int capacity = MinecraftThermalInput
                .GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK;
        MinecraftThermalInput.ItemEnvironmentSampleCache cache =
                new MinecraftThermalInput.ItemEnvironmentSampleCache(capacity);
        MinecraftThermalInput.MutableEnvironmentSample source =
                new MinecraftThermalInput.MutableEnvironmentSample();
        source.setFallbackAir(-12.5D, 80L);
        source.setObservationTick(80L);

        int stores = 0;
        int hits = 0;
        for (int claim = 0; claim < CLAIMS_PER_TICK; claim++) {
            int location = claim % UNIQUE_LOCATIONS;
            int cached = cache.find(80L, location, 4, -location);
            if (cached >= 0) {
                hits++;
            } else if (cache.store(location, 4, -location, source)) {
                stores++;
            }
        }
        assertEquals(64, capacity);
        assertEquals(capacity, stores);
        assertEquals((CLAIMS_PER_TICK / UNIQUE_LOCATIONS - 1) * capacity, hits);
        assertEquals(capacity, cache.size());
        assertFalse(cache.canAdmit());
        System.out.printf(
                "FH_T19_WORKLOAD item_sample_claims=%d unique_locations=%d "
                        + "stores=%d hits=%d overflow=%d capacity=%d%n",
                CLAIMS_PER_TICK,
                UNIQUE_LOCATIONS,
                stores,
                hits,
                CLAIMS_PER_TICK - stores - hits,
                capacity);

        MinecraftThermalInput.MutableEnvironmentSample copied =
                new MinecraftThermalInput.MutableEnvironmentSample();
        int sameQuarter = cache.find(80L, 17, 4, -17);
        assertTrue(sameQuarter >= 0);
        cache.copyTo(sameQuarter, copied);
        assertEquals(-12.5D, copied.airTemperatureC());
        assertEquals(80L, copied.observationTick());

        assertEquals(-1, cache.find(81L, 17, 4, -17));
        assertEquals(81L, cache.generationTick());
        assertEquals(0, cache.size());
        assertTrue(cache.canAdmit());
        assertTrue(cache.store(255, 4, -255, source));
        assertEquals(1, cache.size());

        cache.close();
        assertEquals(Long.MIN_VALUE, cache.generationTick());
        assertEquals(0, cache.size());
    }

    @Test
    void fixedCacheStorageAndHitPathHaveAnExecutableAllocationCeiling()
            throws Exception {
        int capacity = MinecraftThermalInput
                .GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK;
        MinecraftThermalInput.ItemEnvironmentSampleCache cache =
                new MinecraftThermalInput.ItemEnvironmentSampleCache(capacity);
        MinecraftThermalInput.MutableEnvironmentSample source =
                new MinecraftThermalInput.MutableEnvironmentSample();
        source.setFallbackAir(-8.0D, 100L);
        source.setObservationTick(100L);
        for (int location = 0; location < capacity; location++) {
            assertEquals(-1, cache.find(100L, location, 8, -location));
            assertTrue(cache.store(location, 8, -location, source));
        }

        for (Field field : cache.getClass().getDeclaredFields()) {
            assertFalse(Map.class.isAssignableFrom(field.getType()));
            assertFalse(Collection.class.isAssignableFrom(field.getType()));
            assertFalse(Iterable.class.isAssignableFrom(field.getType()));
        }
        assertEquals(capacity, arrayLength(cache, "quarterX"));
        assertEquals(capacity, arrayLength(cache, "quarterY"));
        assertEquals(capacity, arrayLength(cache, "quarterZ"));
        assertEquals(capacity, arrayLength(cache, "samples"));

        MinecraftThermalInput.MutableEnvironmentSample copied =
                new MinecraftThermalInput.MutableEnvironmentSample();
        for (int warmup = 0; warmup < 10_000; warmup++) {
            int location = warmup & (capacity - 1);
            int cached = cache.find(100L, location, 8, -location);
            cache.copyTo(cached, copied);
        }

        ThreadMXBean bean = allocationBean();
        long threadId = Thread.currentThread().getId();
        long beforeBytes = bean.getThreadAllocatedBytes(threadId);
        long checksum = 0L;
        for (int claim = 0; claim < 100_000; claim++) {
            int location = claim & (capacity - 1);
            int cached = cache.find(100L, location, 8, -location);
            cache.copyTo(cached, copied);
            checksum += cached;
        }
        long allocatedBytes = bean.getThreadAllocatedBytes(threadId) - beforeBytes;

        assertTrue(checksum > 0L);
        assertEquals(capacity, cache.size());
        assertTrue(allocatedBytes <= STEADY_ALLOCATION_LIMIT_BYTES,
                "sample-cache hit path allocated " + allocatedBytes + " bytes");
        System.out.printf(
                "FH_T19_WORKLOAD item_sample_cache_hits=%d allocated_bytes=%d "
                        + "ceiling_bytes=%d live_entries=%d%n",
                100_000,
                allocatedBytes,
                STEADY_ALLOCATION_LIMIT_BYTES,
                cache.size());
    }

    private static int arrayLength(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return java.lang.reflect.Array.getLength(field.get(target));
    }

    private static ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean base =
                ManagementFactory.getThreadMXBean();
        assertTrue(base instanceof ThreadMXBean);
        ThreadMXBean bean = (ThreadMXBean) base;
        assertTrue(bean.isThreadAllocatedMemorySupported());
        if (!bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        return bean;
    }
}
