/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.climate.chunkheatdata;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class ChunkHeatDataCapabilityProvider {
    @CapabilityInject(ChunkHeatData.class)
    public static Capability<ChunkHeatData> CAPABILITY;
    public static final ResourceLocation KEY = new ResourceLocation(FHMain.MODID, "chunk_data");

    public static void setup() {
        CapabilityManager.INSTANCE.register(ChunkHeatData.class, new Capability.IStorage<ChunkHeatData>() {
            public INBT writeNBT(Capability<ChunkHeatData> capability, ChunkHeatData instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<ChunkHeatData> capability, ChunkHeatData instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, () -> {
            return null;
        });
    }


}