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

package com.teammoeg.frostedheart.content;

import net.minecraft.item.Food;

public class FHFoods {
    public static final Food VEGETABLE_SAWDUST_SOUP = buildStew(6); // 掺杂了木屑的蔬菜汤
    public static final Food RYE_SAWDUST_PORRIDGE = buildStew(6); // 掺杂了木屑的黑麦面糊
    public static final Food RYE_BREAD = (new Food.Builder()).hunger(5).saturation(0.6F).build();

    private static Food buildStew(int hunger) {
        return (new Food.Builder()).hunger(hunger).saturation(0.6F).build();
    }
}
