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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.network.WarehouseS2CPacket;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class WarehouseMenu extends CBlockEntityMenu<WarehouseBlockEntity> {
	private List<VirtualItemStack> ResourceClientCache = List.of();

	public WarehouseMenu(int id, Inventory inventoryPlayer, WarehouseBlockEntity tile) {
		super(FHMenuTypes.WAREHOUSE.get(),tile,id, inventoryPlayer.player,32);

		super.addPlayerInventory(inventoryPlayer, 8, 139, 197);
	}

	@Override
	public void receiveMessage(short btnId, int state) {
		switch(btnId) {
		}
	}
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack originalStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);

		if (slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			originalStack = slotStack.copy();

			if (!player.level().isClientSide) {
				TeamTown town = TeamTown.from(player);
                IActionExecutorHandler executor = town.getActionExecutorHandler();
                //构建存入 Action
				TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(
						slotStack,
						ResourceActionType.ADD,
						ResourceActionMode.MAXIMIZE
				);

                var result = (TownResourceActionResults.ItemStackActionResult) executor.execute(action);
				ItemStack itemLeft = result.itemStackLeft();
				if(!result.itemStackModified().isEmpty()){
					slot.set(itemLeft);
					slot.setChanged();
					List<VirtualItemStack> list = VirtualItemStack.toClientVisualList(town.getResourceHolder().getAllItems());
					FHNetwork.INSTANCE.sendPlayer((ServerPlayer) player, new WarehouseS2CPacket(list));
                }
            }

			if (slotStack.getCount() == originalStack.getCount()) {
				return ItemStack.EMPTY;
			}
		}

		return originalStack;
	}
	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
	}

	public void updateResourceList(List<VirtualItemStack> newResources) {
		this.ResourceClientCache=List.copyOf(newResources);
	}

	public List<VirtualItemStack> getResources() {
		return ResourceClientCache;
	}
}
