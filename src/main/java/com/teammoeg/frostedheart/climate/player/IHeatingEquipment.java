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

package com.teammoeg.frostedheart.climate.player;

import net.minecraft.item.ItemStack;

/**
 * Interface IHeatingEquipment.
 * Interface for Heating Equipment Item
 *
 * @author khjxiaogu
 * file: IHeatingEquipment.java
 * @date 2021年9月14日
 */
public interface IHeatingEquipment {

    /**
     * Compute new body temperature.<br>
     *
     * @param stack           the stack<br>
     * @param bodyTemp        the body temp<br>
     * @param environmentTemp the environment temp<br>
     * @return returns body temperature change
     */
    float compute(ItemStack stack, float bodyTemp, float environmentTemp);

    /**
     * get max temperature delta.<br>
     *
     * @param stack the stack<br>
     * @return returns max temperature delta
     */
    float getMax(ItemStack stack);

    default boolean canHandHeld() {
        return false;
    }
}
