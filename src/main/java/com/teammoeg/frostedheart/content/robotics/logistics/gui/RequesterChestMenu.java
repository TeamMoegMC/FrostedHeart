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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuSlots;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.RequesterTileEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.IItemHandler;

public class RequesterChestMenu extends LogisticChestMenu<RequesterTileEntity> {
	public List<CDataSlot<Filter>> list=new ArrayList<>();
	public RequesterChestMenu(int pContainerId, Inventory inv,RequesterTileEntity blockEntity) {
		super(FHMenuTypes.REQUEST_CHEST.get(),blockEntity, pContainerId, inv);
		for(int i=0;i<blockEntity.filters.length;i++) {
			list.add(FHMenuSlots.FILTER_ENCODER_SLOT.create(this));
		}
		super.addPlayerInventory(inv, 8, 118, 176);
	}

	public RequesterChestMenu(int pContainerId,RequesterTileEntity blockEntity, Inventory inv, IItemHandler handler) {
		super(FHMenuTypes.REQUEST_CHEST.get(),blockEntity, pContainerId, inv, handler);
		for(int i=0;i<blockEntity.filters.length;i++) {
			CDataSlot<Filter> slot=FHMenuSlots.FILTER_ENCODER_SLOT.create(this);
			final int nslot=i;
			slot.bind(()->blockEntity.filters[nslot]);
			list.add(slot);
		}
		super.addPlayerInventory(inv, 8, 118, 176);
	}
	public void sendMessage(int cmdId,int slotId,int state) {
		this.sendMessage(((cmdId&0xff)<<8)+(slotId&0xff), state);
	}
	public void setFilterItem(int slot) {
		sendMessage(0,slot,0);
	}
	public void unsetFilterItem(int slot) {
		sendMessage(1,slot,0);
	}
	public void setFilterIgnoreNbt(int slot,boolean ignore) {
		sendMessage(2,slot,ignore?1:0);
	}
	public void setFilterSize(int slot,int size) {
		sendMessage(3,slot,size);
	}
	public void receiveMessage(short btnId, int state) {
		int cmdId=(btnId&0xff00)>>8;
		int slotId=btnId&0xff;
		System.out.println(cmdId+","+slotId+","+state);
		switch(cmdId) {
		case 0:this.blockEntity.filters[slotId]=new Filter(new ItemKey(this.getCarried()), true, this.getCarried().getCount());break;
		case 1:this.blockEntity.filters[slotId]=null;break;
		case 2:this.blockEntity.filters[slotId].setIgnoreNbt(state>0);break;
		case 3:this.blockEntity.filters[slotId].setSize(state);break;
		}
	}
}
