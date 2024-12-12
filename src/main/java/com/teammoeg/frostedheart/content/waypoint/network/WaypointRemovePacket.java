package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WaypointRemovePacket(String id) implements FHMessage {

    public WaypointRemovePacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                ClientWaypointManager.removeWaypointWithoutSendingPacket(id);
            } else {
                WaypointManager.getManager(context.get().getSender()).removeWaypointWithoutSendingPacket(id);
            }
        });
        context.get().setPacketHandled(true);
    }
}
