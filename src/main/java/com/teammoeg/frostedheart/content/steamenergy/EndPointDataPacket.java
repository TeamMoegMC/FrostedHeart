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

package com.teammoeg.frostedheart.content.steamenergy;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.SpecialResearch;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.trade.ClientHeatHandler;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class EndPointDataPacket {
    private final Collection<EndPointData> data;

    public EndPointDataPacket(HeatEnergyNetwork ewn) {
        this.data = ewn.data.values();

    }

    public EndPointDataPacket(PacketBuffer buffer) {
        data = SerializeUtil.readList(buffer, EndPointData::readNetwork);
    }

    public void encode(PacketBuffer buffer) {
        SerializeUtil.writeList(buffer, data, EndPointData::writeNetwork);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	ClientHeatHandler.loadEndPoint(data);
        });
        context.get().setPacketHandled(true);
    }
}
