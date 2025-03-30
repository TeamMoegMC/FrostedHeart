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

package com.teammoeg.frostedheart;

import com.teammoeg.chorda.network.CBaseNetwork;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.FHNotifyChunkHeatUpdatePacket;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.FHRequestInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.FHResponseInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.C2SOpenClothesScreenMessage;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.content.climate.network.FHTemperatureDisplayPacket;
import com.teammoeg.frostedheart.content.health.network.C2SOpenNutritionScreenMessage;
import com.teammoeg.frostedheart.content.scenario.network.C2SLinkClickedPacket;
import com.teammoeg.frostedheart.content.scenario.network.C2SRenderingStatusMessage;
import com.teammoeg.frostedheart.content.scenario.network.C2SScenarioCookies;
import com.teammoeg.frostedheart.content.scenario.network.C2SScenarioResponsePacket;
import com.teammoeg.frostedheart.content.scenario.network.C2SClientReadyPacket;
import com.teammoeg.frostedheart.content.scenario.network.C2SSettingsPacket;
import com.teammoeg.frostedheart.content.scenario.network.S2CRequestCookieMessage;
import com.teammoeg.frostedheart.content.scenario.network.S2CScenarioCommandPacket;
import com.teammoeg.frostedheart.content.scenario.network.S2CSenarioActPacket;
import com.teammoeg.frostedheart.content.scenario.network.S2CSenarioScenePacket;
import com.teammoeg.frostedheart.content.scenario.network.S2CSetCookiesMessage;
import com.teammoeg.frostedheart.content.scenario.network.S2CWaitTransMessage;
import com.teammoeg.frostedheart.content.steamenergy.EndPointDataPacket;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkRequestC2SPacket;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkResponseS2CPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipRequestPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayPopupPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import com.teammoeg.frostedheart.content.town.TeamTownDataS2CPacket;
import com.teammoeg.frostedheart.content.trade.network.BargainRequestPacket;
import com.teammoeg.frostedheart.content.trade.network.BargainResponse;
import com.teammoeg.frostedheart.content.trade.network.TradeCommitPacket;
import com.teammoeg.frostedheart.content.trade.network.TradeUpdatePacket;
import com.teammoeg.frostedheart.content.water.network.PlayerDrinkWaterMessage;
import com.teammoeg.frostedheart.content.water.network.PlayerWaterLevelSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointRemovePacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncAllPacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;

public class FHNetwork extends CBaseNetwork {
	private FHNetwork() {
		super(FHMain.MODID);
	}
	public static final FHNetwork INSTANCE=new FHNetwork();
    @Override
	public void registerMessages() {
        // CHANNEL.registerMessage(id++, ChunkWatchPacket.class,
        // ChunkWatchPacket::encode, ChunkWatchPacket::new, ChunkWatchPacket::handle);
        // CHANNEL.registerMessage(id++, ChunkUnwatchPacket.class,
        // ChunkUnwatchPacket::encode, ChunkUnwatchPacket::new,
        // ChunkUnwatchPacket::handle);
        // CHANNEL.registerMessage(id++, TemperatureChangePacket.class,
        // TemperatureChangePacket::encode, TemperatureChangePacket::new,
        // TemperatureChangePacket::handle);

        //Climate Messages
        registerMessage("body_data", FHBodyDataSyncPacket.class);
        //registerMessage("temperature_data", FHDatapackSyncPacket.class);

        registerMessage("climate_data", FHClimatePacket.class);
        registerMessage("temperature_display", FHTemperatureDisplayPacket.class);
        registerMessage("open_clothes", C2SOpenClothesScreenMessage.class);


        //Trade Messages
        registerMessage("bargain_request", BargainRequestPacket.class);
        registerMessage("bargain_response", BargainResponse.class);
        registerMessage("trade_commit", TradeCommitPacket.class);
        registerMessage("trade_update", TradeUpdatePacket.class);

        //Scenario Messages
        registerMessage("scenario_client_op", C2SScenarioResponsePacket.class);
        registerMessage("scenario_server_command", S2CScenarioCommandPacket.class);
        registerMessage("scenario_scene", S2CSenarioScenePacket.class);
        registerMessage("scenario_ready", C2SClientReadyPacket.class);
        registerMessage("scenario_link", C2SLinkClickedPacket.class);
        registerMessage("scenario_act", S2CSenarioActPacket.class);
        registerMessage("scenario_settings", C2SSettingsPacket.class);
        registerMessage("scenario_set_cookie", S2CSetCookiesMessage.class);
        registerMessage("scenario_get_cookie", S2CRequestCookieMessage.class);
        registerMessage("scenario_send_cookie", C2SScenarioCookies.class);
        registerMessage("scenario_render_status",C2SRenderingStatusMessage.class);
        registerMessage("scenario_wait_render",S2CWaitTransMessage.class);
        

        // Heat Messages
        registerMessage("heat_endpoint", EndPointDataPacket.class);
        registerMessage("heat_network_request_c2s", HeatNetworkRequestC2SPacket.class);
        registerMessage("heat_network_response_s2c", HeatNetworkResponseS2CPacket.class);

        // Town Messages
        registerMessage("team_town_data_s2c", TeamTownDataS2CPacket.class);

        // Generator Messages
        //registerMessage("generator_upgrade", GeneratorModifyPacket.class);

        // Tip Messages
        registerMessage("single_tip", DisplayTipPacket.class);
        registerMessage("custom_tip", DisplayCustomTipPacket.class);
        registerMessage("display_request", DisplayCustomTipRequestPacket.class);
        registerMessage("popup", DisplayPopupPacket.class);

        // Waypoint Messages
        registerMessage("waypoint_remove", WaypointRemovePacket.class);
        registerMessage("waypoint_sync", WaypointSyncPacket.class);
        registerMessage("waypoint_sync_all", WaypointSyncAllPacket.class);

        // Water level
        registerMessage("water_level", PlayerWaterLevelSyncPacket.class);
        registerMessage("player_drink_water", PlayerDrinkWaterMessage.class);

        // Nutrition
        //registerMessage("nutrition", PlayerNutritionSyncPacket.class);
        registerMessage("open_nutrition", C2SOpenNutritionScreenMessage.class);

        // Infrared View
        registerMessage("infrared_view_c2s", FHRequestInfraredViewDataSyncPacket.class);
        registerMessage("infrared_view_s2c", FHResponseInfraredViewDataSyncPacket.class);
        registerMessage("notify_chunk_heat_update", FHNotifyChunkHeatUpdatePacket.class);
    }
}