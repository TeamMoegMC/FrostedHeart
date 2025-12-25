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

package com.teammoeg.chorda.client;

import java.util.List;

import net.minecraft.network.chat.HoverEvent.ItemStackInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Lazy;
/*
 * client only event to display multiple items
 * This is a hack and can not be sent by network
 * */
public class MultipleItemHoverEvent extends ItemStackInfo {
	List<Lazy<ItemStack>> stacks;
	public MultipleItemHoverEvent(List<Lazy<ItemStack>> pStack) {
		super(ItemStack.EMPTY);
		stacks=pStack;
	}
	@Override
	public ItemStack getItemStack() {
		return stacks.get((int) ((System.currentTimeMillis()/1000)%stacks.size())).get();
	}
	

}
