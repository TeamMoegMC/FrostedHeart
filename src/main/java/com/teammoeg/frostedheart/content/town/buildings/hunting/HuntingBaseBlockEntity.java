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

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;

import lombok.Getter;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

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

	public boolean scanStructure(HuntingBaseBuilding building) {
		BlockPos housePos = this.getBlockPos();
		RoomData rd=BlockScanner.scanRoomDataFromBlock(level,housePos);
		if (rd!=null&&rd.doors.size()>0) {
			building.setVolume(rd.volume);
			building.setArea(rd.area);
			building.setTemperature(rd.calculateTemperature(level));
			building.setOccupiedVolume(rd.calculateOccupiedVolume());
			building.setTanningRackNum(rd.countInsideBlock(t->t.is(FHTags.Blocks.TANNING_RACK.get())));
			building.setRating(computeRating(building.getVolume(), building.getArea(), building.getTemperature(), this.getTemperatureModifier()));
			FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
			FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
			building.setMaxResidents(HuntingDailyModel.calculateCapacity(
					calculateSpaceRating(rd.volume, rd.area, scoring),
					rd.area, config.floorBlocksPerWorkerSlot.get(),
					config.minimumWorkerSlots.get()));
			return true;
			
		}
		return false;
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
		FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
		FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
		double spaceWeight = config.spaceRatingWeight.get();
		double temperatureWeight = config.temperatureRatingWeight.get();
		double totalWeight = spaceWeight + temperatureWeight;
		if (totalWeight <= 0.0) return 0.0;
		return (spaceWeight * calculateSpaceRating(volume, area, scoring)
				+ temperatureWeight * calculateTemperatureRating(
						temperature + temperatureModifier, scoring))
				/ totalWeight;
	}

	private static double calculateSpaceRating(
			int volume,
			int area,
			FHConfig.Server.Town.BuildingScoring scoring
	) {
		return TownMathFunctions.calculateSpaceRating(
				volume,
				area,
				scoring.spaceAreaCoefficient.get(),
				scoring.spaceHeightLogCoefficient.get(),
				scoring.spaceHeightLogOffset.get(),
				scoring.spaceResponseScale.get(),
				scoring.spaceResponseExponent.get());
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
		super.refresh(building);
		building.setTemperatureModifier(this.getTemperatureModifier());
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
