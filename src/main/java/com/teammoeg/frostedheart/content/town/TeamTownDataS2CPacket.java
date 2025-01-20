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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.chorda.team.CClientTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.chorda.util.io.codec.DataOps;
import com.teammoeg.chorda.util.io.codec.ObjectWriter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TeamTownDataS2CPacket implements CMessage {
	Object data;

    public TeamTownDataS2CPacket(Player player) {
    	this(CTeamDataManager.get(player).getData(FHSpecialDataTypes.TOWN_DATA));
    }

	public TeamTownDataS2CPacket(FriendlyByteBuf buffer) {
		data=ObjectWriter.readObject(buffer);
	}

	public TeamTownDataS2CPacket(TeamTownData townData) {
		try {
			data= FHSpecialDataTypes.TOWN_DATA.saveData(DataOps.COMPRESSED, townData);
		} catch (Exception e) {
			FHMain.LOGGER.error("Failed to save town data when syncing town data", e);
		}
    }
    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
			try {
				CClientTeamDataManager.INSTANCE.getInstance().setData(FHSpecialDataTypes.TOWN_DATA, FHSpecialDataTypes.TOWN_DATA.loadData(DataOps.COMPRESSED, data));
			} catch (Exception e) {
				FHMain.LOGGER.error("Failed to load data when syncing town data", e);
			}
		});
        context.get().setPacketHandled(true);
    }

	@Override
	public void encode(FriendlyByteBuf buffer) {
		ObjectWriter.writeObject(buffer, data);
	}
}
