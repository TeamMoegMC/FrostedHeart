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

package com.teammoeg.frostedheart.content.utility.vawt;

import com.teammoeg.frostedheart.content.utility.vawt.VAWTBlock.VAWTType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class VAWTItem extends BlockItem {
    private final VAWTType type;

    public VAWTItem(Block block, Properties props, VAWTType type) {
        super(block, props);
        this.type = type;
    }

    public static long getWear(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null) return 0;
        return Math.max(0L, tag.contains("VAWTWear") ? tag.getLong("VAWTWear") : tag.getLong("Damage"));
    }

    public static void setWear(ItemStack stack, long wear) {
        stack.getOrCreateTag().putLong("VAWTWear", Math.max(0L, wear));
        stack.getOrCreateTag().remove("Damage");
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getWear(stack) > 0;
    }

    private float remainingFraction(ItemStack stack) {
        long maximum = type.getDurability();
        return maximum <= 0 ? 0 : (float) Math.max(0D, 1D - (double) getWear(stack) / maximum);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * remainingFraction(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return net.minecraft.util.Mth.hsvToRgb(remainingFraction(stack) / 3F, 1F, 1F);
    }
}
