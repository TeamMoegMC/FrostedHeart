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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockScanner;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import lombok.Getter;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class HuntingBaseBlockEntity extends AbstractTownBuildingBlockEntity<HuntingBaseBuilding> {
	HeatEndpoint endpoint = HeatEndpoint.consumer(99, 1);
	LazyOptional<HeatEndpoint> endpointCap = LazyOptional.of(() -> endpoint);
	@Getter
    private double temperatureModifier = 0;


	public HuntingBaseBlockEntity(BlockPos pos, BlockState state) {
		super(FHBlockEntityTypes.HUNTING_BASE.get(), pos, state);
	}

	public boolean scanStructure(HuntingBaseBuilding building) {
		BlockPos housePos = this.getBlockPos();
		List<BlockPos> doorPosSet = BlockScanner.getBlocksAdjacent(housePos, (pos) -> Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS));
		if (doorPosSet.isEmpty()) return false;
		for (BlockPos doorPos : doorPosSet) {
			BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos) -> !(Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS)), doorPos);// 找到门下面垫的的那个方块
			for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
				assert floorBelowDoor != null;
				BlockPos startPos = floorBelowDoor.relative(direction);// 找到门下方块旁边的方块
				if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos)) {// 如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
					if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos.below()) || FloorBlockScanner.isHouseBlock(level, startPos.above(2))) {// 如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
						continue;
					}
					startPos = startPos.below();
				}
				HuntingBaseBlockScanner scanner = new HuntingBaseBlockScanner(this.level, startPos);
				if (scanner.scan()) {
					building.volume = scanner.getVolume();
					building.area = scanner.getArea();
					building.temperature = scanner.getTemperature();
					building.setOccupiedArea(scanner.getOccupiedArea());
					building.tanningRackNum = scanner.getTanningRackNum();
					building.maxResidents = calculateMaxResidents(building.volume, building.area, scanner.getBeds().size());
					building.rating = computeRating(building.volume, building.area, scanner.getDecorations(), building.temperature, this.getTemperatureModifier(), building.maxResidents, scanner.getChestNum());
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Static method to compute hunting base rating based on provided parameters.
	 * 
	 * @param volume the volume of the hunting base
	 * @param area the area of the hunting base
	 * @param decorations map of decorations
	 * @param temperature the base temperature
	 * @param temperatureModifier the temperature modifier
	 * @param maxResidents maximum number of residents
	 * @param chestNum number of chests
	 * @return computed rating value
	 */
	public static double computeRating(int volume, int area, Map<String, Integer> decorations, double temperature, double temperatureModifier, int maxResidents, int chestNum) {
		return (TownMathFunctions.calculateSpaceRating(volume, area) * (2 + TownMathFunctions.calculateDecorationRating(decorations, area))
				+ 2 * TownMathFunctions.calculateTemperatureRating(temperature + temperatureModifier) +
				(1 - Math.exp(-maxResidents - chestNum))) / 6;
	}

	private static int calculateMaxResidents(int volume, int area, int bedNum) {
			return Math.min((int) (TownMathFunctions.calculateSpaceRating(volume, area) / 4 * area), bedNum);
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

	@Nonnull
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
		if (capability == FHCapabilities.HEAT_EP.capability() && facing == Direction.NORTH) {
			return endpointCap.cast();
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public void refresh(@NotNull HuntingBaseBuilding building) {
		this.scanStructure(building);
		building.temperatureModifier = this.getTemperatureModifier();
	}

	@Override
	public @Nullable HuntingBaseBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
		if(abstractTownBuilding instanceof HuntingBaseBuilding){
			return (HuntingBaseBuilding) abstractTownBuilding;
		}
		return null;
	}

	@Override
	public void invalidateCaps() {
		endpointCap.invalidate();
		super.invalidateCaps();
	}

	@Override
	public @NotNull HuntingBaseBuilding createBuilding() {
		return new HuntingBaseBuilding(this.getBlockPos());
	}
}
