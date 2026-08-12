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
 * 服务端 → 客户端：居民进入 AOI 时的全量出生包。
 * 每条目约 15 字节 + 名字 UTF（定点绝对坐标，不压缩——出生是低频事件）。
 * <p>
 * Server → client: full spawn packet when citizens enter the AOI.
 * ~15 bytes per entry plus the name UTF (absolute fixed-point coords, uncompressed — spawning is low-frequency).
 */
public final class S2CCitizenSpawnPacket implements CMessage {

	/** 出生条目 / Spawn entry */
	public record Entry(int id, int px, int py, int pz, byte yaw, byte state, String name) {
	}

	private final List<Entry> entries;

	public S2CCitizenSpawnPacket(List<Entry> entries) {
		this.entries = entries;
	}

	public S2CCitizenSpawnPacket(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		this.entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			entries.add(new Entry(buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readByte(),
					buf.readByte(), buf.readUtf(64)));
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(entries.size());
		for (Entry e : entries) {
			buf.writeVarInt(e.id());
			buf.writeInt(e.px());
			buf.writeInt(e.py());
			buf.writeInt(e.pz());
			buf.writeByte(e.yaw());
			buf.writeByte(e.state());
			buf.writeUtf(e.name(), 64);
		}
	}

	@Override
	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(
				() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCitizenCache.applySpawn(entries)));
		context.get().setPacketHandled(true);
	}
}
