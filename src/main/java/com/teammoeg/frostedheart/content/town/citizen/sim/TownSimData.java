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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.ai_town.AITownData;
import com.teammoeg.frostedheart.content.town.event.ITownResidentListener;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 城镇居民模拟数据（与队伍零关联的纯模拟数据）：玩家镇与 AI 镇共用同一类，
 * 存储统一在全局单文件（{@code AITownManager} 的 SavedData，玩家镇实例经
 * {@code AITownManager.getPlayerSim} 按队伍 id 取、AI 镇实例内嵌 AITownData）。
 * "模拟完全挂靠 town"——每个城镇（玩家/非玩家）天然拥有自己的一份模拟；
 * 居民增删事件直接驱动出生/移除：adopt 时作为唯一订阅者经
 * {@code TeamTownData.setResidentListener} 注册（门面单订阅，事件由
 * {@code TeamTown} 在房屋分配完成后 fire——锚点必已就绪、无双触发），
 * 每日结算完成经同一监听器刷新锚点，首次接管时一次性恢复。
 * 事件驱动，无周期性对账同步器。
 * <p>
 * NBT 序列化（{@link #toNbt}/{@link #loadFromNbt}）仅落盘 sim（tradeData/
 * nameCache 是运行期身份状态）；解码由存储方 total 化（坏数据绝不向外抛——
 * 会拒绝整个全局文件，按空模拟启动，由接管对账重建）。落盘遵循标准 SavedData
 * 语义：结构变更（出生/移除条目）经 {@link #setMarkDirty} 注入的 dirty 回调
 * 标记，Minecraft 自动保存/停服负责写盘；位置数据为瞬态不标记。
 * <p>
 * Shared per-town citizen simulation data, decoupled from the team entirely:
 * player towns and AI towns use this same class, all stored in the global
 * single-file save ({@code AITownManager}). The simulation is fully attached
 * to the town: resident add/remove drives spawn/removal — on adopt this data
 * registers as the sole subscriber via {@code TeamTownData.setResidentListener}
 * (facade single-subscription; the town fires added only after house
 * allocation — anchor ready, no double-fire); daily settlement refreshes
 * anchors through the same listener; the first scheduler takeover performs a
 * one-time restore. Event-driven, no periodic reconciliation.
 */
public class TownSimData implements CitizenContainer, ITownResidentListener {

	/** 模拟核心数据（SoA 紧排布局） / Core simulation data (SoA layout) */
	public final CitizenSim sim = new CitizenSim(64);

	/**
	 * FH 交易数据（懒创建；null parent 构造，policytype 为 null → 空政策快照）。
	 * 运行期状态，不落盘。
	 * <p>
	 * FH trade data (lazily created; null parent — null policytype → empty policy).
	 * Runtime state, not persisted.
	 */
	private final Int2ObjectOpenHashMap<FHVillagerData> tradeData = new Int2ObjectOpenHashMap<>();
	/**
	 * 显示名缓存（simId → "名 姓"）。同步层的属性缓存，不是身份——
	 * 身份是 Resident.simId ↔ CitizenSim.uuidHi/uuidLo。运行期状态，不落盘。
	 * <p>
	 * Display-name cache (simId → "First Last"). A sync-layer attribute cache,
	 * not identity — identity is Resident.simId ↔ CitizenSim.uuidHi/uuidLo.
	 * Runtime state, not persisted.
	 */
	private final Int2ObjectOpenHashMap<String> nameCache = new Int2ObjectOpenHashMap<>();

	/** 最后活跃维度（transient）：维度切换时全量重生 / Last active level: full respawn across dimensions */
	private transient ResourceKey<Level> lastActiveLevel;
	/** 接管状态（transient）：调度器首次接管时完成一次性恢复 + 注册事件 / Adopted flag: one-time restore + event registration */
	private transient boolean adopted = false;
	/** 接管时的调度器/维度引用（transient，事件回调内使用）/ Scheduler & level references (transient, used in callbacks) */
	private transient CitizenSimScheduler activeSched;
	private transient ServerLevel activeLevel;
	/** 结构变更 dirty 回调（transient，由存储方注入：玩家镇 → AITownManager::markDirty；AI 镇不设——AITownData 自管）/ Structural-change dirty callback (transient, injected by the store: player towns → AITownManager::markDirty; AI towns leave unset — AITownData manages its own dirty) */
	private transient Runnable markDirtyCallback;

	public TownSimData() {
		super();
	}

	/**
	 * 注入结构变更 dirty 回调（存储方在 adopt 前调用）：条目出生/移除时标记，
	 * 由 Minecraft 自动保存/停服负责落盘（标准 SavedData 语义，无周期调度）。
	 * <p>
	 * Injects the structural-change dirty callback (called by the store before
	 * adopt): marks on entry spawn/removal; Minecraft's autosave and server
	 * stop handle the actual write (standard SavedData semantics, no periodic
	 * scheduling).
	 *
	 * @param callback 回调；null 清除 / the callback; null clears it
	 */
	public void setMarkDirty(Runnable callback) {
		this.markDirtyCallback = callback;
	}

	/* ===================== 序列化 ===================== */

	/**
	 * 序列化（仅写 sim；tradeData/nameCache/lastActiveLevel/adopted 均不落盘）。
	 * public：供存储方（AITownManager 全局文件）与 CODEC 调用。
	 * <p>
	 * Serializes (sim only; tradeData/nameCache/lastActiveLevel/adopted are
	 * runtime state, not persisted). Public for the store (AITownManager's
	 * global save) and the CODEC.
	 */
	public static CompoundTag toNbt(TownSimData data) {
		return data.sim.save(new CompoundTag());
	}

	/**
	 * 从 NBT 恢复（逐条合并进 final sim）。
	 * <p>
	 * Restores from NBT (merges entry by entry into the final sim).
	 *
	 * @param tag 源标签 / source tag
	 */
	public void loadFromNbt(CompoundTag tag) {
		CompoundTag simTag = tag.getCompound("sim");
		if (simTag.isEmpty())
			return;
		CitizenSim loaded = CitizenSim.load(simTag);
		for (int k = 0; k < loaded.size(); k++) {
			int i = sim.add(loaded.id[k], loaded.px[k], loaded.py[k], loaded.pz[k], loaded.tickPhase[k]);
			sim.yaw[i] = loaded.yaw[k];
            sim.syaw[i] = loaded.syaw[k];
			sim.state[i] = loaded.state[k];
			sim.homeX[i] = loaded.homeX[k];
			sim.homeZ[i] = loaded.homeZ[k];
			sim.wx[i] = loaded.wx[k];
			sim.wz[i] = loaded.wz[k];
			sim.uuidHi[i] = loaded.uuidHi[k];
			sim.uuidLo[i] = loaded.uuidLo[k];
			sim.tx[i] = loaded.tx[k];
			sim.ty[i] = loaded.ty[k];
			sim.tz[i] = loaded.tz[k];
		}
	}

	/* ===================== 事件入口（TeamTownData 门面单订阅回调） ===================== */

	/**
	 * 居民加入城镇 → 立即出生模拟条目（事件驱动）。
	 * 经 {@code TeamTownData.fireResidentAdded} 调用（唯一触发点：
	 * {@code TeamTown.addResident} 成功路径 / {@code debugAddResident}，房屋分配
	 * 完成后——锚点必已就绪，单次通知无双触发）。幂等：按 uuid 查重，重复事件只
	 * 更新锚点/工作/名字。未接管（未 adopt）时未注册，此回调不会被调用。
	 * <p>
	 * A resident entered the town → spawn its simulation entry immediately
	 * (event-driven). Invoked via {@code TeamTownData.fireResidentAdded}
	 * (sole trigger: successful {@code TeamTown.addResident} /
	 * {@code debugAddResident}, after house allocation — anchor guaranteed
	 * ready, single notification, no double-fire). Idempotent: deduped by
	 * uuid; duplicates only refresh anchors/work/name. Not called before
	 * adoption.
	 */
	@Override
	public void onResidentAdded(Resident resident) {
		if (activeSched == null || activeLevel == null)
			return;
		UUID u = resident.getUUID();
		long hi = u.getMostSignificantBits();
		long lo = u.getLeastSignificantBits();
		BlockPos house = resident.getHousePos();
		BlockPos work = resident.getWorkPos();
		BlockPos anchor = house != null ? house : work;
		if (anchor == null)
			return; // 无家无业：不建条目（与每日无家移除一致）
		int idx = sim.findByUuid(hi, lo);
		if (idx >= 0) {
			// 幂等：重复事件只更新锚点/工作/名字（房屋与工作每日由城镇重分配）
			sim.homeX[idx] = anchor.getX();
			sim.homeZ[idx] = anchor.getZ();
			sim.wx[idx] = work != null ? work.getX() : -1;
			sim.wz[idx] = work != null ? work.getZ() : -1;
			nameCache.put(sim.id[idx], resident.getFirstName() + " " + resident.getLastName());
			return;
		}
		// 新条目：出生在锚点附近地面，双向绑定身份（simId ↔ uuid）
		BlockPos g = CitizenSimScheduler.groundNear(activeLevel, anchor);
		int id = activeSched.allocId(activeLevel);
		int i = sim.add(id, (g.getX() << 10) + 512, g.getY() << 10, (g.getZ() << 10) + 512,
				(byte) (id % BehaviorSystem.SLICE));
		sim.uuidHi[i] = hi;
		sim.uuidLo[i] = lo;
		sim.homeX[i] = anchor.getX();
		sim.homeZ[i] = anchor.getZ();
		sim.wx[i] = work != null ? work.getX() : -1;
		sim.wz[i] = work != null ? work.getZ() : -1;
		resident.setSimId(id);
		nameCache.put(id, resident.getFirstName() + " " + resident.getLastName());
		// 结构变更（新条目出生）：标记 dirty，随 Minecraft 自动保存/停服落盘
		if (markDirtyCallback != null)
			markDirtyCallback.run();
	}

	/**
	 * 居民移出城镇 → 立即移除模拟条目（despawn 由调度器统一广播）。
	 * 经 {@code TeamTownData.fireResidentRemoved} 调用（{@code TeamTown.removeResident}
	 * 集合移除完成后单次通知）。幂等：未见过的居民忽略。
	 * <p>
	 * A resident left the town → its simulation entry is removed immediately
	 * (the despawn broadcast goes through the scheduler's unified removal).
	 * Invoked via {@code TeamTownData.fireResidentRemoved} (single notification
	 * after the collection removal in {@code TeamTown.removeResident}).
	 * Idempotent: unknown residents are ignored.
	 */
	@Override
	public void onResidentRemoved(Resident resident) {
		if (activeSched == null || activeLevel == null)
			return;
		UUID u = resident.getUUID();
		int idx = sim.findByUuid(u.getMostSignificantBits(), u.getLeastSignificantBits());
		if (idx < 0)
			return;
		resident.setSimId(-1);
		activeSched.remove(activeLevel, sim.id[idx]);
		// 结构变更（条目移除）：标记 dirty，随 Minecraft 自动保存/停服落盘
		if (markDirtyCallback != null)
			markDirtyCallback.run();
	}

	/**
	 * 每日结算完成（tickMorning 末尾，经 {@code TeamTownData.fireMorningDone}）→
	 * 重扫本镇居民刷新锚点/工作/名字、移除无家条目、剪除幽灵（跟随 town 生命周期，
	 * 无需周期性对账）。
	 * <p>
	 * Daily settlement done (end of tickMorning, via
	 * {@code TeamTownData.fireMorningDone}) → rescan this town's residents to
	 * refresh anchors/work/names, drop homeless entries and prune ghosts
	 * (follows the town lifecycle, no periodic reconciliation).
	 */
	@Override
	public void onTownMorningDone(TeamTownData townData) {
		if (activeSched == null || activeLevel == null)
			return;
		syncTownResidents(activeLevel, townData);
	}

	/* ===================== 接管与恢复 ===================== */

	/**
	 * 调度器首次接管：一次性恢复（覆盖启动加载——codec 加载期监听器未注册——
	 * 与旧存档迁移）并把本实例注册为 {@code TeamTownData} 的唯一居民生命周期
	 * 监听器（门面单订阅——事件通道一层，无转发集合/监听器接口，与
	 * DataSyncCache 的 ObservableTownMap 钩子链完全分离）。
	 * <p>
	 * First scheduler takeover: one-time restore (covers startup loading —
	 * the listener is unregistered during codec load — and old-save migration)
	 * and registers this instance as the sole resident lifecycle listener on
	 * {@code TeamTownData} (facade single-subscription — one event layer, no
	 * forwarding sets or listener interfaces, fully separate from the
	 * ObservableTownMap hook chain used by DataSyncCache).
	 *
	 * @param sched 调度器 / the scheduler
	 * @param level 维度 / the level
	 * @param townData 本镇数据 / this town's data
	 */
	public void adopt(CitizenSimScheduler sched, ServerLevel level, TeamTownData townData) {
		this.activeSched = sched;
		this.activeLevel = level;
		this.adopted = true;
		townData.setResidentListener(this);
		syncTownResidents(level, townData);
	}

	/**
	 * 调度器首次接管（AI 镇重载）：设置运行期引用并做一次性对账——AI 镇居民
	 * 由命令运行期添加，存在"添加时 sim 未接管 → 不出生"窗口，接管时按居民
	 * 集合对账补建（事件驱动出生走 {@link #onResidentAdded}；对账与玩家镇共用
	 * 同一逻辑，AI 镇居民不演化故此后无每日对账）。
	 * <p>
	 * First scheduler takeover (AI-town overload): sets the runtime references
	 * and performs a one-time reconciliation — AI residents are added by
	 * commands at runtime, so there is a window where an addition lands before
	 * adoption; the takeover reconciles against the resident collection to
	 * backfill (event-driven spawn via {@link #onResidentAdded}; the same logic
	 * as player towns, and since AI residents do not evolve there is no daily
	 * reconciliation afterwards).
	 *
	 * @param sched 调度器 / the scheduler
	 * @param level 维度 / the level
	 * @param aiTown 本 AI 镇数据 / this AI town's data
	 */
	public void adopt(CitizenSimScheduler sched, ServerLevel level, AITownData aiTown) {
		this.activeSched = sched;
		this.activeLevel = level;
		this.adopted = true;
		syncTownResidents(level, aiTown.getAllResidents());
	}

	/**
	 * 是否已被调度器接管。
	 * <p>
	 * Whether the scheduler has adopted this data.
	 *
	 * @return 已接管返回 true / true if adopted
	 */
	public boolean isAdopted() {
		return adopted;
	}

	/**
	 * 维度变更检测（每 20 tick 由调度器调用）：跨维度时全量清空本镇模拟
	 * （条目重生由事件驱动在新维度重新出生；despawn 由调度器 registry diff 广播）。
	 * <p>
	 * Level-change detection (called every 20 ticks by the scheduler): on a
	 * cross-dimension switch the town's entries are fully cleared (respawning is
	 * event-driven on the new level; despawns go through the registry diff).
	 *
	 * @param level 当前维度 / the current level
	 * @param sched 调度器 / the scheduler
	 */
	public void onLevelChange(ServerLevel level, CitizenSimScheduler sched) {
		if (lastActiveLevel != null && !lastActiveLevel.equals(level.dimension())) {
			sim.clear();
			tradeData.clear();
			nameCache.clear();
			FHMain.LOGGER.debug("Citizen sim for town reset across dimension change");
		}
		lastActiveLevel = level.dimension();
	}

	/**
	 * 全量对账本镇居民与模拟条目（adopt 一次性恢复与每日尾钩共用；玩家镇与
	 * AI 镇同一逻辑）：uuid 匹配重建 simId 绑定（位置连续）、补缺失条目、
	 * 无家移除、幽灵剪除。
	 * <p>
	 * Full reconciliation of this town's residents against simulation entries
	 * (shared by the one-time adopt restore and the daily tail hook; the same
	 * logic for player and AI towns): rebuilds simId bindings by uuid (position
	 * continuity), spawns missing entries, drops homeless ones and prunes ghosts.
	 */
	private void syncTownResidents(ServerLevel level, Collection<Resident> residents) {
		Set<Long> liveKeys = new HashSet<>();
		for (Resident r : residents) {
			UUID u = r.getUUID();
			long hi = u.getMostSignificantBits();
			long lo = u.getLeastSignificantBits();
			liveKeys.add(hi ^ lo);
			BlockPos house = r.getHousePos();
			BlockPos work = r.getWorkPos();
			BlockPos anchor = house != null ? house : work;
			int idx = sim.findByUuid(hi, lo);
			if (anchor == null) {
				// 无家无业：城镇每日也让它掉血至死，模拟里不保留幽灵
				if (idx >= 0) {
					r.setSimId(-1);
					activeSched.remove(level, sim.id[idx]);
				}
				continue;
			}
			if (idx < 0) {
				// 缺失条目：重建（事件丢失/旧存档迁移后的一次性恢复）
				onResidentAdded(r);
			} else {
				// 既有条目：更新锚点/工作/名字（房屋与工作每日由城镇重分配）
				sim.homeX[idx] = anchor.getX();
				sim.homeZ[idx] = anchor.getZ();
				sim.wx[idx] = work != null ? work.getX() : -1;
				sim.wz[idx] = work != null ? work.getZ() : -1;
				r.setSimId(sim.id[idx]);
				nameCache.put(sim.id[idx], r.getFirstName() + " " + r.getLastName());
			}
		}
		// 幽灵剪除：本容器条目代表的居民已不在城镇（防御非门面直写/事件丢失）。
		// 先收集后删除——swap-remove 会移动尾部元素，边扫边删会跳过元素。
		IntArrayList stale = new IntArrayList();
		for (int i = 0; i < sim.size(); i++) {
			if (sim.uuidHi[i] == 0 && sim.uuidLo[i] == 0)
				continue; // 防御：城镇容器不应有未托管条目
			if (!liveKeys.contains(sim.uuidHi[i] ^ sim.uuidLo[i]))
				stale.add(sim.id[i]);
		}
		for (int k = 0; k < stale.size(); k++)
			activeSched.remove(level, stale.getInt(k));
	}

	/**
	 * 玩家镇适配：经门面取居民集合后委托 Collection 版对账。
	 * <p>
	 * Player-town adapter: resolves residents through the facade and delegates
	 * to the collection version.
	 */
	private void syncTownResidents(ServerLevel level, TeamTownData townData) {
		syncTownResidents(level, TeamTown.create(townData).getAllResidents());
	}

	/* ===================== CitizenContainer ===================== */

	@Override
	public CitizenSim sim() {
		return sim;
	}

	@Override
	public String getCitizenName(int citizenId) {
		return nameCache.get(citizenId);
	}

	@Override
	public FHVillagerData getTradeData(int citizenId) {
		FHVillagerData vd = tradeData.get(citizenId);
		if (vd == null) {
			vd = new FHVillagerData(null);
			vd.setCitizenId(citizenId);
			tradeData.put(citizenId, vd);
		}
		return vd;
	}

	@Override
	public void onDataRemoved(int citizenId) {
		tradeData.remove(citizenId);
		nameCache.remove(citizenId);
	}
	// markDirty：default 空实现——城镇容器经 holder 自动存盘，无需显式标记
}
