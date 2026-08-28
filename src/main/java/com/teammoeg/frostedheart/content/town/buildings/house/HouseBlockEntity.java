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

package com.teammoeg.frostedheart.content.town.buildings.house;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A house in the town.
 * <p>
 * Functionality: - Provide a place for residents to live - (Optional) Consume
 * heat to add temperature based on the heat level - Consume resources to
 * maintain the house - Check if the house structure is valid - Compute comfort
 * rating based on the house structure
 */
public class HouseBlockEntity extends AbstractTownBuildingBlockEntity<HouseBuilding> implements MenuProvider {

	@Getter
    private double temperatureModifier = 0;

	/** Tile data, stored in tile entity. */
	HeatEndpoint endpoint = HeatEndpoint.consumer(99, 1);

	public HouseBlockEntity(BlockPos pos, BlockState state) {
		super(FHBlockEntityTypes.HOUSE.get(), pos, state);
	}

	/**
	 * Check if work environment is valid.
	 * <p>
	 * For the house, this implies whether the house would accommodate the
	 * residents, consume resources, and other.
	 * <p>
	 * Room structure should be valid. Temperature should be within a reasonable
	 * range.
	 */
	public void refresh(@NotNull HouseBuilding building) {
		super.refresh(building);
		building.setTemperatureModifier(temperatureModifier);
	}

	@Override
	public @Nullable HouseBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
		if(abstractTownBuilding instanceof HouseBuilding){
			return (HouseBuilding) abstractTownBuilding;
		}
		return null;
	}

	/**
	 * Determine whether the house structure is well-defined.
	 * <p>
	 * Check room insulation Check minimum volume Check within generator range (or
	 * just check steam connection instead?)
	 * <p>
	 *
	 * @return whether the house structure is valid
	 */
	public boolean scanStructure(HouseBuilding building) {
		BlockPos housePos = this.getBlockPos();
		RoomData rd=BlockScanner.scanRoomDataFromBlock(level,housePos);
		if (rd!=null&&rd.doors.size()>0) {
			//FHMain.LOGGER.debug("HouseScanner: scan successful");
			building.setVolume(rd.volume);
			building.setArea(rd.area);
			FHConfig.Server.Town.Housing housing = FHConfig.SERVER.TOWN.HOUSING;
			building.setDecorationRating(TownMathFunctions.calculateDecorationRating(
				rd.findAllBlock(t->t.is(FHTags.Blocks.TOWN_DECORATIONS.get())),
					rd.area,
					housing.decorationCountLogOffset.get(),
					housing.decorationCountLogMultiplier.get(),
					housing.decorationTypeBaseScore.get(),
					housing.decorationBaseDemand.get(),
					housing.decorationFloorBlocksPerDemand.get()));
			building.setTemperature(rd.calculateTemperature(level));
			building.setOccupiedVolume(rd.calculateOccupiedVolume());
			building.setMaxResidents(calculateMaxResidents(building.getArea(), building.getVolume(), rd.countInsideBlock(t->t.is(BlockTags.BEDS))));
			building.setLayout(rd.findBlockPosition(t->t.is(BlockTags.BEDS)&&((!t.hasProperty(BedBlock.PART))||t.getValue(BedBlock.PART)==BedPart.HEAD)), rd.doors.get(0));
			return true;
		}
		building.clearLayout();
		return false;
	}


	public static int calculateMaxResidents(int area, int volume, int bedNum) {
		FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
		FHConfig.Server.Town.Housing housing = FHConfig.SERVER.TOWN.HOUSING;
		double spaceRating = TownMathFunctions.calculateSpaceRating(
				volume,
				area,
				scoring.spaceAreaCoefficient.get(),
				scoring.spaceHeightLogCoefficient.get(),
				scoring.spaceHeightLogOffset.get(),
				scoring.spaceResponseScale.get(),
				scoring.spaceResponseExponent.get());
        return HouseDailyModel.calculateCapacity(
                spaceRating, area, housing.floorBlocksPerResident.get(), bedNum);
	}

	@Override
	public void tick() {
		assert level != null;
		if (!level.isClientSide) {
			if (endpoint.tryDrainHeat(1)) {
				temperatureModifier = Math.max(
						endpoint.getTempLevel() * 10,
						FHConfig.SERVER.TOWN.BUILDING_SCORING.comfortableTemperatureCelsius.get());
				if (setActive(true)) {
					setChanged();
				}
			} else {
				temperatureModifier = 0;
				if (setActive(false)) {
					setChanged();
				}
			}
		} else if (getIsActive()) {
			FHClientUtils.spawnSteamParticles(level, worldPosition);
		}
		this.addToSchedulerQueue();
	}

	@Override
	public void readCustomNBT(CompoundTag compoundNBT, boolean isPacket) {
		super.readCustomNBT(compoundNBT, isPacket);
		endpoint.load(compoundNBT, isPacket);
	}

	@Override
	public void writeCustomNBT(CompoundTag compoundNBT, boolean isPacket) {
		super.writeCustomNBT(compoundNBT, isPacket);
		endpoint.save(compoundNBT, isPacket);
	}

	LazyOptional<HeatEndpoint> endpointCap = LazyOptional.of(() -> endpoint);

	@Nonnull
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
		if (capability == FHCapabilities.HEAT_EP.capability() && facing == Direction.NORTH) {
			return endpointCap.cast();
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public void invalidateCaps() {
		endpointCap.invalidate();
		super.invalidateCaps();
	}

	@Override
	public @NotNull HouseBuilding createBuilding() {
		return new  HouseBuilding(this.getBlockPos());
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(
			int id,
			@NotNull Inventory playerInventory,
			@NotNull Player player
	) {
		return new HouseMenu(id, playerInventory, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.frostedheart.house");
	}
}
