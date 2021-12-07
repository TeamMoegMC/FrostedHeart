/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.network;

import com.teammoeg.frostedheart.data.DataEntry;
import com.teammoeg.frostedheart.data.FHDataManager;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class FHDatapackSyncPacket {
    DataEntry[] entries;

    public FHDatapackSyncPacket() {
        entries = FHDataManager.save();
    }

    FHDatapackSyncPacket(PacketBuffer buffer) {
        decode(buffer);
    }

    void decode(PacketBuffer buffer) {
        entries = new DataEntry[buffer.readVarInt()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new DataEntry(buffer);
        }
    }

    void encode(PacketBuffer buffer) {
        buffer.writeVarInt(entries.length);
        for (DataEntry de : entries)
            de.encode(buffer);
    }

    void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            FHDataManager.load(entries);
        });
        context.get().setPacketHandled(true);
    }
}
