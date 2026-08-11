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

import net.minecraft.server.level.ServerLevel;

/**
 * 居民行为系统：扁平整数状态机，分帧驱动（每单位 1Hz）。
 * 纯整数运算，单次决策亚微秒级；随机性由 id+时间确定性播种。
 * 当前实现为城镇作息：上班时段（07:20–18:20）有工作的居民通勤到工作锚点站岗
 * （偶尔踱步），其余时间在家周围闲逛，夜晚回家睡觉；无工作居民（wx=-1）白天闲逛。
 * 职业生产链任务后续以"任务提供者"注册进同一状态机，不改动热路径。
 * <p>
 * Citizen behavior system: flat integer state machine driven by time slicing
 * (1 Hz per citizen). Pure integer math, sub-microsecond per decision;
 * randomness is deterministically seeded by id+time. The current baseline is
 * a town routine: during work hours (07:20–18:20) employed citizens commute to
 * the work anchor and stand on duty (occasional pacing), otherwise wander near
 * home, returning home and sleeping at night; the unemployed (wx = -1) wander
 * by day. Job/production tasks will later register as task providers into the
 * same state machine without touching the hot path.
 */
public final class BehaviorSystem {

	/** 分帧数：每 20 tick 每个居民决策一次 / Slice count: each citizen decides once per 20 ticks */
	public static final int SLICE = 20;
	/** 闲逛半径（方块） / Wander radius in blocks */
	private static final int WANDER_RADIUS = 12;
	/** 工作时间窗起点（世界日时刻，24000 制，约 07:20） / Work window start (world day time, 07:20) */
	private static final long WORK_START = 2000;
	/** 工作时间窗终点（约 18:20；夜晚 12542 前到家） / Work window end (18:20; home before night 12542) */
	private static final long WORK_END = 12500;
	/** 到岗后踱步半径（方块） / Pacing radius around the post when on duty (blocks) */
	private static final int WORK_PACE_RADIUS = 3;
	/** 到岗后每决策周期踱步概率（/256） / Pacing probability per decision cycle when on duty (/256) */
	private static final int WORK_PACE_CHANCE = 10;

	/**
	 * 处理本 tick 分片内所有活跃居民的行为决策。
	 * 遍历调度器的全部容器（每镇一份模拟 + 未托管命令居民）。
	 * <p>
	 * Runs behavior decisions for all active citizens in this tick's slice,
	 * iterating every container of the scheduler (one simulation per town plus
	 * the unmanaged command citizens).
	 *
	 * @param sched 调度器 / the scheduler
	 * @param level 维度 / the level
	 * @param slice 本 tick 负责的分片 / slice handled this tick
	 * @param gameTime 当前游戏时间 / current game time
	 */
	public void tick(CitizenSimScheduler sched, ServerLevel level, int slice, long gameTime) {
		boolean night = isNight(level);
		boolean workTime = isWorkTime(level);
		for (CitizenContainer c : sched.containers()) {
			CitizenSim sim = c.sim();
			int n = sim.size();
			for (int i = 0; i < n; i++) {
				if (sim.tickPhase[i] != slice)
					continue;
				if (!sched.isActive(c, i))
					continue;
				tickOne(sim, i, night, workTime, gameTime);
			}
		}
	}

