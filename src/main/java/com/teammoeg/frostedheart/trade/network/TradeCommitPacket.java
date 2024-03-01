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

package com.teammoeg.frostedheart.trade.network;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.network.FHMessage;
import com.teammoeg.frostedheart.trade.gui.TradeContainer;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class TradeCommitPacket implements FHMessage {
    private Map<String, Integer> offer;

    public TradeCommitPacket(Map<String, Integer> offer) {
        super();
        this.offer = offer;
    }

    public TradeCommitPacket(PacketBuffer buffer) {
        offer = SerializeUtil.readStringMap(buffer, new LinkedHashMap<>(), PacketBuffer::readVarInt);
    }

    public void encode(PacketBuffer buffer) {
        SerializeUtil.writeStringMap(buffer, offer, (p, b) -> b.writeVarInt(p));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Container cont = context.get().getSender().openContainer;
            if (cont instanceof TradeContainer) {
                TradeContainer trade = (TradeContainer) cont;
                trade.setOrder(offer);
                trade.commitTrade(context.get().getSender());
            }
        });
        context.get().setPacketHandled(true);
    }
}
