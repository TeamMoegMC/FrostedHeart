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

/**
 * 居民行为状态常量与共享查表数据。
 * 所有状态以 byte 存储，客户端与服务端共用同一份方向/速度表，保证双方外推结果一致。
 * <p>
 * Citizen behavior state constants and shared lookup tables.
 * States are stored as bytes; both sides share identical direction/speed tables
 * so client-side extrapolation matches the server exactly.
 */
public final class CitizenState {

	private CitizenState() {
	}

	/** 定点数缩放：1 方块 = 1024 单位 / Fixed-point scale: 1 block = 1024 units */
	public static final int FIXED_SCALE = 1024;
	/** 无方向（静止）哨兵值 / Sentinel for "no direction" (stationary) */
	public static final byte DIR_NONE = (byte) 255;
    /** 到达判定距离（定点，1.5 方块） / Arrival threshold (fixed-point, 1.5 blocks) */
    public static final int ARRIVE_DIST2 = 1536 * 1536;

	/** 空闲 / Idle */
	public static final byte IDLE = 0;
	/** 闲逛移动中 / Wandering */
	public static final byte WANDER = 1;
	/** 回家途中 / Returning home */
	public static final byte RETURN_HOME = 2;
	/** 睡觉 / Sleeping */
	public static final byte SLEEP = 3;
	/** 工作 / Working */
	public static final byte WORK = 4;
	/** 状态总数（用于客户端配色表） / Total state count (used by client color table) */
	public static final int STATE_COUNT = 5;
	/** 状态展示名（调试与交互反馈用） / State display names (debug and interaction feedback) */
	public static final String[] STATE_NAMES = { "空闲", "闲逛", "回家", "睡觉", "工作" };

	/**
	 * 各状态移动速度，单位：定点单位/tick。0 表示该状态下不移动。
	 * WORK 为"通勤中或已到岗"：途中按此速移动，到岗（dir==DIR_NONE）即静止。
	 * <p>
	 * Movement speed per state, in fixed-point units per tick. 0 means the state is stationary.
	 * WORK means "commuting or on duty": moves at this speed while en route, stands when
	 * arrived (dir == DIR_NONE).
	 */
	public static final short[] SPEED = new short[STATE_COUNT];
	/** 是否为移动状态 / Whether the state involves movement */
	public static final boolean[] MOVING = new boolean[STATE_COUNT];

    /** 256 向方向表（yaw 0–255 → MC yaw 角度 → 位移分量定点值） */
    public static final int[] DIR_X_256 = new int[256];
    public static final int[] DIR_Z_256 = new int[256];

    /** 16 向索引 → 对应的 8‑bit yaw（0‑255），由 yawFromDir 预计算 */
    public static final byte[] DIR_TO_YAW = new byte[16];

	static {
		SPEED[IDLE] = 0;
		SPEED[WANDER] = 140; // ≈ 2.7 方块/秒
		SPEED[RETURN_HOME] = 200; // ≈ 3.9 方块/秒
		SPEED[SLEEP] = 0;
		SPEED[WORK] = 200; // ≈ 3.9 方块/秒（通勤；到岗后 dir==NONE 自然静止）
		for (int i = 0; i < STATE_COUNT; i++){
			MOVING[i] = SPEED[i] > 0;
        }

        for (int i = 0; i < 256; i++) {
            double mcYaw = (i & 0xFF) * 360.0 / 256.0; // 0 = south, clockwise
            double rad = Math.toRadians(mcYaw);
            // MC: x 方向为 -sin, z 方向为 cos
            DIR_X_256[i] = (int) Math.round(-Math.sin(rad) * FIXED_SCALE);
            DIR_Z_256[i] = (int) Math.round( Math.cos(rad) * FIXED_SCALE);
        }

        for (int i = 0; i < 16; i++) {
            DIR_TO_YAW[i] = yawFromDir(i);
        }
	}

	/**
	 * 由位移向量求 16 向方向索引。
	 * <p>
	 * Resolves a displacement vector to a 16-way direction index.
	 *
	 * @param dx X 位移（定点） / X displacement (fixed-point)
	 * @param dz Z 位移（定点） / Z displacement (fixed-point)
	 * @return 0–15 方向索引 / direction index 0–15
	 */
	public static int dirFromVector(int dx, int dz) {
		return (int) Math.round(Math.atan2(dz, dx) * 8.0 / Math.PI) & 15;
	}

	/**
	 * 由 16 向方向索引求量化 yaw 字节（0–255 映射 0–360°）。
	 * <p>
	 * Converts a 16-way direction index to a quantized yaw byte (0–255 maps to 0–360°).
	 * MC 朝向约定：yaw=0 朝 +Z（南），顺时针增加 / MC convention: yaw 0 faces +Z (south), increasing clockwise.
	 *
	 * @param dir 方向索引 / direction index
	 * @return 量化 yaw / quantized yaw
	 */
	public static byte yawFromDir(int dir) {
		double angleDeg = Math.toDegrees(dir * Math.PI / 8.0);
		// 数学角（+X 为 0）转 MC yaw：east(+X)=-90°，south(+Z)=0°，即 mcYaw = angleDeg - 90
		double mcYaw = angleDeg - 90.0;
		return (byte) (int) Math.round(mcYaw * 256.0 / 360.0);
	}

	/**
	 * 确定性随机数：由居民 id 与游戏时间驱动的 xorshift32。
	 * 保证断线重连与重放后行为一致。
	 * <p>
	 * Deterministic xorshift32 random driven by citizen id and game time,
	 * ensuring consistent behavior across reconnects and replays.
	 *
	 * @param id 居民 id / citizen id
	 * @param tick 当前游戏时间 / current game time
	 * @return 伪随机整数 / pseudo-random integer
	 */
	public static int nextRand(int id, long tick) {
		int x = id * 0x9E3779B1 ^ (int) tick;
		x ^= x << 13;
		x ^= x >>> 17;
		x ^= x << 5;
		return x;
	}
}
