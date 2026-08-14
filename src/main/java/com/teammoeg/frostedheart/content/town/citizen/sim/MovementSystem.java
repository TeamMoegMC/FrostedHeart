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

import com.teammoeg.frostedheart.content.town.citizen.nav.FlowField;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 移动积分系统：每 tick 全量扫描活跃移动单位，纯定点查表运算。
 * 远距经 {@link FlowField} 流场寻向（未就绪自动回退直线），近距直线逼近精确目标，
 * 另含邻居分离力与高度图贴合（跨越方块边界时重采样）。
 * <p>
 * Movement integration system: full scan of active moving units every tick,
 * pure fixed-point table lookups. Long-range steering samples the shared
 * {@link FlowField} (graceful fallback to straight-line while not ready),
 * short-range steering goes straight at the precise target; neighbor
 * separation and heightmap conformance (resampled on block-boundary crossing)
 * included.
 */
public final class MovementSystem {
	/** 分离半径（定点，1.5 方块） / Separation radius (fixed-point, 1.5 blocks) */
	private static final int SEP_DIST2 = 1536 * 1536;
	/** 单 tick 最大分离位移（定点） / Max separation displacement per tick (fixed-point) */
	private static final int SEP_MAX = 48;
	/** 启用流场的最小目标距离平方（定点，12 方块）；近距直线可直达精确目标 / Min squared target distance for flow-field steering (fixed-point, 12 blocks) */
	private static final long FIELD_MIN_DIST2 = 12288L * 12288L;
	/** 卡住判定窗口（tick）：移动中超过此时长未跨越方块边界视为卡住 / Stuck window (ticks): moving without crossing a block boundary longer than this counts as stuck */
	private static final int STUCK_TICKS = 100;

	private final IntArrayList neighborBuf = new IntArrayList(32);

