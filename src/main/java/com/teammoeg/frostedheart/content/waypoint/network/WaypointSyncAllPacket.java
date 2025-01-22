package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.util.io.SerializeUtil;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class WaypointSyncAllPacket implements CMessage {
    private Map<String, AbstractWaypoint> waypoints = new HashMap<>();

    public WaypointSyncAllPacket(ServerPlayer player) {
        FHCapabilities.WAYPOINT.getCapability(player).ifPresent((cap) -> this.waypoints = cap.getWaypoints());
    }

    public WaypointSyncAllPacket(FriendlyByteBuf buffer) {
        List<AbstractWaypoint> list=SerializeUtil.readList(buffer, WaypointManager.registry::read);
        for (AbstractWaypoint waypoint : list) {
            if (waypoint != null) {
                waypoints.put(waypoint.getId(), waypoint);
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        SerializeUtil.writeList2(buffer, waypoints.values(), WaypointManager.registry::write);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
            context.get().enqueueWork(() -> ClientWaypointManager.setWaypoints(waypoints))
        );
        context.get().setPacketHandled(true);
    }
}
