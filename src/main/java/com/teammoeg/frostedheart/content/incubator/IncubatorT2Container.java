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

package com.teammoeg.frostedheart.content.incubator;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.menu.slots.UIFluidTank;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;

import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class IncubatorT2Container extends CBlockEntityMenu<HeatIncubatorTileEntity> {
	UIFluidTank tankin=new UIFluidTank(this,6000);
	UIFluidTank tankout=new UIFluidTank(this,6000);
	CDataSlot<Float> process=CCustomMenuSlot.SLOT_FIXED.create(this);
	CDataSlot<Float> fuel=CCustomMenuSlot.SLOT_FIXED.create(this);
	CDataSlot<Float> efficiency=CCustomMenuSlot.SLOT_FIXED.create(this);
	CDataSlot<Float> heat=CCustomMenuSlot.SLOT_FIXED.create(this);
	CDataSlot<Boolean> isFoodRecipe=CCustomMenuSlot.SLOT_BOOL.create(this);
    public IncubatorT2Container(int id, Inventory inventoryPlayer, HeatIncubatorTileEntity tile) {
    	this(id,inventoryPlayer,tile,false);
    }
    public IncubatorT2Container(int id, Inventory inventoryPlayer, HeatIncubatorTileEntity tile,boolean isServer) {
        super(FHMenuTypes.INCUBATOR_T2.get(), tile, id,inventoryPlayer.player, 4);
        if(isServer) {
        	tankin.bind(tile.fluid[0]);
        	tankout.bind(tile.fluid[1]);
        	process.bind(()->tile.process*1f/tile.processMax);
        	fuel.bind(()->tile.fuel*1f/tile.fuelMax);
        	efficiency.bind(()->tile.efficiency);
        	heat.bind(()->Mth.clamp(tile.network.getHeat()/tile.network.getMaxIntake(), 0, 1));
        }
        /*this.addSlot(new IESlot(this, this.inv, 0, 34, 52) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return tile.isStackValid(0, itemStack);
            }
        });*/
        this.addSlot(new IESlot(this, this.inv, 1, 65, 44) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(1, itemStack);
            }
        });
        this.addSlot(new IESlot(this, this.inv, 2, 65, 26) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(2, itemStack);
            }
        });
        this.addSlot(new IESlot.Output(this, this.inv, 3, 147, 35));

        this.addPlayerInventory(inventoryPlayer,8,84,142);
    }
}

