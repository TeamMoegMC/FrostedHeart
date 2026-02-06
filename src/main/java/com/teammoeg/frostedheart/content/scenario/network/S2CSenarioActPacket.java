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

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class S2CSenarioActPacket implements CMessage {
    private final String title;
    private final String subtitle;
    public S2CSenarioActPacket(FriendlyByteBuf buffer) {
    	title =  SerializeUtil.readOptional(buffer, FriendlyByteBuf::readUtf).orElse(null);
    	subtitle = SerializeUtil.readOptional(buffer, FriendlyByteBuf::readUtf).orElse(null);
    }
	public S2CSenarioActPacket(String title, String subtitle) {
		super();
		this.title = title;
		this.subtitle = subtitle;
	}
	public void encode(FriendlyByteBuf buffer) {
        SerializeUtil.writeOptional2(buffer, title, FriendlyByteBuf::writeUtf);
        SerializeUtil.writeOptional2(buffer, subtitle, FriendlyByteBuf::writeUtf);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(ClientScene.INSTANCE!=null)
        		ClientScene.INSTANCE.setActHud(title,subtitle);
        });
        context.get().setPacketHandled(true);
    }
}
