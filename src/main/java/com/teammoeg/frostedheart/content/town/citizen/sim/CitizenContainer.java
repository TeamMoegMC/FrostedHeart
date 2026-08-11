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

package com.teammoeg.frostedheart.content.town.citizen.sim;

import com.teammoeg.frostedheart.content.trade.FHVillagerData;

/**
 * 居民模拟容器的统一接口：模拟层（调度器/行为/移动/同步）只依赖此接口，
 * 不关心居民挂靠在哪里——城镇容器（{@link TownSimData}，挂 team holder）
 * 与未托管容器（{@link UnmanagedCitizenData}，命令生成的野居民）都实现它。
 * "模拟完全挂靠 town"的反面：命令居民仍保留在 per-level 未托管容器里，
 * 两者经统一接口由 {@link CitizenSimScheduler} 调度。
 * <p>
 * Unified interface for citizen-simulation containers: the simulation layers
 * (scheduler / behavior / movement / sync) depend only on this interface,
 * regardless of where citizens are anchored — town containers
 * ({@link TownSimData}, on the team holder) and the unmanaged container
 * ({@link UnmanagedCitizenData}, for command-spawned citizens) both implement
 * it. The per-level scheduler drives them uniformly.
 */
public interface CitizenContainer {

	/**
	 * 本容器持有的模拟核心数据。
	 * <p>
	 * The simulation core data held by this container.
	 *
	 * @return 模拟数据 / the sim data
	 */
	CitizenSim sim();

	/**
	 * 居民显示名（同步层用于 spawn 包的姓名广播）。
	 * <p>
	 * Display name of a citizen (used by the sync layer for spawn packets).
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 显示名；无则返回 null（同步层回退空串）/ display name, or null if none
	 */
	String getCitizenName(int citizenId);

	/**
	 * 居民的交易数据（FH Trade 接口入口）。
	 * <p>
	 * Trade data of a citizen (FH Trade interface entry).
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 交易数据；无则懒创建 / the trade data, lazily created
	 */
	FHVillagerData getTradeData(int citizenId);

	/**
	 * 条目被移除时的清理回调（交易数据、显示名等身份状态）。
	 * 由调度器的统一移除路径调用，容器负责各自的缓存清理。
	 * <p>
	 * Cleanup callback when an entry is removed (trade data, display name
	 * and other per-container identity state). Called by the scheduler's
	 * unified removal path; each container cleans its own caches.
	 *
	 * @param citizenId 稳定 id / stable id
	 */
	default void onDataRemoved(int citizenId) {
	}

	/**
	 * 标记数据需要持久化。城镇容器持久化走 holder 自动存盘（无需显式标记），
	 * 未托管容器覆写为 SavedData 的 setDirty()。
	 * <p>
	 * Marks the data dirty for persistence. Town containers persist via the
	 * holder's automatic save (no explicit mark needed); the unmanaged
	 * container overrides this with its SavedData's setDirty().
	 */
	default void markDirty() {
	}
}
