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

package com.teammoeg.frostedheart.content.research.insight;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.network.FHInsightSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

public class InsightHandler {
    // TODO: This now ticks every packet, change this to send only when needed.

    /**
     * Sync insight data to the client for display purpose.
     */
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer) {
            PacketDistributor.PacketTarget currentPlayer = PacketDistributor.PLAYER.with(
                    () -> (ServerPlayer) event.player);
            FHNetwork.send(currentPlayer, new FHInsightSyncPacket(ResearchDataAPI.getData(event.player).getHolder()));
        }
    }
}
