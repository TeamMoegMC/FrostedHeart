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

import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

/**
 * 客户端居民渲染状态，快照插值 + 方向外推。
 * 渲染位置 = lerp(上一帧渲染位置（含外推尾巴）, 最新快照) + 超出窗口后沿当前方向
 * 按状态速度外推（钳制 {@value #EXTRAPOLATE_CLAMP} 秒）：新快照到达时以当前渲染
 * 位置为新插值起点，位置连续不瞬移，与服务端 Dead Reckoning 模型严格同源。
 * <p>
 * Client-side citizen render state: snapshot interpolation plus directional
 * extrapolation. Render position = lerp(current rendered position incl. the
 * extrapolation tail, latest snapshot), extrapolating past the window along
 * the current direction at the state speed (clamped to
 * {@value #EXTRAPOLATE_CLAMP} s). A snapshot arrival re-anchors the
 * interpolation at the current render position, keeping the path continuous
 * (no teleport), strictly mirroring the server-side dead-reckoning model.
 */
public final class ClientCitizen {

/**
 * 外推钳制时长（秒）。与服务端 {@link com.teammoeg.frostedheart.content.town.citizen.sync.SyncEngine#HEARTBEAT_INTERVAL}
 *（20 tick = 1 秒）对齐并留余量，保证心跳更新到来前外推持续不中断，
 * 避免"跑几步就停住"的现象。
 * <p>
 * Extrapolation clamp in seconds. Aligned with the server heartbeat
 * ({@link com.teammoeg.frostedheart.content.town.citizen.sync.SyncEngine#HEARTBEAT_INTERVAL},
 * 20 ticks = 1 s) with margin so extrapolation continues until the next heartbeat,
 * preventing the "runs a few steps then freezes" artifact.
 */
private static final double EXTRAPOLATE_CLAMP = 1.5;

	public final int id;
	/** 真实姓名（spawn 包同步，城镇托管居民）；空串 = 未托管，回退 CitizenNames 派生名 / Real name (synced by the spawn packet, town-backed); empty = unmanaged, falls back to CitizenNames */
	public final String name;
	/** 上一快照 / Previous snapshot */
	public double x0, y0, z0;
	/** 最新快照 / Latest snapshot */
	public double x1, y1, z1;
	public byte dir;
	public byte state;
	/** 最近一次非 NONE 的方向（供假实体/低模保持朝向） / Last non-NONE direction (facing persistence for fake entity/low-poly) */
	public byte lastDir;
	/** 方向持久性过滤：候选方向（-1=无） / Dir persistence filter: candidate (-1 = none) */
	private byte pendingDir = -1;
	/** 方向持久性过滤：候选连续批包数（≥2 才提交 lastDir） / Dir persistence filter: consecutive batches seen (commit to lastDir at ≥2) */
	private byte pendingCount = 0;
	/** 快照到达时间（nanoTime） / Snapshot arrival times (nanoTime) */
	private long t0, t1;
	private final double[] posBuf = new double[3];

	ClientCitizen(int id, int px, int py, int pz, byte dir, byte state, String name) {
		this.id = id;
		this.name = name == null ? "" : name;
		this.x0 = this.x1 = px / 1024.0;
		this.y0 = this.y1 = py / 1024.0;
		this.z0 = this.z1 = pz / 1024.0;
		this.dir = dir;
		this.state = state;
		this.lastDir = dir != CitizenState.DIR_NONE ? dir : 4; // 默认朝南 / default facing south
		this.t0 = this.t1 = System.nanoTime();
	}

