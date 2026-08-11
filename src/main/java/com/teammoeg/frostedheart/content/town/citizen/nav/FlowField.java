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

package com.teammoeg.frostedheart.content.town.citizen.nav;

import java.util.ArrayDeque;
import java.util.Arrays;

import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

/**
 * 流场：以目的地为源反向 BFS 得出的距离场，所有同目标居民共享一次计算。
 * 区域 {@value #SIZE}×{@value #SIZE} 格，基于主线程采集的高度快照在 worker
 * 线程构建（构建过程不触碰世界，线程安全）；相邻格高度差大于 1 视为不可通行，
 * 未加载区块视为不可通行。构建完成后不可变。
 * <p>
 * Flow field: a distance field produced by reverse BFS from the destination,
 * shared by all citizens heading to the same target. Covers a
 * {@value #SIZE}×{@value #SIZE} region, built on a worker thread from a height
 * snapshot taken on the main thread (no world access during build, thread-safe).
 * Neighbor cells with a height difference greater than 1, and unloaded chunks,
 * are treated as impassable. Immutable once built.
 */
public final class FlowField {

	/** 区域边长（格） / Region edge length in cells */
	public static final int SIZE = 64;
	/** 不可达标记 / Unreachable marker */
	public static final short UNREACH = Short.MAX_VALUE;
	/** 高度快照中的未加载标记 / Unloaded marker in height snapshots */
	public static final short UNLOADED = Short.MIN_VALUE;

	/** 8 邻居 X/Z 偏移（先四正后四斜） / 8-neighbor X/Z offsets (cardinal first, then diagonal) */
	private static final int[] DX8 = { 1, -1, 0, 0, 1, 1, -1, -1 };
	private static final int[] DZ8 = { 0, 0, 1, -1, 1, -1, 1, -1 };

	/** 区域原点（方块坐标，含边界） / Region origin (block coords, inclusive) */
	public final int originX, originZ;
	/** 场目标（目标区块中心，方块坐标） / Field target (target chunk center, block coords) */
	public final int targetX, targetZ;
	/** BFS 距离场（格数），{@link #UNREACH} 表示不可达 / BFS distance in cells, {@link #UNREACH} = unreachable */
	private final short[] dist;

	private FlowField(int originX, int originZ, int targetX, int targetZ, short[] dist) {
		this.originX = originX;
		this.originZ = originZ;
		this.targetX = targetX;
		this.targetZ = targetZ;
		this.dist = dist;
	}

	/**
	 * 在 worker 线程上由高度快照构建流场。
	 * <p>
	 * Builds the flow field on a worker thread from a height snapshot.
	 *
	 * @param targetX 目标方块 X / target block X
	 * @param targetZ 目标方块 Z / target block Z
	 * @param heights {@value #SIZE}×{@value #SIZE} 绝对高度快照（{@link #UNLOADED} 表示未加载） /
	 *            absolute height snapshot ({@link #UNLOADED} = unloaded)
	 * @return 流场 / the flow field
	 */
	public static FlowField build(int targetX, int targetZ, short[] heights) {
		int originX = targetX - SIZE / 2;
		int originZ = targetZ - SIZE / 2;
		short[] dist = new short[SIZE * SIZE];
		Arrays.fill(dist, UNREACH);
		ArrayDeque<Integer> queue = new ArrayDeque<>(256);
		int startIdx = (SIZE / 2) * SIZE + SIZE / 2;
		dist[startIdx] = 0;
		queue.add(startIdx);
		while (!queue.isEmpty()) {
			int cur = queue.poll();
			int cx = cur % SIZE;
			int cz = cur / SIZE;
			short ch = heights[cur];
			int nd = dist[cur] + 1;
			for (int d = 0; d < 8; d++) {
				int nx = cx + DX8[d];
				int nz = cz + DZ8[d];
				if (nx < 0 || nx >= SIZE || nz < 0 || nz >= SIZE)
					continue;
				int nIdx = nz * SIZE + nx;
				if (dist[nIdx] != UNREACH)
					continue;
				short nh = heights[nIdx];
				if (nh == UNLOADED)
					continue;
				// 起点格可能落在未加载快照上，此时不做高度约束
				if (ch != UNLOADED && Math.abs(nh - ch) > 1)
					continue;
				dist[nIdx] = (short) nd;
				queue.add(nIdx);
			}
		}
		return new FlowField(originX, originZ, targetX, targetZ, dist);
	}

	/**
	 * 判断方块是否在场区域内。
	 * <p>
	 * Whether the block position is inside the field region.
	 *
	 * @param blockX 方块 X / block X
	 * @param blockZ 方块 Z / block Z
	 * @return 在区域内返回 true / true if inside
	 */
	public boolean contains(int blockX, int blockZ) {
		return blockX >= originX && blockX < originX + SIZE && blockZ >= originZ && blockZ < originZ + SIZE;
	}

	/**
	 * 采样该位置应走的 16 向方向；不可达、在区域外或已在目标格返回 -1（调用方回退直线寻向）。
	 * <p>
	 * Samples the 16-way direction to walk at the position; returns -1 when
	 * unreachable, outside the region, or already at the target cell (caller
	 * falls back to straight-line steering).
	 *
	 * @param blockX 方块 X / block X
	 * @param blockZ 方块 Z / block Z
	 * @return 0–15 方向索引，或 -1 / direction index 0–15, or -1
	 */
	public int sampleDir(int blockX, int blockZ) {
		if (!contains(blockX, blockZ))
			return -1;
		int x = blockX - originX;
		int z = blockZ - originZ;
		short cur = dist[z * SIZE + x];
		if (cur == UNREACH || cur == 0)
			return -1;
		short best = cur;
		int bestDir = -1;
		for (int d = 0; d < 8; d++) {
			int nx = x + DX8[d];
			int nz = z + DZ8[d];
			if (nx < 0 || nx >= SIZE || nz < 0 || nz >= SIZE)
				continue;
			short nd = dist[nz * SIZE + nx];
			if (nd < best) {
				best = nd;
				bestDir = d;
			}
		}
		if (bestDir < 0)
			return -1;
		return CitizenState.dirFromVector(DX8[bestDir], DZ8[bestDir]);
	}
}
