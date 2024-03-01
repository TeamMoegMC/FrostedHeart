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

package com.teammoeg.frostedheart.town;

import com.teammoeg.frostedheart.climate.network.FHMessage;
import com.teammoeg.frostedheart.team.SpecialDataManager;
import com.teammoeg.frostedheart.team.SpecialDataTypes;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class TeamTownDataS2CPacket implements FHMessage {

    CompoundNBT townData = new CompoundNBT();

    public TeamTownDataS2CPacket(PlayerEntity player) {
        SpecialDataManager.getData(player).getData(SpecialDataTypes.TOWN_DATA).save(townData, true);
    }

    public TeamTownDataS2CPacket(TeamTownData townData) {
        townData.save(this.townData, true);
    }
    @Override
    public void encode(PacketBuffer buffer) {
        buffer.writeCompoundTag(townData);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            PlayerEntity player = ClientUtils.getPlayer();
            if (player != null) {
                SpecialDataManager.getData(player).getData(SpecialDataTypes.TOWN_DATA).deserializeNBT(townData);
            }
        });
        context.get().setPacketHandled(true);
    }
}
