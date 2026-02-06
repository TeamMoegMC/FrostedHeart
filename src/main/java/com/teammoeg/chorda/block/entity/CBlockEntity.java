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

package com.teammoeg.chorda.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
/**
 * CBlockEntity 
 * blockentity with our basic code, for convenience, some code are inspired by Immersive Engineering
 * */
public abstract class CBlockEntity extends BlockEntity implements SyncableBlockEntity,BlockStateAccess {
	protected boolean isUnloaded;
    public CBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
	@Override
	public void load(CompoundTag nbtIn)
	{
		super.load(nbtIn);
		this.readCustomNBT(nbtIn, false);
	}

	/**
	 * @param nbt nbt to read
	 * @param descPacket true if the packet is just for sync data from server? I'm not sure, ask khj for details.
	 */
	public abstract void readCustomNBT(CompoundTag nbt, boolean descPacket);

	@Override
	protected void saveAdditional(CompoundTag nbt)
	{
		super.saveAdditional(nbt);
		this.writeCustomNBT(nbt, false);
	}

	/**
	 * @param nbt nbt to write
	 * @param descPacket true if the packet is just for sync data to client? I'm not sure, ask khj for details.
	 */
	public abstract void writeCustomNBT(CompoundTag nbt, boolean descPacket);

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this, be -> {
			CompoundTag nbttagcompound = new CompoundTag();
			this.writeCustomNBT(nbttagcompound, true);
			return nbttagcompound;
		});
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
	{
		CompoundTag nonNullTag = pkt.getTag()!=null?pkt.getTag(): new CompoundTag();
		this.readCustomNBT(nonNullTag, true);
	}

	@Override
	public void handleUpdateTag(CompoundTag tag)
	{
		this.readCustomNBT(tag, true);
	}

	@Override
	public CompoundTag getUpdateTag()
	{
		CompoundTag nbt = super.getUpdateTag();
		writeCustomNBT(nbt, true);
		return nbt;
	}
	@Override
	public boolean triggerEvent(int id, int type)
	{
		if(id==0||id==255)
		{
			syncData();
			return true;
		}
		else if(id==254)
		{
			BlockState state = level.getBlockState(worldPosition);
			level.sendBlockUpdated(worldPosition, state, state, 3);
			return true;
		}
		return super.triggerEvent(id, type);
	}
    public void syncData() {
        this.setChanged();
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        
    }
	@Override
	public final void setRemoved()
	{
		if(!isUnloaded)
			onRemoved();
		super.setRemoved();
	}
	public void onRemoved(){
		
	}
	@Override
	public void onLoad()
	{
		super.onLoad();
		isUnloaded = false;
	}
	@Override
	public void onChunkUnloaded()
	{
		super.onChunkUnloaded();
		isUnloaded = true;
		onUnloaded();
	}
	public void onUnloaded(){
	}
	protected void setChunkUnsaved()
	{
		if(this.level.hasChunkAt(this.worldPosition))
			this.level.getChunkAt(this.worldPosition).setUnsaved(true);
	}
	@Override
	public void setChanged()
	{
		setChunkUnsaved();
		BlockState state = getBlockState();
		if(state.hasAnalogOutputSignal())
			this.level.updateNeighbourForOutputSignal(this.worldPosition, state.getBlock());
	}
	@Override
	public BlockState getBlock() {
		return this.getBlockState();
	}
	@Override
	public void setBlock(BlockState state) {
		this.level.setBlock(this.worldPosition, state, 6);
		this.setBlockState(state);
		
	}

}
