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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * A ghost-slot configuration for one warehouse interface inventory slot.
 * Matching is exact and includes the complete item NBT.
 */
public record WarehouseInterfaceTarget(SimpleItemKey key, int amount) {
    public static final Codec<WarehouseInterfaceTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SimpleItemKey.CODEC.fieldOf("key").forGetter(WarehouseInterfaceTarget::key),
            Codec.INT.fieldOf("amount").forGetter(WarehouseInterfaceTarget::amount)
    ).apply(instance, WarehouseInterfaceTarget::new));

    public WarehouseInterfaceTarget {
        Objects.requireNonNull(key);
        int maxStackSize = key.toStack(1).getMaxStackSize();
        amount = Mth.clamp(amount, 1, Math.max(1, maxStackSize));
    }

    public static WarehouseInterfaceTarget fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("A warehouse interface target cannot be created from an empty stack");
        }
        return new WarehouseInterfaceTarget(SimpleItemKey.from(stack), stack.getCount());
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && key.matches(stack);
    }

    public ItemStack getDisplayStack() {
        return key.toStack(amount);
    }

    public WarehouseInterfaceTarget withAmount(int newAmount) {
        return new WarehouseInterfaceTarget(key, newAmount);
    }
}
