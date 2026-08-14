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

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;

/**
 * 居民模拟核心数据，SoA（Structure-of-Arrays）紧排布局。
 * 位置使用 1/1024 方块精度的定点数；索引仅在运行期有效，
 * 删除采用"末尾交换"策略 O(1) 完成，外部引用一律使用稳定 id。
 * 一万居民的热数据约 600KB，顺序扫描对 CPU 缓存友好，几乎无 GC 压力。
 * <p>
 * Core citizen simulation data in Structure-of-Arrays layout.
 * Positions are fixed-point at 1/1024 block precision. Indices are runtime-only;
 * removal is O(1) swap-remove, external references must use the stable id.
 * Hot data for 10k citizens is ~600KB, cache-friendly and nearly GC-free.
 */
public final class CitizenSim {

	private int size;
	private int capacity;
	/** 外部稳定 id（自增，永不复用） / External stable id (auto-increment, never reused) */
	public int[] id;
	/** 位置（定点 1/1024 方块） / Position (fixed-point 1/1024 block) */
	public int[] px, py, pz;
	/** 量化朝向 / Quantized yaw */
	public byte[] yaw;
    // Dead Reckoning canonical yaw，客户端应该的规范朝向
    public byte[] syaw;
	/** 行为状态，见 {@link CitizenState} / Behavior state, see {@link CitizenState} */
	public byte[] state;
	/** 归属性锚点（家），方块坐标 / Home anchor, block coordinates */
	public int[] homeX, homeZ;
	/** 工作锚点（方块坐标），-1 = 无工作（命令生成/失业居民）；运行期数据，不落盘（重启后由人口对齐重建） / Work anchor (block coords), -1 = no job (command-spawned/unemployed); runtime-only, rebuilt by the population align after restart */
	public int[] wx, wz;
	/** 代表的城镇居民 UUID 两段（0/0 = 未托管的命令居民） / Town resident UUID halves (0/0 = unmanaged command citizen) */
	public long[] uuidHi, uuidLo;
	/** 当前移动目标（定点坐标） / Current movement target (fixed-point coordinates) */
	public int[] tx, ty, tz;
	/** 分帧相位：id % SLICE / Time-slicing phase: id % SLICE */
	public byte[] tickPhase;
	/** Dead Reckoning 规范模型：上次广播的位置/方向/状态/时刻 / Dead-reckoning canonical model: last broadcast pos/dir/state/time */
	public int[] sx, sy, sz;
	public byte[] sstate;
	public long[] stick;
	/** 卡住计时：上次取得进度（dist2 水位下降）的游戏时刻（int 截断，0=未初始化）；运行期数据，不落盘 / Stuck timer: game time of last progress (dist2 watermark drop) (truncated int, 0 = uninitialized); runtime-only, not persisted */
	public int[] stuckTick;
	/** 行程最小目标距离平方（卡住判定水位，突破才计进度）；运行期数据，不落盘 / Journey min dist2 watermark (stuck detection: only a lower dist2 counts as progress); runtime-only, not persisted */
	public long[] bestDist2;

	private final Int2IntOpenHashMap idToIndex = new Int2IntOpenHashMap();

	public CitizenSim(int initialCapacity) {
		this.capacity = Math.max(16, initialCapacity);
		allocate(capacity);
		Arrays.fill(wx, -1);
		Arrays.fill(wz, -1);
		idToIndex.defaultReturnValue(-1);
	}

	private void allocate(int cap) {
		id = new int[cap];
		px = new int[cap];
		py = new int[cap];
		pz = new int[cap];
		yaw = new byte[cap];
        syaw = new byte[cap];
        state = new byte[cap];
		homeX = new int[cap];
		homeZ = new int[cap];
		wx = new int[cap];
		wz = new int[cap];
		uuidHi = new long[cap];
		uuidLo = new long[cap];
		tx = new int[cap];
		ty = new int[cap];
		tz = new int[cap];
		tickPhase = new byte[cap];
		sx = new int[cap];
		sy = new int[cap];
		sz = new int[cap];
		sstate = new byte[cap];
		stick = new long[cap];
		stuckTick = new int[cap];
		bestDist2 = new long[cap];
	}

