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
        boolean doConform = gameTime % 5 == 0;   // 每 5 tick 站立居民贴地
        boolean doSeparation = (gameTime & 1) == 0; // 每 2 tick 计算分离力

        for (CitizenContainer c : sched.containers()) {
            CitizenSim sim = c.sim();
            int n = sim.size();
			boolean persistedChanged = false;
            for (int i = 0; i < n; i++) {
                if (!sched.isActive(c, i))
                    continue;
				int oldX = sim.px[i];
				int oldY = sim.py[i];
				int oldZ = sim.pz[i];
			int oldTargetX = sim.tx[i];
			int oldTargetZ = sim.tz[i];
			byte oldDir = sim.dir[i];

                if (CitizenState.MOVING[sim.state[i]]) {
                    // 移动居民：只调用一次 step
                    step(sched, sim, level, i, gameTime, doSeparation);
                } else if (doConform) {
                    // 站立居民：每 5 tick 贴地一次
                    conformHeight(sim, level, i);
                }
			persistedChanged |= oldX != sim.px[i] || oldY != sim.py[i] || oldZ != sim.pz[i]
					|| oldTargetX != sim.tx[i] || oldTargetZ != sim.tz[i]
					|| oldDir != sim.dir[i];
            }
			if (persistedChanged)
				c.markDirty();
        }
    }

