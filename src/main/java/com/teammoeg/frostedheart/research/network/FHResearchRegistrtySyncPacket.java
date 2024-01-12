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

package com.teammoeg.frostedheart.research.network;

import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.SpecialResearch;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHResearchRegistrtySyncPacket {
    private final CompoundNBT data;
    List<Research> rss;

    public FHResearchRegistrtySyncPacket() {
        this.data = FHResearch.save(new CompoundNBT());

    }

    public FHResearchRegistrtySyncPacket(PacketBuffer buffer) {
        data = buffer.readCompoundTag();
        rss = SerializeUtil.readList(buffer, SpecialResearch::deserialize);
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeCompoundTag(data);
        FHResearch.saveAll(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            FHResearch.initFromPacket(data, rss);
        });
        context.get().setPacketHandled(true);
    }
}
