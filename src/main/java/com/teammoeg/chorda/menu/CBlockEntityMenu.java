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

import com.teammoeg.chorda.client.cui.menu.DeactivatableSlot;

import blusunrize.immersiveengineering.common.gui.BlockEntityInventory;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
/**
 * 方块实体菜单的基类。将菜单与特定的 {@link BlockEntity} 关联，
 * 自动处理物品栏初始化和验证逻辑。使用 {@link CCustomMenuSlot} 进行数据同步。
 * <p>
 * Base class for block entity menus. Associates a menu with a specific
 * {@link BlockEntity}, automatically handling inventory initialization
 * and validation logic. Use {@link CCustomMenuSlot} for data synchronization.
 *
 * @param <T> 方块实体类型 / the block entity type
 */
public abstract class CBlockEntityMenu<T extends BlockEntity> extends CBaseMenu {
	protected T blockEntity;
	public Container inv;


	/**
	 * 获取关联的方块实体。
	 * <p>
	 * Gets the associated block entity.
	 *
	 * @return 方块实体 / the block entity
	 */
	public T getBlock() {
		return blockEntity;
	}

	protected CBlockEntityMenu(MenuType<?> pMenuType, T blockEntity, int pContainerId, Player player, int inv_start) {
		super(pMenuType, pContainerId,player, inv_start);
		if (blockEntity instanceof IIEInventory)
			inv = new BlockEntityInventory(blockEntity, this);
		else if (blockEntity instanceof Container cont)
			inv = cont;
		this.blockEntity = blockEntity;

	}

	@Override
	protected Validator buildValidator(Validator builder) {
		return super.buildValidator(builder).blockEntity(blockEntity);
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return !blockEntity.isRemoved();
	}
	/**
	 * 绑定服务端逻辑。子类可覆写以在服务端初始化时执行额外操作。
	 * <p>
	 * Binds server-side logic. Subclasses can override to perform additional
	 * operations during server-side initialization.
	 *
	 * @return 当前菜单实例 / the current menu instance
	 */
	public CBlockEntityMenu<T> bindServer(){
		
		return this;
	}
}