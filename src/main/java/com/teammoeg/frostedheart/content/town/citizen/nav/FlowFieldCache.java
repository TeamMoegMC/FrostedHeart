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

import java.util.Iterator;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 流场缓存：按"目标区块"复用流场（同区块目的地的居民共享一场）。
 * 需求由移动系统在主线程登记，每 tick 限 {@value #MAX_NEW_PER_TICK} 个新任务：
 * 主线程采集高度快照（含未加载检测）→ worker 线程 BFS 构建 → 主线程安装。
 * 场到期（{@value #REFRESH_INTERVAL} tick）且仍有需求时重建，以吸收地形变化
 * 与区块加载造成的旧快照失真；超过 {@value #EVICT_AFTER} tick 无需求则回收。
 * v1 不挂钩方块变更事件做精确失效，以周期重建代替（成本可控、实现简单）。
 * <p>
 * Flow field cache: reuses one field per target chunk (citizens heading to the
 * same chunk share a single BFS). Demand is registered by the movement system
 * on the main thread; at most {@value #MAX_NEW_PER_TICK} new builds start per
 * tick: the main thread captures a height snapshot (with unloaded detection),
 * a worker thread runs the BFS, and the result is installed back on the main
 * thread. Fields are rebuilt after {@value #REFRESH_INTERVAL} ticks if still
 * demanded, absorbing terrain edits and stale snapshots from previously
 * unloaded chunks; slots with no demand for {@value #EVICT_AFTER} ticks are
 * evicted. v1 does not hook block-change events for precise invalidation —
 * periodic rebuild is used instead (bounded cost, simpler).
 */
public final class FlowFieldCache {

	/** 每 tick 最多新启动的建场任务数 / Max new field builds started per tick */
	private static final int MAX_NEW_PER_TICK = 2;
	/** 每 tick 最多安装的完成回调数 / Max completion callbacks installed per tick */
	private static final int INSTALL_PER_TICK = 4;
	/** 场复用有效期（tick），到期且仍有需求则重建 / Field reuse lifetime; rebuilt if still demanded */
	private static final long REFRESH_INTERVAL = 2400;
	/** 无需求回收期（tick） / Eviction delay without demand, in ticks */
	private static final long EVICT_AFTER = 6000;

	/** 单个目标区块的缓存槽 / Cache slot for one target chunk */
	private static final class Slot {
		/** 已就绪的流场，构建中为 null / ready field, null while building */
		FlowField field;
		/** 最近一次需求时刻 / last demand game time */
		long lastDemand;
		/** 下次允许重建的时刻 / next allowed rebuild time */
		long refreshAt;
	}

	private final Long2ObjectOpenHashMap<Slot> slots = new Long2ObjectOpenHashMap<>();
	private final LongArrayFIFOQueue demandQueue = new LongArrayFIFOQueue();
	private final LongOpenHashSet queued = new LongOpenHashSet();

	/**
	 * 移动系统调用：取目标方块对应的已就绪流场；未就绪则登记需求并返回 null
	 * （调用方本 tick 回退直线寻向，后续 tick 自动受益）。
	 * <p>
	 * Called by the movement system: returns the ready flow field for the target
	 * block; registers demand and returns null when not ready (caller falls back
	 * to straight-line steering this tick and benefits automatically later).
	 *
	 * @param targetBX 目标方块 X / target block X
	 * @param targetBZ 目标方块 Z / target block Z
	 * @param gameTime 当前游戏时间 / current game time
	 * @return 已就绪流场或 null / ready field or null
	 */
	public FlowField request(int targetBX, int targetBZ, long gameTime) {
		long key = chunkKey(targetBX, targetBZ);
		Slot s = slots.get(key);
		if (s != null) {
			s.lastDemand = gameTime;
			return s.field;
		}
		s = new Slot();
		s.lastDemand = gameTime;
		s.refreshAt = gameTime + REFRESH_INTERVAL;
		slots.put(key, s);
		if (queued.add(key))
			demandQueue.enqueue(key);
		return null;
	}

	/**
	 * 管理器每 tick 调用：消化完成回调、启动限量的新任务、周期刷新与回收。
	 * <p>
	 * Called by the manager every tick: drains completion callbacks, starts a
	 * bounded number of new builds, and performs periodic refresh/eviction.
	 *
	 * @param level 维度 / the level
	 * @param gameTime 当前游戏时间 / current game time
	 */
	public void tick(ServerLevel level, long gameTime) {
		NavJobExecutor.drainMain(INSTALL_PER_TICK);
		for (int k = 0; k < MAX_NEW_PER_TICK && !demandQueue.isEmpty(); k++) {
			long key = demandQueue.dequeueLong();
			queued.remove(key);
			Slot s = slots.get(key);
			if (s == null)
				continue;
			startBuild(level, key, s, gameTime);
		}
		if (gameTime % 200 == 0) {
			Iterator<Long2ObjectOpenHashMap.Entry<Slot>> it = slots.long2ObjectEntrySet().fastIterator();
			while (it.hasNext()) {
				Long2ObjectOpenHashMap.Entry<Slot> e = it.next();
				Slot s = e.getValue();
				if (gameTime - s.lastDemand > EVICT_AFTER) {
					it.remove();
					continue;
				}
				// 仅在场已就绪时安排重建，避免同一槽位任务堆叠
				if (gameTime >= s.refreshAt && s.field != null) {
					s.refreshAt = gameTime + REFRESH_INTERVAL;
					if (queued.add(e.getLongKey()))
						demandQueue.enqueue(e.getLongKey());
				}
			}
		}
	}

	private void startBuild(ServerLevel level, long key, Slot s, long gameTime) {
		int cx = (int) (key >> 32);
		int cz = (int) key;
		int targetX = (cx << 4) + 8;
		int targetZ = (cz << 4) + 8;
		short[] heights = snapshot(level, targetX, targetZ);
		NavJobExecutor.submit(() -> {
			FlowField f = FlowField.build(targetX, targetZ, heights);
			NavJobExecutor.toMain(() -> {
				// 槽位可能已被回收；slot 身份比较防止旧结果覆盖新任务
				if (slots.get(key) == s)
					s.field = f;
			});
		});
	}

	private static short[] snapshot(ServerLevel level, int centerX, int centerZ) {
		int size = FlowField.SIZE;
		short[] h = new short[size * size];
		int ox = centerX - size / 2;
		int oz = centerZ - size / 2;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++) {
				int bx = ox + x;
				int bz = oz + z;
				if (!level.hasChunk(bx >> 4, bz >> 4)) {
					h[z * size + x] = FlowField.UNLOADED;
					continue;
				}
				h[z * size + x] = (short) level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
			}
		}
		return h;
	}

	private static long chunkKey(int blockX, int blockZ) {
		return ((long) (blockX >> 4) << 32) | ((blockZ >> 4) & 0xFFFFFFFFL);
	}
}
