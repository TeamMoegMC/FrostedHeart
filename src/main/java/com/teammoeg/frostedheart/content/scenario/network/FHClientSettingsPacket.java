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

package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioVaribles;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class FHClientSettingsPacket implements CMessage {
    double scale;
    int scaledWidth;
    int scaledHeight;

    public FHClientSettingsPacket(FriendlyByteBuf buffer) {
        scale=buffer.readDouble();
        scaledWidth=buffer.readInt();
        scaledHeight=buffer.readInt();
    }


    public FHClientSettingsPacket() {
        super();
        this.scale=Minecraft.getInstance().getWindow().getGuiScale();
        this.scaledWidth=Minecraft.getInstance().getWindow().getGuiScaledWidth();
        this.scaledHeight=Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }


    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(scale);
        buffer.writeInt(scaledWidth);
        buffer.writeInt(scaledHeight);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            IScenarioVaribles sv=FHScenario.get(context.get().getSender()).getContext().getVaribles();
            sv.getExecutionData().putDouble("uiScale", scale);
            sv.getExecutionData().putDouble("scaledWidth", scaledWidth);
            sv.getExecutionData().putDouble("scaledHeight", scaledHeight);
            /*if(sv.getSnapshot()!=null) {
            	sv.getSnapshot().putDouble("uiScale", scale);
                sv.getSnapshot().putDouble("scaledWidth", scaledWidth);
                sv.getSnapshot().putDouble("scaledHeight", scaledHeight);
            }*/
            	
        });
        context.get().setPacketHandled(true);
    }
}
