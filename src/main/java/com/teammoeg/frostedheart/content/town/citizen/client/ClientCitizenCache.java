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

package com.teammoeg.frostedheart.content.town.citizen.client;

import java.util.Collection;
import java.util.List;

import com.teammoeg.frostedheart.content.town.citizen.sync.S2CCitizenBatchPacket;
import com.teammoeg.frostedheart.content.town.citizen.sync.S2CCitizenSpawnPacket;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端居民缓存：id → 渲染状态。
 * 接收三类同步包并维护本地世界快照；同时提供准星射线选取（交互用）。
 * 仅存在于逻辑客户端，退出世界时清空。
 * <p>
 * Client-side citizen cache: id → render state. Consumes the three sync
 * packet types and maintains the local world snapshot; also provides
 * crosshair ray picking (for interaction). Logical client only; cleared on
 * world exit.
 */
public final class ClientCitizenCache {

	private static final Int2ObjectOpenHashMap<ClientCitizen> CITIZENS = new Int2ObjectOpenHashMap<>();

	private ClientCitizenCache() {
	}

	/**
	 * 应用出生包。
	 * <p>
	 * Applies a spawn packet.
	 *
	 * @param entries 出生条目 / spawn entries
	 */
	public static void applySpawn(List<S2CCitizenSpawnPacket.Entry> entries) {
		for (S2CCitizenSpawnPacket.Entry e : entries)
			CITIZENS.put(e.id(), new ClientCitizen(e.id(), e.px(), e.py(), e.pz(), e.yaw(), e.state(), e.name()));
	}

	/**
	 * 应用移动增量批包（chunk 相对坐标还原为绝对定点坐标）。
	 * <p>
	 * Applies a movement delta batch (restores chunk-relative coords to absolute fixed-point).
	 *
	 * @param groups chunk 分组 / chunk groups
	 */
    public static void applyBatch(List<S2CCitizenBatchPacket.Group> groups) {
        for (S2CCitizenBatchPacket.Group g : groups) {
            int baseX = g.cx() << 14;
            int baseZ = g.cz() << 14;
            for (S2CCitizenBatchPacket.Entry e : g.entries()) {
                ClientCitizen c = CITIZENS.get(e.id());
                if (c == null)
                    continue;
                int px = baseX + e.lx() * S2CCitizenBatchPacket.LOCAL_QUANT;
                int py = e.ly() << 6;
                int pz = baseZ + e.lz() * S2CCitizenBatchPacket.LOCAL_QUANT;
                byte yaw = e.yaw();
                byte state = e.state();
                if ((state & S2CCitizenBatchPacket.ENTRY_PURE_HEARTBEAT) != 0) {
                    // 纯心跳：沿用客户端现有的 yaw/state（服务端保证未变）
                    yaw = c.yaw;
                    state = c.state;
                }
                c.update(px, py, pz, yaw, state);
            }
        }
    }

	/**
	 * 应用销毁包。
	 * <p>
	 * Applies a despawn packet.
	 *
	 * @param ids 居民 id 列表 / citizen id list
	 */
	public static void applyDespawn(IntList ids) {
		for (int i = 0; i < ids.size(); i++)
			CITIZENS.remove(ids.getInt(i));
	}

	/**
	 * 按稳定 id 获取客户端居民，不存在返回 null。
	 * <p>
	 * Gets a cached citizen by stable id, or null if absent.
	 *
	 * @param id 居民 id / citizen id
	 * @return 居民实例或 null / the citizen or null
	 */
	public static ClientCitizen get(int id) {
		return CITIZENS.get(id);
	}

	/**
	 * 清空缓存（退出世界时调用）。
	 * <p>
	 * Clears the cache (called on world exit).
	 */
	public static void clear() {
		CITIZENS.clear();
	}

	/**
	 * 当前缓存的居民数量。
	 * <p>
	 * Current cached citizen count.
	 *
	 * @return 数量 / count
	 */
	public static int size() {
		return CITIZENS.size();
	}

	/**
	 * 全部居民渲染状态（渲染器遍历用）。
	 * <p>
	 * All citizen render states (for the renderer to iterate).
	 *
	 * @return 居民集合 / citizen collection
	 */
	public static Collection<ClientCitizen> values() {
		return CITIZENS.values();
	}

	/**
	 * 准星射线选取：找视线上的居民（垂直圆柱近似，半径 0.5 格）。
	 * <p>
	 * Crosshair ray picking: finds the citizen under the crosshair
	 * (vertical-cylinder approximation, radius 0.5 blocks).
	 *
	 * @param cam 当前相机 / current camera
	 * @param maxDist 最大距离 / max distance
	 * @return 命中的居民 id，未命中返回 -1 / hit citizen id, or -1 if none
	 */
	public static int pick(Camera cam, double maxDist) {
		Vec3 eye = cam.getPosition();
		Vector3f look = cam.getLookVector();
		int bestId = -1;
		double bestT = maxDist;
		for (ClientCitizen c : CITIZENS.values()) {
			double[] pos = c.renderPos();
			double cx = pos[0] - eye.x;
			double cy = pos[1] + 0.9 - eye.y;
			double cz = pos[2] - eye.z;
			double t = cx * look.x + cy * look.y + cz * look.z;
			if (t < 0 || t > bestT)
				continue;
			double closestX = eye.x + look.x * t;
			double closestY = eye.y + look.y * t;
			double closestZ = eye.z + look.z * t;
			double dx = closestX - pos[0];
			double dz = closestZ - pos[2];
			if (dx * dx + dz * dz > 0.25)
				continue;
			if (closestY < pos[1] - 0.2 || closestY > pos[1] + 2.0)
				continue;
			bestId = c.id;
			bestT = t;
		}
		return bestId;
	}
}
