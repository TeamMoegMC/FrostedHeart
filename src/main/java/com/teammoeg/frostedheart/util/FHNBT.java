/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.item.ItemStack;

public class FHNBT {
    public static final String FIRST_LOGIN_GIVE_MANUAL = "first";
    public static final String FIRST_LOGIN_GIVE_NUTRITION = FHMain.MODID + "first_login_give_nutrition";
    public static final String NBT_HEATER_VEST = FHMain.MODID + "heater_vest";

    public static ItemStack ArmorNBT(ItemStack stack) {
        stack.getOrCreateTag().putString("inner_cover", "buff_coat");
        return stack;
    }
}
