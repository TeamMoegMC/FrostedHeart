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

package com.teammoeg.chorda.menu;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
/**
 * 多方块菜单的服务端工厂接口。用于在服务端创建带有多方块上下文的菜单实例。
 * <p>
 * Server-side factory interface for multiblock menus. Used to create menu instances
 * with multiblock context on the server side.
 *
 * @param <T> 多方块状态类型 / the multiblock state type
 * @param <C> 菜单类型 / the menu type
 */
@FunctionalInterface
public interface MultiBlockMenuServerFactory<T extends IMultiblockState,C extends AbstractContainerMenu>
{
	C create(MenuType<C> type, int windowId, Inventory inventoryPlayer, MultiblockMenuContext<T> te);
}