/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.generator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.block.ManagedOwnerTile;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.FTBTeamsCommon;
import dev.ftb.mods.ftbteams.FTBTeamsForge;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
/**
 * Common base class for any generator like block that maintains a heat area
 * 
 * */
public abstract class ZoneHeatingMultiblockTileEntity<T extends ZoneHeatingMultiblockTileEntity<T>> extends MultiblockPartTileEntity<T>
		implements FHBlockInterfaces.IActiveState {
	float temperatureLevel;
	int rangeLevel;

	private boolean initialized;
	boolean isWorking;
	boolean isOverdrive;
	boolean isActualOverdrive;
	boolean isDirty;// mark if temperature change required

	public ZoneHeatingMultiblockTileEntity(IETemplateMultiblock multiblockInstance, TileEntityType<T> type, boolean hasRSControl) {
		super(multiblockInstance, type, hasRSControl);
	}

	public int getActualRange() {
		return (int) (8 + (getRangeLevel()) * 4);
	}

	public int getUpperBound() {
		return MathHelper.ceil(getRangeLevel() * 4);
	}

	public int getLowerBound() {
		return MathHelper.ceil(getRangeLevel());
	}

	public int getActualTemp() {
		return (int) (getTemperatureLevel() * 10);
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
		super.readCustomNBT(nbt, descPacket);
		isWorking = nbt.getBoolean("isWorking");
		isOverdrive = nbt.getBoolean("isOverdrive");
		isActualOverdrive = nbt.getBoolean("Overdriven");
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
		super.writeCustomNBT(nbt, descPacket);
		nbt.putBoolean("isWorking", isWorking);
		nbt.putBoolean("isOverdrive", isOverdrive);
		nbt.putBoolean("Overdriven", isActualOverdrive);
	}

	@Override
	public void disassemble() {
		ChunkHeatData.removeTempAdjust(world, getPos());
		if (shouldUnique() && master() != null)
			master().unregist();
		super.disassemble();
	}

	public void unregist() {
		getTeamData().ifPresent(t -> {
			t.generatorData.actualPos=BlockPos.ZERO;
		});
	}

	public void regist() {
		getTeamData().ifPresent(t -> t.generatorData.actualPos=this.pos);
	}

	public void setOwner(UUID owner) {
		forEachBlock(s -> IOwnerTile.setOwner(s, owner));
	}

	protected Optional<Team> getTeam() {
		UUID owner = getOwner();
		if (owner != null)
			return Optional.ofNullable(TeamManager.INSTANCE.getTeamByID(owner));
		return Optional.empty();
	}

	protected Optional<TeamResearchData> getTeamData() {
		UUID owner = getOwner();
		if (owner != null)
			return Optional.of(ResearchDataAPI.getData(owner));
		return Optional.empty();
	}


	protected void tickControls() {
	}

	protected abstract void onShutDown();

	protected abstract void tickFuel();

	protected abstract void tickEffects(boolean isActive);

	public abstract boolean shouldUnique();

	@Override
	public void tick() {
		checkForNeedlessTicking();
		if (isDummy())
			return;
		// spawn smoke particle
		if (world != null && world.isRemote && formed) {
			tickEffects(getIsActive());
		}

		tickControls();

		if (!world.isRemote && formed) {
			if (isWorking()) {
				final boolean activeBeforeTick = getIsActive();
				tickFuel();
				// set activity status
				final boolean activeAfterTick = getIsActive();
				if (activeBeforeTick != activeAfterTick) {
					this.markDirty();
					if (activeAfterTick) {
						ChunkHeatData.addPillarTempAdjust(world, getPos(), getActualRange(), getUpperBound(),
								getLowerBound(), getActualTemp());
					} else {
						ChunkHeatData.removeTempAdjust(world, getPos());
					}
					setAllActive(activeAfterTick);
				} else if (activeAfterTick) {
					if (isChanged() || !initialized) {
						initialized = true;
						markChanged(false);
						ChunkHeatData.addPillarTempAdjust(world, getPos(), getActualRange(), getUpperBound(),
								getLowerBound(), getActualTemp());
					}
				}
			} else
				shutdownTick();
		}

	}

	public void shutdownTick() {
	}

	public void setWorking(boolean working) {
		if (master() != null) {
			master().isWorking = working;
		}
	}

	public boolean isWorking() {
		if (master() != null)
			return master().isWorking;
		return false;
	}

	public boolean isOverdrive() {
		if (master() != null)
			return master().isOverdrive;
		return false;
	}

	public void markChanged(boolean dirty) {
		if (master() != null)
			master().isDirty = dirty;
	}

	public boolean isChanged() {
		if (master() != null)
			return master().isDirty;
		return false;
	}

	public void setOverdrive(boolean overdrive) {
		if (master() != null) {
			master().isOverdrive = overdrive;
		}
	}

	public boolean isActualOverdrive() {
		if (master() != null)
			return master().isActualOverdrive;
		return false;
	}

	public void setActualOverdrive(boolean isActualOverdrive) {
		if (master() != null) {
			markChanged(true);
			master().isActualOverdrive = isActualOverdrive;
		}
	}

	public float getTemperatureLevel() {
		if (master() != null)
			return master().temperatureLevel;
		return 1;
	}

	public float getRangeLevel() {
		if (master() != null)
			return master().rangeLevel;
		return 1;
	}

	public void setTemperatureLevel(float temperatureLevel) {
		if (master() != null) {
			master().isDirty = true;
			master().temperatureLevel = temperatureLevel;
		}
	}

	public void setRangeLevel(int rangeLevel) {
		if (master() != null) {
			master().isDirty = true;
			master().rangeLevel = rangeLevel;
		}
	}

	protected void setAllActive(boolean state) {
		forEachBlock(s -> s.setActive(state));
	}

	public abstract void forEachBlock(Consumer<T> consumer);

	UUID getOwner() {
		return IOwnerTile.getOwner(this);
	}
}
