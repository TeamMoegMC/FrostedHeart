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

import javax.annotation.Nullable;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

// TODO: Auto-generated Javadoc

/**
 * Interface IWarmKeepingEquipment.
 * Interface for warmkeeping equipments
 *
 * @author khjxiaogu
 * file: IWarmKeepingEquipment.java
 * @date 2021年9月14日
 */
public interface IWarmKeepingEquipment {

    /**
     * returns warm keeping factor.
     * max factor is 1.
     *
     * @param pe    the player, null means get default
     * @param stack the stack<br>
     * @return factor<br>
     */
    float getFactor(@Nullable ServerPlayerEntity pe, ItemStack stack);
}
