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

package com.teammoeg.frostedheart.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.client.util.FHClientUtils;
import com.teammoeg.frostedheart.climate.SurviveTemperature;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHDataSyncPacket {
	private final CompoundNBT data;

	public FHDataSyncPacket(PlayerEntity pe) {
		this.data = SurviveTemperature.getFHData(pe);
	}

	FHDataSyncPacket(PacketBuffer buffer) {
		data = buffer.readCompoundTag();
	}

	void encode(PacketBuffer buffer) {
		buffer.writeCompoundTag(data);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			// Update client-side nbt
			World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> FHClientUtils::getWorld);
			if (world != null) {
				// First, synchronize the chunk data in the capability and cache.
				// Then, update the single data instance with the packet data
				SurviveTemperature.setFHData(FHClientUtils.mc().player,data);
			}
		});
		context.get().setPacketHandled(true);
	}
}
