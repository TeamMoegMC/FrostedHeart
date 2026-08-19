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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongPredicate;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.ITown;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.ai_town.AITownData;
import com.teammoeg.frostedheart.content.town.ai_town.AITownManager;
import com.teammoeg.frostedheart.content.town.citizen.nav.FlowFieldCache;
import com.teammoeg.frostedheart.content.town.citizen.sync.SyncEngine;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 居民模拟调度器：per-level 运行期注册表（不持久化），驱动本维度全部容器——
 * 每个城镇一份模拟（{@link TownSimData}，玩家镇按队伍 id 存全局文件、AI 镇内嵌
 * AITownData）+ 一个未托管容器（{@link UnmanagedCitizenData}，命令居民）。
 * <p>
 * tick 管线（与旧 per-dimension Manager 逐 tick 顺序一致）：
 * 活跃度刷新（20t）→ 容器注册表门控聚合（20t，只做维度激活/首次接管/维度变更
 * 检测与 registry diff despawn，**无对账**——居民增删由城镇事件直接驱动）→
 * 空间网格重建（5t）→ 行为分帧决策（1Hz/单位）→ 移动全量积分 → 网络增量同步。
 * <p>
 * Per-level runtime scheduler (not persisted) driving every container in the
 * level — one simulation per town ({@link TownSimData}; player towns keyed by
 * team id in the global save, AI towns embedded in AITownData) plus one
 * unmanaged container ({@link UnmanagedCitizenData}, command citizens). The
 * tick pipeline matches the old per-dimension manager order.
 * The 20-tick registry refresh is gated aggregation only (dimension activation
 * / first takeover / level-change detection / registry-diff despawn) — no
 * reconciliation, because resident add/remove drives the simulation directly
 * via town events.
 */
public final class CitizenSimScheduler {

	/** 活跃判定半径（方块） / Activity radius in blocks */
	public static final int ACTIVE_RADIUS = 128;

	/** per-level 注册表 / Per-level registry */
	private static final Map<ResourceKey<Level>, CitizenSimScheduler> BY_LEVEL = new ConcurrentHashMap<>();

	/**
	 * 获取指定维度的调度器。
	 * <p>
	 * Gets the scheduler for the level.
	 *
	 * @param level 服务端维度 / server level
	 * @return 调度器 / the scheduler
	 */
	public static CitizenSimScheduler get(ServerLevel level) {
		return BY_LEVEL.computeIfAbsent(level.dimension(), k -> new CitizenSimScheduler());
	}

	/**
	 * 清空注册表（服务器停止时调用，配合 {@code NavJobExecutor.shutdown()}）。
	 * <p>
	 * Clears the registry (on server stop, together with
	 * {@code NavJobExecutor.shutdown()}).
	 */
	public static void resetAll() {
		BY_LEVEL.clear();
		SyncEngine.resetServerVisibility();
	}

	/**
	 * 标记所有已接管容器需要保存。正常停服在清空运行期注册表前调用，确保即使
	 * 某个未来写路径遗漏了细粒度标记，当前权威快照仍会进入停服保存。
	 * <p>
	 * Marks every adopted container dirty. Called before clearing the runtime
	 * registry during a normal server stop so the authoritative snapshot is
	 * included in the stop-save even if a future mutation path misses its
	 * fine-grained mark.
	 */
	public static void markAllDirty() {
		for (CitizenSimScheduler sched : BY_LEVEL.values())
			for (CitizenContainer container : sched.containers)
				container.markDirty();
	}

	public final SpatialGrid grid = new SpatialGrid();
	public final SyncEngine sync = new SyncEngine();
	/** 流场缓存（瞬态，不随存档持久化） / Flow field cache (transient, not persisted) */
	public final FlowFieldCache fields = new FlowFieldCache();
	/** 活跃 cell 集合（SpatialGrid cell 键） / Set of active cells (SpatialGrid cell keys) */
	private final LongOpenHashSet activeCells = new LongOpenHashSet();
	private final BehaviorSystem behavior = new BehaviorSystem();
	private final MovementSystem movement = new MovementSystem();

