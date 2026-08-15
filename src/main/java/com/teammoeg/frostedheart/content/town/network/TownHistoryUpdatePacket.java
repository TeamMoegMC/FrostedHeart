/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Idempotent history merge; every completed /town tick has a distinct town day. */
public class TownHistoryUpdatePacket implements CMessage {
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
        context.get().enqueueWork(() -> CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(town -> {
                    if (townDay >= 0L) town.applyHistoryUpdate(entry, townDay);
                    else town.applyHistoryUpdate(entry);
                }));
        context.get().setPacketHandled(true);
    }
}
