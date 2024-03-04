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

import java.util.function.Function;

import com.teammoeg.frostedheart.content.research.data.IClueData;
import com.teammoeg.frostedheart.util.io.NBTSerializerRegistry;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class ClueDatas {
    public static NBTSerializerRegistry<IClueData> registry = new NBTSerializerRegistry<>();

    public ClueDatas() {
    }
    public static void write(PacketBuffer pb,IClueData fromObj) {
		registry.write(pb, fromObj);
	}
    public static CompoundNBT write(IClueData fromObj) {
		return registry.write(fromObj);
	}

	public static IClueData read(CompoundNBT jo) {
        return registry.read(jo);
    }

    public static IClueData read(PacketBuffer pb) {
        return registry.read(pb);
    }

    public static void register(Class<? extends IClueData> cls, String id, Function<CompoundNBT, IClueData> j, Function<IClueData,CompoundNBT> o, Function<PacketBuffer, IClueData> p) {
        registry.register(cls, id, j,o, p);
    }
}