	/**
	 * 对所有活跃居民执行移动积分（遍历调度器全部容器）。
	 * <p>
	 * Integrates movement for all active citizens (over every scheduler container).
	 *
	 * @param sched 调度器 / the scheduler
	 * @param level 维度 / the level
	 * @param gameTime 当前游戏时间 / current game time
	 */
	public void tickAll(CitizenSimScheduler sched, ServerLevel level, long gameTime) {
		for (CitizenContainer c : sched.containers()) {
			CitizenSim sim = c.sim();
			int n = sim.size();
			for (int i = 0; i < n; i++) {
				if (!sched.isActive(c, i))
					continue;
				// 每 tick 贴合当前列地面：站立（空闲/睡觉）居民不再只有"移动+跨边界"才重采样，
				// 挖掉/填上脚下方块时也能及时重新贴合，杜绝"挖掉方块后一直浮空不刷新"。
				// Re-conform the ground under every active citizen each tick — standing
				// citizens previously re-sampled only when moving across a block
				// boundary, so digging out their feet never refreshed them (floating).
				conformHeight(sim, level, i);
				if (!CitizenState.MOVING[sim.state[i]])
					continue;
				step(sched, sim, level, i, gameTime);
			}
		}
	}

/*	private void step(CitizenSimScheduler sched, CitizenSim sim, ServerLevel level, int i, long gameTime) {
		int dx = sim.tx[i] - sim.px[i];
		int dz = sim.tz[i] - sim.pz[i];
		long dist2 = (long) dx * dx + (long) dz * dz;
		if (dist2 < ARRIVE_DIST2) {
			// 到达：直接停下，不吸附目标（吸附会造成最多 1.5 格瞬时位移 = 可见瞬移）。
			// Arrive: stop in place; do NOT snap onto the target (a snap is a
			// positional jump of up to 1.5 blocks — a visible teleport).
			sim.dir[i] = CitizenState.DIR_NONE;
			sim.rdir[i] = CitizenState.DIR_NONE; // 停止立即上报 / stop is committed immediately
			sim.candDir[i] = -1;
			sim.candAge[i] = 0;
			return;
		}
		int dir = -1;
		// 远距尝试流场；场未就绪/不可达时回退直线
		if (dist2 > FIELD_MIN_DIST2) {
			FlowField field = sched.fields.request(sim.tx[i] >> 10, sim.tz[i] >> 10, gameTime);
			if (field != null)
				dir = field.sampleDir(sim.px[i] >> 10, sim.pz[i] >> 10);
		}
		if (dir < 0)
			dir = CitizenState.dirFromVector(dx, dz);
		// 地形可通行性：直线模式原本完全不查地形，居民会直穿峭壁/墙/水面（"无视地形高度"），
		// 且高度贴合在越界时采样列顶会把居民抬到屋檐/悬空物顶上（"浮空"）。
		// 规则与流场 BFS 一致（高度差 ≤1）+ 落脚净空 2 格 + 非流体 + 未加载区块不可通行；
		// 前进方向被堵时向两侧旋转试探（贴墙绕行），全堵则停下等行为系统重规划。
		// Straight-line steering previously had zero terrain checks (citizens walked
		// through cliffs/walls/water) and height conformance sampled the column top
		// (citizens lifted onto overhangs = floating). Passability now mirrors the
		// flow-field BFS rule (|Δheight| ≤ 1) plus 2-block headroom, no fluid, and
		// unloaded chunks are impassable; blocked cells trigger ±22.5°/±45° wall
		// following, and fully-blocked surroundings stop the citizen for replanning.
		int feetY = sim.py[i] >> 10;
		if (dir >= 0) {
			int bx = sim.px[i] >> 10;
			int bz = sim.pz[i] >> 10;
			int nbX = bx + Integer.signum(CitizenState.DIR_X[dir]);
			int nbZ = bz + Integer.signum(CitizenState.DIR_Z[dir]);
			if (!passable(level, nbX, nbZ, feetY))
				dir = rotatePassable(level, bx, bz, dir, feetY);
		}
		if (dir < 0) {
			// 四周全部不可通行：停下（客户端立即看到停止帧），卡住水位仍兜底
			sim.dir[i] = CitizenState.DIR_NONE;
			sim.rdir[i] = CitizenState.DIR_NONE;
			sim.candDir[i] = -1;
			sim.candAge[i] = 0;
			checkStuck(sim, i, dist2, gameTime);
			return;
		}
		// 转向 dir 逐 tick 重算（分离力后置、近目标角变快都会让它抖动）；上报 dir 加
		// 连续 2 tick 持久性过滤，单 tick 翻转不传播给客户端，杜绝目标 yaw 振荡。
		// Steering dir is recomputed every tick (jittery from late separation and
		// fast angle change near targets); the reported dir commits only after the
		// candidate persists 2 consecutive ticks, so single-tick flips never reach
		// the client and the client-side target yaw stops oscillating.
		sim.dir[i] = (byte) dir;
		sim.yaw[i] = CitizenState.yawFromDir(dir);
		if (dir == CitizenState.DIR_NONE) {
			sim.rdir[i] = CitizenState.DIR_NONE;
			sim.candDir[i] = -1;
			sim.candAge[i] = 0;
		} else if (sim.candDir[i] == dir) {
			if (sim.candAge[i] == 1)
				sim.rdir[i] = (byte) dir; // 连续第 2 tick 相同才提交 / commit on the 2nd consecutive tick
			sim.candAge[i] = 2;
		} else {
			sim.candDir[i] = (byte) dir;
			sim.candAge[i] = 1;
		}
		int speed = CitizenState.SPEED[sim.state[i]];
		// 定点乘法：(分量×1024 * 速度) >> 10
		sim.px[i] += (CitizenState.DIR_X[dir] * speed) >> 10;
		sim.pz[i] += (CitizenState.DIR_Z[dir] * speed) >> 10;

		separate(sched, sim, i);
		checkStuck(sim, i, dist2, gameTime);
	}*/
private void step(CitizenSimScheduler sched, CitizenSim sim, ServerLevel level, int i, long gameTime) {
    int dx = sim.tx[i] - sim.px[i];
    int dz = sim.tz[i] - sim.pz[i];
    long dist2 = (long) dx * dx + (long) dz * dz;

    // 到达：直接停下，yaw 保持不变
    if (dist2 < CitizenState.ARRIVE_DIST2) {
        return;
    }

    // 1. 确定移动目标方向（优先流场，近距/不可用回退直线）
    int moveDir16 = -1;
    if (dist2 > FIELD_MIN_DIST2) {
        FlowField field = sched.fields.request(sim.tx[i] >> 10, sim.tz[i] >> 10, gameTime);
        if (field != null) {
            moveDir16 = field.sampleDir(sim.px[i] >> 10, sim.pz[i] >> 10);
        }
    }
    if (moveDir16 < 0) {
        moveDir16 = CitizenState.dirFromVector(dx, dz);
    }

    // 2. 地形可通行性检查（使用最终确定的方向）
    int feetY = sim.py[i] >> 10;
    int bx = sim.px[i] >> 10;
    int bz = sim.pz[i] >> 10;
    int checkYawByte = CitizenState.DIR_TO_YAW[moveDir16] & 0xFF;
    int nbX = bx + Integer.signum(CitizenState.DIR_X_256[checkYawByte]);
    int nbZ = bz + Integer.signum(CitizenState.DIR_Z_256[checkYawByte]);

    if (!passable(level, nbX, nbZ, feetY)) {
        moveDir16 = rotatePassable(level, bx, bz, moveDir16, feetY);
        if (moveDir16 < 0) {
            // 全堵：停下等重规划
            checkStuck(sim, i, dist2, gameTime);
            return;
        }
    }

    // 3. 目标 yaw（256 级）
    byte targetYaw = CitizenState.DIR_TO_YAW[moveDir16]; // 直接用缓存，避免重复计算

    // 4. 视觉 yaw 缓慢旋转跟随（1 步/tick）
    int curYaw = sim.yaw[i] & 0xFF;
    int tgtYaw = targetYaw & 0xFF;
    int diff = tgtYaw - curYaw;
    if (diff > 128) diff -= 256;
    else if (diff < -128) diff += 256;

    int step = 3; // 每 tick 最多旋转 3 步（约 4.2°/tick）
    if (diff > 0) {
        if (diff < step) step = diff;
        sim.yaw[i] = (byte)((curYaw + step) & 0xFF);
    } else if (diff < 0) {
        if (-diff < step) step = -diff;
        sim.yaw[i] = (byte)((curYaw - step) & 0xFF);
    }

    // 5. 使用最终确定的移动方向进行位移
    int moveYawByte = CitizenState.DIR_TO_YAW[moveDir16] & 0xFF;
    int speed = CitizenState.SPEED[sim.state[i]];
    sim.px[i] += (CitizenState.DIR_X_256[moveYawByte] * speed) >> 10;
    sim.pz[i] += (CitizenState.DIR_Z_256[moveYawByte] * speed) >> 10;

    // 6. 分离力与卡住检测
    separate(sched, sim, i);
    checkStuck(sim, i, dist2, gameTime);
}


