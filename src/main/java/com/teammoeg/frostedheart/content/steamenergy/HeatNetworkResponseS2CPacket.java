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

package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sends all endpoints and total output and intake of a HeatNetwork to the client.
 */
public class HeatNetworkResponseS2CPacket implements CMessage {
    ClientHeatNetworkData data;

    public HeatNetworkResponseS2CPacket(ClientHeatNetworkData data) {
        this.data = data;
    }

    public HeatNetworkResponseS2CPacket(FriendlyByteBuf buffer) {
        this.data = new ClientHeatNetworkData(
                buffer.readBlockPos(),
                buffer.readFloat(),
                buffer.readFloat(),
                SerializeUtil.readList(buffer, HeatEndpoint::readNetwork)
        );
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(data.pos);
        buffer.writeFloat(data.totalEndpointOutput);
        buffer.writeFloat(data.totalEndpointIntake);
        SerializeUtil.writeList(buffer, data.endpoints, HeatEndpoint::writeNetwork);
        buffer.writeBoolean(data.invalid);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // on the client side, update HeatNetwork's fields
            TemperatureGoogleRenderer.setHeatNetworkData(data);
        });
        context.get().setPacketHandled(true);
    }
}
