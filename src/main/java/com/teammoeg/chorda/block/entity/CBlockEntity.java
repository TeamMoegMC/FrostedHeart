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
 * 抽象基础方块实体类，提供 NBT 序列化、客户端-服务器数据同步、区块加载/卸载生命周期管理等基础功能。
 * 部分设计灵感来自沉浸工程（Immersive Engineering）模组。
 * <p>
 * Abstract base block entity class providing NBT serialization, client-server data synchronization,
 * chunk load/unload lifecycle management, and other foundational functionality.
 * Some design patterns are inspired by the Immersive Engineering mod.
 */
public abstract class CBlockEntity extends BlockEntity implements SyncableBlockEntity,BlockStateAccess {
	/** 标记方块实体是否因区块卸载而被移除（而非被主动破坏）。 / Tracks whether the block entity was unloaded (as opposed to actively removed). */
	protected boolean isUnloaded;

	/**
	 * 使用给定的类型、位置和状态构造方块实体。
	 * <p>
	 * Constructs a block entity with the given type, position, and state.
	 *
	 * @param type 方块实体类型 / the block entity type
	 * @param pos 方块位置 / the block position
	 * @param state 方块状态 / the block state
	 */
    public CBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

	/**
	 * 从 NBT 数据加载方块实体状态。调用 {@link #readCustomNBT(CompoundTag, boolean)} 读取自定义数据。
	 * <p>
	 * Loads the block entity state from NBT data. Delegates to {@link #readCustomNBT(CompoundTag, boolean)}
	 * for custom data reading.
	 *
	 * @param nbtIn 包含方块实体数据的 NBT 标签 / the NBT tag containing the block entity data
	 */
	@Override
	public void load(CompoundTag nbtIn)
	{
		super.load(nbtIn);
		this.readCustomNBT(nbtIn, false);
	}

	/**
	 * 从 NBT 标签读取自定义数据。子类必须实现此方法以持久化自己的数据。
	 * <p>
	 * Reads custom data from the given NBT tag. Subclasses must implement this to
	 * persist their own data.
	 *
	 * @param nbt 要读取的 NBT 标签 / the NBT tag to read from
	 * @param descPacket 是否为同步数据包（true 表示用于客户端-服务器同步） / true if this is a sync packet (used for client-server synchronization)
	 */
	public abstract void readCustomNBT(CompoundTag nbt, boolean descPacket);

	/**
	 * 将方块实体状态保存到 NBT 数据。调用 {@link #writeCustomNBT(CompoundTag, boolean)} 写入自定义数据。
	 * <p>
	 * Saves the block entity state to NBT data. Delegates to {@link #writeCustomNBT(CompoundTag, boolean)}
	 * for custom data writing.
	 *
	 * @param nbt 要写入的 NBT 标签 / the NBT tag to write to
	 */
	@Override
	protected void saveAdditional(CompoundTag nbt)
	{
		super.saveAdditional(nbt);
		this.writeCustomNBT(nbt, false);
	}

	/**
	 * 将自定义数据写入 NBT 标签。子类必须实现此方法以持久化自己的数据。
	 * <p>
	 * Writes custom data to the given NBT tag. Subclasses must implement this to
	 * persist their own data.
	 *
	 * @param nbt 要写入的 NBT 标签 / the NBT tag to write to
	 * @param descPacket 是否为同步数据包（true 表示用于客户端-服务器同步） / true if this is a sync packet (used for client-server synchronization)
	 */
	public abstract void writeCustomNBT(CompoundTag nbt, boolean descPacket);

