package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.chorda.capability.capabilities.ItemHandlerWrapper;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.menu.slots.ArmorSlot;
import com.teammoeg.chorda.menu.slots.ArmorSlotItemHandler;
import com.teammoeg.chorda.menu.slots.OffHandSlot;
import com.teammoeg.chorda.menu.slots.OffHandSlotItemHandler;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.block.LiningSlot;
import com.teammoeg.frostedheart.content.climate.block.wardrobe.WardrobeBlock;
import com.teammoeg.frostedheart.content.climate.block.wardrobe.WardrobeBlockEntity;
import com.teammoeg.frostedheart.content.climate.player.BodyPartData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class WareHouseMenu extends CBlockEntityMenu<WarehouseBlockEntity> {
	CDataSlot<Integer> page=CCustomMenuSlot.SLOT_INT.create(this);
	IItemHandler wrap;
	public WareHouseMenu(int id, Inventory inventoryPlayer, WarehouseBlockEntity tile) {
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
