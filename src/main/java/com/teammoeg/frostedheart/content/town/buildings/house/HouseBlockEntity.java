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
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.teammoeg.frostedheart.content.town.TownMathFunctions.calculateDecorationRating;

/**
 * A house in the town.
 * <p>
 * Functionality: - Provide a place for residents to live - (Optional) Consume
 * heat to add temperature based on the heat level - Consume resources to
 * maintain the house - Check if the house structure is valid - Compute comfort
 * rating based on the house structure
 */
public class HouseBlockEntity extends AbstractTownBuildingBlockEntity<HouseBuilding> {

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
		this.scanStructure(building);
		building.temperatureModifier = temperatureModifier;
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
		List<BlockPos> doorPosSet = BlockScanner.getBlocksAdjacent(housePos, (pos) -> Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS));
		if (!doorPosSet.isEmpty()) {
			for (BlockPos doorPos : doorPosSet) {
				BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos) -> !(Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS)), doorPos);// 找到门下面垫的的那个方块
				for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
					//FHMain.LOGGER.debug("HouseScanner: creating new HouseBlockScanner");
					assert floorBelowDoor != null;
					BlockPos startPos = floorBelowDoor.relative(direction);// 找到门下方块旁边的方块
					//FHMain.LOGGER.debug("HouseScanner: start pos 1" + startPos);
					if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos)) {// 如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
						if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos.below()) || FloorBlockScanner.isHouseBlock(level, startPos.above(2))) {// 如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
							continue;
						}
						startPos = startPos.below();
						//FHMain.LOGGER.debug("HouseScanner: start pos 2" + startPos);
					}
					HouseBlockScanner scanner = new HouseBlockScanner(this.level, startPos);
					if (scanner.scan()) {
						//FHMain.LOGGER.debug("HouseScanner: scan successful");
						building.volume = scanner.getVolume();
						building.area = scanner.getArea();
						building.decorationRating = calculateDecorationRating(scanner.decorations, scanner.area);
						building.temperature = scanner.getTemperature();
						building.setOccupiedArea(scanner.getOccupiedArea());
						building.maxResidents = calculateMaxResidents(building.area, building.volume, scanner.getBeds().size());
						return true;
					}
				}
			}
		}
		return false;
	}


	public static int calculateMaxResidents(int area, int volume, int bedNum) {
		int maxResidentOfSpace = (int) (TownMathFunctions.calculateSpaceRating(volume, area) / 4 * area);
        return Math.min(maxResidentOfSpace, bedNum);
	}

	@Override
	public void tick() {
		assert level != null;
		if (!level.isClientSide) {
			if (endpoint.tryDrainHeat(1)) {
				temperatureModifier = Math.max(endpoint.getTempLevel() * 10, TownMathFunctions.COMFORTABLE_TEMP_HOUSE);
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
		return new HouseBuilding(this.getBlockPos());
	}
}