package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointRenderer;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class WaypointSyncAllPacket implements FHMessage {
    private Map<String, AbstractWaypoint> waypoints = new HashMap<>();

    public WaypointSyncAllPacket(ServerPlayerEntity player) {
        FHCapabilities.WAYPOINT.getCapability(player).ifPresent((cap) -> {
            this.waypoints = cap.getWaypoints();
        });
    }

    public WaypointSyncAllPacket(PacketBuffer buffer) {
        ListNBT list = buffer.readCompoundTag().getList("waypoints", Constants.NBT.TAG_COMPOUND);
        for (INBT nbt : list) {
            AbstractWaypoint waypoint = WaypointManager.registry.read((CompoundNBT)nbt);
            if (waypoint != null) {
                waypoints.put(waypoint.getID(), waypoint);
            }
        }
    }

    @Override
    public void encode(PacketBuffer buffer) {
        ListNBT list = new ListNBT();
        CompoundNBT nbt = new CompoundNBT();
        waypoints.forEach((s, waypoints) -> list.add(WaypointManager.registry.write(waypoints)));
        nbt.put("waypoints", list);
        buffer.writeCompoundTag(nbt);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            context.get().enqueueWork(() -> WaypointRenderer.setWaypoints(waypoints));
        });
        context.get().setPacketHandled(true);
    }
}