	/** 本维度全部容器（每次 refreshRegistry 重建） / All containers in this level (rebuilt each refresh) */
	private List<CitizenContainer> containers = new ArrayList<>();
	/** 稳定 id → 容器（C2S 反查与 registry diff） / Stable id → container (C2S lookup & registry diff) */
	private Int2ObjectOpenHashMap<CitizenContainer> byId = new Int2ObjectOpenHashMap<>();
	/** 未托管容器（懒加载，首次访问触发旧格式迁移） / Unmanaged container (lazy; first access migrates old format) */
	private UnmanagedCitizenData unmanaged;
	/** 首次注册表刷新时是否已用全部现存条目校准 id 分配器 / Whether the allocator was calibrated against all existing entries */
	private boolean allocatorReconciled;

	private CitizenSimScheduler() {
	}

	/**
	 * 本维度全部容器（城镇容器 + 未托管容器）。
	 * <p>
	 * All containers in this level (town containers + the unmanaged container).
	 *
	 * @return 容器列表 / container list
	 */
	public List<CitizenContainer> containers() {
		return containers;
	}

	/**
	 * 由稳定 id 反查容器；不存在返回 null。
	 * <p>
	 * Finds the container by stable id; null if absent.
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 容器，或 null / the container, or null
	 */
	public CitizenContainer findById(int citizenId) {
		return byId.get(citizenId);
	}

	/**
	 * 本维度居民总数（全部容器）。
	 * <p>
	 * Total citizen count across all containers.
	 *
	 * @return 总数 / total count
	 */
	public int countAll() {
		int total = 0;
		for (CitizenContainer c : containers)
			total += c.sim().size();
		return total;
	}

	/**
	 * 分配全局唯一稳定 id（委托未托管容器的持久化分配器）。
	 * <p>
	 * Allocates a globally unique stable id (delegates to the unmanaged
	 * container's persisted allocator).
	 *
	 * @param level 维度 / the level
	 * @return 新 id / the new id
	 */
	public int allocId(ServerLevel level) {
		return getUnmanaged(level).allocId();
	}

	/**
	 * 命令生成一个野居民（未托管容器；Y 贴合高度图）。
	 * <p>
	 * Spawns a command citizen (unmanaged container; Y conforms to the heightmap).
	 *
	 * @param level 维度 / the level
	 * @param blockX 方块 X / block X
	 * @param blockZ 方块 Z / block Z
	 * @return 新居民的稳定 id / stable id of the new citizen
	 */
	public int spawnUnmanaged(ServerLevel level, int blockX, int blockZ) {
		UnmanagedCitizenData data = getUnmanaged(level);
		int id = data.spawn(level, blockX, blockZ, -1);
		register(data, id);
		return id;
	}

	/**
	 * 命令生成一个野居民，带地面高度提示。
	 * <p>
	 * Spawns a command citizen with a ground-height hint.
	 *
	 * @param level 维度 / the level
	 * @param blockX 方块 X / block X
	 * @param blockZ 方块 Z / block Z
	 * @param blockY 地面高度提示（方块单位）；&lt;= 0 忽略 / ground-height hint; &lt;= 0 ignores it
	 * @return 新居民的稳定 id / stable id of the new citizen
	 */
	public int spawnUnmanaged(ServerLevel level, int blockX, int blockZ, int blockY) {
		UnmanagedCitizenData data = getUnmanaged(level);
		int id = data.spawn(level, blockX, blockZ, blockY);
		register(data, id);
		return id;
	}

	/**
	 * 只清未托管命令居民（{@code /fhcitizen clear} 语义：不碰 town 居民——
	 * 模拟是 town 的挂件，调试命令不驱动 town 驱动的居民）。
	 * <p>
	 * Clears only unmanaged command citizens ({@code /fhcitizen clear}
	 * semantics: town residents are untouched — the simulation is a town
	 * attachment, debug commands don't drive town-driven citizens).
	 *
	 * @param level 维度 / the level
	 * @return 清除的条目数 / number of cleared entries
	 */
	public int clearUnmanaged(ServerLevel level) {
		UnmanagedCitizenData data = getUnmanaged(level);
		CitizenSim sim = data.sim();
		// 先收集后删：swap-remove 会移动尾部元素，边扫边删会跳过元素
		IntArrayList ids = new IntArrayList(sim.size());
		for (int i = 0; i < sim.size(); i++)
			ids.add(sim.id[i]);
		for (int k = 0; k < ids.size(); k++)
			remove(data, ids.getInt(k));
		return ids.size();
	}

