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

package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 可穿戴热库物品的窄合同。
 * <p>
 * Narrow item contract for an immutable thermal profile and the ItemStack-owned
 * version-one thermal state. It deliberately does not expose a Forge capability.
 */
public interface WearableThermalReservoir {
    WearableThermalProfile thermalProfile(ItemStack stack);

    default double capacityRatio(ItemStack stack) {
        return thermalProfile(stack).capacityRatio();
    }

    default double surfaceCapacityFraction(ItemStack stack) {
        return thermalProfile(stack).surfaceCapacityFraction();
    }

    default double coreSurfaceTransferRatePerSecond(ItemStack stack) {
        return thermalProfile(stack).coreSurfaceTransferRatePerSecond();
    }

    default double playerTransferRatePerSecond(ItemStack stack) {
        return thermalProfile(stack).playerTransferRatePerSecond();
    }

    default Optional<WearableThermalState> thermalState(ItemStack stack) {
        return WearableThermalState.read(stack);
    }

    default Optional<WearableThermalState> restoreOrInitializeThermalStateForServer(
            ItemStack stack,
            double serverEnvironmentTemperatureC
    ) {
        return WearableThermalState.restoreOrInitializeForServer(
                stack, serverEnvironmentTemperatureC);
    }

    default void setTemperaturesC(
            ItemStack stack,
            double coreTemperatureC,
            double surfaceTemperatureC
    ) {
        new WearableThermalState(coreTemperatureC, surfaceTemperatureC).writeTo(stack);
    }
}
