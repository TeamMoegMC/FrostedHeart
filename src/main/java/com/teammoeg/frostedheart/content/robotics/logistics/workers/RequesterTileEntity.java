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

import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.gui.RequesterChestMenu;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticRequestTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

public class RequesterTileEntity extends LogisticBlockEntity implements  MenuProvider {
	
	ItemStackHandler container=new ItemStackHandler(27) {
		@Override
		protected void onContentsChanged(int slot) {
			if(level!=null)
				RequesterTileEntity.this.setChanged();
		}
	};
	public LazyOptional<ItemStackHandler> grid=LazyOptional.of(()->container);
	public Filter[] filters=new Filter[9];
	public RequesterTileEntity(BlockPos pos,BlockState bs) {
		super(FHBlockEntityTypes.REQUESTER_CHEST.get(),pos,bs,9);

	}

	@Override
	public void readCustomNBT(CompoundTag arg0, boolean arg1) {
		container.deserializeNBT(arg0.getCompound("container"));
		ListTag list=arg0.getList("filters", Tag.TAG_COMPOUND);
		for(Tag t:list) {
			CompoundTag tag=(CompoundTag) t;
			filters[tag.getInt("slot")]=Filter.CODEC.decode(NbtOps.INSTANCE, tag.getCompound("filter")).getOrThrow(false,FHMain.LOGGER::info).getFirst();
		}
		
	}

	@Override
	public void writeCustomNBT(CompoundTag arg0, boolean arg1) {
		arg0.put("container", container.serializeNBT());
		ListTag list=new ListTag();
		for(int i=0;i<filters.length;i++) {
			if(filters[i]!=null) {
				CompoundTag tag=new CompoundTag();
				tag.putInt("slot", i);
				tag.put("filter", Filter.CODEC.encodeStart(NbtOps.INSTANCE, filters[i]).getOrThrow(false, FHMain.LOGGER::info));
				list.add(tag);
			}
		}
		arg0.put("filters", list);
	}
	LazyTickWorker worker=new LazyTickWorker(10,()->{
		if(!networks.isEmpty()) {
			boolean hasUplink=false;
			boolean hasRequest=false;
			for(int i=0;i<filters.length;i++) {
				Filter filter=filters[i];
				if(filter!=null&&filter.getKey()!=null) {
					int currcnt=0;
					int freeSpace=0;
					hasUplink=true;
					for(int j=0;j<container.getSlots();j++) {
						ItemStack stack=container.getStackInSlot(j);
						if(stack.isEmpty()) {
							freeSpace+=filter.getKey().getMaxStackSize();
						}else if(filter.matches(stack)) {
							currcnt+=stack.getCount();
							freeSpace+=stack.getMaxStackSize()-stack.getCount();
						}
					}
					int missing=Math.min(filter.getSize()-currcnt,freeSpace);
					if(missing>0) {
						LogisticTaskKey key=getKeys().get(i).get();
						for(LazyOptional<LogisticNetwork> lln:networks) {
							LogisticNetwork logisticNetwork=lln.orElse(null);
							if(logisticNetwork!=null) {
								if(logisticNetwork.canAddTask(key)) {
									hasRequest=true;
									int requestSize=Math.min(missing,filter.getKey().getMaxStackSize());
									logisticNetwork.addTask(key,new LogisticRequestTask(filter,requestSize,getBlockPos(),grid.cast()));
								}
							}
						}
					}
				}
			}
			if(hasUplink) {
				if(hasRequest) {
					uplinkStatus=2;
				}else
					uplinkStatus=1;
			}else
				uplinkStatus=3;
			
		}else
			uplinkStatus=0;
	});
	@Override
	public void tick() {
		super.tick();
		if(!this.level.isClientSide) {
			networkStatus=networks.isEmpty()?0:2;
			worker.tick();
		}
	}

	protected void refreshNetwork() {
		super.refreshNetwork();
		Collection<LazyOptional<LogisticNetwork>> candidate=FHCapabilities.ROBOTIC_LOGISTIC_CHUNK
			.getCapability(level.getChunk(worldPosition))
			.map(chunk->chunk.getNetworkFor(level,worldPosition))
			.orElse(Collections.emptySet());
		networks.addAll(candidate);
		
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap==ForgeCapabilities.ITEM_HANDLER)
			return grid.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new RequesterChestMenu(pContainerId,this,pPlayerInventory,container);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}
	@Override
	public void onRemoved() {
		super.onRemoved();
		grid.invalidate();
	}

	@Override
	public void onUnloaded() {
		super.onUnloaded();
		grid.invalidate();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(!grid.isPresent())
			grid=LazyOptional.of(()->container);
	}


}
