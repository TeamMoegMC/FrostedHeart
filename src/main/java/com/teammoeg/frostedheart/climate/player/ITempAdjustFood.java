/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.climate.player;

import net.minecraft.item.ItemStack;

/**
 * Interface IHotFood.
 * Interface for body warming consumables
 *
 * @author khjxiaogu
 * file: IHotFood.java
 * @date 2021年9月14日
 */
public interface ITempAdjustFood {

    /**
     * Get delta temperature this item would give.
     *
     * @param is the is<br>
     * @return heat<br>
     */
    float getHeat(ItemStack is, float env);

    ;

    /**
     * Get max temperature this item can get.
     *
     * @param is the stack<br>
     * @return max temp<br>
     */
    default float getMaxTemp(ItemStack is) {
        return 15;
    }

    ;

    /**
     * Get min temperature this item can get.
     *
     * @param is the stack<br>
     * @return max temp<br>
     */
    default float getMinTemp(ItemStack is) {
        return -15;
    }
}
