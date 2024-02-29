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

package com.teammoeg.frostedheart.climate.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHBodyDataSyncPacket implements FHMessage {
    private final CompoundNBT data;

    public FHBodyDataSyncPacket(PacketBuffer buffer) {
        data = buffer.readCompoundTag();
    }

    public FHBodyDataSyncPacket(PlayerEntity pe) {
        this.data = PlayerTemperatureData.getCapability(pe).map(t->t.serializeNBT()).orElseGet(CompoundNBT::new);
    }

	@Override
	public void encode(PacketBuffer buffer) {
        buffer.writeCompoundTag(data);
    }

	@Override
	public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getWorld);
            PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
            if (world != null) {
            	PlayerTemperatureData.getCapability(player).ifPresent(t->t.deserializeNBT(data));
            }
        });
        context.get().setPacketHandled(true);
    }
}
