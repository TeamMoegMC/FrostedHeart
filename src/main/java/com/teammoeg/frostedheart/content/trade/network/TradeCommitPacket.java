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

package com.teammoeg.frostedheart.content.trade.network;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.trade.gui.TradeContainer;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TradeCommitPacket implements CMessage {
    private Map<String, Integer> offer;

    public TradeCommitPacket(Map<String, Integer> offer) {
        super();
        this.offer = offer;
    }

    public TradeCommitPacket(FriendlyByteBuf buffer) {
        offer = SerializeUtil.readStringMap(buffer, new LinkedHashMap<>(), FriendlyByteBuf::readVarInt);
    }

    public void encode(FriendlyByteBuf buffer) {
        SerializeUtil.writeStringMap(buffer, offer, (p, b) -> b.writeVarInt(p));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            AbstractContainerMenu cont = context.get().getSender().containerMenu;
            if (cont instanceof TradeContainer) {
                TradeContainer trade = (TradeContainer) cont;
                trade.setOrder(offer);
                trade.commitTrade(context.get().getSender());
            }
        });
        context.get().setPacketHandled(true);
    }
}
