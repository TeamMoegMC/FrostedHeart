/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.world.item.ItemStack;

public class FUtils {
    public static ItemStack ArmorLiningNBT(ItemStack stack) {
        stack.getOrCreateTag().putString("inner_cover", FHMain.MODID + ":straw_lining");
        stack.getTag().putBoolean("inner_bounded", true);//bound lining to arm or
        return CUtils.ArmorNBT(stack, 107, 6);
    }
}
