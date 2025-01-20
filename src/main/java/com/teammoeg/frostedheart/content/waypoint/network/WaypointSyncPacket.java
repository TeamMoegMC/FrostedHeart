package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WaypointSyncPacket(AbstractWaypoint waypoint) implements CMessage {

    public WaypointSyncPacket(FriendlyByteBuf buffer) {
        this(WaypointManager.registry.read(buffer));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        WaypointManager.registry.write(buffer, waypoint);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                ClientWaypointManager.putWaypointWithoutSendingPacket(waypoint);
            } else {
                WaypointManager.getManager(context.get().getSender()).putWaypointWithoutSendingPacket(waypoint);
            }
        });
        context.get().setPacketHandled(true);
    }
}
