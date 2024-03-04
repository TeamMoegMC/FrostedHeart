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

package com.teammoeg.frostedheart.content.research.blocks;

import com.teammoeg.frostedheart.base.item.FHBaseItem;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FHBasePen extends FHBaseItem implements IPen {

    public FHBasePen(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canUse(PlayerEntity e, ItemStack stack, int val) {
        return true;
    }

    @Override
    public void doDamage(PlayerEntity e, ItemStack stack, int val) {
        stack.damageItem(val, e, ex -> {
        });
    }

    @Override
    public int getLevel(ItemStack is, PlayerEntity player) {
        return 0;
    }

}
