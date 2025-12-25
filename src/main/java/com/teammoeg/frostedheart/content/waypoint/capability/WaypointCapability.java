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

package com.teammoeg.frostedheart.content.waypoint.capability;

import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

@Getter
public class WaypointCapability implements NBTSerializable {
    private final Map<String, AbstractWaypoint> waypoints = new HashMap<>();

    public void put(AbstractWaypoint waypoint) {
        waypoints.put(waypoint.getId(), waypoint);
    }

    public void remove(String id) {
        waypoints.remove(id);
    }

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        ListTag list = new ListTag();
        waypoints.forEach((s, waypoint) -> list.add(WaypointManager.registry.write(waypoint)));
        nbt.put("waypoints", list);
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        ListTag list = nbt.getList("waypoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            AbstractWaypoint waypoint = WaypointManager.registry.deserialize(list.getCompound(i));
            if (waypoint != null) {
                waypoints.put(waypoint.getId(), waypoint);
            }
        }
    }
}
