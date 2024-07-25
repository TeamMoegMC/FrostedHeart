package com.teammoeg.frostedheart.content.waypoint;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import com.teammoeg.frostedheart.util.client.AnimationUtil;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WaypointRenderer {
    /**
     * 客户端用于渲染的路径点列表
     */
    private static Map<String, AbstractWaypoint> waypoints = new HashMap<>();
    private static boolean shouldUpdate;

    /**
     * 渲染全部路径点
     */
    public static void render(MatrixStack matrixStack) {
        if (isEmpty() || ClientUtils.getWorld() == null) return;

        //每 50 毫秒更新一次路径点悬停信息
        shouldUpdate = AnimationUtil.progress(50, "shouldUpdateInfo", true) == 1;

        Iterator<Map.Entry<String, AbstractWaypoint>> iterator = waypoints.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AbstractWaypoint> entry = iterator.next();
            AbstractWaypoint waypoint = entry.getValue();
            //删除无效的路径点
            if (!waypoint.valid) {
                waypoint.onClientRemove();
                iterator.remove();
                //渲染启用和维度与当前维度相同的路径点
            } else if (waypoint.enable && waypoint.dimension.equals(ClientUtils.getDimLocation())){
                waypoint.render(matrixStack);
            }
        }
    }

    public static void putWaypoint(AbstractWaypoint waypoint) {
        waypoints.put(waypoint.getID(), waypoint);
    }

    public static void removeWaypoint(String id) {
        waypoints.remove(id);
    }

    public static void setWaypoints(Map<String, AbstractWaypoint> waypoints) {
        WaypointRenderer.waypoints = waypoints;
    }

    public static boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public static boolean shouldUpdate() {
        return shouldUpdate;
    }

    /**
     * 清空客户端的路径点
     */
    public static void clear() {
        waypoints.clear();
    }
}
