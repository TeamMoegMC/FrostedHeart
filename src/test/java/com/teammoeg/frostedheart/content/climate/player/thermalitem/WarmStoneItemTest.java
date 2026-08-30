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

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarmStoneItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void reservoirContractKeepsProfileImmutableAndStateOnTheItemStack() {
        WearableThermalReservoir reservoir = stack -> WearableThermalProfile.HOT_WATER_BAG_DEFAULT;
        ItemStack stack = new ItemStack(Items.STONE);

        assertEquals(WearableThermalProfile.HOT_WATER_BAG_DEFAULT.capacityRatio(),
                reservoir.capacityRatio(stack));
        assertEquals(WearableThermalProfile.HOT_WATER_BAG_DEFAULT.surfaceCapacityFraction(),
                reservoir.surfaceCapacityFraction(stack));
        assertFalse(reservoir.thermalState(stack).isPresent());

        reservoir.setTemperaturesC(stack, 60.0D, 25.0D);
        WearableThermalState state = reservoir.thermalState(stack).orElseThrow();
        assertEquals(60.0D, state.coreTemperatureC());
        assertEquals(25.0D, state.surfaceTemperatureC());
    }

    @Test
    void warmStoneItemDeclaresTheNarrowCurioReservoirContract() {
        assertTrue(WearableThermalReservoir.class.isAssignableFrom(WarmStoneItem.class));
        assertTrue(ICurioItem.class.isAssignableFrom(WarmStoneItem.class));
        assertTrue(WarmStoneItem.canEquipWarmStoneSlot(
                new SlotContext("warm_stone", null, 0, false, true)));
        assertFalse(WarmStoneItem.canEquipWarmStoneSlot(
                new SlotContext("warm_stone", null, 1, false, true)));
        assertFalse(WarmStoneItem.canEquipWarmStoneSlot(
                new SlotContext("charm", null, 0, false, true)));
        try {
            assertEquals(void.class, WarmStoneItem.class.getDeclaredMethod(
                    "inventoryTick", ItemStack.class,
                    net.minecraft.world.level.Level.class, Entity.class,
                    int.class, boolean.class).getReturnType());
            assertEquals(boolean.class, WarmStoneItem.class.getDeclaredMethod(
                    "onEntityItemUpdate", ItemStack.class,
                    ItemEntity.class).getReturnType());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("WarmStoneItem must own both exact item hooks",
                    exception);
        }
    }

    @Test
    void tooltipReadsStateWithoutInitializingOrWritingNbt() {
        ItemStack stack = new ItemStack(Items.STONE);
        List<Component> tooltip = new ArrayList<>();

        WarmStoneItem.appendThermalTooltip(
                stack, WearableThermalProfile.WARM_STONE_DEFAULT, tooltip, TooltipFlag.NORMAL);

        assertFalse(stack.hasTag());
        assertEquals(2, tooltip.size());
        assertTrue(tooltipJson(tooltip.get(0)).contains(
                "wearable_thermal_reservoir.surface_temperature.uninitialized"));
        assertTrue(tooltipJson(tooltip.get(1)).contains("10"));
    }

    @Test
    void advancedTooltipSeparatesCoreAndSurfaceTemperaturesWithoutWritingNbt() {
        ItemStack stack = new ItemStack(Items.STONE);
        new WearableThermalState(56.25D, 18.75D).writeTo(stack);
        CompoundTag before = stack.getTag().copy();
        List<Component> tooltip = new ArrayList<>();

        WarmStoneItem.appendThermalTooltip(
                stack, WearableThermalProfile.HOT_WATER_BAG_DEFAULT, tooltip, TooltipFlag.ADVANCED);

        assertEquals(before, stack.getTag());
        assertEquals(3, tooltip.size());
        assertTrue(tooltipJson(tooltip.get(0)).contains("18.8"));
        assertTrue(tooltipJson(tooltip.get(1)).contains("56.3"));
        assertTrue(tooltipJson(tooltip.get(2)).contains("25"));
    }

    private static String tooltipJson(Component component) {
        return Component.Serializer.toJson(component);
    }
}