	void update(int px, int py, int pz, byte dir, byte state) {
		// 以"当前渲染位置（含外推尾巴）"为新插值起点，快照到达瞬间位置连续、无回跳。
		// 修复前 x0 = x1 会丢弃外推尾巴：匀速行走时尾巴恰好等于真实位移不显形，
		// 但转向/停下时（客户端仍沿旧方向外推）到达帧会瞬间回退 speed × 发包间隔
		// （近距档约 0.5 格、心跳档最多约 2.7 格）= 可见瞬移。
		// Interpolate from the current rendered position (extrapolation tail
		// included) so a snapshot arrival never snaps the render position back.
		// Before: x0 = x1 discarded the extrapolation tail — invisible during
		// straight motion (the tail equals the true displacement), but on
		// turns/stops (client still extrapolating along the stale direction)
		// the arrival frame jumped back speed × packet gap (≈0.5 blocks near
		// tier, up to ≈2.7 blocks at the heartbeat tier) — a visible teleport.
		double[] cur = renderPos();
		long now = System.nanoTime();
		long prevGap = now - this.t0; // 真实收包间隔（自适应的窗口时长估计，而非上一次设置的窗口）
		this.x0 = cur[0];
		this.y0 = cur[1];
		this.z0 = cur[2];
		this.t0 = now;
		this.t1 = now + Math.max(prevGap, 50_000_000L); // 窗口 ≥ 50ms：首个批包前平滑收敛
		this.x1 = px / 1024.0;
		this.y1 = py / 1024.0;
		this.z1 = pz / 1024.0;
		this.dir = dir;
		this.state = state;
		// 连续 2 个批包同值才提交 lastDir：单包抖动（服务端 dir 毛刺）不改变朝向。
		// Commit to lastDir only after the same dir arrives in 2 consecutive
		// batches: single-batch jitter never changes the facing.
		if (dir != CitizenState.DIR_NONE) {
			if (pendingDir == dir) {
				if (++pendingCount >= 2)
					this.lastDir = dir;
			} else {
				pendingDir = dir;
				pendingCount = 1;
			}
		} else {
			pendingDir = -1;
			pendingCount = 0; // DIR_NONE 只清 pending，不写 lastDir（防 & 15 把 255 映射成 dir 15）
		}
		// 注意：不得在此覆盖 t1。t1 是上方设置的"插值窗口结束时刻"（now + 收包间隔），
		// 若重置为当前时刻，interval≈0 会被钳到 50ms：渲染位置每个包到达后 50ms 内
		// 硬贴到快照然后冻结到下一个包（近距 200ms 一跳、心跳档 1s 一跳 ≈3.9 格 = 瞬移），
		// 且外推分支 interval>0.35 永不成立，Dead Reckoning 完全失效；
		// 位置"冻结→猛跳"还让 FakeCitizenManager 按位移求朝向时目标 yaw 来回翻转（抽搐）。
	}

	/**
	 * 当前是否处于移动状态。
	 * <p>
	 * Whether currently in a moving state.
	 *
	 * @return 移动中返回 true / true if moving
	 */
	public boolean isMoving() {
		int s = state & 0xFF;
		return dir != CitizenState.DIR_NONE && s < CitizenState.STATE_COUNT && CitizenState.MOVING[s];
	}

	/**
	 * 计算当前渲染位置（含插值与外推），写入 out[3]。
	 * <p>
	 * Computes the current render position (interpolated + extrapolated) into out[3].
	 *
	 * @return 渲染位置数组 [x, y, z] / render position array [x, y, z]
	 */
	public double[] renderPos() {
		long now = System.nanoTime();
		double interval = (t1 - t0) / 1e9;
		if (interval < 0.05)
			interval = 0.05;
		else if (interval > 1.0)
			interval = 1.0;
		double g = (now - t0) / 1e9 / interval;
		if (g > 1.0)
			g = 1.0;
		double x = x0 + (x1 - x0) * g;
		double y = y0 + (y1 - y0) * g;
		double z = z0 + (z1 - z0) * g;
		if (isMoving()) {
			double extra = (now - t1) / 1e9;
			if (extra > EXTRAPOLATE_CLAMP)
				extra = EXTRAPOLATE_CLAMP;
			// 仅远距档（收包间隔 > 0.35s，8/20 tick 心跳）才外推：
			// 近距档 4 tick 快照足够密集，插值即平滑；外推反而会在服务端停下后
			// 产生过冲回弹（渲染倒退滑回真值，配合行走动画看起来"倒着走"）。
			// Extrapolate only on sparse tiers (packet gap > 0.35 s, the 8/20-tick
			// heartbeats): the 4-tick near tier is dense enough for pure
			// interpolation, and extrapolation there would overshoot on stops
			// (the render glides back to the truth — reads as walking backwards).
			if (extra > 0 && interval > 0.35) {
				int d = dir & 0xFF;
				double speed = CitizenState.SPEED[state & 0xFF] * 20.0 / CitizenState.FIXED_SCALE;
				x += CitizenState.DIR_X[d] / 1024.0 * speed * extra;
				z += CitizenState.DIR_Z[d] / 1024.0 * speed * extra;
			}
		}
		posBuf[0] = x;
		posBuf[1] = y;
		posBuf[2] = z;
		return posBuf;
	}
}
