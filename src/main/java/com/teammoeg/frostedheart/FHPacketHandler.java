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

import com.teammoeg.frostedheart.climate.network.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.climate.network.FHDatapackSyncPacket;
import com.teammoeg.frostedheart.climate.network.FHTemperatureDisplayPacket;
import com.teammoeg.frostedheart.research.network.FHChangeActiveResearchPacket;
import com.teammoeg.frostedheart.research.network.FHClueProgressSyncPacket;
import com.teammoeg.frostedheart.research.network.FHDrawingDeskOperationPacket;
import com.teammoeg.frostedheart.research.network.FHEffectProgressSyncPacket;
import com.teammoeg.frostedheart.research.network.FHEffectTriggerPacket;
import com.teammoeg.frostedheart.research.network.FHEnergyDataSyncPacket;
import com.teammoeg.frostedheart.research.network.FHResearchControlPacket;
import com.teammoeg.frostedheart.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.research.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedheart.research.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedheart.scenario.network.ClientLinkClickedPacket;
import com.teammoeg.frostedheart.scenario.network.ClientScenarioResponsePacket;
import com.teammoeg.frostedheart.scenario.network.FHClientReadyPacket;
import com.teammoeg.frostedheart.scenario.network.ServerScenarioCommandPacket;
import com.teammoeg.frostedheart.scenario.network.ServerSenarioTextPacket;
import com.teammoeg.frostedheart.trade.network.BargainRequestPacket;
import com.teammoeg.frostedheart.trade.network.BargainResponse;
import com.teammoeg.frostedheart.trade.network.TradeCommitPacket;
import com.teammoeg.frostedheart.trade.network.TradeUpdatePacket;
import com.teammoeg.frostedheart.util.FHVersion;

