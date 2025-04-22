package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.IItemHandler;

public class SupplierChestMenu extends LogisticChestMenu {

	public SupplierChestMenu(int pContainerId, Inventory player, IItemHandler handler) {
		super(FHMenuTypes.SUPPLY_CHEST.get(), pContainerId, player, handler);
		// TODO Auto-generated constructor stub
	}

	public SupplierChestMenu(int pContainerId, Inventory player, FriendlyByteBuf data) {
		super(FHMenuTypes.SUPPLY_CHEST.get(), pContainerId, player);
		// TODO Auto-generated constructor stub
	}

}
