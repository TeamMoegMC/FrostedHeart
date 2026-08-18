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
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.gui.LogisticInterfaceChestOutMenu;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticRequestTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;

import lombok.Getter;
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
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticInterfaceChestOutTileEntity extends CBlockEntity implements  CTickableBlockEntity,MenuProvider,LogisticStatusBlockEntity {
	
	ItemStackHandler container=new ItemStackHandler(27) {
		@Override
		protected void onContentsChanged(int slot) {
			if(level!=null)
				LogisticInterfaceChestOutTileEntity.this.setChanged();
		}
	};
	public LazyOptional<ItemStackHandler> grid=LazyOptional.of(()->container);
	public LazyOptional<LogisticNetwork> network;
	public Filter[] filters=new Filter[9];
	@Getter
	protected int networkStatus=0;
	@Getter
	protected int uplinkStatus=0;
	List<Supplier<LogisticTaskKey>> keys=new ArrayList<>(filters.length);
	private int networkCheckTicks;
	public LogisticInterfaceChestOutTileEntity(BlockPos pos,BlockState bs) {
		super(FHBlockEntityTypes.LOGISTIC_INTERFACE_CHEST_OUT.get(),pos,bs);
		for(int i=0;i<filters.length;i++){
			final int cnt=i;
			keys.add(Lazy.of(()->new LogisticTaskKey(pos,cnt)));
		}

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
		if(network!=null&&network.isPresent()) {
			boolean hasUplink=false;
			boolean hasRequest=false;
			LogisticNetwork networkGrid=network.resolve().get();
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
						LogisticTaskKey key=keys.get(i).get();
						if(networkGrid.canAddTask(key)) {
							hasRequest=true;
							int requestSize=Math.min(missing,filter.getKey().getMaxStackSize());
							networkGrid.addTask(key,new LogisticRequestTask(filter,requestSize,getBlockPos(),grid.cast()));
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
		if(!this.level.isClientSide) {
			if(network==null||!network.isPresent()||networkCheckTicks--<=0) {
				refreshNetwork();
				networkCheckTicks=20;
			}
			networkStatus=network!=null&&network.isPresent()?2:0;
			worker.tick();
		}
	}

	private void refreshNetwork() {
		LazyOptional<LogisticNetwork> candidate=FHCapabilities.ROBOTIC_LOGISTIC_CHUNK
			.getCapability(level.getChunk(worldPosition))
			.map(chunk->chunk.getNetworkFor(level,worldPosition))
			.orElse(LazyOptional.empty());
		LogisticNetwork current=network!=null&&network.isPresent()?network.resolve().get():null;
		LogisticNetwork next=candidate.isPresent()?candidate.resolve().get():null;
		if(current!=null&&current!=next)
			current.cancelTasksAt(worldPosition);
		if(current!=next)
			network=next==null?null:candidate;
		else if(current==null)
			network=null;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap==ForgeCapabilities.ITEM_HANDLER)
			return grid.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new LogisticInterfaceChestOutMenu(pContainerId,this,pPlayerInventory,container);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}
	@Override
	public void onRemoved() {
		super.onRemoved();
		if(network!=null&&network.isPresent())
			network.resolve().get().cancelTasksAt(worldPosition);
		network=null;
		grid.invalidate();
	}

	@Override
	public void onUnloaded() {
		network=null;
		grid.invalidate();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(!grid.isPresent())
			grid=LazyOptional.of(()->container);
		networkCheckTicks=0;
	}


}
