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

import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHResearchAttributeSyncPacket {
    private final CompoundNBT data;

    public FHResearchAttributeSyncPacket(CompoundNBT data) {
        super();
        this.data = data.copy();
    }

    public FHResearchAttributeSyncPacket(PacketBuffer buffer) {
        data = buffer.readCompoundTag();
    }

    public FHResearchAttributeSyncPacket(Team team) {
        this.data = FHResearchDataManager.INSTANCE.getData(team).getVariants().copy();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeCompoundTag(data);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            TeamResearchData.getClientInstance().setVariants(this.data);
        });
        context.get().setPacketHandled(true);
    }
}