	/**
	 * 立即登记一个新条目，消除出生后到下次 20t 注册表刷新之间的不可见窗口。
	 * <p>
	 * Registers a new entry immediately, removing the lookup gap between birth
	 * and the next 20-tick registry refresh.
	 *
	 * @param container 所属容器 / owning container
	 * @param citizenId 稳定 id / stable id
	 */
	void register(CitizenContainer container, int citizenId) {
		byId.put(citizenId, container);
	}

	/**
	 * 统一移除路径：容器 sim 移除 + 容器缓存清理（onDataRemoved）+ despawn 广播。
	 * <p>
	 * Unified removal path: container sim removal + container cache cleanup
	 * (onDataRemoved) + despawn broadcast.
	 *
	 * @param level 维度 / the level
	 * @param citizenId 稳定 id / stable id
	 */
	public void remove(ServerLevel level, int citizenId) {
		CitizenContainer c = byId.get(citizenId);
		if (c == null)
			return;
		remove(c, citizenId);
	}

	/**
	 * 从已知所属容器立即移除条目。事件驱动的出生/移除可能发生在两次注册表刷新
	 * 之间，因此不能只依赖可能滞后的 {@link #byId}。
	 * <p>
	 * Removes an entry from its known owner immediately. Event-driven births
	 * and removals can occur between registry refreshes, so this path must not
	 * rely solely on the potentially stale {@link #byId} lookup.
	 *
	 * @param container 所属容器 / owning container
	 * @param citizenId 稳定 id / stable id
	 */
	void remove(CitizenContainer container, int citizenId) {
		if (!removeData(container, citizenId))
			return;
		if (byId.get(citizenId) == container)
			byId.remove(citizenId);
		sync.notifyRemoved(citizenId);
	}

	/** 数据层统一删除语义：条目、运行期缓存与 dirty 标记必须原子同行。 */
	static boolean removeData(CitizenContainer container, int citizenId) {
		if (!container.sim().remove(citizenId))
			return false;
		container.onDataRemoved(citizenId);
		container.markDirty();
		return true;
	}

	/**
	 * 服务端主 tick。
	 * <p>
	 * Main server tick.
	 *
	 * @param level 维度 / the level
	 */
	public void tick(ServerLevel level) {
		long gameTime = level.getGameTime();
		if (gameTime % 20 == 0) {
			refreshActivity(level);
			refreshRegistry(level);
		}
		if (gameTime % 5 == 0)
			grid.rebuild(containers, this::isSpatialPresent);
		behavior.tick(this, level, (int) (gameTime % BehaviorSystem.SLICE), gameTime);
		movement.tickAll(this, level, gameTime);
		fields.tick(level, gameTime);
		sync.flush(this, level, gameTime);
	}

	private void refreshActivity(ServerLevel level) {
		activeCells.clear();
		int r = ACTIVE_RADIUS + 2; // 向外留一格缓冲，避免边界抖动
		for (ServerPlayer p : level.players()) {
			int pbx = p.getBlockX();
			int pbz = p.getBlockZ();
			for (int dx = -r; dx <= r; dx += 2)
				for (int dz = -r; dz <= r; dz += 2)
					activeCells.add(SpatialGrid.cellKey(pbx + dx, pbz + dz));
		}
	}

	/**
	 * 判断居民是否处于活跃区。
	 * <p>
	 * Whether the citizen at the container index is in an active area.
	 *
	 * @param container 容器 / the container
	 * @param index 运行期索引 / runtime index
	 * @return 活跃返回 true / true if active
	 */
	public boolean isActive(CitizenContainer container, int index) {
		CitizenSim sim = container.sim();
		return activeCells.contains(SpatialGrid.cellKey(sim.px[index] >> 10, sim.pz[index] >> 10));
	}

