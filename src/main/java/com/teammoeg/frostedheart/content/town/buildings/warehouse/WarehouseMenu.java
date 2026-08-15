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

	// ---- 客户端视图状态（搜索过滤 + 排序），仅客户端 GUI 使用 ----
	// 过滤排序后的视图缓存，getResources() 惰性重建，避免每帧重复排序
	private final List<VirtualItemStack> clientViewList = new ArrayList<>();
	private WarehouseSortMode sortMode = WarehouseSortMode.AMOUNT_DESC;
	private String searchText = "";
	private boolean viewDirty = true;

	public WarehouseMenu(int id, Inventory inventoryPlayer, WarehouseBlockEntity tile) {
		super(FHMenuTypes.WAREHOUSE.get(), tile, id, inventoryPlayer.player, 32);
		this.player = inventoryPlayer.player;
		//获得城镇资源信息
		if (this.player instanceof ServerPlayer serverPlayer) {
			this.serverSource = () -> {
				if (!canAccessWarehouse()) return Collections.emptyMap();
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

		//主数据已变化，标记视图需要重建（过滤+排序在 getResources() 中惰性执行）
		this.viewDirty = true;
	}

	/**
	 * 获取过滤并排序后的客户端物品视图。
	 * 仅当主数据、搜索词或排序模式变化时才重建，渲染帧内多次调用零开销。
	 */
	public List<VirtualItemStack> getResources() {
		if (viewDirty) {
			rebuildViewList();
			viewDirty = false;
		}
		return clientViewList;
	}

	private void rebuildViewList() {
		clientViewList.clear();
		String query = searchText.trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			clientViewList.addAll(clientItemList);
		} else {
			for (VirtualItemStack vStack : clientItemList) {
				if (vStack.getLowercaseName().contains(query)) {
					clientViewList.add(vStack);
				}
			}
		}
		clientViewList.sort(sortMode.comparator());
	}

	public WarehouseSortMode getSortMode() {
		return sortMode;
	}

	public void setSortMode(WarehouseSortMode sortMode) {
		if (sortMode != null && sortMode != this.sortMode) {
			this.sortMode = sortMode;
			this.viewDirty = true;
		}
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		String newText = searchText == null ? "" : searchText;
		if (!newText.equals(this.searchText)) {
			this.searchText = newText;
			this.viewDirty = true;
		}
	}


	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (!canAccessWarehouse()) {
			return ItemStack.EMPTY;
		}
		ItemStack originalStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);

		if (slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			originalStack = slotStack.copy();

			if (!player.level().isClientSide) {
				TeamTown town = TeamTown.from(player);
				if (town == null) return ItemStack.EMPTY;
				IActionExecutorHandler executor = town.getActionExecutorHandler();
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

	public boolean hasBuilding() {
		return blockEntity.getBuilding().isPresent();
	}

	/**
	 * Server-authoritative access guard for every warehouse inventory path.
	 * A stale menu may outlive its removed block entity for part of a tick, but
	 * it must not expose the shared town resource pool during that window.
	 */
	public boolean canAccessWarehouse() {
		return !blockEntity.isRemoved() && hasBuilding();
	}

	public boolean isWorkable() {
		return blockEntity.getBuilding().map(WarehouseBuilding::isBuildingWorkable).orElse(false);
	}

	public boolean isInitialized() {
		return blockEntity.getBuilding().map(WarehouseBuilding::isInitialized).orElse(false);
	}

	public boolean isStructureValid() {
		return blockEntity.getBuilding().map(WarehouseBuilding::isStructureValid).orElse(false);
	}

	public boolean isAreaOverlapped() {
		return blockEntity.getBuilding().map(WarehouseBuilding::isOccupiedAreaOverlapped).orElse(false);
	}

	public int getVolume() {
		return blockEntity.getBuilding().map(WarehouseBuilding::getVolume).orElse(0);
	}

	public int getArea() {
		return blockEntity.getBuilding().map(WarehouseBuilding::getArea).orElse(0);
	}

	public double getCapacity() {
		return blockEntity.getBuilding().map(WarehouseBuilding::getCapacity).orElse(0.0);
	}
}
