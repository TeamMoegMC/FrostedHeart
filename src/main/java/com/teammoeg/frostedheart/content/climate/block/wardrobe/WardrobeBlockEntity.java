/*
 * Copyright (c) 2026 TeamMoeg
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

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.capability.capabilities.ChangeDetectedItemHandler;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class WardrobeBlockEntity extends CBlockEntity implements MenuProvider  {
	public static final int NUM_INVENTORY=3;
	ChangeDetectedItemHandler[] invs=new ChangeDetectedItemHandler[NUM_INVENTORY];

    public WardrobeBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WARDROBE.get(), pos, state);
        int countSlot=5;
        for(BodyPart bp:BodyPart.values()) {
        	countSlot+=bp.slotNum;
        }
        for(int i=0;i<invs.length;i++) {
        	invs[i]=ChangeDetectedItemHandler.fromBESetChanged(this, new ItemStackHandler(countSlot) {

				@Override
				public int getSlotLimit(int slot) {
					return 1;
				}
        		
        	});
        }
    }
    public Component getDisplayName() {
        return Component.translatable("container.wardrobe");
    }
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
    	
        return new WardrobeMenu(
            id,
            playerInventory,
            this
        );
    }
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		if(!descPacket)
			for(int i=0;i<invs.length;i++)
				invs[i].deserializeNBT(nbt.getCompound("inv"+i));
				
		
	}




	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		if(!descPacket)
			for(int i=0;i<invs.length;i++)
				nbt.put("inv"+i, invs[i].serializeNBT());
		
	}


}