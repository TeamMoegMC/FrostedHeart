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

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * 空间哈希网格，cell 边长 2 方块。
 * 用于邻居查询（分离力）与活跃度判定。每 5 tick 重建一次活跃区，
 * 单位 5 tick 内位移不足一格，不会导致查询失效。
 * <p>
 * Spatial hash grid with 2-block cells. Used for neighbor queries (separation)
 * and activity checks. Rebuilt every 5 ticks; citizens move less than one cell
 * in that window, so staleness is harmless.
 */
public final class SpatialGrid {

	private static final int VISIBILITY_CELL_SHIFT = 4;
	private static final int VISIBILITY_QUERY_HALO = 1;
	private final Long2ObjectOpenHashMap<IntArrayList> cells = new Long2ObjectOpenHashMap<>();
	private final List<IntArrayList> listPool = new java.util.ArrayList<>();
	private final Long2ObjectOpenHashMap<IntArrayList> visibilityCells = new Long2ObjectOpenHashMap<>();
	private final List<IntArrayList> visibilityListPool = new java.util.ArrayList<>();

	/**
	 * 计算方块坐标所属 cell 的打包键。
	 * <p>
	 * Computes the packed cell key for a block coordinate.
	 *
	 * @param blockX 方块 X / block X
	 * @param blockZ 方块 Z / block Z
	 * @return cell 键 / cell key
	 */
	public static long cellKey(int blockX, int blockZ) {
		return ((long) (blockX >> 1) << 32) | ((blockZ >> 1) & 0xFFFFFFFFL);
	}

	/**
	 * 用当前活跃居民位置重建网格。
	 * 条目存稳定 id 而非运行期索引——网格跨多个容器共享（per-level），
	 * 各容器的索引空间独立会冲突；查询方（分离力）按 id 反查容器与索引。
	 * <p>
	 * Rebuilds the grid from currently active citizen positions. Entries store
	 * stable ids instead of runtime indices — the grid is shared across all
	 * containers of a level and their index spaces are independent, so indices
	 * would collide; queryers (separation) resolve container and index by id.
	 *
	 * @param containers 全部容器 / all containers
	 * @param active 活跃度判定（按容器+索引） / activity predicate (by container + index)
	 */
	public void rebuild(List<CitizenContainer> containers, ActivityQuery active) {
		recycle(cells, listPool);
		for (CitizenContainer c : containers) {
			CitizenSim sim = c.sim();
			int n = sim.size();
			for (int i = 0; i < n; i++) {
				if (!active.isActive(c, i))
					continue;
				long key = cellKey(sim.px[i] >> 10, sim.pz[i] >> 10);
				IntArrayList list = cells.get(key);
				if (list == null) {
					list = listPool.isEmpty() ? new IntArrayList(4) : listPool.remove(listPool.size() - 1);
					cells.put(key, list);
				}
				list.add(sim.id[i]);
			}
		}
	}

	/**
	 * 查询指定方块坐标周围 3×3 cell 内的居民稳定 id，写入 out。
	 * <p>
	 * Collects citizen stable ids in the 3×3 cells around a block coordinate into out.
	 *
	 * @param blockX 中心方块 X / center block X
	 * @param blockZ 中心方块 Z / center block Z
	 * @param out 输出列表（调用方负责清空） / output list (caller clears it)
	 */
	public void queryNeighbors(int blockX, int blockZ, IntArrayList out) {
		int cx = blockX >> 1;
		int cz = blockZ >> 1;
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++) {
				IntArrayList list = cells.get(((long) (cx + dx) << 32) | ((cz + dz) & 0xFFFFFFFFL));
				if (list != null)
					out.addAll(list);
			}
	}

	/**
	 * Rebuilds the coarse AOI index from all citizens with a valid runtime state.
	 * Sleeping residents remain indexed; the synchronization layer performs the
	 * authoritative valid-bed and exact-distance checks.
	 */
	public void rebuildVisibility(List<CitizenContainer> containers) {
		recycle(visibilityCells, visibilityListPool);
		for (CitizenContainer container : containers) {
			CitizenSim sim = container.sim();
			for (int i = 0; i < sim.size(); i++) {
				if (!CitizenPresence.behaviorScheduled(sim.state[i] & 0xFF))
					continue;
				long key = visibilityCellKey(sim.px[i] >> 10, sim.pz[i] >> 10);
				IntArrayList list = visibilityCells.get(key);
				if (list == null) {
					list = visibilityListPool.isEmpty()
							? new IntArrayList(8)
							: visibilityListPool.remove(visibilityListPool.size() - 1);
					visibilityCells.put(key, list);
				}
				list.add(sim.id[i]);
			}
		}
	}

	/**
	 * Collects coarse candidates around an AOI. One extra cell is queried on
	 * every side so bounded movement between index rebuilds cannot create a
	 * false negative; callers must still apply the exact circular distance.
	 */
	public void queryVisible(int blockX, int blockZ, int radius, IntArrayList out) {
		int minCellX = ((blockX - radius) >> VISIBILITY_CELL_SHIFT) - VISIBILITY_QUERY_HALO;
		int maxCellX = ((blockX + radius) >> VISIBILITY_CELL_SHIFT) + VISIBILITY_QUERY_HALO;
		int minCellZ = ((blockZ - radius) >> VISIBILITY_CELL_SHIFT) - VISIBILITY_QUERY_HALO;
		int maxCellZ = ((blockZ + radius) >> VISIBILITY_CELL_SHIFT) + VISIBILITY_QUERY_HALO;
		for (int cellX = minCellX; cellX <= maxCellX; cellX++)
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
				IntArrayList list = visibilityCells.get(packCell(cellX, cellZ));
				if (list != null)
					out.addAll(list);
			}
	}

	private static long visibilityCellKey(int blockX, int blockZ) {
		return packCell(blockX >> VISIBILITY_CELL_SHIFT, blockZ >> VISIBILITY_CELL_SHIFT);
	}

	private static long packCell(int cellX, int cellZ) {
		return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
	}

	private static void recycle(Long2ObjectOpenHashMap<IntArrayList> source, List<IntArrayList> pool) {
		for (IntArrayList list : source.values()) {
			list.clear();
			pool.add(list);
		}
		source.clear();
	}

	/**
	 * 活跃度查询回调，避免在网格中引入对调度器的依赖。
	 * <p>
	 * Activity query callback, keeping the grid decoupled from the scheduler.
	 */
	@FunctionalInterface
	public interface ActivityQuery {
		/**
		 * 判断指定容器内指定索引的居民是否活跃。
		 * <p>
		 * Whether the citizen at the container index is active.
		 *
		 * @param container 容器 / the container
		 * @param index 运行期索引 / runtime index
		 * @return 活跃返回 true / true if active
		 */
		boolean isActive(CitizenContainer container, int index);
	}
}
