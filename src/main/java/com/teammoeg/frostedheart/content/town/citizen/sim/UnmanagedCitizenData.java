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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 未托管居民容器（per-level {@link SavedData}）：命令生成（{@code /fhcitizen
 * spawn}）的野居民 + 全局稳定 id 分配器。
 * 复用旧 {@code CitizenSimManager} 的数据文件名 {@value #DATA_NAME}——删除旧
 * Manager 与新建本类必须同一编译单元完成，否则两个 SavedData 争用同一文件。
 * load 对旧格式（顶层 {@code sim} 键）自动迁移：uuid=0 的命令居民保留 id 与
 * 位置，uuid≠0 的镇居民条目丢弃（重启后由各镇接管时 rebind 重建），nextId 取
 * 保存值与现存最大 id 的较大者（永不复用 id）。
 * <p>
 * Unmanaged citizen container (per-level {@link SavedData}): command-spawned
 * citizens plus the global stable-id allocator. Reuses the old
 * {@code CitizenSimManager} data file name — deleting the old manager and
 * creating this class must land in the same compile unit. Loading migrates the
 * old format automatically: uuid=0 command citizens keep id and position,
 * uuid≠0 town residents are dropped (rebuilt by each town's takeover rebind),
 * and nextId takes the max of the saved value and the largest live id (ids are
 * never reused).
 */
public final class UnmanagedCitizenData extends SavedData implements CitizenContainer {

	private static final String DATA_NAME = "fh_citizen_sim";

	private final CitizenSim sim = new CitizenSim(64);
	/** FH 交易数据（命令居民可交易）/ FH trade data (command citizens can trade) */
	private final Int2ObjectOpenHashMap<FHVillagerData> tradeData = new Int2ObjectOpenHashMap<>();
	/**
	 * 显示名缓存。未托管居民无城镇身份 → 恒空；同步层回退 id 派生名，
	 * 招募拦截判据（nameCache 有条目 = 城镇托管）对此容器恒 false。
	 * <p>
	 * Display-name cache. Unmanaged citizens have no town identity — always
	 * empty; the sync layer falls back to the id-derived name, and the
	 * recruit-interception test (entry present = town-backed) is always false
	 * for this container.
	 */
	private final Int2ObjectOpenHashMap<String> nameCache = new Int2ObjectOpenHashMap<>();
	private int nextId = 1;

	private UnmanagedCitizenData() {
	}

	/**
	 * 获取指定维度的未托管容器（懒创建；首次访问触发旧格式迁移）。
	 * <p>
	 * Gets the unmanaged container for the level (lazily created; the first
	 * access triggers the old-format migration).
	 *
	 * @param level 服务端维度 / server level
	 * @return 容器 / the container
	 */
	public static UnmanagedCitizenData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(UnmanagedCitizenData::load, UnmanagedCitizenData::new, DATA_NAME);
	}

	private static UnmanagedCitizenData load(CompoundTag tag) {
		UnmanagedCitizenData data = new UnmanagedCitizenData();
		CompoundTag unmanaged = tag.getCompound("unmanaged");
		if (!unmanaged.isEmpty()) {
			// 新格式（本类写出）
			data.nextId = unmanaged.getInt("nextId");
			data.mergeSim(unmanaged.getCompound("sim"));
		} else {
			// 旧格式（CitizenSimManager 时代）：顶层 nextId + sim
			data.nextId = tag.getInt("nextId");
			CitizenSim loaded = CitizenSim.load(tag.getCompound("sim"));
			for (int k = 0; k < loaded.size(); k++) {
				if (loaded.uuidHi[k] == 0 && loaded.uuidLo[k] == 0) {
					// 命令居民：保留 id 与位置
					int i = data.sim.add(loaded.id[k], loaded.px[k], loaded.py[k], loaded.pz[k], loaded.tickPhase[k]);
					data.sim.yaw[i] = loaded.yaw[k];
                    data.sim.syaw[i] = loaded.syaw[k];
					data.sim.state[i] = loaded.state[k];
					data.sim.homeX[i] = loaded.homeX[k];
					data.sim.homeZ[i] = loaded.homeZ[k];
					data.sim.tx[i] = loaded.tx[k];
					data.sim.ty[i] = loaded.ty[k];
					data.sim.tz[i] = loaded.tz[k];
				}
				// uuid≠0 的镇居民条目丢弃：重启后由各镇接管时 rebind 重建
				// (town residents dropped: rebuilt by each town's takeover rebind)
			}
			data.ensureNextIdAfter(data.maxId());
			data.setDirty(); // 立即回写新格式 / rewrite in the new format
			FHMain.LOGGER.info("Migrated citizen sim save to unmanaged format: {} unmanaged entries", data.sim.size());
		}
		data.ensureNextIdAfter(data.maxId()); // 保底：现存 id 永不复用
		return data;
	}

	/**
	 * 当前存活条目中的最大稳定 id；空容器返回 0。
	 * <p>
	 * Largest live stable id; 0 for an empty container.
	 *
	 * @return 最大 id / max id
	 */
	private int maxId() {
		int max = 0;
		for (int i = 0; i < sim.size(); i++) {
			if (sim.id[i] > max)
				max = sim.id[i];
		}
		return max;
	}

	/**
	 * 确保下一个 id 严格大于给定的已用 id。调度器首次接管时会用本维度所有
	 * town/AI/未托管条目的最大 id 校准一次，以修复跨 SavedData 文件在异常中断
	 * 后可能出现的分配器落后。
	 * <p>
	 * Ensures the next allocated id is strictly above a known used id. On first
	 * takeover the scheduler calibrates this against every town, AI-town and
	 * unmanaged entry in the level, repairing an allocator lag caused by a
	 * partially completed cross-SavedData save.
	 *
	 * @param usedId 已使用的最大 id / largest known used id
	 */
	void ensureNextIdAfter(int usedId) {
		if (usedId < nextId)
			return;
		if (usedId == Integer.MAX_VALUE)
			throw new IllegalStateException("Citizen id space exhausted");
		nextId = Math.max(1, usedId + 1);
		setDirty();
	}

	/**
	 * 逐条合并到 final sim（含运行期字段）。
	 * <p>
	 * Merges entries into the final sim (runtime fields included).
	 *
	 * @param simTag 源标签 / source tag
	 */
	private void mergeSim(CompoundTag simTag) {
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

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag unmanaged = new CompoundTag();
		unmanaged.putInt("nextId", nextId);
		unmanaged.put("sim", sim.save(new CompoundTag()));
		tag.put("unmanaged", unmanaged);
		return tag;
	}

	/**
	 * 分配全局唯一稳定 id（自增，永不复用，随容器持久化）。
	 * <p>
	 * Allocates a globally unique stable id (auto-increment, never reused,
	 * persisted with this container).
	 *
	 * @return 新 id / the new id
	 */
	public int allocId() {
		if (nextId <= 0 || nextId == Integer.MAX_VALUE)
			throw new IllegalStateException("Citizen id space exhausted or corrupt: " + nextId);
		int id = nextId++;
		setDirty();
		return id;
	}

	/**
	 * 生成一个命令居民（Y 自动贴合高度图；带地面高度提示时取提示与列顶的较小者）。
	 * <p>
	 * Spawns a command citizen (Y conforms to the heightmap; a ground-height
	 * hint below the column top wins — anchors are often under roofs).
	 *
	 * @param level 维度 / the level
	 * @param blockX 方块 X / block X
	 * @param blockZ 方块 Z / block Z
	 * @param blockY 地面高度提示（方块单位）；&lt;= 0 忽略 / ground-height hint; &lt;= 0 ignores it
	 * @return 新居民的稳定 id / stable id of the new citizen
	 */
	public int spawn(ServerLevel level, int blockX, int blockZ, int blockY) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
		if (blockY > 0 && blockY < y)
			y = blockY;
		int id = allocId();
		sim.add(id, (blockX << 10) + 512, y << 10, (blockZ << 10) + 512, (byte) (id % BehaviorSystem.SLICE));
		return id;
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

	@Override
	public void markDirty() {
		setDirty();
	}
}
