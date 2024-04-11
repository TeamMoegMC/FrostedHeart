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

package com.teammoeg.frostedheart.content.research.research.clues;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.content.research.data.IClueData;
import com.teammoeg.frostedheart.util.io.registry.TypedCodecRegistry;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;

public class ClueDatas {
    public static final TypedCodecRegistry<IClueData> registry = new TypedCodecRegistry<>();
    public static final Codec<IClueData> CODEC=registry.codec();
    public ClueDatas() {
    }
	public <A extends IClueData> void register(Class<A> cls, String type,Codec<A> codec) {
		registry.register(cls, type,codec);
	}
	public static IClueData read(CompoundNBT nbt) {
		return registry.read(NBTDynamicOps.INSTANCE, nbt);
	}
	public static CompoundNBT write(IClueData nbt) {
		return (CompoundNBT) registry.write(NBTDynamicOps.INSTANCE, nbt);
	}
}
