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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.teammoeg.frostedheart.content.town.resident.Resident;

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
	/** Sleeping citizen is anchored to a currently verified bed head. */
	public static final byte PRESENT_ON_VALID_BED = 1;
	private static final int PRESENTATION_AGE_SHIFT = 1;
	private static final int PRESENTATION_AGE_MASK = 0b110;
	/** 瞬态完整住宅锚点缺失值 / Sentinel for a missing transient full home anchor. */
	public static final long NO_HOME_POS = Long.MIN_VALUE;

	private int size;
	private int capacity;
	/** 外部稳定 id（自增，永不复用） / External stable id (auto-increment, never reused) */
	public int[] id;
	/** 位置（定点 1/1024 方块） / Position (fixed-point 1/1024 block) */
	public int[] px, py, pz;
	/** 16 向移动方向索引（0–15），默认 4 = 南（+Z）；服务端不存连续 yaw——视觉软转向在客户端本地完成 / 16-way direction index (0–15), default 4 = south (+Z); the server stores no continuous yaw — visual soft-turning is client-local */
	public byte[] dir;
    // Dead Reckoning canonical dir，客户端应该的规范方向
    public byte[] sdir;
	/** 行为状态，见 {@link CitizenState} / Behavior state, see {@link CitizenState} */
	public byte[] state;
	/** 归属性锚点（家），方块坐标 / Home anchor, block coordinates */
	public int[] homeX, homeZ;
	/**
	 * 完整住宅锚点（瞬态 packed BlockPos）；旧的 homeX/homeZ 继续承担持久化和
	 * 无住宅布局时的导航回退。/ Full home anchor (transient packed BlockPos);
	 * homeX/homeZ remain the persisted navigation fallback.
	 */
	public long[] homePos;
	/**
	 * 住宅内按 UUID 排序的稳定序号（瞬态，-1 = 无住宅）；供床位与无冲突出口共同使用，不落盘。
	 * / Stable UUID-sorted ordinal inside the home (transient, -1 = no home),
	 * shared by bed assignment and collision-free exit placement.
	 */
	public int[] homeSlot;
	/** Transient presentation bits: valid-bed state plus the Resident age group; never persisted. */
	public byte[] presentationFlags;
	/** 工作锚点（方块坐标），-1 = 无工作（命令生成/失业居民）；运行期数据，不落盘（重启后由人口对齐重建） / Work anchor (block coords), -1 = no job (command-spawned/unemployed); runtime-only, rebuilt by the population align after restart */
	public int[] wx, wz;
	/** 代表的城镇居民 UUID 两段（0/0 = 未托管的命令居民） / Town resident UUID halves (0/0 = unmanaged command citizen) */
	public long[] uuidHi, uuidLo;
	/** 当前移动目标（定点坐标） / Current movement target (fixed-point coordinates) */
	public int[] tx, tz;
	/** Dead Reckoning 规范模型：上次广播的位置/方向/状态/时刻 / Dead-reckoning canonical model: last broadcast pos/dir/state/time */
	public int[] sx, sy, sz;
	public byte[] sstate;
	public long[] stick;
	/** 卡住计时：上次取得进度（dist2 水位下降）的游戏时刻（int 截断，0=未初始化）；运行期数据，不落盘 / Stuck timer: game time of last progress (dist2 watermark drop) (truncated int, 0 = uninitialized); runtime-only, not persisted */
	public int[] stuckTick;
	/** 行程最小目标距离平方（卡住判定水位，突破才计进度）；运行期数据，不落盘 / Journey min dist2 watermark (stuck detection: only a lower dist2 counts as progress); runtime-only, not persisted */
	public long[] bestDist2;
    /** 分离力缓存（定点位移，非计算 tick 复用）；瞬态，不落盘 */
    public int[] sepX, sepZ;
    /** 本 tick 停步标记：移动类状态但 XZ 无实际位移（到岗/卡住/贴墙/未激活）；运行期，不落盘 / Per-tick halt flag: MOVING-class state with no XZ displacement; runtime-only */
    public byte[] halt;
    /** 规范模型的停步标记（上次发送值）；运行期，不落盘 / Canonical halt flag (last sent); runtime-only */
    public byte[] shalt;

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
		dir = new byte[cap];
        sdir = new byte[cap];
        state = new byte[cap];
		homeX = new int[cap];
		homeZ = new int[cap];
		homePos = new long[cap];
		homeSlot = new int[cap];
		presentationFlags = new byte[cap];
		wx = new int[cap];
		wz = new int[cap];
		uuidHi = new long[cap];
		uuidLo = new long[cap];
		tx = new int[cap];
		tz = new int[cap];
		sx = new int[cap];
		sy = new int[cap];
		sz = new int[cap];
		sstate = new byte[cap];
		stick = new long[cap];
		stuckTick = new int[cap];
		bestDist2 = new long[cap];
        sepX = new int[cap];
        sepZ = new int[cap];
        halt = new byte[cap];
        shalt = new byte[cap];
	}

	private void grow() {
		int newCap = capacity * 2;
		id = Arrays.copyOf(id, newCap);
		px = Arrays.copyOf(px, newCap);
		py = Arrays.copyOf(py, newCap);
		pz = Arrays.copyOf(pz, newCap);
		dir = Arrays.copyOf(dir, newCap);
        sdir = Arrays.copyOf(sdir, newCap);
		state = Arrays.copyOf(state, newCap);
		homeX = Arrays.copyOf(homeX, newCap);
		homeZ = Arrays.copyOf(homeZ, newCap);
		homePos = Arrays.copyOf(homePos, newCap);
		homeSlot = Arrays.copyOf(homeSlot, newCap);
		presentationFlags = Arrays.copyOf(presentationFlags, newCap);
		wx = Arrays.copyOf(wx, newCap);
		wz = Arrays.copyOf(wz, newCap);
		uuidHi = Arrays.copyOf(uuidHi, newCap);
		uuidLo = Arrays.copyOf(uuidLo, newCap);
		tx = Arrays.copyOf(tx, newCap);
		tz = Arrays.copyOf(tz, newCap);
		sx = Arrays.copyOf(sx, newCap);
		sy = Arrays.copyOf(sy, newCap);
		sz = Arrays.copyOf(sz, newCap);
		sstate = Arrays.copyOf(sstate, newCap);
		stick = Arrays.copyOf(stick, newCap);
		stuckTick = Arrays.copyOf(stuckTick, newCap);
		bestDist2 = Arrays.copyOf(bestDist2, newCap);
		capacity = newCap;
        sepX = Arrays.copyOf(sepX, newCap);
        sepZ = Arrays.copyOf(sepZ, newCap);
        halt = Arrays.copyOf(halt, newCap);
        shalt = Arrays.copyOf(shalt, newCap);
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
	 * @return 运行期索引 / runtime index
	 */
	public int add(int newId, int x, int y, int z) {
		if (newId <= 0 || newId == Integer.MAX_VALUE)
			throw new IllegalArgumentException("Invalid citizen id " + newId);
		if (idToIndex.containsKey(newId))
			throw new IllegalArgumentException("Duplicate citizen id " + newId);
		if (size == capacity)
			grow();
		int i = size++;
		id[i] = newId;
		px[i] = x;
		py[i] = y;
		pz[i] = z;
		homeX[i] = x >> 10;
		homeZ[i] = z >> 10;
		homePos[i] = NO_HOME_POS;
		homeSlot[i] = -1;
		presentationFlags[i] = 0;
		wx[i] = -1;
		wz[i] = -1;
		uuidHi[i] = 0;
		uuidLo[i] = 0;
		tx[i] = x;
		tz[i] = z;
		dir[i] = 4; // 南（+Z），与旧 yaw=0 的默认朝向一致
        sdir[i] = 4;
        state[i] = CitizenState.IDLE;
		sx[i] = x;
		sy[i] = y;
		sz[i] = z;
		sstate[i] = CitizenState.IDLE;
		stick[i] = 0;
		stuckTick[i] = 0;
		bestDist2[i] = 0;
		idToIndex.put(newId, i);
        sepX[i] = 0;
        sepZ[i] = 0;
        halt[i] = 0;
        shalt[i] = 0;
		return i;
	}

	/**
	 * Compatibility overload accepting a legacy phase parameter.
	 */
	public int add(int newId, int x, int y, int z, byte phase) {
		return add(newId, x, y, z);
	}

	/** Returns the mirrored Resident age used only for client presentation. */
	public int presentationAge(int index) {
		return switch ((presentationFlags[index] & PRESENTATION_AGE_MASK) >>> PRESENTATION_AGE_SHIFT) {
			case 1 -> Resident.AGE_INFANT;
			case 2 -> Resident.AGE_CHILD;
			case 3 -> Resident.AGE_ELDER;
			default -> Resident.AGE_ADULT;
		};
	}

	/** Updates the transient age mirror and reports whether its visual value changed. */
	public boolean setPresentationAge(int index, int age) {
		int encoded = switch (age) {
			case Resident.AGE_INFANT -> 1;
			case Resident.AGE_CHILD -> 2;
			case Resident.AGE_ELDER -> 3;
			default -> 0;
		};
		byte previous = presentationFlags[index];
		presentationFlags[index] = (byte) ((previous & ~PRESENTATION_AGE_MASK)
				| (encoded << PRESENTATION_AGE_SHIFT));
		return previous != presentationFlags[index];
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
			dir[i] = dir[last];
            sdir[i] = sdir[last];
			state[i] = state[last];
			homeX[i] = homeX[last];
			homeZ[i] = homeZ[last];
			homePos[i] = homePos[last];
			homeSlot[i] = homeSlot[last];
			presentationFlags[i] = presentationFlags[last];
			wx[i] = wx[last];
			wz[i] = wz[last];
			uuidHi[i] = uuidHi[last];
			uuidLo[i] = uuidLo[last];
			tx[i] = tx[last];
			tz[i] = tz[last];
			sx[i] = sx[last];
			sy[i] = sy[last];
			sz[i] = sz[last];
			sstate[i] = sstate[last];
			stick[i] = stick[last];
			stuckTick[i] = stuckTick[last];
			bestDist2[i] = bestDist2[last];
			idToIndex.put(id[i], i);
            sepX[i] = sepX[last];
            sepZ[i] = sepZ[last];
            halt[i] = halt[last];
            shalt[i] = shalt[last];
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
	 * 替换一个运行期会话 id，同时维护反向索引。仅用于跨容器 id
	 * 冲突恢复；位置、状态、目标和持久居民 UUID 均保持不变。
	 * <p>
	 * Replaces a runtime session id while maintaining the reverse index.
	 * Used only for cross-container collision recovery; all position,
	 * state, target and durable resident UUID fields are preserved.
	 *
	 * @param index 运行期索引 / runtime index
	 * @param newId 新稳定 id / new stable id
	 * @return 旧 id / previous id
	 */
	public int replaceId(int index, int newId) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException(index);
		if (newId <= 0 || newId == Integer.MAX_VALUE)
			throw new IllegalArgumentException("Invalid citizen id " + newId);
		int oldId = id[index];
		if (oldId == newId)
			return oldId;
		if (idToIndex.containsKey(newId))
			throw new IllegalArgumentException("Duplicate citizen id " + newId);
		idToIndex.remove(oldId);
		id[index] = newId;
		idToIndex.put(newId, index);
		return oldId;
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
		tag.putByteArray("dir", Arrays.copyOf(dir, size));
		tag.putByteArray("state", Arrays.copyOf(state, size));
		tag.putIntArray("homeX", Arrays.copyOf(homeX, size));
		tag.putIntArray("homeZ", Arrays.copyOf(homeZ, size));
		tag.putLongArray("uuidHi", Arrays.copyOf(uuidHi, size));
		tag.putLongArray("uuidLo", Arrays.copyOf(uuidLo, size));
		tag.putIntArray("tx", Arrays.copyOf(tx, size));
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
		int[] ids = tag.getIntArray("id");
		int[] apx = tag.getIntArray("px");
		int[] apy = tag.getIntArray("py");
		int[] apz = tag.getIntArray("pz");
		byte[] adir = tag.getByteArray("dir");
        byte[] asdir = tag.getByteArray("sdir");
		// 旧存档回退：连续 yaw → 16 向方向（见 CitizenState.dirFromYaw）
		byte[] ayaw = tag.getByteArray("yaw");
        byte[] asyaw = tag.getByteArray("syaw");
		byte[] astate = tag.getByteArray("state");
		int[] ahomeX = tag.getIntArray("homeX");
		int[] ahomeZ = tag.getIntArray("homeZ");
		// 旧存档无 uuid 数组 → 空数组，全部视为未托管命令居民（向后兼容）
		long[] auuidHi = tag.getLongArray("uuidHi");
		long[] auuidLo = tag.getLongArray("uuidLo");
		int[] atx = tag.getIntArray("tx");
		int[] atz = tag.getIntArray("tz");
		// id/position are the minimum viable record. Clamp a corrupt declared
		// size to the available core arrays; all remaining arrays are optional
		// so older saves can fall back to the defaults established by add().
		int n = Math.max(0, tag.getInt("size"));
		n = Math.min(n, Math.min(Math.min(ids.length, apx.length),
				Math.min(apy.length, apz.length)));
		CitizenSim sim = new CitizenSim(n);
		Set<UUID> managedIdentities = new HashSet<>();
		for (int k = 0; k < n; k++) {
			if (ids[k] <= 0 || ids[k] == Integer.MAX_VALUE)
				continue;
			// A duplicated stable id makes one array slot unreachable through the
			// id index and breaks removal/sync. Preserve the first valid record and
			// discard later corrupt duplicates.
			if (sim.indexOf(ids[k]) >= 0)
				continue;
			UUID managedIdentity = null;
			if (k < auuidHi.length && k < auuidLo.length
					&& (auuidHi[k] != 0 || auuidLo[k] != 0)) {
				managedIdentity = new UUID(auuidHi[k], auuidLo[k]);
				if (!managedIdentities.add(managedIdentity))
					continue;
			}
			int i = sim.add(ids[k], apx[k], apy[k], apz[k]);
			if (k < adir.length)
				sim.dir[i] = adir[k];
			else if (k < ayaw.length)
				sim.dir[i] = (byte) CitizenState.dirFromYaw(ayaw[k]);
			// sdir was introduced with the canonical dead-reckoning model; the
			// 16-way dir sync replaced both it and yaw. Older saves fall back to
			// the legacy syaw (converted) or the current dir as the baseline.
			if (k < asdir.length)
				sim.sdir[i] = asdir[k];
			else if (k < asyaw.length)
				sim.sdir[i] = (byte) CitizenState.dirFromYaw(asyaw[k]);
			else
				sim.sdir[i] = sim.dir[i];
			if (k < astate.length) {
				int loadedState = astate[k] & 0xFF;
				sim.state[i] = loadedState < CitizenState.STATE_COUNT ? astate[k] : CitizenState.IDLE;
			}
			if (k < ahomeX.length)
				sim.homeX[i] = ahomeX[k];
			if (k < ahomeZ.length)
				sim.homeZ[i] = ahomeZ[k];
			// Identity is an atomic pair. A half-written UUID is treated as the
			// unmanaged sentinel instead of inventing a different resident id.
			if (managedIdentity != null) {
				sim.uuidHi[i] = managedIdentity.getMostSignificantBits();
				sim.uuidLo[i] = managedIdentity.getLeastSignificantBits();
			}
			if (k < atx.length)
				sim.tx[i] = atx[k];
			if (k < atz.length)
				sim.tz[i] = atz[k];
		}
		return sim;
	}
}
