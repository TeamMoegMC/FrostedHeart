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

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.climate.TemperatureCore;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.effects.Effect;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHEffectTriggerPacket {
	private final int researchID;

	public FHEffectTriggerPacket(Research r) {
		this.researchID = r.getRId();
	}

	FHEffectTriggerPacket(PacketBuffer buffer) {
		researchID = buffer.readVarInt();

	}

	void encode(PacketBuffer buffer) {
		buffer.writeVarInt(researchID);
	}

	void handle(Supplier<NetworkEvent.Context> context) {

		context.get().enqueueWork(() -> {
			Research r=FHResearch.researches.getById(researchID);
			TeamResearchData trd=ResearchDataAPI.getData(context.get().getSender());
			ServerPlayerEntity spe=context.get().getSender();
			if(trd.getData(r).isCompleted()) {
				for(Effect e:r.getEffects())
				trd.grantEffect(e,spe);
			}
		});
		context.get().setPacketHandled(true);
	}
}
