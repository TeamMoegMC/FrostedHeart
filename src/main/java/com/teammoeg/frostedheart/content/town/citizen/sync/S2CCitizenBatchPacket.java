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

package com.teammoeg.frostedheart.content.town.citizen.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.citizen.client.ClientCitizenCache;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：移动增量批包，本系统带宽优化的核心。
 * 按 chunk 分组：包头发一次 chunk 基准坐标，包内每人只发 chunk 内偏移
 * （x/z 各 1 字节，1/16 方块精度；y 为 2 字节短整型，1/16 方块精度），
 * 加上方向与状态各 1 字节（state 在前、yaw 在后——state 高位兼作纯心跳
 * 标记，解码端必须先读它），每条目约 7–9 字节；纯心跳条目只发 state
 * 标记字节、省略 yaw，降至 6–8 字节。
 * 客户端按"位置快照 + 方向 × 状态速度"做外推，误差由服务端 Dead Reckoning 控制。
 * <p>
 * Server → client: batched movement delta packet — the core of bandwidth
 * optimization. Entries are grouped by chunk: the header carries the chunk base
 * coordinate once; each entry then only needs intra-chunk offsets (x/z 1 byte
 * each at 1/16-block precision; y as a 2-byte short at 1/16-block precision)
 * plus 1 byte state and 1 byte yaw (state first — its high bit doubles
 * as the pure-heartbeat marker, so the decoder reads it before yaw),
 * roughly 7–9 bytes per entry; pure-heartbeat entries send the state marker
 * byte only, omitting yaw — 6–8 bytes.
 * The client extrapolates "snapshot + yaw × state speed"; error is
 * bounded by server-side dead reckoning.
 */
public final class S2CCitizenBatchPacket implements CMessage {

	/** chunk 内 X/Z 偏移的量化：64 定点单位 = 1/16 方块 / Intra-chunk offset quantization: 64 fixed units = 1/16 block */
	public static final int LOCAL_QUANT = 64;
	/**
	 * 纯心跳条目标记（state 字节高位）：状态/方向均未变的定期重锚条目只发位置，
	 * 编码时省略 yaw 字节；客户端解码后沿用最近值（TCP 保序必一致）。
	 * <p>
	 * Pure-heartbeat entry marker (high bit of the state byte): a periodic
	 * re-anchor entry whose state/yaw are unchanged carries position only and
	 * omits the yaw byte; the client reuses its last values (TCP ordering
	 * guarantees they match).
	 */
	public static final byte ENTRY_PURE_HEARTBEAT = (byte) 0x80;

	/** 单条增量 / One delta entry */
	public record Entry(int id, int lx, int ly, int lz, byte yaw, byte state) {
	}

	/** 一个 chunk 分组 / One chunk group */
	public record Group(int cx, int cz, List<Entry> entries) {
	}

	private final List<Group> groups;

	public S2CCitizenBatchPacket(List<Group> groups) {
		this.groups = groups;
	}

	public S2CCitizenBatchPacket(FriendlyByteBuf buf) {
		int groupCount = buf.readVarInt();
		this.groups = new ArrayList<>(groupCount);
		for (int g = 0; g < groupCount; g++) {
			int cx = buf.readVarInt();
			int cz = buf.readVarInt();
			int count = buf.readVarInt();
			List<Entry> entries = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				int id = buf.readVarInt();
				int lx = buf.readByte() & 0xFF;
				short ly = buf.readShort();
				int lz = buf.readByte() & 0xFF;
				byte st = buf.readByte();
				byte yaw = 0;
				if ((st & ENTRY_PURE_HEARTBEAT) == 0)
					yaw = buf.readByte(); // 纯心跳条目省略 yaw / pure-heartbeat entries omit yaw
				entries.add(new Entry(id, lx, ly, lz, yaw, st));
			}
			groups.add(new Group(cx, cz, entries));
		}
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(groups.size());
		for (Group g : groups) {
			buf.writeVarInt(g.cx());
			buf.writeVarInt(g.cz());
			buf.writeVarInt(g.entries().size());
			for (Entry e : g.entries()) {
				buf.writeVarInt(e.id());
				buf.writeByte(e.lx());
				buf.writeShort(e.ly());
				buf.writeByte(e.lz());
				// state 字节固定写在首位（兼作纯心跳标记位）：解码端必须先读它才能
				// 决定是否省略 yaw——若 state 在第二位，流式解码无法从首字节区分
				// "普通条目的 yaw"与"心跳条目的 state"，会错位（曾因此越界崩溃）。
				// The state byte always comes first (doubling as the pure-heartbeat
				// marker): the decoder branches on it before reading yaw, so the
				// heartbeat entry (state high bit set) writes exactly one byte and
				// yaw is omitted — unambiguous in the stream.
				if ((e.state() & ENTRY_PURE_HEARTBEAT) != 0)
					buf.writeByte(e.state()); // 纯心跳：只写 state 标记字节 / heartbeat: state marker byte only
				else {
					buf.writeByte(e.state());
					buf.writeByte(e.yaw());
				}
			}
		}
	}

	@Override
	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(
				() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCitizenCache.applyBatch(groups)));
		context.get().setPacketHandled(true);
	}
}
