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

package com.teammoeg.chorda.multiblock.components;

import java.util.Optional;
import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.nbt.CompoundTag;

/**
 * 多方块所有者状态的默认实现，同时实现了 {@link IMultiblockState} 和 {@link IOwnerState}。
 * 将所有者 UUID 持久化到 NBT 中，并提供通过所有者 UUID 获取队伍数据的便捷方法。
 * 所有者变更回调默认为空实现，子类可根据需要覆盖。
 * <p>
 * Default implementation of multiblock owner state, implementing both {@link IMultiblockState}
 * and {@link IOwnerState}. Persists the owner UUID to NBT and provides a convenience method
 * to retrieve team data via the owner UUID. The owner change callback is a no-op by default;
 * subclasses can override it as needed.
 *
 * @see IOwnerState
 * @see TeamDataHolder
 */
public class OwnerState implements IMultiblockState, IOwnerState<OwnerState> {

	/** 多方块所有者的 UUID，可能为 null / The UUID of the multiblock owner, may be null */
	UUID owner=null;

	/**
	 * 构造一个没有所有者的默认所有者状态。
	 * <p>
	 * Constructs a default owner state with no owner.
	 */
	public OwnerState() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UUID getOwner() {
		return owner;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	/**
	 * 将所有者 UUID 写入 NBT 进行存档保存。仅在所有者不为 null 时写入。
	 * <p>
	 * Writes the owner UUID to NBT for save persistence. Only writes when the owner is not null.
	 *
	 * @param nbt 要写入的 NBT 复合标签 / The compound tag to write to
	 */
	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		if(owner!=null)
			nbt.putUUID("owner", owner);

	}

	/**
	 * 从 NBT 读取所有者 UUID 以恢复存档。仅在 NBT 中包含 "owner" 键时读取。
	 * <p>
	 * Reads the owner UUID from NBT to restore from a save. Only reads when the NBT contains an "owner" key.
	 *
	 * @param nbt 要读取的 NBT 复合标签 / The compound tag to read from
	 */
	@Override
	public void readSaveNBT(CompoundTag nbt) {
		if(nbt.contains("owner"))
			owner=nbt.getUUID("owner");

	}

	/**
	 * 通过所有者 UUID 获取关联的队伍数据。
	 * <p>
	 * Retrieves the associated team data via the owner UUID.
	 *
	 * @return 包含队伍数据的 Optional，如果没有所有者或找不到队伍数据则为空 / An Optional containing the team data, or empty if there is no owner or team data is not found
	 */
    public Optional<TeamDataHolder> getTeamData() {
        UUID owner = getOwner();
        if (owner != null)
            return Optional.ofNullable(CTeamDataManager.getDataByResearchID(owner));
        return Optional.empty();
    }

	/**
	 * {@inheritDoc}
	 * 默认空实现。子类可覆盖此方法以在所有者变更时执行自定义逻辑。
	 * <p>
	 * Default no-op implementation. Subclasses can override to execute custom logic on owner change.
	 */
	@Override
	public void onOwnerChange(IMultiblockContext<? extends OwnerState> ctx) {

	}



}