// 在 MovementSystem 类中添加以下代码

    /**
     * 合成基础移动方向位移与分离力，并执行轴分离移动。
     * 基础移动速度由状态速度决定；分离力来自邻居，限制在 SEP_MAX 内。
     */
    private void step(CitizenSimScheduler sched, CitizenSim sim, ServerLevel level, int i, long gameTime, boolean doSeparation) {
        int dx = sim.tx[i] - sim.px[i];
        int dz = sim.tz[i] - sim.pz[i];
        long dist2 = (long) dx * dx + (long) dz * dz;

        // 到达：直接停下，方向保持不变
        if (dist2 < CitizenState.ARRIVE_DIST2) {
            return;
        }

        // ===== 1. 确定移动目标方向（优先流场，近距回退直线） =====
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

        // ===== 2. 地形可通行性检查（只用于确定最终导航方向） =====
        int feetY = sim.py[i] >> 10;
        int bx = sim.px[i] >> 10;
        int bz = sim.pz[i] >> 10;
        int nbX = bx + Integer.signum(CitizenState.DIR_X_16[moveDir16]);
        int nbZ = bz + Integer.signum(CitizenState.DIR_Z_16[moveDir16]);

        if (!passable(level, nbX, nbZ, feetY)) {
            moveDir16 = rotatePassable(level, bx, bz, moveDir16, feetY);
            if (moveDir16 < 0) {
                // 全堵：停下等重规划
                checkStuck(sim, i, dist2, gameTime);
                return;
            }
        }

        // ===== 3. 直写 16 向移动方向 =====
        // 服务端不再维护连续视觉 yaw：同步语义就是"移动方向"，客户端本地
        // 软转向（visYaw 闭环追赶），渲染平滑与网络/模拟彻底解耦。
        sim.dir[i] = (byte) moveDir16;

        // ===== 4. 基础位移（由导航方向决定，与同步给客户端的方向严格同源） =====
        int speed = CitizenState.SPEED[sim.state[i]];
        int baseX = (CitizenState.DIR_X_16[moveDir16] * speed) >> 10;
        int baseZ = (CitizenState.DIR_Z_16[moveDir16] * speed) >> 10;

        // 记录移动前所在格，用于判断是否跨格
        int oldBX = sim.px[i] >> 10;
        int oldBZ = sim.pz[i] >> 10;

        // ===== 5. 分离力（只计算，不直接应用） =====
        int pushX, pushZ;
        if (doSeparation) {
            int[] sep = computeSeparation(sched, sim, i);
            pushX = sep[0];
            pushZ = sep[1];
            sim.sepX[i] = pushX;  // 缓存
            sim.sepZ[i] = pushZ;
        } else {
            pushX = sim.sepX[i];
            pushZ = sim.sepZ[i];
        }

        // 合成总位移
        int moveX = baseX + pushX;
        int moveZ = baseZ + pushZ;

        // ===== 6. 轴分离移动（核心碰撞，格内 0 查询，跨格才查一次） =====
        moveAxisSeparated(sched, sim, level, i, moveX, moveZ);

        // ===== 7. 贴地 =====
        // 跨格时立即贴地，无论是否本 tick 已贴过
        int newBX = sim.px[i] >> 10;
        int newBZ = sim.pz[i] >> 10;
        boolean crossed = newBX != oldBX || newBZ != oldBZ;
        if (crossed) {
            conformHeight(sim, level, i);
        }

        // ===== 8. 卡住检测 =====
        checkStuck(sim, i, dist2, gameTime);
    }

    /**
     * 只计算邻居分离力，返回 [pushX, pushZ]。
     * 不直接修改位置，便于和基础位移合成，供轴分离移动统一处理。
     */
    private int[] computeSeparation(CitizenSimScheduler sched, CitizenSim sim, int i) {
        neighborBuf.clear();
        sched.grid.queryNeighbors(sim.px[i] >> 10, sim.pz[i] >> 10, neighborBuf);

        int pushX = 0;
        int pushZ = 0;
        int count = neighborBuf.size();

        for (int k = 0; k < count; k++) {
            int jid = neighborBuf.getInt(k);
            if (jid == sim.id[i]) continue;

            CitizenContainer oc = sched.findById(jid);
            if (oc == null) continue;

            CitizenSim osim = oc.sim();
            int j = osim.indexOf(jid);
            if (j < 0) continue;

            int dx = sim.px[i] - osim.px[j];
            int dz = sim.pz[i] - osim.pz[j];
            int d2 = dx * dx + dz * dz;
            if (d2 == 0 || d2 > SEP_DIST2) continue;

            int push = (SEP_DIST2 - d2) >> 13;
            pushX += (int) (((long) dx * push) >> 10);
            pushZ += (int) (((long) dz * push) >> 10);
        }

        if (pushX > SEP_MAX) pushX = SEP_MAX;
        else if (pushX < -SEP_MAX) pushX = -SEP_MAX;
        if (pushZ > SEP_MAX) pushZ = SEP_MAX;
        else if (pushZ < -SEP_MAX) pushZ = -SEP_MAX;

        return new int[]{pushX, pushZ};
    }

    /**
     * 轴分离移动：先 X 后 Z。
     * 每轴只有在跨格时才调用 passable()，不可通行则把坐标钳制在当前格边界，
     * 从而获得贴墙滑行，并机制性杜绝 corner cutting 与分离力推墙。
     */
    private void moveAxisSeparated(CitizenSimScheduler sched, CitizenSim sim, ServerLevel level,
                                   int i, int moveX, int moveZ) {
        int feetY = sim.py[i] >> 10;

        // ---------- X 轴 ----------
        int oldX = sim.px[i];
        int newX = oldX + moveX;
        int oldBX = oldX >> 10;
        int newBX = newX >> 10;

        if (newBX != oldBX) {
            int nbX = newBX;
            int nbZ = sim.pz[i] >> 10;
            if (!passable(level, nbX, nbZ, feetY)) {
                // 贴墙：不进入新格，停在当前格边界
                if (moveX > 0) {
                    newX = newBX << 10;          // 正方向：新格起点即边界
                } else {
                    newX = (newBX + 1) << 10;    // 负方向：新格终点加 1 即边界
                }
            }
        }
        sim.px[i] = newX;

        // ---------- Z 轴 ----------
        int oldZ = sim.pz[i];
        int newZ = oldZ + moveZ;
        int oldBZ = oldZ >> 10;
        int newBZ = newZ >> 10;

        if (newBZ != oldBZ) {
            int nbX = sim.px[i] >> 10;   // 注意：使用已更新后的 X
            int nbZ = newBZ;
            if (!passable(level, nbX, nbZ, feetY)) {
                if (moveZ > 0) {
                    newZ = newBZ << 10;
                } else {
                    newZ = (newBZ + 1) << 10;
                }
            }
        }
        sim.pz[i] = newZ;
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

            if (passable(level,
                    bx + Integer.signum(CitizenState.DIR_X_16[d1]),
                    bz + Integer.signum(CitizenState.DIR_Z_16[d1]),
                    feetY))
                return d1;
            if (passable(level,
                    bx + Integer.signum(CitizenState.DIR_X_16[d2]),
                    bz + Integer.signum(CitizenState.DIR_Z_16[d2]),
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