	private void separate(CitizenSimScheduler sched, CitizenSim sim, int i) {
		neighborBuf.clear();
		sched.grid.queryNeighbors(sim.px[i] >> 10, sim.pz[i] >> 10, neighborBuf);
		int pushX = 0;
		int pushZ = 0;
		int count = neighborBuf.size();
		for (int k = 0; k < count; k++) {
			// 网格条目是稳定 id（跨容器无索引冲突，两镇居民互相让路）：反查容器与索引
			// Grid entries are stable ids (no index collisions across containers —
			// citizens of different towns separate from each other): resolve the
			// owning container and index.
			int jid = neighborBuf.getInt(k);
			if (jid == sim.id[i])
				continue;
			CitizenContainer oc = sched.findById(jid);
			if (oc == null)
				continue;
			CitizenSim osim = oc.sim();
			int j = osim.indexOf(jid);
			if (j < 0)
				continue;
			int dx = sim.px[i] - osim.px[j];
			int dz = sim.pz[i] - osim.pz[j];
			int d2 = dx * dx + dz * dz;
			if (d2 == 0 || d2 > SEP_DIST2)
				continue;
			// 距离越近推得越开（反比近似，免开方）
			int push = (SEP_DIST2 - d2) >> 13;
			pushX += (int) (((long) dx * push) >> 10);
			pushZ += (int) (((long) dz * push) >> 10);
		}
		if (pushX > SEP_MAX)
			pushX = SEP_MAX;
		else if (pushX < -SEP_MAX)
			pushX = -SEP_MAX;
		if (pushZ > SEP_MAX)
			pushZ = SEP_MAX;
		else if (pushZ < -SEP_MAX)
			pushZ = -SEP_MAX;
		sim.px[i] += pushX;
		sim.pz[i] += pushZ;
	}

