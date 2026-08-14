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
import java.util.Optional;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * AI 城镇数据（独立 Town，无队伍）：“AI城镇自己就是一个town”——不伪造
 * {@code AbstractTeam}，不走队伍存储；由 {@link AITownManager} 的全局单文件
 * （overworld SavedData "fh_ai_towns"）持久化，重启不丢。
 * 不参与每日结算（tickMorning/tickSecond 跳过——调试镇稳定不演化），居民
 * 增删直调 simData 事件回调 → 模拟立即出生/移除条目（与玩家镇事件驱动同构）。
 * 实现 {@link ITownWithResidents}：概念上是 Town 的居民能力面。
 * <p>
 * AI town data: an independent town with no team behind it. Not a fake
 * {@code AbstractTeam} — persisted by {@link AITownManager}'s global
 * single-file save (overworld SavedData "fh_ai_towns"), survives restarts.
 * Skips the daily settlement (stable debug town). Resident add/remove calls
 * the sim event callbacks directly → the simulation spawns/removes entries
 * immediately (same event-driven shape as player towns). Implements
 * {@link ITownWithResidents}: the resident capability face of a Town.
 */
public class AITownData implements ITownWithResidents {

	/** 稳定标识（落盘；byId 键）/ Stable id (persisted; the byId key) */
	private final UUID id;
	/** 镇名（落盘）/ Town name (persisted) */
	private String name;
	/** 维度（落盘）——镇唯一的维度概念，调度器门控依据 / Dimension (persisted) — a town's only dimension concept, the scheduler gate */
	private ResourceKey<Level> dimension;
	/** 居民表（落盘；uuid → resident）/ Residents (persisted; uuid → resident) */
	private final Map<UUID, Resident> residents = new LinkedHashMap<>();
	/** 居民模拟（共用类，随本数据落盘）/ Resident simulation (shared class, persisted with this data) */
	private final TownSimData simData = new TownSimData();

	public AITownData(UUID id, String name, ResourceKey<Level> dimension) {
		this.id = id;
		this.name = name;
		this.dimension = dimension;
	}

	/* ===================== NBT 序列化（total 化） ===================== */

	/**
	 * 从 NBT 恢复（AITownManager load 调用；坏数据逐条防御，绝不向外抛）。
	 * <p>
	 * Restores from NBT (called by AITownManager's load; bad entries are
	 * absorbed defensively, never thrown out).
	 *
	 * @param tag 源标签 / source tag
	 * @return 镇数据 / the town
	 */
	public static AITownData fromNbt(CompoundTag tag) {
		ResourceLocation dimLoc = ResourceLocation.tryParse(tag.getString("dimension"));
		AITownData town = new AITownData(
				UUID.fromString(tag.getString("id")),
				tag.getString("name"),
				dimLoc != null ? ResourceKey.create(Registries.DIMENSION, dimLoc) : Level.OVERWORLD);
		CompoundTag residentsTag = tag.getCompound("residents");
		for (String key : residentsTag.getAllKeys()) {
			try {
				town.residents.put(UUID.fromString(key), new Resident(residentsTag.getCompound(key)));
			} catch (Throwable t) {
				FHMain.LOGGER.error("AITownData: failed to decode resident {} in town {}: {}", key, town.name, t.toString());
			}
		}
		try {
			town.simData.loadFromNbt(tag.getCompound("sim"));
		} catch (Throwable t) {
			FHMain.LOGGER.error("AITownData: failed to decode sim of town {}: {}", town.name, t.toString());
		}
		return town;
	}

	/**
	 * 序列化（AITownManager save 调用）。
	 * <p>
	 * Serializes (called by AITownManager's save).
	 *
	 * @return 标签 / the tag
	 */
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", id.toString());
		tag.putString("name", name);
		tag.putString("dimension", dimension.location().toString());
		CompoundTag residentsTag = new CompoundTag();
		residents.forEach((uuid, resident) -> residentsTag.put(uuid.toString(), resident.serialize()));
		tag.put("residents", residentsTag);
		tag.put("sim", TownSimData.toNbt(simData));
		return tag;
	}

	/* ===================== 访问器 ===================== */

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ResourceKey<Level> dimension() {
		return dimension;
	}

	/**
	 * 维度门控：该镇是否属于此维度（调度器聚合依据，与玩家镇 GENERATOR_DATA
	 * 门控语义一致——AI 镇没有能量塔，维度直接落盘在本数据上）。
	 * <p>
	 * Dimension gate: whether this town belongs to the level (the scheduler
	 * aggregation gate; same semantics as the player-town generator gate —
	 * AI towns have no generator, the dimension is stored directly on this data).
	 *
	 * @param level 当前维度 / the current level
	 * @return 属于返回 true / true if this town is in the level
	 */
	public boolean isInLevel(ServerLevel level) {
		return level.dimension().equals(dimension);
	}

	public TownSimData getSimData() {
		return simData;
	}

	/* ===================== ITownWithResidents ===================== */

	@Override
	public Collection<Resident> getAllResidents() {
		return residents.values();
	}

	@Override
	public Optional<Resident> getResident(UUID id) {
		return Optional.ofNullable(residents.get(id));
	}

	/**
	 * 加入居民：直写居民表 + 事件驱动模拟出生（锚点由调用方预置，与
	 * {@code TeamTown.debugAddResident} 语义一致）+ 结构变更 dirty 标记
	 * （随 Minecraft 自动保存/停服落盘）。
	 * <p>
	 * Adds a resident: direct map write + event-driven simulation spawn (the
	 * anchor is preset by the caller, same semantics as
	 * {@code TeamTown.debugAddResident}) + structural dirty mark (persisted by
	 * Minecraft's autosave / server stop).
	 *
	 * @param resident 加入的居民 / the resident to add
	 * @return 恒 true（AI 镇无房屋容量概念）/ always true (no house-capacity concept)
	 */
	@Override
	public boolean addResident(Resident resident) {
		residents.put(resident.getUUID(), resident);
		simData.onResidentAdded(resident);
		AITownManager.markDirty();
		return true;
	}

	/**
	 * 移除居民：集合移除 + 事件驱动模拟 despawn + 结构变更 dirty 标记。
	 * <p>
	 * Removes a resident: collection removal + event-driven simulation despawn
	 * + structural dirty mark.
	 *
	 * @param uuid 移除的居民 uuid / uuid of the resident to remove
	 * @return 存在并移除返回 true / true if the resident existed and was removed
	 */
	@Override
	public boolean removeResident(UUID uuid) {
		Resident resident = residents.remove(uuid);
		if (resident == null)
			return false;
		simData.onResidentRemoved(resident);
		AITownManager.markDirty();
		return true;
	}
}
