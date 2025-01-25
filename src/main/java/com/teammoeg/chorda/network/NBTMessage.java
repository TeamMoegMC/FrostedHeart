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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public abstract class NBTMessage implements CMessage {
	private CompoundTag tag;

	public NBTMessage(FriendlyByteBuf buffer) {
		this(buffer.readNbt());
	}
	public NBTMessage(CompoundTag tag) {
		super();
		this.tag = tag;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeNbt(tag);
	}


	public CompoundTag getTag() {
		return tag;
	}


	public void setTag(CompoundTag tag) {
		this.tag = tag;
	}

}
