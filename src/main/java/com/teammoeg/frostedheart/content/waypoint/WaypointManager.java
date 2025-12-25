/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.waypoint;

import com.teammoeg.chorda.io.registry.NBTSerializerRegistry;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.waypoint.capability.WaypointCapability;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointRemovePacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.waypoints.*;

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
            FHNetwork.INSTANCE.sendPlayer(player, new WaypointSyncPacket(waypoint));
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
            FHNetwork.INSTANCE.sendPlayer(player, new WaypointRemovePacket(waypoint.getId()));
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

