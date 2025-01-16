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

import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager.DataType;
import com.teammoeg.chorda.network.FHMessage;
import com.teammoeg.chorda.util.io.IdDataPair;
import com.teammoeg.chorda.util.io.SerializeUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class FHDatapackSyncPacket implements FHMessage {
    List<IdDataPair<?>> entries;
    DataType<?> type;
    public FHDatapackSyncPacket(DataType<?> type) {
        entries = FHDataManager.save(type);
        this.type=type;
    }

    public FHDatapackSyncPacket(FriendlyByteBuf buffer) {
        decode(buffer);
    }

    public void decode(FriendlyByteBuf buffer) {
    	type = DataType.types.get(buffer.readByte());
        entries = SerializeUtil.readList(buffer,type::read);
    }

    public void encode(FriendlyByteBuf buffer) {
    	buffer.writeByte(type.getId());
        SerializeUtil.writeList(buffer, (List)entries, type::write);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            FHDataManager.load(type,entries);
        });
        context.get().setPacketHandled(true);
    }
}
