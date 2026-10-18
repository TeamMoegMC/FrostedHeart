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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusClientCache;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TownOperationalStatusResponsePacket implements CMessage {
    private final TownOperationalStatus status;

    public TownOperationalStatusResponsePacket(TownOperationalStatus status) {
        this.status = status;
    }

    public TownOperationalStatusResponsePacket(FriendlyByteBuf buffer) {
        this.status = TownOperationalStatusPacketCodec.read(buffer);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        TownOperationalStatusPacketCodec.write(buffer, status);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TownOperationalStatusClientCache.accept(status));
        context.get().setPacketHandled(true);
    }
}
