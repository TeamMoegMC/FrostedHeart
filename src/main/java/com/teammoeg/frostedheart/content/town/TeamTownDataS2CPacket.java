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

package com.teammoeg.frostedheart.content.town;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHClientTeamDataManager;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.util.io.codec.DataOps;
import com.teammoeg.frostedheart.util.io.codec.ObjectWriter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class TeamTownDataS2CPacket implements FHMessage {
	Object data;

    public TeamTownDataS2CPacket(PlayerEntity player) {
    	this(FHTeamDataManager.get(player).getData(SpecialDataTypes.TOWN_DATA));
    }

	public TeamTownDataS2CPacket(PacketBuffer buffer) {
		data=ObjectWriter.readObject(buffer);
	}

	public TeamTownDataS2CPacket(TeamTownData townData) {
		data=SpecialDataTypes.TOWN_DATA.loadData(DataOps.COMPRESSED, data);
    }
    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> FHClientTeamDataManager.INSTANCE.getInstance().setData(SpecialDataTypes.TOWN_DATA,SpecialDataTypes.TOWN_DATA.loadData(DataOps.COMPRESSED, data)));
        context.get().setPacketHandled(true);
    }

	@Override
	public void encode(PacketBuffer buffer) {
		ObjectWriter.writeObject(buffer, data);
	}
}
