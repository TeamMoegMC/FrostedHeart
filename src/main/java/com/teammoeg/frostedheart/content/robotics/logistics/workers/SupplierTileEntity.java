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
public class SupplierTileEntity extends CBlockEntity implements CTickableBlockEntity,MenuProvider,ILogisticProvider,LogisticStatusBlockEntity {
	@Getter
	RequestLogisticChest container;
	public LazyOptional<LogisticChest> grid=LazyOptional.of(()->container);
	public LazyOptional<LogisticNetwork> network;
	@Getter
	protected int networkStatus=0;
	@Getter
	protected int uplinkStatus=0;
	private final List<Supplier<LogisticTaskKey>> keys=new ArrayList<>(27);
	private int networkCheckTicks;
	public SupplierTileEntity(BlockPos pos,BlockState bs) {
		super(FHBlockEntityTypes.SUPPLIER_CHEST.get(),pos,bs);
		container=new RequestLogisticChest(null,pos,this::setChanged);
		for(int slot=0;slot<27;slot++) {
			final int taskSlot=slot;
			keys.add(()->new LogisticTaskKey(pos,taskSlot));
		}
	}



	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		container.deserialize(nbt.getCompound("chest"));
	}
	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.put("chest",container.serialize());
	}

	LazyTickWorker pushWorker=new LazyTickWorker(10,()->{
		if(network==null||!network.isPresent())
			return;
		LogisticNetwork logisticNetwork=network.resolve().get();
		for(int slot=0;slot<container.getSlots();slot++) {
			if(container.getStackInSlot(slot).isEmpty())
				continue;
			LogisticTaskKey key=keys.get(slot).get();
			if(logisticNetwork.canAddTask(key))
				logisticNetwork.addTask(key,new LogisticPushTask(worldPosition,grid.cast(),slot));
		}
	});

	@Override
	public void tick() {
		if(!this.level.isClientSide) {
			container.setLevel(level);
			container.tick();
			if(network==null||!network.isPresent()||networkCheckTicks--<=0) {
				refreshNetwork();
				networkCheckTicks=20;
			}
			if(network!=null&&network.isPresent()) {
				networkStatus=2;
				if(container.getEmptySlotCount()>=27) {
					uplinkStatus=1;
				}else 
					uplinkStatus=2;
			}else
				uplinkStatus=networkStatus=0;
			pushWorker.tick();
		}
	}

	private void refreshNetwork() {
		LazyOptional<LogisticNetwork> candidate=FHCapabilities.ROBOTIC_LOGISTIC_CHUNK
			.getCapability(level.getChunk(worldPosition))
			.map(chunk->chunk.getNetworkFor(level,worldPosition))
			.orElse(LazyOptional.empty());
		LogisticNetwork current=network!=null&&network.isPresent()?network.resolve().get():null;
		LogisticNetwork next=candidate.isPresent()?candidate.resolve().get():null;
		if(current!=null&&next!=null&&current!=next) {
			current.cancelTasksAt(worldPosition);
			current.getHub().removeElement(grid.cast());
			network=candidate;
			next.getHub().addElement(grid.cast());
		}else if(current==null&&next!=null) {
			network=candidate;
			next.getHub().addElement(grid.cast());
		}else if(current!=null&&next==null) {
			current.cancelTasksAt(worldPosition);
			current.getHub().removeElement(grid.cast());
			network=null;
		}else if(current==null)
			network=null;
	}

	private void disconnectNetwork() {
		if(network!=null&&network.isPresent())
			network.resolve().get().getHub().removeElement(grid.cast());
		network=null;
	}
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new SupplierChestMenu(pContainerId,this,pPlayerInventory,container);
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



	@Override
	public void onRemoved() {
		super.onRemoved();
		if(network!=null&&network.isPresent())
			network.resolve().get().cancelTasksAt(worldPosition);
		disconnectNetwork();
		grid.invalidate();
	}

	@Override
	public void onUnloaded() {
		disconnectNetwork();
		grid.invalidate();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		container.setLevel(level);
		if(!grid.isPresent())
			grid=LazyOptional.of(()->container);
		networkCheckTicks=0;
	}



}
