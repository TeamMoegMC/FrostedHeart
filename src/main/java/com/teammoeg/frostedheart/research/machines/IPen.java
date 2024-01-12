/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.research.machines;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface IPen {

    boolean canUse(@Nullable PlayerEntity e, ItemStack stack, int val);

    void doDamage(@Nullable PlayerEntity e, ItemStack stack, int val);

    default boolean damage(@Nullable PlayerEntity e, ItemStack stack, int val) {
        if (canUse(e, stack, val)) {
            doDamage(e, stack, val);
            return true;
        }
        return false;
    }

    int getLevel(ItemStack is, @Nullable PlayerEntity player);

    default boolean tryDamage(@Nullable PlayerEntity e, ItemStack stack, int val, Supplier<Boolean> onsuccess) {
        if (canUse(e, stack, val)) {
            if (onsuccess.get()) {
                doDamage(e, stack, val);
                return true;
            }
        }
        return false;
    }
}
