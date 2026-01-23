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
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointRemovePacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;

import com.teammoeg.frostedheart.content.waypoint.waypoints.Waypoint;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//TODO 路径点管理UI?
public class ClientWaypointManager {
    /**
     * 客户端用于渲染的路径点列表
     */
    @Setter
    private static Map<String, AbstractWaypoint> waypoints = new HashMap<>();
    /**
     * 玩家当前选择路径点
     */
    @Nullable
    private static AbstractWaypoint selectedWaypoint;
    /**
     * 是否更新路径点的悬停信息
     */
    private static boolean shouldUpdate;

    /**
     * 渲染全部路径点
     */
    public static void renderAll(GuiGraphics graphics) {
        if (!hasWaypoint() || ClientUtils.getWorld() == null) return;

        // 每 tick 更新一次路径点悬停信息
        shouldUpdate = ClientUtils.isGameTimeUpdated() || ClientUtils.getMc().isPaused();

        // 遍历所有路径点
        if (!waypoints.containsValue(selectedWaypoint)) {
            selectedWaypoint = null;
        }
        List<AbstractWaypoint> visible = new ArrayList<>();
        Iterator<Map.Entry<String, AbstractWaypoint>> iterator = waypoints.entrySet().iterator();
        while (iterator.hasNext()) {
            AbstractWaypoint waypoint = iterator.next().getValue();
            // 删除无效的路径点
            if (!waypoint.isValid()) {
                waypoint.onClientRemove();
                iterator.remove();
            // 路径点是否启用并且和处于当前维度
            } else if (waypoint.isEnabled() && waypoint.getDimension().equals(ClientUtils.getDimLocation())){
                visible.add(waypoint);
                updateSelected(waypoint);
            }
        }
        // 置顶选中的路径点
        getSelected().ifPresent(selected -> {
            visible.remove(selected);
            visible.add(selected);
        });
        visible.forEach(waypoint -> waypoint.render(graphics));
    }

    private static void updateSelected(AbstractWaypoint waypoint) {
        Vec2 screenPos = waypoint.getScreenPos();
        boolean inGame = ClientUtils.getMc().mouseHandler.isMouseGrabbed();
        int mouseX = (int) (inGame ? ClientUtils.screenCenterX() : MouseHelper.getScaledX());
        int mouseY = (int) (inGame ? ClientUtils.screenCenterY() : MouseHelper.getScaledY());

        waypoint.setHovered(MouseHelper.isMouseIn(mouseX, mouseY, (int)(screenPos.x-20), (int)(screenPos.y-20), 40, 40));
        if (!waypoint.isHovered()) {
            if (selectedWaypoint == waypoint) {
                selectedWaypoint = null;
            }
            return;
        }

        if (selectedWaypoint == null) {
            selectedWaypoint = waypoint;
            return;
        }

        // 对比两个路径点和目标在屏幕中的距离
        Vec2 pos = new Vec2(mouseX, mouseY);
        float current = screenPos.distanceToSqr(pos);
        float selected = selectedWaypoint.getScreenPos().distanceToSqr(pos);
        selectedWaypoint = current < selected ? waypoint : selectedWaypoint;
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

    public static Optional<AbstractWaypoint> getSelected() {
        return Optional.ofNullable(selectedWaypoint);
    }

    public static boolean fromPickedBlock() {
        HitResult block = ClientUtils.getPlayer().pick(128, ClientUtils.partialTicks(), false);
        if (block.getType() == HitResult.Type.BLOCK) {
            Waypoint waypoint = new Waypoint(((BlockHitResult)block).getBlockPos(), "picked_block", Colors.CYAN);
            waypoint.setFocused(true);
            waypoint.setDisplayName(ClientUtils.getWorld().getBlockState(((BlockHitResult)block).getBlockPos()).getBlock().getName());
            putWaypoint(waypoint);
            return true;
        }
        return false;
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

    /**
     * 悬停时是否显示更详细的路径点信息
     */
    public static boolean shouldShowExtra() {
        return Screen.hasShiftDown();
    }

    /**
     * 清空客户端储存的用于渲染的路径点
     */
    public static void clear() {
        waypoints.clear();
    }
}