	/**
	 * 获取用于客户端同步的更新数据包，包含通过 {@link #writeCustomNBT(CompoundTag, boolean)} 写入的同步数据。
	 * <p>
	 * Gets the update packet for client synchronization, containing sync data
	 * written via {@link #writeCustomNBT(CompoundTag, boolean)}.
	 *
	 * @return 客户端同步数据包 / the client-bound block entity data packet
	 */
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this, be -> {
			CompoundTag nbttagcompound = new CompoundTag();
			this.writeCustomNBT(nbttagcompound, true);
			return nbttagcompound;
		});
	}

	/**
	 * 处理从服务器接收到的数据包，读取同步数据。
	 * <p>
	 * Handles a data packet received from the server, reading synchronized data.
	 *
	 * @param net 网络连接 / the network connection
	 * @param pkt 数据包 / the data packet
	 */
	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
	{
		CompoundTag nonNullTag = pkt.getTag()!=null?pkt.getTag(): new CompoundTag();
		this.readCustomNBT(nonNullTag, true);
	}

	/**
	 * 处理初始区块加载时的更新标签。
	 * <p>
	 * Handles the update tag received during initial chunk loading.
	 *
	 * @param tag 更新标签 / the update tag
	 */
	@Override
	public void handleUpdateTag(CompoundTag tag)
	{
		this.readCustomNBT(tag, true);
	}

	/**
	 * 获取用于初始区块加载同步的更新标签。
	 * <p>
	 * Gets the update tag for initial chunk load synchronization.
	 *
	 * @return 包含同步数据的 NBT 标签 / the NBT tag containing sync data
	 */
	@Override
	public CompoundTag getUpdateTag()
	{
		CompoundTag nbt = super.getUpdateTag();
		writeCustomNBT(nbt, true);
		return nbt;
	}

	/**
	 * 处理方块事件。事件 ID 0 和 255 触发数据同步，事件 ID 254 触发方块更新通知。
	 * <p>
	 * Handles block events. Event IDs 0 and 255 trigger data synchronization,
	 * event ID 254 triggers a block update notification.
	 *
	 * @param id 事件 ID / the event ID
	 * @param type 事件类型 / the event type
	 * @return 如果事件被处理则返回 true / true if the event was handled
	 */
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

	/**
	 * 同步方块实体数据到客户端，同时通知相邻方块更新。
	 * <p>
	 * Synchronizes the block entity data to clients and notifies neighboring blocks of updates.
	 */
    public void syncData() {
        this.setChanged();
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());

    }

	/**
	 * 当方块实体被移除时调用。如果方块实体不是因区块卸载而被移除，则调用 {@link #onRemoved()}。
	 * <p>
	 * Called when the block entity is removed. If not being removed due to chunk unloading,
	 * calls {@link #onRemoved()}.
	 */
	@Override
	public final void setRemoved()
	{
		if(!isUnloaded)
			onRemoved();
		super.setRemoved();
	}

	/**
	 * 当方块实体被主动移除（非区块卸载）时调用的回调。子类可重写此方法以执行清理逻辑。
	 * <p>
	 * Callback invoked when the block entity is actively removed (not due to chunk unloading).
	 * Subclasses can override this to perform cleanup logic.
	 */
	public void onRemoved(){

	}

	/**
	 * 方块实体加载时调用，重置卸载标志。
	 * <p>
	 * Called when the block entity is loaded, resetting the unloaded flag.
	 */
	@Override
	public void onLoad()
	{
		super.onLoad();
		isUnloaded = false;
	}

	/**
	 * 区块卸载时调用，设置卸载标志并触发 {@link #onUnloaded()} 回调。
	 * <p>
	 * Called when the chunk is unloaded, setting the unloaded flag and triggering
	 * the {@link #onUnloaded()} callback.
	 */
	@Override
	public void onChunkUnloaded()
	{
		super.onChunkUnloaded();
		isUnloaded = true;
		onUnloaded();
	}

	/**
	 * 当方块实体因区块卸载而被移除时调用的回调。子类可重写此方法以执行卸载清理逻辑。
	 * <p>
	 * Callback invoked when the block entity is removed due to chunk unloading.
	 * Subclasses can override this to perform unload cleanup logic.
	 */
	public void onUnloaded(){
	}

	/**
	 * 将包含此方块实体的区块标记为未保存，以确保数据会在下次保存时写入磁盘。
	 * <p>
	 * Marks the chunk containing this block entity as unsaved, ensuring data
	 * will be written to disk on the next save.
	 */
	protected void setChunkUnsaved()
	{
		if(this.level.hasChunkAt(this.worldPosition))
			this.level.getChunkAt(this.worldPosition).setUnsaved(true);
	}

	/**
	 * 标记方块实体已更改。将区块标记为未保存，并在方块有模拟输出信号时通知邻居更新。
	 * <p>
	 * Marks the block entity as changed. Marks the chunk as unsaved and notifies
	 * neighbors for output signal updates if the block has an analog output signal.
	 */
	@Override
	public void setChanged()
	{
		setChunkUnsaved();
		BlockState state = getBlockState();
		if(state.hasAnalogOutputSignal())
			this.level.updateNeighbourForOutputSignal(this.worldPosition, state.getBlock());
	}

	/**
	 * 获取当前方块状态（实现 {@link BlockStateAccess} 接口）。
	 * <p>
	 * Gets the current block state (implements {@link BlockStateAccess}).
	 *
	 * @return 当前方块状态 / the current block state
	 */
	@Override
	public BlockState getBlock() {
		return this.getBlockState();
	}

	/**
	 * 设置方块状态，同时更新世界中的方块和内部缓存（实现 {@link BlockStateAccess} 接口）。
	 * <p>
	 * Sets the block state, updating both the world block and internal cache
	 * (implements {@link BlockStateAccess}).
	 *
	 * @param state 新的方块状态 / the new block state
	 */
	@Override
	public void setBlock(BlockState state) {
		this.level.setBlock(this.worldPosition, state, 6);
		this.setBlockState(state);

	}

}
