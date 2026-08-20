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
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.event.ITownResidentListener;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

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
	 * NBT 序列化（{@link #toNbt}/{@link #loadFromNbt}）落盘 sim 与最近所属维度
	 * （tradeData/nameCache 是运行期身份状态）；解码由存储方 total 化（坏数据绝不向外抛——
 * 会拒绝整个全局文件，按空模拟启动，由接管对账重建）。落盘遵循标准 SavedData
 * 语义：任何落盘字段变化（位置、朝向、状态、目标、锚点、出生/移除）经
 * {@link #setMarkDirty} 注入的 dirty 回调标记，Minecraft 自动保存/停服负责
 * 写盘。这样既保留自动保存检查点，也能在正常退出时写入最后位置。
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

	/** 最后活跃维度（落盘）：跨重启维度切换时也能全量重生 / Last active level (persisted): enables full respawn across restart-spanning dimension changes */
	private ResourceLocation lastActiveDimension;
	/** 接管状态（transient）：调度器首次接管时完成一次性恢复 + 注册事件 / Adopted flag: one-time restore + event registration */
	private transient boolean adopted = false;
	/** 接管时的调度器/维度引用（transient，事件回调内使用）/ Scheduler & level references (transient, used in callbacks) */
	private transient CitizenSimScheduler activeSched;
	private transient ServerLevel activeLevel;
	/** 玩家镇门面（transient）；AI 镇没有可扫描住宅布局，保持 null。 */
	private transient TeamTown activeTown;
	/** 全量对账期间延迟床位重排，避免逐居民重复排序。 */
	private transient boolean reconcilingResidents;
	/** 全量对账期间新增、待统一出口定位的居民 id。 */
	private transient IntArrayList reconcilingNewResidents;
	/** 持久化 dirty 回调（transient，玩家镇与 AI 镇均由存储方注入 AITownManager::markDirty）/ Persistence dirty callback (transient, injected as AITownManager::markDirty for both player and AI towns) */
	private transient Runnable markDirtyCallback;

	public TownSimData() {
		super();
	}

	/**
	 * 注入持久化 dirty 回调（存储方在 adopt 前调用）：任何落盘字段变化时标记，
	 * 由 Minecraft 自动保存/停服负责落盘（标准 SavedData 语义）。
	 * <p>
	 * Injects the persistence dirty callback (called by the store before adopt):
	 * any persisted-field mutation marks the backing SavedData; Minecraft's
	 * autosave and server stop handle the actual write.
	 *
	 * @param callback 回调；null 清除 / the callback; null clears it
	 */
	public void setMarkDirty(Runnable callback) {
		this.markDirtyCallback = callback;
	}

	@Override
	public void markDirty() {
		if (markDirtyCallback != null)
			markDirtyCallback.run();
	}

	/* ===================== 序列化 ===================== */

	/**
	 * 序列化（写 sim 与最后活跃维度；tradeData/nameCache/adopted 不落盘）。
	 * public：供存储方（AITownManager 全局文件）与 CODEC 调用。
	 * <p>
	 * Serializes the sim and last active dimension; tradeData/nameCache/adopted
	 * remain runtime-only. Public for the store (AITownManager's global save)
	 * and the CODEC.
	 */
	public static CompoundTag toNbt(TownSimData data) {
		CompoundTag tag = new CompoundTag();
		tag.put("sim", data.sim.save(new CompoundTag()));
		if (data.lastActiveDimension != null)
			tag.putString("dimension", data.lastActiveDimension.toString());
		return tag;
	}

	/**
	 * 从 NBT 恢复（逐条合并进 final sim）。
	 * <p>
	 * Restores from NBT (merges entry by entry into the final sim).
	 *
	 * @param tag 源标签 / source tag
	 */
	public void loadFromNbt(CompoundTag tag) {
		if (tag.contains("dimension")) {
			String encodedDimension = tag.getString("dimension");
			if (!encodedDimension.isBlank()) {
				ResourceLocation dimension = ResourceLocation.tryParse(encodedDimension);
				if (dimension != null)
					lastActiveDimension = dimension;
			}
		}
		// The first hybrid-simulation build accidentally wrote CitizenSim fields
		// directly into this tag. Accept that flat layout while writing the
		// intended nested layout from now on.
		CompoundTag nested = tag.getCompound("sim");
		CompoundTag simTag = nested.isEmpty() ? tag : nested;
		if (simTag.isEmpty())
			return;
		CitizenSim loaded = CitizenSim.load(simTag);
		for (int k = 0; k < loaded.size(); k++) {
			int i = sim.add(loaded.id[k], loaded.px[k], loaded.py[k], loaded.pz[k]);
			sim.dir[i] = loaded.dir[k];
            sim.sdir[i] = loaded.sdir[k];
			sim.state[i] = loaded.state[k];
			sim.homeX[i] = loaded.homeX[k];
			sim.homeZ[i] = loaded.homeZ[k];
			sim.wx[i] = loaded.wx[k];
			sim.wz[i] = loaded.wz[k];
			sim.uuidHi[i] = loaded.uuidHi[k];
			sim.uuidLo[i] = loaded.uuidLo[k];
			sim.tx[i] = loaded.tx[k];
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
			long previousHome = sim.homePos[idx];
			long nextHome = house != null ? house.asLong() : CitizenSim.NO_HOME_POS;
			boolean persistedChanged = sim.homeX[idx] != anchor.getX() || sim.homeZ[idx] != anchor.getZ()
					|| sim.wx[idx] != (work != null ? work.getX() : -1)
					|| sim.wz[idx] != (work != null ? work.getZ() : -1)
					|| previousHome != nextHome;
			sim.homeX[idx] = anchor.getX();
			sim.homeZ[idx] = anchor.getZ();
			sim.homePos[idx] = nextHome;
			if (previousHome != nextHome) {
				sim.homeSlot[idx] = -1;
			}
			sim.wx[idx] = work != null ? work.getX() : -1;
			sim.wz[idx] = work != null ? work.getZ() : -1;
			nameCache.put(sim.id[idx], resident.getFirstName() + " " + resident.getLastName());
			syncPresentationAge(resident, idx);
			if (!reconcilingResidents) {
				rebuildBedAssignments(previousHome);
				if (sim.homePos[idx] != previousHome)
					rebuildBedAssignments(sim.homePos[idx]);
			}
			if (persistedChanged)
				markDirty();
			return;
		}
		// 新条目先加入住宅分组，再由 UUID 槽位统一选择出口；全量对账会在末尾批处理。
		long fullHome = house != null ? house.asLong() : CitizenSim.NO_HOME_POS;
		BlockPos spawnAnchor = BlockPos.of(resolveEntrance(fullHome, anchor.asLong()));
		int id = activeSched.allocId(activeLevel);
		int i = sim.add(id, (spawnAnchor.getX() << 10) + 512, spawnAnchor.getY() << 10,
				(spawnAnchor.getZ() << 10) + 512,
				(byte) (id % BehaviorSystem.SLICE));
		sim.uuidHi[i] = hi;
		sim.uuidLo[i] = lo;
		syncPresentationAge(resident, i);
		sim.homeX[i] = anchor.getX();
		sim.homeZ[i] = anchor.getZ();
		sim.homePos[i] = fullHome;
		sim.wx[i] = work != null ? work.getX() : -1;
		sim.wz[i] = work != null ? work.getZ() : -1;
		resident.setSimId(id);
		nameCache.put(id, resident.getFirstName() + " " + resident.getLastName());
		activeSched.register(this, id);
		boolean night = BehaviorSystem.isNight(activeLevel);
		if (reconcilingResidents) {
			if (reconcilingNewResidents != null)
				reconcilingNewResidents.add(id);
			if (night)
				prepareSleepingState(i);
		} else {
			rebuildBedAssignments(fullHome);
			if (night) {
				prepareSleepingState(i);
				refreshSleepingResident(i);
			} else {
				long worldDay = Math.floorDiv(activeLevel.getDayTime(), 24000L);
				placeAtHomeExit(i, worldDay);
			}
		}
		markDirty();
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
		long previousHome = sim.homePos[idx];
		resident.setSimId(-1);
		activeSched.remove(this, sim.id[idx]);
		rebuildBedAssignments(previousHome);
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
		this.activeTown = TeamTown.create(townData);
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
		ResourceLocation savedDimension = this.lastActiveDimension;
		this.activeSched = sched;
		this.activeLevel = level;
		this.activeTown = TeamTown.create(townData);
		this.lastActiveDimension = level.dimension().location();
		this.adopted = true;
		townData.setResidentListener(this);
		Collection<Resident> residents = activeTown.getAllResidents();
		if (savedDimension != null && !savedDimension.equals(level.dimension().location()))
			rebuildAfterOfflineLevelChange(level, residents);
		else {
			if (savedDimension == null)
				markDirty();
			syncTownResidents(level, residents);
		}
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
		ResourceLocation savedDimension = this.lastActiveDimension;
		this.activeSched = sched;
		this.activeLevel = level;
		this.activeTown = null;
		this.lastActiveDimension = level.dimension().location();
		this.adopted = true;
		if (savedDimension != null && !savedDimension.equals(level.dimension().location()))
			rebuildAfterOfflineLevelChange(level, aiTown.getAllResidents());
		else {
			if (savedDimension == null)
				markDirty();
			syncTownResidents(level, aiTown.getAllResidents());
		}
	}

	/** 首次接管时发现落盘维度不同：旧会话 id 不属于当前 per-level id 空间。 */
	private void rebuildAfterOfflineLevelChange(ServerLevel level, Collection<Resident> residents) {
		for (Resident resident : residents)
			resident.setSimId(-1);
		sim.clear();
		tradeData.clear();
		nameCache.clear();
		markDirty();
		syncTownResidents(level, residents);
		FHMain.LOGGER.debug("Citizen sim rebuilt after offline dimension change: {} entries", sim.size());
	}

	/**
	 * 保留完整模拟状态地替换冲突会话 id，并同步运行期缓存与 Resident 反向绑定。
	 * <p>
	 * Replaces a colliding session id without losing simulation state, keeping
	 * runtime caches and the Resident reverse binding consistent.
	 */
	void reassignId(int index, int newId, Collection<Resident> residents) {
		long hi = sim.uuidHi[index];
		long lo = sim.uuidLo[index];
		int oldId = sim.replaceId(index, newId);
		String name = nameCache.remove(oldId);
		if (name != null)
			nameCache.put(newId, name);
		FHVillagerData trade = tradeData.remove(oldId);
		if (trade != null) {
			trade.setCitizenId(newId);
			tradeData.put(newId, trade);
		}
		for (Resident resident : residents) {
			UUID uuid = resident.getUUID();
			if (uuid.getMostSignificantBits() == hi && uuid.getLeastSignificantBits() == lo) {
				resident.setSimId(newId);
				break;
			}
		}
		markDirty();
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
	 * 维度变更检测（每 20 tick 由调度器调用）：跨维度时先通知旧调度器移除旧 id，
	 * 再切换运行期引用、清空旧模拟并按权威居民集合立即在新维度重建。
	 * <p>
	 * Level-change detection (called every 20 ticks by the scheduler): on a
	 * cross-dimension switch, notifies the old scheduler about the old ids,
	 * switches runtime references, clears the old simulation, and immediately
	 * rebuilds it in the new level from the authoritative resident collection.
	 *
	 * @param level 当前维度 / the current level
	 * @param sched 调度器 / the scheduler
	 * @param residents 本镇权威居民集合 / authoritative residents of this town
	 */
	public void onLevelChange(ServerLevel level, CitizenSimScheduler sched,
			Collection<Resident> residents) {
		if (lastActiveDimension == null || lastActiveDimension.equals(level.dimension().location())) {
			this.activeSched = sched;
			this.activeLevel = level;
			lastActiveDimension = level.dimension().location();
			return;
		}

		CitizenSimScheduler oldSched = this.activeSched;
		IntArrayList oldIds = new IntArrayList(sim.size());
		for (int i = 0; i < sim.size(); i++)
			oldIds.add(sim.id[i]);
		if (oldSched != null && oldSched != sched) {
			for (int i = 0; i < oldIds.size(); i++)
				oldSched.sync.notifyRemoved(oldIds.getInt(i));
		}

		this.activeSched = sched;
		this.activeLevel = level;
		lastActiveDimension = level.dimension().location();
		for (Resident resident : residents)
			resident.setSimId(-1);
		sim.clear();
		tradeData.clear();
		nameCache.clear();
		markDirty();
		syncTownResidents(level, residents);
		FHMain.LOGGER.debug("Citizen sim for town rebuilt across dimension change: {} entries", sim.size());
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
		IntArrayList pendingPlacements = new IntArrayList();
		reconcilingResidents = true;
		reconcilingNewResidents = pendingPlacements;
		try {
			Set<UUID> liveIds = new HashSet<>();
			for (Resident r : residents) {
				UUID u = r.getUUID();
				long hi = u.getMostSignificantBits();
				long lo = u.getLeastSignificantBits();
				liveIds.add(u);
				BlockPos house = r.getHousePos();
				BlockPos work = r.getWorkPos();
				BlockPos anchor = house != null ? house : work;
				int idx = sim.findByUuid(hi, lo);
				if (anchor == null) {
					// 无家无业：城镇每日也让它掉血至死，模拟里不保留幽灵
					if (idx >= 0) {
						r.setSimId(-1);
						activeSched.remove(this, sim.id[idx]);
					}
					continue;
				}
				if (idx < 0) {
					// 缺失条目：重建（事件丢失/旧存档迁移后的一次性恢复）
					onResidentAdded(r);
				} else {
					// 既有条目：更新锚点/工作/名字（房屋与工作每日由城镇重分配）
					long previousHome = sim.homePos[idx];
					long nextHome = house != null ? house.asLong() : CitizenSim.NO_HOME_POS;
					boolean persistedChanged = sim.homeX[idx] != anchor.getX() || sim.homeZ[idx] != anchor.getZ()
							|| sim.wx[idx] != (work != null ? work.getX() : -1)
							|| sim.wz[idx] != (work != null ? work.getZ() : -1)
							|| previousHome != nextHome;
					sim.homeX[idx] = anchor.getX();
					sim.homeZ[idx] = anchor.getZ();
					sim.homePos[idx] = nextHome;
					if (previousHome != nextHome) {
						sim.homeSlot[idx] = -1;
					}
					sim.wx[idx] = work != null ? work.getX() : -1;
					sim.wz[idx] = work != null ? work.getZ() : -1;
					r.setSimId(sim.id[idx]);
					nameCache.put(sim.id[idx], r.getFirstName() + " " + r.getLastName());
					syncPresentationAge(r, idx);
					if (persistedChanged)
						markDirty();
				}
			}
			// 幽灵剪除：本容器条目代表的居民已不在城镇（防御非门面直写/事件丢失）。
			// 先收集后删除——swap-remove 会移动尾部元素，边扫边删会跳过元素。
			IntArrayList stale = new IntArrayList();
			for (int i = 0; i < sim.size(); i++) {
				if (sim.uuidHi[i] == 0 && sim.uuidLo[i] == 0)
					continue; // 防御：城镇容器不应有未托管条目
				if (!liveIds.contains(new UUID(sim.uuidHi[i], sim.uuidLo[i])))
					stale.add(sim.id[i]);
			}
			for (int k = 0; k < stale.size(); k++)
				activeSched.remove(this, stale.getInt(k));
		} finally {
			reconcilingResidents = false;
			reconcilingNewResidents = null;
		}
		rebuildAllBedAssignments();
		placePendingResidents(pendingPlacements, Math.floorDiv(level.getDayTime(), 24000L));
	}

	private void syncPresentationAge(Resident resident, int index) {
		if (sim.setPresentationAge(index, resident.getAge()) && activeSched != null)
			activeSched.sync.notifyAppearance(sim.id[index]);
	}

	/**
	 * 玩家镇适配：经门面取居民集合后委托 Collection 版对账。
	 * <p>
	 * Player-town adapter: resolves residents through the facade and delegates
	 * to the collection version.
	 */
	private void syncTownResidents(ServerLevel level, TeamTownData townData) {
		this.activeTown = TeamTown.create(townData);
		syncTownResidents(level, activeTown.getAllResidents());
	}

	private void rebuildAllBedAssignments() {
		for (int i = 0; i < sim.size(); i++)
			sim.homeSlot[i] = -1;
		Long2ObjectOpenHashMap<IntArrayList> byHome = new Long2ObjectOpenHashMap<>();
		for (int i = 0; i < sim.size(); i++) {
			long home = sim.homePos[i];
			if (home != CitizenSim.NO_HOME_POS)
				byHome.computeIfAbsent(home, ignored -> new IntArrayList()).add(i);
		}
		for (var entry : byHome.long2ObjectEntrySet())
			assignHomeSlots(sim, entry.getValue());
		for (int i = 0; i < sim.size(); i++) {
			if (sim.state[i] == CitizenState.SLEEP)
				refreshSleepingResident(i);
		}
	}

	private void rebuildBedAssignments(long home) {
		if (home == CitizenSim.NO_HOME_POS)
			return;
		IntArrayList indices = new IntArrayList();
		for (int i = 0; i < sim.size(); i++) {
			if (sim.homePos[i] == home)
				indices.add(i);
		}
		assignHomeSlots(sim, indices);
		for (int k = 0; k < indices.size(); k++) {
			int index = indices.getInt(k);
			if (sim.state[index] == CitizenState.SLEEP)
				refreshSleepingResident(index);
		}
	}

	/** UUID 排序决定床位槽；session id 与 SoA 当前顺序都不参与结果。 */
	static void assignHomeSlots(CitizenSim data, IntArrayList indices) {
		for (int k = 0; k < indices.size(); k++)
			data.homeSlot[indices.getInt(k)] = -1;
		if (indices.isEmpty())
			return;
		indices.sort((left, right) -> {
			int c = Long.compare(data.uuidHi[left], data.uuidHi[right]);
			if (c == 0)
				c = Long.compare(data.uuidLo[left], data.uuidLo[right]);
			if (c == 0)
				c = Integer.compare(data.id[left], data.id[right]);
			return c;
		});
		for (int slot = 0; slot < indices.size(); slot++)
			data.homeSlot[indices.getInt(slot)] = slot;
	}

	private HouseBuilding resolveHouse(long home) {
		if (activeTown == null || home == CitizenSim.NO_HOME_POS)
			return null;
		AbstractTownBuilding building = activeTown.getTownBuilding(BlockPos.of(home)).orElse(null);
		return building instanceof HouseBuilding house && house.isStructureValid() ? house : null;
	}

	private long resolveEntrance(long home, long fallback) {
		HouseBuilding house = resolveHouse(home);
		return house != null && house.hasEntrance() ? house.getEntrancePositionLong() : fallback;
	}

	private long resolveBedPosition(int index) {
		HouseBuilding house = resolveHouse(sim.homePos[index]);
		if (house == null)
			return CitizenSim.NO_HOME_POS;
		int slot = sim.homeSlot[index];
		return slot >= 0 && slot < house.getBedCount()
				? house.getBedPositionLong(slot)
				: CitizenSim.NO_HOME_POS;
	}

	private boolean isUsableBed(long packedPos) {
		if (packedPos == CitizenSim.NO_HOME_POS || activeLevel == null)
			return false;
		BlockPos pos = BlockPos.of(packedPos);
		if (!activeLevel.hasChunkAt(pos))
			return false;
		BlockState state = activeLevel.getBlockState(pos);
		return state.getBlock() instanceof BedBlock
				&& state.hasProperty(BedBlock.PART)
				&& state.getValue(BedBlock.PART) == BedPart.HEAD;
	}

	private static long homeExitSeed(long home, long uuidHi, long uuidLo, long worldDay) {
		long identity = home != CitizenSim.NO_HOME_POS ? home : uuidHi ^ Long.rotateLeft(uuidLo, 17);
		long seed = identity ^ worldDay * 0x9E3779B97F4A7C15L;
		seed ^= seed >>> 33;
		seed *= 0xFF51AFD7ED558CCDL;
		seed ^= seed >>> 33;
		return seed;
	}

	private void prepareSleepingState(int index) {
		sim.state[index] = CitizenState.SLEEP;
		sim.presentationFlags[index] &= ~CitizenSim.PRESENT_ON_VALID_BED;
		sim.halt[index] = 0;
		sim.sepX[index] = 0;
		sim.sepZ[index] = 0;
		sim.tx[index] = sim.px[index];
		sim.tz[index] = sim.pz[index];
		sim.stuckTick[index] = 0;
		sim.bestDist2[index] = 0;
	}

	private void refreshSleepingResident(int index) {
		onSleepEntered(index);
		sim.tx[index] = sim.px[index];
		sim.tz[index] = sim.pz[index];
		sim.sepX[index] = 0;
		sim.sepZ[index] = 0;
		sim.stuckTick[index] = 0;
		sim.bestDist2[index] = 0;
	}

	private void placePendingResidents(IntArrayList citizenIds, long worldDay) {
		citizenIds.sort((leftId, rightId) -> {
			int left = sim.indexOf(leftId);
			int right = sim.indexOf(rightId);
			if (left < 0 || right < 0)
				return Integer.compare(left, right);
			int comparison = Long.compare(sim.homePos[left], sim.homePos[right]);
			if (comparison == 0)
				comparison = Integer.compare(sim.homeSlot[left], sim.homeSlot[right]);
			return comparison != 0 ? comparison : Integer.compare(leftId, rightId);
		});
		for (int k = 0; k < citizenIds.size(); k++) {
			int index = sim.indexOf(citizenIds.getInt(k));
			if (index >= 0 && sim.state[index] != CitizenState.SLEEP)
				placeAtHomeExit(index, worldDay);
		}
	}

	private void placeAtHomeExit(int index, long worldDay) {
		BlockPos anchor = BlockPos.of(homeEntrancePosition(index));
		long seed = homeExitSeed(sim.homePos[index], sim.uuidHi[index], sim.uuidLo[index], worldDay);
		BlockPos exit = CitizenSimScheduler.groundNear(activeLevel, anchor, sim.homeSlot[index], seed,
				packedXZ -> isExitOccupied(index, packedXZ));
		sim.px[index] = (exit.getX() << 10) + 512;
		sim.py[index] = exit.getY() << 10;
		sim.pz[index] = (exit.getZ() << 10) + 512;
		sim.tx[index] = sim.px[index];
		sim.tz[index] = sim.pz[index];
		sim.sepX[index] = 0;
		sim.sepZ[index] = 0;
		sim.halt[index] = 0;
		sim.stuckTick[index] = 0;
		sim.bestDist2[index] = Long.MAX_VALUE;
	}

	private boolean isExitOccupied(int selfIndex, long packedXZ) {
		int blockX = BlockPos.getX(packedXZ);
		int blockZ = BlockPos.getZ(packedXZ);
		long home = sim.homePos[selfIndex];
		for (int i = 0; i < sim.size(); i++) {
			if (i != selfIndex && sim.homePos[i] == home && CitizenPresence.spatialPresent(sim.state[i])
					&& (sim.px[i] >> 10) == blockX && (sim.pz[i] >> 10) == blockZ)
				return true;
		}
		return false;
	}

	/* ===================== CitizenContainer ===================== */

	@Override
	public CitizenSim sim() {
		return sim;
	}

	@Override
	public long homeEntrancePosition(int index) {
		long fallback = sim.homePos[index] != CitizenSim.NO_HOME_POS
				? sim.homePos[index]
				: BlockPos.asLong(sim.homeX[index], sim.py[index] >> 10, sim.homeZ[index]);
		return resolveEntrance(sim.homePos[index], fallback);
	}

	@Override
	public void onSleepEntered(int index) {
		long bedPosition = resolveBedPosition(index);
		if (isUsableBed(bedPosition)) {
			BlockPos bed = BlockPos.of(bedPosition);
			sim.px[index] = (bed.getX() << 10) + 512;
			sim.py[index] = bed.getY() << 10;
			sim.pz[index] = (bed.getZ() << 10) + 512;
			BlockState state = activeLevel.getBlockState(bed);
			var facing = state.getValue(BedBlock.FACING);
			sim.dir[index] = (byte) CitizenState.dirFromVector(facing.getStepX(), facing.getStepZ());
			sim.presentationFlags[index] |= CitizenSim.PRESENT_ON_VALID_BED;
			if (activeSched != null)
				activeSched.sync.notifyImmediate(sim.id[index]);
		} else {
			sim.presentationFlags[index] &= ~CitizenSim.PRESENT_ON_VALID_BED;
			BlockPos indoorFallback = BlockPos.of(homeEntrancePosition(index));
			sim.px[index] = (indoorFallback.getX() << 10) + 512;
			sim.py[index] = indoorFallback.getY() << 10;
			sim.pz[index] = (indoorFallback.getZ() << 10) + 512;
			if (activeSched != null)
				activeSched.sync.notifyHidden(sim.id[index]);
		}
	}

	@Override
	public void onWake(ServerLevel level, int index, long worldDay) {
		sim.presentationFlags[index] &= ~CitizenSim.PRESENT_ON_VALID_BED;
		placeAtHomeExit(index, worldDay);
		if (activeSched != null)
			activeSched.sync.notifyImmediate(sim.id[index]);
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
}
