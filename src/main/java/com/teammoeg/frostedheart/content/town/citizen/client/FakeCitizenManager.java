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

import java.util.Iterator;
import java.util.Map;

import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.content.town.citizen.FakeCitizenEntity;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * 假实体生命周期管理器：把客户端缓存中近距居民映射为客户端本地假实体。
 * 进入 24 格生成、超出 28 格移除（迟滞带防抖动）；使用负数实体 id 段，
 * 与服务端实体 id 空间完全隔离。每客户端 tick 将模拟位置/朝向直接写入
 * 实体及其"上一帧"字段，让原版渲染插值自然平滑；行走摆动由实际位移驱动。
 * 与批量渲染器配合：凡由假实体接管的居民，ClientCitizenRenderer 一律跳过。
 * <p>
 * Fake entity lifecycle manager: maps near-range cached citizens to
 * client-local fake entities. Spawn at 24 blocks, remove beyond 28
 * (hysteresis band against flickering); uses a negative entity id space,
 * fully isolated from server ids. Every client tick the simulated
 * position/yaw is written into the entity and its "previous frame" fields
 * so vanilla render interpolation stays smooth; walk swing is driven by
 * actual displacement. Cooperates with the batch renderer: citizens owned
 * by fake entities are always skipped by ClientCitizenRenderer.
 */
public final class FakeCitizenManager {

	/** 生成距离平方（24 格） / Spawn distance squared (24 blocks) */
	private static final double ENTER_DIST2 = 24.0 * 24.0;
	/** 移除距离平方（28 格，迟滞带） / Removal distance squared (28 blocks, hysteresis band) */
	private static final double EXIT_DIST2 = 28.0 * 28.0;
	/** 假实体表：居民 id → 假实体 / Fake entity table: citizen id → fake entity */
	private static final Int2ObjectOpenHashMap<FakeCitizenEntity> ACTIVE = new Int2ObjectOpenHashMap<>();
	/**
	 * 单 tick（1/20 秒）内假实体最大朝向变化角（度）。
	 * 原版村民视觉上转向本就偏慢，保守取 360°/s（即每 tick ≤18°）；
	 * 低于 16 向最小步进 22.5°，足以在约 1.3 tick 内响应方向变化，
	 * 但能 100% 遏制服务端 dir 抖动或 ±180° 翻转导致的"疯狂旋转/抽搐"。
	 * <p>
	 * Max yaw delta per client tick (degrees). Conservative 360°/s (≤18°/tick);
	 * below the 16-way min step 22.5° so it responds to real turns in ~1.3 ticks
	 * but completely suppresses server-side dir jitter / ±180° flips.
	 */
	private static final float MAX_YAW_PER_TICK = 18.0f;

	private FakeCitizenManager() {
	}

