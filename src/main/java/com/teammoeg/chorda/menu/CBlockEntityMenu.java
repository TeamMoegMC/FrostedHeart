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

package com.teammoeg.chorda.menu;

import blusunrize.immersiveengineering.common.gui.BlockEntityInventory;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
/**
 * use of blockentity in screens, use {@link CCustomMenuSlot} to serve as data synchronization instead
 * */
public abstract class CBlockEntityMenu<T extends BlockEntity> extends CBaseMenu {
	protected T blockEntity;
	public Container inv;


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
	public CBlockEntityMenu<T> bindServer(){
		
		return this;
	}
}