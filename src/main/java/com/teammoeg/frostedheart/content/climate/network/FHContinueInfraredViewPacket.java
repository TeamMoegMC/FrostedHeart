/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Receipt credit for the next bounded part; nextPart=-1 cancels a closed view. */
public record FHContinueInfraredViewPacket(int requestId, int nextPart) implements CMessage {
    public FHContinueInfraredViewPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(),buffer.readVarInt()-1);
    }

    @Override public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);buffer.writeVarInt(nextPart+1);
    }

    @Override public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player=context.get().getSender();
            if(player!=null)InfraredTransfers.continueTransfer(player,requestId,nextPart);
        });
        context.get().setPacketHandled(true);
    }
}
