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
import net.minecraft.world.phys.AABB;

/**
 * 客户端居民渲染状态，快照插值 + 16 向方向外推。
 * 渲染位置 = lerp(上一帧渲染位置（含外推尾巴）, 最新快照) + 超出窗口后沿当前方向
 * 按状态速度外推（钳制 {@value #EXTRAPOLATE_CLAMP} 秒）：新快照到达时以当前渲染
 * 位置为新插值起点，位置连续不瞬移，与服务端 Dead Reckoning 模型严格同源。
 * <p>
 * Client-side citizen render state: snapshot interpolation plus directional
 * extrapolation along the synced 16-way direction, strictly mirroring the
 * server-side dead-reckoning model.
 *
 * <p><b>朝向模型：</b>网络只同步 16 向移动方向 {@link #dir}（与状态打包为一个字节），
 * 连续视觉朝向 {@link #visYaw}（0-255）由客户端本地闭环追赶生成——按游戏时间步进，
 * 卡顿不积累误差，收敛必然。dir 变化时按实测包间隔 {@code prevGap} 预推进
 * （回溯转向），抵消一个发包档位的网络延迟。该值纯属渲染，位置外推永远走
 * 精确的 DIR_X_16/DIR_Z_16 查表。</p>
 */
public final class ClientCitizen {

    /**
     * 外推钳制时长（秒）。与服务端最远档心跳（20 tick = 1 秒，最粗节奏）对齐
     * 并留余量，保证心跳更新到来前外推持续不中断，避免"跑几步就停住"的现象。
     */
    private static final double EXTRAPOLATE_CLAMP = 1.5;

    /** 视觉转向基础速率（visYaw 步/秒）：3 步/tick，与原服务端软转向一致 */
    private static final double TURN_RATE = 60.0;
    /** 大角度转向加速倍率：|diff| 超过 32 步（45°）时追赶提速，纯视觉收敛 */
    private static final double TURN_RATE_FAST = TURN_RATE * 4.0;

    public final int id;
    /** 真实姓名（spawn 包同步，城镇托管居民）；空串 = 未托管，回退 CitizenNames 派生名 */
    public final String name;

    /** 上一快照 / Previous snapshot */
    public double x0, y0, z0;
    /** 最新快照 / Latest snapshot */
    public double x1, y1, z1;

    /** 16 向移动方向索引（0-15），网络同步值 / Synced 16-way direction index (0-15) */
    public int dir;
    /** 行为状态 / Behavior state */
    public byte state;
    /** 停步标记：移动类状态但服务端实测未位移（到岗/卡住/贴墙），见到即停止外推 / Halt flag: MOVING-class state but no actual displacement server-side */
    private boolean halt;

    /** 批量渲染光照缓存；包可见以便同包渲染器无映射表读取。 */
    int packedLight;
    int lightBlockX = Integer.MIN_VALUE;
    int lightBlockY = Integer.MIN_VALUE;
    int lightBlockZ = Integer.MIN_VALUE;
    long nextLightSampleTick = Long.MIN_VALUE;

    /** 客户端本地连续视觉朝向（0-255），闭环追赶 DIR_TO_YAW[dir]，纯渲染用 */
    private int visYaw;
    /** visYaw 上次步进的游戏时刻（秒）与零头累积器（亚步进度不丢帧） */
    private double visYawLast = -1, turnAccum;
    /** 快照到达时间（游戏时间秒） / Snapshot arrival times (game-time seconds) */
    private double t0, t1;
	/** Shared CPU/Flywheel walk phase at the start of the current snapshot segment. */
	private float walkPhase;
    private final double[] posBuf = new double[3];
	/** Snapshot-swept frustum bounds, materialized only when the CPU renderer needs them. */
	private AABB cullBox;
	private boolean cullBoxDirty = true;

