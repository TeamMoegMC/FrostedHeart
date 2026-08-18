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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.LogisticInterfaceChestInTileEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.IItemHandler;

public class LogisticInterfaceChestInMenu extends LogisticChestMenu<LogisticInterfaceChestInTileEntity> {

	public LogisticInterfaceChestInMenu(int pContainerId, LogisticInterfaceChestInTileEntity be, Inventory player, IItemHandler handler) {
		super(FHMenuTypes.LOGISTIC_INTERFACE_CHEST_IN.get(), be, pContainerId, player, handler);
		super.addPlayerInventory(player, 8, 87, 145);
	}

	public LogisticInterfaceChestInMenu(int pContainerId, Inventory player, LogisticInterfaceChestInTileEntity be) {
		super(FHMenuTypes.LOGISTIC_INTERFACE_CHEST_IN.get(), be, pContainerId, player);
		super.addPlayerInventory(player, 8, 87, 145);
	}

}
