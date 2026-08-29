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

package com.teammoeg.frostedheart.content.climate.player;

import net.minecraft.world.item.ItemStack;

/**
 * Interface IHeatingEquipment.
 * Capability Interface for Dynamic Heating Equipment
 *
 * @author khjxiaogu
 * file: IHeatingEquipment.java
 * Date: 2021/9/14
 */
public interface BodyHeatingCapability {

    /**
     * Contribute explicit heating power to covered body parts.<br>
     *
     * @param slot            current slot<br>
     * @param stack           the stack<br>
     * @param data       	  player temperature data<br>
     */
    void tickHeating(HeatingDeviceSlot slot,ItemStack stack,HeatingDeviceContext data);
    
    /**
     * Get highest theoretical heating power in watts
     * For display only
     * */
    float getMaxPowerW(ItemStack stack);
    /**
    * Get lowest theoretical heating power in watts
     * For display only
     * */
    float getMinPowerW(ItemStack stack);
}
