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

package com.teammoeg.frostedheart.content.research.network;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.NBTMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// send when player join
public class FHResearchAttributeSyncPacket extends NBTMessage {

    public FHResearchAttributeSyncPacket(CompoundTag data) {
        super(data.copy());
    }

    public FHResearchAttributeSyncPacket(FriendlyByteBuf buffer) {
        super(buffer);
    }

    public FHResearchAttributeSyncPacket(TeamDataHolder team) {
        super(team.getData(FHSpecialDataTypes.RESEARCH_DATA).getVariants().copy());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientResearchDataAPI.getData().get().setVariants(this.getTag()));
        context.get().setPacketHandled(true);
    }
}
