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

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Idempotent history merge; every completed /town tick has a distinct town day. */
public class TownHistoryUpdatePacket implements TownViewPacket {
    private final TownHistoryEntry entry;
    private final long townDay;

    public TownHistoryUpdatePacket(TownHistoryEntry entry) {
        this(entry, -1L);
    }

    public TownHistoryUpdatePacket(TownHistoryEntry entry, long townDay) {
        this.entry = entry;
        this.townDay = townDay;
    }

    public TownHistoryUpdatePacket(FriendlyByteBuf buffer) {
        this.entry = CodecUtil.decodeOrThrow(TownHistoryEntry.CODEC.decode(
                DataOps.COMPRESSED, ObjectWriter.readObject(buffer)));
        this.townDay = buffer.readLong();
    }

    TownHistoryEntry entry() {
        return entry;
    }

    long townDay() {
        return townDay;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        CodecUtil.writeCodec(buffer, TownHistoryEntry.CODEC, entry);
        buffer.writeLong(townDay);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!com.teammoeg.frostedheart.content.town.client.TownClientUpdates.canApply(context.get().getNetworkManager())) return;
            com.teammoeg.frostedheart.content.town.client.TownClientUpdates.apply(town -> {
                    if (townDay >= 0L) town.applyHistoryUpdate(entry, townDay);
                    else town.applyHistoryUpdate(entry);
                });
        });
        context.get().setPacketHandled(true);
    }
}
