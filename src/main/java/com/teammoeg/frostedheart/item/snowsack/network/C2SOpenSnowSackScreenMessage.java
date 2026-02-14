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

package com.teammoeg.frostedheart.item.snowsack.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.item.snowsack.SnowSackItem;
import com.teammoeg.frostedheart.item.snowsack.ui.SnowSackMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record C2SOpenSnowSackScreenMessage(int index) implements CMessage{

	public C2SOpenSnowSackScreenMessage(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(index);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			var player = context.get().getSender();
			if (player != null) {
				var item = player.inventoryMenu.getSlot(index).getItem();
				if (item.getItem() instanceof SnowSackItem) {
					NetworkHooks.openScreen(player, new SnowSackMenuProvider(player, index), buf -> buf.writeInt(index));
				}
			}
		});
		context.get().setPacketHandled(true);
	}

}
