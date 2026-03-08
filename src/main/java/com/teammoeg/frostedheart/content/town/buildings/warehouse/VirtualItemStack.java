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

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class VirtualItemStack {
    private final SimpleItemKey key;
    private long amount;

    public VirtualItemStack(SimpleItemKey key, long amount) {
        this.key = key;
        this.amount = amount;
    }

    public SimpleItemKey getKey() {
        return key;
    }

    public ItemStack getDisplayStack() {
        return key.toStack(1);
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
