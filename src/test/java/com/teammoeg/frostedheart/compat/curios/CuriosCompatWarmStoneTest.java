/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.compat.curios;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuriosCompatWarmStoneTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @SuppressWarnings("removal")
    void registeredSlotHasTheDedicatedIdentityPriorityIconAndSingleCapacity() {
        SlotTypeMessage message = CuriosCompat.warmStoneSlotMessage();

        assertEquals(CuriosCompat.WARM_STONE_SLOT, message.getIdentifier());
        assertEquals(CuriosCompat.WARM_STONE_SLOT_PRIORITY, message.getPriority());
        assertEquals(1, message.getSize());
        assertEquals("frostedheart:slot/empty_warm_stone_slot", message.getIcon().toString());
    }

    @Test
    void slotZeroQueryIgnoresVisibilityAndRenderPresentationState() {
        ItemStack reservoir = new ItemStack(Items.STONE);
        ICurioStacksHandler handler = handler(reservoir, 1);

        ItemStack result = CuriosCompat.wearableThermalReservoirInWarmStoneSlot(
                handler, stack -> stack == reservoir);

        assertSame(reservoir, result);
    }

    @Test
    void slotZeroQueryRejectsEmptyNonReservoirAndMissingSlots() {
        assertTrue(CuriosCompat.wearableThermalReservoirInWarmStoneSlot(
                handler(new ItemStack(Items.STONE), 1), stack -> false).isEmpty());
        assertTrue(CuriosCompat.wearableThermalReservoirInWarmStoneSlot(
                handler(ItemStack.EMPTY, 0), stack -> true).isEmpty());
    }

    private static ICurioStacksHandler handler(ItemStack stack, int slots) {
        IDynamicStackHandler stacks = (IDynamicStackHandler) Proxy.newProxyInstance(
                CuriosCompatWarmStoneTest.class.getClassLoader(),
                new Class<?>[]{IDynamicStackHandler.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getStackInSlot")) {
                        return stack;
                    }
                    if (method.getName().equals("getSlots")) {
                        return slots;
                    }
                    throw new AssertionError("unexpected stack handler method: " + method.getName());
                }
        );
        return (ICurioStacksHandler) Proxy.newProxyInstance(
                CuriosCompatWarmStoneTest.class.getClassLoader(),
                new Class<?>[]{ICurioStacksHandler.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getSlots")) {
                        return slots;
                    }
                    if (method.getName().equals("getStacks")) {
                        return stacks;
                    }
                    if (method.getName().equals("isVisible")
                            || method.getName().equals("getRenders")) {
                        throw new AssertionError("presentation state must not be read");
                    }
                    throw new AssertionError("unexpected curios handler method: " + method.getName());
                }
        );
    }
}