import blusunrize.immersiveengineering.common.network.MessageTileSync;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class FHPacketHandler {

    private static SimpleChannel CHANNEL;

    public static SimpleChannel get() {
        return CHANNEL;
    }

    public static void register() {
        String VERSION = FHMain.local.fetchVersion().orElse(FHVersion.empty).getOriginal();
        System.out.println("[TWR Version Check] FH Network Version: " + VERSION);
        CHANNEL = NetworkRegistry.newSimpleChannel(FHMain.rl("network"), () -> VERSION,
                VERSION::equals, VERSION::equals);
        int id = 0;

        // CHANNEL.registerMessage(id++, ChunkWatchPacket.class,
        // ChunkWatchPacket::encode, ChunkWatchPacket::new, ChunkWatchPacket::handle);
        // CHANNEL.registerMessage(id++, ChunkUnwatchPacket.class,
        // ChunkUnwatchPacket::encode, ChunkUnwatchPacket::new,
        // ChunkUnwatchPacket::handle);
        CHANNEL.registerMessage(id++, MessageTileSync.class, MessageTileSync::toBytes, MessageTileSync::new,
                (t, ctx) -> {
                    t.process(ctx);
                    ctx.get().setPacketHandled(true);
                });
        // CHANNEL.registerMessage(id++, TemperatureChangePacket.class,
        // TemperatureChangePacket::encode, TemperatureChangePacket::new,
        // TemperatureChangePacket::handle);
        CHANNEL.registerMessage(id++, FHBodyDataSyncPacket.class, FHBodyDataSyncPacket::encode, FHBodyDataSyncPacket::new,
                FHBodyDataSyncPacket::handle);
        CHANNEL.registerMessage(id++, FHDatapackSyncPacket.class, FHDatapackSyncPacket::encode,
                FHDatapackSyncPacket::new, FHDatapackSyncPacket::handle);
        CHANNEL.registerMessage(id++, FHResearchRegistrtySyncPacket.class, FHResearchRegistrtySyncPacket::encode,
                FHResearchRegistrtySyncPacket::new, FHResearchRegistrtySyncPacket::handle);
        CHANNEL.registerMessage(id++, FHResearchDataSyncPacket.class, FHResearchDataSyncPacket::encode,
                FHResearchDataSyncPacket::new, FHResearchDataSyncPacket::handle);
        CHANNEL.registerMessage(id++, FHResearchDataUpdatePacket.class, FHResearchDataUpdatePacket::encode,
                FHResearchDataUpdatePacket::new, FHResearchDataUpdatePacket::handle);
        CHANNEL.registerMessage(id++, FHClueProgressSyncPacket.class, FHClueProgressSyncPacket::encode,
                FHClueProgressSyncPacket::new, FHClueProgressSyncPacket::handle);
        CHANNEL.registerMessage(id++, FHClimatePacket.class, FHClimatePacket::encode, FHClimatePacket::new,
                FHClimatePacket::handle);
        CHANNEL.registerMessage(id++, FHEffectTriggerPacket.class, FHEffectTriggerPacket::encode,
                FHEffectTriggerPacket::new, FHEffectTriggerPacket::handle);
        CHANNEL.registerMessage(id++, FHResearchControlPacket.class, FHResearchControlPacket::encode,
                FHResearchControlPacket::new, FHResearchControlPacket::handle);
        CHANNEL.registerMessage(id++, FHChangeActiveResearchPacket.class, FHChangeActiveResearchPacket::encode,
                FHChangeActiveResearchPacket::new, FHChangeActiveResearchPacket::handle);
        CHANNEL.registerMessage(id++, FHDrawingDeskOperationPacket.class, FHDrawingDeskOperationPacket::encode,
                FHDrawingDeskOperationPacket::new, FHDrawingDeskOperationPacket::handle);
        CHANNEL.registerMessage(id++, FHEffectProgressSyncPacket.class, FHEffectProgressSyncPacket::encode,
                FHEffectProgressSyncPacket::new, FHEffectProgressSyncPacket::handle);
        CHANNEL.registerMessage(id++, FHEnergyDataSyncPacket.class, FHEnergyDataSyncPacket::encode,
                FHEnergyDataSyncPacket::new, FHEnergyDataSyncPacket::handle);
        CHANNEL.registerMessage(id++, FHTemperatureDisplayPacket.class, FHTemperatureDisplayPacket::encode,
                FHTemperatureDisplayPacket::new, FHTemperatureDisplayPacket::handle);
        CHANNEL.registerMessage(id++, BargainRequestPacket.class, BargainRequestPacket::encode,
                BargainRequestPacket::new, BargainRequestPacket::handle);
        CHANNEL.registerMessage(id++, BargainResponse.class, BargainResponse::encode,
                BargainResponse::new, BargainResponse::handle);
        CHANNEL.registerMessage(id++, TradeCommitPacket.class, TradeCommitPacket::encode,
                TradeCommitPacket::new, TradeCommitPacket::handle);
        CHANNEL.registerMessage(id++, TradeUpdatePacket.class, TradeUpdatePacket::encode,
                TradeUpdatePacket::new, TradeUpdatePacket::handle);
        CHANNEL.registerMessage(id++, ClientScenarioResponsePacket.class, ClientScenarioResponsePacket::encode,
        	ClientScenarioResponsePacket::new, ClientScenarioResponsePacket::handle);
        CHANNEL.registerMessage(id++, ServerScenarioCommandPacket.class, ServerScenarioCommandPacket::encode,
        	ServerScenarioCommandPacket::new, ServerScenarioCommandPacket::handle);
        CHANNEL.registerMessage(id++, ServerSenarioTextPacket.class, ServerSenarioTextPacket::encode,
        	ServerSenarioTextPacket::new, ServerSenarioTextPacket::handle);
        CHANNEL.registerMessage(id++, FHClientReadyPacket.class, FHClientReadyPacket::encode,
        		FHClientReadyPacket::new, FHClientReadyPacket::handle);
        CHANNEL.registerMessage(id++, ClientLinkClickedPacket.class, ClientLinkClickedPacket::encode, ClientLinkClickedPacket::new, 
        		ClientLinkClickedPacket::handle);
        
    }

    public static void send(PacketDistributor.PacketTarget target, Object message) {
        CHANNEL.send(target, message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

}