	private boolean isSpatialPresent(CitizenContainer container, int index) {
		return isActive(container, index) && CitizenPresence.spatialPresent(container.sim().state[index]);
	}

	/**
	 * 容器注册表门控聚合（20t，无对账）：
	 * <ul>
	 *   <li>维度门控（与 ClimateCommonEvents 一致：能量塔维度）过滤镇容器；</li>
	 *   <li>首次接管：一次性恢复 + 注册居民变更事件（事件驱动人口增删）；</li>
	 *   <li>维度变更检测：跨维度全量重生（onLevelChange）；</li>
	 *   <li>registry diff：本周期消失的条目 → despawn 广播（镇消失/维度切换）。</li>
	 * </ul>
	 * <p>
	 * Gated container-registry aggregation (20t, no reconciliation): dimension
	 * gate (generator dimension, same as ClimateCommonEvents); first takeover
	 * (one-time restore + event subscription); level-change detection; and a
	 * registry diff broadcasting despawns for entries that vanished this
	 * period (town gone / level switched).
	 *
	 * @param level 维度 / the level
	 */
	private void refreshRegistry(ServerLevel level) {
		reconcileAllocator(level);
		List<CitizenContainer> newContainers = new ArrayList<>();
		Int2ObjectOpenHashMap<CitizenContainer> newById = new Int2ObjectOpenHashMap<>();
		UnmanagedCitizenData unmanaged = getUnmanaged(level);
		IntOpenHashSet usedIds = new IntOpenHashSet();
		CitizenSim unmanagedSim = unmanaged.sim();
		for (int i = 0; i < unmanagedSim.size(); i++)
			usedIds.add(unmanagedSim.id[i]);
		CTeamDataManager.INSTANCE.forAllData(FHSpecialDataTypes.TOWN_DATA, (townData, holder) -> {
			if (!ITown.DEBUG_MODE && !isTownInLevel(holder, level))
				return;
			// 模拟数据不挂队伍：全局单文件按队伍 id 取（玩家镇模拟与队伍零关联）
			TownSimData data = AITownManager.getPlayerSim(holder.getId());
			Collection<Resident> residents = TeamTown.create(townData).getAllResidents();
			if (!data.isAdopted()) {
				// 首次接管：注入 dirty 回调 + 一次性恢复（启动加载/旧存档迁移）+ 注册居民变更事件
				data.setMarkDirty(AITownManager::markDirty);
				data.adopt(this, level, townData);
			} else {
				// 维度变更检测：跨维度全量重生
				data.onLevelChange(level, this, residents);
			}
			newContainers.add(data);
			indexTownContainer(data, residents, unmanaged, usedIds, newById);
		});
		// AI 镇段：独立 Town（无队伍），走全局注册表（维度门控用自身落盘维度）
		for (AITownData aiTown : AITownManager.all()) {
			if (!aiTown.isInLevel(level))
				continue;
			TownSimData data = aiTown.getSimData();
			if (!data.isAdopted()) {
				// 首次接管：注入全局 dirty 回调 + 一次性对账（补建添加时未接管窗口的条目）
				data.setMarkDirty(AITownManager::markDirty);
				data.adopt(this, level, aiTown);
			} else {
				// 维度变更检测：跨维度全量重生
				data.onLevelChange(level, this, aiTown.getAllResidents());
			}
			newContainers.add(data);
			indexTownContainer(data, aiTown.getAllResidents(), unmanaged, usedIds, newById);
		}
		this.unmanaged = unmanaged;
		newContainers.add(unmanaged);
		for (int i = 0; i < unmanagedSim.size(); i++)
			newById.put(unmanagedSim.id[i], unmanaged);
		// registry diff：本周期消失的条目 → despawn 广播（镇消失/维度切换/事件竞态兜底）
		for (Int2ObjectOpenHashMap.Entry<CitizenContainer> e : byId.int2ObjectEntrySet()) {
			int id = e.getIntKey();
			if (!newById.containsKey(id))
				sync.notifyRemoved(id);
		}
		this.containers = newContainers;
		this.byId = newById;
	}

