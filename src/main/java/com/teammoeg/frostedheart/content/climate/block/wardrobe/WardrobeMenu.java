/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.block.wardrobe;

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

public class WardrobeMenu extends CBlockEntityMenu<WardrobeBlockEntity> {
	CDataSlot<Integer> page=CCustomMenuSlot.SLOT_INT.create(this);
	IItemHandler wrap;
	public WardrobeMenu(int id, Inventory inventoryPlayer, WardrobeBlockEntity tile) {
		super(FHMenuTypes.WARDROBE.get(),tile,id, inventoryPlayer.player,28);
		//we don't actually switch inventory in client.
		if(inventoryPlayer.player.level().isClientSide)
			wrap=tile.invs[0];
		else
			wrap=new ItemHandlerWrapper(()->tile.invs[page.getValue()]);
		PlayerTemperatureData ptd = PlayerTemperatureData.getCapability(inventoryPlayer.player).resolve().get();
		int y0=6;
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.HEAD, inventoryPlayer, 39, 6, y0));
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.CHEST, inventoryPlayer, 38, 6, y0 + 18));
		this.addSlot(new OffHandSlot(inventoryPlayer.player, inventoryPlayer, 40, 6, y0 + 18 * 2));
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.LEGS, inventoryPlayer, 37, 6, y0 + 18 * 3));
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.FEET, inventoryPlayer, 36, 6, y0 + 18 * 4));
		for (int j = 0; j < 5; j++) {
			BodyPart bp=BodyPart.values()[j];
			BodyPartData clothes=ptd.clothesOfParts.get(bp);
			for (int k = 0; k < clothes.getSize(); ++k) {
				this.addSlot(new LiningSlot(inventoryPlayer.player,bp,
						clothes.clothes, k, 6+18 + k * 18, 6+18*j));
			}
		}
		y0=6;
		this.addSlot(new ArmorSlotItemHandler(inventoryPlayer.player, EquipmentSlot.HEAD, wrap, 3, 121, y0));
		this.addSlot(new ArmorSlotItemHandler(inventoryPlayer.player, EquipmentSlot.CHEST, wrap, 2, 121, y0 + 18));
		this.addSlot(new OffHandSlotItemHandler(inventoryPlayer.player, wrap, 4, 121, y0 + 18 * 2));
		this.addSlot(new ArmorSlotItemHandler(inventoryPlayer.player, EquipmentSlot.LEGS, wrap, 1, 121, y0 + 18 * 3));
		this.addSlot(new ArmorSlotItemHandler(inventoryPlayer.player, EquipmentSlot.FEET, wrap, 0, 121, y0 + 18 * 4));
		int slotOrder=4;
		for (int j = 0; j < 5; j++) {
			BodyPart bp=BodyPart.values()[j];
			
			for (int k = 0; k < bp.slotNum; ++k) {
				this.addSlot(new LiningSlot(inventoryPlayer.player,bp,wrap, ++slotOrder, 121+18 + k * 18, 6+18*j));
			}
		}
		/*for(int p=0;p<3;p++) {
	        for(int k=0;k<1;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.HEAD,blockEntity, 8*p+0+k, 100+k*18, 7,p));
	        }
	        for(int k=0;k<3;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.TORSO,blockEntity, 8*p+1+k, 100+k*18, 30,p));
	        }
	        for(int k=0;k<3;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.LEGS,blockEntity, 8*p+4+k, 100+k*18, 53,p));
	        }
	        for(int k=0;k<1;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.FEET,blockEntity, 8*p+7+k, 100+k*18, 76,p));
	        }
		}*/
		super.addPlayerInventory(inventoryPlayer, 18, 120, 178);
	}
	@Override
	public QuickMoveStackBuilder defineQuickMoveStack() {
		return QuickMoveStackBuilder.first(0,2).then(3,5).then(5,18).then(19,28).then(2).then(18);
	}

	public void swapSlots() {
		int tslots=wrap.getSlots();
		for(int i=0;i<tslots;i++) {
			Slot slot1=this.getSlot(i);
			Slot slot2=this.getSlot(tslots+i);
			
			if((slot1.mayPickup(getPlayer())||slot1.getItem().isEmpty())&&(slot2.mayPickup(getPlayer())||slot2.getItem().isEmpty())) {
				ItemStack stack1=slot1.getItem();
				ItemStack stack2=slot2.getItem();
				if((stack1.isEmpty()||slot2.mayPlace(stack1))&&(stack2.isEmpty()||slot1.mayPlace(stack2))) {
					slot2.set(stack1);
					slot1.set(stack2);
				}
			}
		}
	}
	@Override
	public void receiveMessage(short btnId, int state) {
		switch(btnId) {
		case 1:swapSlots();break;
		case 2:page.setValue(Mth.clamp(state, 0, blockEntity.invs.length-1));
		}
	}
	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
		if (pPlayer instanceof ServerPlayer) {
			WardrobeBlock.setOpened(blockEntity.getLevel(),blockEntity.getBlockPos(),blockEntity.getBlockState(),pPlayer,false);
		}
	}
}
