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

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticChest;
import com.teammoeg.frostedheart.content.robotics.logistics.gui.StorageChestMenu;

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

public class StorageTileEntity extends CBlockEntity implements CTickableBlockEntity,MenuProvider,ILogisticProvider,LogisticStatusBlockEntity{
	@Getter
	public LogisticChest container;
	public LazyOptional<LogisticChest> grid=LazyOptional.of(()->container);
	public LazyOptional<LogisticNetwork> network;
	ItemKey filter;
	@Getter
	protected int networkStatus=0;
	@Getter
	protected int uplinkStatus=0;
	public StorageTileEntity(BlockPos pos,BlockState bs) {
		super(FHBlockEntityTypes.STORAGE_CHEST.get(),pos,bs);
		container=new LogisticChest(null,pos);
	}
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		container.deserialize(nbt.getCompound("chest"));
	}
	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.put("chest",container.serialize());
	}
	@Override
	public void tick() {
		if(!this.level.isClientSide) {
			container.tick();
			if(network==null||!network.isPresent()) {
				Optional<LazyOptional<LogisticNetwork>> chunkData=FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.
				getCapability(this.level.getChunk(this.worldPosition)).map(t->t.getNetworkFor(level, worldPosition));
				if(chunkData.isPresent()) {
					LazyOptional<LogisticNetwork> ln=chunkData.get();
					if(ln.isPresent()) {
						network=ln;
						FHMain.LOGGER.info("register self against network sto "+ln);
						ln.resolve().get().getHub().addElement(grid.cast());
					}
				}
				uplinkStatus=networkStatus=0;
			}else {
				uplinkStatus=networkStatus=2;
			}
			
		}
		//.ifPresent(t->t.getNetworkFor(level, worldPosition));
	}
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new StorageChestMenu(pContainerId,this,pPlayerInventory,container);
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
		grid.invalidate();
	}
}
