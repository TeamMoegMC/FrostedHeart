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

package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticChest;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.RequestLogisticChest;
import com.teammoeg.frostedheart.content.robotics.logistics.gui.SupplierChestMenu;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticPushTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

@SuppressWarnings("unused")
public class SupplierTileEntity extends LogisticProviderBlockEntity implements MenuProvider,ILogisticProvider {

	

	
	public SupplierTileEntity(BlockPos pos,BlockState bs) {
		super(FHBlockEntityTypes.SUPPLIER_CHEST.get(),pos,bs,27,new RequestLogisticChest(null,pos));
	}


	LazyTickWorker pushWorker=new LazyTickWorker(10,()->{
		if(networks.isEmpty())
			return;
		
		for(int slot=0;slot<getContainer().getSlots();slot++) {
			if(getContainer().getStackInSlot(slot).isEmpty())
				continue;
			LogisticTaskKey key=getKeys().get(slot).get();
			for(LazyOptional<LogisticNetwork> lln:networks) {
				LogisticNetwork logisticNetwork=lln.orElse(null);
				if(logisticNetwork!=null) {
					if(logisticNetwork.canAddTask(key))
						logisticNetwork.addTask(key,new LogisticPushTask(worldPosition,grid.cast(),slot));
				}
			}
		}
	});

	@Override
	public void tick() {
		super.tick();
		if(!this.level.isClientSide) {
			if(!networks.isEmpty()) {
				for(int i=0;i<getContainer().getSlots();i++){
					if(!getContainer().getStackInSlot(i).isEmpty()) {
						uplinkStatus=2;
						break;
					}
				}
			}
			pushWorker.tick();
		}
	}
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new SupplierChestMenu(pContainerId,this,pPlayerInventory,getContainer());
	}
	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap==ForgeCapabilities.ITEM_HANDLER)
			return grid.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}

}
