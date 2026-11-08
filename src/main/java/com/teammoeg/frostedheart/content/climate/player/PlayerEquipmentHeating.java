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
        collect(player, context, false);
        collect(player, context, true);
    }

    private static void collect(ServerPlayer player, HeatingDeviceContext context, boolean controlled) {
        if (CompatModule.isCuriosLoaded()) {
            for (Pair<ISlotType, ItemStack> entry
                    : CuriosCompat.getAllCuriosAndSlotsIfVisible(player)) {
                apply(context.curiosSlot(entry.getFirst()), entry.getSecond(), context, controlled);
            }
        }
        for (EquipmentSlot slot : HeatingDeviceSlot.EQUIPMENT_SLOTS) {
            apply(HeatingDeviceSlot.vanilla(slot), player.getItemBySlot(slot), context, controlled);
        }
    }

    private static void apply(HeatingDeviceSlot slot, ItemStack stack, HeatingDeviceContext context,
                              boolean controlled) {
        BodyHeatingCapability heating = FHCapabilities.EQUIPMENT_HEATING
                .getCapability(stack).orElse(null);
        if (heating != null && heating.isControlled() == controlled) {
            heating.tickHeating(slot, stack, context);
        }
    }
}
