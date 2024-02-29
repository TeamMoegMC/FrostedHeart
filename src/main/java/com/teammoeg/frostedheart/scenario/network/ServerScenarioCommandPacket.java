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

package com.teammoeg.frostedheart.scenario.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.climate.network.FHMessage;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.client.FHScenarioClient;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ServerScenarioCommandPacket implements FHMessage {
    private String commandName;
    Map<String, String> params;

    public ServerScenarioCommandPacket(PacketBuffer buffer) {
        commandName = buffer.readString();
        params = SerializeUtil.readStringMap(buffer, new HashMap<>(), PacketBuffer::readString);
    }

    
    public ServerScenarioCommandPacket(String commandName, Map<String, String> params) {
        super();
        this.commandName = commandName;
        this.params = params;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeString(commandName);
        SerializeUtil.writeStringMap(buffer, params, (v, p) -> p.writeString(v));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
        	//System.out.println(this);
            FHScenarioClient.callCommand(commandName, ClientScene.INSTANCE, params);
        });
        context.get().setPacketHandled(true);
    }

	@Override
	public String toString() {
		return "ServerScenarioCommandPacket [commandName=" + commandName + ", params=" + params + "]";
	}
}
