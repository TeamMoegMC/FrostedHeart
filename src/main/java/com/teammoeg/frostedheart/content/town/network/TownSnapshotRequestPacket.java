package com.teammoeg.frostedheart.content.town.network;

import java.util.function.Supplier;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.TownSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** A single coalesced request; the server resolves current ownership itself. */
public final class TownSnapshotRequestPacket implements CMessage {
    public TownSnapshotRequestPacket() {}
    public TownSnapshotRequestPacket(FriendlyByteBuf buffer) {}
    @Override public void encode(FriendlyByteBuf buffer) {}
    @Override public void handle(Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> { if (ctx.getSender() != null) TownSync.requestFull(ctx.getSender()); });
        ctx.setPacketHandled(true);
    }
}
