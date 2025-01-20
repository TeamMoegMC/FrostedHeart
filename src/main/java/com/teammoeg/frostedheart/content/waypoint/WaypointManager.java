package com.teammoeg.frostedheart.content.waypoint;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.waypoint.capability.WaypointCapability;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointRemovePacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.waypoints.*;
import com.teammoeg.chorda.util.io.registry.NBTSerializerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.Optional;

public class WaypointManager {
    public static final NBTSerializerRegistry<AbstractWaypoint> registry = new NBTSerializerRegistry<>();
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

        WaypointCapability cap = playerCap.orElse(new WaypointCapability());
        this.waypoints = cap.getWaypoints();
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
        playerCap.ifPresent((cap) -> cap.put(waypoint));
    }

    public void removeWaypoint(String id) {
        removeWaypoint(waypoints.get(id));
    }

    public void removeWaypoint(AbstractWaypoint waypoint) {
        if (waypoint == null) return;
        playerCap.ifPresent((cap) -> {
            waypoint.invalidate();
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new WaypointRemovePacket(waypoint.getId()));
            waypoint.onServerRemove();
            cap.remove(waypoint.getId());
        });
    }

    public void removeWaypointWithoutSendingPacket(String id) {
        Optional<AbstractWaypoint> waypoint = Optional.of(waypoints.get(id));
        waypoint.ifPresent((w) -> playerCap.ifPresent((cap) -> {
            w.invalidate();
            w.onServerRemove();
            cap.remove(w.getId());
        }));
    }

    public Map<String, AbstractWaypoint> getAll() {
        return waypoints;
    }
}