	/** 将 town 容器加入注册表；跨容器 id 冲突时仅换会话 id，不丢当前位置与状态。 */
	private void indexTownContainer(TownSimData data, Collection<Resident> residents,
			UnmanagedCitizenData allocator, IntOpenHashSet usedIds,
			Int2ObjectOpenHashMap<CitizenContainer> registry) {
		CitizenSim sim = data.sim();
		allocator.ensureNextIdAfter(maxId(sim));
		for (int i = 0; i < sim.size(); i++) {
			int id = sim.id[i];
			if (!usedIds.add(id)) {
				int replacement = allocator.allocId();
				data.reassignId(i, replacement, residents);
				usedIds.add(replacement);
				FHMain.LOGGER.warn("Reassigned duplicate citizen id {} to {} while rebuilding level registry",
						id, replacement);
				id = replacement;
			}
			registry.put(id, data);
		}
	}

	/**
	 * 首次接管前校准 per-level id 分配器。条目和分配器分属两个 SavedData 文件，
	 * 异常中断可能只完成其中一个文件的写入；扫描一次现存最大 id 可避免重用。
	 */
	private void reconcileAllocator(ServerLevel level) {
		if (allocatorReconciled)
			return;
		UnmanagedCitizenData data = getUnmanaged(level);
		int[] highestId = { maxId(data.sim()) };
		CTeamDataManager.INSTANCE.forAllData(FHSpecialDataTypes.TOWN_DATA, (townData, holder) -> {
			if (!ITown.DEBUG_MODE && !isTownInLevel(holder, level))
				return;
			highestId[0] = Math.max(highestId[0], maxId(AITownManager.getPlayerSim(holder.getId()).sim()));
		});
		for (AITownData aiTown : AITownManager.all()) {
			if (aiTown.isInLevel(level))
				highestId[0] = Math.max(highestId[0], maxId(aiTown.getSimData().sim()));
		}
		data.ensureNextIdAfter(highestId[0]);
		allocatorReconciled = true;
	}

	private static int maxId(CitizenSim sim) {
		int max = 0;
		for (int i = 0; i < sim.size(); i++)
			max = Math.max(max, sim.id[i]);
		return max;
	}

	/**
	 * 获取未托管容器（懒加载）。
	 * <p>
	 * Gets the unmanaged container (lazily created).
	 *
	 * @param level 维度 / the level
	 * @return 容器 / the container
	 */
	private UnmanagedCitizenData getUnmanaged(ServerLevel level) {
		UnmanagedCitizenData data = unmanaged;
		if (data == null) {
			data = UnmanagedCitizenData.get(level);
			unmanaged = data;
		}
		return data;
	}

	/**
	 * 维度门控，与 ClimateCommonEvents.onServerTick 完全一致：仅当队伍能量塔的
	 * dimension 等于当前 level 时才处理该镇（城镇唯一的维度概念）。
	 * <p>
	 * Dimension gate, identical to ClimateCommonEvents.onServerTick: the town is
	 * handled only when the team's generator tower dimension equals this level
	 * (a town's only dimension concept).
	 *
	 * @param holder 队伍数据持有者 / the team data holder
	 * @param level 当前维度 / the current level
	 * @return 该镇属于此维度返回 true / true if the town belongs to this level
	 */
	public static boolean isTownInLevel(TeamDataHolder holder, ServerLevel level) {
		return holder.getOptional(FHSpecialDataTypes.GENERATOR_DATA)
				.filter(g -> level.dimension().equals(g.dimension)).isPresent();
	}

