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

package com.teammoeg.frostedheart;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorModifyPacket;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.content.climate.network.FHDatapackSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.FHTemperatureDisplayPacket;
import com.teammoeg.frostedheart.content.research.network.FHChangeActiveResearchPacket;
import com.teammoeg.frostedheart.content.research.network.FHClueProgressSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHDrawingDeskOperationPacket;
import com.teammoeg.frostedheart.content.research.network.FHEffectProgressSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHEffectTriggerPacket;
import com.teammoeg.frostedheart.content.research.network.FHEnergyDataSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchAttributeSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchControlPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchSyncEndPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchSyncPacket;
import com.teammoeg.frostedheart.content.scenario.network.ClientLinkClickedPacket;
import com.teammoeg.frostedheart.content.scenario.network.ClientScenarioResponsePacket;
import com.teammoeg.frostedheart.content.scenario.network.FHClientReadyPacket;
import com.teammoeg.frostedheart.content.scenario.network.FHClientSettingsPacket;
import com.teammoeg.frostedheart.content.scenario.network.ServerScenarioCommandPacket;
import com.teammoeg.frostedheart.content.scenario.network.ServerSenarioActPacket;
import com.teammoeg.frostedheart.content.scenario.network.ServerSenarioScenePacket;
import com.teammoeg.frostedheart.content.steamenergy.EndPointDataPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncAllPacket;
import com.teammoeg.frostedheart.content.town.TeamTownDataS2CPacket;
import com.teammoeg.frostedheart.content.trade.network.BargainRequestPacket;
import com.teammoeg.frostedheart.content.trade.network.BargainResponse;
import com.teammoeg.frostedheart.content.trade.network.TradeCommitPacket;
import com.teammoeg.frostedheart.content.trade.network.TradeUpdatePacket;

import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class FHNetwork {

    private static SimpleChannel CHANNEL;
    private static Map<Class<? extends FHMessage>,ResourceLocation> classesId=new HashMap<>();
    public static SimpleChannel get() {
        return CHANNEL;
    }
    private static int iid=0;
    /**
     * Register Message Type, would automatically use method in FHMessage as serializer and <init>(PacketBuffer) as deserializer
     * */
    public static synchronized <T extends FHMessage> void registerMessage(String name,Class<T> msg) {
    	classesId.put(msg,FHMain.rl(name));
		try {
			Constructor<T> ctor = msg.getConstructor(PacketBuffer.class);
	    	CHANNEL.registerMessage(++iid,msg,FHMessage::encode,pb->{
	    		try {
					return ctor.newInstance(pb);
				} catch (IllegalAccessException | IllegalArgumentException | InstantiationException |
                         InvocationTargetException e) {
					throw new RuntimeException("Can not create message "+msg.getSimpleName(), e);
				}
            },FHMessage::handle);
		} catch (NoSuchMethodException | SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
    /**
     * Register Message Type, should provide a deserializer
     * */
    public static synchronized <T extends FHMessage> void registerMessage(String name,Class<T> msg,Function<PacketBuffer,T> func) {
    	classesId.put(msg,FHMain.rl(name));
	    CHANNEL.registerMessage(++iid,msg,FHMessage::encode,func,FHMessage::handle);
    }
    public static ResourceLocation getId(Class<? extends FHMessage> cls) {
    	return classesId.get(cls);
    }
    public static void register() {
        String VERSION = ModList.get().getModContainerById(FHMain.MODID).get().getModInfo().getVersion().toString();
        FHMain.LOGGER.info("FH Network Version: " + VERSION);
        CHANNEL = NetworkRegistry.newSimpleChannel(FHMain.rl("network"), () -> VERSION, VERSION::equals, VERSION::equals);

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
        registerMessage("temperature_data", FHDatapackSyncPacket.class);

        registerMessage("climate_data", FHClimatePacket.class);
        registerMessage("temperature_display", FHTemperatureDisplayPacket.class);
        
        //Research Messages
        registerMessage("research_registry", FHResearchRegistrtySyncPacket.class);
        registerMessage("research_sync", FHResearchSyncPacket.class);
        registerMessage("research_sync_end", FHResearchSyncEndPacket.class);
        registerMessage("research_data", FHResearchDataSyncPacket.class);
        registerMessage("research_data_update", FHResearchDataUpdatePacket.class);
        registerMessage("research_clue", FHClueProgressSyncPacket.class);
        registerMessage("research_attribute", FHResearchAttributeSyncPacket.class);
        registerMessage("effect_trigger", FHEffectTriggerPacket.class);
        registerMessage("research_control", FHResearchControlPacket.class);
        registerMessage("research_select", FHChangeActiveResearchPacket.class);
        registerMessage("research_drawdesk", FHDrawingDeskOperationPacket.class);
        registerMessage("research_effect", FHEffectProgressSyncPacket.class);
        registerMessage("research_energy_data", FHEnergyDataSyncPacket.class);
        
        //Trade Messages
        registerMessage("bargain_request", BargainRequestPacket.class);
        registerMessage("bargain_response", BargainResponse.class);
        registerMessage("trade_commit", TradeCommitPacket.class);
        registerMessage("trade_update", TradeUpdatePacket.class);
        
        //Scenario System
        registerMessage("scenario_client_op", ClientScenarioResponsePacket.class);
        registerMessage("scenario_server_command", ServerScenarioCommandPacket.class);
        registerMessage("scenario_scene", ServerSenarioScenePacket.class);
        registerMessage("scenario_ready", FHClientReadyPacket.class);
        registerMessage("scenario_link", ClientLinkClickedPacket.class);
        registerMessage("scenario_act", ServerSenarioActPacket.class);
        registerMessage("scenario_settings", FHClientSettingsPacket.class);

        // Heat Messages
        registerMessage("heat_endpoint", EndPointDataPacket.class);

        // Town Messages
        registerMessage("team_town_data_s2c", TeamTownDataS2CPacket.class);
        
        // Generator Messages
        registerMessage("generator_upgrade", GeneratorModifyPacket.class);

        // Tip Messages
        registerMessage("single_tip", DisplayTipPacket.class);
        registerMessage("custom_tip", DisplayCustomTipPacket.class);

        // Waypoint Messages
        registerMessage("waypoint_sync", WaypointSyncPacket.class);
        registerMessage("waypoint_sync_all", WaypointSyncAllPacket.class);
    }

    public static void send(PacketDistributor.PacketTarget target, FHMessage message) {
        CHANNEL.send(target, message);
    }

    public static void sendToServer(FHMessage message) {
        CHANNEL.sendToServer(message);
    }

}