	/**
	 * 贴合当前列地面高度（含屋檐下扫描），每 tick 对全部活跃居民调用。
	 * 无"跨边界才采样"限制：同一格内方块被挖/填也能立即响应。
	 * 净空兜底（被分离力推进墙列时禁止抬升）+ 单 tick 最大下降 2 格
	 * （模拟坠落感，避免挖深坑后竖直瞬移到坑底）。
	 * <p>
	 * Conforms the feet to the current column's ground (with under-overhang
	 * scan), called every tick for all active citizens. No boundary-crossing
	 * gate: digging/filling within the same block responds immediately.
	 * Headroom fallback (no lift when pushed into a wall) plus a max descent
	 * of 2 blocks/tick (fall-like motion instead of a vertical teleport into
	 * a freshly dug pit).
	 */
	private void conformHeight(CitizenSim sim, ServerLevel level, int i) {
		int bx = sim.px[i] >> 10;
		int bz = sim.pz[i] >> 10;
		int curFeet = sim.py[i] >> 10;
		int feet = feetHeight(level, bx, bz, curFeet + 1);
		// 净空兜底：被分离力推进墙列时禁止抬升（回退到原脚底），防穿墙浮空
		while (feet > curFeet && (level.getBlockState(new BlockPos(bx, feet, bz)).isSolid()
				|| level.getBlockState(new BlockPos(bx, feet + 1, bz)).isSolid()))
			feet--;
		// 单 tick 最多下降 2 格（≈40 格/秒坠落），挖掉脚下方块后平滑落进坑底
		if (feet < curFeet - 2)
			feet = curFeet - 2;
		sim.py[i] = feet << 10;
	}

	/**
	 * 目标格可通行判定：高度差 ≤1（与流场 BFS 规则一致）+ 落脚净空 2 格 + 非流体。
	 * 未加载区块一律不可通行（与流场 UNLOADED 规则一致）。
	 * <p>
	 * Passability of the destination cell: height difference ≤ 1 (same rule as the
	 * flow-field BFS), 2-block standing clearance, no fluid. Unloaded chunks are
	 * always impassable (same as the flow field's UNLOADED rule).
	 */
	private static boolean passable(ServerLevel level, int nbX, int nbZ, int feetY) {
		if (!level.hasChunk(nbX >> 4, nbZ >> 4))
			return false;
		int fB = feetHeight(level, nbX, nbZ, feetY + 1);
		if (Math.abs(fB - feetY) > 1)
			return false;
		BlockPos pos = new BlockPos(nbX, fB, nbZ);
		if (level.getBlockState(pos).isSolid())
			return false; // 落脚处被固体占据（墙/1 格缝隙）
		if (!level.getBlockState(pos).getFluidState().isEmpty())
			return false; // 涉水
		if (level.getBlockState(pos.above()).isSolid())
			return false; // 头顶净空
		return true;
	}

