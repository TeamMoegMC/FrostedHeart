package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.IItemHandler;

public class RequesterChestMenu extends LogisticChestMenu {

	public RequesterChestMenu( int pContainerId, Inventory inv, FriendlyByteBuf data) {
		super(FHMenuTypes.REQUEST_CHEST.get(), pContainerId, inv);
		// TODO Auto-generated constructor stub
	}

	public RequesterChestMenu(int pContainerId, Inventory inv, IItemHandler handler) {
		super(FHMenuTypes.REQUEST_CHEST.get(), pContainerId, inv, handler);
		// TODO Auto-generated constructor stub
	}

}
