package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WaypointRemovePacket implements FHMessage {
    String id;

    public WaypointRemovePacket(String id) {
        this.id = id;
    }

    public WaypointRemovePacket(FriendlyByteBuf buffer) {
        this.id = buffer.readUtf();
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
