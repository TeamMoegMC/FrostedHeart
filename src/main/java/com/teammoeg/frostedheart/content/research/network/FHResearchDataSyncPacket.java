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

import com.teammoeg.frostedheart.FHClientTeamDataManager;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.io.codec.DataOps;
import com.teammoeg.frostedheart.util.io.codec.ObjectWriter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHResearchDataSyncPacket implements FHMessage {
	Object dat;


	public FHResearchDataSyncPacket(FriendlyByteBuf buffer) {
		this.dat = (ObjectWriter.readObject(buffer));
    }

    public FHResearchDataSyncPacket(TeamResearchData team) {
    	this.dat = (SpecialDataTypes.RESEARCH_DATA.saveData(DataOps.COMPRESSED, team));
    }


    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	FHClientTeamDataManager.INSTANCE.getInstance().setData(SpecialDataTypes.RESEARCH_DATA, SpecialDataTypes.RESEARCH_DATA.loadData(DataOps.COMPRESSED, dat));
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::syncJEI);
        });
        context.get().setPacketHandled(true);
    }

	@Override
	public void encode(FriendlyByteBuf buffer) {
		ObjectWriter.writeObject(buffer, dat);
	}
}
