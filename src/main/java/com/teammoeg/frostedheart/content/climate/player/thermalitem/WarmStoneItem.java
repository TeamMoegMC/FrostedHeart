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

import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.item.FHBaseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 暖石类可穿戴热库的通用物品实现。
 * <p>
 * Shared item implementation for warm stones and hot-water bags. Its profile is
 * immutable; each ItemStack owns only the two persisted temperatures.
 */
public class WarmStoneItem extends FHBaseItem
        implements WearableThermalReservoir, ICurioItem {
    private static final InventoryThermalExchangeHandler INVENTORY_EXCHANGE_HANDLER =
            new InventoryThermalExchangeHandler();
    private static final DroppedReservoirExchangeHandler DROPPED_EXCHANGE_HANDLER =
            new DroppedReservoirExchangeHandler();
    private final WearableThermalProfile thermalProfile;

    public WarmStoneItem(Item.Properties properties, WearableThermalProfile thermalProfile) {
        super(properties.stacksTo(1));
        this.thermalProfile = Objects.requireNonNull(thermalProfile, "thermalProfile");
    }

    @Override
    public WearableThermalProfile thermalProfile(ItemStack stack) {
        return thermalProfile;
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slot,
            boolean selected
    ) {
        if (InventoryThermalExchangeHandler.isServerPlayerInventoryContext(
                level.isClientSide, entity instanceof ServerPlayer)) {
            INVENTORY_EXCHANGE_HANDLER.tickServerPlayerInventoryStack(
                    stack, (ServerPlayer) entity, slot);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        DROPPED_EXCHANGE_HANDLER.tickItemEntity(stack, entity);
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        appendThermalTooltip(stack, thermalProfile, tooltip, flag);
    }

    /**
     * Adds client-facing state only; this path deliberately never initializes or writes NBT.
     */
    static void appendThermalTooltip(
            ItemStack stack,
            WearableThermalProfile profile,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        WearableThermalState.read(stack).ifPresentOrElse(state -> {
            tooltip.add(Component.translatable(
                    "tooltip.frostedheart.wearable_thermal_reservoir.surface_temperature",
                    formatTemperature(state.surfaceTemperatureC())
            ).withStyle(ChatFormatting.GRAY));
            if (flag.isAdvanced()) {
                tooltip.add(Component.translatable(
                        "tooltip.frostedheart.wearable_thermal_reservoir.core_temperature",
                        formatTemperature(state.coreTemperatureC())
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
        }, () -> tooltip.add(Component.translatable(
                "tooltip.frostedheart.wearable_thermal_reservoir.surface_temperature.uninitialized"
        ).withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.translatable(
                "tooltip.frostedheart.wearable_thermal_reservoir.capacity",
                formatCapacityPercent(profile.capacityRatio())
        ).withStyle(ChatFormatting.GRAY));
    }

    private static String formatTemperature(double temperatureC) {
        return String.format(Locale.ROOT, "%.1f", temperatureC);
    }

    private static String formatCapacityPercent(double capacityRatio) {
        return String.format(Locale.ROOT, "%.0f", capacityRatio * 100.0D);
    }

    /** Only the dedicated first warm-stone slot may equip this reservoir. */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return canEquipWarmStoneSlot(slotContext);
    }

    static boolean canEquipWarmStoneSlot(SlotContext slotContext) {
        return CuriosCompat.WARM_STONE_SLOT.equals(slotContext.identifier())
                && slotContext.index() == 0;
    }
}