    ClientCitizen(int id, int px, int py, int pz, byte stateDir, String name) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.x0 = this.x1 = px / 1024.0;
        this.y0 = this.y1 = py / 1024.0;
        this.z0 = this.z1 = pz / 1024.0;
        this.dir = CitizenState.unpackDir(stateDir);
        this.state = (byte) CitizenState.unpackState(stateDir);
        this.halt = CitizenState.unpackHalt(stateDir);
		this.walkPhase = CitizenBatchRenderLayout.initialWalkPhase(id);
        // spawn 直接对齐目标朝向：该客户端没有任何历史朝向，"从旧方向过渡"
        // 的旧方向根本不存在，snap 不可感知且是唯一无争议的选择。
        this.visYaw = CitizenState.DIR_TO_YAW[this.dir] & 0xFF;
        this.t0 = this.t1 = now();
    }

    /**
     * 快照到达：用当前渲染位置作为新插值起点，保证位置连续。
     * @param px 绝对定点 X
     * @param py 绝对定点 Y
     * @param pz 绝对定点 Z
     * @param stateDir 状态+方向打包字节
     */
    void update(int px, int py, int pz, byte stateDir) {
        double now = now();
        advanceVisualYaw(now);
		walkPhase = CitizenBatchRenderLayout.advanceWalkPhase(walkPhase, x0, z0, x1, z1);
        int newDir = CitizenState.unpackDir(stateDir);
        byte newState = (byte) CitizenState.unpackState(stateDir);
        boolean sleepTransition = newState != this.state
                && (newState == CitizenState.SLEEP || this.state == CitizenState.SLEEP);
        if (sleepTransition) {
            this.x0 = this.x1 = px / 1024.0;
            this.y0 = this.y1 = py / 1024.0;
            this.z0 = this.z1 = pz / 1024.0;
            this.t0 = this.t1 = now;
            this.dir = newDir;
            this.state = newState;
            this.halt = CitizenState.unpackHalt(stateDir);
            this.visYaw = CitizenState.DIR_TO_YAW[newDir] & 0xFF;
            this.visYawLast = now;
            this.turnAccum = 0;
			invalidateCullBox();
            return;
        }
        double[] cur = renderPos();
        double prevGap = now - this.t0;
        this.x0 = cur[0];
        this.y0 = cur[1];
        this.z0 = cur[2];
        this.t0 = now;
        this.t1 = now + Math.max(prevGap, 0.05);
        this.x1 = px / 1024.0;
        this.y1 = py / 1024.0;
        this.z1 = pz / 1024.0;
        if (newDir != this.dir) {
            // 回溯转向：dir 变化经过一个发包档位的网络延迟才到达，假设服务端
            // 在上一个快照后即开始转向，按实测包间隔 prevGap 预推进 visYaw
            // （封顶不越过目标）。零带宽开销抹平转向延迟。
            int target = CitizenState.DIR_TO_YAW[newDir] & 0xFF;
            int diff = target - visYaw;
            if (diff > 128) diff -= 256;
            else if (diff < -128) diff += 256;
            int advance = (int) (prevGap * TURN_RATE);
            if (diff > 0) visYaw = (visYaw + Math.min(diff, advance)) & 0xFF;
            else if (diff < 0) visYaw = (visYaw + Math.max(diff, -advance)) & 0xFF;
        }
        this.dir = newDir;
        this.state = newState;
        this.halt = CitizenState.unpackHalt(stateDir);
		invalidateCullBox();
    }

    /**
     * 当前是否实际在移动。state 为移动类且服务端未标停步（halt）才算——
     * 到岗站立的 WORK、卡住、贴墙钳制的居民不外推，消除"漂移↔回拉"振荡。
     */
    public boolean isMoving() {
        int s = state & 0xFF;
        return s < CitizenState.STATE_COUNT && CitizenState.MOVING[s] && !halt;
    }

    /**
     * 连续视觉朝向（0-255），供渲染使用。
     * 闭环追赶 DIR_TO_YAW[dir]：短路径角差，基础速率 {@value #TURN_RATE} 步/秒，
     * 大角度（&gt;45°）加速至 {@value #TURN_RATE_FAST}。步进按游戏时间计，
     * 客户端卡顿时不积累偏差，恢复后自动补进度；误差单调收敛，不存在漂移。
     */
    public int visualYaw() {
        return advanceVisualYaw(now());
    }

    private int advanceVisualYaw(double now) {
        if (visYawLast < 0) {
            visYawLast = now;
            return visYaw;
        }
        double dt = now - visYawLast;
        visYawLast = now;
        if (dt <= 0)
            return visYaw;
        int target = CitizenState.DIR_TO_YAW[dir] & 0xFF;
        int diff = target - visYaw;
        if (diff > 128) diff -= 256;
        else if (diff < -128) diff += 256;
        if (diff == 0)
            return visYaw;
        turnAccum += dt * (Math.abs(diff) > 32 ? TURN_RATE_FAST : TURN_RATE);
        int step = (int) turnAccum;
        turnAccum -= step;
        if (step <= 0)
            return visYaw;
        if (diff > 0) visYaw = (visYaw + Math.min(diff, step)) & 0xFF;
        else visYaw = (visYaw + Math.max(diff, -step)) & 0xFF;
        return visYaw;
    }

	double snapshotStartSeconds() {
		return t0;
	}

	double snapshotEndSeconds() {
		return t1;
	}

	float walkPhase() {
		return walkPhase;
	}

	static double currentTimeSeconds() {
		return now();
	}

    /**
     * 计算当前渲染位置（插值 + 沿 16 向同步方向外推，与服务端规范模型严格同源）。
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
            // 外推无条件启用（extra>0 即插值窗口已耗尽、下一包迟到）：
            // 服务端 Dead Reckoning 模型假设客户端时刻沿 dir 外推，客户端必须同源。
            // 旧实现有 interval>0.35 的门限，近距档位（窗口 0.2s）永远不外推——
            // 包稍晚到渲染就冻结在快照点，包到达后 lerp 加速追赶，
            // "冻结→追赶"的碎步在视觉上就是小幅度瞬移。
            if (extra > 0) {
                double speed = CitizenState.SPEED[state & 0xFF] * 20.0 / CitizenState.FIXED_SCALE;
                x += CitizenState.DIR_X_16[dir] / 1024.0 * speed * extra;
                z += CitizenState.DIR_Z_16[dir] / 1024.0 * speed * extra;
            }
        }
        posBuf[0] = x;
        posBuf[1] = y;
        posBuf[2] = z;
        return posBuf;
    }

	AABB cullingBox() {
		if (cullBoxDirty) {
			cullBox = createCullingBox(x0, y0, z0, x1, y1, z1, state & 0xFF, dir, halt);
			cullBoxDirty = false;
		}
		return cullBox;
	}

	private void invalidateCullBox() {
		cullBoxDirty = true;
	}

	static AABB createCullingBox(double x0, double y0, double z0,
			double x1, double y1, double z1, int state, int dir, boolean halt) {
		double extrapolatedX = x1;
		double extrapolatedZ = z1;
		boolean moving = state < CitizenState.STATE_COUNT && CitizenState.MOVING[state] && !halt;
		if (moving) {
			double speed = CitizenState.SPEED[state] * 20.0 / CitizenState.FIXED_SCALE;
			extrapolatedX += CitizenState.DIR_X_16[dir] / 1024.0 * speed * EXTRAPOLATE_CLAMP;
			extrapolatedZ += CitizenState.DIR_Z_16[dir] / 1024.0 * speed * EXTRAPOLATE_CLAMP;
		}
		double minX = Math.min(Math.min(x0, x1), extrapolatedX);
		double maxX = Math.max(Math.max(x0, x1), extrapolatedX);
		double minY = Math.min(y0, y1);
		double maxY = Math.max(y0, y1);
		double minZ = Math.min(Math.min(z0, z1), extrapolatedZ);
		double maxZ = Math.max(Math.max(z0, z1), extrapolatedZ);
		if (state == CitizenState.SLEEP) {
			return new AABB(minX - 1.35, minY + 0.45, minZ - 1.35,
					maxX + 1.35, maxY + 0.95, maxZ + 1.35);
		}
		return new AABB(minX - 0.5, minY, minZ - 0.5,
				maxX + 0.5, maxY + 2.0, maxZ + 0.5);
	}

    // 当前游戏时间（秒），暂停时不增长。与全部秒制常量（EXTRAPOLATE_CLAMP=1.5、
    // 窗口钳制 0.05~1.0、TURN_RATE=60/s）一致；帧时间小数部分使包间间隔
    // 精确到亚 tick。曾误返回 tick（回归 B：窗口坍缩为 1 tick，渲染位置分段冻结/瞬移）。
    // Current game time in seconds (pause-aware). Matches the second-scale constants
    // below; the frame-time fraction keeps inter-packet gaps sub-tick accurate.
    private static double now() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return 0.0;
        return (mc.level.getGameTime() + mc.getFrameTime()) / 20.0;
    }
}
