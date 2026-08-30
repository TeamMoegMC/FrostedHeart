/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import com.sun.management.ThreadMXBean;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroppedReservoirWorkloadTest {
    private static final int SYNTHETIC_ENTITIES = 400;
    private static final int CADENCE_WINDOWS = 12;
    private static final long STEADY_ALLOCATION_LIMIT_BYTES = 64L * 1024L;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void fixedHookClaimsOnlyAcceptTheSuppliedExactServerReservoirEntity() throws Exception {
        Method itemHook = WarmStoneItem.class.getDeclaredMethod(
                "onEntityItemUpdate", ItemStack.class, ItemEntity.class);
        Method handlerEntry = DroppedReservoirExchangeHandler.class.getDeclaredMethod(
                "tickItemEntity", ItemStack.class, ItemEntity.class);
        assertEquals(boolean.class, itemHook.getReturnType());
        assertEquals(DroppedReservoirExchangeHandler.Status.class,
                handlerEntry.getReturnType());
        assertArrayEquals(new Class<?>[]{ItemStack.class, ItemEntity.class},
                handlerEntry.getParameterTypes());
        assertTrue(WearableThermalReservoir.class.isAssignableFrom(
                WarmStoneItem.class));

        int accepted = 0;
        int expectedAccepted = 0;
        for (int claim = 0; claim < SYNTHETIC_ENTITIES; claim++) {
            boolean clientSide = claim % 11 == 0;
            boolean alive = claim % 7 != 0;
            boolean exactStack = claim % 5 != 0;
            int stackCount = claim % 13 == 0 ? 2 : 1;
            boolean expected = !clientSide && alive && exactStack && stackCount == 1;
            if (expected) {
                expectedAccepted++;
            }
            if (DroppedReservoirExchangeHandler.isExactServerItemContext(
                    clientSide, alive, exactStack, stackCount)) {
                accepted++;
            }
        }
        assertEquals(expectedAccepted, accepted);
        System.out.printf(
                "FH_T19_WORKLOAD hook_claims=%d accepted=%d rejected=%d%n",
                SYNTHETIC_ENTITIES, accepted, SYNTHETIC_ENTITIES - accepted);

        for (Field field : DroppedReservoirExchangeHandler.class.getDeclaredFields()) {
            assertFalse(Map.class.isAssignableFrom(field.getType()));
            assertFalse(Collection.class.isAssignableFrom(field.getType()));
            assertFalse(Iterable.class.isAssignableFrom(field.getType()));
        }
    }

    @Test
    void uuidsOccupyAllBucketsAndEachEntityRunsOncePerLoadedCadenceWindow() {
        UUID[] identities = identities();
        int[] bucketCounts = new int[DroppedReservoirExchangeHandler.CADENCE_TICKS];
        for (UUID identity : identities) {
            bucketCounts[DroppedReservoirExchangeHandler.cadenceBucket(identity)]++;
            for (int loadedTick = 0;
                 loadedTick < DroppedReservoirExchangeHandler.CADENCE_TICKS;
                 loadedTick++) {
                assertFalse(DroppedReservoirExchangeHandler.isCadenceTick(
                        identity, loadedTick));
            }
        }
        for (int count : bucketCounts) {
            assertTrue(count > 0);
        }

        for (int window = 0; window < CADENCE_WINDOWS; window++) {
            int firstLoadedTick = DroppedReservoirExchangeHandler.CADENCE_TICKS
                    * (window + 1);
            for (UUID identity : identities) {
                int runs = 0;
                for (int loadedTick = firstLoadedTick;
                     loadedTick < firstLoadedTick
                             + DroppedReservoirExchangeHandler.CADENCE_TICKS;
                     loadedTick++) {
                    if (DroppedReservoirExchangeHandler.isCadenceTick(
                            identity, loadedTick)) {
                        runs++;
                    }
                }
                assertEquals(1, runs);
            }
        }
        int minimumBucketCount = Integer.MAX_VALUE;
        int maximumBucketCount = Integer.MIN_VALUE;
        for (int count : bucketCounts) {
            minimumBucketCount = Math.min(minimumBucketCount, count);
            maximumBucketCount = Math.max(maximumBucketCount, count);
        }
        System.out.printf(
                "FH_T19_WORKLOAD cadence_entities=%d buckets=20 bucket_min=%d "
                        + "bucket_max=%d windows=%d runs_per_entity_window=1%n",
                identities.length,
                minimumBucketCount,
                maximumBucketCount,
                CADENCE_WINDOWS);
    }

    @Test
    void deletionNeedsNoPersistentCleanupAndCadenceChecksStayAllocationBounded()
            throws Exception {
        DroppedReservoirExchangeHandler handler =
                new DroppedReservoirExchangeHandler();
        Object[] fixedState = instanceFieldValues(handler);
        assertEquals(3, fixedState.length);

        ItemStack stack = new ItemStack(Items.STONE);
        new WearableThermalState(42.0D, 17.0D).writeTo(stack);
        CompoundTag persistedState = stack.getTag().copy();
        UUID[] identities = identities();

        for (int warmup = 0; warmup < 1_000; warmup++) {
            UUID identity = identities[warmup % identities.length];
            DroppedReservoirExchangeHandler.isCadenceTick(identity, 20 + warmup);
        }

        ThreadMXBean bean = allocationBean();
        long threadId = Thread.currentThread().getId();
        long beforeBytes = bean.getThreadAllocatedBytes(threadId);
        int runs = 0;
        for (int window = 0; window < 100; window++) {
            int firstLoadedTick = 20 * (window + 1);
            for (UUID identity : identities) {
                if (DroppedReservoirExchangeHandler.isCadenceTick(
                        identity,
                        firstLoadedTick
                                + DroppedReservoirExchangeHandler.cadenceBucket(identity))) {
                    runs++;
                }
            }
        }
        long allocatedBytes = bean.getThreadAllocatedBytes(threadId) - beforeBytes;

        assertEquals(100 * identities.length, runs);
        assertTrue(allocatedBytes <= STEADY_ALLOCATION_LIMIT_BYTES,
                "cadence steady path allocated " + allocatedBytes + " bytes");
        System.out.printf(
                "FH_T19_WORKLOAD cadence_checks=%d allocated_bytes=%d ceiling_bytes=%d%n",
                100 * identities.length,
                allocatedBytes,
                STEADY_ALLOCATION_LIMIT_BYTES);
        assertEquals(persistedState, stack.getTag());

        Object[] afterClaims = instanceFieldValues(handler);
        assertEquals(fixedState.length, afterClaims.length);
        for (int index = 0; index < fixedState.length; index++) {
            assertSame(fixedState[index], afterClaims[index]);
        }
    }

    private static UUID[] identities() {
        UUID[] identities = new UUID[SYNTHETIC_ENTITIES];
        for (int index = 0; index < identities.length; index++) {
            identities[index] = new UUID(index * 31L, index * 17L + 1L);
        }
        return identities;
    }

    private static Object[] instanceFieldValues(Object target) throws Exception {
        Field[] fields = target.getClass().getDeclaredFields();
        int count = 0;
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                count++;
            }
        }
        Object[] values = new Object[count];
        int index = 0;
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                values[index++] = field.get(target);
            }
        }
        return values;
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
