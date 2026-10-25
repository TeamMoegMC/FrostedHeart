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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class HuntingBaseBlockEntity extends AbstractTownBuildingBlockEntity<HuntingBaseBuilding> implements MenuProvider {
	final HeatEndpoint endpoint;
	final LazyOptional<HeatEndpoint> endpointCap;
	@Getter
    private double temperatureModifier = 0;


	public HuntingBaseBlockEntity(BlockPos pos, BlockState state) {
		super(FHBlockEntityTypes.HUNTING_BASE.get(), pos, state);
		FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
		endpoint = HeatEndpoint.consumer(
				config.heatEndpointPriority.get(),
				config.heatConsumptionPerTick.get().floatValue()
		);
		endpointCap = LazyOptional.of(() -> endpoint);
	}

    public StructureScanResult scanStructure(HuntingBaseBuilding building) {
        RoomData room = BlockScanner.scanRoomDataFromBlock(level, worldPosition);
        if (room == null) {
            return StructureScanResult.DATA_UNAVAILABLE;
        }
        if (room.doors.isEmpty()) {
            room = room.withValidity(false);
        }
        if (!room.isValid()) {
            building.setOccupiedVolume(room.occupiedCells);
            return StructureScanResult.INVALID;
        }
        int tanningRacks = room.countInsideBlock(state -> state.is(FHTags.Blocks.TANNING_RACK.get()));
        FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
        int capacity = HuntingDailyModel.calculateCapacity(calculateSpaceRating(room.volume, room.area, scoring),
                room.area, config.floorBlocksPerWorkerSlot.get(), config.minimumWorkerSlots.get());
        building.setOccupiedVolume(room.occupiedCells);
        building.setVolume(room.volume);
        building.setArea(room.area);
        building.setTanningRackNum(tanningRacks);
        building.setMaxResidents(capacity);
        return StructureScanResult.VALID;
    }

	/**
	 * Static method to compute hunting base rating based on provided parameters.
	 * 
	 * @param volume the volume of the hunting base
	 * @param area the area of the hunting base
	 * @param temperature the base temperature
	 * @param temperatureModifier the temperature modifier
	 * @return computed rating value
	 */
	public static double computeRating(int volume, int area,  double temperature, double temperatureModifier) {
		// A new or invalid core may not have a usable geometry snapshot yet.
		if (volume <= 0 || area <= 0) return 0.0;
		FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
		FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
		double spaceWeight = config.spaceRatingWeight.get();
		double temperatureWeight = config.temperatureRatingWeight.get();
		double totalWeight = spaceWeight + temperatureWeight;
		if (totalWeight <= 0.0) return 0.0;
		double rating = (spaceWeight * calculateSpaceRating(volume, area, scoring)
				+ temperatureWeight * calculateTemperatureRating(
						temperature + temperatureModifier, scoring))
				/ totalWeight;
		return Double.isFinite(rating) ? rating : 0.0;
	}

	private static double calculateSpaceRating(
			int volume,
			int area,
			FHConfig.Server.Town.BuildingScoring scoring
	) {
		if (volume <= 0 || area <= 0) return 0.0;
		double rating = TownMathFunctions.calculateSpaceRating(
				volume,
				area,
				scoring.spaceAreaCoefficient.get(),
				scoring.spaceHeightLogCoefficient.get(),
				scoring.spaceHeightLogOffset.get(),
				scoring.spaceResponseScale.get(),
				scoring.spaceResponseExponent.get());
		return Double.isFinite(rating) ? rating : 0.0;
	}

	private static double calculateTemperatureRating(
			double temperature,
			FHConfig.Server.Town.BuildingScoring scoring
	) {
		return TownMathFunctions.calculateTemperatureRating(
				temperature,
				scoring.comfortableTemperatureCelsius.get(),
				scoring.minimumTemperatureRating.get(),
				scoring.temperatureRatingSlope.get(),
				scoring.temperatureRatingHalfPointDifferenceCelsius.get());
	}

	@Override
	public void tick() {
        double previousModifier = temperatureModifier;
		assert level != null;
		if (!level.isClientSide) {
			FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
			double heatConsumptionPerTick = config.heatConsumptionPerTick.get();
			if (heatConsumptionPerTick > 0.0 && endpoint.tryDrainHeat((float) heatConsumptionPerTick)) {
				temperatureModifier = Math.max(
						endpoint.getTempLevel() * config.heatTemperatureLevelScaleCelsius.get(),
						config.minimumHeatingModifierCelsius.get()
				);
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
		if (!level.isClientSide && Double.compare(previousModifier, temperatureModifier) != 0) {
            getBuilding().ifPresent(building -> building.updateBatch(() -> {
                building.setTemperatureModifier(temperatureModifier);
                building.setRating(computeRating(building.getVolume(), building.getArea(),
                        building.getTemperature(), temperatureModifier));
            }));
        }
	}

	@Nonnull
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
		if (capability == FHCapabilities.HEAT_EP.capability() && facing == Direction.NORTH) {
			return endpointCap.cast();
		}
		return super.getCapability(capability, facing);
	}

	@Override
	protected void refreshEnvironment(HuntingBaseBuilding building) {
        double temperature = BlockScanner.sampleTemperature(level, building.getOccupiedVolume());
        if (Double.isFinite(temperature)) {
            building.setTemperature(temperature);
        }
        building.setTemperatureModifier(temperatureModifier);
        building.setRating(computeRating(building.getVolume(), building.getArea(),
                building.getTemperature(), building.getTemperatureModifier()));
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

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new HuntingBaseMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.hunting_base");
    }
}
