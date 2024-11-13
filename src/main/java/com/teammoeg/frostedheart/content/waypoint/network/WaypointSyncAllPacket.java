package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.base.network.FHMessage;
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
import java.util.Map;
import java.util.function.Supplier;

public class WaypointSyncAllPacket implements FHMessage {
    private Map<String, AbstractWaypoint> waypoints = new HashMap<>();

    public WaypointSyncAllPacket(ServerPlayer player) {
        FHCapabilities.WAYPOINT.getCapability(player).ifPresent((cap) -> {
            this.waypoints = cap.getWaypoints();
        });
    }

    public WaypointSyncAllPacket(FriendlyByteBuf buffer) {
        ListTag list = buffer.readNbt().getList("waypoints", Tag.TAG_COMPOUND);
        for (Tag nbt : list) {
            AbstractWaypoint waypoint = WaypointManager.registry.read((CompoundTag)nbt);
            if (waypoint != null) {
                waypoints.put(waypoint.getID(), waypoint);
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        ListTag list = new ListTag();
        CompoundTag nbt = new CompoundTag();
        waypoints.forEach((s, waypoints) -> list.add(WaypointManager.registry.write(waypoints)));
        nbt.put("waypoints", list);
        buffer.writeNbt(nbt);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            context.get().enqueueWork(() -> ClientWaypointManager.setWaypoints(waypoints));
        });
        context.get().setPacketHandled(true);
    }
}
