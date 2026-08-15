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

package com.teammoeg.frostedheart.content.town.ai_town;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * AI 城镇注册表 + 全局模拟存储（overworld SavedData {@value #DATA_NAME}）：
 * <ul>
 *   <li>AI 镇列表（{@link AITownData}，自包含居民+模拟，独立 Town 无队伍）；</li>
 *   <li>玩家镇模拟表（key = 队伍 holder id——TownSimData 与队伍零关联，玩家镇与
 *       AI 镇的模拟统一存全局单文件）；</li>
 *   <li>lastAIId（/fhcitizen ai_add_resident 定位最近创建的 AI 镇）。</li>
 * </ul>
 * 落盘遵循标准 SavedData 语义：任何持久字段变化（含移动位置/状态/目标）经
 * {@link #markDirty()}（setDirty）标记，Minecraft 内建 6000t 自动保存 +
 * 停服保存负责写盘——无自定义存盘调度器或逐 tick 磁盘 I/O。
 * <p>
 * AI town registry + global simulation store (overworld SavedData
 * {@value #DATA_NAME}): AI towns (self-contained, no team) and the player-town
 * simulation table (keyed by team holder id — the simulation is decoupled from
 * teams entirely). Persistence follows standard SavedData semantics: all
 * persisted-field changes mark dirty via {@link #markDirty()}; Minecraft's
 * built-in 6000-tick autosave and the server-stop save handle the actual
 * writes — no custom save scheduler or per-tick disk I/O.
 */
public final class AITownManager {

	private static final String DATA_NAME = "fh_ai_towns";

	/** 全局文件载体（懒加载；服务器停止时清空）/ The save file (lazily loaded; cleared on server stop) */
	private static AITownSaveData saveData;
	/** AI 镇列表（重启自文件恢复）/ AI towns (restored from the file on load) */
	private static final Map<UUID, AITownData> byId = new LinkedHashMap<>();
	/** 最近创建的 AI 镇 id（/fhcitizen ai_add_resident 定位）/ id of the most recently created AI town */
	private static UUID lastAIId;
	/** 玩家镇模拟表（key = 队伍 holder id）/ Player-town simulations (keyed by team holder id) */
	private static final Map<UUID, TownSimData> playerSims = new LinkedHashMap<>();

	private AITownManager() {
	}

	/**
	 * 获取或创建指定名字的 AI 镇（同名幂等，返回已存在实例）。
	 * 新镇落盘在玩家的当前维度（调度器门控依据）。
	 * <p>
	 * Gets or creates the AI town with the given name (same-name idempotent).
	 * A new town is pinned to the caller's current dimension (the scheduler gate).
	 *
	 * @param name 镇名 / town name
	 * @param dimension 维度 / the dimension
	 * @return 镇数据 / the town
	 */
	public static AITownData getOrCreate(String name, ResourceKey<Level> dimension) {
		init();
		for (AITownData town : byId.values())
			if (town.getName().equals(name))
				return town;
		AITownData town = new AITownData(UUID.randomUUID(), name, dimension);
		byId.put(town.getId(), town);
		lastAIId = town.getId();
		markDirty();
		return town;
	}

	/**
	 * 最近创建的 AI 镇；不存在返回 null。
	 * <p>
	 * The most recently created AI town; null if none.
	 *
	 * @return 镇数据或 null / the town, or null
	 */
	public static AITownData lastAI() {
		init();
		return lastAIId != null ? byId.get(lastAIId) : null;
	}

	/**
	 * 全部 AI 镇（调度器聚合遍历用）。
	 * <p>
	 * All AI towns (for the scheduler's registry aggregation).
	 *
	 * @return 镇集合 / town collection
	 */
	public static Collection<AITownData> all() {
		init();
		return byId.values();
	}

	/**
	 * 玩家镇模拟：按队伍 holder id 取（懒创建——与队伍零关联的纯模拟数据，
	 * 统一存全局单文件，不随 chorda_data 落盘）。
	 * <p>
	 * Player-town simulation by team holder id (lazily created — pure
	 * simulation data decoupled from the team, stored in the global file,
	 * not in the chorda team save).
	 *
	 * @param holderId 队伍数据持有者 id / the team data holder id
	 * @return 模拟数据 / the simulation data
	 */
	public static TownSimData getPlayerSim(UUID holderId) {
		init();
		return playerSims.computeIfAbsent(holderId, k -> new TownSimData());
	}

	/**
	 * 标记全局文件 dirty（结构变更后调用）：不立即写盘，由 Minecraft 内建
	 * 自动保存/停服保存落盘并清标记（标准 SavedData 语义）。
	 * <p>
	 * Marks the global save dirty (call after structural changes): no immediate
	 * write — Minecraft's built-in autosave / server stop persist it (standard
	 * SavedData semantics).
	 */
	public static void markDirty() {
		if (saveData != null)
			saveData.setDirty();
	}

	/**
	 * 清空运行期状态（服务器停止时调用，防跨世界残留）。
	 * <p>
	 * Clears the runtime state (on server stop, preventing cross-world residue).
	 */
	public static void reset() {
		saveData = null;
		byId.clear();
		lastAIId = null;
		playerSims.clear();
	}

	/** 懒加载全局文件（首次调用时；overworld 恒先于其他维度加载） */
	private static void init() {
		if (saveData == null) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.overworld();
				saveData = overworld.getDataStorage().computeIfAbsent(AITownSaveData::load, AITownSaveData::new,
						DATA_NAME);
			}
		}
	}

	/** 全局单文件载体：序列化当前静态状态（AI 镇列表 + 玩家镇模拟表 + lastAIId） */
	private static class AITownSaveData extends SavedData {

		@Override
		public CompoundTag save(CompoundTag tag) {
			CompoundTag townsTag = new CompoundTag();
			for (AITownData town : byId.values())
				townsTag.put(town.getId().toString(), town.save());
			tag.put("towns", townsTag);
			CompoundTag simsTag = new CompoundTag();
			for (Map.Entry<UUID, TownSimData> e : playerSims.entrySet())
				simsTag.put(e.getKey().toString(), TownSimData.toNbt(e.getValue()));
			tag.put("playerSims", simsTag);
			if (lastAIId != null)
				tag.putUUID("lastAIId", lastAIId);
			return tag;
		}

		private static AITownSaveData load(CompoundTag tag) {
			// 逐条 try/catch total 化：单条坏数据绝不拒绝整个文件
			CompoundTag townsTag = tag.getCompound("towns");
			for (String key : townsTag.getAllKeys()) {
				try {
					AITownData town = AITownData.fromNbt(townsTag.getCompound(key));
					byId.put(town.getId(), town);
				} catch (Throwable t) {
					FHMain.LOGGER.error("AITownManager: failed to decode AI town {}: {}", key, t.toString());
				}
			}
			CompoundTag simsTag = tag.getCompound("playerSims");
			for (String key : simsTag.getAllKeys()) {
				try {
					TownSimData sim = new TownSimData();
					sim.loadFromNbt(simsTag.getCompound(key));
					playerSims.put(UUID.fromString(key), sim);
				} catch (Throwable t) {
					FHMain.LOGGER.error("AITownManager: failed to decode player sim {}: {}", key, t.toString());
				}
			}
			if (tag.contains("lastAIId")) {
				try {
					UUID decodedLast = tag.getUUID("lastAIId");
					lastAIId = byId.containsKey(decodedLast) ? decodedLast : null;
				} catch (RuntimeException ex) {
					FHMain.LOGGER.error("AITownManager: failed to decode lastAIId: {}", ex.toString());
					lastAIId = null;
				}
			}
			return new AITownSaveData();
		}
	}
}
