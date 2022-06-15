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

package com.teammoeg.frostedheart.network.research;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.climate.TemperatureCore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHEnergyDataSyncPacket {
    private final long energy;
    private final long penergy;
    public FHEnergyDataSyncPacket(CompoundNBT data) {
        this.energy = data.getLong("energy");
        this.penergy =data.getLong("penergy");
    }

    public FHEnergyDataSyncPacket(PacketBuffer buffer) {
        energy=buffer.readVarLong();
        penergy=buffer.readVarLong();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeVarLong(energy);
        buffer.writeVarLong(penergy);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
            if (player != null) {
            	CompoundNBT data=TemperatureCore.getFHData(player);
            	data.putLong("energy", energy);
            	data.putLong("penergy", penergy);
                TemperatureCore.setFHData(player, data);
            }
        });
        context.get().setPacketHandled(true);
    }
}
