package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointRenderer;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class WaypointSyncPacket implements FHMessage {
    private final AbstractWaypoint waypoint;

    public WaypointSyncPacket(AbstractWaypoint waypoint) {
        this.waypoint = waypoint;
    }

    public WaypointSyncPacket(PacketBuffer buffer) {
        this.waypoint = WaypointManager.registry.read(buffer);
    }

    @Override
    public void encode(PacketBuffer buffer) {
        WaypointManager.registry.write(buffer, waypoint);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            WaypointRenderer.putWaypoint(waypoint);
        });
        context.get().setPacketHandled(true);
    }
}
