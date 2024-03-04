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

package com.teammoeg.frostedheart.content.research.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.NBTMessage;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHResearchDataSyncPacket extends NBTMessage {

    public FHResearchDataSyncPacket(CompoundNBT data) {
        super(data);
    }

    public FHResearchDataSyncPacket(PacketBuffer buffer) {
        super(buffer);
    }

    public FHResearchDataSyncPacket(TeamResearchData team) {
        super(team.serialize(true));
    }


    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientResearchDataAPI.getData().deserialize(this.getTag(), true);
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::syncJEI);
        });
        context.get().setPacketHandled(true);
    }
}
