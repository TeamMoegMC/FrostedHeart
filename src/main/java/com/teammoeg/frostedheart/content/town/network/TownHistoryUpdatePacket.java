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

    public TownHistoryUpdatePacket(TownHistoryEntry entry) {
        this.entry = entry;
    }

    public TownHistoryUpdatePacket(FriendlyByteBuf buffer) {
        this.entry = CodecUtil.decodeOrThrow(TownHistoryEntry.CODEC.decode(
                DataOps.COMPRESSED, ObjectWriter.readObject(buffer)));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        CodecUtil.writeCodec(buffer, TownHistoryEntry.CODEC, entry);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(town -> town.applyHistoryUpdate(entry)));
        context.get().setPacketHandled(true);
    }
}
