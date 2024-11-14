package com.teammoeg.frostedheart.content.waypoint;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.waypoint.capability.WaypointCapability;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointRemovePacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.waypoints.*;
import com.teammoeg.frostedheart.util.io.registry.NBTSerializerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WaypointManager {
    public static NBTSerializerRegistry<AbstractWaypoint> registry = new NBTSerializerRegistry<>();
    static {
        registry.register(Waypoint.class, "default", Waypoint::new, AbstractWaypoint::serializeNBT, Waypoint::new);
        registry.register(EntityWaypoint.class, "entity", EntityWaypoint::new, AbstractWaypoint::serializeNBT, EntityWaypoint::new);
        registry.register(SunStationWaypoint.class, "sun_station", SunStationWaypoint::new, AbstractWaypoint::serializeNBT, SunStationWaypoint::new);
        registry.register(ColumbiatWaypoint.class, "columbiat", ColumbiatWaypoint::new, AbstractWaypoint::serializeNBT, ColumbiatWaypoint::new);
    }

    private final ServerPlayer player;
    private final LazyOptional<WaypointCapability> playerCap;
    private final Map<String, AbstractWaypoint> waypoints;

    private WaypointManager(ServerPlayer player) {
        this.player = player;
        this.playerCap = FHCapabilities.WAYPOINT.getCapability(player);

        WaypointCapability cap = playerCap.orElse(null);
        this.waypoints = cap == null ? new HashMap<>() : cap.getWaypoints();
    }

    public static WaypointManager getManager(ServerPlayer player) {
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

    public void putWaypointWithoutSendingPacket(AbstractWaypoint waypoint) {
        playerCap.ifPresent((cap) -> {
            cap.put(waypoint);
        });
    }

    public void removeWaypoint(String id) {
        removeWaypoint(waypoints.get(id));
    }

    public void removeWaypoint(AbstractWaypoint waypoint) {
        if (waypoint == null) return;
        playerCap.ifPresent((cap) -> {
            waypoint.valid = false;
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new WaypointRemovePacket(waypoint.getID()));
            waypoint.onServerRemove();
            cap.remove(waypoint.getID());
        });
    }

    public void removeWaypointWithoutSendingPacket(String id) {
        Optional<AbstractWaypoint> waypoint = Optional.of(waypoints.get(id));
        waypoint.ifPresent((w) -> playerCap.ifPresent((cap) -> {
            w.valid = false;
            w.onServerRemove();
            cap.remove(w.getID());
        }));
    }

    @Nullable
    public Map<String, AbstractWaypoint> getAll() {
        return waypoints;
    }
}

