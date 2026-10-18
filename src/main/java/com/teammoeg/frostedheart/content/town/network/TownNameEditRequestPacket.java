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

package com.teammoeg.frostedheart.content.town.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownNamingModel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Server-authoritative town rename; the sender's own team is always used. */
public class TownNameEditRequestPacket implements CMessage {
    private final String requestedName;

    public TownNameEditRequestPacket(String requestedName) {
        this.requestedName = requestedName;
    }

    public TownNameEditRequestPacket(FriendlyByteBuf buffer) {
        this.requestedName = buffer.readUtf(TownNamingModel.MAX_TOWN_NAME_LENGTH);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(requestedName, TownNamingModel.MAX_TOWN_NAME_LENGTH);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            TownNamingModel.normalizeTownName(requestedName).ifPresent(name -> {
                var teamData = CTeamDataManager.get(player);
                teamData.getOptional(FHSpecialDataTypes.TOWN_DATA).ifPresent(townData -> {
                    townData.createTeamTown().setName(name);
                    teamData.sendToOnline(FHNetwork.INSTANCE, new TownNameUpdatePacket(name));
                });
            });
        });
        context.get().setPacketHandled(true);
    }
}
