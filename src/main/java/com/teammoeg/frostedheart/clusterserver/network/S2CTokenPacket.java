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

package com.teammoeg.frostedheart.clusterserver.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class S2CTokenPacket implements CMessage{
	String token;

	public S2CTokenPacket(String token) {
		super();
		this.token = token;
	}
	public S2CTokenPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf());
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(token);
	}

	@Override
	public void handle(Supplier<Context> context) {
		
		ClientConnectionHelper.token=token;
		
	}

}
