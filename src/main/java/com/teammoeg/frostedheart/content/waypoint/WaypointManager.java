package com.teammoeg.frostedheart.content.waypoint;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.waypoint.capability.WaypointCapability;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.EntityWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.SunStationWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.waypoint;
import com.teammoeg.frostedheart.util.io.registry.NBTSerializerRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;

public class WaypointManager {
    public static NBTSerializerRegistry<AbstractWaypoint> registry = new NBTSerializerRegistry<>();
    static {
        registry.register(waypoint.class, "default", waypoint::new, AbstractWaypoint::serializeNBT, waypoint::new);
        registry.register(EntityWaypoint.class, "entity", EntityWaypoint::new, AbstractWaypoint::serializeNBT, EntityWaypoint::new);
        registry.register(SunStationWaypoint.class, "sun_station", SunStationWaypoint::new, AbstractWaypoint::serializeNBT, SunStationWaypoint::new);
    }

    private final ServerPlayerEntity player;
    private final LazyOptional<WaypointCapability> playerCap;
    private final Map<String, AbstractWaypoint> waypoints;

    private WaypointManager(ServerPlayerEntity player) {
        this.player = player;
        this.playerCap = FHCapabilities.WAYPOINT.getCapability(player);

        WaypointCapability cap = playerCap.orElse(null);
        this.waypoints = cap == null ? null : cap.getWaypoints();
    }

    public static WaypointManager getManager(ServerPlayerEntity player) {
        return new WaypointManager(player);
    }

    /**
     * 添加或替换一个路径点
     */
    public void putWaypoint(AbstractWaypoint waypoint) {
        playerCap.ifPresent((cap) -> {
            cap.put(waypoint);
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new WaypointSyncPacket(waypoint));
        });
    }

    public void removeWaypoint(AbstractWaypoint waypoint) {
        playerCap.ifPresent((cap) -> {
            waypoint.valid = false;
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new WaypointSyncPacket(waypoint));
            waypoint.onServerRemove();
            cap.remove(waypoint.getID());
        });
    }

    @Nullable
    public AbstractWaypoint getWaypoint(String id) {
        return waypoints == null ? null : waypoints.get(id);
    }

    @Nullable
    public Map<String, AbstractWaypoint> getAll() {
        return waypoints;
    }

    //TODO 还是逃不过 Capability
    //TODO 路径点管理UI
}

