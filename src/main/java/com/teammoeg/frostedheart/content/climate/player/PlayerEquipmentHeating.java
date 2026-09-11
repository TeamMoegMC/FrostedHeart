/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package com.teammoeg.frostedheart.content.climate.player;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.ISlotType;

/** Discovers equipped heating capabilities and lets each item add watts. */
public final class PlayerEquipmentHeating {
    private PlayerEquipmentHeating() {
    }

    static void collect(ServerPlayer player, HeatingDeviceContext context) {
        if (CompatModule.isCuriosLoaded()) {
            for (Pair<ISlotType, ItemStack> entry
                    : CuriosCompat.getAllCuriosAndSlotsIfVisible(player)) {
                apply(context.curiosSlot(entry.getFirst()), entry.getSecond(), context);
            }
        }
        for (EquipmentSlot slot : HeatingDeviceSlot.EQUIPMENT_SLOTS) {
            apply(HeatingDeviceSlot.vanilla(slot), player.getItemBySlot(slot), context);
        }
    }

    private static void apply(HeatingDeviceSlot slot, ItemStack stack, HeatingDeviceContext context) {
        BodyHeatingCapability heating = FHCapabilities.EQUIPMENT_HEATING
                .getCapability(stack).orElse(null);
        if (heating != null) {
            heating.tickHeating(slot, stack, context);
        }
    }
}
