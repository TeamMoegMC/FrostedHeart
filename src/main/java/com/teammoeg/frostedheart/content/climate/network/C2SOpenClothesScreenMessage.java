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

package com.teammoeg.frostedheart.content.climate.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.menu.DummyMenuProvider;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.block.ClothesInventoryMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkHooks;

public record C2SOpenClothesScreenMessage() implements CMessage{
	public C2SOpenClothesScreenMessage(FriendlyByteBuf buffer) {
		this();
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			NetworkHooks.openScreen(context.get().getSender(), new DummyMenuProvider((id,inv,player)->new ClothesInventoryMenu(id,inv)));
		});
		context.get().setPacketHandled(true);
	}

}
