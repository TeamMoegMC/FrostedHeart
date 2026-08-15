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
import net.minecraft.client.Minecraft;

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
 *
 * <p>朝向现为 8‑bit 连续 yaw（0‑255 ≈ 0‑360°），直接从服务端接收，客户端不做
 * 任何过滤或防抖，彻底消除转弯冻结与抖动。</p>
 */
public final class ClientCitizen {

    /**
     * 外推钳制时长（秒）。与服务端最远档心跳（20 tick = 1 秒，最粗节奏）对齐
     * 并留余量，保证心跳更新到来前外推持续不中断，避免"跑几步就停住"的现象。
     */
    private static final double EXTRAPOLATE_CLAMP = 1.5;

    public final int id;
    /** 真实姓名（spawn 包同步，城镇托管居民）；空串 = 未托管，回退 CitizenNames 派生名 */
    public final String name;

    /** 上一快照 / Previous snapshot */
    public double x0, y0, z0;
    /** 最新快照 / Latest snapshot */
    public double x1, y1, z1;

    /** 连续朝向 (0‑255) / Continuous yaw (0‑255) */
    public byte yaw;
    /** 行为状态 / Behavior state */
    public byte state;

    /** 快照到达时间（游戏时间秒） / Snapshot arrival times (game-time seconds) */
    private double t0, t1;
    private final double[] posBuf = new double[3];

    ClientCitizen(int id, int px, int py, int pz, byte yaw, byte state, String name) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.x0 = this.x1 = px / 1024.0;
        this.y0 = this.y1 = py / 1024.0;
        this.z0 = this.z1 = pz / 1024.0;
        this.yaw = yaw;
        this.state = state;
        this.t0 = this.t1 = now();
    }

    /**
     * 快照到达：用当前渲染位置作为新插值起点，保证位置连续。
     * @param px 绝对定点 X
     * @param py 绝对定点 Y
     * @param pz 绝对定点 Z
     * @param yaw 连续朝向 (0‑255)
     * @param state 行为状态
     */
    void update(int px, int py, int pz, byte yaw, byte state) {
        double[] cur = renderPos();
        double now = now();
        double prevGap = now - this.t0;
        this.x0 = cur[0];
        this.y0 = cur[1];
        this.z0 = cur[2];
        this.t0 = now;
        this.t1 = now + Math.max(prevGap, 0.05);
        this.x1 = px / 1024.0;
        this.y1 = py / 1024.0;
        this.z1 = pz / 1024.0;
        this.yaw = yaw;
        this.state = state;
    }

    /**
     * 当前是否处于移动状态。仅根据 state 判断，不再依赖哨兵 dir。
     */
    public boolean isMoving() {
        int s = state & 0xFF;
        return s < CitizenState.STATE_COUNT && CitizenState.MOVING[s];
    }

    /**
     * 计算当前渲染位置（插值 + 外推）。
     */
    public double[] renderPos() {
        double now = now();
        double interval = t1 - t0;
        if (interval < 0.05) interval = 0.05;
        else if (interval > 1.0) interval = 1.0;

        double g = (now - t0) / interval;
        if (g > 1.0) g = 1.0;

        double x = x0 + (x1 - x0) * g;
        double y = y0 + (y1 - y0) * g;
        double z = z0 + (z1 - z0) * g;

        if (isMoving()) {
            double extra = now - t1;
            if (extra > EXTRAPOLATE_CLAMP) extra = EXTRAPOLATE_CLAMP;
            if (extra > 0 && interval > 0.35) {
                int idx = yaw & 0xFF;
                double speed = CitizenState.SPEED[state & 0xFF] * 20.0 / CitizenState.FIXED_SCALE;
                x += CitizenState.DIR_X_256[idx] / 1024.0 * speed * extra;
                z += CitizenState.DIR_Z_256[idx] / 1024.0 * speed * extra;
            }
        }
        posBuf[0] = x;
        posBuf[1] = y;
        posBuf[2] = z;
        return posBuf;
    }

    // 当前游戏时间（秒），暂停时不增长。与下方全部秒制常量（EXTRAPOLATE_CLAMP=1.5、
    // 窗口钳制 0.05~1.0、外推门限 interval>0.35）一致；帧时间小数部分使包间间隔
    // 精确到亚 tick。曾误返回 tick（回归 B：窗口坍缩为 1 tick，渲染位置分段冻结/瞬移）。
    // Current game time in seconds (pause-aware). Matches the second-scale constants
    // below; the frame-time fraction keeps inter-packet gaps sub-tick accurate.
    private static double now() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0.0;
        return (mc.level.getGameTime() + mc.getFrameTime()) / 20.0;
    }
}
