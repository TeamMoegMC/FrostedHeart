/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.event;

import java.util.EventListener;

/**
 * 客户端 GUI 用于监听城镇数据增量变化的监听器。
 * <p>
 * GUI 在打开时通过 {@link com.teammoeg.frostedheart.content.town.TeamTownData#addClientListener}
 * 注册自身，在关闭时通过 {@link com.teammoeg.frostedheart.content.town.TeamTownData#removeClientListener}
 * 移除。增量同步包（TownBuildingUpdatePacket / TownResidentUpdatePacket / TownResourceUpdatePacket）
 * 在 {@code applyXxxUpdate} 中按类别触发对应回调，全量同步包（TeamTownDataS2CPacket）在替换实例后
 * 触发全部类别回调，GUI 在回调中刷新界面即可。
 * <p>
 * 所有方法均提供默认空实现，GUI 只需重写它关心的类别（例如只显示资源的界面重写
 * {@link #onResourcesChanged()} 即可），避免被迫实现不关心的方法。
 * <p>
 * Client-side listener that GUI screens implement to react to incremental town-data
 * updates pushed from the server. Register on open, remove on close; refresh the UI in
 * the relevant callback. All methods have empty default bodies so a GUI only
 * overrides the categories it actually displays.
 */
public interface ITownDataUpdateListener extends EventListener {
    /**
     * 建筑数据发生变化时调用（新增 / 修改 / 移除）。
     * <p>Called when building data changes (add / modify / remove).</p>
     */
    default void onBuildingsChanged() {
    }

    /**
     * 居民数据发生变化时调用（新增 / 修改 / 移除）。
     * <p>Called when resident data changes (add / modify / remove).</p>
     */
    default void onResidentsChanged() {
    }

    /**
     * 资源数据发生变化时调用（数量变化 / 已占用容量变化）。
     * <p>Called when resource data changes (amount / occupied capacity).</p>
     */
    default void onResourcesChanged() {
    }

    /** Called when retained settlement history receives or refreshes an entry. */
    default void onHistoryChanged() {
    }

    /** Called when the player-authored staffing queue or a target changes. */
    default void onStaffingChanged() {
    }

    /** Called when the residential-care queue or a guarantee changes. */
    default void onHousingChanged() {
    }

    /** Called when active, pending, or cooldown policy state changes. */
    default void onPoliciesChanged() {
    }
}
