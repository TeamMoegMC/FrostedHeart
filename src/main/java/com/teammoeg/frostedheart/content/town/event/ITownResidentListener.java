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

import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.resident.Resident;

/**
 * 城镇居民生命周期事件监听器（服务端）。
 * <p>
 * 居民模拟（{@code TownSimData}）是唯一订阅者，经 {@code TeamTownData.setResidentListener}
 * 单引用设置。事件由 {@code TeamTownData} 门面 fire（{@code TeamTown.addResident} 成功路径/
 * {@code debugAddResident} → {@code onResidentAdded}；{@code removeResident} →
 * {@code onResidentRemoved}；tickMorning 末尾 → {@code onTownMorningDone}）——
 * 与 {@code ObservableTownMap} 的钩子链无关（那是 DataSyncCache 增量更新的专用通道）。
 * 触发时机保证：added 事件只在居民房屋分配完成后 fire（housePos 已就绪），
 * 因此模拟收到 added 时锚点必可用；同一居民最多收到一次 added（put 先于分配，
 * 中途不通知，无"双触发"）。实现仍应幂等（防御重复事件/幽灵条目）。
 * 全 default no-op；仅在服务端设置，客户端实例无人注册。
 * <p>
 * Town resident lifecycle event listener (server only). The citizen simulation
 * is the sole subscriber, set via a single reference on {@code TeamTownData}.
 * Events fire from the town facade (successful {@code TeamTown.addResident} /
 * {@code debugAddResident} → {@code onResidentAdded}; {@code removeResident} →
 * {@code onResidentRemoved}; end of tickMorning → {@code onTownMorningDone}) —
 * unrelated to the ObservableTownMap hook chain (which is DataSyncCache's
 * incremental-sync channel). Timing guarantee: the added event fires only after
 * the house is allocated (housePos ready), so the simulation always sees a valid
 * anchor; a resident gets at most one added event (no double-fire). Implementations
 * should still be idempotent (defense against duplicate events/ghost entries).
 * All defaults are no-ops; never set on client instances.
 */
public interface ITownResidentListener {

    /**
     * 居民已加入城镇（房屋分配完成后，锚点可用）。幂等。
     * <p>
     * A resident joined the town (after house allocation; anchor ready). Idempotent.
     *
     * @param resident 加入的居民 / the added resident
     */
    default void onResidentAdded(Resident resident) {
    }

    /**
     * 居民已移出城镇（集合移除完成后）。幂等（未见过的居民忽略）。
     * <p>
     * A resident left the town (after collection removal). Idempotent
     * (unknown residents are ignored).
     *
     * @param resident 移出的居民 / the removed resident
     */
    default void onResidentRemoved(Resident resident) {
    }

    /**
     * 每日结算完成（tickMorning 末尾，锚点/工作重分配与难民处理完成后）。
     * <p>
     * Daily settlement done (end of tickMorning, after anchor/work reallocation
     * and refugee handling).
     *
     * @param townData 本镇数据 / this town's data
     */
    default void onTownMorningDone(TeamTownData townData) {
    }
}
