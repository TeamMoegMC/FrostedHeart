package com.teammoeg.frostedheart.content.waypoint.capability;

import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class WaypointCapability implements NBTSerializable {
    private final Map<String, AbstractWaypoint> waypoints = new HashMap<>();

    public Map<String, AbstractWaypoint> getWaypoints() {
        return waypoints;
    }

    public void put(AbstractWaypoint waypoint) {
        waypoints.put(waypoint.getID(), waypoint);
    }

    public void remove(String id) {
        waypoints.remove(id);
    }

    @Override
    public void save(CompoundNBT nbt, boolean isPacket) {
        ListNBT list = new ListNBT();
        waypoints.forEach((s, waypoint) -> list.add(WaypointManager.registry.write(waypoint)));
        nbt.put("waypoints", list);
    }

    @Override
    public void load(CompoundNBT nbt, boolean isPacket) {
        ListNBT list = nbt.getList("waypoints", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            AbstractWaypoint waypoint = WaypointManager.registry.deserialize(list.getCompound(i));
            if (waypoint != null) {
                waypoints.put(waypoint.getID(), waypoint);
            }
        }
    }
}
