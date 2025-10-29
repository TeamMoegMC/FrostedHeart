package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.IItemHandler;

public class WarehouseMenu extends CBlockEntityMenu<WarehouseBlockEntity> {
	CDataSlot<Integer> page=CCustomMenuSlot.SLOT_INT.create(this);
	IItemHandler wrap;
	public WarehouseMenu(int id, Inventory inventoryPlayer, WarehouseBlockEntity tile) {
		super(FHMenuTypes.WAREHOUSE.get(),tile,id, inventoryPlayer.player,32);

		super.addPlayerInventory(inventoryPlayer, 8, 84, 142);
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
}
