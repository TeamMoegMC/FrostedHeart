package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.SupplierTileEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.IItemHandler;

public class SupplierChestMenu extends LogisticChestMenu<SupplierTileEntity> {

	public SupplierChestMenu(int pContainerId, SupplierTileEntity be, Inventory player, IItemHandler handler) {
		super(FHMenuTypes.SUPPLY_CHEST.get(), be, pContainerId, player, handler);
		super.addPlayerInventory(player, 8, 87, 145);
	}

	public SupplierChestMenu(int pContainerId, Inventory player, SupplierTileEntity be) {
		super(FHMenuTypes.SUPPLY_CHEST.get(), be, pContainerId, player);
		super.addPlayerInventory(player, 8, 87, 145);
	}

}