	private void grow() {
		int newCap = capacity * 2;
		id = Arrays.copyOf(id, newCap);
		px = Arrays.copyOf(px, newCap);
		py = Arrays.copyOf(py, newCap);
		pz = Arrays.copyOf(pz, newCap);
		yaw = Arrays.copyOf(yaw, newCap);
        syaw = Arrays.copyOf(syaw, newCap);
		state = Arrays.copyOf(state, newCap);
		homeX = Arrays.copyOf(homeX, newCap);
		homeZ = Arrays.copyOf(homeZ, newCap);
		wx = Arrays.copyOf(wx, newCap);
		wz = Arrays.copyOf(wz, newCap);
		uuidHi = Arrays.copyOf(uuidHi, newCap);
		uuidLo = Arrays.copyOf(uuidLo, newCap);
		tx = Arrays.copyOf(tx, newCap);
		ty = Arrays.copyOf(ty, newCap);
		tz = Arrays.copyOf(tz, newCap);
		tickPhase = Arrays.copyOf(tickPhase, newCap);
		sx = Arrays.copyOf(sx, newCap);
		sy = Arrays.copyOf(sy, newCap);
		sz = Arrays.copyOf(sz, newCap);
		sstate = Arrays.copyOf(sstate, newCap);
		stick = Arrays.copyOf(stick, newCap);
		stuckTick = Arrays.copyOf(stuckTick, newCap);
		bestDist2 = Arrays.copyOf(bestDist2, newCap);
		capacity = newCap;
	}

	/**
	 * 新增一个居民。
	 * <p>
	 * Adds a citizen.
	 *
	 * @param newId 稳定 id / stable id
	 * @param x 出生 X（定点） / spawn X (fixed-point)
	 * @param y 出生 Y（定点） / spawn Y (fixed-point)
	 * @param z 出生 Z（定点） / spawn Z (fixed-point)
	 * @param phase 分帧相位 / time-slicing phase
	 * @return 运行期索引 / runtime index
	 */
	public int add(int newId, int x, int y, int z, byte phase) {
		if (size == capacity)
			grow();
		int i = size++;
		id[i] = newId;
		px[i] = x;
		py[i] = y;
		pz[i] = z;
		homeX[i] = x >> 10;
		homeZ[i] = z >> 10;
		wx[i] = -1;
		wz[i] = -1;
		uuidHi[i] = 0;
		uuidLo[i] = 0;
		tx[i] = x;
		ty[i] = y;
		tz[i] = z;
		yaw[i] = 0;
        syaw[i] = 0;
        state[i] = CitizenState.IDLE;
		tickPhase[i] = phase;
		sx[i] = x;
		sy[i] = y;
		sz[i] = z;
		sstate[i] = CitizenState.IDLE;
		stick[i] = 0;
		stuckTick[i] = 0;
		bestDist2[i] = 0;
		idToIndex.put(newId, i);
		return i;
	}

	/**
	 * 按稳定 id 删除居民（末尾交换，O(1)）。
	 * <p>
	 * Removes a citizen by stable id (swap-remove, O(1)).
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 是否删除成功 / whether removal succeeded
	 */
	public boolean remove(int citizenId) {
		int i = idToIndex.remove(citizenId);
		if (i < 0)
			return false;
		int last = --size;
		if (i != last) {
			id[i] = id[last];
			px[i] = px[last];
			py[i] = py[last];
			pz[i] = pz[last];
			yaw[i] = yaw[last];
            syaw[i] = syaw[last];
			state[i] = state[last];
			homeX[i] = homeX[last];
			homeZ[i] = homeZ[last];
			wx[i] = wx[last];
			wz[i] = wz[last];
			uuidHi[i] = uuidHi[last];
			uuidLo[i] = uuidLo[last];
			tx[i] = tx[last];
			ty[i] = ty[last];
			tz[i] = tz[last];
			tickPhase[i] = tickPhase[last];
			sx[i] = sx[last];
			sy[i] = sy[last];
			sz[i] = sz[last];
			sstate[i] = sstate[last];
			stick[i] = stick[last];
			stuckTick[i] = stuckTick[last];
			bestDist2[i] = bestDist2[last];
			idToIndex.put(id[i], i);
		}
		return true;
	}

	/**
	 * 由稳定 id 查运行期索引。
	 * <p>
	 * Looks up the runtime index by stable id.
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 索引，不存在返回 -1 / index, or -1 if absent
	 */
	public int indexOf(int citizenId) {
		return idToIndex.get(citizenId);
	}

	/**
	 * 按城镇居民 UUID（两段 long）查运行期索引；未找到返回 -1。
	 * 线性扫描——仅在 1Hz 人口对齐时调用，城镇规模（几十~几百）下开销可忽略。
	 * <p>
	 * Finds the runtime index by town resident UUID halves; -1 if absent.
	 * Linear scan — called only by the 1 Hz population align, negligible at town scale.
	 *
	 * @param hi UUID 高位 / UUID most significant bits
	 * @param lo UUID 低位 / UUID least significant bits
	 * @return 索引，不存在返回 -1 / index, or -1 if absent
	 */
	public int findByUuid(long hi, long lo) {
		for (int i = 0; i < size; i++) {
			if (uuidHi[i] == hi && uuidLo[i] == lo)
				return i;
		}
		return -1;
	}