	/**
	 * 前进方向被堵时按 ±22.5°、±45° 顺序试探可通行方向（贴墙绕行）；全堵返回 -1。
	 * <p>
	 * When the forward cell is impassable, try ±22.5° then ±45° bins for a
	 * passable direction (wall following); -1 if all are blocked.
	 */
    private static int rotatePassable(ServerLevel level, int bx, int bz, int dir, int feetY) {
        for (int k = 1; k <= 2; k++) {
            int d1 = (dir + k) & 15;
            int d2 = (dir - k + 16) & 15;

            int yawD1 = CitizenState.DIR_TO_YAW[d1] & 0xFF;
            int yawD2 = CitizenState.DIR_TO_YAW[d2] & 0xFF;

            if (passable(level,
                    bx + Integer.signum(CitizenState.DIR_X_256[yawD1]),
                    bz + Integer.signum(CitizenState.DIR_Z_256[yawD1]),
                    feetY))
                return d1;
            if (passable(level,
                    bx + Integer.signum(CitizenState.DIR_X_256[yawD2]),
                    bz + Integer.signum(CitizenState.DIR_Z_256[yawD2]),
                    feetY))
                return d2;
        }
        return -1;
    }

	/**
	 * 该方块列中 ≤ maxFeet 的最高可站立脚底高度（方块单位）。
	 * 正常地形即高度图列顶；被屋檐/桥梁等悬空物遮挡（列顶 &gt; maxFeet）时
	 * 向下扫描最近实心方块顶面，让居民在遮挡下方的地面行走，
	 * 而不是被抬到遮挡物顶上（浮空根因修复）。
	 * <p>
	 * Highest standable feet height ≤ maxFeet in the column (block units).
	 * On open terrain this is the heightmap top; under an overhang (top &gt; maxFeet)
	 * it scans down to the nearest solid top so citizens keep walking on the
	 * ground instead of being lifted onto the overhang (floating root cause).
	 */
	private static int feetHeight(ServerLevel level, int bx, int bz, int maxFeet) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
		if (top <= maxFeet)
			return top; // 无遮挡：列顶即地面
		for (int f = maxFeet; f > maxFeet - 24; f--) {
			if (level.getBlockState(new BlockPos(bx, f - 1, bz)).isSolid())
				return f;
		}
		return top; // 兜底：极端结构按列顶（不劣于原行为）
	}

	/**
	 * 卡住自救：进度 = 距目标平方距离跌破"本次行程水位"（watermark）。
	 * 环绕被占目标点、被地形/人群堵住时 dist2 不再下降 → 超时后停下，
	 * 行为系统下一决策周期会重新规划目标（避障绕行交给流场重建周期吸收）。
	 * <p>
	 * Stuck self-rescue: progress means the squared distance to the target has
	 * dropped below this trip's watermark. While orbiting an occupied target
	 * or blocked by terrain/crowds the dist2 stops dropping, so the unit stops
	 * after {@link #STUCK_TICKS} and the behavior system replans at the next
	 * decision cycle (obstacle rerouting is absorbed by the flow-field rebuild
	 * cycle).
	 *
	 * @param sim 模拟数据 / the sim data
	 * @param i 运行期索引 / runtime index
	 * @param dist2 当前距目标平方距离（定点） / current squared target distance (fixed-point)
	 * @param gameTime 当前游戏时间 / current game time
	 */
    private void checkStuck(CitizenSim sim, int i, long dist2, long gameTime) {
        int now = (int) gameTime;
        if (sim.stuckTick[i] == 0) {
            sim.stuckTick[i] = now;
            sim.bestDist2[i] = dist2;
            return;
        }
        if (dist2 < sim.bestDist2[i]) {
            sim.bestDist2[i] = dist2;
            sim.stuckTick[i] = now;
            return;
        }
        if (now - sim.stuckTick[i] > STUCK_TICKS) {
            // 卡住自救：将目标拉回当前位置，下一 tick 自动停止移动
            sim.tx[i] = sim.px[i];
            sim.tz[i] = sim.pz[i];
            sim.stuckTick[i] = now;
        }
    }
}
