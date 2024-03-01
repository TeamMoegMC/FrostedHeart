/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.research.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.network.NBTMessage;
import com.teammoeg.frostedheart.research.FHResearch;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHResearchRegistrtySyncPacket extends NBTMessage {

    public FHResearchRegistrtySyncPacket() {
        super(FHResearch.save(new CompoundNBT()));

    }

    public FHResearchRegistrtySyncPacket(PacketBuffer buffer) {
        super(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            FHResearch.initFromRegistry(this.getTag());
        });
        context.get().setPacketHandled(true);
    }
}