	/**
	 * 当前存活居民数量。
	 * <p>
	 * Current number of living citizens.
	 *
	 * @return 数量 / count
	 */
	public int size() {
		return size;
	}

	/**
	 * 清空全部居民（每次移除都走 swap-remove，id 映射同步维护）。
	 * 用于跨维度全量重生（onLevelChange）——旧条目逐条经 remove 清理，
	 * 调用方需自行处理外部引用（nameCache/tradeData/同步通知）。
	 * <p>
	 * Removes all citizens (each removal is a swap-remove keeping the id map
	 * in sync). Used for full respawn across dimensions (onLevelChange) —
	 * callers must clean up external references (nameCache/tradeData/sync).
	 */
	public void clear() {
		while (size > 0) {
			remove(id[0]);
		}
	}

	/**
	 * 序列化到 NBT（按 SoA 数组直写，万级落盘 &lt; 1MB）。
	 * <p>
	 * Serializes to NBT (raw SoA arrays, &lt; 1MB for 10k citizens).
	 *
	 * @param tag 目标标签 / target tag
	 * @return 写入后的标签 / the written tag
	 */
	public CompoundTag save(CompoundTag tag) {
		tag.putInt("size", size);
		tag.putIntArray("id", Arrays.copyOf(id, size));
		tag.putIntArray("px", Arrays.copyOf(px, size));
		tag.putIntArray("py", Arrays.copyOf(py, size));
		tag.putIntArray("pz", Arrays.copyOf(pz, size));
		tag.putByteArray("yaw", Arrays.copyOf(yaw, size));
        tag.putByteArray("syaw", Arrays.copyOf(syaw, size));
		tag.putByteArray("state", Arrays.copyOf(state, size));
		tag.putIntArray("homeX", Arrays.copyOf(homeX, size));
		tag.putIntArray("homeZ", Arrays.copyOf(homeZ, size));
		tag.putLongArray("uuidHi", Arrays.copyOf(uuidHi, size));
		tag.putLongArray("uuidLo", Arrays.copyOf(uuidLo, size));
		tag.putIntArray("tx", Arrays.copyOf(tx, size));
		tag.putIntArray("ty", Arrays.copyOf(ty, size));
		tag.putIntArray("tz", Arrays.copyOf(tz, size));
		return tag;
	}

	/**
	 * 从 NBT 反序列化。
	 * <p>
	 * Deserializes from NBT.
	 *
	 * @param tag 源标签 / source tag
	 * @return 恢复后的实例 / the restored instance
	 */
	public static CitizenSim load(CompoundTag tag) {
		CitizenSim sim = new CitizenSim(tag.getInt("size"));
		int n = tag.getInt("size");
		int[] ids = tag.getIntArray("id");
		int[] apx = tag.getIntArray("px");
		int[] apy = tag.getIntArray("py");
		int[] apz = tag.getIntArray("pz");
		byte[] ayaw = tag.getByteArray("yaw");
        byte[] asyaw = tag.getByteArray("syaw");
		byte[] astate = tag.getByteArray("state");
		int[] ahomeX = tag.getIntArray("homeX");
		int[] ahomeZ = tag.getIntArray("homeZ");
		// 旧存档无 uuid 数组 → 空数组，全部视为未托管命令居民（向后兼容）
		long[] auuidHi = tag.getLongArray("uuidHi");
		long[] auuidLo = tag.getLongArray("uuidLo");
		int[] atx = tag.getIntArray("tx");
		int[] aty = tag.getIntArray("ty");
		int[] atz = tag.getIntArray("tz");
		for (int k = 0; k < n; k++) {
			int i = sim.add(ids[k], apx[k], apy[k], apz[k], (byte) (ids[k] % 20));
			sim.yaw[i] = ayaw[k];
            sim.syaw[i] = asyaw[k];
			sim.state[i] = astate[k];
			sim.homeX[i] = ahomeX[k];
			sim.homeZ[i] = ahomeZ[k];
			if (k < auuidHi.length)
				sim.uuidHi[i] = auuidHi[k];
			if (k < auuidLo.length)
				sim.uuidLo[i] = auuidLo[k];
			sim.tx[i] = atx[k];
			sim.ty[i] = aty[k];
			sim.tz[i] = atz[k];
		}
		return sim;
	}
}
