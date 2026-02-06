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

package com.teammoeg.frostedheart.content.energy.wind;

import com.teammoeg.frostedheart.content.energy.wind.VAWTBlock.VAWTType;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class VAWTItem extends BlockItem {
	@Override
	public boolean isDamageable(ItemStack stack) {
		return true;
	}

	VAWTType type;
	public VAWTItem(Block block, Properties props,VAWTType type) {
		super(block, props);
		this.type=type;
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		return type.getDurability();
	}

}