	/**
	 * 每客户端 tick 驱动一次（END 阶段，渲染帧之前）。
	 * <p>
	 * Driven once per client tick (END phase, before render frames).
	 *
	 * @param mc Minecraft 实例 / the Minecraft instance
	 */
	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null || mc.player == null)
			return;
		double px = mc.player.getX();
		double pz = mc.player.getZ();

		// 第一遍：驱动已活跃的假实体；缓存消失 / 实体失效 / 超距则移除
		Iterator<Map.Entry<Integer, FakeCitizenEntity>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, FakeCitizenEntity> e = it.next();
			FakeCitizenEntity ent = e.getValue();
			ClientCitizen c = ClientCitizenCache.get(e.getKey());
			boolean drop = c == null || ent.isRemoved() || ent.level() != level;
			if (!drop) {
				double[] pos = c.renderPos();
				double dx = pos[0] - px;
				double dz = pos[2] - pz;
				if (dx * dx + dz * dz > EXIT_DIST2)
					drop = true;
				else
					drive(ent, c, pos);
			}
			if (drop) {
				// 走原版 despawn 路径：标记移除 + onClientRemoval，tickEntities 负责清理查找表
				// Follow the vanilla despawn path so the entity lookup is purged by tickEntities
				level.removeEntity(ent.getId(), Entity.RemovalReason.DISCARDED);
				it.remove();
			}
		}

		// 第二遍：为进入近距范围的缓存居民生成假实体
		for (ClientCitizen c : ClientCitizenCache.values()) {
			if (ACTIVE.containsKey(c.id))
				continue;
			double[] pos = c.renderPos();
			double dx = pos[0] - px;
			double dz = pos[2] - pz;
			if (dx * dx + dz * dz > ENTER_DIST2)
				continue;
			FakeCitizenEntity ent = new FakeCitizenEntity(FHEntityTypes.FAKE_CITIZEN.get(), level);
			ent.setCitizenId(c.id);
            ent.setId(-c.id - 1);
			ent.setPos(pos[0], pos[1], pos[2]);
			ent.xo = pos[0];
			ent.yo = pos[1];
			ent.zo = pos[2];
		// yawOf 返回 0~360°，而原版 setYRot/setYHeadRot/setYBodyRot 内部会用 wrapDegrees 折叠到 ±180°。
		// 若 yRotO/yHeadRotO/yBodyRotO 仍存 0~360° 的原始值，渲染插值会走"0°→270°"的绕远长路径，
		// 视觉上就是假实体(spawn)时抽搐/旋转一次。统一用 wrapDegrees 折叠，并让旧值与新值一致。
		// yawOf returns 0–360°, but vanilla setYRot/setYHeadRot/setYBodyRot wrap to ±180°.
		// Without wrapping yRotO etc., render interpolation takes the long 0°→270° path → spawn twitch.
		float yaw = Mth.wrapDegrees(yawOf(c));
		ent.setYRot(yaw);
		ent.yRotO = yaw;
		ent.setYHeadRot(yaw);
		ent.yHeadRotO = yaw;
		ent.setYBodyRot(yaw);
		ent.yBodyRotO = yaw;
			// ClientLevel.addEntity 是私有的，公开入口为 putNonPlayerEntity（内部触发 EntityJoinLevelEvent 并入表）
			// ClientLevel.addEntity is private; putNonPlayerEntity is the public spawn entry point
			level.putNonPlayerEntity(ent.getId(), ent);
			ACTIVE.put(c.id, ent);
		}
	}

	/**
	 * 将模拟缓存的位置/朝向写入实体及其插值旧值，驱动行走摆动动画。
	 * <p>
	 * Writes the cached position/yaw into the entity and its interpolation
	 * old-values, driving the walk swing animation.
	 *
	 * @param ent 假实体 / the fake entity
	 * @param c 模拟缓存 / the simulation cache entry
	 * @param pos 当前渲染位置 / current render position
	 */
	private static void drive(FakeCitizenEntity ent, ClientCitizen c, double[] pos) {
		double oldX = ent.getX();
		double oldZ = ent.getZ();
		ent.xo = oldX;
		ent.yo = ent.getY();
		ent.zo = oldZ;
		ent.setPos(pos[0], pos[1], pos[2]);
		ent.tickCount++;
		float moved = (float) Math.sqrt((pos[0] - oldX) * (pos[0] - oldX) + (pos[2] - oldZ) * (pos[2] - oldZ));
		// 行走位移一阶 EMA：分离力/外推抖动会让 moved 每 tick 有噪点，平滑后腿部摆动稳定。
		// First-order EMA on per-tick moved distance: separation/extrapolation noise
		// otherwise makes blocked citizens visibly march in place.
		moved = (moved + ent.smoothMoved) * 0.5f;
		ent.smoothMoved = moved;
		ent.walkAnimation.update(moved, 0.4F);
		// 软随 yaw：目标 yaw（wrapDegrees 折叠 ±180°）与当前 yaw 求短路径角差后按
		// 指数阻尼趋近（K=0.5，原版 Mth.rotateIfNecessary 同语义），速率钳制仅作
		// 极端翻转的兜底。newYaw 保持连续不折叠（跨 ±180° 接缝时累计为 181°、182°…），
		// 因为原版实体渲染对 yRotO→yRot 用 plain lerp（不绕短路径）——每 tick 折叠是
		// "单帧 342° 横扫"（疯狂旋转）的直接原因；只有数值同侧才能短路径过渡。
		// Soft-follow yaw: exponential approach (K=0.5, same semantics as vanilla
		// Mth.rotateIfNecessary) on the shortest-path delta to the wrapped target;
		// the rate clamp is a backstop against extreme flips only. newYaw stays
		// continuous (unwrapped) across the ±180° seam because vanilla renders the
		// entity yaw with plain lerp — per-tick folding is what caused one-frame spins.
		float targetYaw = Mth.wrapDegrees(yawOf(c));
		float curYaw = ent.getYRot();                // 上一 tick 渲染 yaw / last tick's yaw
		// 短路径角差：落在 (-180,180] / shortest signed angular delta in (-180,180]
		float delta = Mth.wrapDegrees(targetYaw - curYaw);
        // 现在 yaw 是 256 级连续值，单步只有 1.4°，不再需要 2° 大死区。
        // 直接用指数趋近，让微小变化也能平滑跟随；K=0.5 已经能平滑过渡。
        // With 256-step continuous yaw, one step is only 1.4°; the 2° deadband
        // would block it. Exponential approach alone is smooth enough.
        if (Math.abs(delta) < 0.1f)
            return;
		float step = delta * 0.5f;                   // 指数趋近 K=0.5 / exponential approach K=0.5
		if (step > MAX_YAW_PER_TICK)
			step = MAX_YAW_PER_TICK;
		else if (step < -MAX_YAW_PER_TICK)
			step = -MAX_YAW_PER_TICK;
		if (Math.abs(step) > 0.05f) {
			// 保持连续：跨接缝时 newYaw 累计为 ±180 以外的值（如 181°），不折叠。
			// keep continuous: let newYaw accumulate beyond ±180 across the seam, never fold.
			float newYaw = curYaw + step;
			// 防御性重锚定：|yaw| 过大时把"新值"与"旧值"同步平移 ±360——
			// 同量平移下插值结果旋转等价，不会制造接缝绕远，仅防止数值无限累计。
			// defensive re-anchor: shift new and old values together by ±360 when
			// |yaw| grows too large — rotation-equivalent, never creates a long path.
			float shift = 0;
			if (newYaw > 360.0f)
				shift = -360.0f;
			else if (newYaw < -360.0f)
				shift = 360.0f;
			// yRotO/yHeadRotO/yBodyRotO = 当前渲染值（同平移），让原版插值短路径平滑过渡。
			// old values = current render value (shifted together) so vanilla
			// interpolation takes the short path.
			ent.yRotO = curYaw + shift;
			ent.yHeadRotO = ent.getYHeadRot() + shift;
			ent.yBodyRotO = ent.yBodyRot + shift;
			ent.setYRot(newYaw + shift);
			ent.setYHeadRot(newYaw + shift);
			ent.setYBodyRot(newYaw + shift);
		}
	}

	/**
	 * 由最近非静止方向求 MC 朝向角（度）。
	 * <p>
	 * Resolves the MC yaw (degrees) from the last non-stationary direction.
	 *
	 * @param c 模拟缓存 / the simulation cache entry
	 * @return 朝向角 / yaw in degrees
	 */
    private static float yawOf(ClientCitizen c) {
        return (c.yaw & 0xFF) * (360.0f / 256.0f);
    }

	/**
	 * 某居民当前是否由假实体接管（批量渲染器据此跳过）。
	 * <p>
	 * Whether a citizen is currently owned by a fake entity (the batch
	 * renderer skips those).
	 *
	 * @param citizenId 居民 id / citizen id
	 * @return 已接管返回 true / true if owned by a fake entity
	 */
	public static boolean has(int citizenId) {
		return ACTIVE.containsKey(citizenId);
	}

	/**
	 * 清空全部假实体（退出世界时调用）。
	 * <p>
	 * Disposes all fake entities (called on world exit).
	 */
    public static void clearAll() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            for (FakeCitizenEntity e : ACTIVE.values())
                level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
        } else {
            for (FakeCitizenEntity e : ACTIVE.values())
                e.discard();
        }
        ACTIVE.clear();
    }
}
