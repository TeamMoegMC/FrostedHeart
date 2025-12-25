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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointRemovePacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;

import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

//TODO 路径点管理UI
public class ClientWaypointManager {
    /**
     * 客户端用于渲染的路径点列表
     */
    @Setter
    private static Map<String, AbstractWaypoint> waypoints = new HashMap<>();
    /**
     * 玩家当前注视或鼠标悬停在的路径点
     */
    @Nullable
    private static AbstractWaypoint hoveredWaypoint;
    /**
     * 是否更新路径点的悬停信息
     */
    private static boolean shouldUpdate;
    /**
     * 悬停时是否显示更详细的路径点信息
     */
    private static boolean shouldShowExtra;

    /**
     * 渲染全部路径点
     */
    public static void renderAll(GuiGraphics graphics) {
        if (!hasWaypoint() || ClientUtils.getWorld() == null) return;

        // 每 tick 更新一次路径点悬停信息
        shouldUpdate = ClientUtils.isGameTimeUpdated() || ClientUtils.getMc().isPaused();

        // Shift 键被按下或玩家潜行
        shouldShowExtra = ClientUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || ClientUtils.getPlayer().isShiftKeyDown();

        // 遍历所有路径点
        Iterator<Map.Entry<String, AbstractWaypoint>> iterator = waypoints.entrySet().iterator();
        while (iterator.hasNext()) {
            AbstractWaypoint waypoint = iterator.next().getValue();
            // 删除无效的路径点
            if (!waypoint.isValid()) {
                waypoint.onClientRemove();
                iterator.remove();
            // 路径点是否启用并且和当前维度相符
            } else if (waypoint.enable && waypoint.dimension.equals(ClientUtils.getDimLocation())){
                waypoint.render(graphics);
                updateHovered(waypoint);
            }
        }
    }

    private static void updateHovered(AbstractWaypoint waypoint) {
        if (!waypoints.containsValue(hoveredWaypoint)) {
            hoveredWaypoint = null;
        }

        // 检查该路径点是否靠近玩家鼠标或屏幕中心区域
        Vec2 screenPos = waypoint.getScreenPos();
        boolean flag = false;
        if (!ClientUtils.getMc().mouseHandler.isMouseGrabbed()) {
            // 鼠标
            flag = MouseHelper.isMouseIn(MouseHelper.getScaledX(), MouseHelper.getScaledY(), (int)(screenPos.x-10), (int)(screenPos.y-10), 20, 20);
        }
        if (!flag) {
            float x = screenPos.x / ClientUtils.screenWidth();
            float y = screenPos.y / ClientUtils.screenHeight();
            //屏幕中心区域
            if (!(x >= 0.45 && x <= 0.55 && y >= 0.45 && y <= 0.55)) {
                if (hoveredWaypoint == waypoint) {
                    hoveredWaypoint = null;
                }
                return;
            }
        }

        if (hoveredWaypoint == null) {
            hoveredWaypoint = waypoint;
            return;
        }

        Vec2 pos;
        if (!ClientUtils.getMc().mouseHandler.isMouseGrabbed()) {
            // 鼠标坐标
            pos = new Vec2(MouseHelper.getScaledX(), MouseHelper.getScaledY());
        } else {
            // 屏幕中心
            pos = new Vec2(ClientUtils.screenWidth()*0.5F, ClientUtils.screenHeight()*0.5F);
        }

        // 对比两个路径点和目标在屏幕中的距离
        float current = screenPos.distanceToSqr(pos);
        float hovered = hoveredWaypoint.getScreenPos().distanceToSqr(pos);
        hoveredWaypoint = current < hovered ? waypoint : hoveredWaypoint;
    }

    public static void putWaypoint(AbstractWaypoint waypoint) {
        putWaypointWithoutSendingPacket(waypoint);
        FHNetwork.INSTANCE.sendToServer( new WaypointSyncPacket(waypoint));
    }

    public static void putWaypointWithoutSendingPacket(AbstractWaypoint waypoint) {
        waypoints.put(waypoint.getId(), waypoint);
    }

    public static void removeWaypoint(String id) {
        if (waypoints.containsKey(id)) {
            FHNetwork.INSTANCE.sendToServer( new WaypointRemovePacket(id));
            removeWaypointWithoutSendingPacket(id);
        }
    }

    public static void removeWaypoint(AbstractWaypoint waypoint) {
        FHNetwork.INSTANCE.sendToServer( new WaypointRemovePacket(waypoint.getId()));
        waypoint.invalidate();
    }

    public static void removeWaypointWithoutSendingPacket(String id) {
        Optional<AbstractWaypoint> waypoint = Optional.of(waypoints.get(id));
        waypoint.ifPresent(AbstractWaypoint::invalidate);
    }

    @Nullable
    public static AbstractWaypoint getWaypoint(String id) {
        return waypoints.get(id);
    }

    public static Map<String, AbstractWaypoint> getAllWaypoints() {
        return Collections.unmodifiableMap(waypoints);
    }

    public static Optional<AbstractWaypoint> getHovered() {
        return Optional.ofNullable(hoveredWaypoint);
    }

    public static boolean containsWaypoint(String id) {
        return waypoints.containsKey(id);
    }

    public static boolean hasWaypoint() {
        return !waypoints.isEmpty();
    }

    public static boolean shouldUpdate() {
        return shouldUpdate;
    }

    public static boolean shouldShowExtra() {
        return shouldShowExtra;
    }

    /**
     * 清空客户端储存的用于渲染的路径点
     */
    public static void clear() {
        waypoints.clear();
    }
}
