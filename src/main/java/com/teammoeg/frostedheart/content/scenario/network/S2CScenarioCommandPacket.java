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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.FHScenarioClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record S2CScenarioCommandPacket(int runid,String commandName, Map<String, String> params) implements CMessage {


    public S2CScenarioCommandPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(),buffer.readUtf(),SerializeUtil.readStringMap(buffer, new HashMap<>(), FriendlyByteBuf::readUtf));
    }

    

    public void encode(FriendlyByteBuf buffer) {
    	buffer.writeVarInt(runid);
        buffer.writeUtf(commandName);
        SerializeUtil.writeStringMap(buffer, params, (v, p) -> p.writeUtf(v));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
        	//System.out.println(this);
        	ClientScene.INSTANCE.curRunId=runid;
            FHScenarioClient.callCommand(commandName, ClientScene.INSTANCE, params);
        });
        context.get().setPacketHandled(true);
    }

	@Override
	public String toString() {
		return "ServerScenarioCommandPacket [commandName=" + commandName + ", params=" + params + "]";
	}
}
