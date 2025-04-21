package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.IItemHandler;

public class StorageChestMenu extends LogisticChestMenu {

	public StorageChestMenu(int pContainerId, Inventory player, IItemHandler handler) {
		super(FHMenuTypes.STORAGE_CHEST.get(), pContainerId, player, handler);
		// TODO Auto-generated constructor stub
	}

	public StorageChestMenu(int pContainerId, Inventory player, FriendlyByteBuf data) {
		super(FHMenuTypes.STORAGE_CHEST.get(), pContainerId, player);
		// TODO Auto-generated constructor stub
	}

}