	/**
	 * 地面生成点：锚点常是 1 格的建筑标记块，列顶可能是屋顶。
	 * 先在锚点 ±(2,4,6,8) 的 4 个方向找不高于锚点+2 的开放地面；
	 * 找不到回退锚点 y-1（MovementSystem 的逐 tick 贴合会修正 ±2 格以内偏差）。
	 * <p>
	 * Ground spawn point: anchors are often single marker blocks whose column
	 * top may be a roof. Searches the 4 directions at ±(2,4,6,8) for open ground
	 * no higher than anchor y + 2; falls back to anchor y - 1 (the per-tick
	 * MovementSystem conforming corrects deviations within ±2 blocks).
	 *
	 * @param level 维度 / the level
	 * @param anchor 建筑锚点 / the building anchor
	 * @return 生成位置 / spawn position
	 */
	public static BlockPos groundNear(ServerLevel level, BlockPos anchor) {
		for (int r = 2; r <= 8; r += 2) {
			for (int k = 0; k < 4; k++) {
				int ox = anchor.getX() + DIR_X[k] * r;
				int oz = anchor.getZ() + DIR_Z[k] * r;
				BlockPos candidate = safeExitAt(level, anchor, ox, oz);
				if (candidate != null)
					return candidate;
			}
		}
		BlockPos entrance = safeExitAt(level, anchor, anchor.getX(), anchor.getZ());
		return entrance != null ? entrance : anchor;
	}

	/**
	 * 按住宅内稳定槽位选择首选出口，并在地形不可用或已占用时向外探测。
	 * 槽位只存在于运行时；常规路径首个候选即可命中，复杂地形最多检查固定数量的候选。
	 */
	public static BlockPos groundNear(ServerLevel level, BlockPos anchor, int homeSlot,
			long seed, LongPredicate occupiedXZ) {
		for (int attempt = 0; attempt < EXIT_SEARCH_ATTEMPTS; attempt++) {
			long packedXZ = exitCandidatePosition(anchor, homeSlot, seed, attempt);
			if (occupiedXZ.test(packedXZ))
				continue;
			BlockPos candidate = safeExitAt(level, anchor, BlockPos.getX(packedXZ), BlockPos.getZ(packedXZ));
			if (candidate != null)
				return candidate;
		}
		BlockPos entrance = safeExitAt(level, anchor, anchor.getX(), anchor.getZ());
		long entranceXZ = BlockPos.asLong(anchor.getX(), 0, anchor.getZ());
		return entrance != null && !occupiedXZ.test(entranceXZ) ? entrance : groundNear(level, anchor);
	}

	/** Compatibility overload for callers without a house-local slot. */
	public static BlockPos groundNear(ServerLevel level, BlockPos anchor, long seed) {
		return groundNear(level, anchor, -1, seed, ignored -> false);
	}

	static long exitCandidatePosition(BlockPos anchor, int homeSlot, long seed, int attempt) {
		int firstSlot = homeSlot >= 0 ? homeSlot : ((int) seed & 31);
		int slot = firstSlot + attempt;
		int radius = 2 + (slot >>> 3);
		int directionOffset = (int) (seed ^ (seed >>> 32)) & 7;
		int direction = (slot + directionOffset) & 7;
		int x = anchor.getX() + EXIT_DIR_X[direction] * radius;
		int z = anchor.getZ() + EXIT_DIR_Z[direction] * radius;
		return BlockPos.asLong(x, 0, z);
	}

	private static BlockPos safeExitAt(ServerLevel level, BlockPos anchor, int x, int z) {
		if (!level.hasChunkAt(x, z))
			return null;
		int feetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (feetY < anchor.getY() - 2 || feetY > anchor.getY() + 2)
			return null;
		BlockPos feet = new BlockPos(x, feetY, z);
		BlockPos head = feet.above();
		BlockPos support = feet.below();
		var feetState = level.getBlockState(feet);
		var headState = level.getBlockState(head);
		var supportState = level.getBlockState(support);
		if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()
				|| !supportState.getFluidState().isEmpty())
			return null;
		if (!feetState.getCollisionShape(level, feet).isEmpty()
				|| !headState.getCollisionShape(level, head).isEmpty()
				|| supportState.getCollisionShape(level, support).isEmpty())
			return null;
		return feet;
	}

	static final int EXIT_SEARCH_ATTEMPTS = 256;
	private static final int[] DIR_X = { 1, -1, 0, 0 };
	private static final int[] DIR_Z = { 0, 0, 1, -1 };
	private static final int[] EXIT_DIR_X = { 1, 1, 0, -1, -1, -1, 0, 1 };
	private static final int[] EXIT_DIR_Z = { 0, 1, 1, 1, 0, -1, -1, -1 };
}
