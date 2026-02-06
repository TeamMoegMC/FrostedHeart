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
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class S2CSenarioScenePacket implements CMessage {
	private final int curTextId;
    private final String text;
    private final boolean isReline;
    private final boolean isNowait;
    private final boolean resetScene;
    private final RunStatus status;
    private final boolean noDelay;
    public S2CSenarioScenePacket(FriendlyByteBuf buffer) {
    	curTextId=buffer.readVarInt();
        text = buffer.readUtf(1024 * 300);
        isReline = buffer.readBoolean();
        isNowait = buffer.readBoolean();
        resetScene=buffer.readBoolean();
        status=RunStatus.values()[buffer.readByte()];
        noDelay=buffer.readBoolean();
    }



    public S2CSenarioScenePacket(int curTextId,String text, boolean isReline, boolean isNowait, boolean resetScene,RunStatus status,boolean noDelay) {
		super();
		this.curTextId=curTextId;
		this.text = text;
		this.isReline = isReline;
		this.isNowait = isNowait;
		this.resetScene = resetScene;
		this.status=status;
		this.noDelay=noDelay;
	}



	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(curTextId);
        buffer.writeUtf(text, 1024 * 300);
        buffer.writeBoolean(isReline);
        buffer.writeBoolean(isNowait);
        buffer.writeBoolean(resetScene);
        buffer.writeByte(status.ordinal());
        buffer.writeBoolean(noDelay);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(ClientScene.INSTANCE!=null) {
        		//System.out.println("scene sent");
        		ClientScene.INSTANCE.process(curTextId,text, isReline, isNowait,resetScene,status);
        		ClientScene.INSTANCE.sendImmediately=noDelay;
        	}
        });
        
        context.get().setPacketHandled(true);
    }
}
