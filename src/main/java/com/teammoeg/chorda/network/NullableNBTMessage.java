/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.network;

import java.util.Optional;

import com.teammoeg.chorda.io.SerializeUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public abstract class NullableNBTMessage extends NBTMessage {

	public NullableNBTMessage(FriendlyByteBuf buffer) {
		this(SerializeUtil.readOptional(buffer, FriendlyByteBuf::readNbt));
	}

	public NullableNBTMessage(Optional<CompoundTag> tag) {
		super(tag.orElse(null));
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeOptional2(buffer, this.getTag(),FriendlyByteBuf::writeNbt);
	}

}
