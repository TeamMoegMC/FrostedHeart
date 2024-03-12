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

package com.teammoeg.frostedheart.content.climate.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHDataManager;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.climate.data.DataEntry;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHDatapackSyncPacket implements FHMessage {
    DataEntry[] entries;

    public FHDatapackSyncPacket() {
        entries = FHDataManager.save();
    }

    public FHDatapackSyncPacket(PacketBuffer buffer) {
        decode(buffer);
    }

    public void decode(PacketBuffer buffer) {
        entries = new DataEntry[buffer.readVarInt()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new DataEntry(buffer);
        }
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeVarInt(entries.length);
        for (DataEntry de : entries)
            de.encode(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            FHDataManager.load(entries);
        });
        context.get().setPacketHandled(true);
    }
}
