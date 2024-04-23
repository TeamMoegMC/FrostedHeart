package com.teammoeg.frostedheart.content.tips.client.waypoint;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class WaypointManager {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Map<String, Waypoint> WAYPOINTS = new HashMap<>();

    public static void renderWaypoints(MatrixStack ms) {
        if (MC.player == null || isEmpty()) return;
        WAYPOINTS.forEach((id, waypoint) -> waypoint.render(ms));
    }

    public static void create(Vector3f target, String ID) {
        create(target, 0xFFC6FCFF, ID, false);
    }

    public static void create(BlockPos target, String ID) {
        create(new Vector3f(target.getX(), target.getY(), target.getZ()), 0xFFC6FCFF, ID, true);
    }

    /**
     * @param blockPos {@code true} 如果是方块坐标
     */
    public static void create(float x, float y , float z, String ID, boolean blockPos) {
        create(new Vector3f(x, y, z), 0xFFC6FCFF, ID, blockPos);
    }

    public static void create(Vector3f target, int color, String ID, boolean blockPos) {
        if (!exists(ID)) {
            WAYPOINTS.put(ID, new RhombusWaypoint(target, color, ID));
            if (blockPos) blockPosOffset(ID);
        }
    }

    public static void addWaypoint(Waypoint waypoint) {
        if (!exists(waypoint.id)) {
            WAYPOINTS.put(waypoint.id, waypoint);
        }
    }

    public static Waypoint getWaypoint(String id) {
        return WAYPOINTS.get(id);
    }

    public static void setVisible(boolean visible, String ID) {
        if (!exists(ID)) return;
        WAYPOINTS.get(ID).visible = visible;
    }

    public static void setFocus(boolean focus, String ID) {
        if (!exists(ID)) return;
        WAYPOINTS.get(ID).focus = focus;
    }

    public static void setColor(int color, String ID) {
        if (!exists(ID)) return;
        WAYPOINTS.get(ID).color = color;
    }

    public static void setTarget(Vector3f target, String ID) {
        if (!exists(ID)) return;
        WAYPOINTS.get(ID).target = target;
    }

    public static void setTarget(BlockPos target, String ID) {
        if (!exists(ID)) return;
        setTarget(new Vector3f(target.getX(), target.getY(), target.getZ()), ID);
        blockPosOffset(ID);
    }

    public static void setTarget(int x, int y , int z, String ID) {
        if (!exists(ID)) return;
        setTarget(new Vector3f(x, y, z), ID);
    }

    public static Vector3f getTarget(String ID) {
        return exists(ID) ? WAYPOINTS.get(ID).target : new Vector3f(0,0,0);
    }

    public static void blockPosOffset(String ID) {
        if (!exists(ID)) return;
        WAYPOINTS.get(ID).target.add(0.5F, 0.5F, 0.5F);
    }

    public static void blockPosOffset(Waypoint waypoint) {
        waypoint.target.add(0.5F, 0.5F, 0.5F);
    }

    public static boolean exists(String ID) {
        return WAYPOINTS.containsKey(ID);
    }

    public static void remove(String ID) {
        WAYPOINTS.remove(ID);
    }

    public static void removeAll() {
        WAYPOINTS.clear();
    }

    public static boolean isEmpty() {
        return WAYPOINTS.isEmpty();
    }

    //TODO 保存，还是逃不过 Capability
}

