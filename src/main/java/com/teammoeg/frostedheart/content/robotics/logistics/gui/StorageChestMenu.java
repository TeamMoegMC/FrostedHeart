package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.StorageTileEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.IItemHandler;

public class StorageChestMenu extends LogisticChestMenu<StorageTileEntity> {

	public StorageChestMenu(int pContainerId, StorageTileEntity be, Inventory player, IItemHandler handler) {
		super(FHMenuTypes.STORAGE_CHEST.get(), be, pContainerId, player, handler);
		super.addPlayerInventory(player, 8, 87, 145);
	}

	public StorageChestMenu(int pContainerId, Inventory player, StorageTileEntity be) {
		super(FHMenuTypes.STORAGE_CHEST.get(), be, pContainerId, player);
		super.addPlayerInventory(player, 8, 87, 145);
	}

}
