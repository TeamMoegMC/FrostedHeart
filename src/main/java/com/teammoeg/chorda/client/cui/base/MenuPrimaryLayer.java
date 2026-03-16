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

package com.teammoeg.chorda.client.cui.base;


import javax.annotation.Nullable;

import com.teammoeg.chorda.client.ClientUtils;

import lombok.Getter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * 带容器菜单的主层，扩展PrimaryLayer以支持Minecraft容器菜单（背包/工作台等）交互。
 * 自动处理物品槽位的悬停工具提示显示。
 * <p>
 * Menu-aware primary layer extending PrimaryLayer to support Minecraft container menu
 * (inventory/crafting table etc.) interactions. Automatically handles item slot hover
 * tooltip display.
 *
 * @param <T> 容器菜单类型 / The container menu type
 */
public class MenuPrimaryLayer<T extends AbstractContainerMenu> extends PrimaryLayer {
	@Getter
	protected T menu;
	public MenuPrimaryLayer(T container) {
		this.menu=container;
	}
	@Nullable
	public Slot getSlotUnderMouse() {
		Screen screen=this.getScreen().getScreen();
		if(screen instanceof AbstractContainerScreen acs)
			return acs.getSlotUnderMouse();
		return null;
	}
	@Override
	public void getTooltip(TooltipBuilder list) {
		@Nullable
		Slot slotUnderMouse = getSlotUnderMouse();
		if (this.menu.getCarried().isEmpty() && slotUnderMouse != null && slotUnderMouse.hasItem()) {
			AbstractContainerScreen.getTooltipFromItem(ClientUtils.getMc(), slotUnderMouse.getItem()).forEach(list::accept);
		}
		super.getTooltip(list);
	}

}
