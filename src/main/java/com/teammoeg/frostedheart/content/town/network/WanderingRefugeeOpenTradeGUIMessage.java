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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 *  wanderer open trade gui message.<br>
 *  Client to server message.
 */
public class WanderingRefugeeOpenTradeGUIMessage implements CMessage {
    int refugeeID;

    /**
     * decoder
     */
    public WanderingRefugeeOpenTradeGUIMessage(FriendlyByteBuf buffer){
        refugeeID = buffer.readVarInt();
    }

    public WanderingRefugeeOpenTradeGUIMessage(int refugeeID) {
        this.refugeeID = refugeeID;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(refugeeID);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if(player==null){
                FHMain.LOGGER.error("Network error: player in WanderingRefugeeOpenTradeGUIMessage is null!");
                return;
            }
            WanderingRefugee refugee = (WanderingRefugee) player.level().getEntity(refugeeID);
            if(refugee==null) {
                FHMain.LOGGER.error("Network error: refugee in WanderingRefugeeOpenTradeGUIMessage is null!");
                return;
            }
            refugee.openTradingScreen(player);
        });
    }
}
