/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
