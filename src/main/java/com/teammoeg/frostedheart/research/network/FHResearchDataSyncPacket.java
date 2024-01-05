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

package com.teammoeg.frostedheart.research.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHResearchDataSyncPacket {
    private final CompoundNBT data;

    public FHResearchDataSyncPacket(Team team) {
        this.data = FHResearchDataManager.INSTANCE.getData(team).serialize(true);
    }

    public FHResearchDataSyncPacket(CompoundNBT data) {
		super();
		this.data = data;
	}

	public FHResearchDataSyncPacket(PacketBuffer buffer) {
        data = buffer.readCompoundTag();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeCompoundTag(data);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            TeamResearchData.resetClientInstance();
            TeamResearchData.getClientInstance().deserialize(data, true);
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::syncJEI);
        });
        context.get().setPacketHandled(true);
    }
}
