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

package com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.NBTMessage;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.chorda.util.client.ClientUtils;
import com.teammoeg.chorda.util.io.NBTSerializable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class FHBodyDataSyncPacket extends NBTMessage {
    public FHBodyDataSyncPacket(Player pe) {
        super(PlayerTemperatureData.getCapability(pe).map(NBTSerializable::serializeNBT).orElseGet(CompoundTag::new));
    }


    public FHBodyDataSyncPacket(FriendlyByteBuf buffer) {
        super(buffer);
    }


    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            Level world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getWorld);
            Player player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
            if (world != null) {
                PlayerTemperatureData.getCapability(player).ifPresent(t -> t.deserializeNBT(super.getTag()));
            }
        });
        context.get().setPacketHandled(true);
    }
}
