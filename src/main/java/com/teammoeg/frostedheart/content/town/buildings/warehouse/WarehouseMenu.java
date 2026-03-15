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
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.network.WarehouseUpdatePacket;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Supplier;

public class WarehouseMenu extends CBlockEntityMenu<WarehouseBlockEntity> {
	private final Supplier<Map<SimpleItemKey, Long>> serverSource;
	private Map<SimpleItemKey, Long> previousAvailableStacks = Collections.emptyMap();
	public final List<VirtualItemStack> clientItemList = new ArrayList<>();
	private final Map<SimpleItemKey, VirtualItemStack> clientItemMap = new HashMap<>();
	private boolean isFirstSync = true;
	private final Player player;

	public WarehouseMenu(int id, Inventory inventoryPlayer, WarehouseBlockEntity tile) {
		super(FHMenuTypes.WAREHOUSE.get(), tile, id, inventoryPlayer.player, 32);
		this.player = inventoryPlayer.player;

		if (this.player instanceof ServerPlayer serverPlayer) {
			this.serverSource = () -> {
				TeamTown town = TeamTown.from(serverPlayer);
				if (town == null) return Collections.emptyMap();
				return town.getResourceHolder().getVirtualItemMap();
			};

		} else {
			this.serverSource = Collections::emptyMap;
		}

		super.addPlayerInventory(inventoryPlayer, 8, 140, 197);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		if (this.player instanceof ServerPlayer serverPlayer) {
			try {
				detectAndSendChanges(serverPlayer);
			} catch (Exception e) {
				FHMain.LOGGER.warn("Failed to send incremental inventory update to client");
			}
		}
	}

	private void detectAndSendChanges(ServerPlayer serverPlayer) {
		Map<SimpleItemKey, Long> current = this.serverSource.get();
		if (current == this.previousAvailableStacks) {
			return;
		}
		if (current == null || (current.isEmpty() && previousAvailableStacks.isEmpty())) {
			return;
		}
		List<VirtualItemStack> changes = new ArrayList<>();

		if (isFirstSync) {
			for (var entry : current.entrySet()) {
				if (entry.getValue() > 0) {
					changes.add(new VirtualItemStack(entry.getKey(), entry.getValue()));
				}
			}
			if (!changes.isEmpty()) {
				FHNetwork.INSTANCE.sendPlayer(serverPlayer, new WarehouseUpdatePacket(changes, false));
			}
			this.isFirstSync = false;
			this.previousAvailableStacks = current;

		} else {
			//增量更新逻辑
			//找出数量发生变化但还存在的物品
			for (var entry : current.entrySet()) {
				long currentCount = entry.getValue();
				long prevCount = previousAvailableStacks.getOrDefault(entry.getKey(), 0L);
				if (currentCount != prevCount) {
					changes.add(new VirtualItemStack(entry.getKey(), currentCount));
				}
			}
			//找出之前有，现在没有的物品
			for (SimpleItemKey key : previousAvailableStacks.keySet()) {
				if (!current.containsKey(key)) {
					changes.add(new VirtualItemStack(key, 0));
				}
			}

			if (!changes.isEmpty()) {
				FHNetwork.INSTANCE.sendPlayer(serverPlayer, new WarehouseUpdatePacket(changes, true));
				this.previousAvailableStacks = current;
			}
		}
	}

	public void updateResourceList(List<VirtualItemStack> changes, boolean isIncremental) {
		if (!isIncremental) {
			//全量模式
			this.clientItemList.clear();
			this.clientItemMap.clear();

			for (VirtualItemStack vStack : changes) {
				this.clientItemList.add(vStack);
				this.clientItemMap.put(SimpleItemKey.from(vStack), vStack);
			}
		} else {
			//增量模式
			for (VirtualItemStack change : changes) {
				SimpleItemKey key = SimpleItemKey.from(change);
				VirtualItemStack existing = this.clientItemMap.get(key);

				if (change.getAmount() <= 0) {
					if (existing != null) {
						this.clientItemMap.remove(key);
						this.clientItemList.remove(existing);
					}
				} else {
					if (existing != null) {
						existing.setAmount(change.getAmount());
					} else {
						this.clientItemMap.put(key, change);
						this.clientItemList.add(change);
					}
				}
			}
		}

		//排序
		this.clientItemList.sort(Comparator.comparingLong(VirtualItemStack::getAmount).reversed());
	}

	public List<VirtualItemStack> getResources() {
		return this.clientItemList;
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
				if (town == null) return ItemStack.EMPTY;
				//构建存入 Action
				TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(
						slotStack,
						ResourceActionType.ADD,
						ResourceActionMode.MAXIMIZE
				);

				var result = executor.execute(action);
				ItemStack itemLeft = result.itemStackLeft();
				if(!result.itemStackModified().isEmpty()){
					slot.set(itemLeft);
					slot.setChanged();
				}
			}

			if (slotStack.getCount() == originalStack.getCount()) {
				return ItemStack.EMPTY;
			}
		}

		return originalStack;
	}
}
