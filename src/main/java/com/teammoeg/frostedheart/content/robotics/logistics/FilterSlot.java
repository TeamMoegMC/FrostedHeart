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

package com.teammoeg.frostedheart.content.robotics.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticRequestTask;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FilterSlot {
	public static final Codec<FilterSlot> CODEC=RecordCodecBuilder.create(t->t.group(
		ItemStack.CODEC.fieldOf("filter").forGetter(o->o.filter),
		Codec.BOOL.fieldOf("strict").forGetter(o->o.strictNBT)).apply(t,FilterSlot::new));
	ItemStack filter=ItemStack.EMPTY;
	boolean strictNBT;
	public FilterSlot() {
	}
	public FilterSlot(ItemStack filter, boolean strictNBT) {
		super();
		this.filter = filter;
		this.strictNBT = strictNBT;
	}
	public boolean isValidFor(ItemStack stack) {
		if(filter.isEmpty())return true;
		return ((!strictNBT)||ItemStack.isSameItemSameTags(stack, filter))&&ItemStack.isSameItem(stack,filter);
	}
	public boolean isEmpty() {
		return filter.isEmpty();
	}
	public LogisticRequestTask createTask(BlockEntity target,int size) {
		return new LogisticRequestTask(filter, size, strictNBT, target);
	}
}
