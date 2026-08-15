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

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.citizen.client.ClientCitizenCache;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：居民离开 AOI 或被移除时的销毁包，仅含 id 列表。
 * <p>
 * Server → client: despawn packet for citizens leaving the AOI or being
 * removed; carries only an id list.
 */
public final class S2CCitizenDespawnPacket implements CMessage {

	private final IntList ids;

	public S2CCitizenDespawnPacket(IntList ids) {
		this.ids = ids;
	}

	public S2CCitizenDespawnPacket(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		this.ids = new IntArrayList(count);
		for (int i = 0; i < count; i++)
			ids.add(buf.readVarInt());
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(ids.size());
		for (int i = 0; i < ids.size(); i++)
			buf.writeVarInt(ids.getInt(i));
	}

	@Override
	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(
				() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCitizenCache.applyDespawn(ids)));
		context.get().setPacketHandled(true);
	}
}
