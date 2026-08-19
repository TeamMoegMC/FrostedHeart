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

package com.teammoeg.frostedheart.content.town.citizen;

import com.teammoeg.frostedheart.content.town.citizen.client.CitizenMenuClient;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;

/**
 * 假居民实体：仅存在于客户端的"代理傀儡"，为近距离且入选详细展示预算的居民提供
 * 真实的人形渲染与原版交互手感（准星高亮、点击判定）。候选 16 格进入、
 * 20 格退出，实际数量由客户端 {@code maxDetailedCitizenEntities} 严格限制。
 * 它不参与任何服务端逻辑：无 AI、无寻路、无重力、无网络同步、不存档；
 * 位置与朝向由 FakeCitizenManager 每客户端 tick 按模拟缓存直接驱动。
 * 服务端完全不知道它的存在，因此零额外网络开销。
 * <p>
 * Fake citizen entity: a client-only proxy puppet giving selected near-range
 * citizens real humanoid rendering and vanilla interaction feel (crosshair
 * highlight and click picking). Candidates enter at 16 blocks, leave at 20,
 * and are strictly bounded by the client
 * {@code maxDetailedCitizenEntities} setting. It runs no server logic at all: no
 * AI, no pathfinding, no gravity, no network sync, no saving; position and
 * yaw are driven every client tick by FakeCitizenManager from the
 * simulation cache. The server never knows it exists, so it costs zero
 * extra network traffic.
 */
public class FakeCitizenEntity extends Mob {

	/** 关联的模拟居民稳定 id / Associated simulated citizen stable id */
	private int citizenId = -1;

	/**
	 * 行走位移平滑值（FakeCitizenManager 每 tick 写入，一阶 EMA）。
	 * <p>
	 * Smoothed per-tick moved distance (first-order EMA, written each client
	 * tick by FakeCitizenManager).
	 */
	public float smoothMoved;

	public FakeCitizenEntity(EntityType<? extends FakeCitizenEntity> type, Level level) {
		super(type, level);
		this.setInvulnerable(true);
		this.setNoGravity(true);
	}

	/**
	 * 关联的模拟居民 id。
	 * <p>
	 * The associated simulated citizen id.
	 *
	 * @return 居民 id / citizen id
	 */
	public int getCitizenId() {
		return citizenId;
	}

	public void setCitizenId(int citizenId) {
		this.citizenId = citizenId;
	}

	/**
	 * 空 tick：位姿完全由 FakeCitizenManager 驱动，禁用 AI/移动/碰撞/击退等一切原版行为。
	 * <p>
	 * Empty tick: pose is fully driven by FakeCitizenManager; all vanilla
	 * behavior (AI, movement, collision, knockback) is disabled.
	 */
	@Override
	public void tick() {
		// 有意为空 / intentionally empty
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	/**
	 * 右键交互：客户端打开交互菜单（流浪难民同款对话 GUI，菜单按钮再发 C2S 动作包）。
	 * 正常情况下准星拦截（CitizenClientEvents）已先行处理，本方法作为实体射线命中时的兜底路径。
	 * <p>
	 * Right-click interaction: the client opens the interaction menu (the same
	 * dialogue GUI the wandering refugee uses; menu buttons send C2S action
	 * packets). Normally crosshair interception (CitizenClientEvents) handles
	 * it first; this is the fallback path when the entity ray trace wins.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide) {
			if (citizenId >= 0)
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CitizenMenuClient.open(citizenId));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/**
	 * 本实体从不由服务端生成，此方法仅为满足抽象约束。
	 * <p>
	 * This entity is never spawned by the server; implemented only to satisfy
	 * the abstract contract.
	 */
	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}
}
