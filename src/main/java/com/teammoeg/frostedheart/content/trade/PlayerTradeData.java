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

package com.teammoeg.frostedheart.content.trade;

import com.teammoeg.chorda.io.NBTSerializable;

import net.minecraft.nbt.CompoundTag;

public class PlayerTradeData implements NBTSerializable {
	int killed_indirect;
	public PlayerTradeData() {
	}
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		nbt.putInt("killed_indirect", killed_indirect);
		
	}
	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
		killed_indirect=nbt.getInt("killed_indirect");
	}

}
