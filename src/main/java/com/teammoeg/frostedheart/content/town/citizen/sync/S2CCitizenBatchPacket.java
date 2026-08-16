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
 * 外加 1 个状态+方向打包字节（bit0-2 状态，bit3-6 十六向方向，见
 * {@link com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState#packStateDir}），
 * 每条目恒定 6–8 字节，无分支编解码。
 * 客户端按"位置快照 + 16 向方向 × 状态速度"做外推，误差由服务端 Dead Reckoning 控制。
 * <p>
 * Server → client: batched movement delta packet — the core of bandwidth
 * optimization. Entries are grouped by chunk: the header carries the chunk base
 * coordinate once; each entry then only needs intra-chunk offsets (x/z 1 byte
 * each at 1/16-block precision; y as a 2-byte short at 1/16-block precision)
 * plus one packed state+direction byte (bits 0-2 state, bits 3-6 16-way
 * direction, see CitizenState.packStateDir) — a constant 6–8 bytes per entry
 * with branchless encode/decode.
 * The client extrapolates "snapshot + 16-way dir × state speed"; error is
 * bounded by server-side dead reckoning.
 */
public final class S2CCitizenBatchPacket implements CMessage {

	/** chunk 内 X/Z 偏移的量化：64 定点单位 = 1/16 方块 / Intra-chunk offset quantization: 64 fixed units = 1/16 block */
	public static final int LOCAL_QUANT = 64;

	/** 单条增量 / One delta entry */
	public record Entry(int id, int lx, int ly, int lz, byte stateDir) {
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
				byte sd = buf.readByte();
				entries.add(new Entry(id, lx, ly, lz, sd));
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
				buf.writeByte(e.stateDir());
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
