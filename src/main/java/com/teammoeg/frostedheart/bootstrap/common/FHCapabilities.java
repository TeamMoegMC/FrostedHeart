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

package com.teammoeg.frostedheart.bootstrap.common;

import static com.teammoeg.chorda.capability.CapabilityRegistry.*;

import com.teammoeg.chorda.capability.types.codec.CodecCapabilityType;
import com.teammoeg.chorda.capability.types.nbt.NBTCapabilityType;
import com.teammoeg.chorda.capability.types.nonpresistent.TransientCapability;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.player.BodyHeatingCapability;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.content.health.dailykitchen.WantedFoodCapability;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.RobotChunk;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatStorageCapability;
import com.teammoeg.frostedheart.content.town.ChunkTownResourceCapability;
import com.teammoeg.frostedheart.content.trade.PlayerTradeData;
import com.teammoeg.frostedheart.content.utility.DeathInventoryData;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.content.waypoint.capability.WaypointCapability;

public class FHCapabilities {
	
	
	public static final NBTCapabilityType<WorldClimate> CLIMATE_DATA=register(WorldClimate.class);
	public static final NBTCapabilityType<DeathInventoryData> DEATH_INV=register(DeathInventoryData.class);
	public static final NBTCapabilityType<PlayerTemperatureData> PLAYER_TEMP=register(PlayerTemperatureData.class);
	//public static final NBTCapabilityType<EnergyCore> ENERGY=register(EnergyCore.class);
	public static final NBTCapabilityType<ScenarioConductor> SCENARIO=register(ScenarioConductor.class);
	public static final NBTCapabilityType<PlayerTradeData> TRADE_PLAYER=register(PlayerTradeData.class);
	
	public static final CodecCapabilityType<ChunkHeatData> CHUNK_HEAT=register(ChunkHeatData.class,ChunkHeatData.CODEC);
	public static final NBTCapabilityType<HeatEndpoint> HEAT_EP=register(HeatEndpoint.class);
	public static final TransientCapability<HeatStorageCapability> ITEM_HEAT=registerTransient(HeatStorageCapability.class);
	public static final TransientCapability<LogisticNetwork> LOGISTIC=registerTransient(LogisticNetwork.class);

	public static final NBTCapabilityType<WantedFoodCapability> WANTED_FOOD=register(WantedFoodCapability.class);
	public static final NBTCapabilityType<ChunkTownResourceCapability> CHUNK_TOWN_RESOURCE=register(ChunkTownResourceCapability.class);
	public static final NBTCapabilityType<RobotChunk> ROBOTIC_LOGISTIC_CHUNK=register(RobotChunk.class);
	public static final NBTCapabilityType<WaypointCapability> WAYPOINT=register(WaypointCapability.class);
	public static final NBTCapabilityType<WaterLevelCapability> PLAYER_WATER_LEVEL = register(WaterLevelCapability.class);
	public static final NBTCapabilityType<NutritionCapability> PLAYER_NUTRITION = register(NutritionCapability.class);
	public static final TransientCapability<BodyHeatingCapability> EQUIPMENT_HEATING=registerTransient(BodyHeatingCapability.class);

	
	public static void setup() {
	
	}

}
