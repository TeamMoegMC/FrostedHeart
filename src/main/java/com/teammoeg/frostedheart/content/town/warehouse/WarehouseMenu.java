package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.network.WarehouseS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarehouseMenu extends CBlockEntityMenu<WarehouseBlockEntity> {

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
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
	}
}
