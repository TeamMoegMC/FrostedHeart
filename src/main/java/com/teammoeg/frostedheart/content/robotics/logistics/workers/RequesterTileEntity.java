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

package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import com.teammoeg.frostedheart.content.robotics.logistics.gui.RequesterChestMenu;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticRequestTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;

import it.unimi.dsi.fastutil.ints.IntArrayList;
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

public class RequesterTileEntity extends CBlockEntity implements  CTickableBlockEntity,MenuProvider,LogisticStatusBlockEntity {
	
	ItemStackHandler container=new ItemStackHandler(27);
	public LazyOptional<ItemStackHandler> grid=LazyOptional.of(()->container);
	public LazyOptional<LogisticNetwork> network;
	static final int MAX_TASKS=27;
	public Filter[] filters=new Filter[9];
	@Getter
	protected int networkStatus=0;
	@Getter
	protected int uplinkStatus=0;
	List<Supplier<LogisticTaskKey>> keys=new ArrayList<>(MAX_TASKS);
	public RequesterTileEntity(BlockPos pos,BlockState bs) {
		super(FHBlockEntityTypes.REQUESTER_CHEST.get(),pos,bs);
		for(int i=0;i<MAX_TASKS;i++){
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
			int idx=0;
			int[] lens=new int[filters.length];
			int total=0;
			for(int i=0;i<filters.length;i++) {
				
				if(filters[i]!=null) {
					int currcnt=0;
					hasUplink=true;
					for(int j=0;j<container.getSlots();j++) {
						ItemStack stack=container.getStackInSlot(j);
						if(filters[i].matches(stack)) {
							currcnt+=stack.getCount();
						}
					}if(currcnt<filters[i].getSize()) {
						lens[i]=filters[i].getSize()-currcnt;
						total+=lens[i];
					}
				}
			}
			int curidx=0;
			LogisticNetwork networkGrid=network.resolve().get();
			IntArrayList ial=new IntArrayList(MAX_TASKS);
			for(int i=0;i<MAX_TASKS;i++) {
				Supplier<LogisticTaskKey> lt=keys.get(curidx);
				if(networkGrid.canAddTask(lt.get())) {
					ial.add(i);
					hasRequest=true;
				
				}
			}
			if(hasRequest) {
				float perCount=total*1f/ial.size();
				int ialidx=0;
				//split keys with weight
				for(int i=0;i<filters.length;i++) {
					int cnt=Math.round(lens[i]/perCount);
					if(cnt>0)
						for(int j=0;j<cnt;j++) {
							int ncnt=Math.min(lens[i], 64);
							lens[i]-=ncnt;
							
							networkGrid.addTask(keys.get(ial.getInt(ialidx)).get(), new LogisticRequestTask(filters[i],ncnt,this.getBlockPos(),grid.cast()));
							ialidx++;
						}
				}
				//split reminder between each filter
				if(ialidx<ial.size()) {
					for(int j=ialidx;j<ial.size();j++) {
						int icuridx=j%filters.length;
						int ncnt=Math.min(lens[icuridx], 64);
						if(ncnt>0) {
							lens[icuridx]-=ncnt;
							networkGrid.addTask(keys.get(ial.getInt(j)).get(), new LogisticRequestTask(filters[icuridx],ncnt,this.getBlockPos(),grid.cast()));
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
			if(network==null||!network.isPresent()) {
				networkStatus=0;
				Optional<LazyOptional<LogisticNetwork>> chunkData=FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.
				getCapability(this.level.getChunk(this.worldPosition)).map(t->t.getNetworkFor(level, worldPosition));
				if(chunkData.isPresent()) {
					LazyOptional<LogisticNetwork> ln=chunkData.get();
					if(ln.isPresent()) {
						FHMain.LOGGER.info("register self against network req "+ln);
						network=ln;
					}
				}
			}else
				networkStatus=2;
			worker.tick();
		}
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


}
