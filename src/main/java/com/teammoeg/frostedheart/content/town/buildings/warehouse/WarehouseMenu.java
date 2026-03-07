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
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Supplier;

public class WarehouseMenu extends CBlockEntityMenu<WarehouseBlockEntity> {
	private final Supplier<Map<SimpleItemKey, Long>> serverSource;
	private Map<SimpleItemKey, Long> previousAvailableStacks = new HashMap<>();
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

				Map<ItemStack, Double> rawMap = town.getResourceHolder().getAllItems();
				Map<SimpleItemKey, Long> convertedMap = new HashMap<>();
				for (Map.Entry<ItemStack, Double> entry : rawMap.entrySet()) {
					ItemStack rawStack = entry.getKey();
					Double rawAmount = entry.getValue();
					SimpleItemKey newKey = SimpleItemKey.from(rawStack);
					long newAmount = rawAmount.longValue();
					convertedMap.merge(newKey, newAmount, Long::sum);
				}
				return convertedMap;
			};
			this.previousAvailableStacks = new HashMap<>();

		} else {
			this.serverSource = Collections::emptyMap;
		}

		super.addPlayerInventory(inventoryPlayer, 8, 139, 197);
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
		Map<SimpleItemKey, Long> currentAvailableStacks = this.serverSource.get();
		if (currentAvailableStacks == null || currentAvailableStacks.isEmpty() && previousAvailableStacks.isEmpty()) {
			return;
		}

		List<VirtualItemStack> changesToSend = new ArrayList<>();

		if (isFirstSync) {
			for (Map.Entry<SimpleItemKey, Long> entry : currentAvailableStacks.entrySet()) {
				if (entry.getValue() > 0) {
					changesToSend.add(new VirtualItemStack(entry.getKey().toStack(1), entry.getValue()));
				}
			}

			if (!changesToSend.isEmpty()) {
				FHNetwork.INSTANCE.sendPlayer(serverPlayer, new WarehouseUpdatePacket(changesToSend, false));
			}
			this.isFirstSync = false;

		} else {
			//增量更新逻辑
			//找出数量发生变化但还存在的物品
			for (Map.Entry<SimpleItemKey, Long> entry : currentAvailableStacks.entrySet()) {
				SimpleItemKey key = entry.getKey();
				long currentCount = entry.getValue();
				long prevCount = previousAvailableStacks.getOrDefault(key, 0L);

				if (currentCount != prevCount) {
					changesToSend.add(new VirtualItemStack(key.toStack(1), currentCount));
				}
			}
			//找出之前有，现在没有的物品
			for (SimpleItemKey key : previousAvailableStacks.keySet()) {
				if (!currentAvailableStacks.containsKey(key)) {
					changesToSend.add(new VirtualItemStack(key.toStack(1), 0));
				}
			}

			if (!changesToSend.isEmpty()) {
				FHNetwork.INSTANCE.sendPlayer(serverPlayer,
						new WarehouseUpdatePacket(changesToSend, true));
			}

			this.previousAvailableStacks = currentAvailableStacks;
		}
	}

	@Override
	public void receiveMessage(short btnId, int state) {
		switch(btnId) {
		}
	}
	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
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

	public record SimpleItemKey(Item item, @Nullable CompoundTag tag) {
		public static SimpleItemKey from(ItemStack stack) {
			return new SimpleItemKey(stack.getItem(), stack.getTag());
		}

		public static SimpleItemKey from(VirtualItemStack vStack) {
			return from(vStack.getItemStack());
		}

		public ItemStack toStack(int count) {
			ItemStack s = new ItemStack(item, count);
			s.setTag(tag != null ? tag.copy() : null);
			return s;
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
				}
			}

			if (slotStack.getCount() == originalStack.getCount()) {
				return ItemStack.EMPTY;
			}
		}

		return originalStack;
	}
}
