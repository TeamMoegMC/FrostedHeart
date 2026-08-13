/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownNamingModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