	private void tickOne(CitizenSim sim, int i, boolean night, boolean workTime, long gameTime) {
		switch (sim.state[i]) {
		case CitizenState.IDLE:
			if (night) {
				startMove(sim, i, sim.homeX[i] << 10, sim.homeZ[i] << 10, CitizenState.RETURN_HOME, gameTime);
			} else if (workTime && sim.wx[i] != -1) {
				// 上班时间且有工作：优先去上班（通勤），闲逛让位
				startMove(sim, i, sim.wx[i] << 10, sim.wz[i] << 10, CitizenState.WORK, gameTime);
			} else if ((CitizenState.nextRand(sim.id[i], gameTime) & 0xFF) < 26) {
				// 约 10% 概率开始一次闲逛
				int r1 = CitizenState.nextRand(sim.id[i], gameTime + 1);
				int r2 = CitizenState.nextRand(sim.id[i], gameTime + 2);
				int ox = ((r1 & 0xFF) - 128) * WANDER_RADIUS / 128;
				int oz = ((r2 & 0xFF) - 128) * WANDER_RADIUS / 128;
				startMove(sim, i, (sim.homeX[i] + ox) << 10, (sim.homeZ[i] + oz) << 10, CitizenState.WANDER, gameTime);
			}
			break;
		case CitizenState.WANDER:
			if (night) {
				startMove(sim, i, sim.homeX[i] << 10, sim.homeZ[i] << 10, CitizenState.RETURN_HOME, gameTime);
			} else if (workTime && sim.wx[i] != -1) {
				startMove(sim, i, sim.wx[i] << 10, sim.wz[i] << 10, CitizenState.WORK, gameTime);
			} else if (sim.dir[i] == CitizenState.DIR_NONE) {
				// 已到达闲逛目标（移动系统停下了单位）
				sim.state[i] = CitizenState.IDLE;
			}
			break;
		case CitizenState.RETURN_HOME:
			if (sim.dir[i] == CitizenState.DIR_NONE) {
				sim.state[i] = night ? CitizenState.SLEEP : CitizenState.IDLE;
			}
			break;
		case CitizenState.SLEEP:
			if (!night)
				sim.state[i] = CitizenState.IDLE;
			break;
		case CitizenState.WORK:
			// 下班（夜晚或工作时段结束）优先于一切，直接回家
			if (night || !workTime) {
				startMove(sim, i, sim.homeX[i] << 10, sim.homeZ[i] << 10, CitizenState.RETURN_HOME, gameTime);
			} else if (sim.dir[i] == CitizenState.DIR_NONE) {
				// 已到岗：原地站岗；小概率在岗位 ±3 格内踱步（原地踱步，不离开岗位）
				int r = CitizenState.nextRand(sim.id[i], gameTime) & 0xFF;
				if (r < WORK_PACE_CHANCE) {
					int r1 = CitizenState.nextRand(sim.id[i], gameTime + 1);
					int r2 = CitizenState.nextRand(sim.id[i], gameTime + 2);
					int ox = ((r1 & 0xFF) - 128) * WORK_PACE_RADIUS / 128;
					int oz = ((r2 & 0xFF) - 128) * WORK_PACE_RADIUS / 128;
					startMove(sim, i, (sim.wx[i] + ox) << 10, (sim.wz[i] + oz) << 10, CitizenState.WORK, gameTime);
				}
			}
			break;
		default:
			sim.state[i] = CitizenState.IDLE;
		}
	}

	private void startMove(CitizenSim sim, int i, int targetX, int targetZ, byte newState, long gameTime) {
		sim.tx[i] = targetX + 512; // 目标方块中心
		sim.tz[i] = targetZ + 512;
		sim.state[i] = newState;
		sim.stuckTick[i] = (int) gameTime; // 新行程重置卡住计时 / reset the stuck timer for a new trip
		sim.bestDist2[i] = Long.MAX_VALUE; // 重置进度水位 / reset the progress watermark
	}

	/**
	 * 判断当前是否为夜晚（可睡觉时段）。
	 * <p>
	 * Whether it is currently night (sleep-able period).
	 *
	 * @param level 维度 / the level
	 * @return 夜晚返回 true / true at night
	 */
	public static boolean isNight(ServerLevel level) {
		long day = level.getDayTime() % 24000L;
		return day >= 12542 && day <= 23460;
	}

	/**
	 * 判断当前是否为工作时间（白天工作窗内）。
	 * <p>
	 * Whether it is currently work time (inside the daytime work window).
	 *
	 * @param level 维度 / the level
	 * @return 工作时段返回 true / true during work hours
	 */
	public static boolean isWorkTime(ServerLevel level) {
		long day = level.getDayTime() % 24000L;
		return day >= WORK_START && day < WORK_END;
	}
}
