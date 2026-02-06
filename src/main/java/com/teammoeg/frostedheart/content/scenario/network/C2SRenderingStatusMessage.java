/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.Act;
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record C2SRenderingStatusMessage(int thread,boolean isTransComplete) implements CMessage{
	public C2SRenderingStatusMessage(FriendlyByteBuf buffer) {
		this(buffer.readVarInt(),buffer.readBoolean());
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(thread);
		buffer.writeBoolean(isTransComplete);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			ScenarioConductor conductor=FHScenario.get(context.get().getSender());
			Act act=conductor.getById(thread);
			if(act.getStatus()==RunStatus.WAITRENDER&&!isTransComplete) {
				act.setStatus(RunStatus.RUNNING);
			}else if(act.getStatus()==RunStatus.WAITTRANS&&isTransComplete) {
				act.setStatus(RunStatus.RUNNING);
			}
		});
		context.get().setPacketHandled(true);
	}

}
