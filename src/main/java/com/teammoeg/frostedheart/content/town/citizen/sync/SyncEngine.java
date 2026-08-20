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

package com.teammoeg.frostedheart.content.town.citizen.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenContainer;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenPresence;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSim;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;
import com.teammoeg.frostedheart.content.trade.gui.TradeContainer;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 网络同步引擎：AOI 兴趣管理 + Dead Reckoning 误差驱动发包 + 按距离分频 + 合包。
 * <ul>
 *   <li>AOI：每 {@value #AOI_REFRESH} tick 以玩家为中心 {@value #AOI_RADIUS} 格刷新可见集合，
 *       进出触发 spawn/despawn 全量包；</li>
 *   <li>Dead Reckoning：服务端镜像客户端外推模型，仅当真实位置与外推位置误差超过
 *       {@value #ERROR_DIST} 方块（或方向/状态变化）时才将该居民标记为脏；匀速行走仅按档位心跳重锚；</li>
	 * <li>分频：按最近观察者距离 32/64/+∞ 格分 4/8/20 tick 三档，按 stick 间隔错开避免尖峰；</li>
	 *   <li>合包：每人每 {@value #FLUSH_INTERVAL} tick 发送一个或多个批包，chunk 分组，
	 *       单包最多 {@value #MAX_ENTRIES_PER_PACKET} 条（带宽预算）。</li>
 * </ul>
 * 规范模型（last-sent）为全体玩家共享的近似值：以"任一玩家收到"为更新时机，
 * 远距玩家收到的条目可能略多于理论最小值，换取 O(居民数) 而非 O(居民数×玩家数) 的存储。
 * <p>
	 * Network sync engine: AOI interest management + dead-reckoning error-driven
	 * sending + distance-tiered frequency + packet batching. A flush may emit
	 * multiple packets per player; see the Chinese list
 * above for parameters. The canonical (last-sent) model is a shared
 * approximation across players, updated when any player is sent an entry —
 * trading slightly more entries for far players against O(citizens) instead of
 * O(citizens × players) bookkeeping.
 */
public final class SyncEngine {

	/** AOI 半径（方块） / AOI radius in blocks */
	public static final int AOI_RADIUS = 96;
	/** AOI 刷新周期（tick） / AOI refresh interval in ticks */
	public static final int AOI_REFRESH = 20;
	/** 增量发送周期（tick） / Delta flush interval in ticks */
	public static final int FLUSH_INTERVAL = 4;
	/** 单包条目上限（带宽预算） / Max entries per packet (bandwidth budget) */
	public static final int MAX_ENTRIES_PER_PACKET = 240;
	/**
	 * Dead Reckoning 误差阈值（方块）。0.3 → 0.2：16 向同步后转弯/贴墙滑行期间
	 * 客户端沿旧方向外推，旧阈值允许 0.3 格漂移才修正，中距肉眼可辨回拉；
	 * 收紧后修正更早、单次回拉更小。带宽代价有界——发送频率本就受档位间隔
	 * 钳制（近档至多 4 tick 一次），阈值只决定"到点发不发"。
	 * <p>
	 * Dead-reckoning error threshold in blocks. Tightened 0.3 → 0.2: with
	 * 16-way sync the client extrapolates along the stale dir during turns and
	 * wall slides; 0.3 blocks of drift before correction was visible at mid
	 * range. Bandwidth cost is bounded — send rate is already clamped by the
	 * tier interval (at most once per 4 ticks near), the threshold only
	 * decides whether a due check produces an entry.
	 */
	private static final double ERROR_DIST = 0.2;
	private static final int ERROR2 = (int) (ERROR_DIST * 1024) * (int) (ERROR_DIST * 1024);
	/** 外推时间钳制（tick），防止长时间未更新导致溢出 / Extrapolation clamp in ticks, prevents overflow */
	private static final long DT_CLAMP = 40;
	/** Existing tracked entries rank as if four blocks nearer. */
	private static final int RETAIN_ADVANTAGE_Q = 4 * 16;
	private static final Map<MinecraftServer, VisibilityCoordinator> VISIBILITY_COORDINATORS = new IdentityHashMap<>();

	/** 各玩家当前追踪的居民 id 集合 / Tracked citizen id sets per player */
	private final Map<ServerPlayer, IntOpenHashSet> tracked = new HashMap<>();
	/** Reused selected sets populated by the server-wide visibility coordinator. */
	private final Map<ServerPlayer, IntOpenHashSet> selectedScratch = new HashMap<>();
	/** 待广播的移除 id / Pending removal ids to broadcast */
	private final IntOpenHashSet pendingHidden = new IntOpenHashSet();
	/** Discrete sleep/wake snapshots that bypass distance-tier throttling. */
	private final IntOpenHashSet pendingImmediate = new IntOpenHashSet();
	/** Low-frequency age changes for clients that already track the citizen. */
	private final IntOpenHashSet pendingAppearance = new IntOpenHashSet();
	/** 本 flush 周期内"脏且到期"的居民 id / Dirty-and-due ids in the current flush */
	private final IntOpenHashSet due = new IntOpenHashSet();
	/** Unique IDs tracked by at least one player during the current flush. */
	private final IntOpenHashSet trackedUnion = new IntOpenHashSet();
	/** IDs included in a packet after sendPlayer returned normally during this flush. */
	private final IntOpenHashSet handedOffToAnyPlayer = new IntOpenHashSet();
	/** Reused coarse AOI candidate buffer. */
	private final IntArrayList visibilityCandidates = new IntArrayList();
	private int[] selectionHeapIds = new int[0];
	private long[] selectionHeapRanks = new long[0];
	private int[] playerBlockXs = new int[0];
	private int[] playerBlockZs = new int[0];
	private int selectionHeapSize;
	private boolean aoiRefreshRequested = true;
	private CitizenSimScheduler activeScheduler;
	private ServerLevel activeLevel;

	/**
	 * Resolves all dimension candidates together so the total cap is a real
	 * server-wide limit rather than one independent limit per level.
	 */
	private static final class VisibilityCoordinator {
		private final IdentityHashMap<SyncEngine, Boolean> engines = new IdentityHashMap<>();
		private final List<SyncEngine> candidateEngines = new ArrayList<>();
		private final List<ServerPlayer> candidatePlayers = new ArrayList<>();
		private final IntArrayList candidateIds = new IntArrayList();
		private final LongArrayList candidateRanks = new LongArrayList();
		private int[] globalHeap = new int[0];
		private int globalHeapSize;
		private int lastPerPlayer = -1;
		private int lastPerServer = -1;

		private void refresh() {
			int perPlayer = FHConfig.SERVER.TOWN.maxVisibleCitizensPerPlayer.get();
			int perServer = FHConfig.SERVER.TOWN.maxVisibleCitizensPerServer.get();
			boolean requested = perPlayer != lastPerPlayer || perServer != lastPerServer;
			for (SyncEngine engine : engines.keySet())
				requested |= engine.aoiRefreshRequested;
			if (!requested)
				return;
			lastPerPlayer = perPlayer;
			lastPerServer = perServer;

			candidateEngines.clear();
			candidatePlayers.clear();
			candidateIds.clear();
			candidateRanks.clear();
			for (SyncEngine engine : engines.keySet()) {
				engine.prepareSelectionScratch();
				if (engine.activeLevel == null || engine.activeScheduler == null || perPlayer == 0)
					continue;
				engine.activeScheduler.ensureVisibilityIndex();
				for (ServerPlayer player : engine.activeLevel.players())
					engine.collectPlayerCandidates(player, perPlayer, candidateEngines,
							candidatePlayers, candidateIds, candidateRanks);
			}

			int candidateCount = candidateIds.size();
			if (perServer >= candidateCount) {
				for (int i = 0; i < candidateCount; i++)
					select(i);
			} else if (perServer > 0) {
				ensureGlobalHeap(perServer);
				globalHeapSize = 0;
				for (int i = 0; i < candidateCount; i++)
					offerGlobal(i, perServer);
				for (int i = 0; i < globalHeapSize; i++)
					select(globalHeap[i]);
			}

			for (SyncEngine engine : engines.keySet()) {
				engine.applySelections();
				engine.aoiRefreshRequested = false;
			}
		}

		private void select(int candidate) {
			SyncEngine engine = candidateEngines.get(candidate);
			ServerPlayer player = candidatePlayers.get(candidate);
			engine.selectedScratch.computeIfAbsent(player, ignored -> new IntOpenHashSet())
					.add(candidateIds.getInt(candidate));
		}

		private void ensureGlobalHeap(int capacity) {
			if (globalHeap.length < capacity)
				globalHeap = new int[capacity];
		}

		private void offerGlobal(int candidate, int capacity) {
			if (globalHeapSize < capacity) {
				int child = globalHeapSize++;
				while (child > 0) {
					int parent = (child - 1) >>> 1;
					if (compareCandidates(candidate, globalHeap[parent]) <= 0)
						break;
					globalHeap[child] = globalHeap[parent];
					child = parent;
				}
				globalHeap[child] = candidate;
				return;
			}
			if (compareCandidates(candidate, globalHeap[0]) >= 0)
				return;
			int parent = 0;
			while (true) {
				int left = parent * 2 + 1;
				if (left >= globalHeapSize)
					break;
				int right = left + 1;
				int worse = right < globalHeapSize
						&& compareCandidates(globalHeap[right], globalHeap[left]) > 0 ? right : left;
				if (compareCandidates(globalHeap[worse], candidate) <= 0)
					break;
				globalHeap[parent] = globalHeap[worse];
				parent = worse;
			}
			globalHeap[parent] = candidate;
		}

		/** Positive means left is a worse (later) global candidate. */
		private int compareCandidates(int left, int right) {
			int comparison = Long.compare(candidateRanks.getLong(left), candidateRanks.getLong(right));
			if (comparison != 0)
				return comparison;
			ServerPlayer lp = candidatePlayers.get(left);
			ServerPlayer rp = candidatePlayers.get(right);
			comparison = Long.compare(lp.getUUID().getMostSignificantBits(), rp.getUUID().getMostSignificantBits());
			return comparison != 0 ? comparison
					: Long.compare(lp.getUUID().getLeastSignificantBits(), rp.getUUID().getLeastSignificantBits());
		}
	}

	/** Runs once at the end of the logical server tick. */
	public static void refreshServerVisibility(MinecraftServer server) {
		VisibilityCoordinator coordinator = VISIBILITY_COORDINATORS.get(server);
		if (coordinator != null)
			coordinator.refresh();
	}

	/** Clears runtime-only cross-dimension visibility state on server shutdown. */
	public static void resetServerVisibility() {
		VISIBILITY_COORDINATORS.clear();
	}

	/**
	 * 记录居民被移除，下一 flush 向追踪者广播 despawn。
	 * <p>
	 * Records a citizen removal; the despawn is broadcast to trackers on the next flush.
	 *
	 * @param citizenId 稳定 id / stable id
	 */
	public void notifyRemoved(int citizenId) {
		notifyHidden(citizenId);
	}

	/**
	 * Queues an immediate client-side despawn without removing the simulation row.
	 * Used when a citizen enters an indoor state such as sleep.
	 */
	public void notifyHidden(int citizenId) {
		pendingHidden.add(citizenId);
		if (activeScheduler != null)
			activeScheduler.invalidateVisibilityIndex();
	}

	/** Queues a sleep/wake snapshot that bypasses normal distance throttling. */
	public void notifyImmediate(int citizenId) {
		pendingImmediate.add(citizenId);
		if (activeScheduler != null)
			activeScheduler.invalidateVisibilityIndex();
	}

	/** Queues a low-frequency visual metadata update, currently the Resident age group. */
	public void notifyAppearance(int citizenId) {
		pendingAppearance.add(citizenId);
	}

	/** Whether this player's authoritative presentation set contains the citizen. */
	public boolean isTracked(ServerPlayer player, int citizenId) {
		IntOpenHashSet ids = tracked.get(player);
		return ids != null && ids.contains(citizenId);
	}

	/**
	 * 主同步入口，每 tick 由调度器调用。
	 * <p>
	 * Main sync entry, called by the scheduler every tick.
	 *
	 * @param sched 调度器 / the scheduler
	 * @param level 维度 / the level
	 * @param gameTime 当前游戏时间 / current game time
	 */
	public void flush(CitizenSimScheduler sched, ServerLevel level, long gameTime) {
		this.activeScheduler = sched;
		this.activeLevel = level;
		VISIBILITY_COORDINATORS.computeIfAbsent(level.getServer(), ignored -> new VisibilityCoordinator())
				.engines.put(this, Boolean.TRUE);
		List<ServerPlayer> players = level.players();
		tracked.keySet().removeIf(p -> !players.contains(p));
		selectedScratch.keySet().removeIf(p -> !players.contains(p));
		if (players.isEmpty()) {
			tracked.clear();
			selectedScratch.clear();
			pendingHidden.clear();
			pendingImmediate.clear();
			pendingAppearance.clear();
			return;
		}
		drainHidden(players);
		drainAppearance(players);
		if (gameTime % AOI_REFRESH == 0)
			aoiRefreshRequested = true;
		if (gameTime % FLUSH_INTERVAL == 0)
			flushDeltas(sched, players, gameTime);
	}

	private void prepareSelectionScratch() {
		if (activeLevel == null)
			return;
		List<ServerPlayer> players = activeLevel.players();
		tracked.keySet().removeIf(p -> !players.contains(p));
		selectedScratch.keySet().removeIf(p -> !players.contains(p));
		for (ServerPlayer player : players)
			selectedScratch.computeIfAbsent(player, ignored -> new IntOpenHashSet()).clear();
	}

	private void collectPlayerCandidates(ServerPlayer player, int limit,
			List<SyncEngine> candidateEngines, List<ServerPlayer> candidatePlayers,
			IntArrayList candidateIds, LongArrayList candidateRanks) {
		long aoi2 = (long) AOI_RADIUS * AOI_RADIUS;
		int pbx = player.getBlockX();
		int pbz = player.getBlockZ();
		int interactingId = player.containerMenu instanceof TradeContainer trade
				? trade.data.getCitizenId() : -1;
		IntOpenHashSet old = tracked.get(player);
		ensureSelectionHeap(limit);
		selectionHeapSize = 0;
		visibilityCandidates.clear();
		activeScheduler.grid.queryVisible(pbx, pbz, AOI_RADIUS, visibilityCandidates);
		for (int candidate = 0; candidate < visibilityCandidates.size(); candidate++) {
			int id = visibilityCandidates.getInt(candidate);
			CitizenContainer container = activeScheduler.findById(id);
			if (container == null)
				continue;
			CitizenSim sim = container.sim();
			int i = sim.indexOf(id);
			if (i < 0 || !CitizenPresence.presentationEligible(sim, i))
				continue;
			long dx = (sim.px[i] >> 10) - pbx;
			long dz = (sim.pz[i] >> 10) - pbz;
			long distance2 = dx * dx + dz * dz;
			if (distance2 > aoi2)
				continue;
			int distanceQ = (int) (Math.sqrt(distance2) * 16.0);
			if (old != null && old.contains(id))
				distanceQ = Math.max(0, distanceQ - RETAIN_ADVANTAGE_Q);
			long rank = id == interactingId
					? Long.MIN_VALUE | (id & 0xFFFFFFFFL)
					: ((long) distanceQ << 32) | (id & 0xFFFFFFFFL);
			offerSelection(id, rank, limit);
		}
		for (int i = 0; i < selectionHeapSize; i++) {
			candidateEngines.add(this);
			candidatePlayers.add(player);
			candidateIds.add(selectionHeapIds[i]);
			candidateRanks.add(selectionHeapRanks[i]);
		}
	}

	private void ensureSelectionHeap(int capacity) {
		if (selectionHeapIds.length >= capacity)
			return;
		selectionHeapIds = new int[capacity];
		selectionHeapRanks = new long[capacity];
	}

	private void offerSelection(int id, long rank, int capacity) {
		if (selectionHeapSize < capacity) {
			int child = selectionHeapSize++;
			while (child > 0) {
				int parent = (child - 1) >>> 1;
				if (rank <= selectionHeapRanks[parent])
					break;
				selectionHeapIds[child] = selectionHeapIds[parent];
				selectionHeapRanks[child] = selectionHeapRanks[parent];
				child = parent;
			}
			selectionHeapIds[child] = id;
			selectionHeapRanks[child] = rank;
			return;
		}
		if (rank >= selectionHeapRanks[0])
			return;
		int parent = 0;
		while (true) {
			int left = parent * 2 + 1;
			if (left >= selectionHeapSize)
				break;
			int right = left + 1;
			int worse = right < selectionHeapSize && selectionHeapRanks[right] > selectionHeapRanks[left]
					? right : left;
			if (selectionHeapRanks[worse] <= rank)
				break;
			selectionHeapIds[parent] = selectionHeapIds[worse];
			selectionHeapRanks[parent] = selectionHeapRanks[worse];
			parent = worse;
		}
		selectionHeapIds[parent] = id;
		selectionHeapRanks[parent] = rank;
	}

	private void applySelections() {
		if (activeLevel == null || activeScheduler == null)
			return;
		for (ServerPlayer player : activeLevel.players())
			applySelection(player, selectedScratch.computeIfAbsent(player, ignored -> new IntOpenHashSet()));
	}

	private void applySelection(ServerPlayer player, IntOpenHashSet selected) {
		IntOpenHashSet old = tracked.computeIfAbsent(player, ignored -> new IntOpenHashSet());
		IntArrayList despawns = new IntArrayList();
		for (int id : old)
			if (!selected.contains(id))
				despawns.add(id);
		List<S2CCitizenSpawnPacket.Entry> spawns = new ArrayList<>();
		for (int id : selected) {
			if (old.contains(id))
				continue;
			S2CCitizenSpawnPacket.Entry entry = createSpawnEntry(id);
			if (entry != null)
				spawns.add(entry);
		}
		// Despawn first so applying packets can never transiently exceed either cap.
		if (!despawns.isEmpty())
			FHNetwork.INSTANCE.sendPlayer(player, new S2CCitizenDespawnPacket(despawns));
		if (!spawns.isEmpty())
			FHNetwork.INSTANCE.sendPlayer(player, new S2CCitizenSpawnPacket(spawns));
		old.clear();
		old.addAll(selected);
	}

	private void drainHidden(List<ServerPlayer> players) {
		if (pendingHidden.isEmpty())
			return;
		for (ServerPlayer player : players) {
			IntOpenHashSet set = tracked.get(player);
			if (set == null || set.isEmpty())
				continue;
			IntArrayList despawns = new IntArrayList();
			for (int id : pendingHidden)
				if (set.remove(id))
					despawns.add(id);
			if (!despawns.isEmpty())
				FHNetwork.INSTANCE.sendPlayer(player, new S2CCitizenDespawnPacket(despawns));
		}
		pendingHidden.clear();
	}

	private void drainAppearance(List<ServerPlayer> players) {
		if (pendingAppearance.isEmpty())
			return;
		for (ServerPlayer player : players) {
			IntOpenHashSet set = tracked.get(player);
			if (set == null || set.isEmpty())
				continue;
			List<S2CCitizenSpawnPacket.Entry> updates = new ArrayList<>();
			for (int id : pendingAppearance) {
				if (!set.contains(id))
					continue;
				S2CCitizenSpawnPacket.Entry entry = createSpawnEntry(id);
				if (entry != null)
					updates.add(entry);
			}
			if (!updates.isEmpty())
				FHNetwork.INSTANCE.sendPlayer(player, new S2CCitizenSpawnPacket(updates));
		}
		pendingAppearance.clear();
	}

	private S2CCitizenSpawnPacket.Entry createSpawnEntry(int id) {
		CitizenContainer container = activeScheduler.findById(id);
		if (container == null)
			return null;
		CitizenSim sim = container.sim();
		int index = sim.indexOf(id);
		if (index < 0 || !CitizenPresence.presentationEligible(sim, index))
			return null;
		String name = container.getCitizenName(id);
		byte stateDir = CitizenState.packStateDir(sim.state[index], sim.dir[index]);
		if (sim.halt[index] != 0)
			stateDir |= (byte) CitizenState.HALT_BIT;
		return new S2CCitizenSpawnPacket.Entry(id, sim.px[index], sim.py[index], sim.pz[index],
				stateDir, (byte) sim.presentationAge(index), name == null ? "" : name);
	}

	private void flushDeltas(CitizenSimScheduler sched, List<ServerPlayer> players, long gameTime) {
		due.clear();
		trackedUnion.clear();
		handedOffToAnyPlayer.clear();
		long aoi2 = (long) AOI_RADIUS * AOI_RADIUS;

		// 缓存玩家坐标并构建 tracked union；数组只增长，不在每次 flush 分配。
		int playerCount = players.size();
		ensurePlayerCapacity(playerCount);
		for (int j = 0; j < playerCount; j++) {
			ServerPlayer p = players.get(j);
			playerBlockXs[j] = p.getBlockX();
			playerBlockZs[j] = p.getBlockZ();
			IntOpenHashSet playerTracked = tracked.get(p);
			if (playerTracked != null)
				trackedUnion.addAll(playerTracked);
		}

		// 只检查至少被一名玩家追踪的 id；最近距离仍对全部玩家求值，保持原分频语义。
		for (int id : trackedUnion) {
			CitizenContainer container = sched.findById(id);
			if (container == null)
				continue;
			CitizenSim sim = container.sim();
			int i = sim.indexOf(id);
			if (i < 0 || !CitizenPresence.presentationEligible(sim, i))
				continue;
			long minDist2 = Long.MAX_VALUE;
			for (int j = 0; j < playerCount; j++) {
				long dx = (sim.px[i] >> 10) - playerBlockXs[j];
				long dz = (sim.pz[i] >> 10) - playerBlockZs[j];
				long d2 = dx * dx + dz * dz;
				if (d2 < minDist2)
					minDist2 = d2;
			}
			if (minDist2 > aoi2)
				continue;
			int interval = tierInterval(minDist2);
			if (!isDirty(sim, i, gameTime, interval))
				continue;
			// halt 上升沿（走→停）绕过档位限频：停步信号每晚一个档位，
			// 客户端就多外推一段再被回拉（驻留抽搐的直接成因），必须抢发。
			boolean haltEdge = sim.halt[i] != 0 && sim.shalt[i] == 0;
			if (!haltEdge && gameTime - sim.stick[i] < interval)
				continue;
			due.add(id);
		}
		if (!pendingImmediate.isEmpty()) {
			due.addAll(pendingImmediate);
			pendingImmediate.clear();
		}
		// 广播移除
		if (due.isEmpty())
			return;
		// 按玩家组包：chunk 分组 + 条目上限。240 是单包上限，不是本 flush 截断点。
		// Packet/canonical ordering:
		// due -> per-player groups -> <=240 packet batches -> send -> handedOffToAnyPlayer -> canonical writeback
		for (ServerPlayer p : players) {
			IntOpenHashSet set = tracked.get(p);
			if (set == null || set.isEmpty())
				continue;
			Long2ObjectOpenHashMap<List<S2CCitizenBatchPacket.Entry>> byChunk = new Long2ObjectOpenHashMap<>();
			for (int id : set) {
				if (!due.contains(id))
					continue;
				CitizenContainer c = sched.findById(id);
				if (c == null)
					continue;
				CitizenSim sim = c.sim();
				int i = sim.indexOf(id);
				if (i < 0)
					continue;
				if (!CitizenPresence.presentationEligible(sim, i))
					continue;
				int cx = sim.px[i] >> 14; // 定点坐标 >> 14 = chunk 坐标
				int cz = sim.pz[i] >> 14;
				long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
				int lx = (sim.px[i] - (cx << 14)) / S2CCitizenBatchPacket.LOCAL_QUANT;
				int lz = (sim.pz[i] - (cz << 14)) / S2CCitizenBatchPacket.LOCAL_QUANT;
				int ly = sim.py[i] >> 6;
				// state+dir 打包为一个同步字节（bit0-2 状态，bit3-6 方向，bit7 停步标记）：
				// 16 向方向取代连续 yaw 后，状态与方向恒可合发，每条目恒定 6-8 字节。
				// 停步位告知客户端"移动类状态但本 tick 未位移"，立即停止外推。
				// state+dir packed into one sync byte (bits 0-2 state, bits 3-6 dir,
				// bit 7 halt): constant 6-8 bytes per entry; the halt bit tells the
				// client "MOVING-class state but no displacement this tick" so it
				// stops extrapolating at once.
				byte sd = CitizenState.packStateDir(sim.state[i], sim.dir[i]);
				if (sim.halt[i] != 0)
					sd |= (byte) CitizenState.HALT_BIT;
				byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(new S2CCitizenBatchPacket.Entry(id, lx, ly,
						lz, sd));
			}
			if (byChunk.isEmpty())
				continue;
			List<S2CCitizenBatchPacket.Group> groups = new ArrayList<>(byChunk.size());
			for (Iterator<Long2ObjectOpenHashMap.Entry<List<S2CCitizenBatchPacket.Entry>>> it = byChunk
					.long2ObjectEntrySet().fastIterator(); it.hasNext();) {
				Long2ObjectOpenHashMap.Entry<List<S2CCitizenBatchPacket.Entry>> e = it.next();
				long key = e.getLongKey();
				groups.add(new S2CCitizenBatchPacket.Group((int) (key >> 32), (int) key, e.getValue()));
			}
			CitizenDeltaPacketBatcher.forEachPacket(groups, MAX_ENTRIES_PER_PACKET, packetGroups -> {
				FHNetwork.INSTANCE.sendPlayer(p, new S2CCitizenBatchPacket(packetGroups));
				for (S2CCitizenBatchPacket.Group group : packetGroups)
					for (S2CCitizenBatchPacket.Entry entry : group.entries())
						handedOffToAnyPlayer.add(entry.id());
			});
		}
		// 规范模型延后到本 flush 末尾统一回写：脏检测必须与"上一次发送"的规范值
		// 比较；若在收集阶段就地更新，比较恒成立，dir/state 变化将永不触发补包
		// （回归 A：曾表现为客户端朝向/状态冻结）。
		// 与组包循环同模式：经 id 反查容器与索引（防御移除竞态）。
		// Canonical model is written back at the END of the flush: the dirty check
		// must compare against what was sent LAST flush; updating during collection
		// made the comparison vacuously true, freezing client dir/state
		// (regression A). Same id->container/index re-resolution as the packet-build
		// loop (defensive against removal races).
		for (int id : handedOffToAnyPlayer) {
			CitizenContainer c = sched.findById(id);
			if (c == null)
				continue;
			CitizenSim sim = c.sim();
			int i = sim.indexOf(id);
			if (i < 0)
				continue;
			sim.sx[i] = sim.px[i];
			sim.sy[i] = sim.py[i];
			sim.sz[i] = sim.pz[i];
			sim.sdir[i] = sim.dir[i];
			sim.sstate[i] = sim.state[i];
			sim.shalt[i] = sim.halt[i];
			sim.stick[i] = gameTime;
		}
	}

	private void ensurePlayerCapacity(int playerCount) {
		if (playerBlockXs.length >= playerCount)
			return;
		int capacity = Math.max(playerCount, Math.max(4, playerBlockXs.length * 2));
		playerBlockXs = Arrays.copyOf(playerBlockXs, capacity);
		playerBlockZs = Arrays.copyOf(playerBlockZs, capacity);
	}

	/**
	 * Dead Reckoning 脏检测：真实位置与规范外推模型的误差是否超阈值。
	 * <p>
	 * Dead-reckoning dirty check: whether the true position diverges from the
	 * canonical extrapolated model beyond the threshold.
	 */
	private boolean isDirty(CitizenSim sim, int i, long gameTime, int interval) {
        if (sim.state[i] != sim.sstate[i] || sim.dir[i] != sim.sdir[i] || sim.halt[i] != sim.shalt[i])
			return true;
		long dt = gameTime - sim.stick[i];
		// 移动心跳：匀速状态下 Dead Reckoning 误差恒 0 不会自然发包，
		// 强制按距离档位刷新基准点——与发包节奏（tierInterval）同频。
		// 若心跳固定 20 tick 而近档发包 4 tick：方向翻转 burst 后接 1s 长间隙，
		// 客户端自适应窗口（prevGap）骤短、段长骤长 → 沿行进方向猛冲瞬移。
		// 同频后包间隔均匀，窗口恒等于段长，该问题在机制上消失。
		// 停步（shalt=1）后不再发移动心跳：客户端已知其站立，无漂移可锚，
		// 到岗站立的 WORK 居民自此零流量（原来每档位一个心跳直到状态翻转）。
		// Movement heartbeat follows the distance tier (same cadence as the
		// flush interval): a fixed 20-tick heartbeat next to a 4-tick near-tier
		// flush made a dir-flip burst followed by a 1 s gap; the client's
		// adaptive window (prevGap) then matched neither — blast-forward
		// teleports. Uniform cadence keeps window ≡ segment time. Once halted
		// (shalt=1) the heartbeat stops: the client knows the citizen stands,
		// there is no drift to re-anchor, and at-post WORK citizens cost zero
		// traffic.
		int ss = sim.sstate[i] & 0xFF;
		boolean moving = ss < CitizenState.STATE_COUNT && CitizenState.MOVING[ss] && sim.shalt[i] == 0;
		if (moving && dt >= interval)
			return true;
		if (dt > DT_CLAMP)
			dt = DT_CLAMP;

		int predX = sim.sx[i];
		int predZ = sim.sz[i];
        // 规范外推方向：16 向查表，与移动积分的真实位移方向严格同源
        // （旧实现用滞后的连续 syaw 外推，转向期间制造误差触发冤枉补包）
        int sdir = sim.sdir[i] & 15;

        if (moving) {
            int speed = CitizenState.SPEED[ss];
            predX += (int)(((long)CitizenState.DIR_X_16[sdir] * speed * dt) >> 10);
            predZ += (int)(((long)CitizenState.DIR_Z_16[sdir] * speed * dt) >> 10);
        }

		long ex = sim.px[i] - predX;
		long ey = sim.py[i] - sim.sy[i];
		long ez = sim.pz[i] - predZ;
		return ex * ex + ey * ey + ez * ez > ERROR2;
	}

	private static int tierInterval(long minDist2) {
		if (minDist2 < 32L * 32L)
			return 4;
		if (minDist2 < 64L * 64L)
			return 8;
		return 20;
	}
}
