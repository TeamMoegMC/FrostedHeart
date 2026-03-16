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

package com.teammoeg.chorda.client;

import java.util.List;

import net.minecraft.network.chat.HoverEvent.ItemStackInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Lazy;
/**
 * 客户端专用的多物品悬停事件，用于在工具提示中循环显示多个物品。
 * 这是一个hack实现，不能通过网络发送。物品每秒自动轮换显示。
 * <p>
 * Client-only multiple item hover event for cycling through multiple items in tooltips.
 * This is a hack implementation and cannot be sent over the network.
 * Items are automatically rotated every second.
 */
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
